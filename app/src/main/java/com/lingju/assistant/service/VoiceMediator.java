package com.lingju.assistant.service;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AmrEncoder;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.util.UserWords;
import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.event.ChatMsgEvent;
import com.lingju.assistant.activity.event.LongRecognizeEvent;
import com.lingju.assistant.activity.event.NavigateEvent;
import com.lingju.assistant.activity.event.RecordUpdateEvent;
import com.lingju.assistant.activity.event.SynthesizeEvent;
import com.lingju.assistant.activity.event.VolumeChangedEvent;
import com.lingju.assistant.entity.CmdAction;
import com.lingju.assistant.player.audio.LingjuAudioPlayer;
import com.lingju.assistant.receiver.MediaButtonReceiver;
import com.lingju.assistant.service.event.HeadSetEvent;
import com.lingju.assistant.service.process.DefaultProcessor;
import com.lingju.assistant.social.weibo.Constants;
import com.lingju.audio.PcmRecorder;
import com.lingju.audio.SystemVoiceMediator;
import com.lingju.audio.engine.IflyRecognizer;
import com.lingju.audio.engine.IflySynthesizer;
import com.lingju.audio.engine.VoiceAwakener;
import com.lingju.audio.engine.base.RecognizerBase;
import com.lingju.audio.engine.base.SpeechMsg;
import com.lingju.audio.engine.base.SpeechMsgBuilder;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.audio.engine.base.WakeupEngineBase;
import com.lingju.common.adapter.ChatRobotBuilder;
import com.lingju.common.log.Log;
import com.lingju.config.Setting;
import com.lingju.context.entity.TapeEntity;
import com.lingju.lbsmodule.proxy.BNavigatorProxy;
import com.lingju.model.PlayMusic;
import com.lingju.model.Tape;
import com.lingju.model.dao.TapeEntityDao;
import com.lingju.model.temp.speech.ResponseMsg;
import com.lingju.robot.AndroidChatRobotBuilder;
import com.lingju.util.NetUtil;
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Administrator on 2016/11/5.
 */
public class VoiceMediator implements SystemVoiceMediator {
    private final static String TAG = "VoiceMediator";

    public final static int AUTO_TYPE = 0;
    public final static int XIMALAYA_TYPE = 1;
    private static VoiceMediator instance;
    //识别引擎
    protected RecognizerBase recognizer;
    //合成引擎
    protected SynthesizerBase synthesizer;
    //唤醒引擎
    protected WakeupEngineBase awakener;

