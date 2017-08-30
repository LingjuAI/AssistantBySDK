package com.lingju.audio.engine.base;

import android.content.Context;
import android.util.Log;

import com.iflytek.cloud.SpeechSynthesizer;
import com.lingju.audio.PcmPlayer;
import com.lingju.audio.SystemVoiceMediator;
import com.lingju.util.NetUtil;

import java.util.Observable;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;


public abstract class SynthesizerBase extends Observable {

    private final static String TAG = "SynthesizerBase";
    public final static String INTERRUPTED_LONG_TIPS[] = new String[]{"朗读已被导航打断，继续导航",
            "抱歉，刚才的回答被导航提示打断了",
            "抱歉，为了播报导航提示，打断了刚才的朗读"};
    public final static String INTERRUPTED_SHORT_TIPS[] = new String[]{"刚才被打断了，我再重说一遍。",
            "又被打断了，我再重说一遍。",
            "我再重说一遍。"};

    protected static boolean inited;
    protected static SynthesizerBase instance;
    /**
     * 等待合成消息队列
     **/
    protected Queue<SpeechMsg> messages = new ConcurrentLinkedQueue<SpeechMsg>();
    /**
     * 被打断合成的消息队列
     **/
    protected Queue<SpeechMsg> interruptedMessages = new ConcurrentLinkedQueue<SpeechMsg>();
    protected SpeechMsg currentMessage;
    protected SpeechMsg lastMessage;
    protected Context mContext;
    protected SystemVoiceMediator mediator;
    protected PcmPlayer pcmPlayer;
    public static String initWelcome;
    private Lock sLock = new ReentrantLock();

    public static boolean isInited() {
        return inited;
    }

    public static SynthesizerBase get() {
        return instance;
    }

    public String getCurrentMsg() {
        return currentMessage != null ? currentMessage.text() : null;
    }

    public String getLastMessage() {
        return lastMessage.text();
    }

    public SpeechMsg getCurrentMessage() {
        return currentMessage;
    }

    public int getCurrentOrigin() {
        return currentMessage != null ? currentMessage.origin() : 0;
    }

    /**
     * 判断当前合成引擎是否处于朗读状态
     *
     * @return true=是
     */
    public abstract boolean isSpeaking();

    public void requestAudioFocus() {
    }

    public void abandonAudioFocus() {
    }

    public void addMessageWaitSpeak(String msg) {
        addMessageWaitSpeak(new SpeechMsgBuilder(msg).build());
    }

    /**
     * 如果当前处于合成朗读阶段，信息添加到队列末尾排队等待合成朗读，如果处于静默状态，马上合成朗读
     *
     * @param msg
     */
    public io.reactivex.Observable<SpeechMsg> addMessageWaitSpeak(SpeechMsg msg) {
        Log.w(TAG, "addMessageWaitSpeak>>>>" + msg.text());
        sLock.lock();
        try {
            if (isSpeaking()) {
                Log.w(TAG, "addMessageWaitSpeak isSpeaking");
                messages.offer(msg);
            } else {
                return startSpeak(msg);
            }
        } finally {
            sLock.unlock();
        }
        return null;
    }

    public io.reactivex.Observable<SpeechMsg> startSpeakAbsolute(String msg) {
        return startSpeakAbsolute(new SpeechMsgBuilder(msg).build());
    }

    /**
     * 合成语音，等待合成的队列将被清空，合成中的语音将被打断，打断后马上开始合成
     *
     * @param msg
     * @return
     */
    public io.reactivex.Observable<SpeechMsg> startSpeakAbsolute(SpeechMsg msg) {
        Log.i(TAG, "startSpeakAbsolute>>>>>>>>" + msg.text());
        sLock.lock();
        int r = 0;
        try {
            if (isSpeaking()) {
                Log.e(TAG, "startSpeakAbsolute isSpeaking........");
                if (mediator.compareSpeechMsg(msg, currentMessage)) {       //比较合成文本优先级（导航最优）
                    stopSpeakingAbsolte(msg);    //准备合成的文本为导航相关，停止当前合成，优先播放
                } else {
                    Log.e(TAG, "startSpeakAbsolute>>addMessageWaitSpeak");
                    messages.offer(msg); //准备合成的文本为普通文本，放入合成队列中等待，当前合成完成后在合成
                    return io.reactivex.Observable.empty();
                }
            }
            return startSpeak(msg);
        } finally {
            sLock.unlock();
        }
    }

    protected io.reactivex.Observable<SpeechMsg> createEmptySuccessCallback(SpeechMsg msg) {
        msg.setState(SpeechMsg.State.Completed);
        return io.reactivex.Observable.create(new ObservableOnSubscribe<SpeechMsg>() {
            @Override
            public void subscribe(ObservableEmitter<SpeechMsg> e) throws Exception {
                e.onComplete();
            }
        });
    }

