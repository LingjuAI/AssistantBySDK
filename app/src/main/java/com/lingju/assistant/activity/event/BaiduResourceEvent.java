package com.lingju.assistant.activity.event;

/**
 * Created by Administrator on 2015/12/11.
 */
public class BaiduResourceEvent {

    private int state;

    public BaiduResourceEvent(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
