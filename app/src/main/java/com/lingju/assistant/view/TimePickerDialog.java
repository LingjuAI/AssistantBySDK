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
import com.lingju.model.SimpleDate;

public class TimePickerDialog extends Dialog implements View.OnClickListener{
	private Activity context;
	private SimpleDate time=new SimpleDate();
	private TimePicker tp;
	private TimePicker.OnchangedListener onChangedListener;

	public TimePickerDialog(Activity context, int time, TimePicker.OnchangedListener listener) {
		super(context, R.style.lingju_dialog2);
		setCancelable(true);
		this.context=context;
		this.time.setValue(time);;
		this.onChangedListener=listener;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_timepicker);
		findViewById(R.id.dtp_close).setOnClickListener(this);
		tp=(TimePicker) findViewById(R.id.dtp_timepicker);
		tp.setDefaultTime(time);
		tp.setOnChangedListener(onChangedListener);
	}

	@Override
	public void onClick(View v) {
		cancel();
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


	@Override
	public void show() {
		Window dialogWindow =getWindow();
        LayoutParams lp = dialogWindow.getAttributes();
        dialogWindow.setGravity(Gravity.LEFT| Gravity.BOTTOM);
        WindowManager m = context.getWindowManager();
        Display d = m.getDefaultDisplay(); // 获取屏幕宽、高用
        lp.width = d.getWidth(); 
        lp.x = 0;
        lp.y = 0;
        dialogWindow.setAttributes(lp);
		super.show();
	}
}
