package com.lingju.assistant.activity.index.presenter;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.lingju.assistant.activity.AlarmRingingActivity;
import com.lingju.assistant.activity.index.IAlarmRinging;
import com.lingju.assistant.service.AssistantService;
import com.lingju.assistant.service.RemindService;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.model.AlarmClock;
import com.lingju.model.SimpleDate;
import com.lingju.model.dao.AssistDao;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ken on 2016/12/2.
 */
public class AlarmRingPresenter implements IAlarmRinging.IPresenter {

    private Context mContext;
    private AssistDao mAssistDao;
    private IAlarmRinging.AlarmRingingView mRingingView;
    private AlarmClock mAlarm;
    private MediaPlayer player = new MediaPlayer();
    private Timer timer;
    private boolean isDelay;

    public AlarmRingPresenter(IAlarmRinging.AlarmRingingView ringingView) {
        this.mRingingView = ringingView;
        this.mContext = (Context) ringingView;
        mAssistDao = AssistDao.getInstance();
    }

    @Override
    public void goRing(Intent intent) {
        if (intent != null) {
            long id = intent.getLongExtra(RemindService.ID, 0);
            if (id > 0) {
                mAlarm = mAssistDao.findAlarmById(id);
                Log.i("LingJu", "响铃啦：" + new SimpleDate(mAlarm.getRtime()).toString());
            }
        }
        if (mAlarm != null && mAlarm.getValid() == 1) {
            mRingingView.setRingRime(new SimpleDate(mAlarm.getRtime()).toString());
            if (TextUtils.isEmpty(mAlarm.getPath())) {
                ContentResolver cr = ((AlarmRingingActivity) mRingingView).getContentResolver();
                String path = null;
                if (cr != null) {
                    Cursor cursor = cr.query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, new String[]{
                                    MediaStore.Audio.Media._ID,
                                    MediaStore.Audio.Media.TITLE,
                                    MediaStore.Audio.Media.DATA,
                                    MediaStore.Audio.Media.DURATION,
                                    MediaStore.Audio.Media.SIZE
                            },
                            MediaStore.Audio.Media.DURATION + ">?",
                            new String[]{"10000"}, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
                    if (null != cursor) {
                        try {
                            if (cursor.moveToFirst()) {
                                path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                            }
                        } finally {
                            cursor.close();
                        }
                    }
                }
                mAlarm.setPath(path);
            }

            if (!TextUtils.isEmpty(mAlarm.getPath())) {
                startRingTask();
            }
        }
    }

    private void startRingTask() {
        play(mAlarm.getPath());
        if (timer == null)
            timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                mRingingView.destoryView();
            }
        }, 180000);
    }

    private void play(String path) {
        try {
            player.reset();
            player.setDataSource(path);
            player.setLooping(true);
            player.prepare();
            player.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void stopRing() {
        if (isDelay) {
            /*mAlarm.setRtime(new SimpleDate().toValue() + 5);
            mAssistDao.updateAlarm(mAlarm);
            //开启闹钟服务
            Intent rIntent = new Intent(mContext, RemindService.class);
            rIntent.putExtra(RemindService.CMD, (RemindService.ALARM << 4) + RemindService.ADD);
            rIntent.putExtra(RemindService.ID, mAlarm.getId());
            mContext.startService(rIntent);*/
            Intent delayIntent = new Intent(mContext, AssistantService.class);
            delayIntent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.DELAY_ALARM);
            delayIntent.putExtra(RemindService.ID, mAlarm.getId());
            mContext.startService(delayIntent);

            //合成提示语音
            StringBuilder builder = new StringBuilder();
            SimpleDate sd = new SimpleDate();
            sd.setValue(sd.toValue() + 5);
            builder.append(sd.toString())
                    .append("将再次响铃");
            SynthesizerBase.get().startSpeakAbsolute(builder.toString())
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .subscribe();
        } else if (mAlarm.getFrequency() == 0) {
            mAlarm.setValid(0);
            mAssistDao.updateAlarm(mAlarm);

        }
        player.stop();
        player.release();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void delayRing() {
        isDelay = true;
        mRingingView.destoryView();
    }
}
