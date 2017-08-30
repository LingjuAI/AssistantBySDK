package com.lingju.assistant.activity.event;

import com.lingju.model.BaiduAddress;

import java.util.ArrayList;

/**
 * Created by Administrator on 2015/8/13.
 */
public class MapCmdEvent {

    public final static int ZOOM_IN=0;
    public final static int ZOOM_OUT=1;
    public final static int SHOW_TRAFFIC=2;
    public final static int ZOOM=3;

    private int cmd;
    private ArrayList<BaiduAddress> addresses;
    private int value;

    public MapCmdEvent(int cmd) {
        this.cmd = cmd;
    }

    public int getValue() {
        return value;
    }

    public MapCmdEvent setValue(int value) {
        this.value = value;
        return this;
    }

    public MapCmdEvent(int cmd,ArrayList<BaiduAddress> list) {
        this.cmd = cmd;
        this.addresses=list;

    }

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public ArrayList<BaiduAddress> getAddresses() {
        return addresses;
    }

    public void setAddresses(ArrayList<BaiduAddress> addresses) {
        this.addresses = addresses;
    }
}
