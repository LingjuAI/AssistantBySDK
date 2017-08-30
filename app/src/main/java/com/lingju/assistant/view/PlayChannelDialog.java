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
public class PlayChannelDialog extends Dialog {
    private Context mContext;
    PlayChannel playChannel;
    public PlayChannelDialog(Context context, PlayChannel channel) {
        super(context, R.style.lingju_commond_dialog);
        mContext = context;
        playChannel = channel;
    }
    protected PlayChannelDialog(Context context, int theme) {
        super(context, theme);
        mContext=context;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.play_channel_dialog);
        RadioGroup rg=(RadioGroup) findViewById(R.id.apc_buttons);
        int id= AppConfig.dPreferences.getInt(AppConfig.PLAY_CHANNEL, R.id.apc_button2);
        if(id!=0){
            rg.check(id);
        }
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                AppConfig.dPreferences.edit().putInt(AppConfig.PLAY_CHANNEL, checkedId).commit();
                playChannel.setPlayChannel();
                PlayChannelDialog.this.cancel();
            }
        });
        //取消按钮点击监听
        TextView cancle = (TextView) findViewById(R.id.tv_cancel);
        cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayChannelDialog.this.cancel();
            }
        });
    }
     public interface PlayChannel{
        public void setPlayChannel();
     }
}
