package com.lingju.assistant.baidunavi.adapter;

import android.content.Context;

import com.baidu.navisdk.adapter.BNOuterTTSPlayerCallback;
import com.lingju.assistant.AppConfig;
import com.lingju.assistant.activity.event.CallTaskEvent;
import com.lingju.audio.engine.IflyRecognizer;
import com.lingju.audio.engine.base.SpeechMsg;
import com.lingju.audio.engine.base.SpeechMsgBuilder;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.lbsmodule.proxy.SoundUtilsProxy;
import com.lingju.common.log.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;


/**
 * Created by Administrator on 2015/9/17.
 */
public class BaiduTTSPlayerCallBack implements BNOuterTTSPlayerCallback {

    private final static String TAG = "BaiduTTSPlayerCallBack";
    public final static String NAVI_START_TIPS_COUNT = "nav_start_tips_count";
    private static SoundUtilsProxy mDingSound = null;
    private static SoundUtilsProxy mHighwayDididiSound = null;
    private static SoundUtilsProxy mCruiserPassSound = null;
    public static String NAVI_START_TIPS[];
    public static String NAVI_STOP_REG_TIPS = "抱歉，为了播报导航提示，打断了你的讲话，遇到这种情况您可以唤醒我后再说一遍";
    public final static String NAVI_KEYWORDS[] = new String[]{
            "向右", "向左", "右转", "左转", "转弯", "掉头", "环岛",
            "靠左", "靠右", "靠最左", "靠最右", "沿中间",
            "进入", "到达",
            "左侧", "右侧", "中间车", "最右侧", "最左侧"
    };
    //public final static String NAVI_KEYWORDS[]=new String[]{"左", "右","转弯","掉头","环岛","沿中间","中间车", "进入","到达"};
    private int stopRegTipsCout;

    public static String NaviText;
    static boolean haveTTS;
    private Context context;

    private AtomicBoolean inCallTask = new AtomicBoolean(false);
    private boolean inCall;

    private Random rd = new Random();

    public BaiduTTSPlayerCallBack(Context context) {
        if (context == null)
            throw new NullPointerException("context is null");
        this.context = context;
        /*if(mDingSound==null){
            mDingSound = new SoundUtilsProxy(context, 1711603714);
            mHighwayDididiSound = new SoundUtilsProxy(context, 1711603713);
            mCruiserPassSound = new SoundUtilsProxy(context, 1711603712);
        }*/
        if (NAVI_START_TIPS == null) {
            NAVI_START_TIPS = /*new String[]{"导航开始提示"};*/context.getResources().getStringArray(com.lingju.assistant.R.array.navi_start_tips);
        }
        EventBus.getDefault().register(this);
    }

