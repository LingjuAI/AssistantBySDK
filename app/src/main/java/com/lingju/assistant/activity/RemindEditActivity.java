package com.lingju.assistant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.activity.event.AdditionAppendEvent;
import com.lingju.assistant.activity.event.RecordUpdateEvent;
import com.lingju.assistant.service.RemindService;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.assistant.view.VoiceInputComponent;
import com.lingju.model.Remind;
import com.lingju.model.SimpleDate;
import com.lingju.model.dao.AssistDao;
import com.lingju.util.AssistUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class RemindEditActivity extends GoBackActivity {

    public final static int FOR_REMIND_EDIT = 4;
    @BindView(R.id.tv_date)
    TextView mTvDate;
    @BindView(R.id.tv_time)
    TextView mTvTime;
    @BindView(R.id.are_voice_input)
    VoiceInputComponent mAreVoiceInput;
    @BindView(R.id.are_content)
    EditText mAreContent;
    @BindView(R.id.are_date)
    TextView mAreDate;
    @BindView(R.id.are_time)
    TextView mAreTime;
    @BindView(R.id.are_fr)
    TextView mAreFr;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
    private SimpleDate dt = new SimpleDate();
    private Calendar dd = Calendar.getInstance();
    private Date updateTime = new Date(System.currentTimeMillis());
    private int fr = 0;

    private Remind remind;
    private Intent intent;
    private int mCursorIndex;
    private AssistDao mAssistDao;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remind_edit);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        mAssistDao = AssistDao.getInstance();
        mAreVoiceInput.setOnResultListener(onResultListener);
        mAreContent.addTextChangedListener(mTextWatcher);
        intent = getIntent();
        if (intent != null) {
            long id = intent.getLongExtra("id", 0);
            if (id > 0) {
                remind = mAssistDao.findRemindById(id);
                fr = remind.getFrequency();
                dt = new SimpleDate(remind.getRtime());
                dd.setTimeInMillis(remind.getRdate().getTime());
                updateTime = remind.getCreated();
                mAreContent.setText(remind.getContent());
                mAreContent.post(new Runnable() {
                    @Override
                    public void run() {
                        mCursorIndex = remind.getContent().length();
                        mAreContent.setSelection(mCursorIndex);
                    }
                });
            }
        }

        mAreTime.setText(dt.toString());
        mAreDate.setText(sdf.format(dd.getTimeInMillis()));
        mAreFr.setText(AssistUtils.translateRemindFrequency(fr, dd));
        String formatTime = new SimpleDateFormat("yyyy.MM.dd HH:mm").format(updateTime);
        String[] split = formatTime.split(" ");
        mTvDate.setText(split[0]);
        mTvTime.setText(split[1]);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            switch (resultCode) {
                case TimePickerActivity.FOR_TIME_RESULT:
                    int v = data.getIntExtra(TimePickerActivity.TIME, -1);
                    if (v > -1) {
                        dt = new SimpleDate(v);
                        mAreTime.setText(dt.toString());
                    }
                    break;
                case DatePickerActivity.FOR_DATE_RESULT:
                    dd.setTimeInMillis(data.getLongExtra(DatePickerActivity.DATE, System.currentTimeMillis()));
                    mAreDate.setText(sdf.format(new Date(dd.getTimeInMillis())));
                    break;
                case RemindFrSettingActivity.FOR_FR:
                    fr = data.getIntExtra(RemindFrSettingActivity.TYPE, 0);
                    mAreFr.setText(AssistUtils.translateRemindFrequency(fr, dd));
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mAreVoiceInput.isRecording()) {
                new CommonDialog(RemindEditActivity.this, "温馨提示", "离开编辑页面前，请先关闭录音识别哦！", "知道了").show();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    /**
     * 刷新录音话筒图标状态
     **/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBtnUpdateEvent(RecordUpdateEvent e) {
        mAreVoiceInput.setVoiceButton(e.getState());
        //        vic.setHeadSetMode(AssistantApplication.isHeadSet);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRecordEvent(AdditionAppendEvent event) {
        /* 模拟话筒点击，开始识别 */
        mAreVoiceInput.switchRecord();
    }

    @OnClick({R.id.are_back, R.id.are_cancel, R.id.are_del, R.id.are_confirm,
            R.id.are_content, R.id.are_date, R.id.are_time, R.id.are_fr})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.are_back:
            case R.id.are_cancel:
                if (mAreVoiceInput.isRecording()) {
                    new CommonDialog(RemindEditActivity.this, "温馨提示", "离开编辑页面前，请先关闭录音识别哦！", "知道了").show();
                    return;
                }
                goBack();
                break;
            case R.id.are_date:
                Intent dIntent = new Intent(RemindEditActivity.this, DatePickerActivity.class);
                dIntent.putExtra(DatePickerActivity.TITLE, "我的提醒");
                dIntent.putExtra(DatePickerActivity.DATE, dd.getTimeInMillis());
                startActivityForResult(dIntent, DatePickerActivity.FOR_DATE_RESULT);
                goInto();
                break;
            case R.id.are_time:
                Intent tIntent = new Intent(RemindEditActivity.this, TimePickerActivity.class);
                tIntent.putExtra(TimePickerActivity.TITLE, "我的提醒");
                tIntent.putExtra(TimePickerActivity.TIME, dt.toValue());
                startActivityForResult(tIntent, TimePickerActivity.FOR_TIME_RESULT);
                goInto();
                break;
            case R.id.are_fr:
                Intent fIntent = new Intent(RemindEditActivity.this, RemindFrSettingActivity.class);
                fIntent.putExtra(RemindFrSettingActivity.TYPE, fr);
                fIntent.putExtra(RemindFrSettingActivity.DATE, dd.getTimeInMillis());
                startActivityForResult(fIntent, RemindFrSettingActivity.FOR_FR);
                goInto();
                break;
            case R.id.are_del:
                if (mAreVoiceInput.isRecording()) {
                    new CommonDialog(RemindEditActivity.this, "温馨提示", "离开编辑页面前，请先关闭录音识别哦！", "知道了").show();
                    return;
                }
                if (remind != null && remind.getId() != null) {
                    Intent rIntent = new Intent(RemindEditActivity.this, RemindService.class);
                    rIntent.putExtra(RemindService.CMD, (RemindService.REMIND << 4) + RemindService.CANCEL);
                    rIntent.putExtra(RemindService.ID, remind.getId());
                    mAssistDao.deleteRemind(remind);
                    startService(rIntent);
                    if (intent != null) {
                        setResult(FOR_REMIND_EDIT, intent);
                    }
                }
                goBack();
                break;
            case R.id.are_confirm:
                if (mAreVoiceInput.isRecording()) {
                    new CommonDialog(RemindEditActivity.this, "温馨提示", "离开编辑页面前，请先关闭录音识别哦！", "知道了").show();
                    return;
                }
                String content = mAreContent.getText().toString().trim();
                if (TextUtils.isEmpty(content)) {
                    new CommonDialog(RemindEditActivity.this, "错误提示", "请输入提醒内容！", "确定").show();
                    return;
                }
                if (remind == null) {
                    remind = new Remind();
                }
                /* 填充提醒记录 */
                remind.setContent(content);
                remind.setValid(1);
                remind.setCreated(new Date(System.currentTimeMillis()));
                remind.setRdate(dd.getTime());
                remind.setRtime(dt.toString());
                remind.setFrequency(fr);
                if (remind.getId() == null) {
                    mAssistDao.insertRemind(remind);
                    remind = mAssistDao.findRemindNewCreated();
                } else {
                    mAssistDao.updateRemind(remind);
                }
                /* 通知提醒服务打开 */
                Intent rIntent = new Intent(RemindEditActivity.this, RemindService.class);
                rIntent.putExtra(RemindService.CMD, (RemindService.REMIND << 4) + RemindService.ADD);
                rIntent.putExtra(RemindService.ID, remind.getId());
                startService(rIntent);
                if (intent != null) {
                    setResult(FOR_REMIND_EDIT, intent);
                }
                goBack();
                break;
            case R.id.are_content:
                mCursorIndex = mAreContent.getSelectionStart();
                break;
        }
    }

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            mCursorIndex = mAreContent.getSelectionStart();
        }
    };

    private VoiceInputComponent.OnResultListener onResultListener = new VoiceInputComponent.OnResultListener() {

        @Override
        public void onResult(String text) {
            mAreContent.getText().insert(mCursorIndex, text);
            mAreContent.setSelection(mCursorIndex);
        }

        @Override
        public void onError(int errorCode, String description) {
            new CommonDialog(RemindEditActivity.this, "错误提示", description, "确定").show();
        }
    };

}
