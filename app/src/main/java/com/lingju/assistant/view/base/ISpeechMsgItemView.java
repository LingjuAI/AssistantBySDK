package com.lingju.assistant.view.base;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.lingju.model.temp.speech.SpeechMsg;

/**
 * Created by Administrator on 2016/11/4.
 */
public interface ISpeechMsgItemView<T extends SpeechMsg> {


    public String getText();

    public int getPosition();

    public void setPosition(int p);

    public void bind(int position,T t);

}
