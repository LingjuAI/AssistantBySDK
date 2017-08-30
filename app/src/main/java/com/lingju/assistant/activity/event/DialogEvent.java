package com.lingju.assistant.activity.event;

/**
 * Created by Ken on 2017/6/20.
 */
public class DialogEvent {
    public final static int SHOW_TYPE=0;
    public final static int CANCEL_NORMAL_TYPE=1;
    public final static int CANCEL_TOGGLE_TYPE=2;
    public final static int SHOW_WALK_TYPE=3;
    public final static int CANCEL_WALK_TYPE=4;
    private int eventType = -1;

    public DialogEvent(int eventType) {
        this.eventType = eventType;
    }

    public int getEventType() {
        return eventType;
    }
}
