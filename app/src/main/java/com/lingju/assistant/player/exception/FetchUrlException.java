package com.lingju.assistant.player.exception;

/**
 * Created by Administrator on 2016/11/19.
 */
public class FetchUrlException extends LingjuPlayException {

    public FetchUrlException(int code) {
        super(code);
    }

    public FetchUrlException(int code, String detailMessage) {
        super(code, detailMessage);
    }

    public FetchUrlException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public FetchUrlException(Throwable throwable) {
        super(throwable);
    }

}
