package com.lingju.assistant.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.lingju.assistant.R;

public class CommonDialog extends Dialog implements View.OnClickListener{
	private Context context;
	private String title;
	private String content;
	private String cancel="取消";
	private String confirm;
	private int cancelBackground;
	private int confirmBackground;

	private OnCancelListener onCancelListener;
	private OnConfirmListener onConfirmListener;
	private boolean oneButtonOnly=false;
	
	public CommonDialog(Context context, String title, String content, String confirmText) {
		super(context, R.style.lingju_commond_dialog);
		this.context=context;
		this.title=title;
		this.content=content;
		this.confirm=confirmText;
		this.oneButtonOnly=true;
	}
	
	public CommonDialog(Context context, String title, String content, String cancelText, String confirmText) {
		super(context, R.style.lingju_commond_dialog);
		this.context=context;
		this.title=title;
		this.content=content;
		this.cancel=cancelText;
		this.confirm=confirmText;
		setCancelable(false);
	}
	
	public CommonDialog setCancelBackground(int cancelBackground) {
		this.cancelBackground = cancelBackground;
		findViewById(R.id.cd_cancel).setBackgroundColor(cancelBackground);
		return this;
	}
	
	public CommonDialog setConfirmBackground(int confirmBackground) {
		this.confirmBackground = confirmBackground;
		findViewById(R.id.cd_confirm).setBackgroundColor(confirmBackground);
		return this;
	}

	
	public String getCancelText() {
		return cancel;
	}
	
	public String getConfirmText() {
		return confirm;
	}
	
	public CommonDialog setOnCancelListener(OnCancelListener listener) {
		this.onCancelListener=listener;
		return this;
	}
	
	public CommonDialog setOnConfirmListener(OnConfirmListener onConfirmListener) {
		this.onConfirmListener = onConfirmListener;
		return this;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.common_dialog);
		findViewById(R.id.cd_confirm).setOnClickListener(this);
		((TextView)findViewById(R.id.cd_confirm)).setText(confirm);
		((TextView)findViewById(R.id.cd_title)).setText(title);
		((TextView)findViewById(R.id.cd_content)).setText(content);
		if(oneButtonOnly){
			findViewById(R.id.cd_cancel).setVisibility(View.GONE);
		}
		else{
		    ((TextView)findViewById(R.id.cd_cancel)).setText(cancel);
			findViewById(R.id.cd_cancel).setOnClickListener(this);
		}
	}

	@Override
	public void onClick(View v) {
		cancel();
		switch(v.getId()){
		case R.id.cd_cancel:
			if(onCancelListener!=null){
				onCancelListener.onCancel();
			}
			break;
		case R.id.cd_confirm:
			if(onConfirmListener!=null){
				onConfirmListener.onConfirm();
			}
			break;
		}
	}
	
	public interface OnCancelListener{
		void onCancel();
	}
	
	public interface OnConfirmListener{
		void onConfirm();
	}
}
