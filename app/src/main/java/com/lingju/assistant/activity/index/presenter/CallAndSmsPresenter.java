package com.lingju.assistant.activity.index.presenter;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.activity.event.CallTaskEvent;
import com.lingju.assistant.activity.event.ChatMsgEvent;
import com.lingju.assistant.activity.event.DialogEvent;
import com.lingju.assistant.activity.event.RobotTipsEvent;
import com.lingju.assistant.activity.event.SynthesizeEvent;
import com.lingju.assistant.activity.index.IAdditionAssist;
import com.lingju.assistant.entity.CallAndSmsMsg;
import com.lingju.assistant.entity.CallBean;
import com.lingju.assistant.entity.RobotConstant;
import com.lingju.assistant.entity.SmsBean;
import com.lingju.assistant.player.event.UpdateWaittingSeekBarEvent;
import com.lingju.assistant.service.AssistantService;
import com.lingju.assistant.service.process.DefaultProcessor;
import com.lingju.assistant.service.process.MobileCommProcessor;
import com.lingju.audio.engine.base.SpeechMsg;
import com.lingju.audio.engine.base.SpeechMsgBuilder;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.common.log.Log;
import com.lingju.config.Setting;
import com.lingju.context.entity.Command;
import com.lingju.context.entity.ContactNum;
import com.lingju.context.entity.Contacts;
import com.lingju.model.ContactsProxy;
import com.lingju.model.dao.CallAndSmsDao;
import com.lingju.model.temp.speech.ResponseMsg;
import com.lingju.util.JsonUtils;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ken on 2017/1/3.
 */
public class CallAndSmsPresenter {

    private final static int CALL_TYPE = 110;
    private final static int SMS_TYPE = 111;

    private IAdditionAssist.AssistView mAssistView;
    private Activity mContext;
    private Timer communicationTimer;       //电话、短信任务定时器
    private WaitForActionTask waitForActionTask;    //电话、短信任务
    private String[] keywords;
    private AppConfig mAppConfig;
    private String nick;    //昵称
    private CallAndSmsReceiver mCallReceiver;
    private String phoneNumber;
    private String smsContent;
    private int actionType;
    private boolean isAfter;

    public CallAndSmsPresenter(IAdditionAssist.AssistView view) {
        this.mAssistView = view;
        this.mContext = (Activity) view;
        mAppConfig = (AppConfig) mContext.getApplication();
    }

