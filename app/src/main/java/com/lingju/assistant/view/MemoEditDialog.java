package com.lingju.assistant.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.index.view.ChatListFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MemoEditDialog extends Dialog {

    @BindView(R.id.edit_memo_time)
    TextView mEditMemoTime;
    @BindView(R.id.edit_memo_count)
    TextView mEditMemoCount;
    @BindView(R.id.edit_memo_content)
    EditText mEditMemoContent;
    @BindView(R.id.tv_save)
    TextView mTvSave;
    @BindView(R.id.vic_input)
    VoiceInputComponent mVicInput;
    private Context mContext;
    private OnMemoEditListener mEditListener;
    private String content;
    private String modifyTime;
    private boolean isNewCreated;
    /**
     * 文本光标索引
     **/
    private int mCursorIndex;

    public MemoEditDialog(Context context, String content, String time, boolean isNewCreated, OnMemoEditListener listener) {
        super(context, R.style.lingju_dialog3);
        this.mContext = context;
        this.content = content;
        this.modifyTime = time;
        this.isNewCreated = isNewCreated;
        this.mEditListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_memo_edit);
        ButterKnife.bind(this);
        mEditMemoContent.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    cancel();
                    return true;
                }
                return false;
            }
        });
        mEditMemoContent.addTextChangedListener(mTextWatcher);
        mEditMemoContent.setText(content);
        mEditMemoContent.setSelection(content.length());
        mCursorIndex = content.length();
        mTvSave.setText(isNewCreated ? "创建" : "保存");
        mEditMemoTime.setText(modifyTime);
        mVicInput.setOnResultListener(onResultListener);
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
            mEditMemoCount.setText("字数" + s.length());
            mCursorIndex = mEditMemoContent.getSelectionStart();
            if (TextUtils.isEmpty(mEditMemoContent.getText().toString().trim())) {
                mTvSave.setTextColor(mContext.getResources().getColor(R.color.forbid_click_color));
            } else {
                mTvSave.setTextColor(mContext.getResources().getColor(R.color.base_blue));
            }
        }
    };

    @OnClick({R.id.edit_memo_half_screen, R.id.tv_save, R.id.tv_cancel})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.edit_memo_half_screen:
                if (mEditListener != null) {
                    mEditListener.onBack(mEditMemoContent.getText().toString());
                }
                break;
            case R.id.tv_save:
                content = mEditMemoContent.getText().toString().trim();
                if (TextUtils.isEmpty(content))
                    return;
                if (mEditListener != null) {
                    mEditListener.onSave(content);
                }
                break;
            case R.id.tv_cancel:
                if (mEditListener != null) {
                    mEditListener.onCancel();
                }
                break;
        }
        cancel();
    }

    @Override
    public void cancel() {
        mVicInput.stopRecord();
        super.cancel();
    }

    /**
     * 录音识别结果回调
     **/
    private VoiceInputComponent.OnResultListener onResultListener = new VoiceInputComponent.OnResultListener() {

        @Override
        public void onResult(String text) {
            if (TextUtils.isEmpty(text)) {
                return;
            } else if (text.length() < 6 && (text.startsWith(ChatListFragment.SAVE_KEYWORDS[0])
                    || text.startsWith(ChatListFragment.SAVE_KEYWORDS[1])
                    || text.startsWith(ChatListFragment.SAVE_KEYWORDS[2]))) {     //保存
                String content = mEditMemoContent.getText().toString().trim();
                if (TextUtils.isEmpty(content)) {
                    new CommonDialog(mContext, "编辑备忘", "您还没输入备忘内容哦", "知道了").show();
                    return;
                }
                if (mEditListener != null) {
                    mEditListener.onSave(content);
                    cancel();
                }
            } else if (text.length() < 6 && (text.startsWith(ChatListFragment.QUIT_KEYWORDS[0])
                    || text.startsWith(ChatListFragment.QUIT_KEYWORDS[1]))) {     //取消
                if (mEditListener != null) {
                    mEditListener.onCancel();
                    cancel();
                }
            } else {
                mEditMemoContent.getText().insert(mCursorIndex, text);
                mEditMemoContent.setSelection(mCursorIndex);
            }
        }

        @Override
        public void onError(int errorCode, String description) {
            new CommonDialog(mContext, "错误提示", description, "确定").show();
        }
    };

    public interface OnMemoEditListener {
        void onCancel();

        void onBack(String content);

        void onSave(String content);
    }
}
