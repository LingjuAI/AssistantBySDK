package com.lingju.assistant.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.lingju.assistant.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2017/3/16.
 */
public class NickEditDialog extends Dialog {

    public static final int INPUT_ZH = 1;
    private Context context;
    @BindView(R.id.ned_name)
    TextView mNedName;
    @BindView(R.id.et_nick)
    EditText mEtNick;
    @BindView(R.id.til_nick)
    TextInputLayout mTilNick;
    @BindView(R.id.ned_confirm)
    TextView mNedConfirm;
    private String contact;
    private String nick;
    private OnNickEditListener mEditListener;
    private String contentHint;
    private int inputType;

    public NickEditDialog(Context context, String contact, String nick) {
        super(context, R.style.lingju_commond_dialog);
        this.contact = contact;
        this.nick = nick;
        this.context = context;
    }

    public NickEditDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_nick_edit);
        ButterKnife.bind(this);

        mNedName.setText(contact);
        mEtNick.setText(nick);
        mEtNick.setSelection(mEtNick.length());
        mEtNick.addTextChangedListener(tw);
    }

    public NickEditDialog setOnNickEditListener(OnNickEditListener listener) {
        this.mEditListener = listener;
        return this;
    }

    @OnClick({R.id.ned_cancel, R.id.ned_confirm/*, R.id.ned_name*/})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ned_cancel:
                cancel();
                break;
            case R.id.ned_confirm:
                if (mEditListener != null)
                    mEditListener.onConfirm(mEtNick.getText().toString().trim());
                break;
            /*case R.id.ned_name:
                if (mEditListener != null)
                    mEditListener.onContactEdit();
                break;*/
        }
    }

    public interface OnNickEditListener {
        void onContactEdit();

        void onConfirm(String nick);
    }

    int i = 0;
    private TextWatcher tw = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            mNedConfirm.setTextColor(TextUtils.isEmpty(mEtNick.getText().toString().trim())
                    ? context.getResources().getColor(R.color.forbid_click_color)
                    : context.getResources().getColor(R.color.base_blue));
            if (inputType == INPUT_ZH) {
                int l = s.length();
                while (--l >= 0) {
                    if (TextUtils.isEmpty(s))
                        break;
                    if (s.charAt(l) < 0x4E00 || s.charAt(l) > 0x9FB0) {
                        s.delete(l, l + 1);
                    }
                }
            }
        }
    };

    public NickEditDialog setHint(String text) {
        this.contentHint = text;
        if (mEtNick != null)
            mEtNick.setHint(text);
        return this;
    }

    public NickEditDialog setInputType(int inputType) {
        this.inputType = inputType;
        return this;
    }

    public TextInputLayout getTextInputLayout() {
        return mTilNick;
    }
}
