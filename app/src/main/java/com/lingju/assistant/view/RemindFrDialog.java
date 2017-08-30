package com.lingju.assistant.view;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.util.AssistUtils;

import java.util.Calendar;

public class RemindFrDialog extends Dialog implements View.OnClickListener {
    private Activity context;
    private OnResultListener onResultListener;
    private int fr = 0;
    private Calendar date;
    private int[] map = new int[]{
            R.id.remind_fr0,
            R.id.remind_fr1,
            R.id.remind_fr2,
            R.id.remind_fr3,
            R.id.remind_fr4,
            R.id.remind_fr5,
            R.id.remind_fr6,
            R.id.remind_fr7,
            R.id.remind_fr8,
            R.id.remind_fr9,
            R.id.remind_fr10
    };

    public RemindFrDialog(Activity context, int fr, Calendar date, OnResultListener listener) {
        super(context, R.style.lingju_commond_dialog);
        // setCancelable(false);
        this.context = context;
        this.fr = fr;
        this.date = date;
        this.onResultListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_remind_fr);
        /*findViewById(R.id.drf_cancel).setOnClickListener(this);
		findViewById(R.id.drf_confirm).setOnClickListener(this);*/
        RadioGroup rg = (RadioGroup) findViewById(R.id.remind_fr_buttons);
        rg.check(map[fr]);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                for (int i = 0; i < map.length; i++) {
                    if (map[i] == checkedId) {
                        fr = i;
                        if (onResultListener != null) {
                            onResultListener.onResult(fr);
                        }
                        cancel();
                        return;
                    }
                }
            }
        });
        //取消按钮点击监听
        TextView cancle = (TextView) findViewById(R.id.tv_cancel);
        cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RemindFrDialog.this.cancel();
            }
        });
        ((RadioButton) findViewById(R.id.remind_fr9)).setText(AssistUtils.translateRemindFrequency(9, date));
        ((RadioButton) findViewById(R.id.remind_fr10)).setText(AssistUtils.translateRemindFrequency(10, date));
    }

    @Override
    public void onClick(View v) {
        cancel();
//        if (v.getId() == R.id.drf_confirm) {
//            if (onResultListener != null) {
//                onResultListener.onResult(fr);
//            }
//        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            cancel();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (context.getWindow().getAttributes().screenBrightness == 0.01f) {
            LayoutParams params = context.getWindow().getAttributes();
            params.screenBrightness = LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            context.getWindow().setAttributes(params);
        }
        return super.dispatchTouchEvent(ev);
    }


    public interface OnResultListener {
        void onResult(int fr);
    }
}
