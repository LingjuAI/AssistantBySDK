package com.lingju.assistant.activity.event;

/**
 * Created by Administrator on 2015/8/7.
 */
public class NavigateEvent {

    public final static int START_NAVI=0;
    public final static int PAUSE_NAVI=1;
    public final static int RESUME_NAVI=2;
    public final static int STOP_NAVI=3;
    public final static int NAVI_SHOW_FULL_LINE=4;
    public final static int RESUME_TO_START_COUNTDOWN=5;
    public final static int STOP_COUNTDOWN=6;
    public final static int STOP_NAVI_BACKGROUND=7;
    public final static int SHOW_NAVI_GUIDE=8;
    public final static int START_AWAKEN=9;



    private int type;

    public NavigateEvent(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
