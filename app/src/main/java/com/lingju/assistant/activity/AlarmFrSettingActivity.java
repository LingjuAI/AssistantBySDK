package com.lingju.assistant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.util.AssistUtils;

import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2016/12/3.
 */
public class AlarmFrSettingActivity extends GoBackActivity {

    public final static String TYPE = "type";
    public final static int FOR_FR = 3;

    private int type = 0;

    private int[] checkBoxs = new int[]{
            R.id.alarm_fr0,
            R.id.alarm_fr1,
            R.id.alarm_fr2,
            R.id.alarm_fr3,
            R.id.alarm_fr4,
            R.id.alarm_fr5,
            R.id.alarm_fr6,
            R.id.alarm_fr7,
    };
    private Map<Integer, CheckBox> map = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_fr);
        ButterKnife.bind(this);
        Intent intent = getIntent();
        if (intent != null) {
            type = intent.getIntExtra(TYPE, 0);
        }
        initView();
        initData();
    }

    private void initData() {
        int[] frs = AssistUtils.transalteWeekDays(type);
        for (int index : frs) {
            map.get(checkBoxs[index]).setChecked(true);
        }
    }

    private void initView() {
        for (int i = 0; i < checkBoxs.length; i++) {
            CheckBox fr = (CheckBox) findViewById(checkBoxs[i]);
            fr.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        if (buttonView.getId() == checkBoxs[0]) {
                            for (int i = 1; i < checkBoxs.length; i++) {
                                map.get(checkBoxs[i]).setChecked(false);
                            }
                        } else {
                            map.get(checkBoxs[0]).setChecked(false);
                        }
                    }
                }
            });
            map.put(checkBoxs[i], fr);
        }
    }

    @OnClick(R.id.aaf_back)
    public void clickBack() {
        type = 0;
        if (!map.get(checkBoxs[0]).isChecked()) {
            for (int i = 1; i < checkBoxs.length; i++) {
                if (map.get(checkBoxs[i]).isChecked()) {
                    type<<=3;
                    type+=i;
                }
            }
        }
        Intent intent = new Intent(this, AlarmEditActivity.class);
        intent.putExtra(TYPE, type);
        setResult(FOR_FR, intent);
        goBack();
    }
}
