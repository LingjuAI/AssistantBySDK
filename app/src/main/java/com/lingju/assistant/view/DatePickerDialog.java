package com.lingju.assistant.view;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import com.lingju.assistant.R;

import java.util.Calendar;

public class DatePickerDialog extends Dialog implements View.OnClickListener{
	private Activity context;
	private Calendar date=Calendar.getInstance();
	private DatePicker dp;
	private DatePicker.OnchangedListener onChangedListener;

	public DatePickerDialog(Activity context, long date, DatePicker.OnchangedListener listener) {
		super(context, R.style.lingju_dialog2);
		setCancelable(true);
		this.context=context;
		this.date.setTimeInMillis(date);
		this.onChangedListener=listener;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_datepicker);
		findViewById(R.id.ddp_close).setOnClickListener(this);
		dp=(DatePicker) findViewById(R.id.ddp_datepicker);
		dp.setDefaultDate(date.getTimeInMillis());
		dp.setOnChangedListener(onChangedListener);
	}

	@Override
	public void onClick(View v) {
		cancel();
	}

	public Calendar getDate() {
		return date=dp.getSelectedDate();
	}

	@Override
	public void show() {
		Window dialogWindow =getWindow();
        LayoutParams lp = dialogWindow.getAttributes();
        dialogWindow.setGravity(Gravity.CENTER|Gravity.BOTTOM);
        WindowManager m = context.getWindowManager();
        Display d = m.getDefaultDisplay(); // 获取屏幕宽、高用
        lp.width = d.getWidth();
        lp.x = 0;
        lp.y = 0;
        dialogWindow.setAttributes(lp);
		super.show();
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if(context.getWindow().getAttributes().screenBrightness==0.01f){
			LayoutParams params=context.getWindow().getAttributes();
			params.screenBrightness= LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
			context.getWindow().setAttributes(params);
		}
		return super.dispatchTouchEvent(ev);
	}
}
