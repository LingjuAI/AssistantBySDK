package com.lingju.assistant.activity.event;


import com.lingju.context.entity.Command;

/**
 * Created by Ken on 2017/navi/3.
 */
public class CallAndSmsEvent {

    private String text;
    private Command cmd;
    private int type;

    public CallAndSmsEvent(Command cmd, String text, int type) {
        this.cmd = cmd;
        this.text = text;
        this.type = type;
    }

    public Command getCmd() {
        return cmd;
    }

    public String getText() {
        return text;
    }

    public int getType() {
        return type;
    }
}
