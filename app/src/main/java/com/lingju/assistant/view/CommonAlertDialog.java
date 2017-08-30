package com.lingju.assistant.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.common.log.Log;

public class CommonAlertDialog extends Dialog{
	private Context context;
	private String title;
	private String content;
	private OnCancelListener onCancelListener;

	public CommonAlertDialog(Context context, String title, String content) {
		super(context, R.style.lingju_commond_dialog);
		this.context=context;
		this.title=title;
		this.content=content;
		setCancelable(false);
	}

	public CommonAlertDialog setContent(String content) {
		this.content = content;
		((TextView)findViewById(R.id.cad_content)).setText(content);
		return this;
	}

	public CommonAlertDialog setTitle(String title) {
		this.title = title;
		((TextView)findViewById(R.id.cad_title)).setText(title);
		return this;
	}
	public CommonAlertDialog setOnCancelListener(OnCancelListener listener) {
		this.onCancelListener=listener;
		return this;
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i("CommonAlertDialog","onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.common_alert_dialog);
		((TextView)findViewById(R.id.cad_title)).setText(title);
		((TextView)findViewById(R.id.cad_content)).setText(content);
		//取消按钮点击监听
		TextView cancle = (TextView) findViewById(R.id.tv_cancel);
		cancle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CommonAlertDialog.this.cancel();
				if(onCancelListener!=null){
					//onCancelListener.onCancel();
				}
			}
		});
//		((CircleProgressBar)findViewById(R.id.progress_bar)).setColorSchemeResources(R.color.red_style, R.color.second_base_color, R.color.base_blue, R.color.colorPrimary);
//		findViewById(R.id.cad_close).setOnClickListener(new View.OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				cancel();
//			}
//		});
	}

	public interface OnCancelListener{
		public void onCancel();
	}
}