    /**
     * 电话、短信指令处理
     *
     * @param text 回复文本
     * @param cmd  指令对象
     * @param type 用户输入类型  0：键盘输入  1：语音录入
     **/
    public void onCallResponse(String text, final Command cmd, int type) {
        SpeechMsgBuilder msgBuilder = SpeechMsgBuilder.create(text);
        //讯飞合成设置数字“按值播报”
        SynthesizerBase.get().setRdnString(true);
        // 将打电话任务事件发送给百度导航播报，让其知道当前处于电话、短信任务中，停止播报
        if (EventBus.getDefault().hasSubscriberForEvent(CallTaskEvent.class))
            EventBus.getDefault().post(new CallTaskEvent());
        if (cmd.getOutc() == DefaultProcessor.OUTC_ASK)      //说完话后自动开启识别
            msgBuilder.setContextMode(SpeechMsg.CONTEXT_KEEP_RECOGNIZE);
        //发送对话提示语
        EventBus.getDefault().post(new RobotTipsEvent(cmd.getTtext()));
        actionType = 0;
        isAfter = true;
        try {
            JSONArray actions = new JSONArray(cmd.getActions());
            JSONObject lastAction = actions.getJSONObject(actions.length() - 1);
            JSONObject lastTarget = lastAction.getJSONObject("target");
            Integer action = RobotConstant.ActionMap.get(lastAction.getString("action"));
            switch (lastTarget.getInt("id")) {
                case RobotConstant.ACTION_CONTACT:
                    if (action == RobotConstant.OPEN) {        //昵称快捷呼叫页面
                        Contacts contact = JsonUtils.getObj(lastTarget.toString(), Contacts.class);
                        nick = contact.getNickname();
                        Intent i = new Intent();
                        i.setAction(Intent.ACTION_PICK);
                        i.setData(ContactsContract.Contacts.CONTENT_URI);
                        mContext.startActivityForResult(i, CALL_TYPE);
                        msgBuilder.setContextMode(SpeechMsg.CONTEXT_AUTO);
                    }
                    break;
                case RobotConstant.ACTION_PHONE_NUM:
                    if (action == RobotConstant.SELECT) {       //选择正确还是错误
                        isAfter = false;
                        EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(text), null, null, null));
                        ContactNum num = JsonUtils.getObj(lastTarget.toString(), ContactNum.class);
                        fillKeyWords(num.getName(), num.getNumber());
                        EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.UPDATE_CALL_SMS_STATE));
                        EventBus.getDefault().post(new ChatMsgEvent(new CallAndSmsMsg(keywords, MobileCommProcessor.ConfirmNameCall)));
                    }
                    break;
                case RobotConstant.ACTION_CALL:
                    EventBus.getDefault().post(new DialogEvent(DialogEvent.CANCEL_TOGGLE_TYPE));
                    switch (action) {
                        case RobotConstant.CALL:
                            CallBean call = JsonUtils.getObj(lastTarget.toString(), CallBean.class);
                            if (TextUtils.isEmpty(call.getCode())) {    //立刻呼叫
                                cancelCommunicationTimer();
                                communicationTimer = new Timer();
                                if (waitForActionTask == null)
                                    waitForActionTask = new WaitForActionTask(keywords[1]);
                                communicationTimer.schedule(waitForActionTask, 0);
                            } else {    //呼出电话，倒计时
                                isAfter = false;
                                EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(text), null, null, null));
                                actionType = CALL_TYPE;
                                //设置免提标记
                                mAppConfig.speaker_on_one = call.isHandsFree();
                                fillKeyWords(call.getName(), call.getCode());
                                waitForActionTask = new WaitForActionTask(keywords[1]);
                                EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.UPDATE_CALL_SMS_STATE));
                                EventBus.getDefault().post(new ChatMsgEvent(new CallAndSmsMsg(keywords, MobileCommProcessor.WaittingForCall)));
                            }
                            break;
                        case RobotConstant.VIEW:        //显示刚才的电话
                            isAfter = false;
                            EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(text), null, null, null));
                            CallBean lastCall = JsonUtils.getObj(lastTarget.toString(), CallBean.class);
                            fillKeyWords(lastCall.getName(), lastCall.getCode());
                            EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.UPDATE_CALL_SMS_STATE));
                            EventBus.getDefault().post(new ChatMsgEvent(new CallAndSmsMsg(keywords, MobileCommProcessor.ConfirmLastCall)));
                            break;
                        case RobotConstant.CANCEL:      //取消呼叫 
                            cancelCommunicationTimer();
                            EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.UPDATE_CALL_SMS_STATE));
                            msgBuilder.setContextMode(SpeechMsg.CONTEXT_AUTO);
                            stopRecognize();
                            break;
                        case RobotConstant.OPEN:    //打开免提
                            mAppConfig.incoming_speaker_on = true;
                            AppConfig.dPreferences.edit().putBoolean(AppConfig.INCOMING_SPEAKER_ON, mAppConfig.incoming_speaker_on).commit();
                            break;
                        case RobotConstant.CLOSE:   //关闭免提
                            mAppConfig.incoming_speaker_on = false;
                            AppConfig.dPreferences.edit().putBoolean(AppConfig.INCOMING_SPEAKER_ON, mAppConfig.incoming_speaker_on).apply();
                            break;
                    }
                    break;
                case RobotConstant.ACTION_SMS:
                    EventBus.getDefault().post(new DialogEvent(DialogEvent.CANCEL_TOGGLE_TYPE));
                    switch (action) {
                        case RobotConstant.CREATE:      //新建、编辑、重新输入短信
                        case RobotConstant.MODIFY:
                            isAfter = false;
                            EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(text), null, null, null));
                            SmsBean sms = JsonUtils.getObj(lastTarget.toString(), SmsBean.class);
                            fillKeyWords(sms.getName(), sms.getCode(), sms.getContent());
                            EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.UPDATE_CALL_SMS_STATE));
                            EventBus.getDefault().post(new ChatMsgEvent(new CallAndSmsMsg(keywords, MobileCommProcessor.ConfirmForSend)));
                            break;
                        case RobotConstant.SEND:
                            SmsBean sendSms = JsonUtils.getObj(lastTarget.toString(), SmsBean.class);
                            if (TextUtils.isEmpty(sendSms.getContent())) {       //立刻发送
                                cancelCommunicationTimer();
                                communicationTimer = new Timer();
                                if (waitForActionTask == null) {
                                    waitForActionTask = new WaitForActionTask(keywords[1], keywords[2], 1);
                                }
                                communicationTimer.schedule(waitForActionTask, 0);
                            } else {     //发送短信，倒计时
                                actionType = SMS_TYPE;
                                isAfter = false;
                                EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(text), null, null, null));
                                fillKeyWords(sendSms.getName(), sendSms.getCode(), sendSms.getContent());
                                waitForActionTask = new WaitForActionTask(keywords[1], keywords[2], 1);
                                EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.UPDATE_CALL_SMS_STATE));
                                EventBus.getDefault().post(new ChatMsgEvent(new CallAndSmsMsg(keywords, MobileCommProcessor.WaittingForSend)));
                            }
                            break;
                        case RobotConstant.VIEW:        //显示刚才的短信
                            isAfter = false;
                            SmsBean lastSms = JsonUtils.getObj(lastTarget.toString(), SmsBean.class);
                            EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(text), null, null, null));
                            fillKeyWords(lastSms.getName(), lastSms.getCode(), lastSms.getContent());
                            EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.UPDATE_CALL_SMS_STATE));
                            EventBus.getDefault().post(new ChatMsgEvent(new CallAndSmsMsg(keywords, MobileCommProcessor.ConfirmLastMsg)));
                            break;
                        case RobotConstant.CANCEL:      //取消发送
                            cancelCommunicationTimer();
                            EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.UPDATE_CALL_SMS_STATE));
                            msgBuilder.setContextMode(SpeechMsg.CONTEXT_AUTO);
                            stopRecognize();
                            break;
                        case RobotConstant.OPEN:        //打开短信、来电播报 
                            switchBroadcast(true);
                            break;
                        case RobotConstant.CLOSE:       //关闭播报 
                            switchBroadcast(false);
                            break;
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (isAfter)
            EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(text), null, null, null));
        if (type == AssistantService.INPUT_VOICE) {
            final SpeechMsg speechMsg = msgBuilder.setText(text).build();
            SynthesizerBase.get().startSpeakAbsolute(speechMsg)
                        /* 合成是在Observable的subscribe()开始的，所以要在这之前通知动画播放。
                         *  doOnSubscribe 执行在离它最近的 subscribeOn() 所指定的线程。*/
                    .doOnNext(new Consumer<SpeechMsg>() {
                        @Override
                        public void accept(SpeechMsg speechMsg) throws Exception {
                            if (speechMsg.state() == SpeechMsg.State.OnBegin)
                                EventBus.getDefault().post(new SynthesizeEvent(SynthesizeEvent.SYNTH_START));
                        }
                    })
                    .doOnComplete(new Action() {
                        @Override
                        public void run() throws Exception {
                            EventBus.getDefault().post(new SynthesizeEvent(SynthesizeEvent.SYNTH_END));
                            if (speechMsg.state() == SpeechMsg.State.Completed) {
                                startTimer();
                            }
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .subscribe();
        } else {
            startTimer();
        }
    }

    private void switchBroadcast(boolean flag) {
        mAppConfig.inmsg_tips = flag;
        mAppConfig.incoming_tips = flag;
        AppConfig.dPreferences.edit().putBoolean(AppConfig.IN_MSG_TIPS, mAppConfig.inmsg_tips).commit();
        AppConfig.dPreferences.edit().putBoolean(AppConfig.INCOMING_TIPS, mAppConfig.incoming_tips).commit();
    }

    private void fillKeyWords(String... msg) {
        keywords = new String[msg.length];
        for (int i = 0; i < msg.length; i++)
            keywords[i] = msg[i];
    }

    /**
     * 开启定时器，准备执行任务
     **/
    private void startTimer() {
        if (actionType == CALL_TYPE) {    //拨号
            cancelCommunicationTimer();
            if (keywords.length == 2) {
                // 通知对话框刷新进度条
                EventBus.getDefault().post(new UpdateWaittingSeekBarEvent(true));
                // 5秒后执行拨号任务
                communicationTimer = new Timer();
                if (waitForActionTask == null)
                    waitForActionTask = new WaitForActionTask(keywords[1]);
                communicationTimer.schedule(waitForActionTask, 5000);
            }
        } else if (actionType == SMS_TYPE) {     //发短信
            cancelCommunicationTimer();
            if (keywords.length == 3) {
                EventBus.getDefault().post(new UpdateWaittingSeekBarEvent(true));
                communicationTimer = new Timer();
                if (waitForActionTask == null)
                    waitForActionTask = new WaitForActionTask(keywords[1], keywords[2], 1);
                communicationTimer.schedule(waitForActionTask, 5000);
            }
        }
    }

    /**
     * 供聊天列表中的短信编辑（ConfirmForSend）卡片手动调用发送短信
     *
     * @param number      收件人号码
     * @param content     短信内容
     * @param immediately 是否马上发送
     **/
    public void manualSendSms(String number, String content, boolean immediately) {
        cancelCommunicationTimer();
        communicationTimer = new Timer();
        waitForActionTask = new WaitForActionTask(number, content, 1);
        if (immediately) {
            communicationTimer.schedule(waitForActionTask, 0);
        } else {
            EventBus.getDefault().post(new UpdateWaittingSeekBarEvent(true));
            communicationTimer.schedule(waitForActionTask, 5000);
        }
    }

    /**
     * 更新页面回调数据
     **/
    public void updateData(Intent data, int requestCode) {
        if (requestCode == CALL_TYPE) {
            if (data == null)
                return;
            Uri uri = data.getData();
            String name = null;
            if (uri != null) {
                String id = uri.toString();
                id = id.substring(id.lastIndexOf("/") + 1, id.length());
                Cursor cursor = null;
                int index = 0;
                try {
                    cursor = mContext.getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null,
                            "sort_key COLLATE LOCALIZED ASC");
                    while (cursor.moveToNext()) {
                        index++;
                        long contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                        if (Long.valueOf(id) == contactId) {
                            name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                            break;
                        }
                    }
                    //更新联系人昵称
                    ContactsProxy contact = new ContactsProxy();
                    contact.setRawContactId(id);
                    contact.setNickName(nick);
                    contact.setName(name);
                    CallAndSmsDao.getInstance(mContext).updateNickName(contact);
                    //同步更新
                    CallAndSmsDao.getInstance(mContext).sync(CallAndSmsDao.getInstance(mContext).getSyncDao(CallAndSmsDao.ContactsDao.class));
                    sendMessageToRobot("第" + index + "个");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (cursor != null)
                        cursor.close();
                }

            }
        }
    }

    /**
     * 停止识别
     **/
    public void stopRecognize() {
        Intent intent = new Intent(mContext, AssistantService.class);
        intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_RECOGNIZE);
        mContext.startService(intent);
    }

    /**
     * 手机摇晃事件处理
     **/
    public boolean shakeHandle() {
        if (!SynthesizerBase.isInited())
            return false;
        if (communicationTimer != null) {
            sendMessageToRobot("取消");
            return true;
        } else if (!AppConfig.dPreferences.getBoolean(AppConfig.SHAKE_WAKE, false)) {
            return false;
        } else if (SynthesizerBase.get().isSpeaking()) {
            SynthesizerBase.get().stopSpeaking();
            return true;
        }
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            if (am.isMusicActive()) {
                /* 停止播放音乐 */
                Intent intent = new Intent(mContext, AssistantService.class);
                intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PAUSE_PLAY);
                mContext.startService(intent);
                /* 停止识别、合成，尝试唤醒 */
                Intent voiceIntent = new Intent(mContext, AssistantService.class);
                voiceIntent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.SEND_TO_ROBOT_FOR_END_TASK);
                voiceIntent.putExtra(AssistantService.END_TASK, false);
                mContext.startService(voiceIntent);
                return true;
            }
        }

        return false;
    }


    /**
     * 取消定时任务，销毁定时器
     **/
    public void cancelCommunicationTimer() {
        if (communicationTimer != null) {
            communicationTimer.cancel();
            communicationTimer = null;
            waitForActionTask = null;
        }
        if (EventBus.getDefault().hasSubscriberForEvent(CallTaskEvent.class)) {
            EventBus.getDefault().post(new CallTaskEvent(CallTaskEvent.STATE_END));
        }
    }

    /**
     * 向机器人发送对话内容(语音录入类型)
     **/
    private void sendMessageToRobot(String text) {
        Intent intent = new Intent(mContext, AssistantService.class);
        intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.SEND_TO_ROBOT);
        intent.putExtra(AssistantService.TEXT, text);
        mContext.startService(intent);
    }

    public void unRegisterReceiver() {
        if (mCallReceiver != null)
            mContext.unregisterReceiver(mCallReceiver);
    }

    /**
     * 打电话/发短信任务
     **/
    private class WaitForActionTask extends TimerTask {
        private String number;
        private String content;
        private int type;//0=电话，1=短信

        public WaitForActionTask(String number) {
            this(number, null, 0);
        }

        public WaitForActionTask(String number, String content, int type) {
            this.number = number;
            this.content = content;
            this.type = type;
        }

        @Override
        public void run() {
            //注册广播接收者
            unRegisterReceiver();
            mCallReceiver = new CallAndSmsReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
            intentFilter.addAction(SMS_SEND_ACTION);
            mContext.registerReceiver(mCallReceiver, intentFilter);

            // EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECORD_IDLE));
            switch (type) {
                case 0:     //电话
                    Intent speechIntent = new Intent(mContext, AssistantService.class);
                    speechIntent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_VOICE_MODE);
                    mContext.startService(speechIntent);
                    Log.i("LingJu", "手机厂商：" + Build.MANUFACTURER);
                    //由于华为、小米、三星手机无法直接呼出紧急电话，所以在该号码前加区号（魅族可以）
                    if (number.length() == 3 && number.matches("^(110|119|120)$") && !"Meizu".equals(Build.MANUFACTURER)) {
                        String city = Setting.formatCity(Setting.getAddress().getCity());
                        number = CallAndSmsDao.getInstance(mContext).getZipCode(city) + number;
                    }
                    // PermissionUtils.checkPermission(mContext, PermissionUtils.OP_CALL_PHONE);
                    Intent icall = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + this.number));
                    icall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    mContext.startActivity(icall);

                    break;
                case 1:     //短信
                    // PermissionUtils.checkPermission(mContext, PermissionUtils.OP_SEND_SMS);
                    SmsManager smsManager = SmsManager.getDefault();
                    if (content != null && content.length() > 0) {
                        content += "-发自小灵机器人";
                        //切分短信，每七十个汉字为一条短信，不足七十就只有一条：返回的是字符串的List集合
                        List<String> texts = smsManager.divideMessage(content);
                        //发送短信
                        PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, new Intent(SMS_SEND_ACTION), 0);
                        for (String tt : texts) {
                            phoneNumber = number;
                            smsContent = tt;
                            smsManager.sendTextMessage(number, null, tt, pi, null);
                        }
                    }
                    break;
            }
            /* 停止识别、合成 */
            Intent intent = new Intent(mContext, AssistantService.class);
            intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.SEND_TO_ROBOT_FOR_END_TASK);
            intent.putExtra(AssistantService.END_TASK, true);
            mContext.startService(intent);
            if (EventBus.getDefault().hasSubscriberForEvent(CallTaskEvent.class)) {
                EventBus.getDefault().post(new CallTaskEvent(CallTaskEvent.STATE_END));
            }

            cancelCommunicationTimer();
        }

    }

    /**
     * 判断手机是否是飞行模式
     *
     * @param context
     * @return
     */
    public boolean getAirplaneMode(Context context) {
        int isAirplaneMode = Settings.System.getInt(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0);
        return isAirplaneMode == 1;
    }

    public static final String SMS_SEND_ACTION = "sms_send_action";

    /**
     * 电话拨号，短信发送广播接收者
     **/
    private class CallAndSmsReceiver extends BroadcastReceiver {
        int type = -1;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (type != -1)
                return;
            String action = intent.getAction();
            if (Intent.ACTION_NEW_OUTGOING_CALL.equals(action)) {
                TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
                if (TelephonyManager.SIM_STATE_READY != tm.getSimState() || getAirplaneMode(mContext))  //SIM卡未准备或飞行模式拨号失败
                    type = MobileCommProcessor.FailedCall;
                else
                    type = MobileCommProcessor.CompletedCall;
            } else if (SMS_SEND_ACTION.equals(action)) {
                if (getResultCode() == Activity.RESULT_OK) { //短信发送成功
                    type = MobileCommProcessor.CompletedSend;
                    AppConfig.mContactUtils.insertSMS(phoneNumber, smsContent);
                    phoneNumber = null;
                    smsContent = null;
                    CallAndSmsDao.getInstance(mContext).sync(CallAndSmsDao.getInstance(mContext).getSyncDao(CallAndSmsDao.MessageDao.class));
                } else {
                    Log.i("LingJu", "发送短信错误码：" + getResultCode());
                    type = MobileCommProcessor.FailedSend;
                }
            }
            if (type != -1) {
                EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.UPDATE_CALL_SMS_STATE));
                EventBus.getDefault().post(new ChatMsgEvent(new CallAndSmsMsg(keywords, type)));
            }
        }
    }
}
