package com.lingju.assistant.activity.event;

/**
 * Created by Ken on 2016/11/29.
 */
public class BottomBoxStateEvent {

    private boolean isShow;

    public BottomBoxStateEvent(boolean isShow) {
        this.isShow = isShow;
    }

    public boolean isShow() {
        return isShow;
    }
}