    /**
     * 调用本方法时务必保证当前没有处于合成状态
     *
     * @param msg
     * @return
     */
    protected io.reactivex.Observable<SpeechMsg> startSpeak(SpeechMsg msg) {
        Log.w(TAG, "startSpeak text:" + msg.text());
        if (mContext != null) {
            if (!mediator.allowSynthersize(msg)) {//TODO 此处判断需挪到前面
                Log.e(TAG, "msg.priority can not be synthesize!");
                return io.reactivex.Observable.empty();
            }
        }
        currentMessage = msg;
        if (msg.text().length() == 0) {
            Log.e(TAG, "msg.length==0");
            return createEmptySuccessCallback(msg);
        }
        if (mediator.isBlueToothHeadSet()) {
            Log.e(TAG, "setAudioManager mode to MODE_IN_CALL");
            mediator.startBluetoothSco();
        }
        if (msg.forceLocalEngine()) {
            //msg.forceLocalEngine=true;
        } else {
            //forceSwitchEngine(RobotApplication.online);
        }
        return synthesize(msg);
    }

    protected abstract io.reactivex.Observable<SpeechMsg> synthesize(SpeechMsg msg);

    public abstract void destory();

    /**
     * 停止合成语音，回调监听依然生效
     */
    public void stopSpeaking() {
        Log.e(TAG, "stopSpeaking");
        stop();
        /*lastMessage=currentMessage;
        synthesizerListener.onCompleted(null);*/
        mediator.stopBluetoothSco();
    }

    public abstract void pauseSpeaking();

    public abstract void resumeSpeaking();

    public void stopSpeakingAbsolte() {
        stopSpeakingAbsolte(null);
    }

    /**
     * 取消合成语音，不管当前合成进行到何处，回调监听基本不会生效，极少部分特殊的除外
     *
     * @param newMsg 即将合成的文本，在此用于比较正在播报的文本是否需要放置到被打断队列中，而不是说取消该文本的合成。
     */
    private void stopSpeakingAbsolte(SpeechMsg newMsg) {
        Log.e(TAG, "stopSpeakingAbsolte");
        if (!isSpeaking() || currentMessage == null)
            return;
        messages.clear();
        if (currentMessage.retryTimes() > 0) {
            currentMessage.setInvalid(true);
            interruptedMessages.offer(currentMessage.decreaseRetryTimes().clone());
        } else if (newMsg != null) {
            currentMessage.setInvalid(true);
            lastMessage = currentMessage;
            if (newMsg.origin() == SpeechMsg.ORIGIN_NAVI && lastMessage.origin() == SpeechMsg.ORIGIN_COMMON) {
                Random random = new Random();
                interruptedMessages.add(
                        new SpeechMsgBuilder(
                                lastMessage.text().length() > 100
                                        ? INTERRUPTED_LONG_TIPS[random.nextInt(INTERRUPTED_LONG_TIPS.length)]
                                        : INTERRUPTED_SHORT_TIPS[random.nextInt(INTERRUPTED_SHORT_TIPS.length)] + lastMessage.text()
                        ).setOrigin(SpeechMsg.ORIGIN_FINAL)
                                .build());
            }
        }

        stop();
        mediator.stopBluetoothSco();
        /*if(messageWithCallBack!=null&&!messageWithCallBack.isEndWhenInterrupted()){
            messageWithCallBack.onComplete();
		}*/
    }

    public void clearInterruptedMessages() {
        /*if(currentMessage!=null){
            currentMessage.repeatCntInterrupted=0;
		}*/
        interruptedMessages.clear();
    }

    protected abstract void stop();

    public abstract SpeechSynthesizer getSpeechEngine();

    public abstract void resetParam();

    public abstract void resetParam(NetUtil.NetType type);

    public abstract void setRdnString(boolean flag);

    protected abstract void forceSwitchEngine(boolean online);

    public abstract boolean isForceLocalEngine();

    public abstract void setForceLocalEngine(boolean flag);

    public abstract void reSpeak();

    public abstract boolean isLocalEngine();

    public void reSpeakLastSpeech() {
        if (lastMessage != null)
            startSpeakAbsolute(lastMessage);
    }

    protected void keepVoiceCtrl(SpeechMsg currentMsg) {
        // TODO Auto-generated method stub
        Log.i(TAG, "keepVoiceCtrl>>>" + Thread.currentThread().getName());
        abandonAudioFocus();
        if (mediator.mobileRing())
            return;
        SpeechMsg msg = null;
        mediator.stopBluetoothSco();
        lastMessage = currentMsg;

        if ((msg = interruptedMessages.poll()) != null || (msg = messages.poll()) != null) {
            startSpeakAbsolute(msg).subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .subscribe();
        } else {
            mediator.keepVoiceCtrl(currentMsg);
        }
    }


    public static class FailedException extends Exception {

        public FailedException() {
        }

        public FailedException(String detailMessage) {
            super(detailMessage);
        }

        public FailedException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public FailedException(Throwable throwable) {
            super(throwable);
        }
    }

}
