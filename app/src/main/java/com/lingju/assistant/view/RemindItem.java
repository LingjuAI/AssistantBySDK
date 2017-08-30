package com.lingju.assistant.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.model.Remind;
import com.lingju.model.SimpleDate;
import com.lingju.util.AssistUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class RemindItem extends LinearLayout implements View.OnLongClickListener{
	
	private String title;
	private int frequency;
	private int time;  
	
	private Remind remind;
	
	private TextView titleText;
	private TextView contentText;
	private RemindDaysItem.OnEditListener editListener;
	private RemindDaysItem item;
	
	
	public RemindItem(Context context, Remind remind, RemindDaysItem.OnEditListener editListener) {
		super(context);
		this.remind=remind;
		this.title=remind.getContent();
		this.frequency=remind.getFrequency();
		this.time=new SimpleDate(remind.getRtime()).toValue();
		this.editListener=editListener;
		init(context);
	}

	public RemindItem(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.lingju);
		title=a.getString(R.styleable.lingju_item_title);
		frequency=a.getInt(R.styleable.lingju_frequency, 0);
		time=a.getInt(R.styleable.lingju_time, 0);
		a.recycle();
		init(context);
	}
	
	public void setRemind(Remind remind) {
		this.remind = remind;
	}

	public RemindItem(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.lingju);
		title=a.getString(R.styleable.lingju_item_title);
		frequency=a.getInt(R.styleable.lingju_frequency, 0);
		time=a.getInt(R.styleable.lingju_time, 0);
		a.recycle();
		init(context);
	}
	
	public void setItem(RemindDaysItem item) {
		this.item = item;
	}
	
	//单位像素px和dp的转化
	private int dp2px(int dp){
		return (int)(getResources().getDisplayMetrics().density*dp+0.5f);
	}
	
	private void init(Context context){
		setOrientation(VERTICAL);
		Resources rs=context.getResources();
		titleText=new TextView(context);
		titleText.setText(title);
		titleText.setSingleLine(true);
		titleText.setEllipsize(TruncateAt.END);
		titleText.setTextColor(rs.getColorStateList(R.color.new_text_color_first));
		titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		titleText.setPadding(0, dp2px(11), 0, 0);
		//titleText.getPaint().setFakeBoldText(true);
		LayoutParams lp=new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		//lp.setMargins(0, 0, 0, 10);
		//titleText.setLayoutParams(lp);
		addView(titleText, lp);
		
		contentText=new TextView(context);
		String fr="",begin="";
		if(remind!=null){
			Calendar cl=Calendar.getInstance();
			cl.setTimeInMillis(remind.getRdate().getTime());
			fr= AssistUtils.translateRemindFrequency(frequency,cl);
			begin=new SimpleDateFormat("yyyy年MM月dd日").format(remind.getRdate())+"起  ";
		}
		else{
			fr=AssistUtils.translateRemindFrequency(frequency);
		}
		contentText.setText(begin+fr+"  "+new SimpleDate(time).toString());
		contentText.setTextColor(rs.getColorStateList(R.color.new_text_color_second));
		contentText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
		contentText.setPadding(0, 0, 0, dp2px(13));
		contentText.setLayoutParams(lp);
		addView(contentText, lp);
		
		View v=new View(context);
		v.setBackgroundResource(R.color.new_line_black_border);
		LayoutParams lp2=new LayoutParams(LayoutParams.MATCH_PARENT, 1);
		addView(v, lp2);
		setBackgroundResource(R.drawable.press);
		setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(editListener!=null){
					editListener.onView(remind);
				}
			}
		});
		setOnLongClickListener(this);
	}
	

	@Override
	public boolean onLongClick(View v) {
		if(editListener!=null){
			editListener.onEdit(remind, item, v);
		}
		return true;
	}
	
	

}
