package com.lingju.assistant.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.view.SwitchButton;
import com.lingju.model.SimpleDate;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2017/1/9.
 */
public class DndSettingActivity extends GoBackActivity implements TimePickerDialog.OnTimeSetListener,SwitchButton.OnCheckedChangeListener/*, OnWheelChangedListener*/ {

    @BindView(R.id.dnd_switch)
    SwitchButton mDndSwitch;
    @BindView(R.id.dnd_switch_box)
    RelativeLayout mDndSwitchBox;
    @BindView(R.id.dnd_ex_switch_box)
    RelativeLayout mDndExSwitchBox;
//    @BindView(R.id.dnd_hour)
//    WheelView mDndHour;
//    @BindView(R.id.dnd_mimute)
//    WheelView mDndMimute;
//    @BindView(R.id.dnd_am_pm)
//    WheelView mDndAmPm;
    @BindView(R.id.title_text)
    TextView mTitleText;
    @BindView(R.id.dnd_start_time)
    TextView mDndStartTime;
    @BindView(R.id.dnd_start_day)
    TextView mDndStartDay;
    @BindView(R.id.dnd_end_time)
    TextView mDndEndTime;
    @BindView(R.id.dnd_end_day)
    TextView mDndEndDay;
    @BindView(R.id.dnd_ex_switch)
    SwitchButton mDndExSwitch;
    @BindView(R.id.dnd_closed_text)
    TextView mDndClosedText;
    @BindView(R.id.dnd_ex_text)
    TextView mDndExText;
    @BindView(R.id.dnd_ex_text_describe)
    TextView mDndExTextDescribe;
    @BindView(R.id.dnd_start_box)
    RelativeLayout mDndStartBox;
    @BindView(R.id.dnd_end_box)
    RelativeLayout mDndEndBox;
    private CheckBox mode24Hours;
    private AppConfig mAppConfig;
    private boolean isStart = true;
    private SimpleDate dndStart;
    private SimpleDate dndEnd;
    private String ampmArray[] = new String[]{"上午", "下午"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dnd_setting);
        ButterKnife.bind(this);
        mDndSwitchBox.setClickable(true);
        mDndExSwitchBox.setClickable(true);
        mAppConfig = (AppConfig) getApplication();
        init();
        /* 该方法必须写在finish()之前，在finish的时候调用 */
       // setResult(CallSettingActivity.RESULT_DND_TIPS);
    }

    private void init() {
        isStart=AppConfig.dPreferences.getBoolean(AppConfig.IS_DND_START,true);
        Log.i("isStart","isStart"+isStart);
        mDndSwitch.setOnCheckedChangeListener(this);
        mDndExSwitch.setOnCheckedChangeListener(this);
        mDndSwitch.setChecked(mAppConfig.dndMode);
        mDndExSwitch.setChecked(mAppConfig.dndExMode);
        mDndStartTime.setText(mAppConfig.dndStart.toString());
        mDndEndTime.setText(mAppConfig.dndEnd.toString());

//        NumericWheelAdapter hourAdapter = new NumericWheelAdapter(this, 0, 23);
//        hourAdapter.setItemResource(R.layout.wheel_text_item);
//        hourAdapter.setItemTextResource(R.id.text);
//        mDndHour.setViewAdapter(hourAdapter);
//        mDndHour.setCyclic(true);
//        mDndHour.setCurrentItem(mAppConfig.dndStart.getHour());
//        mDndHour.addChangingListener(this);

//        NumericWheelAdapter minuteAdapter = new NumericWheelAdapter(this, 0, 59);
//        minuteAdapter.setItemResource(R.layout.wheel_text_item);
//        minuteAdapter.setItemTextResource(R.id.text);
//        mDndMimute.setViewAdapter(minuteAdapter);
//        mDndMimute.setCyclic(true);
//        mDndMimute.setCurrentItem(mAppConfig.dndStart.getMinute());
//        mDndMimute.addChangingListener(this);

//        ArrayWheelAdapter<String> ampmAdapter = new ArrayWheelAdapter<>(this, ampmArray);
//        ampmAdapter.setItemResource(R.layout.wheel_text_item);
//        ampmAdapter.setItemTextResource(R.id.text);
//        mDndAmPm.setViewAdapter(ampmAdapter);
//        mDndAmPm.setCyclic(false);
//        mDndAmPm.setCurrentItem(mAppConfig.dndStart.getHour() < 13 ? 0 : 1);
//        mDndAmPm.addChangingListener(this);


    }

    @Override
    protected void onResume() {
        super.onResume();
        setDndDay();

    }

    private void setDndDay() {
        dndStart=mAppConfig.dndStart;
        dndEnd=mAppConfig.dndEnd;
        if(dndStart.equals(dndEnd)){
            mDndStartDay.setText("  ");
            mDndEndDay.setText("次日");
        }
        if(dndStart.gt(dndEnd)){
            Log.i("dndsetting","dndStart.gt(dndEnd)"+dndStart.gt(dndEnd));
            mDndStartDay.setText("  ");
            mDndEndDay.setText("次日");
        }else{
            mDndStartDay.setText("  ");
            mDndEndDay.setText("  ");
        }
    }

    /**
     * 设置时间选择视图样式
     **/
    private void setTimeLine() {
//        if (isStart) {
////            mDndHour.setCurrentItem(mAppConfig.dndStart.getHour());
////            mDndMimute.setCurrentItem(mAppConfig.dndStart.getMinute());
////            mDndAmPm.setCurrentItem(mAppConfig.dndStart.getHour() < 13 ? 0 : 1);
//            mDndStartTimeLine.setBackgroundColor(getResources().getColor(R.color.base_blue));
//            mDndEndTimeLine.setBackgroundColor(getResources().getColor(R.color.common_background));
//        } else {
////            mDndHour.setCurrentItem(mAppConfig.dndEnd.getHour());
////            mDndMimute.setCurrentItem(mAppConfig.dndEnd.getMinute());
////            mDndAmPm.setCurrentItem(mAppConfig.dndEnd.getHour() < 13 ? 0 : 1);
//            mDndEndTimeLine.setBackgroundColor(getResources().getColor(R.color.base_blue));
//            mDndStartTimeLine.setBackgroundColor(getResources().getColor(R.color.common_background));
//        }
    }

    @Override
    protected void onDestroy() {
        AppConfig.dPreferences.edit().putString(AppConfig.DND_START_MOMENTS, mAppConfig.dndStart.toString()).commit();
        AppConfig.dPreferences.edit().putString(AppConfig.DND_END_MOMENTS, mAppConfig.dndEnd.toString()).commit();
        super.onDestroy();
    }

    @OnClick({ R.id.dnd_start_box, R.id.dnd_end_box,R.id.dnd_switch_box,R.id.dnd_ex_switch_box})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.dnd_start_box:
                AppConfig.dPreferences.edit().putBoolean(AppConfig.IS_DND_START,true).commit();
               // Calendar now = Calendar.getInstance();
                TimePickerDialog tpdStart = TimePickerDialog.newInstance(
                        DndSettingActivity.this,
                        /*now.get(Calendar.HOUR_OF_DAY),
                        now.get(Calendar.MINUTE),*/
                        //初始化界面显示上一次的时间
                        mAppConfig.dndStart.getHour(),
                        mAppConfig.dndStart.getMinute(),
                        true
                );
                tpdStart.setThemeDark(false);
                tpdStart.vibrate(false);
                tpdStart.dismissOnPause(false);
                tpdStart.enableSeconds(false);
                tpdStart.setVersion(TimePickerDialog.Version.VERSION_2);
                tpdStart.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        Log.d("TimePicker", "Dialog was cancelled");
                    }
                });
                tpdStart.show(getFragmentManager(), "Timepickerdialog");
                isStart = true;
               // setTimeLine();
                break;
            case R.id.dnd_end_box:
                AppConfig.dPreferences.edit().putBoolean(AppConfig.IS_DND_START,false).commit();
                // Calendar now = Calendar.getInstance();
                TimePickerDialog tpdEnt = TimePickerDialog.newInstance(
                        DndSettingActivity.this,
                        /*now.get(Calendar.HOUR_OF_DAY),
                        now.get(Calendar.MINUTE),*/
                        //初始化界面显示上一次的时间
                        mAppConfig.dndEnd.getHour(),
                        mAppConfig.dndEnd.getMinute(),
                        true
                );
                tpdEnt.setThemeDark(false);
                tpdEnt.vibrate(false);
                tpdEnt.dismissOnPause(false);
                tpdEnt.enableSeconds(false);
                tpdEnt.setVersion(TimePickerDialog.Version.VERSION_2);
                tpdEnt.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        Log.d("TimePicker", "Dialog was cancelled");
                    }
                });
                tpdEnt.show(getFragmentManager(), "Timepickerdialog");
                isStart = false;
                setTimeLine();
                break;
            case R.id.dnd_switch_box:
                if(mDndSwitch.isChecked()){
                    mDndSwitch.setChecked(false);
                }else{
                    mDndSwitch.setChecked(true);
                }
                break;
            case R.id.dnd_ex_switch_box:
                if(mDndExSwitch.isChecked()){
                    mDndExSwitch.setChecked(false);
                }else{
                    mDndExSwitch.setChecked(true);
                }
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.dnd_switch:
                mAppConfig.dndMode = isChecked;
                if(false==isChecked){
                    mDndStartBox.setClickable(false);
                    mDndEndBox.setClickable(false);
                    mDndExSwitch.setEnabled(false);
                    mDndExSwitchBox.setClickable(false);
                    mDndExSwitch.setBackColor(getResources().getColorStateList(R.color.ksw_md_solid_normal));
                    mDndClosedText.setText("已关闭，如有来电或来信将正常提醒");
                    mDndStartTime.setTextColor(getResources().getColor(R.color.common_disable_black));
                    mTitleText.setTextColor(getResources().getColor(R.color.common_disable_black));
//                    mDndStartTimeLine.setBackgroundColor(getResources().getColor(R.color.common_disable_black));
                    mDndEndTime.setTextColor(getResources().getColor(R.color.common_disable_black));
//                    mDndEndTimeLine.setBackgroundColor(getResources().getColor(R.color.common_disable_black));
                    mDndExText.setTextColor(getResources().getColor(R.color.common_disable_black));
                    mDndExTextDescribe.setTextColor(getResources().getColor(R.color.common_disable_black));
                    mDndEndDay.setTextColor(getResources().getColor(R.color.common_disable_black));
//                    if(isStart==true){
//                        mDndStartTimeLine.setBackgroundColor(getResources().getColor(R.color.common_disable_black));
//                        mDndEndTimeLine.setBackgroundColor(getResources().getColor(R.color.common_background));
//                    }else{
//                        mDndEndTimeLine.setBackgroundColor(getResources().getColor(R.color.common_disable_black));
//                        mDndStartTimeLine.setBackgroundColor(getResources().getColor(R.color.common_background));
//                    }
                }else if(true==isChecked){
                    mDndStartBox.setClickable(true);
                    mDndEndBox.setClickable(true);
                    mDndExSwitch.setEnabled(true);
                    mDndExSwitchBox.setClickable(true);
                    if(mDndExSwitch.isChecked()){
                        mDndExSwitch.setBackColor(getResources().getColorStateList(R.color.ksw_md_back_checked_color));
                    }else{
                        mDndExSwitch.setBackColor(getResources().getColorStateList(R.color.ksw_md_solid_normal));
                    }
                    mDndClosedText.setText("已打开，以下时间段如有来电或来信将不提醒");
                    setTimeLine();
                    mDndStartTime.setTextColor(getResources().getColor(R.color.new_text_color_first));
                    mTitleText.setTextColor(getResources().getColor(R.color.new_text_color_first));
                    mDndEndTime.setTextColor(getResources().getColor(R.color.new_text_color_first));
                    mDndExText.setTextColor(getResources().getColor(R.color.new_text_color_first));
                    mDndExTextDescribe.setTextColor(getResources().getColor(R.color.common_text_black));
                    mDndEndDay.setTextColor(getResources().getColor(R.color.new_text_color_second));
                }
                AppConfig.dPreferences.edit().putBoolean(AppConfig.DND_MODE, isChecked).commit();
                break;
            case R.id.dnd_ex_switch:
                mAppConfig.dndExMode = isChecked;
                AppConfig.dPreferences.edit().putBoolean(AppConfig.DND_EX_MODE, isChecked).commit();
                break;
        }
    }

