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
import com.lingju.assistant.view.CommonDialog;
import com.lingju.assistant.view.VoiceInputComponent;
import com.lingju.model.Memo;
import com.lingju.model.dao.AssistDao;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2016/12/19.
 */
public class MemoEditActivity extends GoBackActivity {

    public final static int FOR_MEMO_EDIT = 5;
    @BindView(R.id.tv_date)
    TextView mTvDate;
    @BindView(R.id.tv_time)
    TextView mTvTime;
    @BindView(R.id.ame_content)
    EditText mAmeContent;
    @BindView(R.id.ame_voice_input)
    VoiceInputComponent mAmeVoiceInput;
    private Memo memo;
    private Intent intent;
    private long editTime = System.currentTimeMillis();
    /**
     * 文本光标索引
     **/
    private int mCursorIndex;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memo_edit);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        mAmeContent.addTextChangedListener(mTextWatcher);
        mAmeVoiceInput.setOnResultListener(onResultListener);
        intent = getIntent();
        if (intent != null) {
            long id = intent.getLongExtra("id", 0);
            if (id > 0) {
                memo = AssistDao.getInstance().findMemoById(id);
                mAmeContent.setText(memo.getContent());
                mAmeContent.post(new Runnable() {
                    @Override
                    public void run() {
                        /* 等该View加载完成后设置光标位置 */
                        mAmeContent.setSelection(memo.getContent().length());
                        mCursorIndex = memo.getContent().length();
                    }
                });
                /* 获取上次修改时间，若无上次修改则使用创建时间 */
                editTime = memo.getModified() == null ? memo.getCreated().getTime() : memo.getModified().getTime();
            }
        }
        String formatTime = new SimpleDateFormat("yyyy.MM.dd HH:mm").format(new Date(editTime));
        String[] split = formatTime.split(" ");
        mTvDate.setText(split[0]);
        mTvTime.setText(split[1]);
    }

    /**
     * 刷新录音话筒图标状态
     **/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRecordUpdateEvent(RecordUpdateEvent e) {
        mAmeVoiceInput.setVoiceButton(e.getState());
        //		vic.setHeadSetMode(AssistantApplication.isHeadSet);
    }

    /**
     * 耳机模式单击按键开始录音
     **/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRecordEvent(AdditionAppendEvent event) {
        /* 模拟话筒点击，开始识别 */
        mAmeVoiceInput.switchRecord();
    }

    /**
     * 文本观察者
     **/
    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            mCursorIndex = mAmeContent.getSelectionStart();
        }
    };

    @OnClick({R.id.ame_back, R.id.ame_del, R.id.ame_content})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ame_back:
                if (mAmeVoiceInput.isRecording()) {
                    new CommonDialog(MemoEditActivity.this, "温馨提示", "离开编辑页面前，请先关闭录音识别哦！", "知道了").show();
                    return;
                }
                String content = mAmeContent.getText().toString().trim();
                if (!TextUtils.isEmpty(content)) {
                    if (memo == null) {
                        memo = new Memo();
                        memo.setContent(content);
                        memo.setCreated(new Date(System.currentTimeMillis()));
                        AssistDao.getInstance().insertMemo(memo);
                    } else {
                        memo.setContent(content);
                        memo.setModified(new Date(System.currentTimeMillis()));
                        AssistDao.getInstance().updateMemo(memo);
                    }
                    if (intent != null) {
                        setResult(FOR_MEMO_EDIT, intent);
                    }
                }
                goBack();
                break;
            case R.id.ame_del:
                if (mAmeVoiceInput.isRecording()) {
                    new CommonDialog(MemoEditActivity.this, "温馨提示", "离开编辑页面前，请先关闭录音识别哦！", "知道了").show();
                    return;
                }
                if (memo != null) {
                    AssistDao.getInstance().deleteMemo(memo);
                    if (intent != null)
                        setResult(FOR_MEMO_EDIT, intent);
                }
                goBack();
                break;
            case R.id.ame_content:
                mCursorIndex = mAmeContent.getSelectionStart();
                break;
        }
    }


    /**
     * 录音识别结果回调
     **/
    private VoiceInputComponent.OnResultListener onResultListener = new VoiceInputComponent.OnResultListener() {

        @Override
        public void onResult(String text) {
            if (TextUtils.isEmpty(text)) {
                return;
            }
            mAmeContent.getText().insert(mCursorIndex, text);
            mAmeContent.setSelection(mCursorIndex);
        }

        @Override
        public void onError(int errorCode, String description) {
            new CommonDialog(MemoEditActivity.this, "错误提示", description, "确定").show();
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mAmeVoiceInput.isRecording()) {
                new CommonDialog(MemoEditActivity.this, "温馨提示", "离开编辑页面前，请先关闭录音识别哦！", "知道了").show();
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
}
