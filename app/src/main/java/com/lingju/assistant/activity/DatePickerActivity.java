package com.lingju.assistant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.view.DatePicker;

public class DatePickerActivity extends GoBackActivity implements View.OnClickListener{
	
	public final static int FOR_DATE_RESULT=1;
	public final static String DATE="date";
	public final static String TITLE="title";
	private Intent intent;
	private DatePicker dp;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_datepicker);
		findViewById(R.id.adp_back).setOnClickListener(this);
		dp=(DatePicker) findViewById(R.id.adp_datepicker);
		intent=getIntent();
		if(intent!=null){
			long d=intent.getLongExtra(DATE, System.currentTimeMillis());
			dp.setDefaultDate(d);
			String title=intent.getStringExtra(TITLE);
			if(title!=null){
				((TextView)findViewById(R.id.adp_title)).setText(title);
			}
		}
	}
	
	

	@Override
	public void onClick(View v) {
		if(intent!=null){
			intent.putExtra(DATE, dp.getSelectedDate().getTimeInMillis());
			setResult(FOR_DATE_RESULT, intent);
		}
		goBack();
	}
}
