package com.lingju.assistant.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.Html;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.model.Accounting;

public class AccountingTextView extends LinearLayout implements View.OnLongClickListener{
	
	private Accounting account;
	private TextView project;
	private AccountTimeLineItem.OnEditListener editListener;
	
	public AccountingTextView(Context context, Accounting account, AccountTimeLineItem.OnEditListener editListener) {
		super(context);
		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		this.account=account;
		this.editListener=editListener;
		init(context, account.getEtype(), account.getAmount()>0?
				(account.getAmount()%100==0?Double.toString(account.getAmount()/100):account.getAmount()/100+"."+account.getAmount()%100)
				:"0",account.getAtype()==0);
	}

	public AccountingTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.lingju);
		String p=a.getString(R.styleable.lingju_project);
		String am=a.getString(R.styleable.lingju_amount);
		boolean isLeft=a.getBoolean(R.styleable.lingju_alignLeft, true);
		a.recycle();
		init(context,p,am,isLeft);
	}

	public AccountingTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.lingju);
		String p=a.getString(R.styleable.lingju_project);
		String am=a.getString(R.styleable.lingju_amount);
		boolean isLeft=a.getBoolean(R.styleable.lingju_alignLeft, true);
		a.recycle();
		init(context,p,am,isLeft);
	}
	
	public Accounting getAccount() {
		return account;
	}
	
	public void setAccount(Accounting account) {
		this.account = account;
	}
	
	//单位像素px和dp的转化
		private int dp2px(int dp){
			return (int)(getResources().getDisplayMetrics().density*dp+0.5f);
		}
	
	private void init(Context context,String p,String a,boolean isLeft){
		project=new TextView(context);
		Resources rs=context.getResources();
		project.setTextColor(rs.getColorStateList(R.color.new_text_color_first));
		//project.setBackgroundColor(Color.rgb(235, 235, 235));
		project.setGravity(Gravity.CENTER_VERTICAL);
		project.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		String text=isLeft?p+"<font color=\"#32c0c4\">"+a+"</font>元":p+"<font color=\"#f09c42\">"+a+"</font>元";
		project.setText(Html.fromHtml(text));
		addView(project);
		setPadding(0, dp2px(8), 0, dp2px(10));
		/*amount=new TextView(context);
		amount.setTextColor(rs.getColorStateList(isLeft?R.color.base_blue:R.color.brown));
		amount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
		amount.setText(a);
		
		TextView t3=new TextView(context);
		t3.setTextColor(rs.getColorStateList(R.color.new_text_color_first));
		t3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
		t3.setText("元");
		addView(amount);
		addView(t3);*/
		project.setGravity(isLeft?Gravity.RIGHT:Gravity.LEFT);
		setGravity(isLeft?Gravity.RIGHT:Gravity.LEFT);
		setBackgroundResource(R.drawable.press);
		setOnLongClickListener(this);
		setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(editListener!=null){
					editListener.onView(account);
				}
			}
		});
	}

	@Override
	public boolean onLongClick(View v) {
		if(editListener!=null){
			editListener.onEdit(account);
		}
		return true;
	}

}
