package com.lingju.assistant.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.AlarmRingingActivity;
import com.lingju.assistant.activity.RemindActivity;
import com.lingju.assistant.view.RemindDialog;
import com.lingju.model.AlarmClock;
import com.lingju.model.Remind;
import com.lingju.model.SimpleDate;
import com.lingju.model.dao.AssistDao;
import com.lingju.util.TimeUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RemindService extends Service {
    private final static String TAG = "RemindService";
    private AssistDao dao;

    public final static int NotificationId = 45;
    public final static String REMIND_ACTION = "com.lingju.assistant.service.RemindService.Remind";
    public final static String ALARM_ACTION = "com.lingju.assistant.service.RemindService.Alarm";
    public final static String CMD = "cmd";
    public final static String ID = "id";
    public final static String IDS = "ids";

    public final static int ADD = 0x0;
    public final static int UPDATE = 0x1;
    public final static int CANCEL = 0x2;

    public final static int REMIND = 0x1;
    public final static int REMIND_TRIGGER = 0x2;
    public final static int ALARM = 0x3;
    public final static int ALARM_TRIGGER = 0x4;

    public final long DayTime = 86400000L;
    public final long WeekTime = 604800000L;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");
        dao = AssistDao.getInstance();
        startService(new Intent(this, AssistantService.class));
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        if (intent != null) {
            int t = intent.getIntExtra(CMD, -1);
            Long id = (Long) intent.getSerializableExtra(ID);
            if (id == null)
                id = 0L;
            Log.e(TAG, "onStartCommand,CMD=" + t);
            if (t >> 4 == REMIND) {        //设置提醒
                List<Remind> list = new ArrayList<>();
                if (id == 0) {
                    ArrayList<Integer> ids = intent.getIntegerArrayListExtra(IDS);
                    for (int i : ids) {
                        list.add(dao.findRemindById(i));
                    }
                } else {
                    list.add(dao.findRemindById(id));
                }
                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                SimpleDate sd = new SimpleDate();
                switch (t & 0x0f) {
                    case ADD:
                        for (Remind r : list) {
                            if (r.getValid() == 1) {
                                Intent rIntent = new Intent(REMIND_ACTION);
                                rIntent.putExtra(CMD, REMIND_TRIGGER);
                                rIntent.putExtra(ID, r.getId());
                                PendingIntent pi = PendingIntent.getService(this, r.getId().intValue(), rIntent, 0);
                                am.cancel(pi);
                                addRemindAlarm(Calendar.getInstance(), am, r, sd, pi);
                            }

                        }
                        break;
                    case CANCEL:
                        if (id > 0) {
                            Intent rIntent = new Intent(REMIND_ACTION);
                            rIntent.putExtra(CMD, REMIND_TRIGGER);
                            rIntent.putExtra(ID, id);
                            am.cancel(PendingIntent.getService(this, id.intValue(), rIntent, 0));
                        }
                        break;
                }
            } else if (t == REMIND_TRIGGER) {        //提醒通知
                Remind remind = dao.findRemindById(id);
                if (remind != null && remind.getValid() == 1) {
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(RemindService.this)
                                    .setAutoCancel(true)
                                    .setSmallIcon(R.drawable.ic_launcher)
                                    .setContentTitle(getResources().getString(R.string.app_name))
                                    .setContentText("您有一条新的提醒通知！");
                    Intent resultIntent = new Intent(RemindService.this, RemindActivity.class);
                    resultIntent.putExtra(ID, id);
                    resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    PendingIntent resultPendingIntent = PendingIntent.getActivity(RemindService.this, 0, resultIntent, PendingIntent.FLAG_ONE_SHOT);
                    mBuilder.setContentIntent(resultPendingIntent);
                    Notification nf = mBuilder.build();
                    NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    nm.notify(NotificationId, nf);
                    Intent mIntent = new Intent(this, AssistantService.class);
                    mIntent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.SPEAK_REMIND_TEXT);
                    startService(mIntent);
                    Intent dIntent = new Intent(RemindDialog.class.getName());
                    dIntent.addCategory(Intent.CATEGORY_DEFAULT);
                    dIntent.putExtra(ID, id);
                    dIntent.putExtra("text", remind.getContent());
                    dIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(dIntent);
                }
            } else if (t >> 4 == ALARM) {
                List<AlarmClock> list = new ArrayList<>();
                if (id == 0) {
                    ArrayList<Integer> ids = intent.getIntegerArrayListExtra(IDS);
                    for (int i : ids) {
                        list.add(dao.findAlarmById(i));
                    }
                } else {
                    list.add(dao.findAlarmById(id));
                }
                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                SimpleDate sd = new SimpleDate();

                switch (t & 0x0f) {
                    case ADD:
                        for (AlarmClock a : list) {
                            if (a != null && a.getValid() == 1) {
                                Intent rIntent = new Intent(ALARM_ACTION);
                                rIntent.putExtra(CMD, ALARM_TRIGGER);
                                rIntent.putExtra(ID, a.getId());
                                PendingIntent pi = PendingIntent.getService(this, a.getId().intValue(), rIntent, 0);
                                am.cancel(pi);
                                addAlarm(Calendar.getInstance(), am, a, sd, pi);
                            }
                        }
                        break;
                    case CANCEL:
                        if (id > 0) {
                            Intent rIntent = new Intent(ALARM_ACTION);
                            rIntent.putExtra(CMD, ALARM_TRIGGER);
                            rIntent.putExtra(ID, id);
                            am.cancel(PendingIntent.getService(this, id.intValue(), rIntent, 0));
                        }
                        break;
                }
            } else if (t == ALARM_TRIGGER) {
                if (id > 0) {
                    AlarmClock ac = dao.findAlarmById(id);
                    if (ac != null) {
                        if (ac.getValid() == 1) {
                            boolean legal = ac.getFrequency() == 0;
                            if (!legal) {
                                Calendar cl = Calendar.getInstance();
                                int d = cl.get(Calendar.DAY_OF_WEEK);
                            /* 校正星期几（1-7 -->星期一到星期日） */
                                d = d == 1 ? 7 : d - 1;
                                if (Integer.toOctalString(ac.getFrequency()).indexOf(Integer.toString(d)) != -1) {
                                    legal = true;
                                }
                            }
                            if (legal) {
                                Intent resultIntent = new Intent(RemindService.this, AlarmRingingActivity.class);
                                resultIntent.putExtra(ID, id);
                                resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(resultIntent);
                            }
                        }
                    }
                }
            }
        }
        return super.onStartCommand(intent, START_STICKY, startId);
    }

    /**
     * 添加提醒闹铃
     **/
    private void addRemindAlarm(Calendar cl, AlarmManager am, Remind r, SimpleDate sd, PendingIntent pi) {
        cl.setTime(r.getRdate());
        sd.setValue(new SimpleDate(r.getRtime()).toValue());
        cl.set(Calendar.HOUR_OF_DAY, sd.getHour());
        cl.set(Calendar.MINUTE, sd.getMinute());
        if (r.getFrequency() == 0) {
            if (cl.getTimeInMillis() >= System.currentTimeMillis()) {
                Log.i(TAG, "addRemindAlarm fr=0,>>" + TimeUtils.time2String(cl.getTime()));
                setRemind(am, cl, pi);
            }
        } else if (r.getFrequency() == 8) {
            while (cl.getTimeInMillis() < System.currentTimeMillis()) {
                cl.setTimeInMillis(cl.getTimeInMillis() + DayTime);
                //				cl.setTimeInMillis(cl.getTimeInMillis()+1000*120);
            }
            Log.i(TAG, "addRemindAlarm fr=8,>>" + TimeUtils.time2String(cl.getTime()));
            setRemind(am, cl, pi);
        }
        if (r.getFrequency() > 0 && r.getFrequency() < 8) {
            while (cl.get(Calendar.DAY_OF_WEEK) != (r.getFrequency() % 7 + 1)) {
                cl.setTimeInMillis(cl.getTimeInMillis() + DayTime);
            }
            // SimpleDate now = new SimpleDate();
            long current = System.currentTimeMillis();
            while (/*sd.lt(now) &&*/ cl.getTimeInMillis() < current) {
                cl.setTimeInMillis(cl.getTimeInMillis() + 7 * DayTime);
            }
            Log.i(TAG, "addRemindAlarm fr=1,7,>>" + TimeUtils.time2String(cl.getTime()));
            setRemind(am, cl, pi);
        } else if (r.getFrequency() == 9) {
            Calendar now = Calendar.getInstance();
            now.set(Calendar.HOUR_OF_DAY, sd.getHour());
            now.set(Calendar.MINUTE, sd.getMinute());
            while (now.get(Calendar.DAY_OF_MONTH) != cl.get(Calendar.DAY_OF_MONTH)) {
                now.setTimeInMillis(now.getTimeInMillis() + DayTime);
            }
            SimpleDate nowd = new SimpleDate();
            if (sd.lt(nowd)) {
                return;
            }
            Log.i(TAG, "addRemindAlarm fr=9,>>" + TimeUtils.time2String(now.getTime()));
            am.set(AlarmManager.RTC_WAKEUP, now.getTimeInMillis(), pi);
        } else if (r.getFrequency() == 10) {
            Calendar now = Calendar.getInstance();
            now.set(Calendar.HOUR_OF_DAY, sd.getHour());
            now.set(Calendar.MINUTE, sd.getMinute());
            int i = 0;
            while (cl.get(Calendar.MONTH) != now.get(Calendar.MONTH) || cl.get(Calendar.DAY_OF_MONTH) != now.get(Calendar.DAY_OF_MONTH)) {
                now.setTimeInMillis(now.getTimeInMillis() + DayTime);
                i++;
                if (i > 31) {//如果提醒日期距离本日的时间超过31天，则忽略这条提醒
                    return;
                }
            }
            SimpleDate nowd = new SimpleDate();
            if (sd.lt(nowd)) {
                return;
            }
            Log.i(TAG, "addRemindAlarm fr=10,>>" + TimeUtils.time2String(now.getTime()));
            //am.set(AlarmManager.RTC_WAKEUP, now.getTimeInMillis(), pi);
            setRemind(am, now, pi);
        }
    }

    private void setRemind(AlarmManager am, Calendar now, PendingIntent pi) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            am.set(AlarmManager.RTC_WAKEUP, now.getTimeInMillis(), pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, now.getTimeInMillis(), pi);
        }
    }


    private void addAlarm(Calendar cl, AlarmManager am, final AlarmClock a, SimpleDate sd, PendingIntent pi) {
        sd.setValue(a.getRtime());
        Calendar now = Calendar.getInstance();
        if (a.getFrequency() == 0) {
            Log.i(TAG, "addAlarm >>" + sd.toString());
            cl.setTime(a.getRdate());
            cl.set(Calendar.HOUR_OF_DAY, sd.getHour());
            cl.set(Calendar.MINUTE, sd.getMinute());
            Log.i(TAG, "时差：" + (cl.getTimeInMillis() - now.getTimeInMillis()));
            if (cl.getTimeInMillis() < now.getTimeInMillis()) {
                cl.setTimeInMillis(cl.getTimeInMillis() + DayTime);
            }
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
                am.set(AlarmManager.RTC_WAKEUP, cl.getTimeInMillis(), pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, cl.getTimeInMillis(), pi);
            }
        } else if (a.getFrequency() > 0) {
            cl.set(Calendar.HOUR_OF_DAY, sd.getHour());
            cl.set(Calendar.MINUTE, sd.getMinute());
            if (cl.getTimeInMillis() < now.getTimeInMillis()) {
                cl.setTimeInMillis(cl.getTimeInMillis() + DayTime);
            }
            String frs = Integer.toOctalString(a.getFrequency());
            Log.i(TAG, "setAlarm but not add fr=" + frs + ",>>" + TimeUtils.time2String(cl.getTime()));
            int week = cl.get(Calendar.DAY_OF_WEEK);
            week = week - 1 == 0 ? 7 : week - 1;
            if (!frs.contains(String.valueOf(week)))
                return;
            Log.i(TAG, "addAlarm fr=" + frs + ",>>" + TimeUtils.time2String(cl.getTime()));
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
                am.setRepeating(AlarmManager.RTC_WAKEUP, cl.getTimeInMillis(), DayTime, pi);
            } else {
                //AlarmManager.RTC_WAKEUP 在魅族、小米等定制系统，系统休眠时，如果周期小于5分钟不会唤醒触发任务(可能)
                am.setExact(AlarmManager.RTC_WAKEUP, cl.getTimeInMillis(), pi);
               /* Single.timer(cl.getTimeInMillis(), TimeUnit.MILLISECONDS)
                        .doOnSuccess(new Consumer<Long>() {
                            @Override
                            public void accept(Long aLong) throws Exception {
                                Log.i("LingJu", "RemindService accept()>>>到点啦！！");
                                Intent rIntent = new Intent(ALARM_ACTION);
                                rIntent.putExtra(CMD, ALARM_TRIGGER);
                                rIntent.putExtra(ID, a.getId());
                                startService(rIntent);
                            }
                        })
                        .subscribe();*/
            }
        }
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy");
    }

}
