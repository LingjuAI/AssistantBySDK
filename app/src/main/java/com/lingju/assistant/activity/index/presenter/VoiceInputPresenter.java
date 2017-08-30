package com.lingju.assistant.activity.index.presenter;

import com.lingju.assistant.activity.index.IVoiceInput;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Administrator on 2016/11/4.
 */
public class VoiceInputPresenter implements IVoiceInput.VoicePresenter {

    private IVoiceInput.VoiceInputView voiceInputView;

    public VoiceInputPresenter(IVoiceInput.VoiceInputView voiceInputView){
        this.voiceInputView=voiceInputView;
        this.voiceInputView.setPresenter(this);
    }


    @Override
    public void startRecognize() {

    }

    @Override
    public void stopRecognize() {

    }

    @Override
    public void subscribe() {
        EventBus.getDefault().register(voiceInputView);
    }

    @Override
    public void unsubscribe() {
        EventBus.getDefault().unregister(voiceInputView);
    }

}
