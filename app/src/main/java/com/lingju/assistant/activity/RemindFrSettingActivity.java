package com.lingju.assistant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.util.AssistUtils;

import java.util.Calendar;

public class RemindFrSettingActivity extends GoBackActivity {

	public final static String TYPE="type";
	public final static String DATE="date";
	
	public final static int FOR_FR=3;
	
	private Intent intent;
	private int type=0;
	private Calendar date=Calendar.getInstance();
	
	private int[] map=new int[]{
	R.id.remind_fr0,
	R.id.remind_fr1,
	R.id.remind_fr2,
	R.id.remind_fr3,
	R.id.remind_fr4,
	R.id.remind_fr5,
	R.id.remind_fr6,
	R.id.remind_fr7,
	R.id.remind_fr8,
	R.id.remind_fr9,
	R.id.remind_fr10
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_remind_fr);
		intent=getIntent();
		if(intent!=null){
			type=intent.getIntExtra(TYPE, 0);
			date.setTimeInMillis(intent.getLongExtra(DATE, System.currentTimeMillis()));
		}
		RadioGroup rg=(RadioGroup) findViewById(R.id.remind_fr_buttons);
		rg.check(map[type]);
		rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				for(int i=0;i<map.length;i++){
					if(map[i]==checkedId){
						type=i;
						return;
					}
				}
			}
		});
		((RadioButton)findViewById(R.id.remind_fr9)).setText(AssistUtils.translateRemindFrequency(9,date));
		((RadioButton)findViewById(R.id.remind_fr10)).setText(AssistUtils.translateRemindFrequency(10,date));
		findViewById(R.id.arf_back).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(intent!=null){
					intent.putExtra(TYPE, type);
					setResult(FOR_FR, intent);
				}
				goBack();
			}
		});
		
	}
	
	

}
