package com.lingju.assistant.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.MainActivity;
import com.lingju.assistant.activity.event.RecordUpdateEvent;
import com.lingju.common.log.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


public class NaviVoiceInputDialog extends Dialog implements View.OnClickListener {
    private final static String TAG = "NaviVoiceInputDialog";
    private Activity context;
    private VoiceComponent voiceBt;
    private final String tips[] = new String[]{
            "查看全程路线",
            "附近的加油站",
            "导航去广州塔"
    };
    private final TextView tipsText[] = new TextView[3];
/*
    public NaviVoiceInputDialog(Activity context) {
		this(context);
	}*/

    public NaviVoiceInputDialog(Activity context, String... tips) {
        super(context, R.style.full_dialog);
        setCancelable(false);
        this.context = context;
        if (tips != null && tips.length > 0) {
            for (int i = 0; i < this.tips.length; i++) {
                this.tips[i] = null;
            }
            for (int i = 0; i < tips.length; i++) {
                this.tips[i] = tips[i];
                if ((i + 1) >= this.tips.length) {
                    break;
                }
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_navi_voice_input);
        voiceBt = (VoiceComponent) findViewById(R.id.dnvi_voice_bt);
        tipsText[0] = (TextView) findViewById(R.id.dnvi_tips1);
        tipsText[1] = (TextView) findViewById(R.id.dnvi_tips2);
        tipsText[2] = (TextView) findViewById(R.id.dnvi_tips3);
        for (int i = 0; i < tips.length; i++) {
            if (!TextUtils.isEmpty(tips[i])) {
                tipsText[i].setText(tips[i]);
                tipsText[i].setVisibility(View.VISIBLE);
            } else {
                tipsText[i].setVisibility(View.GONE);
            }
        }
        findViewById(R.id.dnvi_close_bt).setOnClickListener(this);
        findViewById(R.id.dnvi_more_bt).setOnClickListener(this);
    }

    @Override
    public void dismiss() {
        EventBus.getDefault().unregister(this);
        super.dismiss();
    }

    @Override
    public void onBackPressed() {
        Log.i(TAG, "onBackPressed");
        dismiss();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRecordEvent(RecordUpdateEvent e) {
        switch (e.getState()) {
            case RecordUpdateEvent.RECORD_IDLE:
                voiceBt.setRecordIdleState();
                break;
            case RecordUpdateEvent.RECORDING:
                voiceBt.setRecordStartState();
                break;
            case RecordUpdateEvent.RECOGNIZING:
                voiceBt.setRecognizeCompletedState();
                break;
            case RecordUpdateEvent.RECORD_IDLE_AFTER_RECOGNIZED:
                voiceBt.setRecordIdleState();
                dismiss();
                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
                // context.overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
                break;
        }
    }

/*	private int dp2px(int dp){
        return (int)(0.5f+context.getResources().getDisplayMetrics().density*(float)dp);
	}*/

    @Override
    public void onClick(View v) {
        cancel();
        switch (v.getId()) {
            case R.id.dnvi_more_bt:
                Intent intent = new Intent(context, MainActivity.class);
                intent.putExtra(MainActivity.RESULT_CODE, MainActivity.FOR_INTRODUCE);
                context.startActivity(intent);
                break;
            case R.id.dnvi_close_bt:
                voiceBt.onRecord();
                break;
        }
    }

    @Override
    public void show() {
        Window dialogWindow = getWindow();
        LayoutParams lp = dialogWindow.getAttributes();
        lp.width = LayoutParams.MATCH_PARENT;
        lp.height = LayoutParams.MATCH_PARENT;
        /*WindowManager m = context.getWindowManager();
        Display d = m.getDefaultDisplay(); // 获取屏幕宽、高用
        lp.width = d.getWidth();*/
        dialogWindow.setAttributes(lp);
        EventBus.getDefault().register(this);
        super.show();
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
        voiceBt.onRecord();
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
    }
}
