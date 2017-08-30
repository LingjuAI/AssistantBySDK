package com.lingju.assistant.entity;

/**
 * Created by Ken on 2017/5/18.
 */
public class CmdAction {

    private int actionId;
    private String action;
    private int outc;

    public CmdAction(String action, int actionId, int outc) {
        this.action = action;
        this.actionId = actionId;
        this.outc = outc;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getActionId() {
        return actionId;
    }

    public void setActionId(int actionId) {
        this.actionId = actionId;
    }

    public int getOutc() {
        return outc;
    }

    public void setOutc(int outc) {
        this.outc = outc;
    }
}
