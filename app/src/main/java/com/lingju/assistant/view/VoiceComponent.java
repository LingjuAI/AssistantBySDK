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

import com.lingju.assistant.R;
import com.lingju.assistant.activity.event.IntroduceShowEvent;
import com.lingju.assistant.service.AssistantService;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.common.log.Log;

import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VoiceComponent extends RelativeLayout {
    private final static String TAG = "VoiceComponent";
    @BindView(R.id.iv_mic_default)
    AppCompatImageView mIvMicDefault;
    @BindView(R.id.iv_circle_record)
    AppCompatImageView mIvCircleRecord;
    @BindView(R.id.iv_circle_wait)
    AppCompatImageView mIvCircleWait;
    @BindView(R.id.voice_bt)
    RelativeLayout voiceBt;
    private Context context;
    private Animation micAn1 = null;
    private Animation micAn2 = null;
    private boolean recording;
    private OnTouchListener onTouchListener;
    private int current_state = -1;

    public VoiceComponent(Context context) {
        super(context);
    }

    public VoiceComponent(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VoiceComponent(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }


    private void init(Context context) {
        this.context = context;
        View contentView = LayoutInflater.from(context).inflate(R.layout.mic_common_input, this);
        ButterKnife.bind(this, contentView);

        micAn1 = AnimationUtils.loadAnimation(context, R.anim.mic_loading);
        micAn2 = AnimationUtils.loadAnimation(context, R.anim.mic_listening_loading);

        mIvMicDefault.setOnTouchListener(defaultTounchListener);
        mIvCircleWait.setOnTouchListener(defaultTounchListener);
        mIvCircleRecord.setOnTouchListener(defaultTounchListener);
        setVoiceButton(0);
    }
        public void setVoiceBtBackground(int id){
            if(id==0){
                voiceBt.setBackgroundDrawable(null);
            }
        }

    private OnTouchListener defaultTounchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Log.w(TAG, "voiceInputDefaultBT.setOnTouchListener event:" + event.describeContents());
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
        recording = false;
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
        setVoiceButton(0);
    }

    public void setHeadSetMode(boolean isHeadset) {
        mIvMicDefault.setImageLevel(isHeadset ? 1 : 0);
    }

    /**
     * 停止录音
     */
    private void stopRecord() {
        setRecordIdleState();
        stopRecognize();
    }

    private void stopWakeup() {
        Intent intent = new Intent(context, AssistantService.class);
        intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_WAKEUP_LISTEN);
        context.startService(intent);
    }

    private void stopRecognize() {
        Intent intent = new Intent(context, AssistantService.class);
        intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_RECOGNIZE);
        context.startService(intent);
    }

    private void startRecognize() {
        Intent intent = new Intent(context, AssistantService.class);
        intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.START_RECOGNIZE);
        context.startService(intent);
    }

    /**
     * 准备进入录音状态
     */
    private void prepareToRecord() {
        if (SynthesizerBase.isInited()) {
            /*Intent intent = new Intent(context, AssistantService.class);
            intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PAUSE_PLAY);
            context.startService(intent);*/
            startRecognize();
            setRecordStartState();
        }
    }


    private void setVoiceButton(int status) {
        Log.e(TAG, "setVoiceButton  .....STATUE:" + status);
        if (status == current_state)
            return;
        current_state = status;
        mIvCircleRecord.clearAnimation();
        mIvCircleWait.clearAnimation();
        switch (status) {
            case 0:
                mIvMicDefault.setVisibility(View.VISIBLE);
                mIvCircleRecord.setVisibility(View.GONE);
                mIvCircleWait.setVisibility(View.GONE);
                break;
            case 1:
                mIvMicDefault.setVisibility(View.GONE);
                mIvCircleWait.setVisibility(GONE);
                mIvCircleRecord.setVisibility(View.VISIBLE);
                mIvCircleRecord.startAnimation(micAn1);
                break;
            case 2:
                mIvMicDefault.setVisibility(View.GONE);
                mIvCircleRecord.setVisibility(View.GONE);
                mIvCircleWait.setVisibility(View.VISIBLE);
                mIvCircleWait.startAnimation(micAn2);
                break;
        }
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        this.onTouchListener = l;
        //super.setOnTouchListener(l);
    }


}
