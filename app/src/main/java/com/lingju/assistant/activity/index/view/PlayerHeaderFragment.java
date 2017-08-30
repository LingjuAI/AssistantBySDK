package com.lingju.assistant.activity.index.view;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.LevelListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.MainActivity;
import com.lingju.assistant.activity.event.ChatMsgEvent;
import com.lingju.assistant.player.audio.IBatchPlayer;
import com.lingju.assistant.player.audio.LingjuAudioPlayer;
import com.lingju.assistant.service.AssistantService;
import com.lingju.model.PlayMusic;
import com.lingju.common.log.Log;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2016/11/10.
 */
public class PlayerHeaderFragment extends Fragment implements IBatchPlayer.HeaderView {

    public final static int MSG_PLAY_STATE = 0;
    public final static int MSG_PLAY_MUSIC = 1;

    @BindView(R.id.music_name)
    TextView mMusicName;
    @BindView(R.id.music_author)
    TextView mMusicAuthor;
    @BindView(R.id.main_music_msg_box)
    LinearLayout mMainMusicMsgBox;
    @BindView(R.id.play_bt)
    ImageButton mPlayBt;
    @BindView(R.id.play_next_bt)
    ImageButton mPlayNextBt;
    @BindView(R.id.play_lyirc)
    ImageButton mPlayLyirc;
    @BindView(R.id.player_process)
    SeekBar mPlayerProcess;
    private IBatchPlayer.Presenter mPresenter;

