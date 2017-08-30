package com.lingju.assistant.player.exception;

/**
 * Created by Administrator on 2016/11/19.
 */
public class LingjuPlayException extends Exception {

    private int code;

    public LingjuPlayException(int code) {
        this.code=code;
    }

    public LingjuPlayException(int code, String detailMessage) {
        super(detailMessage);
        this.code=code;
    }

    public LingjuPlayException setCode(int code) {
        this.code = code;
        return this;
    }

    public int getCode() {
        return code;
    }

    public LingjuPlayException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public LingjuPlayException(Throwable throwable) {
        super(throwable);
    }
}
