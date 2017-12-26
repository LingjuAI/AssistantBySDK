package com.lingju.assistant.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.download.impl.DownloadFileService;
import com.lingju.assistant.player.audio.LingjuAudioPlayer;
import com.lingju.assistant.receiver.MediaButtonReceiver;
import com.lingju.assistant.service.VoiceMediator;
import com.lingju.assistant.view.CommonAlertDialog;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.assistant.view.IncomingTipsTimesDialog;
import com.lingju.assistant.view.PlayChannelDialog;
import com.lingju.assistant.view.SwitchButton;
import com.lingju.assistant.view.base.BaseDialog;
import com.lingju.model.Version;
import com.lingju.util.MusicUtils;
import com.lingju.util.NetUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2017/1/6.
 */
public class SettingActivity extends GoBackActivity implements SwitchButton.OnCheckedChangeListener, View.OnClickListener {

    @BindView(R.id.setting_wakeup)
    SwitchButton mSettingWakeup;
    @BindView(R.id.setting_wifi)
    SwitchButton mSettingWifi;
    @BindView(R.id.allow_wire_bt)
    SwitchButton mAllowWireBt;
    @BindView(R.id.setting_shake_for_wake)
    SwitchButton mSettingShakeForWake;
    @BindView(R.id.setting_play_channel_result)
    TextView mSettingPlayChannelResult;
    @BindView(R.id.setting_speaker_name)
    TextView mSettingSpeakerName;