    private PlayHeaderHandler mHandler;
    private boolean playerListOpened;
    private NotificationManager notificationManager;
    private RemoteViews remoteViews;
    private Notification notification;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_player_header, container, false);
        ButterKnife.bind(this, view);
        //针对文本跑马灯效果
        mMusicAuthor.setSelected(true);
        mMusicName.setSelected(true);
        mPlayBt.setImageLevel(1);
        mHandler = new PlayHeaderHandler(Looper.getMainLooper(), this);
        mPlayerProcess.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.e("PlayerHeaderFragment", "seekBar.onStopTrackingTouch>>" + seekBar.getProgress());
                seekBar.setProgress(seekBar.getProgress());
                mPresenter.resetPlayProgress(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
            }
        });
        return view;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initNotification(getActivity());
        if (mPresenter != null)
            mPresenter.subscribe();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mPresenter != null)
            mPresenter.unsubscribe();
    }


    @OnClick({R.id.main_music_msg_box, R.id.play_bt, R.id.play_next_bt, R.id.play_lyirc, R.id.menu})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.main_music_msg_box:
                ((MainActivity) getActivity()).setPlayerListPager(!playerListOpened);
                playerListOpened = !playerListOpened;
                break;
            case R.id.play_bt:
                Intent tIntent = new Intent(getActivity(), AssistantService.class);
                tIntent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.TOGGLE_PLAY);
                getActivity().startService(tIntent);
                break;
            case R.id.play_next_bt:
                Intent nIntent = new Intent(getActivity(), AssistantService.class);
                nIntent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.NEXT_MUSIC);
                getActivity().startService(nIntent);
                break;
            case R.id.play_lyirc:
                disappear();
                /*//停止声音合成
                if (IflySynthesizer.isInited())
                    IflySynthesizer.get().stopSpeaking();*/
                //停止播放歌曲
                Intent intent = new Intent(getActivity(), AssistantService.class);
                intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PAUSE_PLAY);
                getActivity().startService(intent);
                //添加系统提示语
                EventBus.getDefault().post(new ChatMsgEvent(null, null, "音乐已停止播放", null));
                break;
            case R.id.menu:
                ((MainActivity) getActivity()).openSlidingMenu();
                break;
        }
    }

    @Override
    public void setPlayState(boolean playing) {
        if (mHandler == null) {
            mHandler = new PlayHeaderHandler(Looper.getMainLooper(), this);
        }
        mHandler.obtainMessage(MSG_PLAY_STATE, playing).sendToTarget();
    }

    @Override
    public void updateSeekBar(int currentDuration, int duration) {
        if (mPlayerProcess != null)
            mPlayerProcess.setProgress(currentDuration * 100 / duration);
    }

    @Override
    public void showPlayMusic(PlayMusic playMusic) {
        if (mHandler == null) {
            mHandler = new PlayHeaderHandler(Looper.getMainLooper(), this);
        }
        mHandler.obtainMessage(MSG_PLAY_MUSIC, playMusic).sendToTarget();
    }

    @Override
    public void showPlayListPager() {
        ((MainActivity) getActivity()).setPlayerListPager(true);
    }

    @Override
    public void disappear() {
        mPresenter.setHeaderInitState(true);
        getActivity().getSupportFragmentManager().beginTransaction().hide(this).commit();
    }

    @Override
    public void initNotification(Context context) {
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.music_notification);
        Intent intent1 = new Intent(context.getApplicationContext(), AssistantService.class);
        intent1.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.TOGGLE_PLAY);
        PendingIntent pi1 = PendingIntent.getService(context.getApplicationContext(), AssistantService.ServiceCmd.TOGGLE_PLAY, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notice_switch, pi1);
        Intent intent2 = new Intent(context.getApplicationContext(), AssistantService.class);
        intent2.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.NEXT_MUSIC);
        PendingIntent pi2 = PendingIntent.getService(context.getApplicationContext(), AssistantService.ServiceCmd.NEXT_MUSIC, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notice_next, pi2);
        Intent intent3 = new Intent(context.getApplicationContext(), AssistantService.class);
        intent3.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.CLOSE_PLAY_NOTIFICATION);
        PendingIntent pi3 = PendingIntent.getService(context.getApplicationContext(), AssistantService.ServiceCmd.CLOSE_PLAY_NOTIFICATION, intent3, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notice_close, pi3);
        /* 点击通知栏进入主界面 */
        Intent intent4 = new Intent(context.getApplicationContext(), MainActivity.class);
        PendingIntent pi4 = PendingIntent.getActivity(context.getApplicationContext(), 0, intent4, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setAutoCancel(false);

        mBuilder.setSmallIcon(R.drawable.ic_launcher);
        mBuilder.setContent(remoteViews);
        notification = mBuilder.build();
        notification.contentIntent = pi4;
        // notification.flags = Notification.FLAG_NO_CLEAR; // 点击清除按钮时就会清除消息通知,但是点击通知栏的通知时不会消失
        notification.flags = Notification.FLAG_ONGOING_EVENT; // 点击清除按钮不会清除消息通知,可以用来表示在正在运行
    }

    @Override
    public void noitfiy(String title, String singer, Boolean isPlay) {
        if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(singer)) {
            remoteViews.setTextViewText(R.id.notice_title, title);
            remoteViews.setTextViewText(R.id.notice_singer, singer);
        }
        if (isPlay != null)
            remoteViews.setImageViewResource(R.id.notice_switch, isPlay ? R.drawable.pause : R.drawable.play);
        notificationManager.notify(LingjuAudioPlayer.NOTIFICATION_ID, notification);
    }

    @Override
    public void setPresenter(IBatchPlayer.Presenter presenter) {
        this.mPresenter = presenter;
    }


    /**
     * 静态内部类加弱应用防止内存泄漏
     **/
    static class PlayHeaderHandler extends Handler {
        WeakReference<PlayerHeaderFragment> phFragment;

        public PlayHeaderHandler(Looper looper, PlayerHeaderFragment fragment) {
            super(looper);
            phFragment = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            PlayerHeaderFragment headerFragment = phFragment.get();
            if (headerFragment == null) {
                return;
            }
            switch (msg.what) {
                case MSG_PLAY_STATE:
                    phFragment.get().updatePlayBar((boolean) msg.obj);
                    break;
                case MSG_PLAY_MUSIC:
                    phFragment.get().setPlayMusic((PlayMusic) msg.obj);
                    break;
            }
        }
    }

    private void setPlayMusic(PlayMusic playMusic) {
        mMusicName.setText(playMusic.getTitle());
        mMusicAuthor.setText(playMusic.getSinger());
    }

    private void updatePlayBar(boolean playing) {
        LevelListDrawable drawable = (LevelListDrawable) mPlayBt.getDrawable();
        drawable.setLevel(playing ? 0 : 1);
    }
}
