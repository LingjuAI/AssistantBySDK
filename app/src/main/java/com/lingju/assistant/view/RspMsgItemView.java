package com.lingju.assistant.view;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.LevelListDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.view.base.BaseMsgItemView;

/**
 * Created by Administrator on 2016/11/4.
 */
public class RspMsgItemView extends BaseMsgItemView implements View.OnTouchListener {

    private AnimationDrawable animation;
    private OnItemClickListener listener;
    private LevelListDrawable mListDrawable;
    private LinearLayout mLlTextBox;
    private float mStartX;
    private float mStartY;

    public RspMsgItemView(Context context) {
        super(context);
    }

    public RspMsgItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RspMsgItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void init(Context mContext) {
        LayoutInflater iflater = LayoutInflater.from(mContext);
        iflater.inflate(R.layout.common_bubble_dialog_left, this);
        mTextView = (TextView) findViewById(R.id.common_bubble_left_text);
        mTextView.setOnTouchListener(this);
        mListDrawable = (LevelListDrawable) mTextView.getBackground();
    }

    public void setListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void startAnimation() {
        try {
            if (mListDrawable.getLevel() == 0) {
                mListDrawable.setLevel(1);
                animation = (AnimationDrawable) mListDrawable.getCurrent();
                if (!animation.isRunning())
                    animation.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopAnimation() {
        if (animation == null)
            return;
        try {
            if (mListDrawable.getLevel() == 1) {
                animation.stop();
                mListDrawable.setLevel(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartX = event.getRawX();
                mStartY = event.getRawY();
                return false;
            case MotionEvent.ACTION_UP:
                float endX = event.getRawX();
                float endY = event.getRawY();
                if (Math.abs(endX - mStartX) <= 5 && Math.abs(endY - mStartY) <= 5) {     //上下、左右移动距离不超过5px视为点击事件
                    if (listener != null) {
                        if (mListDrawable.getLevel() == 1) {
                            listener.onTextClick(position, this);
                        } else {
                            listener.onSpeakerClick(position, this);
                        }
                    }
                }
                break;
        }
        return false;
    }

    public interface OnItemClickListener {

        void onSpeakerClick(int position, RspMsgItemView v);

        void onTextClick(int position, RspMsgItemView v);

    }
}
