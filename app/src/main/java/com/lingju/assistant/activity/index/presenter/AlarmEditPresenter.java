package com.lingju.assistant.activity.index.presenter;

import android.content.Intent;
import android.text.TextUtils;

import com.lingju.assistant.activity.AlarmEditActivity;
import com.lingju.assistant.activity.AlarmFrSettingActivity;
import com.lingju.assistant.activity.RingListActivity;
import com.lingju.assistant.activity.TimePickerActivity;
import com.lingju.assistant.activity.index.IAlarmEdit;
import com.lingju.assistant.service.RemindService;
import com.lingju.assistant.view.AlarmFrDialog;
import com.lingju.model.AlarmClock;
import com.lingju.model.SimpleDate;
import com.lingju.model.dao.AssistDao;
import com.lingju.util.AssistUtils;

import java.sql.Timestamp;

/**
 * Created by Ken on 2016/12/3.
 */
public class AlarmEditPresenter implements IAlarmEdit.IPresenter {

    private AlarmEditActivity mAlarmEditView;
    private AssistDao mAssistDao;
    private SimpleDate dt = new SimpleDate();
    private String path;
    private String ring = "默认";
    private int fr = 0;
    private AlarmClock alarm;
    private AlarmFrDialog alarmFrDialog;

    public AlarmEditPresenter(IAlarmEdit.AlarmEditView editView) {
        this.mAlarmEditView = (AlarmEditActivity) editView;
        mAssistDao = AssistDao.getInstance();
    }

    @Override
    public void initData(Intent intent) {
        if (intent != null) {
            long id = intent.getLongExtra(RemindService.ID, 0);
            if (id > 0) {
                alarm = mAssistDao.findAlarmById(id);
                dt = new SimpleDate(alarm.getRtime());
                path = alarm.getPath();
                ring = alarm.getRing();
                fr = alarm.getFrequency();
            }
        }
        mAlarmEditView.setFrequency(AssistUtils.transalteWeekDayString(fr));
        mAlarmEditView.setRing(ring);
        mAlarmEditView.setTime(dt.toString());
    }

    @Override
    public void setAlarmText(Intent intent, int resultCode) {
        if (intent != null) {
            switch (resultCode) {
                case TimePickerActivity.FOR_TIME_RESULT:
                    int time = intent.getIntExtra(TimePickerActivity.TIME, 0);
                    if (time > 0) {
                        dt.setValue(time);
                        mAlarmEditView.setTime(dt.toString());
                    }
                    break;
                case AlarmFrSettingActivity.FOR_FR:
                    fr = intent.getIntExtra(AlarmFrSettingActivity.TYPE, 0);
                    mAlarmEditView.setFrequency(AssistUtils.transalteWeekDayString(fr));
                    break;
                case RingListActivity.FOR_RING_SELECTE:
                    ring=intent.getStringExtra(RingListActivity.RING);
                    path=intent.getStringExtra(RingListActivity.URL);
                    mAlarmEditView.setRing(ring);
                    break;
            }
        }
    }

    @Override
    public void cancelEdit() {
        goBack();
    }

    @Override
    public void deleteAlarm() {
        if (alarm != null) {
            /* 1.向闹钟服务发送取消闹钟intent */
            Intent aIntent = new Intent(mAlarmEditView, RemindService.class);
            aIntent.putExtra(RemindService.CMD, (RemindService.ALARM << 4) + RemindService.CANCEL);
            aIntent.putExtra(RemindService.ID, alarm.getId());
            mAlarmEditView.startService(aIntent);
            /* 2.删除数据库中的闹钟记录 */
            mAssistDao.deleteAlarm(alarm);
            /* 3.回到闹钟列表页面 */
            mAlarmEditView.setResult(AlarmEditActivity.FOR_ALARM_EDIT);
        }
        goBack();
    }

    @Override
    public void saveAlarm() {
        /* 1.设置闹钟属性 */
        if (alarm == null) {
            alarm = new AlarmClock();
        }
        alarm.setCreated(new Timestamp(System.currentTimeMillis()));
        if (fr != 0) {
            alarm.setValid(1);
            alarm.setCreated(new Timestamp(System.currentTimeMillis()));
        }
        alarm.setRing(ring);
        if (!TextUtils.isEmpty(path)) {
            alarm.setPath(path);
        }
        alarm.setRtime(dt.toValue());
        alarm.setFrequency(fr);
        /* 2.保存闹钟记录 */
        if (alarm.getId() == null) {
            mAssistDao.insertAlarm(alarm);
            alarm = mAssistDao.findAlarmNewCreated();
        } else {
            mAssistDao.updateAlarm(alarm);
        }
        /* 3.向闹钟服务发送添加闹钟intent */
        Intent arIntent = new Intent(mAlarmEditView, RemindService.class);
        arIntent.putExtra(RemindService.CMD, (RemindService.ALARM << 4) + RemindService.ADD);
        arIntent.putExtra(RemindService.ID, alarm.getId());
        mAlarmEditView.startService(arIntent);
        /* 4.回到闹钟列表页面 */
        mAlarmEditView.setResult(AlarmEditActivity.FOR_ALARM_EDIT);
        goBack();
    }

    @Override
    public void toSetTime() {
        Intent tIntent = new Intent(mAlarmEditView, TimePickerActivity.class);
        tIntent.putExtra(TimePickerActivity.TITLE, "闹钟");
        tIntent.putExtra(TimePickerActivity.TIME, dt.toValue());
        mAlarmEditView.startActivityForResult(tIntent, TimePickerActivity.FOR_TIME_RESULT);
        goInto();
    }

    @Override
    public void toSetFr() {
//        Intent fIntent = new Intent(mAlarmEditView, AlarmFrSettingActivity.class);
//        fIntent.putExtra(AlarmFrSettingActivity.TYPE, fr);
//        mAlarmEditView.startActivityForResult(fIntent, AlarmFrSettingActivity.FOR_FR);
//        goInto();
        /*if (alarmFrDialog == null) {
            alarmFrDialog = new AlarmFrDialog(mAlarmEditView, fr, new AlarmFrDialog.OnResultListener() {
                @Override
                public void onResult(int fr) {
                    mAlarmEditView.setFrequency(AssistUtils.transalteWeekDayString(fr));
                }
            });
        }
        alarmFrDialog.show();*/
    }

    @Override
    public void toSetRing() {
        Intent rIntent = new Intent(mAlarmEditView, RingListActivity.class);
        rIntent.putExtra(RingListActivity.URL, path);
        rIntent.putExtra(RingListActivity.RING, ring);
        mAlarmEditView.startActivityForResult(rIntent, RingListActivity.FOR_RING_SELECTE);
        goInto();
    }

    private void goBack() {
        mAlarmEditView.goBack();
    }

    private void goInto() {
        mAlarmEditView.goInto();
    }
}
