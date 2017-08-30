package com.lingju.assistant.activity.event;

/**
 * Created by Administrator on 2015/8/12.
 * 在导航页面切换规划路线
 */
public class NaviSwitchRouteLineEvent {

    private int prefrence;
    private boolean startImmediately=true;

    public NaviSwitchRouteLineEvent(int prefrence) {
        this.prefrence = prefrence;
    }
    public NaviSwitchRouteLineEvent(int prefrence,boolean startImmediately) {
        this.prefrence = prefrence;
        this.startImmediately=startImmediately;
    }

    public int getPrefrence() {
        return prefrence;
    }

    public void setPrefrence(int prefrence) {
        this.prefrence = prefrence;
    }

    public boolean isStartImmediately() {
        return startImmediately;
    }
}
