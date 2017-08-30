package com.lingju.assistant.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.R;

public class LingjuCheckListBox extends LinearLayout{
	private String title;
	private TextView titleText;
	private ImageView closeBT;
	private int count=1;
	private int checkedPosition=-1;
	private Object[] list;
	private Context mContext;
	private CheckedListener listener;
	
	public LingjuCheckListBox(Context context, String title, String[] array) {
		super(context);
		this.title=title;
		this.list=array;
		init(context);
	}

	public LingjuCheckListBox(Context context, AttributeSet attrs) {
		super(context, attrs);
		defaultInit(context,attrs);
		init(context);
	}

	public LingjuCheckListBox(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		defaultInit(context,attrs);
		init(context);
	}
	
	private void defaultInit(Context context,AttributeSet attrs){
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.lingju);
		this.title=a.getString(R.styleable.lingju_item_title);
		this.list=a.getTextArray(R.styleable.lingju_array);
		/*int arrayRid=a.getInt(R.styleable.lingju_array, -1);
		Log.e("LingjuCheckListBox", "title="+title+",arrayID="+arrayRid);
		if(arrayRid>-1){
			this.list=context.getResources().getStringArray(arrayRid);
		}*/
		a.recycle();
	}
	
	private void init(Context context){
		this.mContext=context;
		LayoutInflater inflater=LayoutInflater.from(context);
		inflater.inflate(R.layout.call_select_dialog, this);
		if(list!=null){
			setList(list, title);
		}
		else if(this.title!=null){
			LinearLayout v=(LinearLayout) getChildAt(0);
			LinearLayout v1=(LinearLayout) v.getChildAt(0);
			titleText=(TextView)v1.getChildAt(0);
			closeBT=(ImageView) v1.getChildAt(1);
			closeBT.setOnTouchListener(defaultTounchListener);
			titleText.setText(this.title);
		}
	}
	
	public void setList(String[] array){
		setList(array, null);
	}
	
	public void setList(Object[] array,String title){
		
		if(array==null||array.length==0)return;
		LayoutInflater inflater=LayoutInflater.from(this.mContext);
		LinearLayout v=(LinearLayout) getChildAt(0);
		
		LinearLayout v1=(LinearLayout) v.getChildAt(0);
		if(titleText==null)
		titleText=(TextView)v1.getChildAt(0);
		if(closeBT==null){
			closeBT=(ImageView) v1.getChildAt(1);
			closeBT.setOnTouchListener(defaultTounchListener);
		}
		if(title!=null){
			titleText.setText(title);
			this.title=title;
		}
		int c=array.length;
		int b=1;
		if(c>this.count){
			int i=0;
			for(;i<this.count;i++){
				TextView t=(TextView) v.getChildAt(b+i);
				t.setText(array[i].toString());
				if(t.getTag()==null||!(t.getTag() instanceof Integer)){
					t.setTag(i);
					t.setOnTouchListener(defaultTounchListener);
				}
				/*t.setTag(i);
				t.setOnTouchListener(defaultTounchListener);*/
			}
			for(;i<c;i++){
				TextView textView=(TextView) inflater.inflate(R.layout.lingju_check_list_box_item, null);
				textView.setText(array[i].toString());
				textView.setTag(i);
				textView.setOnTouchListener(defaultTounchListener);
				v.addView(textView, i+b);
			}
		}
		else if(c==this.count){
			for(int i=0;i<this.count;i++){
				TextView t=(TextView) v.getChildAt(b+i);
				t.setText(array[i].toString());
				/*t.setTag(i);
				t.setOnTouchListener(defaultTounchListener);*/
			}
		}
		else{
			int i=0;
			for(;i<c;i++){
				TextView t=(TextView) v.getChildAt(b+i);
				t.setText(array[i].toString());
				/*t.setTag(i);
				t.setOnTouchListener(defaultTounchListener);*/
			}
			for(;i<this.count;i++){
				v.removeViewAt(i+b);
			}
		}
		this.count=c;
	}
	
	public int getCount() {
		return count;
	}
	
	public int getCheckedPosition() {
		return checkedPosition;
	}
	
	public void setCheckedPosition(int checkedPosition) {
		this.checkedPosition = checkedPosition;
	}
	
	public String getTitle() {
		return title;
	}
	
	
	public void setCheckListener(CheckedListener listener){
		this.listener=listener;
	}

	private OnTouchListener defaultTounchListener=new OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			Log.w("LingjuCheckListBox", "onTouch...."+v.getTag());
			if(event.getAction()==MotionEvent.ACTION_DOWN){
				if(v==closeBT){
					if(listener!=null){
						listener.onClose(true);
					}
					return true;
				}
				if(v.getTag() instanceof Integer){
					checkedPosition=(Integer) v.getTag();
					Log.i("LingjuCheckListBox", "CheckedPosition="+checkedPosition);
					if(listener!=null){
						listener.checked(checkedPosition);
						listener.onClose(false);
					}
					return true;
				}
			}
			return false;
		}
	};
	
	public interface CheckedListener{
		void checked(int position);
		void onClose(boolean notify);
	}
	
}
