package com.lingju.assistant.view.base;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.model.temp.speech.SpeechMsg;

/**
 * Created by Administrator on 2016/11/5.
 */
public abstract class BaseMsgItemView extends LinearLayout implements ISpeechMsgItemView<SpeechMsg> {

    protected int position;
    protected TextView mTextView;

    public BaseMsgItemView(Context context) {
        super(context);
        init(context);
    }

    public BaseMsgItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BaseMsgItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    protected abstract void init(Context mContext);

    @Override
    public String getText() {
        return mTextView.getText().toString();
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public void setPosition(int p) {
        this.position = p;
    }

    @Override
    public void bind(int position, SpeechMsg speechMsg) {
        this.position = position;
        String content = speechMsg.text;
        if (content.endsWith("。"))
            content = content.substring(0, content.length() - 1);
        this.mTextView.setText(content);
    }

}
