package com.lingju.audio.engine.base;

import org.json.JSONArray;

/**
 * Created by Administrator on 2016/11/3.
 */
public interface SpeechMsg extends Cloneable {

    /**
     * 如果当前正在识别，打断之
     */
    public final static int PRIORITY_ABOVE_RECOGNIZE=1;
    /**
     * 如果当前正在识别，丢弃本次合成
     */
    public final static int PRIORITY_BELOW_RECOGNIZE=0;

    public final static int ORIGIN_FINAL=-1;
    public final static int ORIGIN_DEFAULT=0;
    public final static int ORIGIN_COMMON=1;
    public final static int ORIGIN_NAVI=2;

    /**
     * 根据上下文自动判断
     */
    public final static int CONTEXT_AUTO=0;
    /**
     * 合成朗读完成后主动启动语音识别
     */
    public final static int CONTEXT_KEEP_RECOGNIZE=1;
    /**
     * 合成朗读完成后保持语音唤醒监听
     */
    public final static int CONTEXT_KEEP_AWAKEN=2;

    public enum State {
        Idle,OnBegin,Buffering,Speaking,OnPaused,OnResume,OnInterrupted,Error,Completed
    }

    public SpeechMsg clone();

    SpeechMsgBuilder getBuilder();

    public long id();

    public State state();

    public SpeechMsg setState(State state);

    public int priority();

    public int origin();

    public String text();

    public int errorCount();

    public int contextMode();

    public JSONArray sections();

    public int retryTimes();

    public boolean forceLocalEngine();

    public boolean invalid();

    public void setInvalid(boolean invalid);

    public SpeechMsg increaseError();

    public SpeechMsg decreaseRetryTimes();

    public int pausedTime();
}
