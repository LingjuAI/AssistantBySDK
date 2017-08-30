package com.lingju.assistant.view;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.service.RemindService;
import com.lingju.assistant.view.base.BaseEditDialog;
import com.lingju.model.AlarmClock;
import com.lingju.model.SimpleDate;
import com.lingju.model.dao.AssistDao;
import com.lingju.util.AssistUtils;

import java.sql.Timestamp;

public class AlarmEditDialog extends BaseEditDialog implements View.OnClickListener{
	private OnAlarmEditListener defaultListener;
	private AlarmClock alarm;
	private TextView timeText;
	private TextView frText;
	private TextView ringText;

	private SimpleDate dt=new SimpleDate();
	private String path;
	private String ring="默认";
	private int fr=0;

	public AlarmEditDialog(Activity context, AlarmClock alarm, OnAlarmEditListener defaultListener) {
		super(context, R.style.lingju_dialog1);
		setCancelable(false);
		this.alarm=alarm;
		this.defaultListener=defaultListener;
	}

	@Override
	public void setTime(SimpleDate time) {
		dt=time;
		timeText.setText(dt.toString());
	}

	@Override
	public void setFrequency(int fr){
		this.fr=fr;
		frText.setText(AssistUtils.transalteWeekDayString(fr));
	}

	@Override
	public void setRing(String ring,String path) {
		this.ring = ring;
		this.path=path;
		ringText.setText(ring);
	}


	public void setDefaultEditListener(OnAlarmEditListener listener) {
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

		View taskView = View.inflate(mContext, R.layout.alarm_edit_dialog, null);
		timeText=(TextView) taskView.findViewById(R.id.aed_time);
		frText=(TextView) taskView.findViewById(R.id.aed_fr);
		ringText=(TextView)taskView.findViewById(R.id.aed_ring);

		taskView.findViewById(R.id.aed_close).setOnClickListener(this);
		taskView.findViewById(R.id.aed_cancel).setOnClickListener(this);
		taskView.findViewById(R.id.aed_confirm).setOnClickListener(this);
		timeText.setOnClickListener(this);
		frText.setOnClickListener(this);
		ringText.setOnClickListener(this);
		if(alarm!=null){
			fr=alarm.getFrequency();
			dt=new SimpleDate(alarm.getRtime());
		}
		timeText.setText(dt.toString());
		frText.setText(AssistUtils.transalteWeekDayString(fr));

		llTaskContainer.addView(taskView);
	}

	@Override
	public boolean confirm(){
		if(alarm==null){
			alarm=new AlarmClock();
			alarm.setCreated(new Timestamp(System.currentTimeMillis()));
		}
		if(fr!=0){
			alarm.setValid(1);
			alarm.setCreated(new Timestamp(System.currentTimeMillis()));
		}
		alarm.setRing(ring);
		if(!TextUtils.isEmpty(path)){
			alarm.setPath(path);
		}
		alarm.setRtime(dt.toValue());
		alarm.setFrequency(fr);
		if(alarm.getId()== null){
			/* 插入新的闹钟 */
			AssistDao.getInstance().insertAlarm(alarm);
			/* 查询最新插入的闹钟 */
			alarm=AssistDao.getInstance().findAlarmNewCreated();
		}
		else{
			/* 修改已存在的闹钟 */
			AssistDao.getInstance().updateAlarm(alarm);
		}
		Intent arIntent=new Intent(mContext, RemindService.class);
		arIntent.putExtra(RemindService.CMD,(RemindService.ALARM<<4)+RemindService.ADD);
		arIntent.putExtra(RemindService.ID, alarm.getId());
		mContext.startService(arIntent);
		cancel();
		return true;
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.aed_cancel:
		case R.id.aed_close:
			cancel();
			if(defaultListener!=null){
				defaultListener.onCancel();
			}
			break;
		case R.id.aed_confirm:
			confirm();
			if(defaultListener!=null){
				defaultListener.onConfirm();
			}
			break;
		case R.id.aed_time:
			if(defaultListener!=null){
				defaultListener.changeTime(dt.toValue());
			}
			break;
		case R.id.aed_ring:
			if(defaultListener!=null){
				defaultListener.onSelectRing(ring, path);
			}
			break;
		case R.id.aed_fr:
			if(defaultListener!=null){
				defaultListener.changeFrquency(fr);
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
	
	 public interface OnAlarmEditListener {
	        public void onConfirm();
	        public void onCancel();
	        public void onSelectRing(String ring, String path);
	        public void changeTime(int time);
	        public void changeFrquency(int frequency);
	 }

}
