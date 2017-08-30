package com.lingju.assistant.activity.event;

/**
 * Created by Administrator on 2015/9/17.
 */
public class CallTaskEvent {
    public final static int STATE_START=0;
    public final static int STATE_END=1;

    private int type;
    private int state;

    public CallTaskEvent(){
        this(0,STATE_START);
    }

    public CallTaskEvent(int state) {
        this(0,state);
    }

    public CallTaskEvent(int type,int state){
        this.type=type;
        this.state=state;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