    public void destroy() {
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onCallEvent(CallTaskEvent e) {
        if (e.getState() == CallTaskEvent.STATE_START) {
            inCallTask.set(true);
        } else if (e.getState() == CallTaskEvent.STATE_END) {
            inCallTask.set(false);
        }
    }

    public boolean isInCallTask() {
        return inCallTask.get();
    }

    /*

    public void testSound(){
        mCruiserPassSound.play();
    }
*/

    @Override
    public int getTTSState() {
        //Log.i(TAG, "getTTSState>>");
        int state = BNOuterTTSPlayerCallback.PLAYER_STATE_NOT_INIT;
        if (haveTTS && SynthesizerBase.isInited()) {
            state = SynthesizerBase.get().isSpeaking() ?
                    (SynthesizerBase.get().getCurrentOrigin() == SpeechMsg.ORIGIN_NAVI ?
                            BNOuterTTSPlayerCallback.PLAYER_STATE_PLAYING : BNOuterTTSPlayerCallback.PLAYER_STATE_IDLE) :
                    BNOuterTTSPlayerCallback.PLAYER_STATE_IDLE;
        }
        Log.i(TAG, "getTTSState>>" + state);
        return state;
    }

    private boolean containKeyword(String text) {
        for (String key : NAVI_KEYWORDS) {
            if (text.indexOf(key) != -1)
                return true;
        }
        return false;
    }

    @Override
    public int playTTSText(String text, int state) {
        Log.i("LingJu", "playTTSText:" + text + ",state=" + state);
        if (/*!BNavigatorProxy.getInstance().isNaviBegin() ||*/ inCallTask.get() || inCall)
            return BNOuterTTSPlayerCallback.PLAYER_STATE_PLAYING;
        text = text.replaceAll("\\<usraud\\>", "").replaceAll("\\<\\/usraud\\>", "");
        if (text.contains("导航开始") && NAVI_START_TIPS != null && NAVI_START_TIPS.length > 0) {
            int tipsCount = AppConfig.getSettingInt(NAVI_START_TIPS_COUNT, 0);
            if (tipsCount < 5) {
                if (tipsCount % 2 == 0) {
                    text = NAVI_START_TIPS[rd.nextInt(NAVI_START_TIPS.length)];
                }
                AppConfig.setSettingInt(NAVI_START_TIPS_COUNT, ++tipsCount);
            }
            stopRegTipsCout = 2;
            Log.i(TAG, "text>>" + text);
            SpeechMsgBuilder builder = SpeechMsgBuilder.create(text)
                    //                    .setContextMode(SpeechMsg.CONTEXT_KEEP_AWAKEN)
                    .setPriority(SpeechMsg.PRIORITY_ABOVE_RECOGNIZE)
                    .setOrigin(SpeechMsg.ORIGIN_NAVI)
                    .setForceLocalEngine(true);

            Observable<SpeechMsg> msgObservable = SynthesizerBase.get().addMessageWaitSpeak(builder.build());
            if (msgObservable != null) {
                msgObservable.subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation())
                        .subscribe();
            }
            return BNOuterTTSPlayerCallback.PLAYER_STATE_PLAYING;
        }
        if (text.indexOf("导航结束") != -1) {
            return BNOuterTTSPlayerCallback.PLAYER_STATE_IDLE;
        }
        //Log.i(TAG, "playTTSText:" + text);

        if (text.startsWith("叮")) {
            Log.e("TTS", "TTSPlayerControl.playTTSText() play 叮.");
           /* if(mDingSound != null) {
                mDingSound.play();
            }*/

            return 1;
        }

        if (text.startsWith("嗒嗒嗒")) {
            Log.e("TTS", "TTSPlayerControl.playTTSText() play 嗒嗒嗒.");
           /* if(mCruiserPassSound != null) {
                mCruiserPassSound.play();
            }*/

            return 1;
        }

        if (text.startsWith("嘀嘀嘀")) {
            Log.e("TTS", "TTSPlayerControl.playTTSText() play 嘀嘀嘀.");
            /*if(mHighwayDididiSound != null) {
                mHighwayDididiSound.play();
            }*/

            text = text.substring("嘀嘀嘀".length());
        }


        NaviText = text;

        state = state == 0 ? containKeyword(text) ? 1 : 0 : state;

        if (IflyRecognizer.getInstance().isListening() && stopRegTipsCout > 0) {
            text = text + "。" + NAVI_STOP_REG_TIPS;
            stopRegTipsCout--;
        }

        SynthesizerBase.get().requestAudioFocus();
        if (state == 1) {      //空闲状态直接播报
            SpeechMsgBuilder builder = SpeechMsgBuilder.create(text)
                    .setPriority(SpeechMsg.PRIORITY_ABOVE_RECOGNIZE)
                    .setOrigin(SpeechMsg.ORIGIN_NAVI)
                    .setForceLocalEngine(true);
            SynthesizerBase.get().startSpeakAbsolute(builder.build())
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .subscribe();
        } else if (SynthesizerBase.isInited()) {      //其余状态添加播报队列等待播报
            SpeechMsgBuilder builder = SpeechMsgBuilder.create(text)
                    .setPriority(SpeechMsg.PRIORITY_BELOW_RECOGNIZE)
                    .setOrigin(SpeechMsg.ORIGIN_NAVI)
                    .setForceLocalEngine(true);
            Observable<SpeechMsg> msgObservable = SynthesizerBase.get().addMessageWaitSpeak(builder.build());
            if (msgObservable != null) {
                msgObservable.subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation())
                        .subscribe();
            }
            /*SynthesizerBase.get().startSpeakAbsolute(builder.build())
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .subscribe();*/

        } else {
            return BNOuterTTSPlayerCallback.PLAYER_STATE_IDLE;
        }
        return BNOuterTTSPlayerCallback.PLAYER_STATE_PLAYING;
    }

    @Override
    public void phoneCalling() {
        Log.i(TAG, "phoneCalling");
        inCall = true;
    }

    @Override
    public void phoneHangUp() {
        Log.i(TAG, "phoneHangUp");
        inCall = false;
    }

    @Override
    public void initTTSPlayer() {
        Log.i(TAG, "initTTSPlayer");
    }

    @Override
    public void releaseTTSPlayer() {
        Log.i(TAG, "releaseTTSPlayer");

    }

    @Override
    public void stopTTS() {
        Log.i(TAG, "stopTTS");
        SynthesizerBase.get().stopSpeakingAbsolte();
    }

    @Override
    public void resumeTTS() {
        Log.i(TAG, "resumeTTS");
        SynthesizerBase.get().resumeSpeaking();
    }

    @Override
    public void pauseTTS() {
        Log.i(TAG, "pauseTTS");
        SynthesizerBase.get().pauseSpeaking();
    }
}
