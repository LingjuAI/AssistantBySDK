package com.lingju.assistant.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.model.Accounting;
import com.lingju.common.log.Log;

import java.util.List;

public class AccountTimeLineItem extends LinearLayout {
	private final static String TAG="AccountTimeLineItem";
	
	private Context context;
	private LinearLayout leftBox;
	private LinearLayout rightBox;
	private List<Accounting> leftData;
	private List<Accounting> rightData;
	private String date;
	
	private OnEditListener editListener;
		
	public AccountTimeLineItem(Context context, List<Accounting> leftData, List<Accounting> rightData, String date, OnEditListener editListener) {
		super(context);
		this.leftData=leftData;
		this.rightData=rightData;
		this.date=date;
		this.editListener=editListener;
		init(context);
	}

	public AccountTimeLineItem(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public AccountTimeLineItem(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	public void setEditListener(OnEditListener editListener) {
		this.editListener = editListener;
	}
	
	public OnEditListener getEditListener() {
		return editListener;
	}
	
	public void setLeftData(List<Accounting> leftData) {
		this.leftData = leftData;
		leftBox.removeAllViews();
		if(leftData!=null&&leftData.size()>0){
			Log.d(TAG, "leftData.size()=="+leftData.size());
			for(int i=0;i<leftData.size();i++){
				leftBox.addView(new AccountingTextView(context, leftData.get(i),editListener));
			}
		}
	}
	
	public void setRightData(List<Accounting> rightData) {
		this.rightData = rightData;
		rightBox.removeAllViews();
		if(rightData!=null&&rightData.size()>0){
			Log.d(TAG, "rightData.size()=="+rightData.size());
			for(int i=0;i<rightData.size();i++){
				rightBox.addView(new AccountingTextView(context, rightData.get(i),editListener));
			}
		}
	}
	
	public List<Accounting> getLeftData() {
		return leftData;
	}
	
	public List<Accounting> getRightData() {
		return rightData;
	}
	
	private void init(Context context){
		this.context=context;
		LayoutInflater infalter=LayoutInflater.from(context);
		infalter.inflate(R.layout.accounting_timeline, this);
		leftBox=(LinearLayout) findViewById(R.id.atimeline_left_box);
		rightBox=(LinearLayout) findViewById(R.id.atimeline_right_box);
		((TextView)findViewById(R.id.atimeline_time)).setText(this.date.substring(this.date.indexOf(".")+1));
		if(leftData!=null&&leftData.size()>0){
			Log.d(TAG, "leftData.size()=="+leftData.size());
			for(int i=0;i<leftData.size();i++){
				leftBox.addView(new AccountingTextView(context, leftData.get(i),editListener));
			}
		}
		if(rightData!=null&&rightData.size()>0){
			Log.d(TAG, "rightData.size()=="+rightData.size());
			for(int i=0;i<rightData.size();i++){
				rightBox.addView(new AccountingTextView(context, rightData.get(i),editListener));
			}
		}
	}
	
	public int getLeftBoxChildCount(){
		return leftBox.getChildCount();
	}
	
	public int getRightBoxChildCount(){
		return rightBox.getChildCount();
	}
	
	public void removeLeftChildAt(int i){
		leftBox.removeViewAt(i);
	}
	
	public void removeRightChildAt(int i){
		rightBox.removeViewAt(i);
	}
	
	public View getLeftBoxChildAt(int i){
		return leftBox.getChildAt(i);
	}
	
	public View getRightBoxChildAt(int i){
		return rightBox.getChildAt(i);
	}
	
	public interface OnEditListener{
		public void onEdit(Accounting account);
		public void onView(Accounting account);
	}

}
