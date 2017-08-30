package com.lingju.assistant.view.base;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.event.ChatMsgEvent;
import com.lingju.assistant.activity.event.RecordUpdateEvent;
import com.lingju.assistant.activity.event.RobotTipsEvent;
import com.lingju.assistant.view.VoiceComponent;
import com.lingju.model.SimpleDate;
import com.lingju.model.temp.speech.ResponseMsg;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


/**
 * Created by Ken on 2016/10/20.
 */
public abstract class BaseEditDialog extends Dialog {

    private VoiceComponent voiceButton;
    private LinearLayout mLlTaskContainer;
    private TextView tipsText;
    private TextView inputText;
    public Activity mContext;

    public BaseEditDialog(Context context) {
        super(context);
        this.mContext = (Activity) context;
    }

    public BaseEditDialog(Context context, int theme) {
        super(context, theme);
        this.mContext = (Activity) context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.base_edit_dialog);
        voiceButton = (VoiceComponent) findViewById(R.id.input_voice_bt);
        //        voiceButton.setHeadSetMode(((MainActivity) context).isHeadset);
        tipsText = (TextView) findViewById(R.id.main_input_tips);
        inputText = (TextView) findViewById(R.id.task_dialog_text);
        EventBus.getDefault().register(this);

        mLlTaskContainer = (LinearLayout) findViewById(R.id.ll_task_container);
        initTaskView(mLlTaskContainer);
    }

    public void setInputText(String input) {
        if (!TextUtils.isEmpty(input))
            inputText.setText(input);
    }

    /**
     * 初始化任务对话框视图，让子类必须具体实现
     **/
    protected abstract void initTaskView(LinearLayout llTaskContainer);

    /**
     * 不确定方法，子类可以选择实现
     **/
    public void setTime(SimpleDate time) {
    }

    public void setFrequency(int fr) {
    }

    public void setRing(String ring, String path) {
    }

    public boolean confirm() {
        return false;
    }

    public void append(String memo) {
    }

    public void setTime(long time) {
    }

    public void setProject(String proText) {
    }

    public void setAmount(double amount) {
    }

    /**
     * new start
     **/
    @Override
    public void cancel() {
        super.cancel();
        EventBus.getDefault().unregister(this);
        if (mDialogListener != null) {
            mDialogListener.onDialogCancel();
            mDialogListener = null;
        }
    }

    @Override
    public void show() {
        Window dialogWindow = getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        dialogWindow.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        WindowManager m = mContext.getWindowManager();
        Display d = m.getDefaultDisplay(); // 获取屏幕宽、高用
        lp.width = (int) (d.getWidth() * 0.8);
        lp.y = 80; // 新位置Y坐标
        dialogWindow.setAttributes(lp);
        super.show();
        if (mDialogListener != null) {
            mDialogListener.onDialogShow();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(mContext.getWindow().getAttributes().screenBrightness==0.01f){
            WindowManager.LayoutParams params=mContext.getWindow().getAttributes();
            params.screenBrightness= WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            mContext.getWindow().setAttributes(params);
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * new end 刷新录音话筒图标状态
     **/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateVoiceState(RecordUpdateEvent e) {
        switch (e.getState()) {
            case RecordUpdateEvent.RECORD_IDLE:
            case RecordUpdateEvent.RECORD_IDLE_AFTER_RECOGNIZED:
                voiceButton.setRecordIdleState();
                break;
            case RecordUpdateEvent.RECORDING:
                voiceButton.setRecordStartState();
                break;
            case RecordUpdateEvent.RECOGNIZING:
                voiceButton.setRecognizeCompletedState();
                break;
        }
        //        voiceButton.setHeadSetMode(((MainActivity) context).isHeadset);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showMicButtonTips(RobotTipsEvent tips) {
        if (TextUtils.isEmpty(tips.getText())) {
            tipsText.setText(mContext.getResources().getString(R.string.click_mic_tips));
        } else {
            tipsText.setText(tips.getText());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showInputContent(ChatMsgEvent event) {
        if (event.textMsg != null) {
            if (event.textMsg instanceof ResponseMsg)
                return;
            inputText.setText(event.textMsg.text);
        }
    }

    /**
     * 对话框变化监听器
     **/
    public interface DialogChangelListener {
        /**
         * 对话框消失时响应
         **/
        void onDialogCancel();

        /**
         * 对话框显示时响应
         **/
        void onDialogShow();
    }

    private DialogChangelListener mDialogListener;

    public void setOnDialogChangelListener(DialogChangelListener listener) {
        this.mDialogListener = listener;
    }
}
