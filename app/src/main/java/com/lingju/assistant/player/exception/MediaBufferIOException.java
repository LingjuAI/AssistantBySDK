package com.lingju.assistant.player.exception;

/**
 * Created by Administrator on 2016/11/19.
 */
public class MediaBufferIOException extends LingjuPlayException {
    public MediaBufferIOException(int code) {
        super(code);
    }

    public MediaBufferIOException(int code, String detailMessage) {
        super(code, detailMessage);
    }

    public MediaBufferIOException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public MediaBufferIOException(Throwable throwable) {
        super(throwable);
    }
}