    private ChatStateListener chatListener;
    private Context mContext;
    private boolean isHeadSet;
    private BluetoothA2dp bluetoothA2dp;
    private BluetoothHeadset bluetoothHeadset;
    private boolean isBlueHeadSet;
    private boolean suportA2DP;
    private int remindDialogFlag;
    private CmdAction currentAction;        //记录最新一次对话处于什么动作场景，用于判断不说话时是否推送NOANSWER
    /**
     * 是否处于唤醒录音模式状态标记
     **/
    private boolean is_wakeup_mode = false;
    /**
     * 音频播放类型（0：歌曲  1：有声内容）
     **/
    private int audioPlayType = AUTO_TYPE;
    /**
     * 是否正在通话中
     */
    private boolean mobileRing = false;
    /**
     * 是否处于来电响铃中
     */
    private boolean calling = false;
    private int mCurrentVolume;     //当前媒体音量
    private AudioManager mAudioManager;
    private AtomicBoolean robotResponse = new AtomicBoolean(false);
    protected SoundPool soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 100);
    protected int waitID;
    protected int waitPlayID;
    private boolean is_walk_navi;

    protected VoiceMediator(Context context) {
        this.mContext = context;
        initVoiceComponent();
        try {
            waitID = soundPool.load(mContext.getAssets().openFd(mContext.getResources().getString(R.string.audio_wait_response)), 1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            EventBus.getDefault().register(this);
        }
    }

    public static synchronized VoiceMediator create(Context context) {
        if (instance == null)
            instance = new VoiceMediator(context);
        return instance;
    }

    public static VoiceMediator get() {
        return instance;
    }

    protected void initVoiceComponent() {
        Log.i(TAG, "initVoiceComponent>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        /*tPools.execute(new Runnable() {
            @Override
			public void run() {
				for(String s:BaiduSynthesizer.OFFLINE_RS){
					copyFromAssetsToSdcard(RobotApplication.firstOpen,s,Setting.DEFAULT_DIR+"/"+s);
				}
				synthesizer = BaiduSynthesizer.createInstance(RobotService.this);

			}
		});*/
        //recognizer= BaiduRecognizer.createInstance(RobotService.this);
        SpeechUtility.createUtility(mContext, SpeechConstant.APPID + "=" + Constants.XUNFEI_APPID +
                "," + SpeechConstant.MODE_MSC + "=" + SpeechConstant.MODE_AUTO);
        //com.iflytek.cloud.Setting.setLogLevel(Setting.LOG_LEVEL.detail);
        com.iflytek.cloud.Setting.setShowLog(false);
        com.iflytek.cloud.Setting.setLocationEnable(true);
        Single.just(0)
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        recognizer = IflyRecognizer.createInstance(mContext, VoiceMediator.this);
                        synthesizer = IflySynthesizer.createInstance(mContext, VoiceMediator.this);
                        awakener = VoiceAwakener.createInstance(mContext, VoiceMediator.this);
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .subscribe();
        Log.i(TAG, "initVoiceComponent>>>>>>>>>>>>>>>>>>>>>>>>>>>END");
    }

    /**
     * 耳机操作事件处理
     **/
    @Subscribe
    public void onHeadSetEvent(HeadSetEvent e) {
        switch (e) {
            case Connect:
                this.isHeadSet = true;
                getHook();
                EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(mContext.getResources().getString(R.string.headset_mode_tips)), null, null, null));
                break;
            case Disconnect:
                this.isHeadSet = false;
                if (mAudioManager != null)
                    mAudioManager.abandonAudioFocus(mFocusChangeListener);
                break;
            case Click:
                break;
            case DoubleClick:
                break;
            case TrebleClick:
                break;
        }
    }

    /**
     * 获取音频焦点，夺回耳机按键广播
     **/
    public void getHook() {
        if (AppConfig.dPreferences.getBoolean(AppConfig.ALLOW_WIRE, true)) {
            if (mAudioManager == null)
                mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            // 抢回焦点
            /** 由于耳机广播注册对象是单例且广播接受者唯一的，如果接收者栈中已存在该接收者对象则直接返回。
             * 所以在重新注册本应用的耳机广播接收者之前需要先注销已失效但仍存在的接收者 **/
            mAudioManager.unregisterMediaButtonEventReceiver(new ComponentName(mContext.getApplicationContext(), MediaButtonReceiver.class));
            isFocus = mAudioManager.requestAudioFocus(mFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
                    != AudioManager.AUDIOFOCUS_REQUEST_FAILED;
            /* 注册耳机广播接收者 */
            mAudioManager.registerMediaButtonEventReceiver(new ComponentName(mContext.getApplicationContext(), MediaButtonReceiver.class));
            Log.i("LingJu", "MainActivity getHook():" + mAudioManager.isMusicActive());
        }
    }

    public boolean isFocus() {
        return isFocus;
    }

    /**
     * 是否获取到音频焦点标记
     **/
    private boolean isFocus = true;
    private AudioManager.OnAudioFocusChangeListener mFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.i("LingJu", "焦点状态：" + focusChange);
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    isFocus = false;
                    mAudioManager.abandonAudioFocus(mFocusChangeListener);
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    Log.i("LingJu", "MainActivity onAudioFocusChange()");
                    isFocus = true;
                    break;
            }
        }
    };

    public void setBluetoothHeadset(BluetoothHeadset bluetoothHeadset) {
        this.bluetoothHeadset = bluetoothHeadset;
    }

    public void setBluetoothA2dp(BluetoothA2dp bluetoothA2dp) {
        this.bluetoothA2dp = bluetoothA2dp;
    }

    @Override
    public BluetoothHeadset getBluetoothHeadset() {
        return bluetoothHeadset;
    }

    @Override
    public BluetoothA2dp getBluetoothA2dp() {
        return bluetoothA2dp;
    }

    public void setBlueHeadSet(boolean blueHeadSet) {
        isBlueHeadSet = blueHeadSet;
    }

    public void setSuportA2DP(boolean suportA2DP) {
        this.suportA2DP = suportA2DP;
    }

    /**
     * 设置合成参数
     **/
    public void setSynthParams(NetUtil.NetType type) {
        synthesizer.resetParam(type);
    }

    @Override
    public boolean isPlaying() {
        return LingjuAudioPlayer.get().isPlaying();
    }

    @Override
    public boolean isTinging() {
        return XmPlayerManager.getInstance(mContext).isPlaying();
    }

    @Override
    public void setAudioPlayType(int playType) {
        this.audioPlayType = playType;
    }

    @Override
    public int getAudioPlayType() {
        return audioPlayType;
    }

    @Override
    public boolean isHeadset() {
        return isHeadSet;
    }

    public void setHeadSet(boolean isHeadSet) {
        this.isHeadSet = isHeadSet;
    }

    @Override
    public boolean isWakeUpMode() {
        return is_wakeup_mode;
    }

    @Override
    public void setWakeupModeFlag(boolean isWakeUpMode) {
        is_wakeup_mode = isWakeUpMode;
    }

    @Override
    public boolean isBlueToothHeadSet() {
        return isBlueHeadSet;
    }

    public boolean isSuportA2DP() {
        return suportA2DP;
    }

    @Override
    public void openBlueHeadSet4Recognition() {
        if (isBlueToothHeadSet() && getBluetoothHeadset() != null && !getBluetoothHeadset().getConnectedDevices().isEmpty()) {
            int c = 0;
            BluetoothDevice device = getBluetoothHeadset().getConnectedDevices().get(0);
            while (isBlueToothHeadSet() && !getBluetoothHeadset().isAudioConnected(device)) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "c=" + c);
                c++;
                if (c > 30) {
                    break;
                }
            }
        }
    }

    @Override
    public void startBluetoothSco() {
        if (isBlueToothHeadSet()) {
            BluetoothHeadset headset = getBluetoothHeadset();
            if (headset == null || headset.getConnectedDevices().isEmpty() || headset.isAudioConnected(headset.getConnectedDevices().get(0)))
                return;
            Log.e(TAG, "startBluetoothSco:device size=" + headset.getConnectedDevices().size());
            headset.startVoiceRecognition(headset.getConnectedDevices().get(0));
        }
    }

    @Override
    public void stopBluetoothSco() {
        if (isBlueToothHeadSet() && getBluetoothHeadset() != null && !getBluetoothHeadset().getConnectedDevices().isEmpty()) {
            if (getBluetoothHeadset().isAudioConnected(getBluetoothHeadset().getConnectedDevices().get(0))) {
                Log.e(TAG, "stopBluetoothSco>>stopBluetoothSco");
                getBluetoothHeadset().stopVoiceRecognition(getBluetoothHeadset().getConnectedDevices().get(0));
                try {
                    int c = 0;
                    while (getBluetoothHeadset().isAudioConnected(getBluetoothHeadset().getConnectedDevices().get(0))) {
                        Thread.sleep(100);
                        Log.e(TAG, "stopBluetoothSco c=" + c);
                        c++;
                        if (c > 10) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void stopSynthesize() {
        if (synthesizer != null) {
            Log.i(TAG, "stopsynthe................");
            synthesizer.stopSpeakingAbsolte();
        }
    }

    /**
     * 停止识别和说话并且尝试打开唤醒（如果当前为唤醒模式则可以打开）
     **/
    public void stopSpeakAndWakeup(boolean wakeup) {
        stopRecognize();
        synthesizer.stopSpeakingAbsolte();
        if (wakeup)
            tryToWakeup();
    }

    /**
     * 尝试打开唤醒（如果当前为唤醒模式则可以打开）
     **/
    @Override
    public void tryToWakeup() {
        Log.i("LingJu", "VoiceMediator tryToWakeup()>>> " + is_wakeup_mode);
        if (is_wakeup_mode)
            startWakeup();
    }

    @Override
    public void changeMediaVolume(int percent) {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mCurrentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volume = (int) ((percent / 100.0) * maxVolume + 0.5);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_SAME, AudioManager.FLAG_PLAY_SOUND
                        | AudioManager.FLAG_SHOW_UI);
    }

    @Override
    public void recordCurrentVolume() {
        is_walk_navi = true;
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mCurrentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void resumeMediaVolume() {
        is_walk_navi = false;
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrentVolume, 0);
    }

    @Override
    public boolean isWalkNavi() {
        return is_walk_navi;
    }

    @Override
    public void stopRecognize() {
        Log.i(TAG, "stopRecognize");
        if (recognizer != null) {
            recognizer.stopRecognize();
        }
    }

    @Override
    public void onWakenup(String wakeupWord) {
        /* 唤醒成功，开启识别 */
        EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECORDING));
        startRecognize();
    }

    @Override
    public void sendMsg2Robot(String msg) {
        if (chatListener != null)
            chatListener.onInput(msg);
    }

    @Override
    public void setWakeUpMode(boolean flag) {
        if (synthesizer != null) {
            is_wakeup_mode = flag;

            AppConfig.dPreferences.edit().putBoolean(AppConfig.WAKEUP_MODE, flag).commit();
            if (flag) {
                if (synthesizer.isSpeaking() || recognizer.isRecognizing()) {
                    return;
                } else {
                    startWakeup();
                }
            } else {
                stopWakenup();
            }
        }
    }

    @Override
    public boolean allowSynthersize(SpeechMsg msg) {
        boolean result = true;
        try {
            if (BNavigatorProxy.getInstance().isNaviBegin() && msg.origin() == SpeechMsg.ORIGIN_DEFAULT) {
                msg.getBuilder().setOrigin(SpeechMsg.ORIGIN_COMMON);
            }
            /*if (!isHeadSet) {     //合成声音先关闭唤醒
                stopWakenup();
            }*/
            if (msg.priority() == SpeechMsg.PRIORITY_ABOVE_RECOGNIZE) {     //合成优先
                Intent intent = new Intent(mContext, AssistantService.class);
                intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_RECOGNIZE);
                mContext.startService(intent);
                EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECORD_IDLE));
            } else {
                if (recognizer.isRecognizing()) {   //识别优先
                    result = false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, "allowSynthersize>>" + Boolean.toString(result));
        return result;
    }

    @Override
    public boolean compareSpeechMsg(SpeechMsg nSpeechMsg, SpeechMsg currentSpeechMsg) {
        if (currentSpeechMsg != null && currentSpeechMsg.origin() == SpeechMsg.ORIGIN_NAVI) {
            if (nSpeechMsg.origin() != SpeechMsg.ORIGIN_NAVI) {
                if (nSpeechMsg.origin() == SpeechMsg.ORIGIN_DEFAULT)
                    nSpeechMsg.getBuilder().setOrigin(SpeechMsg.ORIGIN_COMMON);
                return false;
            }
        }
        return true;
    }

    public void setCurrentAction(CmdAction action) {
        this.currentAction = action;
    }

    @Override
    public void onRecognizeError(int code, String msg) {
        EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECORD_IDLE_AFTER_RECOGNIZED));
        if (recognizer.isPlayingBeforRecognize()) {
            EventBus.getDefault().post(LingjuAudioPlayer.get().currentPlayMusic());
            LingjuAudioPlayer.get().play();
        }
        if (recognizer.isTingBeforeRecognize()) {
            XmPlayerManager.getInstance(mContext).play();
        }
        tryToWakeup();
        boolean showError = false;
        switch (code) {
            case ErrorCode.MSP_ERROR_TIME_OUT://10114
            case ErrorCode.ERROR_NETWORK_TIMEOUT://20002
            case ErrorCode.ERROR_NET_EXCEPTION://20003
            case ErrorCode.ERROR_NO_NETWORK://20001
                msg = Setting.RECOGNIZE_NETWORK_ERROR;
                break;
            case 20005:
                msg = Setting.RECOGNIZE_NOMATCH_ERROR;
                break;
            case ErrorCode.MSP_ERROR_NO_DATA://10118
                // TODO: 2017/5/18 等待拨号、发短信时不做任何回复
                if ((currentAction == null || ((currentAction.getOutc() & DefaultProcessor.OUTC_ASK) != DefaultProcessor.OUTC_ASK))
                        && !EventBus.getDefault().hasSubscriberForEvent(NavigateEvent.class)) {
                    msg = Setting.RECOGNIZE_NODATA_ERROR;
                } else {
                    Intent intent = new Intent(mContext, AssistantService.class);
                    intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.SEND_TO_ROBOT);
                    intent.putExtra(AssistantService.TEXT, ChatRobotBuilder.NOANSWER);
                    mContext.startService(intent);
                    return;
                }
                break;
            case 20017:
                msg = "很抱歉，识别引擎出错，请关闭程序重试！";
                showError = true;
                break;
            case ErrorCode.ERROR_AUDIO_RECORD:
                msg = msg + "，请您在设置-应用管理-应用详情下的权限管理检查是否允许录音权限";
                showError = true;
                break;
            default:
                msg = Setting.RECOGNIZE_ERROR_TIPS;
                break;
        }
        if (showError) {
            EventBus.getDefault().post(new SynthesizeEvent());
        }
        if (!TextUtils.isEmpty(msg)/* && code == ErrorCode.MSP_ERROR_NO_DATA*/) {
            EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(msg), null, null, null));
            speak(new SpeechMsgBuilder(msg), true);
        }
    }

    private void speak(SpeechMsgBuilder msgBuilder, final boolean isAnim) {
        SynthesizerBase.get().startSpeakAbsolute(msgBuilder.build())
                .doOnNext(new Consumer<SpeechMsg>() {
                    @Override
                    public void accept(SpeechMsg speechMsg) throws Exception {
                        if (speechMsg.state() == SpeechMsg.State.OnBegin && isAnim)
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

    @Override
    public void onRecoginzeWait() {
        EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECOGNIZING));
    }

    @Override
    public void onRecoginzeResult(String result) {
        Log.i(TAG, "onRecoginzeResult>>" + result);
        if (EventBus.getDefault().hasSubscriberForEvent(NavigateEvent.class)) {
            EventBus.getDefault().post(new NavigateEvent(NavigateEvent.STOP_COUNTDOWN));
        }
        if (recognizer.isPlayingBeforRecognize()) {
            PlayMusic music = LingjuAudioPlayer.get().currentPlayMusic();
            if (music != null)
                EventBus.getDefault().post(music);
            LingjuAudioPlayer.get().play();
        }
        if (recognizer.isTingBeforeRecognize()) {
            XmPlayerManager.getInstance(mContext).play();
        }
        EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECORD_IDLE_AFTER_RECOGNIZED));
        if (!TextUtils.isEmpty(result) && !"。".equals(result)) {
            //将识别结果发送给机器人
            if (chatListener != null) {
                robotResponse.set(false);
                Observable.just(0)
                        .delay(750, TimeUnit.MILLISECONDS)
                        .filter(new Predicate<Integer>() {
                            @Override
                            public boolean test(Integer integer) throws Exception {
                                Log.i("LingJu", "过滤器：" + (!robotResponse.get()));
                                return !robotResponse.get();
                            }
                        })
                        .doOnNext(new Consumer<Integer>() {
                            @Override
                            public void accept(Integer integer) throws Exception {
                                if (!robotResponse.get()) {
                                    Log.i("LingJu", "播放等待提示音");
                                    AudioManager mgr = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                                    float streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
                                    float streamVolumeMax = mgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                                    float volume = streamVolumeCurrent / streamVolumeMax;
                                    waitPlayID = soundPool.play(waitID, volume, volume, 1, -1, 1f);
                                }
                            }
                        })
                        .observeOn(Schedulers.io())
                        .subscribeOn(Schedulers.io())
                        .subscribe();
                chatListener.onInput(result);
                /* 添加模式不需要将识别文本添加聊天视图 */
                if (result.contains("&"))
                    result = result.substring(0, result.lastIndexOf("&"));
                EventBus.getDefault().post(new ChatMsgEvent(new com.lingju.model.temp.speech.SpeechMsg(result), null, null, null));
            }
        } else {
            onRecognizeError(ErrorCode.MSP_ERROR_NO_DATA, "");
        }
    }

    /**
     * 结束录音模式时调用
     **/
    @Override
    public void onTapeResult(final String tapeContent) {
        if (TextUtils.isEmpty(tapeContent)) {
            currentAction.setOutc(1);
            onRecognizeError(ErrorCode.MSP_ERROR_NO_DATA, Setting.RECOGNIZE_NODATA_ERROR);
            return;
        }
        //退出录音模式
        IflyRecognizer.getInstance().setRecognizeMode(false);
        String tapePath = PcmRecorder.TAPE_SRC + System.currentTimeMillis() + ".amr";
        //压缩并保存录音文件
        AmrEncoder.pcm2Amr(PcmRecorder.TEMP_FILE, tapePath);
        final Tape tape = new Tape();
        tape.setText(tapeContent);
        tape.setUrl(tapePath);
        tape.setCreated(new Date());
        tape.setSynced(false);
        //保存录音记录
        TapeEntityDao.getInstance().insertTape(tape);
        //上传录音对象方式1(新策略，暂未实现)
        // uploadTapeEntity(tape);
        // Log.i("LingJu", "onSyncComplete()>>>" + tape.getSid());

        TapeEntityDao.getInstance().setOnSyncListener(new TapeEntityDao.OnSyncListener() {
            @Override
            public void onSyncComplete() {
                Log.i("LingJu", "onSyncComplete()>>>" + tape.getSid());
                onRecoginzeResult(tapeContent + "&" + tape.getSid());
            }
        });
        ////上传录音对象方式2(同步记录)
        TapeEntityDao.getInstance().sync();
    }

    private void uploadTapeEntity(Tape tape) {
        TapeEntity entity = new TapeEntity();
        entity.setText(tape.getText());
        entity.setUrl(tape.getUrl());
        entity.setCreated(tape.getCreated());
        entity.setModified(tape.getModified());
        entity.setLid(tape.getId().intValue());
        entity.setSid(tape.getSid());
        entity.setRecyle(tape.getRecyle());
        entity.setSynced(tape.getSynced());
        entity.setTimestamp(tape.getTimestamp());
        AndroidChatRobotBuilder.get().robot().append(entity);
    }

    @Override
    public void stopWaitPlay() {
        soundPool.stop(waitPlayID);
    }

    @Override
    public void setRobotResponse(boolean hasResponse) {
        robotResponse.set(hasResponse);
    }

    /**
     * 长时间录音识别结果回调，该模式下不需要将识别文本作为对话输出
     **/
    @Override
    public void onLongRecoginzeResult(String result) {
        EventBus.getDefault().post(new LongRecognizeEvent(result));
    }

    @Override
    public void startRecognize() {
        Log.i(TAG, "startRecognize");
        if (synthesizer.isSpeaking()) {
            synthesizer.stopSpeakingAbsolte();
        }
        recognizer.startRecognize();
    }

    /**
     * 提醒提前天数弹窗设置标记
     **/
    public void setRemindDialogFlag(int flag) {
        //0代表默认、1代表提醒天数弹窗
        remindDialogFlag = flag;
    }

    /**
     * 根据msg设置的模式进行对应的语音操作
     **/
    @Override
    public void keepVoiceCtrl(SpeechMsg msg) {
        switch (msg.contextMode()) {
            case SpeechMsg.CONTEXT_KEEP_RECOGNIZE:
                if (msg.state() == SpeechMsg.State.Completed) {
                    startRecognize();
                    //remindDialogFlag为0代表默认、1代表提醒天数弹窗
                    EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECORDING, remindDialogFlag));
                }
                break;
            case SpeechMsg.CONTEXT_KEEP_AWAKEN:
                tryToWakeup();
                break;
            default:
                break;
        }
    }

    @Override
    public void setCalling(boolean isCalling) {
        this.calling = isCalling;
    }

    @Override
    public boolean isCalling() {
        return calling;
    }

    @Override
    public void setMobileRing(boolean mobileRing) {
        this.mobileRing = mobileRing;
    }

    @Override
    public boolean mobileRing() {
        return mobileRing;
    }

    @Override
    public void pausePlay() {
        LingjuAudioPlayer.get().pause();
    }

    @Override
    public void pauseTing() {
        XmPlayerManager.getInstance(mContext).pause();
    }

    @Override
    public void startWakeup() {
        if (awakener != null) {
            awakener.startListening();
        }
    }

    /**
     * 关闭唤醒
     **/
    @Override
    public void stopWakenup() {
        if (awakener != null)
            awakener.stopCompletely();
    }

    @Override
    public void updateLexicon() {
        String[] keywords = mContext.getResources().getStringArray(R.array.keywords);
        UserWords userWords = new UserWords();
        for (String keyword : keywords) {
            userWords.putWord(keyword);
        }
        recognizer.updateLexicon(userWords.toString());
    }

    @Override
    public void onRecoginzeBegin() {
        Log.i(TAG, "onRecoginzeBegin");
        stopWaitPlay();
    }

    @Override
    public void onRecoginzeVolumeChanged(int v) {
        EventBus.getDefault().post(new VolumeChangedEvent(v));
    }

    @Override
    public boolean preToCall() {
        return false;
    }

    @Override
    public void onSynthesizerInited(int code) {

    }

    @Override
    public void onSynthesizerError(String errorMsg) {
        EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(errorMsg), null, null, null));
    }

    //打开扬声器
    @Override
    public boolean openSpeaker() {
        Log.e(TAG, "openSpeaker");
        if (isHeadSet)
            return false;
        try {
            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            switch (mAudioManager.getMode()) {
                case AudioManager.MODE_IN_CALL:
                    System.out.println("MODE_IN_CALL");
                    break;
                case AudioManager.MODE_IN_COMMUNICATION:
                    System.out.println("MODE_IN_COMMUNICATION");
                    break;
                case AudioManager.MODE_NORMAL:
                    System.out.println("MODE_NORMAL");
                    break;
                case AudioManager.MODE_RINGTONE:
                    System.out.println("MODE_RINGTONE");
                    break;
                case AudioManager.MODE_INVALID:
                    System.out.println("MODE_INVALID");
                    break;
            }
            if (!mAudioManager.isSpeakerphoneOn() && mAudioManager.getMode() == AudioManager.MODE_IN_CALL) {
                Log.e(TAG, "openSpeaker>>setSpeakerphoneOn");
                mAudioManager.setSpeakerphoneOn(true);

                mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                        mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                        AudioManager.STREAM_VOICE_CALL);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Handler createHandler() {
        return new Handler(Looper.getMainLooper());
    }

    public void setChatStateListener(ChatStateListener chatListener) {
        this.chatListener = chatListener;
    }
}
