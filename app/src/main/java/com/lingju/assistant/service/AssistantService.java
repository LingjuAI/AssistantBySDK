package com.lingju.assistant.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.AlarmRingingActivity;
import com.lingju.assistant.activity.MainActivity;
import com.lingju.assistant.activity.NaviSetLineActivity;
import com.lingju.assistant.activity.TrafficShowActivity;
import com.lingju.assistant.activity.event.InitNaviManagerEvent;
import com.lingju.assistant.activity.event.MapCmdEvent;
import com.lingju.assistant.activity.event.NaviRouteCalculateEvent;
import com.lingju.assistant.activity.event.NavigateEvent;
import com.lingju.assistant.activity.event.NetWorkEvent;
import com.lingju.assistant.activity.event.RecordUpdateEvent;
import com.lingju.assistant.entity.CmdAction;
import com.lingju.assistant.entity.RobotConstant;
import com.lingju.assistant.player.audio.IBatchPlayer;
import com.lingju.assistant.player.audio.LingjuAudioPlayer;
import com.lingju.assistant.service.event.HeadSetEvent;
import com.lingju.assistant.service.process.AssistProcessor;
import com.lingju.assistant.service.process.AwakenCtrlProcessor;
import com.lingju.assistant.service.process.DefaultProcessor;
import com.lingju.assistant.service.process.MobileCommProcessor;
import com.lingju.assistant.service.process.MusicOptProcessor;
import com.lingju.assistant.service.process.MusicPlayProcessor;
import com.lingju.assistant.service.process.NaviProcessor;
import com.lingju.assistant.service.process.TingPlayProcessor;
import com.lingju.assistant.service.process.base.BaseProcessor;
import com.lingju.assistant.service.process.base.IProcessor;
import com.lingju.assistant.social.weibo.Constants;
import com.lingju.assistant.view.wheel.widget.adapters.NumericWheelAdapter;
import com.lingju.audio.SystemVoiceMediator;
import com.lingju.audio.engine.IflyRecognizer;
import com.lingju.audio.engine.IflySynthesizer;
import com.lingju.audio.engine.base.SpeechMsg;
import com.lingju.audio.engine.base.SpeechMsgBuilder;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.common.adapter.ChatRobotBuilder;
import com.lingju.common.callback.ResponseCallBack;
import com.lingju.common.log.Log;
import com.lingju.common.repository.SyncDao;
import com.lingju.context.entity.SyncSegment;
import com.lingju.context.entity.base.IChatResult;
import com.lingju.lbsmodule.location.BaiduLocateManager;
import com.lingju.model.AlarmClock;
import com.lingju.model.BaiduAddress;
import com.lingju.model.Remind;
import com.lingju.model.SimpleDate;
import com.lingju.model.SmsInfo;
import com.lingju.model.dao.AssistDao;
import com.lingju.model.dao.AssistEntityDao;
import com.lingju.model.dao.BaiduNaviDao;
import com.lingju.model.dao.CallAndSmsDao;
import com.lingju.model.dao.DaoManager;
import com.lingju.model.dao.TapeEntityDao;
import com.lingju.robot.AndroidChatRobotBuilder;
import com.lingju.robot.base.IChatRobot;
import com.lingju.util.NetUtil;
import com.lingju.util.XmlyManager;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class AssistantService extends Service implements SystemVoiceMediator.ChatStateListener, IBatchPlayer.PlayStateListener {

    private final static String TAG = "AssistantService";
    public final static String CMD = "cmd";
    public final static String FLAG = "flag";
    public static final String TEXT = "text";
    public final static String CALLBACK = "callBack";
    public static final String END_TASK = "end_task";
    public static final String TRY_TO_WAKEUP = "try_to_wakeup";
    /**
     * 输入类型
     **/
    public static final String INPUT_TYPE = "input_type";
    /**
     * 键盘输入
     **/
    public static final int INPUT_KEYBOARD = 0;
    /**
     * 语音录入
     **/
    public static final int INPUT_VOICE = 1;

    private VoiceMediator voiceMediator;
    protected IChatRobot chat;
    protected Map<Integer, IProcessor> processors = new Hashtable<>();
    private AppConfig mAppConfig;
    private LingjuAudioPlayer mAudioPlayer;
    private PowerManager mPowerManager;

    public AssistantService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mAppConfig = (AppConfig) getApplication();
        voiceMediator = VoiceMediator.create(this);
        voiceMediator.setChatStateListener(this);
        //创建数据库相关的表及dao
        DaoManager.create(this);
        mAudioPlayer = LingjuAudioPlayer.create(this);
        mAudioPlayer.setPlayStateListener(this);
        mAudioPlayer.setBluetoothChannelController(bluetoothController);
        chatRobotInited();
        // 初始化百度地图SDK组件
        SDKInitializer.initialize(getApplicationContext());
        BaiduLocateManager.createInstance(getApplicationContext());
        NavigatorService.createInstance(BaiduNaviDao.getInstance(), calculateRouteListener, this);
        //初始化喜马拉雅SDK
        XmlyManager.create(this);
        initProcessor();
        registerReveicer();
        isBlueToothHeadsetConnected();
        if (AppConfig.NewInstallFirstOpen)
            checkAudioRecordPermisssion();
        AppConfig.MainServiceStarted = true;
        //百度地图sdk超级奇葩的bug,在部分android4.+的手机测试发现，如果该方法在BaiduNaviManager.getInstance().init调用之后调用，则该方法无效
        SuggestionSearch.newInstance().requestSuggestion(new SuggestionSearchOption().city("广州市").keyword("天河城"));
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Looper.prepare();
                EventBus.getDefault().post(new InitNaviManagerEvent());
            }
        }, 3000);
        setAlarm();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Log.i(TAG, "onStartCommand>>" + intent.getExtras());
            switch (intent.getIntExtra(CMD, 0)) {
                case ServiceCmd.START_RECOGNIZE:
                    voiceMediator.startRecognize();
                    break;
                case ServiceCmd.STOP_RECOGNIZE:
                    stopRecognize();
                    break;
                case ServiceCmd.STOP_WAKEUP_LISTEN:
                    voiceMediator.stopWakenup();
                    break;
                case ServiceCmd.START_WAKEUP_MODE:
                    voiceMediator.setWakeUpMode(true);
                    break;
                case ServiceCmd.CLOSE_WAKEUP_MODE:
                    voiceMediator.setWakeUpMode(false);
                    break;
                case ServiceCmd.STOP_VOICE_MODE:
                    EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECORD_IDLE));
                    voiceMediator.stopRecognize();
                    voiceMediator.stopSynthesize();
                    voiceMediator.stopWakenup();
                    break;
                case ServiceCmd.HEADSET_RECOGNIZE:
                    if (IflyRecognizer.getInstance().isListening()) {
                        stopRecognize();
                    } else {
                        voiceMediator.stopWakenup();
                        EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECORDING));
                        voiceMediator.startRecognize();
                    }
                    break;
                case ServiceCmd.GET_HOOK:
                    voiceMediator.getHook();
                    break;
                case ServiceCmd.PAUSE_PLAY:
                    if (mAudioPlayer.isPlaying()) {
                        mAudioPlayer.pause();
                        voiceMediator.tryToWakeup();
                    }
                    break;
                case ServiceCmd.PLAY_IN_BACKGROUND:
                    // TODO: 2017/3/18 借用通知栏，保证服务在应用退出后仍能持续运行
                    onBackground(intent.getBooleanExtra(FLAG, false));
                    break;
                case ServiceCmd.TOGGLE_PLAY:
                    if (mAudioPlayer.isPlaying()) {
                        mAudioPlayer.pause();
                        voiceMediator.tryToWakeup();
                    } else {
                        // voiceMediator.stopWakenup();
                        if (mAudioPlayer.currentPlayMusic() != null)
                            EventBus.getDefault().post(mAudioPlayer.currentPlayMusic());
                        mAudioPlayer.play();
                    }
                    break;
                case ServiceCmd.NEXT_MUSIC:
                    // voiceMediator.stopWakenup();
                    mAudioPlayer.playNext().subscribe();
                    if (mAudioPlayer.currentPlayMusic() != null)
                        EventBus.getDefault().post(mAudioPlayer.currentPlayMusic());
                    break;
                case ServiceCmd.CLOSE_PLAY_NOTIFICATION:
                    mAudioPlayer.closeNotification();
                    break;
                case ServiceCmd.SEND_TO_ROBOT_FOR_END_TASK:
                    if (intent.getBooleanExtra(END_TASK, true) && chat != null)
                        chat.process("取消", null);
                    EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECORD_IDLE));
                    voiceMediator.stopSpeakAndWakeup(intent.getBooleanExtra(TRY_TO_WAKEUP, true));
                    break;
                case ServiceCmd.SEND_TO_ROBOT:
                    voiceMediator.stopRecognize();
                    voiceMediator.stopSynthesize();
                    String inputText = intent.getStringExtra(TEXT);
                    // int inputType = intent.getIntExtra(INPUT_TYPE, INPUT_VOICE);
                    sendMessageToRobot(inputText, INPUT_VOICE);
                    break;
                case ServiceCmd.SPEAK_REMIND_TEXT:
                    if (voiceMediator != null) {
                        final AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        SpeechMsgBuilder builder = SpeechMsgBuilder.create("您有一条新的提醒通知，请点击查看！");
                        voiceMediator.setSynthParams(NetUtil.NetType.NETWORK_TYPE_NONE);
                        builder.setContextMode(SpeechMsg.CONTEXT_KEEP_AWAKEN);
                    /*final int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    if (!voiceMediator.isHeadset())
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);*/
                        SynthesizerBase.get().startSpeakAbsolute(builder.build())
                            /*.doOnComplete(new Action() {
                                @Override
                                public void run() throws Exception {
                                    if (!voiceMediator.isHeadset())
                                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
                                }
                            })*/
                                .subscribeOn(Schedulers.io())
                                .observeOn(Schedulers.computation())
                                .subscribe();
                        if (mAudioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION) == AudioManager.VIBRATE_SETTING_ON) {
                            final Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                            v.vibrate(new long[]{500, 1000, 500, 1000}, 0);
                            new Timer().schedule(new TimerTask() {

                                @Override
                                public void run() {
                                    v.cancel();
                                }
                            }, 4500);
                        }
                    }

                    break;
                case ServiceCmd.INIT_ALARM:
                    setAlarm();
                    break;
                case ServiceCmd.DELAY_ALARM:
                    final long alarmId = intent.getLongExtra(RemindService.ID, 0);
                    Single.timer(5, TimeUnit.MINUTES)
                            .doOnSuccess(new Consumer<Long>() {
                                @Override
                                public void accept(Long aLong) throws Exception {
                                    android.util.Log.i("LingJu", "AssistantService accept()");
                                    Intent rIntent = new Intent(AssistantService.this, AlarmRingingActivity.class);
                                    rIntent.putExtra(RemindService.ID, alarmId);
                                    rIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(rIntent);
                                }
                            })
                            .subscribe();
                    break;
                case ServiceCmd.PUSH_ROUTE_CACULATE:
                    String text = intent.getStringExtra(TEXT);
                    Log.i("Lingju", "发送的数据：............." + text);
                    if (!TextUtils.isEmpty(text) && AndroidChatRobotBuilder.get().isRobotCreated()) {
                        if (intent.getBooleanExtra(CALLBACK, false)) {
                            voiceMediator.stopSynthesize();
                            sendMessageToRobot(text, INPUT_VOICE);
                        } else
                            //这种不要回复的对话一般是发送取消命令，结束当前会话场景
                            chat.process(text, null);
                    }
                    break;
                case ServiceCmd.CANCEL_TING_PLAY_TASK:
                    processors.get(BaseProcessor.CMD_TING).cancelTingTask();
                    break;
                case ServiceCmd.SYNC_CALL_SMS:
                    syncCallAndSms();
                    break;
            }
        }
        return super.onStartCommand(intent, START_STICKY, startId);
    }

    private void stopRecognize() {
        voiceMediator.stopRecognize();
        EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECORD_IDLE));
        voiceMediator.tryToWakeup();
    }

    /**
     * 每次启动AssistantService时重置闹钟
     **/
    private void setAlarm() {
        Log.i(TAG, "setAlarm");
        List<Remind> reminds = AssistDao.getInstance().findAllRemind(false);
        if (reminds.size() > 0) {
            ArrayList<Integer> is = new ArrayList<>();
            Calendar cl = Calendar.getInstance();
            SimpleDate sd = new SimpleDate();
            for (Remind r : reminds) {
                if (r.getFrequency() == 0) {
                    cl.setTime(r.getRdate());
                    sd.setValue(new SimpleDate(r.getRtime()).toValue());
                    cl.set(Calendar.HOUR_OF_DAY, sd.getHour());
                    cl.set(Calendar.MINUTE, sd.getMinute());
                    cl.set(Calendar.MILLISECOND, 0);
                    if (System.currentTimeMillis() < cl.getTimeInMillis()) {
                        is.add(r.getId().intValue());
                    }
                } else {
                    is.add(r.getId().intValue());
                }
            }
            if (is.size() > 0) {
                Intent rIntent = new Intent(AssistantService.this, RemindService.class);
                rIntent.putExtra(RemindService.CMD, (RemindService.REMIND << 4) + RemindService.ADD);
                rIntent.putIntegerArrayListExtra(RemindService.IDS, is);
                startService(rIntent);
            }
        }

        List<AlarmClock> alarms = AssistDao.getInstance().findAllAlarm();
        if (alarms.size() > 0) {
            ArrayList<Integer> is = new ArrayList<Integer>();
            Calendar cl = Calendar.getInstance();
            SimpleDate sd = new SimpleDate();
            for (AlarmClock a : alarms) {
                if (a.getValid() == 0)
                    continue;
                if (a.getFrequency() == 0) {
                    cl.setTime(a.getRdate());
                    sd.setValue(a.getRtime());
                    cl.set(Calendar.HOUR_OF_DAY, sd.getHour());
                    cl.set(Calendar.MINUTE, sd.getMinute());
                    cl.set(Calendar.MILLISECOND, 0);
                    if (System.currentTimeMillis() < cl.getTimeInMillis()) {
                        is.add(a.getId().intValue());
                    } else {
                        a.setValid(0);
                        a.setSynced(false);
                        AssistDao.getInstance().updateAlarm(a);
                    }
                } else {
                    is.add(a.getId().intValue());
                }
            }
            if (is.size() > 0) {
                Intent aIntent = new Intent(AssistantService.this, RemindService.class);
                aIntent.putExtra(RemindService.CMD, (RemindService.ALARM << 4) + RemindService.ADD);
                aIntent.putIntegerArrayListExtra(RemindService.IDS, is);
                startService(aIntent);
            }
        }
    }

    private void syncCallAndSms() {
        CallAndSmsDao.ContactsDao contactsDao = CallAndSmsDao.getInstance(this).getSyncDao(CallAndSmsDao.ContactsDao.class);
        CallAndSmsDao.CallLogDao callLogDao = CallAndSmsDao.getInstance(this).getSyncDao(CallAndSmsDao.CallLogDao.class);
        CallAndSmsDao.MessageDao messageDao = CallAndSmsDao.getInstance(this).getSyncDao(CallAndSmsDao.MessageDao.class);
        Observable.fromArray(contactsDao, callLogDao, messageDao)
                .doOnNext(new Consumer<SyncDao<? extends SyncSegment>>() {
                    @Override
                    public void accept(SyncDao<? extends SyncSegment> syncDao) throws Exception {
                        chat.actionTargetAccessor().sync(syncDao);
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    private void onBackground(boolean flag) {
        if (flag /*&& voiceMediator.isWakeUpMode()*/) {
            RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.wake_notification);
            Intent resultIntent = new Intent(AssistantService.this, MainActivity.class);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext(), ServiceCmd.PLAY_IN_BACKGROUND, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
            mBuilder.setContentIntent(resultPendingIntent);
            mBuilder.setAutoCancel(false);
            mBuilder.setSmallIcon(R.drawable.ic_launcher);
            mBuilder.setContent(remoteViews);
            Notification nf = mBuilder.build();
            nf.flags = Notification.FLAG_ONGOING_EVENT;
            startForeground(1, nf);
        } else {
            stopForeground(true);
        }
    }

    private void initProcessor() {
        IProcessor temp;
        temp = new DefaultProcessor(this, voiceMediator);
        processors.put(temp.aimCmd(), temp);
        temp = new AssistProcessor(this, voiceMediator);
        processors.put(temp.aimCmd(), temp);
        temp = new AwakenCtrlProcessor(this, voiceMediator);
        processors.put(temp.aimCmd(), temp);
        temp = new MobileCommProcessor(this, voiceMediator, mHandler);
        processors.put(temp.aimCmd(), temp);
        temp = new MusicOptProcessor(this, voiceMediator);
        processors.put(temp.aimCmd(), temp);
        temp = new MusicPlayProcessor(this, voiceMediator);
        processors.put(temp.aimCmd(), temp);
        temp = new NaviProcessor(this, voiceMediator);
        processors.put(temp.aimCmd(), temp);
        temp = new TingPlayProcessor(this, voiceMediator);
        processors.put(temp.aimCmd(), temp);
    }

    /**
     * 主线程handler
     **/
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    processors.get(BaseProcessor.CMD_CALL).smsMsgHandle();
                    break;
            }
        }
    };

    public void registerReveicer() {
        registerReceiver(netWorkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        IntentFilter inf = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        inf.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(scrennReceiver, inf);
        registerReceiver(headSetInReceiver, new IntentFilter("android.intent.action.HEADSET_PLUG"));
        IntentFilter bit = new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        // bit.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        //bit.addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);
        registerReceiver(blueConnectStateBroadcastReceiver, bit);
        IntentFilter smsFilter = new IntentFilter(SMS_RECEIVED_ACTION);
        smsFilter.setPriority(2147483647);
        smsFilter.addAction(GSM_SMS_RECEIVED_ACTION);
        registerReceiver(smsReceiver, smsFilter);
    }

    /**
     * 注销广播接收器
     */
    public void unRegisterReveicer() {
        unregisterReceiver(netWorkChangeReceiver);
        unregisterReceiver(scrennReceiver);
        unregisterReceiver(headSetInReceiver);
        unregisterReceiver(blueConnectStateBroadcastReceiver);
        unregisterReceiver(smsReceiver);
    }

    /**
     * 监听电话的接听和结束事件
     */
    /*private void phoneCallListener() {
        //电话状态监听
        TelephonyManager telManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telManager.listen(new MobliePhoneStateListener(),
                PhoneStateListener.LISTEN_CALL_STATE);
    }*/
    @Override
    public void onDestroy() {
        Log.i("LingJu", "AssistantService onDestroy()");
        unRegisterReveicer();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * 触发录音权限
     **/
    private void checkAudioRecordPermisssion() {
        Single.just(1).doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable disposable) throws Exception {
                Log.i(TAG, "checkAudioRecordPermisssion>>" + Thread.currentThread());
                int minBufferLength = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferLength);
                try {
                    byte[] buffer = new byte[minBufferLength];
                    audioRecord.startRecording();
                    if (audioRecord.read(buffer, 0, minBufferLength) > 0) {
                        Log.i(TAG, "had Record Permission!!!");
                    }
                    audioRecord.stop();
                    buffer = null;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        audioRecord.stop();
                    }
                    audioRecord.release();
                }
            }
        })
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    /**
     * 机器人初始初始化
     **/
    protected void chatRobotInited() {
        Single.just(0).doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable disposable) throws Exception {
                final long start = System.currentTimeMillis();
                AndroidChatRobotBuilder.create(getApplicationContext(), Constants.LINGJU_APPKEY)
                        .setMusicContext(new AudioAccessAdapter(mAudioPlayer))
                        .setLocationAdapter(new LocationAccessAdapter(mAppConfig.address))
                        .setNetworkAdapter(new NetWorkAccessAdapter(getApplicationContext()))
                        .build(new ChatRobotBuilder.RobotInitListener() {
                            @Override
                            public void initComplete(int i) {
                                if (i == 0) {    //初始化成功
                                    chat = AndroidChatRobotBuilder.get().robot();
                                    if (AppConfig.NewInstallFirstOpen)
                                        mergeServerData();
                                }
                                Log.i("LingJu", "chatRobotInited" + (i == 0 ? "成功" : "失败") + " 耗时>>>" + (System.currentTimeMillis() - start) + "毫秒");
                            }
                        });
            }
        })
                .subscribeOn(Schedulers.newThread())
                .subscribe();

    }

    /**
     * 同步服务器数据
     **/
    private void mergeServerData() {
        syncCallAndSms();
        BaiduNaviDao.getInstance().sync();
        AssistEntityDao.RemindEntityDao remindDao = AssistEntityDao.create().getDao(AssistEntityDao.RemindEntityDao.class);
        chat.actionTargetAccessor().sync(remindDao);
        AssistEntityDao.MemoEntityDao memoDao = AssistEntityDao.create().getDao(AssistEntityDao.MemoEntityDao.class);
        chat.actionTargetAccessor().sync(memoDao);
        AssistEntityDao.AlarmEntityDao alarmDao = AssistEntityDao.create().getDao(AssistEntityDao.AlarmEntityDao.class);
        chat.actionTargetAccessor().sync(alarmDao);
        AssistEntityDao.BillEntityDao billDao = AssistEntityDao.create().getDao(AssistEntityDao.BillEntityDao.class);
        chat.actionTargetAccessor().sync(billDao);
        chat.actionTargetAccessor().sync(TapeEntityDao.getInstance());
    }

    @Override
    public void onInput(String text) {
        sendMessageToRobot(text, INPUT_VOICE);
    }

    @Override
    public void onResult(IChatResult result) {
        handleResult(result, INPUT_VOICE);
    }

    /**
     * 将用户键盘输入/语音录入的文本提交给机器人进行应答
     *
     * @param text      输入文本
     * @param inputType 输入类型 1：语音录入  0：键盘输入
     **/
    private void sendMessageToRobot(String text, int inputType) {
        Log.i("LingJu", "AssistantService sendMessageToRobot()>>>" + text);
        if (AndroidChatRobotBuilder.get().isRobotCreated())
            chat.process(text, new RobotResponseCallBack(inputType));
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void isBlueToothHeadsetConnected() {
        try {
            BluetoothAdapter adapter;
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                adapter = BluetoothAdapter.getDefaultAdapter();
            } else {
                BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                adapter = bm.getAdapter();
            }
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.setBluetoothA2dpOn(true);
            voiceMediator.setBlueHeadSet(adapter.getProfileConnectionState(android.bluetooth.BluetoothProfile.HEADSET) != android.bluetooth.BluetoothProfile.STATE_DISCONNECTED);
            voiceMediator.setSuportA2DP(adapter.getProfileConnectionState(android.bluetooth.BluetoothProfile.A2DP) != android.bluetooth.BluetoothProfile.STATE_DISCONNECTED);
            if (voiceMediator.isBlueToothHeadSet()) {
                adapter.getProfileProxy(this, blueHeadsetListener, BluetoothProfile.A2DP);
                adapter.getProfileProxy(this, blueHeadsetListener, BluetoothProfile.HEADSET);
            }
            Log.e(TAG, "isBlueHeadset=" + Boolean.toString(voiceMediator.isBlueToothHeadSet()));
            Log.e(TAG, "suportA2DP=" + Boolean.toString(voiceMediator.isSuportA2DP()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 蓝牙控制器
     **/
    private IBatchPlayer.BluetoothChannelController bluetoothController = new IBatchPlayer.BluetoothChannelController() {
        @Override
        public void execute(AudioManager audioManager) {
            if (voiceMediator.isBlueToothHeadSet()) {
                if (!voiceMediator.isSuportA2DP()) {
                    if (audioManager.getMode() != AudioManager.MODE_NORMAL) {
                        Log.e(TAG, "playInChannel>>setMode(AudioManager.MODE_NORMAL)");
                        audioManager.setMode(AudioManager.MODE_NORMAL);
                    }
                    if (audioManager.isBluetoothScoOn()) {
                        audioManager.setBluetoothScoOn(false);
                        audioManager.stopBluetoothSco();
                    }
                } else {
                    if (!audioManager.isBluetoothA2dpOn()) {
                        Log.e(TAG, "playInChannel>>setBluetoothA2dpOn(true)");
                        audioManager.setBluetoothA2dpOn(true);
                    }
                }
            }
        }
    };

    /**
     * 导航算路回调
     **/
    private NavigatorService.CalculateRouteListener calculateRouteListener = new NavigatorService.CalculateRouteListener() {

        @Override
        public void onCalculateRoute2Home(boolean showInRouteGuide) {
            if (showInRouteGuide) {
                showRouteGuide();
            } else {
                BaiduAddress ad = BaiduNaviDao.getInstance().getHomeOrCompanyAddress(getResources().getString(R.string.home));
                BaiduAddress nad = BaiduNaviDao.getInstance().get(((AppConfig) getApplication()).address);
                ArrayList<BaiduAddress> list = new ArrayList<>();
                if (ad != null)
                    list.add(ad);
                list.add(nad);
                if (EventBus.getDefault().hasSubscriberForEvent(MapCmdEvent.class)) {
                    EventBus.getDefault().post(new MapCmdEvent(MapCmdEvent.SHOW_TRAFFIC, list));
                } else {
                    Intent intent = new Intent(AssistantService.this, TrafficShowActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putParcelableArrayListExtra("addresses", list);
                    startActivity(intent);
                }
            }
        }

        @Override
        public void onCalculateRoute2Company(boolean showInRouteGuide) {
            if (showInRouteGuide) {
                showRouteGuide();
            } else {
                BaiduAddress ad = BaiduNaviDao.getInstance().getHomeOrCompanyAddress(getResources().getString(R.string.company));
                BaiduAddress nad = BaiduNaviDao.getInstance().get(((AppConfig) getApplication()).address);
                ArrayList<BaiduAddress> list = new ArrayList<>();
                if (ad != null)
                    list.add(ad);
                list.add(nad);
                if (EventBus.getDefault().hasSubscriberForEvent(MapCmdEvent.class)) {
                    EventBus.getDefault().post(new MapCmdEvent(MapCmdEvent.SHOW_TRAFFIC, list));
                } else {
                    Intent intent = new Intent(AssistantService.this, TrafficShowActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putParcelableArrayListExtra("addresses", list);
                    startActivity(intent);
                }
            }
        }

        private void showRouteGuide() {
            if (EventBus.getDefault().hasSubscriberForEvent(NaviRouteCalculateEvent.class)) {
                EventBus.getDefault().post(new NavigateEvent(NavigateEvent.SHOW_NAVI_GUIDE));
            } else {
                Intent intent = new Intent(AssistantService.this, NaviSetLineActivity.class);
                intent.putExtra("voice", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(NaviSetLineActivity.CALCULATE_ROAD, NaviSetLineActivity.CALCULATED_SHOW);
                startActivity(intent);
            }
        }
    };

    /**
     * 短信接收广播action，官方不再对外开放，只能自定义
     **/
    public static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
    public static final String GSM_SMS_RECEIVED_ACTION = "android.provider.Telephony.GSM_SMS_RECEIVED";
    public final static String UNKOWN_NAME = "陌生号码";
    /**
     * 短信接收广播
     **/
    private BroadcastReceiver smsReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            //先判断广播消息
            String action = intent.getAction();
            if (SMS_RECEIVED_ACTION.equals(action) || GSM_SMS_RECEIVED_ACTION.equals(action)) {
                //获取intent参数
                Bundle bundle = intent.getExtras();
                //判断bundle内容
                if (bundle != null) {
                    //取pdus内容,转换为Object[]
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    //解析完内容后分析具体参数
                    String sender = null, content = "", lastContent = "";
                    long date = 0;
                    SmsInfo sms = new SmsInfo();
                    for (int i = 0; i < pdus.length; i++) {
                        SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        content = lastContent.length() >= msg.getMessageBody().length() ? content + msg.getMessageBody() : msg.getMessageBody() + content;
                        lastContent = msg.getMessageBody();
                        sender = msg.getOriginatingAddress();
                        date = msg.getTimestampMillis();
                        if (TextUtils.isEmpty(sender) && sender.startsWith("+86"))
                            sender = sender.substring(3);
                    }
                    sms.setTime(date);
                    sms.setContent(content);
                    StringBuilder number = new StringBuilder(sender);
                    processors.get(BaseProcessor.CMD_CALL).receiveSms(sms, number);
                }
            }
        }
    };

    BluetoothProfile.ServiceListener blueHeadsetListener = new BluetoothProfile.ServiceListener() {

        @Override
        public void onServiceDisconnected(int profile) {
            Log.e("blueHeadsetListener", "onServiceDisconnected:" + profile);
            if (profile == BluetoothProfile.A2DP) {
                voiceMediator.setBluetoothA2dp(null);
            } else if (profile == BluetoothProfile.HEADSET) {
                voiceMediator.setBluetoothHeadset(null);
            }
        }

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.e("blueHeadsetListener", "onServiceConnected:" + profile);
            if (profile == BluetoothProfile.A2DP) {
                voiceMediator.setBluetoothA2dp((BluetoothA2dp) proxy);
            } else if (profile == BluetoothProfile.HEADSET) {
                voiceMediator.setBluetoothHeadset((BluetoothHeadset) proxy);
            }
        }
    };

    BroadcastReceiver blueConnectStateBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "blueConnectStateBroadcastReceiver action>>>>" + intent.getAction());
            if (intent.getAction() == null)
                return;
            if (intent.getAction().equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
                int blueconState = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, 0);
                switch (blueconState) {
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.i(TAG, "blueConnectStateBroadcastReceiver>>>>STATE_CONNECTED");
                        voiceMediator.setBlueHeadSet(true);
                        isBlueToothHeadsetConnected();
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.i(TAG, "blueConnectStateBroadcastReceiver>>>>STATE_CONNECTING");
                        break;
                    case BluetoothAdapter.STATE_DISCONNECTED:
                        Log.i(TAG, "blueConnectStateBroadcastReceiver>>>>STATE_DISCONNECTED");
                        voiceMediator.setBlueHeadSet(false);
                        voiceMediator.setSuportA2DP(false);
                        AudioManager mAudioManager_ = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        mAudioManager_.setBluetoothScoOn(false);
                        mAudioManager_.stopBluetoothSco();
                        break;
                    case BluetoothAdapter.STATE_DISCONNECTING:
                        Log.i(TAG, "blueConnectStateBroadcastReceiver>>>>STATE_DISCONNECTING");
                        voiceMediator.setSuportA2DP(false);
                        break;
                    default:
                        break;
                }
            }
        }
    };


    private BroadcastReceiver headSetInReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "headSetInReceiver>>" + intent.getExtras());
            if (intent.hasExtra("state")) {
                EventBus.getDefault().post(intent.getIntExtra("state", 0) == 1 ?
                        HeadSetEvent.Connect : HeadSetEvent.Disconnect);
            }
        }
    };

    private BroadcastReceiver netWorkChangeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null)
                return;
            Log.i(TAG, "netWorkChangeReceiver>>" + intent.getAction() + "-" + intent.getExtras());
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                /* 实时更新网络状态 */
                NetUtil.NetType last = AppConfig.Network;
                AppConfig.Network = NetUtil.getInstance(getApplication()).getCurrentNetType();
                if (NetUtil.getInstance(getApplication()).getCurrentNetType().equals(NetUtil.NetType.NETWORK_TYPE_NONE)) {
                    // EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(Setting.RECOGNIZE_NOMATCH_ERROR), null, null, null));
                    EventBus.getDefault().post(new NetWorkEvent(NetWorkEvent.NO_NETWORK));
                }
                if (last != AppConfig.Network || AppConfig.Network.isMobileNetwork() && BaiduLocateManager.get() != null) {
                    BaiduLocateManager.get().start();
                }
                if (AndroidChatRobotBuilder.get() != null && !AndroidChatRobotBuilder.get().isRobotCreated()) {
                    chatRobotInited();
                }
                //TODO 设置AI引擎离线工作
                /*if (robotBuilder != null)
                    robotBuilder.updateNetWork(RobotApplication.online);*/
            }
        }
    };

    /**
     * 屏幕状态广播接收者
     **/
    private BroadcastReceiver scrennReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "scrennOffReceiver>>" + intent.getAction());
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Log.i(TAG, "NOTIFICATION_ID");
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                Log.i(TAG, "------ACTION_SCREEN_ON");
            }
        }
    };

    @Override
    public void onMusicFetchError(int errorType) {

    }

    @Override
    public void onMusicPrepareError(int errorCode) {

    }

    @Override
    public void onMusicPlayStart() {

    }

    @Override
    public void onMusicPlayCompleted() {

    }


    public class RobotResponseCallBack implements ResponseCallBack {

        private final int type;

        public RobotResponseCallBack(int type) {
            this.type = type;
        }

        @Override
        public void onResult(IChatResult r) {
            Log.i("LingJu", "RobotResponseCallBack>>>onResult>>>>" + r.getText() + "\n cmd:" + r.cmd().toJsonString());
            handleResult(r, type);
        }
    }

    /**
     * 处理回复信息
     **/
    private void handleResult(IChatResult r, int type) {
        EventBus.getDefault().post(new RecordUpdateEvent(RecordUpdateEvent.RECORD_IDLE));
        String text = r.getText();
        if (r.getStatus() != IChatResult.Status.SUCCESS) {
            text = r.getStatus().toString();
        }
        IflySynthesizer.get().setRdnString(false);
        IflySynthesizer.get().clearInterruptedMessages();
        //关闭请求服务器提示音
        voiceMediator.setRobotResponse(true);
        voiceMediator.stopWaitPlay();
        // touchScreen();
        voiceMediator.setCurrentAction(new CmdAction(null, 0, r.cmd().getOutc()));
        // EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(text), null, null, null));
        String actions = r.cmd().getActions();
        if (TextUtils.isEmpty(actions)) {   //没有语义对象动作，闲聊
            processors.get(BaseProcessor.CMD_DEFAULT).handle(r.cmd(), text, type);
        } else {
            try {
                JSONArray jsonArray = new JSONArray(actions);
                if (jsonArray.length() == 0) {   //闲聊
                    processors.get(BaseProcessor.CMD_DEFAULT).handle(r.cmd(), text, type);
                    return;
                }
                JSONObject actionJson = jsonArray.getJSONObject(jsonArray.length() - 1);
                if (actionJson.isNull("target")) {   //动作对象中没有动作目标，闲聊
                    processors.get(BaseProcessor.CMD_DEFAULT).handle(r.cmd(), text, type);
                } else {
                    // if (!actionJson.isNull("hints"))
                    //     text = actionJson.getJSONObject("hints").getString("success");
                    JSONObject target = actionJson.getJSONObject("target");
                    voiceMediator.setCurrentAction(new CmdAction(actionJson.getString("action"), target.getInt("id"), r.cmd().getOutc()));
                    switch (target.getInt("id")) {
                        case RobotConstant.ACTION_MEMO:     //提醒、备忘、闹钟、记账模块
                        case RobotConstant.ACTION_ALARM:
                        case RobotConstant.ACTION_REMIND:
                        case RobotConstant.ACTION_ACCOUNTING:
                        case RobotConstant.ACTION_TAPE:     //录音控制
                        case RobotConstant.ACTION_DIALOG:   //显示对话框
                            processors.get(BaseProcessor.CMD_ADDITION).handle(r.cmd(), text, type);
                            break;
                        case RobotConstant.ACTION_ALBUM:
                            //有声内容播放模块
                            processors.get(BaseProcessor.CMD_TING).handle(r.cmd(), text, type);
                            break;
                        case RobotConstant.ACTION_PLAYER:
                            if (!target.isNull("volume") || "AUDIO".equals(target.getString("type"))) {
                                if (!target.isNull("origin")
                                        && ("XIMALAYA".equals(target.getString("origin"))
                                        || "KAOLA".equals(target.getString("origin")))) {
                                    //有声内容播放模块
                                    processors.get(BaseProcessor.CMD_TING).handle(r.cmd(), text, type);
                                } else {
                                    //灵聚音乐播放及音频控制模块
                                    processors.get(BaseProcessor.CMD_PLAY).handle(r.cmd(), text, type);
                                }
                            } else {
                                Log.i("LingJu", "视频播放模块");
                                text = "抱歉，我还没有接入视频资源库，暂时不支持视频功能。";
                                processors.get(BaseProcessor.CMD_DEFAULT).handle(r.cmd(), text, type);
                            }
                            break;
                        case RobotConstant.ACTION_FAVOR:    //音频文件处理模块
                        case RobotConstant.ACTION_VOICE_ENGINE:
                            processors.get(BaseProcessor.CMD_OPTIONS).handle(r.cmd(), text, type);
                            break;
                        case RobotConstant.ACTION_AWAKEN:   //唤醒模块
                            processors.get(BaseProcessor.CMD_AWAKEN).handle(r.cmd(), text, type);
                            break;
                        case RobotConstant.ACTION_CONTACT:      //电话短信模块
                        case RobotConstant.ACTION_CALL_LOG:
                        case RobotConstant.ACTION_CALL:
                        case RobotConstant.ACTION_SMS:
                        case RobotConstant.ACTION_SMS_LOG:
                        case RobotConstant.ACTION_PHONE_NUM:
                            processors.get(BaseProcessor.CMD_CALL).handle(r.cmd(), text, type);
                            break;
                        case RobotConstant.ACTION_NAVIGATION:       //导航模块
                        case RobotConstant.ACTION_ROUTE:
                        case RobotConstant.ACTION_ADDRESS:
                        case RobotConstant.ACTION_ROUTENODE:
                        case RobotConstant.ACTION_PLAT:
                            processors.get(BaseProcessor.CMD_NAVI).handle(r.cmd(), text, type);
                            break;
                        default:    //未实现场景
                            Log.i("LingJu", "动作对象ID：" + target.getInt("id"));
                            text = "抱歉，我暂时还不支持该功能。";
                            processors.get(BaseProcessor.CMD_DEFAULT).handle(r.cmd(), text, type);
                            break;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                // TODO: 2017/5/10 回复解析出错，给用户友好的提示
            }
        }
    }

    /**
     * 点亮屏幕
     **/
    private void touchScreen() {
        if (mPowerManager == null) {
            //获取电源管理器对象
            mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        }
        //获取PowerManager.WakeLock对象,后面的参数|表示同时传入两个值,最后的是LogCat里用的Tag
        //ON_AFTER_RELEASE ：重置暗屏时间，在释放后仍会保持亮度一段时间
        //PowerManager.SCREEN_BRIGHT_WAKE_LOCK： 高亮屏幕
        PowerManager.WakeLock wl = mPowerManager.newWakeLock(PowerManager.ON_AFTER_RELEASE | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
        //点亮屏幕
        wl.acquire();
        //释放
        wl.release();
    }

    public interface ServiceCmd {
        /**
         * 语音设置部分
         **/
        int START_RECOGNIZE = 1;
        int STOP_RECOGNIZE = 2;
        int STOP_WAKEUP_LISTEN = 3;     //仅关闭唤醒，不记录
        int START_WAKEUP_MODE = 4;      //开启唤醒模式，并记录
        int CLOSE_WAKEUP_MODE = 5;      //关闭唤醒模式，并记录
        int STOP_VOICE_MODE = 16;       //关闭语音模块（关闭识别、合成、唤醒）
        int HEADSET_RECOGNIZE = 18;
        int GET_HOOK = 19;

        /**
         * 音乐播放部分
         **/
        int PAUSE_PLAY = 6;       //录音时停止播放
        int PLAY_IN_BACKGROUND = 7;
        int TOGGLE_PLAY = 8;    //暂停、播放
        int NEXT_MUSIC = 9;     //下一首
        int CLOSE_PLAY_NOTIFICATION = 10;   //关闭通知栏
        int SEND_TO_ROBOT_FOR_END_TASK = 11;
        int SEND_TO_ROBOT = 12;     //发送文本给robot

        int CANCEL_TING_PLAY_TASK = 20;     //取消自动播放有声内容任务

        /**
         * 闹钟、提醒、备忘、记账部分
         **/
        int SPEAK_REMIND_TEXT = 13;     //合成提醒文本
        int INIT_ALARM = 14;        //初始化闹钟、提醒
        int DELAY_ALARM = 17;       //5分钟后再响铃

        int SYNC_CALL_SMS = 21;     //同步联系人、通话记录等数据
        /**
         * 导航部分
         **/
        int PUSH_ROUTE_CACULATE = 15;   //发送与导航相关或不需回复的信息给机器人
    }

}
