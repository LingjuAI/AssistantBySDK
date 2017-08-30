package com.lingju.assistant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.view.TimePicker;
import com.lingju.model.SimpleDate;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2016/12/3.
 */
public class TimePickerActivity extends GoBackActivity {

    public final static int FOR_TIME_RESULT = 2;
    public final static String TIME = "time";
    public final static String TITLE = "title";
    @BindView(R.id.atp_timepicker)
    TimePicker mAtpTimepicker;
    @BindView(R.id.atp_title)
    TextView mAtpTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timepicker);
        ButterKnife.bind(this);
        Intent intent =  getIntent();
        if (intent != null) {
            int time = intent.getIntExtra(TIME, -1);
            String title = intent.getStringExtra(TITLE);
            if (!TextUtils.isEmpty(title))
                mAtpTitle.setText(title);
            mAtpTimepicker.setDefaultTime(time > -1 ? new SimpleDate(time) : new SimpleDate());

        }
    }

    @OnClick(R.id.atp_back)
    public void clickBack(){
        Intent intent = new Intent(this, AlarmEditActivity.class);
        intent.putExtra(TIME, mAtpTimepicker.getSelectedTime().toValue());
        setResult(FOR_TIME_RESULT, intent);
        goBack();
    }
}
