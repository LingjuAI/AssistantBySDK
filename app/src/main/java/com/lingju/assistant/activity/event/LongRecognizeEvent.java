package com.lingju.assistant.activity.event;

/**
 * Created by Ken on 2017/2/21.
 */
public class LongRecognizeEvent {

    private String result;

    public LongRecognizeEvent(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

}
