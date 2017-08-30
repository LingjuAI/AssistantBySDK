package com.lingju.assistant.activity.event;

public class SynthesizeEvent {

    /**
     * 开始合成
     **/
    public static final int SYNTH_START = 1;
    /**
     * 结束合成
     **/
    public static final int SYNTH_END = 0;

    private int state = -1;

    /**
     * 该构造方法供合成错误提示事件使用
     **/
    public SynthesizeEvent() {
    }

    public SynthesizeEvent(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

}
