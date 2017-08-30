package com.lingju.assistant.entity;

/**
 * Created by Ken on 2016/12/14.
 * <p/>
 * 是否需要操作对象
 */
public class Operation {
    private String no;
    private String yes;
    private String text;

    public Operation(String no, String yes, String text) {
        this.no = no;
        this.yes = yes;
        this.text = text;
    }

    public String getNo() {
        return no;
    }

    public void setNo(String no) {
        this.no = no;
    }

    public String getYes() {
        return yes;
    }

    public void setYes(String yes) {
        this.yes = yes;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
