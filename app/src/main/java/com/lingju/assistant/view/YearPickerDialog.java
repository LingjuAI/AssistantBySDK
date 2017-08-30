package com.lingju.assistant.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.lingju.assistant.R;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2017/3/7.
 */
public class YearPickerDialog extends Dialog {
    @BindView(R.id.year_picker)
    DatePicker mYearPicker;
    private Calendar date = Calendar.getInstance();
    private OnYearPickedListener mPickedListener;

    public YearPickerDialog(Context context, long date, OnYearPickedListener listener) {
        super(context, R.style.lingju_commond_dialog);
        this.date.setTimeInMillis(date);
        this.mPickedListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_yearpicker);
        ButterKnife.bind(this);
        mYearPicker.setDefaultDate(date.getTimeInMillis());
        mYearPicker.setOnChangedListener(new DatePicker.OnchangedListener() {
            @Override
            public void onChanged(Calendar date) {
                YearPickerDialog.this.date = date;
            }
        });
    }

    @OnClick({R.id.ayp_cancel, R.id.ayp_confirm})
    public void onClick(View view) {
        cancel();
        switch (view.getId()) {
            case R.id.ayp_confirm:
                if (mPickedListener != null)
                    mPickedListener.onPicked(date.get(Calendar.YEAR));
                break;
        }
    }

    public interface OnYearPickedListener {
        void onPicked(int year);
    }
}
