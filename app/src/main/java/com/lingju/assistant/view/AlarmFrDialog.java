package com.lingju.assistant.view;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.util.AssistUtils;

import java.util.Calendar;

public class AlarmFrDialog extends Dialog implements View.OnClickListener {
    private Activity context;
    private OnResultListener onResultListener;
    private int fr = 0;
    private boolean repeat;
    private boolean confirmable;

    private int[] map = new int[]{
            R.id.alarm_fr0,
            R.id.alarm_fr1,
            R.id.alarm_fr2,
            R.id.alarm_fr3,
            R.id.alarm_fr4,
            R.id.alarm_fr5,
            R.id.alarm_fr6,
            R.id.alarm_fr7,
    };
    private TextView mTvConfirm;

    public AlarmFrDialog(Activity context, int fr, boolean repeat, OnResultListener listener) {
        super(context, R.style.lingju_commond_dialog);
        setCancelable(false);
        this.context = context;
        this.fr = fr;
        this.repeat = repeat;
        this.onResultListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarm_fr_dialog);
        findViewById(R.id.afd_cancel).setOnClickListener(this);
        mTvConfirm = (TextView) findViewById(R.id.afd_confirm);
        mTvConfirm.setOnClickListener(this);

        for (int i = 0; i < map.length; i++) {
            ((AppCompatCheckBox) findViewById(map[i])).setOnCheckedChangeListener(checkListener);
        }
        if (repeat) {
            int is[] = AssistUtils.transalteWeekDays(fr);
            for (int i : is) {
                ((AppCompatCheckBox) findViewById(map[i])).setChecked(true);
            }
        } else {
            ((AppCompatCheckBox) findViewById(map[0])).setChecked(true);
        }

    }

    private CheckBox.OnCheckedChangeListener checkListener = new CheckBox.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Log.i("AlarmFrcheckListener", "buttonView=" + buttonView);
            if (isChecked) {
                confirmable = true;
                if (buttonView.getId() == map[0]) {
                    checkedOnce(true);
                } else {
                    checkedOnce(false);
                }
            } else {
                for (int id : map) {
                    if (((AppCompatCheckBox) findViewById(id)).isChecked()) {
                        confirmable = true;
                        break;
                    }
                    confirmable = false;
                }
            }
            mTvConfirm.setTextColor(confirmable ? context.getResources().getColor(R.color.base_blue)
                    : context.getResources().getColor(R.color.forbid_click_color));
        }
    };

    /**
     * 仅一次与其他周期互斥
     **/
    private void checkedOnce(boolean flag) {
        if (flag) {
            for (int i = 1; i < map.length; i++) {
                ((AppCompatCheckBox) findViewById(map[i])).setChecked(false);
            }
        } else {
            ((AppCompatCheckBox) findViewById(map[0])).setChecked(false);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.afd_confirm) {
            if (!((AppCompatCheckBox) findViewById(map[0])).isChecked()) {
                fr = 0;
                for (int i = 1; i < map.length; i++) {
                    if (((AppCompatCheckBox) findViewById(map[i])).isChecked()) {
                        fr <<= 3;
                        fr += i;
                    }
                }
                if (fr == 0) {
                    return;
                }
                repeat = true;
            } else {
                int week = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
                fr = week - 1 > 0 ? week - 1 : 7;
                repeat = false;
            }
            if (onResultListener != null) {
                onResultListener.onResult(fr, repeat);
            }
        }
        cancel();
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
        void onResult(int fr, boolean repeat);
    }
}
