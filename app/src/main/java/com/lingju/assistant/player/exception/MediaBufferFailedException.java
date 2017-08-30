package com.lingju.assistant.player.exception;

/**
 * Created by Administrator on 2016/11/19.
 */
public class MediaBufferFailedException extends LingjuPlayException {
    public MediaBufferFailedException(int code) {
        super(code);
    }

    public MediaBufferFailedException(int code, String detailMessage) {
        super(code, detailMessage);
    }

    public MediaBufferFailedException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public MediaBufferFailedException(Throwable throwable) {
        super(throwable);
    }
}
