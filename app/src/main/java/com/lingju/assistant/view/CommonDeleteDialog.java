package com.lingju.assistant.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.lingju.assistant.R;

public class CommonDeleteDialog extends Dialog implements View.OnClickListener{
	private Context context;
	private String title;
	private String content;
	private String contentHint="";
	private String cancel="取消";
	private String confirm;
	private int cancelBackground;
	private int confirmBackground;
	private EditText contentText;

	private TextView textInputLayout;
	private int inputLimits=5;
	private int inputType=0;
	public final static int INPUT_ZH=1;

	private OnCancelListener onCancelListener;
	private OnConfirmListener onConfirmListener;
	private OnTitleListener onTitleListener;
	private boolean oneButtonOnly=false;

	public CommonDeleteDialog(Context context, String title, String content, String confirmText) {
		super(context, R.style.lingju_commond_dialog);
		this.context=context;
		this.title=title;
		this.content=content;
		this.confirm=confirmText;
		this.oneButtonOnly=true;
	}

	public CommonDeleteDialog(Context context, String title, String content, String cancelText, String confirmText) {
		super(context, R.style.lingju_commond_dialog);
		this.context=context;
		this.title=title;
		this.content=content;
		this.cancel=cancelText;
		this.confirm=confirmText;
		setCancelable(false);
	}
	
	public CommonDeleteDialog setCancelBackground(int cancelBackground) {
		this.cancelBackground = cancelBackground;
		findViewById(R.id.ced_cancel).setBackgroundColor(cancelBackground);
		return this;
	}
	
	public CommonDeleteDialog setConfirmBackground(int confirmBackground) {
		this.confirmBackground = confirmBackground;
		findViewById(R.id.ced_confirm).setBackgroundColor(confirmBackground);
		return this;
	}
	
	public CommonDeleteDialog setOnCancelListener(OnCancelListener listener) {
		this.onCancelListener=listener;
		return this;
	}
	
	public CommonDeleteDialog setOnConfirmListener(OnConfirmListener onConfirmListener) {
		this.onConfirmListener = onConfirmListener;
		return this;
	}
	public CommonDeleteDialog setOnTitleListener(OnTitleListener onTitleListener) {
		this.onTitleListener = onTitleListener;
		return this;
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i("CommonEditDialog","onCreate>>limits="+inputLimits);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.common_delete_dialog);
		findViewById(R.id.ced_confirm).setOnClickListener(this);
		findViewById(R.id.ced_title).setOnClickListener(this);
		((TextView)findViewById(R.id.ced_confirm)).setText(confirm);
		((TextView)findViewById(R.id.ced_title)).setText(title);
		textInputLayout= (TextView) findViewById(R.id.edit_content);
		textInputLayout.setText(content);
		if(oneButtonOnly){
			findViewById(R.id.ced_cancel).setVisibility(View.GONE);
			findViewById(R.id.ced_empty).setVisibility(View.GONE);
		}
		else{
		    ((TextView)findViewById(R.id.ced_cancel)).setText(cancel);
			findViewById(R.id.ced_cancel).setOnClickListener(this);
		}
	}
	
	private TextWatcher tw=new TextWatcher() {
		//private int beforeMark;
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			//Log.i("CommonEditDialog.onTextChanged", "s="+s+",start="+start+",before="+before+",count="+count);
			
		}
		
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			//beforeMark=start;
			//Log.i("CommonEditDialog.beforeTextChanged", "s="+s+",start="+start+",after="+after+",count="+count);
		}
		
		@Override
		public void afterTextChanged(Editable s) {
			if(inputType==INPUT_ZH){
				int l=s.length();
				while(--l>=0){
					if(s.charAt(l)<0x4E00||s.charAt(l)>0x9FB0){
						s.delete(l, l+1);
					}
				}
			}
		}
	};
	
	public CommonDeleteDialog setHint(String text){
		this.contentHint=text;
		if(contentText!=null)
		contentText.setHint(text);
		return this;
	}
	
	public CommonDeleteDialog setInputType(int inputType) {
		this.inputType = inputType;
		return this;
	}
	
	public CommonDeleteDialog setInputLimits(int inputLimits) {
		this.inputLimits = inputLimits;
		if(contentText!=null)
			contentText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(inputLimits)});
		return this;
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.ced_cancel:
			cancel();
			if(onCancelListener!=null){
				onCancelListener.onCancel();
			}
			break;
		case R.id.ced_confirm:
			if(onConfirmListener!=null){
				onConfirmListener.onConfirm(textInputLayout.getText().toString());
			}
			break;
		case R.id.ced_title:
				if(onTitleListener!=null){
					onTitleListener.onTitle();
				}
			 break;
		}

	}
	
	public interface OnCancelListener{
		public void onCancel();
	}
	
	public interface OnConfirmListener{
		public void onConfirm(String text);
	}
	public interface OnTitleListener{
		public void onTitle();
	}

}