//    @Override
//    public void onChanged(WheelView wheel, int oldValue, int newValue) {
//        switch (wheel.getId()) {
//            case R.id.dnd_hour:
//                if (isStart) {
//                    mAppConfig.dndStart.setHour(newValue);
//                    mDndStartTime.setText(mAppConfig.dndStart.toString());
//                } else {
//                    mAppConfig.dndEnd.setHour(newValue);
//                    mDndEndTime.setText(mAppConfig.dndEnd.toString());
//                }
//                mDndAmPm.setCurrentItem(newValue < 13 ? 0 : 1);
//                break;
//            case R.id.dnd_mimute:
//                if (isStart) {
//                    mAppConfig.dndStart.setMinute(newValue);
//                    mDndStartTime.setText(mAppConfig.dndStart.toString());
//                } else {
//                    mAppConfig.dndEnd.setMinute(newValue);
//                    mDndEndTime.setText(mAppConfig.dndEnd.toString());
//                }
//                break;
//            case R.id.dnd_am_pm:
//                int hour;
//                if (isStart) {
//                    hour = mAppConfig.dndStart.getHour();
//                    if (newValue == 1) {
//                        mAppConfig.dndStart.setHour(hour % 12 + 12);
//                        mDndHour.setCurrentItem(hour % 12 + 12);
//                    } else {
//                        mAppConfig.dndStart.setHour(hour % 12);
//                        mDndHour.setCurrentItem(hour % 12);
//                    }
//                } else {
//                    hour = mAppConfig.dndEnd.getHour();
//                    if (newValue == 1) {
//                        mAppConfig.dndEnd.setHour(hour % 12 + 12);
//                        mDndHour.setCurrentItem(hour % 12 + 12);
//                    } else {
//                        mAppConfig.dndEnd.setHour(hour % 12);
//                        mDndHour.setCurrentItem(hour % 12);
//                    }
//                }
//                break;
//        }
//    }

    @Override
    public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
        String hourString = hourOfDay < 10 ? "0"+hourOfDay : ""+hourOfDay;
        String minuteString = minute < 10 ? "0"+minute : ""+minute;
        String secondString = second < 10 ? "0"+second : ""+second;
        if(isStart){
            String time = hourString+":"+minuteString;
            dndStart = new SimpleDate(time);
            mAppConfig.dndStart.setHour(Integer.parseInt(hourString));
            mAppConfig.dndStart.setMinute(Integer.parseInt(minuteString));
            AppConfig.dPreferences.edit().putString(AppConfig.DND_START_MOMENTS,dndStart.toString()).commit();
            mDndStartTime.setText(time);
        }else{
            String time = hourString+":"+minuteString;
            dndEnd = new SimpleDate(time);
            mAppConfig.dndEnd.setHour(Integer.parseInt(hourString));
            mAppConfig.dndEnd.setMinute(Integer.parseInt(minuteString));
            AppConfig.dPreferences.edit().putString(AppConfig.DND_START_MOMENTS,dndEnd.toString()).commit();
            mDndEndTime.setText(time);
        }
        setDndDay();
    }
}
