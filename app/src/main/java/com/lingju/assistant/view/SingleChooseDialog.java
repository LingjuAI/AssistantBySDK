package com.lingju.assistant.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.util.ScreenUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2017/7/17.
 */
public class SingleChooseDialog extends Dialog implements RadioGroup.OnCheckedChangeListener {

    @BindView(R.id.cd_title)
    TextView mScdTitle;
    @BindView(R.id.rg_choice)
    RadioGroup mRgChoice;
    @BindView(R.id.choice_box)
    LinearLayout mChoiceBox;
    private Context mContext;
    private String[] datas;
    private String title;
    private int mSelectId;

    public SingleChooseDialog(Context context, String title, String[] datas) {
        super(context, R.style.lingju_commond_dialog);
        this.mContext = context;
        this.title = title;
        this.datas = datas;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_choice);
        ButterKnife.bind(this);
        mRgChoice.setOnCheckedChangeListener(this);
        init();
    }

    private void init() {
        mRgChoice.setVisibility(View.VISIBLE);
        mChoiceBox.setVisibility(View.GONE);
        mScdTitle.setText(title);
        for (int i = 0; i < datas.length; i++) {
            AppCompatRadioButton arb = new AppCompatRadioButton(mContext);
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(-1, ScreenUtil.getInstance().dip2px(48));
            arb.setLayoutParams(layoutParams);
            arb.setGravity(Gravity.CENTER_VERTICAL);
            arb.setId(i);
            arb.setText(datas[i]);
            arb.setTextSize(15);
            arb.setTextColor(mContext.getResources().getColor(R.color.new_text_color_first));
            arb.setPadding(ScreenUtil.getInstance().dip2px(16), 0, 0, 0);
            if (i == 0)
                arb.setChecked(true);
            mRgChoice.addView(arb);
        }
    }

    @OnClick({R.id.cd_cancel, R.id.cd_confirm})
    public void onClick(View view) {
        cancel();
        switch (view.getId()) {
            case R.id.cd_cancel:
                if (mListener != null)
                    mListener.onCancel();
                break;
            case R.id.cd_confirm:
                if (mListener != null) {
                    RadioButton selectRb = (RadioButton) mRgChoice.findViewById(mSelectId);
                    mListener.onChoose(selectRb.getText().toString());
                }
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        mSelectId = checkedId;
    }

    private ChooseListener mListener;

    public SingleChooseDialog SetOnChooseListener(ChooseListener listener) {
        this.mListener = listener;
        return this;
    }

    public interface ChooseListener {
        void onChoose(String content);
        void onCancel();
    }
}
