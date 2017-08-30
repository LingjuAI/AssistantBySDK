package com.lingju.assistant.service.process;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.event.CallAndSmsEvent;
import com.lingju.assistant.activity.event.ChatMsgEvent;
import com.lingju.assistant.activity.event.SynthesizeEvent;
import com.lingju.assistant.player.audio.LingjuAudioPlayer;
import com.lingju.assistant.service.AssistantService;
import com.lingju.assistant.service.process.base.BaseProcessor;
import com.lingju.audio.SystemVoiceMediator;
import com.lingju.audio.engine.base.SpeechMsg;
import com.lingju.audio.engine.base.SpeechMsgBuilder;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.common.log.Log;
import com.lingju.context.entity.Command;
import com.lingju.context.entity.ContactNum;
import com.lingju.model.Contact;
import com.lingju.model.SmsInfo;
import com.lingju.model.dao.CallAndSmsDao;
import com.lingju.model.temp.speech.ResponseMsg;
import com.lingju.util.PhoneContactUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Administrator on 2016/11/5.
 */
public class MobileCommProcessor extends BaseProcessor {

    private static final String TAG = "LingJu";
    /**
     * 显示联系人详细信息的对话框
     */
    public final static int CallDialogType = 0;
    /**
     * 显示短信内容的对话框
     */
    public final static int SmsDialogType = 1;

    /**
     * 选择多个联系人或者号码的对话框
     */
    public final static int CheckListDialogType = 2;
    //CallDialogType的子类
    public final static int WaittingForCall = 00;
    public final static int ConfirmNameCall = 01;
    public final static int ConfirmNameSms = 02;
    public final static int ConfirmLastCall = 03;
    public final static int CompletedCall = 04;
    public final static int FailedCall = 05;
    //SmsDialogType的子类
    public final static int WaittingForSend = 010;
    public final static int ConfirmForSend = 011;
    public final static int ConfirmLastMsg = 012;
    public final static int CompletedSend = 013;
    public final static int FailedSend = 014;
    //CheckListDialogType的子类
    public final static int CheckForNameCall = 020;
    public final static int CheckForNumCall = 021;
    public final static int CheckForNameSms = 022;
    public final static int CheckForNumSms = 023;
    private AppConfig mAppConfig;
    private ThreadPoolExecutor tPools;      //线程池
    private int mobileState;      //通话状态
    private int current_volume;     //当前音量
    private Handler mHandler;
    private int inMsgTipsFlow;
    private boolean isSmsReceiverValid;

