package com.lingju.assistant.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;

/**
 * Created by Dyy on 2017/2/5.
 */
public class IncomingTipsTimesDialog extends Dialog {
    private Context mContext;
    TipsTimes tipsTimes;

    public IncomingTipsTimesDialog(Context context, TipsTimes tips) {
        super(context, R.style.lingju_commond_dialog);
        mContext=context;
        tipsTimes = tips;
    }

    public IncomingTipsTimesDialog(Context context, int theme) {
        super(context, theme);
        mContext=context;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.incoming_tips_times_dialog);
        RadioGroup rg = (RadioGroup) findViewById(R.id.aitt_buttons);
        int id = AppConfig.dPreferences.getInt(AppConfig.INCOMING_TIPS_TIMES, 0);
        if (id != 0) {
            rg.check(id);
        }
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                AppConfig.dPreferences.edit().putInt(AppConfig.INCOMING_TIPS_TIMES, checkedId).commit();
                tipsTimes.setTipsTimes();
                IncomingTipsTimesDialog.this.cancel();
            }
        });
        //取消按钮点击监听
        TextView cancle = (TextView) findViewById(R.id.tv_cancel);
        cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IncomingTipsTimesDialog.this.cancel();
            }
        });
    }

    public interface TipsTimes{
        public void setTipsTimes();
    }
}
