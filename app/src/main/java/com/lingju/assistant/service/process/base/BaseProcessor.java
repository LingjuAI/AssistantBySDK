package com.lingju.assistant.service.process.base;

import android.content.Context;

import com.lingju.assistant.service.AssistantService;
import com.lingju.audio.SystemVoiceMediator;
import com.lingju.audio.engine.IflySynthesizer;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.context.entity.Command;
import com.lingju.context.entity.base.IChatResult;
import com.lingju.model.SmsInfo;

/**
 * Created by Administrator on 2016/11/5.
 */
public abstract class BaseProcessor implements IProcessor {
    //线程变量，最新的IChatResult
    protected final static ThreadLocal<IChatResult> THREAD_CHATRESULT = new ThreadLocal<>();

    protected SystemVoiceMediator voiceMediator;
    protected Context mContext;
    protected SynthesizerBase mSynthesizer;
    public static final int CMD_DEFAULT = 0;
    public static final int CMD_PLAY = 1;
    public static final int CMD_OPTIONS = 2;
    public static final int CMD_AWAKEN = 3;
    public static final int CMD_CALL = 4;
    public static final int CMD_ADDITION = 5;
    public static final int CMD_NAVI = 9;
    public static final int CMD_TING = 6;

    public BaseProcessor(Context mContext, SystemVoiceMediator mediator) {
        this.voiceMediator = mediator;
        this.mContext = mContext;
        this.mSynthesizer = IflySynthesizer.getInstance();
    }

    /**
     * 请注意，子类若要获取有效的值IChatResult，请务必重写本方法，
     * 子类须在handle方法中通过变量缓存线程变量
     * {@linkplain BaseProcessor#THREAD_CHATRESULT THREAD_CHATRESULT}中的{@linkplain IChatResult IChatResult}值
     * 以供本方法调用
     *
     * @return
     */
    @Override
    public IChatResult getCurrentChatResult() {
        return THREAD_CHATRESULT.get();
    }

    @Override
    public IProcessor bind2CurrentThread(IChatResult chatResult) {
        THREAD_CHATRESULT.set(chatResult);
        return this;
    }

    @Override
    public void handle(Command cmd, String text, int inputType) {
        /** 因为每一次语音识别都会关闭唤醒，所以在每一次语音回复之后（即肯定上一环节为语音识别）
         * 尝试打开唤醒（如果处于打开唤醒状态） **/
        if (inputType == AssistantService.INPUT_VOICE) {
            voiceMediator.tryToWakeup();
        }
    }

    @Override
    public void smsMsgHandle() {

    }

    @Override
    public void receiveSms(SmsInfo sms, StringBuilder number) {

    }

    @Override
    public void cancelTingTask(){

    }
}
