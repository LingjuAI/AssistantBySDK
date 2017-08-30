package com.lingju.model.temp.speech;

import org.json.JSONArray;

/**
 * Created by Administrator on 2016/11/4.
 */
public class ResponseSetionsMsg extends ResponseMsg {

    private JSONArray setions;

    public ResponseSetionsMsg(String text) {
        super(text);
    }

    public ResponseSetionsMsg(String text, JSONArray setions) {
        super(text);
        this.setions = setions;
    }

    public void setSetions(JSONArray setions) {
        this.setions = setions;
    }

    public JSONArray getSetions() {
        return setions;
    }
}
