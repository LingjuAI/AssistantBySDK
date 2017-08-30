package com.lingju.assistant.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.entity.DateList;
import com.lingju.model.Memo;

public class MemoDaysItem extends LinearLayout {
	
	private Context context;
	private LinearLayout mainBox;
	private TextView timeText;
	private DateList<Memo> datas;
	private OnEditListener editListener;
	
	public MemoDaysItem(Context context,OnEditListener editListener) {
		super(context);
		this.editListener=editListener;
		init(context);
	}

	public MemoDaysItem(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	

	public MemoDaysItem(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	private void init(Context context){
		this.context=context;
		LayoutInflater inflater=LayoutInflater.from(context);
		inflater.inflate(R.layout.remind_timeline, this);
		timeText=(TextView) findViewById(R.id.rtimeline_time);
		mainBox=(LinearLayout) findViewById(R.id.rtimeline_main_box);
		if(datas!=null){
			resetMainBox(datas);
		}
	}
	
	public void removeMemoItem(View v){
		mainBox.removeView(v);
	}
	
	public DateList<Memo> getDatas() {
		return datas;
	}
	
	public void resetMainBox(DateList<Memo> datas){
		if(datas!=null){
			this.datas=datas;
			if(mainBox.getChildCount()>0)
			mainBox.removeAllViews();
			if(datas.getList()!=null&&datas.getList().size()>0){
				int l=datas.getList().size();
				for(int i=0;i<l;i++){
					MemoItem item=new MemoItem(context,datas.getList().get(i),editListener);
					item.setItem(this);
					mainBox.addView(item);
				}
			}
			timeText.setText(datas.getDate().substring(datas.getDate().indexOf(".")+1));
		}
	}
	
	public interface OnEditListener{
		public void onEdit(Memo memo, MemoDaysItem item, View v);
		public void onView(Memo memo);
	}

}
