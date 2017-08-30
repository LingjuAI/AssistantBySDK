package com.lingju.assistant.view;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.event.IntroduceShowEvent;
import com.lingju.assistant.activity.event.RecordUpdateEvent;
import com.lingju.assistant.service.AssistantService;
import com.lingju.assistant.view.wave.WaveView;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.common.log.Log;

import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Ken on 2017/3/14.
 */
public class VoiceWaveComponent extends RelativeLayout {

    private static final String TAG = "VoiceWaveComponent";
    @BindView(R.id.rl_mic)
    RelativeLayout mRlMic;
    @BindView(R.id.wv_record)
    WaveView mWvRecord;
    @BindView(R.id.iv_mic_default)
    AppCompatImageView mIvMicDefalut;
    @BindView(R.id.iv_circle_wait)
    AppCompatImageView mIvCirCleWait;
    private Context mContext;
    private boolean recording;
    private OnTouchListener onTouchListener;
    private int current_state = -1;
    private int baseAmplitude;
    private Animation mWaitAnim;

    public VoiceWaveComponent(Context context) {
        this(context, null);
    }

    public VoiceWaveComponent(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        init();
    }

    private void init() {
        LayoutInflater.from(mContext).inflate(R.layout.mic_wave_input, this);
        ButterKnife.bind(this);
        mRlMic.setOnTouchListener(defaultTounchListener);
        mWvRecord.setOnTouchListener(defaultTounchListener);
        mWaitAnim = AnimationUtils.loadAnimation(mContext, R.anim.mic_listening_loading);
        setVoiceButton(0);
    }

    private OnTouchListener defaultTounchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (onTouchListener != null) {
                onTouchListener.onTouch(v, event);
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Log.i("log", "action_down");
                onRecord();
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                Log.i(TAG, "action_up");
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                Log.i(TAG, "action_move");
                return true;
            }
            return false;
        }
    };

    /**
     * 设置录音波纹振幅
     **/
    public void setWaveAmplitude(int amplitude) {
        if (amplitude == mWvRecord.getAmplitude()) {
            return;
        }
        if (amplitude == 0) {   //获取一次录音波纹的有效高度
            baseAmplitude = mWvRecord.getAmplitude();
            return;
        }
        mWvRecord.setAmplitude(amplitude);
    }

    public int getBaseAmplitude() {
        return baseAmplitude;
    }

    public void onRecord() {
        EventBus.getDefault().post(new IntroduceShowEvent(false));
        if (recording) {
            stopRecord();
        } else {
            stopWakeup();
            prepareToRecord();
        }
    }

    /**
     * 识别刚结束的状态
     */
    public void setRecognizeCompletedState() {
        // recording = false;
        setVoiceButton(2);
    }

    /**
     * 录音开始状态
     */
    public void setRecordStartState() {
        recording = true;
        setVoiceButton(1);
    }

    /**
     * 录音进入闲置状态
     */
    public void setRecordIdleState() {
        recording = false;
        /*if (mWvRecord.getVisibility() == VISIBLE) {
            recordEndAnim();
        }*/
        setVoiceButton(0);
    }

    /**
     * 停止录音
     */
    private void stopRecord() {
        EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECORD_IDLE));
        stopRecognize();
    }

    private void stopWakeup() {
        Intent intent = new Intent(mContext, AssistantService.class);
        intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_WAKEUP_LISTEN);
        mContext.startService(intent);
    }

    private void stopRecognize() {
        Intent intent = new Intent(mContext, AssistantService.class);
        intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_RECOGNIZE);
        mContext.startService(intent);
    }

    private void startRecognize() {
        Intent intent = new Intent(mContext, AssistantService.class);
        intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.START_RECOGNIZE);
        mContext.startService(intent);
    }

    /**
     * 准备进入录音状态
     */
    private void prepareToRecord() {
        if (SynthesizerBase.isInited()) {
            //按下话筒开启录音动画标记，保证录音波纹正常显示
             AppConfig.dPreferences.edit().putBoolean("wave_show", true).commit();
            startRecognize();
            EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECORDING));
        }
    }

    private void setVoiceButton(int status) {
        Log.e(TAG, "setVoiceButton  .....STATUE:" + status);
        if (status == current_state)
            return;
        current_state = status;
        switch (status) {
            case 0:
                //隐藏波纹，显示话筒
                mRlMic.setVisibility(VISIBLE);
                mIvMicDefalut.setVisibility(VISIBLE);
                mIvCirCleWait.setVisibility(GONE);
                mIvCirCleWait.clearAnimation();
                mWvRecord.resetAmplitude();
                mWvRecord.setVisibility(GONE);
                mWvRecord.surfaceDestroyed(mWvRecord.getHolder());
                break;
            case 1:
                // recordStartAnim();
                mWvRecord.setVisibility(VISIBLE);
                mRlMic.setVisibility(GONE);
                mIvCirCleWait.setVisibility(GONE);
                mWvRecord.startWave();

                break;
            case 2:
                mRlMic.setVisibility(VISIBLE);
                mIvCirCleWait.setVisibility(VISIBLE);
                mIvCirCleWait.startAnimation(mWaitAnim);
                mIvMicDefalut.setVisibility(GONE);
                mWvRecord.setVisibility(GONE);
                mWvRecord.surfaceDestroyed(mWvRecord.getHolder());
                break;
        }
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        this.onTouchListener = l;
    }
}
