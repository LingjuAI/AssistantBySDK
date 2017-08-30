package com.lingju.model.temp.speech;

/**
 * Created by Administrator on 2016/11/4.
 */
public class ResponseMsg extends SpeechMsg {

    public int type;

    public ResponseMsg(String text) {
        super(text);
    }

    public ResponseMsg setType(int type) {
        this.type = type;
        return this;
    }
}
