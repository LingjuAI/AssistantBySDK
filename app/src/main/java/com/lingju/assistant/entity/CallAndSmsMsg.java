package com.lingju.assistant.entity;

/**
 * Created by Ken on 2017/2/28.
 */
public class CallAndSmsMsg {

    private int type;
    private String[] contents;
    private boolean isCompleted;    //任务是否已完成标记
    private boolean firstTouchOnSmsEdit = true;    //第一次触摸短信编辑卡片

    public CallAndSmsMsg(String[] contents, int type) {
        this.contents = contents;
        this.type = type;
    }

    public String[] getContents() {
        return contents;
    }

    public void setContents(String[] contents) {
        this.contents = contents;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public boolean isFirstTouchOnSmsEdit() {
        return firstTouchOnSmsEdit;
    }

    public void setFirstTouchOnSmsEdit(boolean firstTouchOnSmsEdit) {
        this.firstTouchOnSmsEdit = firstTouchOnSmsEdit;
    }
}
