package com.lingju.assistant.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.StopListennerActivity;
import com.lingju.assistant.activity.index.IAlarmRinging;
import com.lingju.assistant.activity.index.presenter.AlarmRingPresenter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2016/11/29.
 */
public class AlarmRingingActivity extends StopListennerActivity implements IAlarmRinging.AlarmRingingView {


    @BindView(R.id.aar_time)
    TextView mAarTime;
    private AlarmRingPresenter mPresenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_ringing);
        ButterKnife.bind(this);
        mPresenter = new AlarmRingPresenter(this);
        mPresenter.goRing(getIntent());
    }

    @Override
    public void setRingRime(String time) {
        mAarTime.setText(time);
    }

    @Override
    public void destoryView() {
        finish();
    }

    @OnClick(R.id.aar_close)
    public void close() {
        destoryView();
    }

    @OnClick(R.id.aar_delay)
    public void delay() {
        mPresenter.delayRing();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.stopRing();
    }
}
