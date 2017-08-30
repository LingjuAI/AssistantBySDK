package com.lingju.assistant.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.view.base.BaseMsgItemView;
import com.lingju.model.temp.speech.SpeechMsg;

/**
 * Created by Administrator on 2016/11/4.
 */
public class ReqMsgItemView extends BaseMsgItemView {

    public ReqMsgItemView(Context context) {
        super(context);
    }

    public ReqMsgItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ReqMsgItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void init(Context mContext) {
        LayoutInflater iflater=LayoutInflater.from(mContext);
        iflater.inflate(R.layout.common_bubble_dialog_right, this);
        mTextView = (TextView) findViewById(R.id.common_bubble_right_text);
    }

}
