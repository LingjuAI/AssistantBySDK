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
import com.lingju.model.Memo;

public class MemoItem extends LinearLayout implements View.OnLongClickListener{
	
	private String content; 
	
	private Memo memo;
	private TextView contentText;
	private MemoDaysItem.OnEditListener editListener;
	private MemoDaysItem item;
	
	
	public MemoItem(Context context, Memo memo, MemoDaysItem.OnEditListener editListener) {
		super(context);
		this.memo=memo;
		this.content=memo.getContent();
		this.editListener=editListener;
		init(context);
	}

	public MemoItem(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.lingju);
		content=a.getString(R.styleable.lingju_item_title);
		a.recycle();
		init(context);
	}
	
	public MemoItem(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.lingju);
		content=a.getString(R.styleable.lingju_item_title);
		a.recycle();
		init(context);
	}
	
	public void setItem(MemoDaysItem item) {
		this.item = item;
	}
	
	//单位像素px和dp的转化
	private int dp2px(int dp){
		return (int)(getResources().getDisplayMetrics().density*dp+0.5f);
	}
	
	private void init(Context context){
		setOrientation(VERTICAL);
		Resources rs=context.getResources();
		contentText=new TextView(context);
		contentText.setText(content);
		contentText.setTextColor(rs.getColorStateList(R.color.new_text_color_first));
		contentText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		contentText.setPadding(0, dp2px(11), 0, dp2px(11));
		//contentText.getPaint().setFakeBoldText(true);
		contentText.setSingleLine(true);
		contentText.setEllipsize(TruncateAt.END);
		LayoutParams lp=new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		//lp.setMargins(0,dp2px(10), 0, dp2px(10));
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
					editListener.onView(memo);
				}
			}
		});
		setOnLongClickListener(this);
	}
	

	@Override
	public boolean onLongClick(View v) {
		if(editListener!=null){
			editListener.onEdit(memo, item, v);
		}
		return true;
	}
	
	

}
