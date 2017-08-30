package com.lingju.assistant.player.event;

/**
 * Created by Administrator on 2015/7/9.
 */
public class UpdatePlayListEvent {
    public final static int INITED=4;

    private int type;

    public UpdatePlayListEvent(int type){
        this.type=type;
    }

    public int getType() {
        return type;
    }
}
