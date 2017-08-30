package com.lingju.assistant.view;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.service.RemindService;
import com.lingju.assistant.view.base.BaseEditDialog;
import com.lingju.model.Remind;
import com.lingju.model.SimpleDate;
import com.lingju.model.dao.AssistDao;
import com.lingju.util.AssistUtils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class RemindEditDialog extends BaseEditDialog implements View.OnClickListener{
	private OnRemindEditListener defaultListener;
	private Remind remind;
	private EditText contentText;
	private TextView timeText;
	private TextView dateText;
	private TextView frText;

	private SimpleDate dt=new SimpleDate();
	private Calendar dd=Calendar.getInstance();
	private int fr=0;
	int aheads[];
	String msg;
	private SimpleDateFormat sf=new SimpleDateFormat("yyyy年MM月dd日");

	public RemindEditDialog(Activity context, Remind remind, int aheads[], String msg, OnRemindEditListener defaultListener) {
		super(context, R.style.lingju_dialog1);
		setCancelable(false);
		this.remind=remind;
		this.defaultListener=defaultListener;
		this.aheads=aheads;
		this.msg=msg;
	}

	public RemindEditDialog(Activity context, Remind remind, OnRemindEditListener defaultListener) {
		super(context, R.style.lingju_dialog1);
		setCancelable(false);
		this.remind=remind;
		this.defaultListener=defaultListener;
	}

	public void setTime(SimpleDate time) {
		dt=time;
		timeText.setText(dt.toString());
	}

	public void setDate(long date){
		dd.setTimeInMillis(date);
		dateText.setText(sf.format(new Date(dd.getTimeInMillis())));
		frText.setText(AssistUtils.translateRemindFrequency(fr,dd));
	}

	public void setFrequency(int fr){
		this.fr=fr;
		frText.setText(AssistUtils.translateRemindFrequency(fr,dd));
	}


	public void setDefaultEditListener(OnRemindEditListener listener) {
		this.defaultListener=listener;
	}

	@Override
	public void show() {
		Window dialogWindow =getWindow();
        LayoutParams lp = dialogWindow.getAttributes();
        dialogWindow.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.TOP);
        WindowManager m = mContext.getWindowManager();
        Display d = m.getDefaultDisplay(); // 获取屏幕宽、高用
        lp.width = (int) (d.getWidth() * 0.8);
        lp.y = 80; // 新位置Y坐标
        dialogWindow.setAttributes(lp);
		super.show();
	}

	@Override
	protected void initTaskView(LinearLayout llTaskContainer) {
		View taskView = LayoutInflater.from(mContext).inflate(R.layout.remind_edit_dialog, null);
		contentText=(EditText) taskView.findViewById(R.id.red_content);
		timeText=(TextView) taskView.findViewById(R.id.red_time);
		dateText=(TextView) taskView.findViewById(R.id.red_date);
		frText=(TextView) taskView.findViewById(R.id.red_fr);

		taskView.findViewById(R.id.red_close).setOnClickListener(this);
		taskView.findViewById(R.id.red_cancel).setOnClickListener(this);
		taskView.findViewById(R.id.red_confirm).setOnClickListener(this);
		timeText.setOnClickListener(this);
		dateText.setOnClickListener(this);
		frText.setOnClickListener(this);
		if(remind!=null){
			fr=remind.getFrequency();
			dt=new SimpleDate(remind.getRtime());
			dd.setTimeInMillis(remind.getRdate().getTime());
			contentText.setText(remind.getContent());
		}
		timeText.setText(dt.toString());
		if(aheads==null){
			dateText.setText(sf.format(new Date(dd.getTimeInMillis())));
			frText.setText(AssistUtils.translateRemindFrequency(fr,dd));
		}
		else{
			taskView.findViewById(R.id.red_date_box).setVisibility(View.GONE);
			TextView msgT=(TextView)taskView.findViewById(R.id.red_msg);
			msgT.setText(msg);
			msgT.setVisibility(View.VISIBLE);
		}

		llTaskContainer.addView(taskView);
	}

	@Override
	public boolean confirm(){
		if(remind==null){
			remind=new Remind();
		}
		if(TextUtils.isEmpty(contentText.getText())){
			if(defaultListener!=null){
				defaultListener.onError("请输入提醒内容");
			}
			return false;
		}
		remind.setContent(contentText.getText().toString());
		remind.setCreated(new Timestamp(System.currentTimeMillis()));
		remind.setRtime(dt.toString());
		if(aheads==null){
			aheads=new int[]{0};
		}
		for(int i=0;i<aheads.length;i++){
			remind.setRdate(new Timestamp(dd.getTimeInMillis()-24*3600000*aheads[i]));
			remind.setFrequency(fr);
			AssistDao.getInstance().insertRemind(remind);
		}
		if(aheads.length==1&&aheads[0]==0){
			remind = AssistDao.getInstance().findRemindNewCreated();
			Intent rIntent=new Intent(mContext, RemindService.class);
			rIntent.putExtra(RemindService.CMD, (RemindService.REMIND<<4)+ RemindService.ADD);
			rIntent.putExtra(RemindService.ID, remind.getId());
			mContext.startService(rIntent);
		}
		cancel();
		return true;
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.red_cancel:
		case R.id.red_close:
			cancel();
			if(defaultListener!=null){
				defaultListener.onCancel();
			}
			break;
		case R.id.red_confirm:
			if(confirm()&&defaultListener!=null){
				defaultListener.onConfirm();
			}
			break;
		case R.id.red_date:
			if(defaultListener!=null){
				defaultListener.changeDate(dd.getTimeInMillis());
			}
			break;
		case R.id.red_time:
			if(defaultListener!=null){
				defaultListener.changeTime(dt.toValue());
			}
			break;
		case R.id.red_fr:
			if(defaultListener!=null){
				defaultListener.changeFrquency(fr,dd);
			}
			break;
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if(mContext.getWindow().getAttributes().screenBrightness==0.01f){
			LayoutParams params=mContext.getWindow().getAttributes();
			params.screenBrightness= LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
			mContext.getWindow().setAttributes(params);
		}
		return super.dispatchTouchEvent(ev);
	}

	 public interface OnRemindEditListener {
		 	public void onError(String msg);
	        public void onConfirm();
	        public void onCancel();
	        public void changeTime(int amount);
	        public void changeDate(long date);
	        public void changeFrquency(int frequency, Calendar date);
	 }

}
