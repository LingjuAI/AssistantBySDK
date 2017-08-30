package com.lingju.assistant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.activity.index.IAlarmEdit;
import com.lingju.assistant.activity.index.presenter.AlarmEditPresenter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2016/12/3.
 */
public class AlarmEditActivity extends GoBackActivity implements IAlarmEdit.AlarmEditView {

    public static final int FOR_ALARM_EDIT = 6;
    @BindView(R.id.aae_time)
    TextView mAaeTime;
    @BindView(R.id.aae_fr)
    TextView mAaeFr;
    @BindView(R.id.aae_ring)
    TextView mAaeRing;
    private IAlarmEdit.IPresenter mEditPresenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_edit);
        ButterKnife.bind(this);
        mEditPresenter = new AlarmEditPresenter(this);
        mEditPresenter.initData(getIntent());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mEditPresenter.setAlarmText(data, resultCode);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void setTime(String time) {
        mAaeTime.setText(time);
    }

    @Override
    public void setFrequency(String fr) {
        mAaeFr.setText(fr);
    }

    @Override
    public void setRing(String ring) {
        mAaeRing.setText(ring);
    }

    @OnClick({R.id.aae_cancel, R.id.aae_back})
    public void clickBack(){
        mEditPresenter.cancelEdit();
    }

    @OnClick(R.id.aae_del)
    public void clickDelete(){
        mEditPresenter.deleteAlarm();
    }

    @OnClick(R.id.aae_confirm)
    public void clickSave(){
        mEditPresenter.saveAlarm();
    }

    @OnClick(R.id.aae_time)
    public void clickTime(){
        mEditPresenter.toSetTime();
    }

    @OnClick(R.id.aae_fr)
    public void clickFr(){
        mEditPresenter.toSetFr();
    }

    @OnClick(R.id.aae_ring)
    public void clickRing(){
        mEditPresenter.toSetRing();
    }
}
