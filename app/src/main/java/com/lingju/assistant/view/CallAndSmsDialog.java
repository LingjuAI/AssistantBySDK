package com.lingju.assistant.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.player.event.UpdateWaittingSeekBarEvent;
import com.lingju.assistant.service.process.MobileCommProcessor;
import com.lingju.assistant.view.base.BaseEditDialog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2017/1/3.
 */
public class CallAndSmsDialog extends BaseEditDialog {

    @BindView(R.id.main_task_selector)
    LingjuCheckListBox mMainTaskSelector;
    @BindView(R.id.main_confirm_call_title)
    TextView mMainConfirmCallTitle;
    @BindView(R.id.main_confirm_call_name)
    TextView mMainConfirmCallName;
    @BindView(R.id.main_confirm_call_number)
    TextView mMainConfirmCallNumber;
    @BindView(R.id.main_confirm_call_job)
    TextView mMainConfirmCallJob;
    @BindView(R.id.main_confirm_call_company)
    TextView mMainConfirmCallCompany;
    @BindView(R.id.main_confirm_call_seekbar)
    SeekBar mMainConfirmCallSeekbar;
    @BindView(R.id.main_confirm_call_dialog)
    LinearLayout mMainConfirmCallDialog;
    @BindView(R.id.main_confirm_sms_title)
    TextView mMainConfirmSmsTitle;
    @BindView(R.id.main_confirm_sms_name)
    TextView mMainConfirmSmsName;
    @BindView(R.id.main_confirm_sms_content)
    TextView mMainConfirmSmsContent;
    @BindView(R.id.main_confirm_sms_seekbar)
    SeekBar mMainConfirmSmsSeekbar;
    @BindView(R.id.main_confirm_sms_1)
    TextView mMainConfirmSms1;
    @BindView(R.id.main_confirm_sms_2)
    TextView mMainConfirmSms2;
    @BindView(R.id.main_confirm_sms_3)
    TextView mMainConfirmSms3;
    @BindView(R.id.main_confirm_sms_dialog)
    LinearLayout mMainConfirmSmsDialog;
    @BindView(R.id.main_confirm_call_true)
    TextView mMainConfirmCallTrue;
    @BindView(R.id.main_confirm_call_false)
    TextView mMainConfirmCallFalse;
    private OnCallDialogListener mDialogListener;
    private Timer waitActionTimer;      //电话、短信任务倒计时定时器
    private int type = -1;       //对话框类型

    public CallAndSmsDialog(Context context, OnCallDialogListener listener) {
        super(context, R.style.lingju_dialog1);
        this.mDialogListener = listener;
    }

    @Override
    protected void initTaskView(LinearLayout llTaskContainer) {
        View callView = View.inflate(mContext, R.layout.dialog_call_sms, null);
        ButterKnife.bind(this, callView);
        mMainTaskSelector.setCheckListener(checkedListener);
        llTaskContainer.addView(callView);
    }