    @BindView(R.id.setting_incoming_call_tips_switch)
    SwitchButton mSettingIncomingCallTipsSwitch;
    @BindView(R.id.setting_incoming_tips_times_result)
    TextView mSettingIncomingTipsTimesResult;
    @BindView(R.id.setting_receiver_msg_tips_switch)
    SwitchButton mSettingReceiverMsgTipsSwitch;
    @BindView(R.id.setting_dnd_tips)
    TextView mSettingDndTips;
    @BindView(R.id.setting_incoming_speakeron_switch)
    SwitchButton mSettingIncomingSpeakeronSwitch;
    @BindView(R.id.li_title0)
    RelativeLayout titleWakeup;
    @BindView(R.id.title_wifi)
    RelativeLayout titleWifi;
    @BindView(R.id.li_title)
    RelativeLayout titleAllowWireBt;
    @BindView(R.id.li_title7)
    RelativeLayout titleSettingShake;
    @BindView(R.id.incoming_call)
    RelativeLayout titleSettingIncomingCall;
    @BindView(R.id.incoming_sms)
    RelativeLayout titleIncomingSpeakeronSwitch;
    @BindView(R.id.loudspeaker_open)
    RelativeLayout titleReceiverMsgTips;
    @BindView(R.id.setting_update_version_text)
    TextView updateVersionText;
    private BaseDialog shareDialog;
    private IncomingTipsTimesDialog tipsTimesDialog;
    private PlayChannelDialog playChannelDialog;
    private AppConfig mAppConfig;
    private CommonAlertDialog progressDialog;
    public final static int RESULT_DND_TIPS = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        ButterKnife.bind(this);
        mAppConfig = (AppConfig) getApplication();
        mSettingShakeForWake.setOnCheckedChangeListener(this);
        mSettingWakeup.setOnCheckedChangeListener(this);
        mSettingWifi.setOnCheckedChangeListener(this);
        mAllowWireBt.setOnCheckedChangeListener(this);
        titleWakeup.setClickable(true);
        titleWifi.setClickable(true);
        titleAllowWireBt.setClickable(true);
        titleSettingShake.setClickable(true);
        titleSettingIncomingCall.setClickable(true);
        titleIncomingSpeakeronSwitch.setClickable(true);
        titleReceiverMsgTips.setClickable(true);
        mSettingIncomingCallTipsSwitch.setOnCheckedChangeListener(this);
        mSettingReceiverMsgTipsSwitch.setOnCheckedChangeListener(this);
        mSettingIncomingSpeakeronSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSettingWakeup.setChecked(VoiceMediator.get().isWakeUpMode());
        mSettingWifi.setChecked(AppConfig.dPreferences.getBoolean(AppConfig.DOWNLOAD_ON_WIFI, false));
        mAllowWireBt.setChecked(AppConfig.dPreferences.getBoolean(AppConfig.ALLOW_WIRE, true));
        mSettingShakeForWake.setChecked(AppConfig.dPreferences.getBoolean(AppConfig.SHAKE_WAKE, false));
        mSettingIncomingCallTipsSwitch.setChecked(mAppConfig.incoming_tips);
        mSettingIncomingTipsTimesResult.setText(getTipsTimes());
        mSettingReceiverMsgTipsSwitch.setChecked(mAppConfig.inmsg_tips);
        mSettingIncomingSpeakeronSwitch.setChecked(mAppConfig.incoming_speaker_on);
        String temp = "扬声器播放";
        if (R.id.apc_button1 == AppConfig.dPreferences.getInt(AppConfig.PLAY_CHANNEL, R.id.apc_button2)) {
            temp = "蓝牙播放";
        }
        mSettingPlayChannelResult.setText(temp);
        mSettingSpeakerName.setText(mAppConfig.speakers[mAppConfig.checkedSpeakerItemPosition][0]);
        try {
            updateVersionText.setText("当前版本：v"+getPackageManager().getPackageInfo(this.getPackageName(),0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        mSettingDndTips.setText(getDefaultDNDtext());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (shareDialog != null) {
            shareDialog.dismiss();
            shareDialog = null;
        }
    }

    private String getDefaultDNDtext() {
        StringBuilder sb = new StringBuilder();
        // sb.append(mAppConfig.dndMode ? "开" : "关");
        if (mAppConfig.dndMode) {
            sb.append('[').append(mAppConfig.dndStart.toString());
            sb.append("-").append(mAppConfig.dndStart.gt(mAppConfig.dndEnd) ? "次日" : "").append(mAppConfig.dndEnd.toString()).append("]");
        } else {
            sb.append("关");
        }
        return sb.toString();
    }

    /**
     * 获取来电提示次数文本
     **/
    private String getTipsTimes() {
        String temp = "2次";
        switch (AppConfig.dPreferences.getInt(AppConfig.INCOMING_TIPS_TIMES, 0)) {
            case R.id.aitt_button1:
                temp = "1次";
                break;
            case R.id.aitt_button2:
                temp = "2次";
                break;
            case R.id.aitt_button3:
                temp = "3次";
                break;
            case R.id.aitt_button4:
                temp = "直到对方挂断";
                break;
        }
        return temp;
    }

    /**
     * 当 SSO 授权 Activity 退出时，该函数被调用。
     *
     * @see {@link Activity#onActivityResult}
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (shareDialog != null) {
            if (shareDialog.getmSsoHandler() != null) {
                shareDialog.getmSsoHandler().authorizeCallBack(requestCode, resultCode, data);
            }

        }
        switch (resultCode) {
            case RESULT_DND_TIPS:
                mSettingDndTips.setText(getDefaultDNDtext());
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @OnClick({R.id.setting_incoming_tips_times, R.id.setting_dnd_item,/*R.id.setting_call_sms,*/R.id.setting_contacts_remark, R.id.setting_play_channel, R.id.setting_speaker,
            R.id.setting_share, R.id.setting_update_version, R.id.setting_about_us, R.id.li_title0, R.id.title_wifi, R.id.li_title, R.id.li_title7, R.id.incoming_call,
            R.id.incoming_sms, R.id.loudspeaker_open})
    public void onClick(View view) {
        boolean isSkip = true;
        switch (view.getId()) {
            case R.id.setting_play_channel:
                if (playChannelDialog == null) {
                    playChannelDialog = new PlayChannelDialog(this, new PlayChannelDialog.PlayChannel() {
                        @Override
                        public void setPlayChannel() {
                            String temp = "扬声器播放";
                            if (R.id.apc_button1 == AppConfig.dPreferences.getInt(AppConfig.PLAY_CHANNEL, R.id.apc_button2)) {
                                temp = "蓝牙播放";
                            }
                            mSettingPlayChannelResult.setText(temp);
                        }
                    });
                    playChannelDialog.setCanceledOnTouchOutside(true);
                }
                playChannelDialog.show();
                break;
            case R.id.setting_speaker:
                startActivity(new Intent(this, SelectSpeakerActivity.class));
                break;
            case R.id.setting_share:
                if (shareDialog == null) {
                    shareDialog = BaseDialog.newInstance(SettingActivity.this);
                }
                shareDialog.show(getSupportFragmentManager(), BaseDialog.class.getSimpleName());
                isSkip = false;
                break;
            case R.id.setting_update_version:
                new CheckVersionTask().execute();
                isSkip = false;
                break;
            case R.id.setting_about_us:
                startActivity(new Intent(this, IntroduceActivity.class));
                break;
            case R.id.setting_incoming_tips_times:
                if (tipsTimesDialog == null) {
                    tipsTimesDialog = new IncomingTipsTimesDialog(SettingActivity.this, new IncomingTipsTimesDialog.TipsTimes() {
                        @Override
                        public void setTipsTimes() {
                            mSettingIncomingTipsTimesResult.setText(getTipsTimes());
                        }
                    });
                    tipsTimesDialog.setCanceledOnTouchOutside(true);
                }
                tipsTimesDialog.show();
                break;
            case R.id.setting_dnd_item:
                startActivityForResult(new Intent(this, DndSettingActivity.class), 0);
                break;
            case R.id.setting_contacts_remark:
                startActivity(new Intent(this, NickSettingActivity.class));
                break;
            case R.id.li_title0:
                clickText(mSettingWakeup);
                break;
            case R.id.title_wifi:
                if (mSettingWifi.isChecked()) {
                    clickText(mSettingWifi);
                } else {
                    new CommonDialog(this, "3G/4G下播放在线音乐", "此功能会消耗较多流量，确认开启吗？", "取消", "开启")
                            .setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                                @Override
                                public void onConfirm() {
                                    clickText(mSettingWifi);
                                }
                            })
                            .show();
                }
                break;
            case R.id.li_title:
                clickText(mAllowWireBt);
                break;
            case R.id.li_title7:
                clickText(mSettingShakeForWake);
                break;
            case R.id.incoming_call:
                clickText(mSettingIncomingCallTipsSwitch);
                break;
            case R.id.incoming_sms:
                clickText(mSettingIncomingSpeakeronSwitch);
                break;
            case R.id.loudspeaker_open:
                clickText(mSettingReceiverMsgTipsSwitch);
                break;
        }
        if (isSkip)
            goInto();
    }

    /**
     * 点击switchButton对应条目的点击事件
     */
    private void clickText(SwitchButton switchButton) {
        if (switchButton.isChecked()) {
            switchButton.setChecked(false);
        } else {
            switchButton.setChecked(true);
        }
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.setting_wakeup:
                VoiceMediator.get().setWakeUpMode(isChecked);
                break;
            case R.id.setting_wifi:
                AppConfig.dPreferences.edit().putBoolean(AppConfig.DOWNLOAD_ON_WIFI, isChecked).commit();
                LingjuAudioPlayer.create(this).setNoOnlinePlayInMobileNet(!isChecked);
                break;
            case R.id.allow_wire_bt:
                AppConfig.dPreferences.edit().putBoolean(AppConfig.ALLOW_WIRE, isChecked).commit();
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (!isChecked) {
                    am.unregisterMediaButtonEventReceiver(new ComponentName(getApplicationContext(), MediaButtonReceiver.class));
                } else {
                    am.registerMediaButtonEventReceiver(new ComponentName(getApplicationContext(), MediaButtonReceiver.class));
                }
                break;
            case R.id.setting_shake_for_wake:
                AppConfig.dPreferences.edit().putBoolean(AppConfig.SHAKE_WAKE, isChecked).commit();
                break;
            case R.id.setting_incoming_call_tips_switch:
                mAppConfig.incoming_tips = isChecked;
                AppConfig.dPreferences.edit().putBoolean(AppConfig.INCOMING_TIPS, isChecked).commit();
                break;
            case R.id.setting_receiver_msg_tips_switch:
                mAppConfig.inmsg_tips = isChecked;
                AppConfig.dPreferences.edit().putBoolean(AppConfig.IN_MSG_TIPS, isChecked).commit();
                break;
            case R.id.setting_incoming_speakeron_switch:
                mAppConfig.incoming_speaker_on = isChecked;
                AppConfig.dPreferences.edit().putBoolean(AppConfig.INCOMING_SPEAKER_ON, isChecked).commit();
                break;
        }
    }


    /**
     * 显示loading界面
     */
    private void showLoading(String title, String message) {
        if (null == progressDialog || !progressDialog.isShowing()) {
            progressDialog =  new CommonAlertDialog(this, title, message);
            progressDialog.show();
        }
    }

    /**
     * 显示loading界面
     */
    private void closeLoading() {
        if (null != progressDialog && progressDialog.isShowing())
            progressDialog.cancel();
    }


    /**
     * 应用版本检测任务
     **/
    private class CheckVersionTask extends AsyncTask<Void, Void, Version> {
        boolean showLoading = true;

        public CheckVersionTask(boolean showLoading) {
            this.showLoading = showLoading;
        }

        public CheckVersionTask() {
        }

        @Override
        protected void onPreExecute() {
            if (showLoading)
                showLoading("版本检查", "新版本获取中...");
        }

        @Override
        protected Version doInBackground(Void... params) {
            return checkUpdateVersion();
        }

        @Override
        protected void onPostExecute(final Version result) {
            closeLoading();
            if (result != null) {
                //newVersionUrl="http://192.168.1.200/hospital.apk";
                new CommonDialog(SettingActivity.this, "版本检查", "有新版本v"+result.getNew_version()+"，您是否需要马上更新？", "取消", "确定")
                        .setOnConfirmListener(new CommonDialog.OnConfirmListener() {

                            @Override
                            public void onConfirm() {
                                installNewVersion(result);
                            }
                        }).show();
            } else if (showLoading) {
                new CommonDialog(SettingActivity.this, "版本检查", "当前版本V" + AppConfig.versionName + "已经是最新的了！", "确定").show();
            }
        }

    }

    /**
     * 安装新版本
     **/
    private void installNewVersion(final Version version) {
        NetUtil.NetType type = NetUtil.getInstance(this).getCurrentNetType();
        mAppConfig.newVersion = version;
        if (type == NetUtil.NetType.NETWORK_TYPE_WIFI) {
            Intent it = new Intent(this, DownloadFileService.class);
            startService(it);
        } else if (type == NetUtil.NetType.NETWORK_TYPE_2G || type == NetUtil.NetType.NETWORK_TYPE_3G) {
            new CommonDialog(SettingActivity.this, "温馨提示", "您当前处于" + type.toString() + "网络中，继续此次版本更新将消耗您较多的移动流量，建议您切换到wifi网络中下载安装，您是否确定要继续？", "否", "是")
                    .setOnConfirmListener(new CommonDialog.OnConfirmListener() {

                        @Override
                        public void onConfirm() {
                            Intent it = new Intent(SettingActivity.this, DownloadFileService.class);
                            startService(it);
                        }
                    }).show();
        } else {
            new CommonDialog(SettingActivity.this, "温馨提示", "您当前的网络异常，请确保手机能正常上网！", "确定").show();
        }
    }

    /**
     * 检测应用版本是否有更新
     **/
    public Version checkUpdateVersion() {
        try {
            Version vs = MusicUtils.checkUpdateVersion(AppConfig.versionName); //提交最新版本号，例如:v1.0.0
            if (vs.isUpdate()) {
                return vs;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
