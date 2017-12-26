package com.lingju.audio.engine;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.lingju.audio.PcmPlayer;
import com.lingju.audio.SystemVoiceMediator;
import com.lingju.audio.config.IflySynConfig;
import com.lingju.audio.config.VoiceConfig;
import com.lingju.audio.engine.base.SpeechMsg;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.util.NetUtil.NetType;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

public class IflySynthesizer extends SynthesizerBase {
    private static String TAG = "IflySynthesizer";
    private SpeechSynthesizer synthesizer;
    private static IflySynthesizer instance_;
    private static String OFFLINE_ENGINE_SPPED = "45";
    private AudioManager audioManager;
    private MySynthesizerListener currentListener;
    private boolean initParam = false;
    private final VoiceConfig mVoiceConfig;

    private IflySynthesizer(Context context, SystemVoiceMediator mediator) {
        super.mContext = context;
        super.mediator = mediator;
        synthesizer = SpeechSynthesizer.createSynthesizer(context, sinitListener);
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mVoiceConfig = VoiceConfig.create(mContext);
        super.pcmPlayer = new PcmPlayer(context, mediator.createHandler());
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (initParam)
                    return;
                resetParam();
                SynthesizerBase.instance = IflySynthesizer.instance_;
                SynthesizerBase.inited = true;
            }
        }, 3000);
    }

    public static boolean isInited() {
        if (instance_ == null)
            return false;
        return true;
    }

    public static IflySynthesizer getInstance() {
        if (instance_ == null)
            try {
                throw new Exception("初始化出错");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        return instance_;
    }

    public static IflySynthesizer createInstance(Context context, SystemVoiceMediator mediator) {
        if (instance_ == null) {
            instance_ = new IflySynthesizer(context, mediator);
        }
        return instance_;
    }

    /**
     * 判断当前合成引擎是否处于朗读状态
     *
     * @return true=是
     */
    public boolean isSpeaking() {
        boolean result = false;
        try {
            result = synthesizer.isSpeaking() || pcmPlayer.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, "isSpeaking>>" + Boolean.toString(result));
        return result;
    }

    protected Observable<SpeechMsg> synthesize(final SpeechMsg msg) {
        /*if(msg.getText().indexOf("已为")>0){
            synthesizer.setParameter(SpeechConstant.PARAMS,"ttp=cssml");
			int i=msg.getText().indexOf("已为");
			msg.setText("<?xml version=\"1.0\" encoding=\"GB2312\"?>\n" +
					"<speak xml:lang=\"cn\">\n" +
					"<sentence>\n" +
					msg.getText().substring(0,i+1)+
					"<phoneme py=\"wei4\">为</phoneme>" +
					msg.getText().substring(i+2)+
					"\n</sentence>\n" +
					"</speak>");
			System.out.println("cssml="+msg.getText());
		}*/
        //暂停音乐播放
        /*if (mediator.isPlaying())
            mediator.pausePlay();*/
        mediator.stopWaitPlay();
        if (msg.sections() != null && isLocalEngine()) {
            requestFocusByLocal();
            return synthesize_(msg);//synthesize(msg.text(),msg.sections());
        } else {
            requestAudioFocus();
            //if(!mVoiceConfig.getVolName().equals(synthesizer.getParameter(SpeechConstant.VOICE_NAME))){
            synthesizer.setParameter(SpeechConstant.SPEED, Integer.toString(mVoiceConfig.getVolSpeed()));
            synthesizer.setParameter(SpeechConstant.VOLUME, Integer.toString(mVoiceConfig.getVolume()));
            synthesizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            synthesizer.setParameter(SpeechConstant.VOICE_NAME, mVoiceConfig.getVolName());
            synthesizer.setParameter(SpeechConstant.PITCH, "50");
            //}
            return Observable.create(new ObservableOnSubscribe<SpeechMsg>() {

                @Override
                public void subscribe(final ObservableEmitter<SpeechMsg> e) throws Exception {
                    e.onNext(msg.setState(SpeechMsg.State.Idle));
                    Log.i(TAG, "startSpeeaking");
                    synthesizer.startSpeaking(msg.text(), (currentListener = new MySynthesizerListener(e, msg)));
                }
            })
                    .doOnNext(defaultErrorConsumer)
                    .retry(new Predicate<Throwable>() {
                        @Override
                        public boolean test(Throwable throwable) throws Exception {
                            return throwable instanceof FailedException;
                        }
                    })
                    /*.filter(new Predicate<SpeechMsg>() {
                        @Override
						public boolean test(SpeechMsg speechMsg) throws Exception {
							return speechMsg.state().ordinal()>= SpeechMsg.State.OnInterrupted.ordinal();
						}
					})*/;
        }
    }

    private void checkValid(ObservableEmitter<SpeechMsg> e, SpeechMsg msg, SpeechMsg.State nextState) {
        if (msg.invalid())
            return;
        e.onNext(msg.setState(nextState));
    }

    private Consumer<SpeechMsg> defaultErrorConsumer = new Consumer<SpeechMsg>() {
        @Override
        public void accept(SpeechMsg speechMsg) throws Exception {
            //if(speechMsg.state()!= SpeechMsg.State.Speaking)
            //	Log.i(TAG,"defaultErrorConsumer>>"+speechMsg.state()+"--"+Thread.currentThread());
            if (speechMsg.invalid())
                return;
            if (SpeechMsg.State.Error == speechMsg.state() && speechMsg.retryTimes() > 0) {
                speechMsg.decreaseRetryTimes();
                speechMsg.increaseError();
                throw new FailedException("errorCount:" + speechMsg.errorCount() + " and retryTimes:" + speechMsg.retryTimes());
            }
        }
    };


    protected Observable<SpeechMsg> synthesize_(final SpeechMsg msg) {
        return Observable.create(new ObservableOnSubscribe<SpeechMsg>() {

            @Override
            public void subscribe(ObservableEmitter<SpeechMsg> e) throws Exception {
                Log.i(TAG, "synthesize_.subscribe>>" + Thread.currentThread().getName());
                List<IflyMsg> list = new ArrayList<>();
                try {
                    JSONArray ja = msg.sections();
                    int i1 = 0, i2;
                    IflySynConfig config;
                    JSONObject temp;
                    long time = System.currentTimeMillis();
                    for (int i = 0; i < ja.length(); i++) {
                        i2 = (temp = ja.getJSONObject(i)).getInt("index");
                        config = IflySynConfig.get(temp.getString("role"), temp.getString("mood"));
                        list.add(new IflyMsg(msg.text().substring(i1, i2), config, (time + i) + ".pcm"));
                        i1 = i2;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                e.onNext(msg.setState(SpeechMsg.State.Idle));
                if (list.size() > 0) {
                    IflyMsg m = list.get(0);
                    synthesizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
                    m.config.applyTo(synthesizer);
                    currentListener = new SectionSynthesizeListener(list, e, msg);
                    //合成文本到一个音频文件，不播放。
                    synthesizer.synthesizeToUri(m.text, pcmPlayer.getCacheDir().getAbsolutePath() + "/" + m.getPath(), currentListener);
                    pcmPlayer.playSync((SectionSynthesizeListener) currentListener);
                }
            }
        })
                .doOnNext(defaultErrorConsumer)
                .retry(new Predicate<Throwable>() {
                    @Override
                    public boolean test(Throwable throwable) throws Exception {
                        return throwable instanceof FailedException;
                    }
                })
                .subscribeOn(Schedulers.io());
                /*.filter(new Predicate<SpeechMsg>() {
                    @Override
					public boolean test(SpeechMsg speechMsg) throws Exception {
						return speechMsg.state().ordinal()>= SpeechMsg.State.OnInterrupted.ordinal();
					}
				});*/
    }

    public void destory() {
        if (isSpeaking())
            stopSpeakingAbsolte();
        synthesizer.destroy();
        instance_ = null;
    }


    public void pauseSpeaking() {
        Log.e(TAG, "pauseSpeaking");
        synthesizer.pauseSpeaking();
    }

    public void resumeSpeaking() {
        synthesizer.resumeSpeaking();
    }

    protected void stop() {
        try {
            if (isSpeaking()) {
                //mContext.stopInMsgTips();
                synthesizer.stopSpeaking();
                currentListener.onInterrupted();
                abandonAudioFocus();
            }
            pcmPlayer.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public SpeechSynthesizer getSpeechEngine() {
        return synthesizer;
    }

    public void resetParam() {
        //setDefaultSynthesizerParam(RobotApplication.getNetType());
    }

    public void resetParam(NetType type) {
        setDefaultSynthesizerParam(type);
    }

    /**
     * 设置数字播报方式
     * 0、1：按值逐字播报
     * 2、3：按串播报
     **/
    public void setRdnString(boolean flag) {
        if (flag)
            synthesizer.setParameter("rdn", "2");
        else
            synthesizer.setParameter("rdn", "0");
    }

    @Override
    public void requestAudioFocus() {
        if (synthesizer != null) {
            synthesizer.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "1");
        }
    }

    private void requestFocusByLocal() {
        audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
    }

    private void abandonFocusByLocal() {
        audioManager.abandonAudioFocus(audioFocusChangeListener);
    }

    @Override
    public void abandonAudioFocus() {
        if (synthesizer != null) {
            synthesizer.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "0");
        }
        abandonFocusByLocal();
    }

    AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        private int volume = 0;

        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.e(TAG, "audioFocusChangeListener.onAudioFocusChange>>>>>>>>>>>>>>>>>>" + focusChange);
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    // pauseSpeaking();
                    break;
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                    /*if (isSpeaking()) {
                        synthesizer.resumeSpeaking();
                    }*/
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 设置默认的合成引擎
     *
     * @param type
     */
    private void setDefaultSynthesizerParam(NetType type) {
        if (isSpeaking()) {
            Log.e(TAG, "播放当中切换播放引擎失败");
            return;
        }
        synthesizer.setParameter(SpeechConstant.SPEED, Integer.toString(mVoiceConfig.getVolSpeed()));
        synthesizer.setParameter(SpeechConstant.VOLUME, Integer.toString(mVoiceConfig.getVolume()));
        synthesizer.setParameter(SpeechConstant.VOICE_NAME, mVoiceConfig.getVolName());
        synthesizer.setParameter("rdn", "0");
        synthesizer.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "0");
        if (mediator.isBlueToothHeadSet()) {
            synthesizer.setParameter(SpeechConstant.STREAM_TYPE, Integer.toString(AudioManager.STREAM_VOICE_CALL));
        } else {
            synthesizer.setParameter(SpeechConstant.STREAM_TYPE, Integer.toString(AudioManager.STREAM_MUSIC));
            AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            if (am.getMode() != AudioManager.MODE_NORMAL) {
                am.setMode(AudioManager.MODE_NORMAL);
            }
        }
        //synthesizer.setParameter(SpeechConstant.ENGINE_TYPE, VoiceConfig.getSynEngineType());
        /*if(type==NetType.NETWORK_TYPE_NONE||type==NetType.NETWORK_TYPE_2G){//网络关闭的状态下启动离线识别
            Log.e(TAG, "离线。。。。。。。。。。。。。。。。。。。");
			synthesizer.setParameter(SpeechConstant.SPEED, OFFLINE_ENGINE_SPPED);
			synthesizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
			synthesizer.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
			isLocalEngine=true;
		}
		else{*/
        Log.e(TAG, "在线。。。。。。。。。。。。。。。。。。。");
        //synthesizer.setParameter(SpeechConstant.SPEED,"70");
        synthesizer.setParameter(SpeechConstant.VOICE_NAME, mVoiceConfig.getVolName());
        //synthesizer.setParameter(SpeechConstant.VOICE_NAME, ChatRobot.isInit()&&ChatRobot.getInstance().getMode()==ChatConstant.CHILDREN_MODE?"vinn":"vixq");
        synthesizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        //}
    }

    /**
     * 合成器初始化监听器
     */
    private InitListener sinitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            // TODO Auto-generated method stub
            Log.d(TAG, "SpeechSynthesizer init() code = " + code);
            if (code == ErrorCode.SUCCESS) {
                initParam = true;
                Log.i(TAG + ".sinitListener", "SpeechSynthesizer init success!");
                resetParam();
                SynthesizerBase.instance = instance_;
                SynthesizerBase.inited = true;
            }
            if (mediator != null) {
                mediator.onSynthesizerInited(code);
            }
        }
    };


    public void onError(int code, String description, int errorCount) {
        Log.e(TAG, "onCompleted 合成出错：" + description + ",errorCode=" + code);
        if (errorCount <= 2) {
            if (description.contains("本地引擎错误")) {
                startSpeakAbsolute(currentMessage);
            } else if (description.contains("网络")) {
                if (!isLocalEngine())
                    resetParam(NetType.NETWORK_TYPE_2G);
                SpeechMsg sp = currentMessage;
                // startSpeakAbsolute("您目前网络不佳，小灵暂时启用离线方式，和您说话。");
                addMessageWaitSpeak(sp);
            } else {
                SpeechMsg sp = currentMessage;
                startSpeakAbsolute("语音合成出错：" + (TextUtils.isEmpty(description) ? code : description) + "。再说一次");
                addMessageWaitSpeak(sp);
            }
        } else {
            description = "语音合成出错,出错原因：" + (TextUtils.isEmpty(description) ? "未知" : description) + ",错误码:" + code + ",请报告技术人员。";
            startSpeakAbsolute(description);
        }
        mediator.onSynthesizerError(description);
    }


    class MySynthesizerListener implements SynthesizerListener {
        final ObservableEmitter<SpeechMsg> emitter;
        final SpeechMsg msg;

        MySynthesizerListener(ObservableEmitter<SpeechMsg> e, SpeechMsg msg) {
            this.emitter = e;
            this.msg = msg;
        }

        @Override
        public void onBufferProgress(int arg0, int arg1, int arg2, String arg3) {
            // TODO Auto-generated method stub
            //Log.i(TAG+".synthesizerListener", "onBufferProgress:"+arg0+"%");
            //context.onSpeakProgress(speechID, arg0);
            checkValid(emitter, msg, SpeechMsg.State.Buffering);
        }


        @Override
        public void onCompleted(SpeechError e) {
            // TODO Auto-generated method stub
            Log.i(TAG + ".synthesizerListener", "onCompleted,code=" + e);
            if (e == null)
                emitter.onNext(msg.setState(SpeechMsg.State.Completed));
            else {
                emitter.onNext(msg.setState(SpeechMsg.State.Error).increaseError());
                onError(e.getErrorCode(), e.getErrorDescription(), msg.errorCount());
            }
            emitter.onComplete();
            keepVoiceCtrl(msg);
            //VoiceService.get().getRecognizer().setLastRecognized(System.currentTimeMillis());
            /*if(e!=null){
                IflySynthesizer.this.onError(e.getErrorCode(), e.getErrorDescription(), errorCount);
				return;
			}*/
            //IflySynthesizer.this.onCompleted(progress<100, current_id);
        }

        @Override
        public void onSpeakBegin() {
            // TODO Auto-generated method stub
            Log.i(TAG + ".synthesizerListener", "onSpeakBegin");
            checkValid(emitter, msg, SpeechMsg.State.OnBegin);
            /*if(currentMessage!=null)
            mediator.onSynthersizeSpeakBegin();*/
        }

        @Override
        public void onSpeakPaused() {
            // TODO Auto-generated method stub
            Log.i(TAG + ".synthesizerListener", "onSpeakPaused");
            checkValid(emitter, msg, SpeechMsg.State.OnPaused);
			/*mediator.onSynthersizeSpeakPaused();*/
            //mContext.allowSynthersize(currentMessage!=null?currentMessage.getPriority():PRIORITY_ABOVE_RECOGNIZE);
        }

        @Override
        public void onSpeakProgress(int p, int arg1, int arg2) {
            // TODO Auto-generated method stub
            checkValid(emitter, msg, SpeechMsg.State.Speaking);
			/*mediator.onSpeakProgress(p, arg1, arg2);
			progress=p;*/
            //Log.i(TAG+".synthesizerListener", "onSpeakProgress"+p+"%");
        }

        @Override
        public void onSpeakResumed() {
            Log.i(TAG + ".synthesizerListener", "onSpeakResumed");
            checkValid(emitter, msg, SpeechMsg.State.OnResume);
        }


        @Override
        public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {
            // TODO Auto-generated method stub
        }

        public void onInterrupted() {
            Log.i(TAG + ".synthesizerListener", "onInterrupted " + msg.invalid());
            emitter.onNext(msg.setState(SpeechMsg.State.OnInterrupted));
            if (msg.invalid())
                return;
            emitter.onComplete();
            keepVoiceCtrl(msg);
        }

    }

    ;

    class SectionSynthesizeListener extends MySynthesizerListener implements PcmPlayer.PlayListener {

        final List<IflyMsg> msgs;
        int p;

        public SectionSynthesizeListener(List<IflyMsg> msgs, ObservableEmitter<SpeechMsg> e, SpeechMsg msg) {
            super(e, msg);
            this.msgs = msgs;
        }

        @Override
        public void onCompleted(SpeechError speechError) {
            Log.i(TAG, "onCompleted...................");
            if (speechError != null) {
                onError(speechError.getMessage());
                return;
            }
            if (msgs.size() - p > 0 && pcmPlayer.playAllowed()) {
                IflyMsg m = msgs.get(p);
                m.setProgress(100);
                pcmPlayer.append(m, msgs.size() - p == 1);
                pcmPlayer.notifyPlay();
                if (msgs.size() - p > 1) {
                    m = msgs.get(++p);
                    m.config.applyTo(synthesizer);
                    synthesizer.synthesizeToUri(m.text, pcmPlayer.getCacheDir().getAbsolutePath() + "/" + m.getPath(), SectionSynthesizeListener.this);
                }
            }
        }

        @Override
        public void onError(String msg) {
            Log.i(TAG, "onError..................." + msg);
            emitter.onNext(SectionSynthesizeListener.this.msg.setState(SpeechMsg.State.Error).increaseError());
            emitter.onComplete();
            keepVoiceCtrl(SectionSynthesizeListener.this.msg);
        }

        @Override
        public void onCompleted() {
            Log.i(TAG, "pcmPlayer.....onCompleted...................");
            emitter.onNext(SectionSynthesizeListener.this.msg.setState(SpeechMsg.State.Error).increaseError());
            emitter.onComplete();
            keepVoiceCtrl(msg);
        }

        @Override
        public void onSpeakProgress(int p) {
            super.onSpeakProgress(p, 0, 0);
        }
    }

    ;


    public void reSpeak() {
        SpeechMsg sp = currentMessage;
        startSpeakAbsolute("您目前网络不佳，小灵暂时启用离线方式，和您说话。");
        addMessageWaitSpeak(sp);
        messages.clear();
        try {
            synthesizer.stopSpeaking();
        } catch (Exception e) {
            e.printStackTrace();
        }
        resetParam(NetType.NETWORK_TYPE_NONE);
        //synthesizer.startSpeaking(currentMessage.getText(), synthesizerListener);
    }

    @Override
    public boolean isForceLocalEngine() {
        // TODO Auto-generated method stub
        return false;//forceLocalEngine;
    }

    @Override
    public void setForceLocalEngine(boolean flag) {
        Log.i(TAG, "setForceLocalEngine>>" + Boolean.toString(flag));
        //forceLocalEngine=true;
        synthesizer.setParameter(SpeechConstant.ENGINE_TYPE, flag ? SpeechConstant.TYPE_CLOUD : SpeechConstant.TYPE_LOCAL);
    }

    @Override
    public boolean isLocalEngine() {
        // TODO Auto-generated method stub
        return synthesizer == null || SpeechConstant.TYPE_LOCAL.equals(synthesizer.getParameter(SpeechConstant.ENGINE_TYPE));
    }

    @Override
    protected void forceSwitchEngine(boolean online) {
        Log.i(TAG, "forceSwitchEngine>>" + Boolean.toString(online));
		/*if(synthesizer.isSpeaking()){
			stopSpeakingAbsolte();
		}*/
        //if(online){
        /*if (!isLocalEngine)
            return;
        isLocalEngine = false;*/
        synthesizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        synthesizer.setParameter(SpeechConstant.SPEED, Integer.toString(mVoiceConfig.getVolSpeed()));
        synthesizer.setParameter(SpeechConstant.VOLUME, Integer.toString(mVoiceConfig.getVolume()));
        synthesizer.setParameter(SpeechConstant.VOICE_NAME, mVoiceConfig.getVolName());
		/*}
		else{
			if(isLocalEngine)return;
			isLocalEngine=true;
			synthesizer.setParameter(SpeechConstant.SPEED, OFFLINE_ENGINE_SPPED);
			synthesizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
			synthesizer.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
		}*/
    }

    public class IflyMsg extends PcmPlayer.PcmMsg {
        String text;
        IflySynConfig config;

        public IflyMsg(String text, IflySynConfig config, String fileName) {
            super(fileName, 0);
            this.text = text;
            this.config = config;
        }
    }

}