    @OnClick({R.id.main_confirm_call_close, R.id.main_confirm_call_true, R.id.main_confirm_call_false, R.id.main_confirm_sms_close,
            R.id.main_confirm_sms_1, R.id.main_confirm_sms_2, R.id.main_confirm_sms_3})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.main_confirm_call_close:
            case R.id.main_confirm_sms_close:
                if (mDialogListener != null)
                    mDialogListener.onCancel(true);
                break;
            case R.id.main_confirm_call_true:
                if (type == MobileCommProcessor.WaittingForCall) {
                    if (mDialogListener != null) {
                        mDialogListener.onConfirmCall();
                        cancelWaitActionTimer();
                    }
                    return;
                }
                if (mDialogListener != null && view instanceof TextView)
                    mDialogListener.onClick(((TextView) view).getText().toString());
                break;
            case R.id.main_confirm_call_false:
            case R.id.main_confirm_sms_1:
            case R.id.main_confirm_sms_2:
            case R.id.main_confirm_sms_3:
                if (mDialogListener != null && view instanceof TextView)
                    mDialogListener.onClick(((TextView) view).getText().toString());
                cancelWaitActionTimer();
                break;
        }
    }

    @Override
    public void cancel() {
        cancelWaitActionTimer();
        super.cancel();
    }

    /**
     * 定时器是否还存在
     **/
    public boolean timerIsAlive() {
        return waitActionTimer != null;
    }

    /**
     * 指定类型对话框的进度条进度刷新事件处理
     **/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUpdateSeekBarEvent(UpdateWaittingSeekBarEvent event) {
        /*if (event.getType() > -1) {
            cancelWaitActionTimer();
            waitActionTimer = new Timer();
            waitActionTimer.schedule(new WaitActionTimerTask(event.getType()), 250, 250);
        }*/
    }

    public void cancelWaitActionTimer() {
        if (waitActionTimer != null) {
            waitActionTimer.cancel();
            waitActionTimer = null;
        }
    }


    /**
     * 设置对话框显示类型
     *
     * @param type     显示类型
     * @param contents 显示内容数组
     **/
    public void setDialogType(int type, String[] contents) {
        this.type = type;
        if (contents == null || contents.length == 0)
            return;
        /* 先将对话框视图全部隐藏 */
        mMainTaskSelector.setVisibility(View.GONE);
        mMainConfirmCallDialog.setVisibility(View.GONE);
        mMainConfirmSmsDialog.setVisibility(View.GONE);
        String title = "";
        if (type >> 3 == MobileCommProcessor.CallDialogType) {
            int cl = contents.length;
            if (cl < 2)
                return;
            mMainConfirmCallName.setText(contents[0].equals("null") ? "未知" : contents[0]);
            mMainConfirmCallNumber.setText(contents[1]);
            if (contents[0].length() > 4) {
                mMainConfirmCallName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 36);
                mMainConfirmCallName.setLines(2);
                mMainConfirmCallName.setEllipsize(TextUtils.TruncateAt.END);
            } else {
                mMainConfirmCallName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 60);
                mMainConfirmCallName.setLines(1);
            }
            if (cl >= 3 && !TextUtils.isEmpty(contents[2]) && !contents[2].equals("null")) {
                mMainConfirmCallCompany.setText("公司：" + contents[2]);
                mMainConfirmCallCompany.setVisibility(View.VISIBLE);
            } else {
                mMainConfirmCallCompany.setVisibility(View.GONE);
            }
            if (cl == 4 && !TextUtils.isEmpty(contents[3]) && !contents[3].equals("null")) {
                mMainConfirmCallJob.setText("职务：" + contents[3]);
                mMainConfirmCallJob.setVisibility(View.VISIBLE);
            } else {
                mMainConfirmCallJob.setVisibility(View.GONE);
            }
            mMainConfirmCallSeekbar.setProgress(0);
            mMainConfirmCallSeekbar.setVisibility(View.GONE);
            title = "打电话";
            mMainConfirmCallFalse.setVisibility(View.VISIBLE);
            mMainConfirmCallTrue.setVisibility(View.VISIBLE);
            mMainConfirmCallFalse.setText("取消呼叫");
            mMainConfirmCallTrue.setText("确定呼叫");
            switch (type) {
                case MobileCommProcessor.WaittingForCall:
                    mMainConfirmCallSeekbar.setVisibility(View.VISIBLE);
                    break;
                case MobileCommProcessor.ConfirmNameCall:
                    mMainConfirmCallFalse.setText("错误");
                    mMainConfirmCallTrue.setText("正确");
                    break;
                case MobileCommProcessor.ConfirmNameSms:
                    mMainConfirmCallFalse.setText("错误");
                    mMainConfirmCallTrue.setText("正确");
                    title = "发短信";
                    break;
                case MobileCommProcessor.ConfirmLastCall:
                    title = "刚才的电话";
                    mMainConfirmCallFalse.setVisibility(View.GONE);
                    mMainConfirmCallTrue.setText("呼叫");
                    break;
            }
            mMainConfirmCallTitle.setText(title);
            mMainConfirmCallDialog.setVisibility(View.VISIBLE);
        } else if (type >> 3 == MobileCommProcessor.SmsDialogType) {
            title = "发短信";
            if (contents.length >= 2) {
                mMainConfirmSmsName.setText(contents[0]);
                if (contents[0].length() > 3) {
                    mMainConfirmSmsName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 36);
                    mMainConfirmSmsName.setLines(2);
                } else {
                    mMainConfirmSmsName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 60);
                    mMainConfirmSmsName.setLines(1);
                }
                mMainConfirmSmsContent.setText(TextUtils.isEmpty(contents[contents.length - 1]) || contents[contents.length - 1].equals("null") ? "" : contents[contents.length - 1]);
            } else
                return;
            mMainConfirmSmsSeekbar.setProgress(0);
            mMainConfirmSmsSeekbar.setVisibility(View.GONE);
            switch (type) {
                case MobileCommProcessor.WaittingForSend:
                    mMainConfirmSms1.setText("取消发送");
                    mMainConfirmSms1.setVisibility(View.VISIBLE);
                    mMainConfirmSms2.setVisibility(View.GONE);
                    mMainConfirmSms3.setVisibility(View.GONE);
                    mMainConfirmSmsSeekbar.setVisibility(View.VISIBLE);
                    break;
                case MobileCommProcessor.ConfirmForSend:
                    mMainConfirmSms1.setVisibility(View.VISIBLE);
                    mMainConfirmSms2.setVisibility(View.VISIBLE);
                    mMainConfirmSms3.setVisibility(View.VISIBLE);
                    mMainConfirmSms1.setText("添加");
                    mMainConfirmSms2.setText("重新输入");
                    mMainConfirmSms3.setText("发送");
                    break;
                case MobileCommProcessor.ConfirmLastMsg:
                    mMainConfirmSms1.setVisibility(View.GONE);
                    mMainConfirmSms2.setVisibility(View.VISIBLE);
                    mMainConfirmSms3.setVisibility(View.VISIBLE);
                    mMainConfirmSms2.setText("回复");
                    mMainConfirmSms3.setText("朗读");
                    title = "最新短信";
                    break;
            }
            mMainConfirmSmsTitle.setText(title);
            mMainConfirmSmsDialog.setVisibility(View.VISIBLE);
        } else if (type >> 3 == MobileCommProcessor.CheckListDialogType) {
            switch (type) {
                case MobileCommProcessor.CheckForNameCall:
                    title = "选择联系人";
                    break;
                case MobileCommProcessor.CheckForNumCall:
                    title = "选择号码";
                    break;
                case MobileCommProcessor.CheckForNameSms:
                    title = "选择收件人";
                    break;
                case MobileCommProcessor.CheckForNumSms:
                    title = "选择收件号码";
                    break;
            }
            mMainTaskSelector.setList(contents, title);
            mMainTaskSelector.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 拨号、发短信确认倒计时任务
     **/
    private class WaitActionTimerTask extends TimerTask {
        private int type;   //0:拨号  1：发短信
        private int percent;

        public WaitActionTimerTask(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }

        @Override
        public void run() {
            if (type == 0) {
                percent = mMainConfirmCallSeekbar.getProgress() + 5;
                if (percent > 100) {
                    cancelWaitActionTimer();
                    if (mDialogListener != null) {
                        mDialogListener.onCancel(false);
                    }
                } else
                    mMainConfirmCallSeekbar.setProgress(percent);
            } else {
                percent = mMainConfirmSmsSeekbar.getProgress() + 5;
                if (percent > 100) {
                    cancelWaitActionTimer();
                    if (mDialogListener != null) {
                        mDialogListener.onCancel(false);
                    }
                } else
                    mMainConfirmSmsSeekbar.setProgress(percent);
            }
        }
    }

    /**
     * 联系人列表监听器
     **/
    private LingjuCheckListBox.CheckedListener checkedListener = new LingjuCheckListBox.CheckedListener() {
        @Override
        public void checked(int position) {
            if (mDialogListener != null) {
                mDialogListener.onClick("第" + (position + 1) + "个");
            }
        }

        @Override
        public void onClose(boolean notify) {
            if (mDialogListener != null) {
                mDialogListener.onCancel(notify);
            }
        }
    };

    public interface OnCallDialogListener {
        /**
         * 取消对话框
         **/
        void onCancel(boolean cancelTask);

        /**
         * 对话框按钮点击
         *
         * @param btn 按钮文本
         **/
        void onClick(String btn);

        /**
         * 确定呼叫
         **/
        void onConfirmCall();
    }
}
