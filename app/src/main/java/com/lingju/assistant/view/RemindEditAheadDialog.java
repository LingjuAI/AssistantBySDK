package com.lingju.assistant.view;

import android.app.Activity;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.lingju.assistant.R;
import com.lingju.assistant.view.base.BaseEditDialog;

public class RemindEditAheadDialog extends BaseEditDialog implements View.OnClickListener{
	private Activity context;
	private OnRemindAheadListener defaultListener;
	private CheckBox[] cbs=new CheckBox[4];
	private StringBuffer days=new StringBuffer();

	public RemindEditAheadDialog(Activity context, OnRemindAheadListener defaultListener) {
		super(context, R.style.lingju_dialog1);
		this.context=context;
		setCancelable(false);
		this.defaultListener=defaultListener;
	}


	public void setDefaultEditListener(OnRemindAheadListener listener) {
		this.defaultListener=listener;
	}

	@Override
	public void show() {
		Window dialogWindow =getWindow();
        LayoutParams lp = dialogWindow.getAttributes();
        dialogWindow.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.TOP);
        WindowManager m = context.getWindowManager();
        Display d = m.getDefaultDisplay(); // 获取屏幕宽、高用
        lp.width = (int) (d.getWidth() * 0.8);
        lp.y = 80; // 新位置Y坐标
        dialogWindow.setAttributes(lp);
		super.show();
	}

	@Override
	protected void initTaskView(LinearLayout llTaskContainer) {
		View taskView = View.inflate(context, R.layout.remind_edit_ahead_dialog, null);
		taskView.findViewById(R.id.read_cancel).setOnClickListener(this);
		taskView.findViewById(R.id.read_confirm).setOnClickListener(this);
		cbs[0]=(CheckBox)taskView.findViewById(R.id.read_fr7);
		cbs[0].setTag("7");
		cbs[1]=(CheckBox)taskView.findViewById(R.id.read_fr5);
		cbs[1].setTag("5");
		cbs[2]=(CheckBox)taskView.findViewById(R.id.read_fr3);
		cbs[2].setTag("3");
		cbs[3]=(CheckBox)taskView.findViewById(R.id.read_fr1);
		cbs[3].setTag("1");

		llTaskContainer.addView(taskView);
	}


	@Override
	public void onClick(View v) {
		cancel();
		switch(v.getId()){
		case R.id.read_cancel:
			if(defaultListener!=null){
				defaultListener.onCancel("不需要");
			}
			break;
		case R.id.read_confirm:
			if(defaultListener!=null){
				days.setLength(0);
				for(int i=0;i<4;i++){
					if(cbs[i].isChecked()){
						days.append(cbs[i].getTag().toString()).append(",");
					}
				}
				if(days.length()>1)days.setLength(days.length()-1);
				defaultListener.onConfirm(days.toString());
			}
			break;
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if(context.getWindow().getAttributes().screenBrightness==0.01f){
			LayoutParams params=context.getWindow().getAttributes();
			params.screenBrightness= LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
			context.getWindow().setAttributes(params);
		}
		return super.dispatchTouchEvent(ev);
	}
	
	 public interface OnRemindAheadListener {
	        void onConfirm(String days);
	        void onCancel(String text);
	 }

}