    public MobileCommProcessor(Context mContext, SystemVoiceMediator mediator, Handler handler) {
        super(mContext, mediator);
        this.mHandler = handler;
        mAppConfig = (AppConfig) ((Service) mContext).getApplication();
        tPools = new ThreadPoolExecutor(10, 20, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        phoneCallListener();
        /* 注册内容观察者 */
        mContext.getContentResolver().registerContentObserver(Uri.parse(PhoneContactUtils.SMS_URI_INBOX),
                true, new SmsObserver(handler));
    }

    @Override
    public int aimCmd() {
        return CMD_CALL;
    }

    @Override
    public void handle(Command cmd, String text, int inputType) {
        super.handle(cmd, text, inputType);
        EventBus.getDefault().post(new CallAndSmsEvent(cmd, text, inputType));
    }

    /**
     * 合成并在聊天视图中显示回复文本
     *
     * @param text       回复文本
     * @param inputType  用户输入类型。只有语音录入才会合成回复文本
     * @param msgBuilder 合成信息对象
     **/
    private void speakAndShowResp(String text, int inputType, SpeechMsgBuilder msgBuilder) {
        /* 发送回复文本到聊天视图 */
        EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(text), null, null, null));
        if (inputType == AssistantService.INPUT_VOICE) {
            msgBuilder.setText(text);
            SynthesizerBase.get().startSpeakAbsolute(msgBuilder.build())
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
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .subscribe();
        }
    }

    /**
     * 检测未接来电、未读短信
     **/
    public boolean missedContact() {
        int size;
        SpeechMsgBuilder msgBuilder = SpeechMsgBuilder.create(null);
        if ((size = mAppConfig.missedCallContacts.size()) > 0) {
            StringBuilder sb = new StringBuilder();
            if (size == 1) {
                Contact c = mAppConfig.missedCallContacts.poll();
                sb.append("刚才有来自").append(c.getName()).append("的未接来电！");
            } else {
                mAppConfig.missedCallContacts.clear();
                sb.append("在您刚才通话的时候，有").append(size).append("个未接来电！");
            }
            speakAndShowResp(sb.toString(), AssistantService.INPUT_VOICE, msgBuilder);
            return true;
        } else if ((size = mAppConfig.missedMsgs.size()) > 0) {
            if (size == 1) {
                /* 记录最新一条未查看短信 */
                SmsInfo sms = mAppConfig.missedMsgs.poll();
                mAppConfig.lastSms.setTime(sms.getTime());
                mAppConfig.lastSms.setContent(sms.getContent());
                mAppConfig.lastSms.setNumber(sms.getNumber());
                mAppConfig.lastSms.setName(sms.getName());
                mAppConfig.lastSms.setType(sms.getType());
                mAppConfig.lastSms.setContact(sms.getContact());
                mHandler.sendEmptyMessage(1);
            } else {
                SmsInfo sms = null, temp;
                while ((temp = mAppConfig.missedMsgs.poll()) != null) {
                    sms = temp;
                }
                /* 记录最新一条未查看短信 */
                mAppConfig.lastSms.setTime(sms.getTime());
                mAppConfig.lastSms.setContent(sms.getContent());
                mAppConfig.lastSms.setNumber(sms.getNumber());
                mAppConfig.lastSms.setName(sms.getName());
                mAppConfig.lastSms.setType(sms.getType());
                mAppConfig.lastSms.setContact(sms.getContact());
                Intent smsIntent = new Intent(mContext, AssistantService.class);
                smsIntent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PUSH_ROUTE_CACULATE);
                smsIntent.putExtra(AssistantService.TEXT, "是否依次朗读短信3");
                String text = "你有多条未读短信，需要依次朗读吗？";
                msgBuilder.setContextMode(SpeechMsg.CONTEXT_KEEP_RECOGNIZE);
                speakAndShowResp(text, AssistantService.INPUT_VOICE, msgBuilder);
            }
            return true;
        }
        return false;
    }

    @Override
    public void smsMsgHandle() {
        Contact c = mAppConfig.lastSms.getContact();
        if (mAppConfig.notInDND(c)&& mAppConfig.inmsg_tips) {
            AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            mAudioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
            tPools.execute(new NumberMsg(mAppConfig.lastSms.getName(), false));
        } else {
            inMsgTipsFlow = 0;
        }
    }

    @Override
    public void receiveSms(SmsInfo sms, StringBuilder number) {
        isSmsReceiverValid = true;
        Contact c = AppConfig.mContactUtils.getContactByNum(number);
        if (c == null) {
            c = new Contact();
            c.setName(number.toString());
            List<ContactNum> codes = new ArrayList<>();
            ContactNum contactNum = new ContactNum();
            contactNum.setNumber(c.getName());
            codes.add(contactNum);
            c.setCodes(codes);
            sms.setName(AssistantService.UNKOWN_NAME);
        } else {
            sms.setName(c.getName());
        }
        sms.setNumber(number.toString());
        sms.setType(1);
        sms.setContact(c);

        if (voiceMediator.mobileRing() || voiceMediator.isCalling() || inMsgTipsFlow > 0) {//通话中，响铃中，来信提示中，均不允许新来的短信打断
            mAppConfig.missedMsgs.offer(sms);
        } else {
            inMsgTipsFlow = 1;
            mAppConfig.lastSms.setTime(sms.getTime());
            mAppConfig.lastSms.setContent(sms.getContent());
            mAppConfig.lastSms.setNumber(sms.getNumber());
            mAppConfig.lastSms.setName(sms.getName());
            mAppConfig.lastSms.setType(sms.getType());
            mAppConfig.lastSms.setContact(sms.getContact());
            if (mAppConfig.notInDND(c) /*&& mAppConfig.CardMode*/ && mAppConfig.inmsg_tips) {
                AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                mAudioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
                tPools.execute(new NumberMsg(mAppConfig.lastSms.getName(), false));
            } else {
                inMsgTipsFlow = 0;
            }
        }
        CallAndSmsDao.getInstance(mContext).sync(CallAndSmsDao.getInstance(mContext).getSyncDao(CallAndSmsDao.MessageDao.class));
    }

    /**
     * 手机短信数据库变化观察者（与内容观察者配合，动态响应数据变化）
     **/
    class SmsObserver extends ContentObserver {

        public SmsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.e(TAG, "SmsObserver>>>>onChange>>" + Boolean.toString(selfChange));
            CallAndSmsDao.getInstance(mContext).sync(CallAndSmsDao.getInstance(mContext).getSyncDao(CallAndSmsDao.MessageDao.class));
            if (isSmsReceiverValid) {     //若已被短信广播接收者拦截则不需要再处理
                isSmsReceiverValid = false;
                return;
            }
            super.onChange(selfChange);
            if (!selfChange) {
                long t = mAppConfig.lastSms.getTime();
                SmsInfo sms = new SmsInfo();
                if (t == AppConfig.mContactUtils.getLastMessage(t, sms)) {
                    Log.e(TAG, "SmsObserver>>>>onChange>>重复触发");
                    return;
                }
                if (voiceMediator.mobileRing() || voiceMediator.isCalling() || inMsgTipsFlow > 0) {//通话中，响铃中，来信提示中，均不允许新来的短信打断
                    mAppConfig.missedMsgs.offer(sms);
                } else {
                    inMsgTipsFlow = 1;
                    mAppConfig.lastSms.setTime(sms.getTime());
                    mAppConfig.lastSms.setContent(sms.getContent());
                    mAppConfig.lastSms.setNumber(sms.getNumber());
                    mAppConfig.lastSms.setName(sms.getName());
                    mAppConfig.lastSms.setType(sms.getType());
                    mAppConfig.lastSms.setContact(sms.getContact());
                    mHandler.sendEmptyMessage(1);
                }
            }
        }
    }

    /**
     * 监听电话的接听和结束事件
     */
    private void phoneCallListener() {
        //电话状态监听
        TelephonyManager telManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        telManager.listen(new MobliePhoneStateListener(),
                PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     * 监听电话的接听和结束事件的具体实现
     *
     * @author Administrator
     */
    class MobliePhoneStateListener extends PhoneStateListener {
        private boolean playing = false;
        private boolean preCall = false;
        private int ringMode;

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            mobileState = state;
            if (mAppConfig == null)
                return;
            switch (mobileState) {
                case TelephonyManager.CALL_STATE_IDLE:  //无任何状态时
                    Log.e("MobliePhoneStateListener", "电话空闲的时候");
                    voiceMediator.setMobileRing(false);
                    voiceMediator.setCalling(false);
                    if (!preCall) {
                        //通话前空闲
                        return;
                    }
                    /* 通话后空闲 */
                    preCall = false;
                    mAppConfig.speaker_on_one = false;
                    //closeSpeaker();
                    if (ringMode == AudioManager.RINGER_MODE_NORMAL) {
                        if (mAppConfig.incoming_tips) {
                            //电话铃声静音
                            mAudioManager.setStreamMute(AudioManager.STREAM_RING, false);
                        }
                    }
                    if ((mAppConfig.incoming_speaker_on || voiceMediator.isWakeUpMode()) && !voiceMediator.isHeadset() && ringMode == AudioManager.RINGER_MODE_NORMAL) {
                        mAudioManager.setSpeakerphoneOn(false);
                    }
                    if (playing) {
                        playing = false;
                        LingjuAudioPlayer.get().play();
                    } else if (mSynthesizer != null) {
                        Intent intent = new Intent(mContext, AssistantService.class);
                        intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.SEND_TO_ROBOT_FOR_END_TASK);
                        intent.putExtra(AssistantService.END_TASK, false);
                        mContext.startService(intent);
                    }
                    //同步通话记录
                    CallAndSmsDao.getInstance(mContext).sync(CallAndSmsDao.getInstance(mContext).getSyncDao(CallAndSmsDao.CallLogDao.class));
                    missedContact();
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.e("MobliePhoneStateListener", "电话接通的时候");
                    voiceMediator.setMobileRing(true);
                    voiceMediator.setCalling(false);
                    if (mAppConfig.missedCallContacts.size() == 1)
                        mAppConfig.missedCallContacts.clear();
                    voiceMediator.stopRecognize();
                    voiceMediator.stopSynthesize();
                    //mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    if (mAppConfig.speaker_on_one || ((mAppConfig.incoming_speaker_on || voiceMediator.isWakeUpMode()) && ringMode == AudioManager.RINGER_MODE_NORMAL && !voiceMediator.isHeadset())) {
                        tPools.execute(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    Log.e(TAG, "CALL_STATE_OFFHOOK openSpeaker");
                                    if (voiceMediator.openSpeaker())
                                        return;
                                    int i = 3;
                                    while (i-- > 0) {
                                        Log.e(TAG, "sleep 500ms to openSpeaker");
                                        Thread.sleep(500);
                                        if (voiceMediator.openSpeaker())
                                            break;
                                    }
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                    preCall = true;
                    if (LingjuAudioPlayer.get().isPlaying()) {
                        playing = true;
                        LingjuAudioPlayer.get().pause();
                    }
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    Log.e("MobliePhoneStateListener", "电话忙的时候");
                    //mobileRing=true;
                    voiceMediator.setCalling(true);
                    preCall = true;
                    if (LingjuAudioPlayer.get().isPlaying()) {
                        playing = true;
                        LingjuAudioPlayer.get().pause();
                    }
                    voiceMediator.stopRecognize();
                    voiceMediator.stopSynthesize();
                    voiceMediator.stopWakenup();

                    StringBuilder number = new StringBuilder(incomingNumber);
                    if (incomingNumber.startsWith("+86"))
                        number.delete(0, 3);
                    Contact c = AppConfig.mContactUtils.getContactByNum(number);

                    String name = number.toString();
                    if (c != null) {
                        name = c.getName();
                        Log.e(TAG, "name=" + name + ",number=" + number);
                        AppConfig.mContactUtils.reSortList(c, number.toString(), System.currentTimeMillis());
                    } else {
                        c = new Contact();
                        c.setName(number.toString());
                        List<ContactNum> codes = new ArrayList<>();
                        ContactNum contactNum = new ContactNum();
                        contactNum.setNumber(number.toString());
                        codes.add(contactNum);
                        c.setCodes(codes);
                    }

                    if (voiceMediator.mobileRing()) {
                        Log.w(TAG, "当前正在通话中，此时又来了电话");
                        mAppConfig.missedCallContacts.offer(c);
                    } else {
                        voiceMediator.setMobileRing(true);
                        ringMode = mAudioManager.getRingerMode();
                        if (mAppConfig.notInDND(c) && ringMode == AudioManager.RINGER_MODE_NORMAL) {
                            if (mAppConfig.incoming_tips) {
                                //静音
                                mAudioManager.setStreamMute(AudioManager.STREAM_RING, true);
                                tPools.execute(new NumberMsg(name, true));
                            }
                        }
                        if (mAppConfig.incoming_speaker_on || (!mAudioManager.isSpeakerphoneOn() && (mAppConfig.incoming_speaker_on || voiceMediator.isWakeUpMode()) && !voiceMediator.isHeadset() && ringMode == AudioManager.RINGER_MODE_NORMAL)) {
                            mAudioManager.setSpeakerphoneOn(true);
                        }
                    }
                    break;
                default:
                    break;
            }

            Log.e("MobliePhoneStateListener", "Incoming number " + incomingNumber); //incomingNumber就是来电号码
        }
    }

    /**
     * 来电、接收短信通知播报
     **/
    class NumberMsg implements Runnable {
        private String name;
        private boolean isCall;

        public NumberMsg(String name, boolean call) {
            this.name = name;
            this.isCall = call;
        }

        @Override
        public void run() {
            voiceMediator.stopWakenup();
            String text;
            SpeechMsgBuilder msgBuilder = SpeechMsgBuilder.create(null);
            if (isCall) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (mobileState != TelephonyManager.CALL_STATE_RINGING) {
                    return;
                }
                text = "您有电话来自" + name + "，请点击屏幕接通电话！";
                /* 获取来电播报次数 */
                int times = AppConfig.dPreferences.getInt(AppConfig.INCOMING_TIPS_TIMES, 0);
                switch (times) {
                    case R.id.aitt_button1:
                        times = 1;
                        break;
                    case R.id.aitt_button2:
                        times = 2;
                        break;
                    case R.id.aitt_button3:
                        times = 3;
                        break;
                    case R.id.aitt_button4:
                        times = 6;
                        break;
                    default:
                        times = 2;
                        break;
                }
                StringBuilder sb = new StringBuilder();
                while (times-- > 0) {
                    sb.append(text);
                }
                text = sb.toString();
                msgBuilder.setForceLocalEngine(true);
                msgBuilder.setContextMode(SpeechMsg.CONTEXT_AUTO);
            } else {
                Intent smsIntent = new Intent(mContext, AssistantService.class);
                smsIntent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PUSH_ROUTE_CACULATE);
                smsIntent.putExtra(AssistantService.TEXT, "有短信发来3");
                text = "您有短信来自" + name + ",如果需要朗读，请对我说朗读短信！";
                msgBuilder.setContextMode(SpeechMsg.CONTEXT_KEEP_RECOGNIZE);
                msgBuilder.setForceLocalEngine(true);
            }
            // final AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            //             current_volume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            //            if (!voiceMediator.isHeadset()) {
            //                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, current_volume, 0);
            //            }
            msgBuilder.setText(text);
            Observable<SpeechMsg> msgObservable = mSynthesizer.addMessageWaitSpeak(msgBuilder.build());
            if (msgObservable != null) {
                msgObservable.doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
//                        if (!voiceMediator.isHeadset())
//                            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, current_volume, 0);
                    }
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation())
                        .subscribe();
            }
        }

    }
}
