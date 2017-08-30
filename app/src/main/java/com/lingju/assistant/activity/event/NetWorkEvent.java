package com.lingju.assistant.activity.event;

/**
 * Created by Dyy on 2017/4/12.
 */
public class NetWorkEvent {
    public final static int NO_NETWORK = 0;
    int type ;

    public NetWorkEvent(int netWorkType) {
        type = netWorkType;
    }

    public int getType() {
        return type;
    }
    public void setType(int type) {
        this.type = type;
    }

}
