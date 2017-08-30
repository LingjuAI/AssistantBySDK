package com.lingju.assistant.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.lingju.assistant.R;
import com.lingju.assistant.view.wheel.widget.OnWheelChangedListener;
import com.lingju.assistant.view.wheel.widget.WheelView;
import com.lingju.assistant.view.wheel.widget.adapters.ArrayWheelAdapter;
import com.lingju.assistant.view.wheel.widget.adapters.NumericWheelAdapter;
import com.lingju.model.SimpleDate;

public class TimePicker extends LinearLayout{
	private WheelView ampmView;
	private WheelView hourView;
	private WheelView minuteView;
	
	private NumericWheelAdapter hourAdapter;
	private NumericWheelAdapter minuteAdapter;
	private ArrayWheelAdapter<String> ampmAdapter;
	private String ampmArray[]=new String[]{"上午","下午"};
	
	private SimpleDate defaultDate=new SimpleDate();
	private OnchangedListener onChangedListener;
	
	
	public TimePicker(Context context, SimpleDate defaultDate) {
		super(context);
		this.defaultDate=defaultDate;
		//this.defaultListener=defaultListener;
		init(context);
	}

	public TimePicker(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public TimePicker(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	
	public void setDefaultTime(SimpleDate defaultTime) {
		if(defaultTime==null||this.defaultDate.equals(defaultTime))return;
		this.defaultDate=defaultTime;
        hourView.setCurrentItem(defaultDate.getHour());
        minuteView.setCurrentItem(defaultDate.getMinute());
        ampmView.setCurrentItem(defaultDate.getHour()<13?0:1);
	}
	
	public void setOnChangedListener(OnchangedListener onChangedListener) {
		this.onChangedListener = onChangedListener;
	}
	
	public SimpleDate getSelectedTime() {
		return defaultDate;
	}
	
	private void init(Context context){
		LayoutInflater inflater=LayoutInflater.from(context);
		inflater.inflate(R.layout.time_picker, this);
		
		ampmView=(WheelView) findViewById(R.id.tp_am_pm);
		hourView=(WheelView) findViewById(R.id.tp_hour);
		minuteView=(WheelView) findViewById(R.id.tp_mimute);
		
		hourAdapter=new NumericWheelAdapter(context, 0, 23);
		hourAdapter.setItemResource(R.layout.wheel_text_item);
        hourAdapter.setItemTextResource(R.id.text);
        hourView.setViewAdapter(hourAdapter);
        hourView.setCyclic(true);
        hourView.setCurrentItem(defaultDate.getHour());
        hourView.addChangingListener(new OnWheelChangedListener() {
			
			@Override
			public void onChanged(WheelView wheel, int oldValue, int newValue) {
				defaultDate.setHour(newValue);
				ampmView.setCurrentItem(newValue<13?0:1);
				if(onChangedListener!=null){
					onChangedListener.onChanged(defaultDate);
				}
			}
		});

		minuteAdapter=new NumericWheelAdapter(context, 0, 59);
		minuteAdapter.setItemResource(R.layout.wheel_text_item);
		minuteAdapter.setItemTextResource(R.id.text);
		minuteView.setViewAdapter(minuteAdapter);
        minuteView.setCyclic(true);
        minuteView.setCurrentItem(defaultDate.getMinute());
        minuteView.addChangingListener(new OnWheelChangedListener() {
			
			@Override
			public void onChanged(WheelView wheel, int oldValue, int newValue) {
				defaultDate.setMinute(newValue);
				if(onChangedListener!=null){
					onChangedListener.onChanged(defaultDate);
				}
			}
		});

		ampmAdapter=new ArrayWheelAdapter<String>(context, ampmArray);
		ampmAdapter.setItemResource(R.layout.wheel_text_item);
        ampmAdapter.setItemTextResource(R.id.text);
        ampmView.setViewAdapter(ampmAdapter);
        ampmView.setCyclic(false);
        ampmView.setCurrentItem(defaultDate.getHour()<13?0:1);
        ampmView.addChangingListener(new OnWheelChangedListener() {
			
			@Override
			public void onChanged(WheelView wheel, int oldValue, int newValue) {
				int hour=defaultDate.getHour();
				if(newValue==1){
					defaultDate.setHour(hour%12+12);
					hourView.setCurrentItem(hour%12+12);
				}
				else{
					defaultDate.setHour( hour%12);
					hourView.setCurrentItem(hour%12);
				}
				if(onChangedListener!=null){
					onChangedListener.onChanged(defaultDate);
				}
			}
		});
	}
	
	public interface OnchangedListener{
		public void onChanged(SimpleDate date);
	}
	
}
