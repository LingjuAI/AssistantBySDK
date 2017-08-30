package com.lingju.model.temp.speech;

import com.lingju.assistant.service.AssistantService;

/**
 * Created by Administrator on 2016/11/4.
 */
public class SpeechMsg {

    public String text;
    private int inputType = AssistantService.INPUT_VOICE;
    private boolean isSimpleChat = false;

    public SpeechMsg(String text) {
        this(text, AssistantService.INPUT_VOICE);
    }

    public SpeechMsg(String text, int type) {
        this.text = text;
        this.inputType = type;
    }

    public int getInputType() {
        return inputType;
    }

    public boolean isSimpleChat() {
        return isSimpleChat;
    }

    public void setSimpleChat(boolean isSimpleChat) {
        this.isSimpleChat = isSimpleChat;
    }
}
