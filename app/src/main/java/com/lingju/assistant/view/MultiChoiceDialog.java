package com.lingju.assistant.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
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
public class MultiChoiceDialog extends Dialog {
    @BindView(R.id.cd_title)
    TextView mCdTitle;
    @BindView(R.id.rg_choice)
    RadioGroup mRgChoice;
    @BindView(R.id.choice_box)
    LinearLayout mChoiceBox;
    private Context mContext;
    private String[] datas;
    private String title;

    public MultiChoiceDialog(Context context, String title, String[] datas) {
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
        init();
    }

    private void init() {
        mRgChoice.setVisibility(View.GONE);
        mChoiceBox.setVisibility(View.VISIBLE);
        mCdTitle.setText(title);
        for (int i = 0; i < datas.length; i++) {
            AppCompatCheckBox cb = new AppCompatCheckBox(mContext);
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(-1, ScreenUtil.getInstance().dip2px(48));
            cb.setLayoutParams(layoutParams);
            cb.setGravity(Gravity.CENTER_VERTICAL);
            cb.setId(i);
            cb.setText(datas[i]);
            cb.setTextSize(15);
            cb.setTextColor(mContext.getResources().getColor(R.color.new_text_color_first));
            cb.setPadding(ScreenUtil.getInstance().dip2px(16), 0, 0, 0);
            if (i == 0)
                cb.setChecked(true);
            mChoiceBox.addView(cb);
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
                    StringBuilder days = new StringBuilder();
                    for (int i = 0; i < datas.length; i++) {
                        CheckBox cb = (CheckBox) mChoiceBox.findViewById(i);
                        if (cb.isChecked()) {
                            days.append(cb.getText().toString()).append(",");
                        }
                    }
                    if (days.length() > 1)
                        days.setLength(days.length() - 1);
                    mListener.onChoose(days.toString());
                }
                break;
        }
    }

    private SingleChooseDialog.ChooseListener mListener;

    public MultiChoiceDialog SetOnChooseListener(SingleChooseDialog.ChooseListener listener) {
        this.mListener = listener;
        return this;
    }
}
