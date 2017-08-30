package com.lingju.assistant.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.lingju.assistant.R;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2017/2/18.
 */
public class AlarmItemDialog extends Dialog {
    @BindView(R.id.alarm_item_btns)
    RadioGroup mAlarmItemBtns;
    @BindView(R.id.alarm_item_rb1)
    RadioButton mAlarmItemRb1;
    @BindView(R.id.alarm_item_rb2)
    RadioButton mAlarmItemRb2;
    @BindView(R.id.alarm_item_rb3)
    RadioButton mAlarmItemRb3;
    private String mAlarmItem;
    private OnItemSelectedListener mSelectedListener;
    private Map<String, Integer> itemMaps = new HashMap<>();

    public AlarmItemDialog(Context context, String item, OnItemSelectedListener listener) {
        super(context, R.style.lingju_commond_dialog);
        mAlarmItem = item;
        mSelectedListener = listener;
    }

    protected AlarmItemDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_alarm_item);
        ButterKnife.bind(this);
        fillMap();
        mAlarmItemBtns.check(itemMaps.get(mAlarmItem));
        mAlarmItemBtns.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton selectedBtn = (RadioButton) findViewById(checkedId);
                mAlarmItem = selectedBtn.getText().toString();
                if (mSelectedListener != null)
                    mSelectedListener.onSelected(mAlarmItem);
                dismiss();
            }
        });
    }

    private void fillMap() {
        itemMaps.put(mAlarmItemRb1.getText().toString(), R.id.alarm_item_rb1);
        itemMaps.put(mAlarmItemRb2.getText().toString(), R.id.alarm_item_rb2);
        itemMaps.put(mAlarmItemRb3.getText().toString(), R.id.alarm_item_rb3);
    }

    @OnClick({R.id.tv_cancel})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_cancel:
                dismiss();
                break;
        }
    }
   /* @OnClick({R.id.alarm_item_cancel, R.id.alarm_item_confirm})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.alarm_item_confirm:
                if (mSelectedListener != null)
                    mSelectedListener.onSelected(mAlarmItem);
                break;
        }
        dismiss();
    }*/

    public interface OnItemSelectedListener {
        void onSelected(String item);
    }
}
