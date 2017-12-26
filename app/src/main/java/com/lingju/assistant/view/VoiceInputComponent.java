package com.lingju.assistant.view;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.LexiconListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.index.view.ChatListFragment;
import com.lingju.assistant.service.AssistantService;
import com.lingju.assistant.service.VoiceMediator;
import com.lingju.audio.PcmRecorder;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.common.log.Log;
import com.lingju.util.AudioJsonParser;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VoiceInputComponent extends LinearLayout {
    private final static String TAG = "VoiceInputComponent";
    @BindView(R.id.iv_mic_default)
    AppCompatImageView mIvMicDefault;
    @BindView(R.id.iv_circle_record)
    AppCompatImageView mIvCircleRecord;
    private Context context;
    private Animation micAn1 = null;
    private boolean recording = false;

    private OnResultListener listener;
    public StringBuffer temp = new StringBuffer();

    private SpeechRecognizer recognizer;
    private SpeechPlayer mPlayer;
    private PcmRecorder mRecorder;
    private String lastText;
    private int current_state = -1;
    private List<String> mSaveKeywords;
    private List<String> mQuitKeywords;

    public VoiceInputComponent(Context context) {
        super(context);
    }

    public VoiceInputComponent(Context context, AttributeSet attrs) {
        super(context, attrs);
        /*TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.lingju);
        inputTips=a.getString(R.styleable.lingju_title);
		a.recycle();*/
        init(context);
    }

    public VoiceInputComponent(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        /*TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.lingju);
        inputTips=a.getString(R.styleable.lingju_title);
		a.recycle();*/
        init(context);
    }

    public void setOnResultListener(OnResultListener listener) {
        this.listener = listener;
    }

    private void init(Context context) {
        this.context = context;
        View contentView = LayoutInflater.from(context).inflate(R.layout.mic_input, this);
        ButterKnife.bind(this, contentView);

        micAn1 = AnimationUtils.loadAnimation(context, R.anim.mic_loading);

        mIvMicDefault.setOnTouchListener(defaultTounchListener);
        mIvCircleRecord.setOnTouchListener(defaultTounchListener);
        setVoiceButton(0);
        initComponent(context);
        mSaveKeywords = Arrays.asList(ChatListFragment.SAVE_KEYWORDS);
        mQuitKeywords = Arrays.asList(ChatListFragment.QUIT_KEYWORDS);
    }

    private void initComponent(Context context) {
        if (mRecorder == null) {
            mRecorder = new PcmRecorder(context);
            mRecorder.setOnRecordListener(recordListener);
        }
        if (recognizer == null) {
            mPlayer = new SpeechPlayer();
            recognizer = SpeechRecognizer.createRecognizer(context, rInitListener);
            if (!"-1".equals(recognizer.getParameter(SpeechConstant.AUDIO_SOURCE))) {
                setRecognizeParams();
            }
        }
    }

    /**
     * 录音过程监听器
     **/
    private PcmRecorder.RecordListener recordListener = new PcmRecorder.RecordListener() {
        @Override
        public void onStart() {
            if (mRecorder.isRecord() && recognizer != null && !recognizer.isListening())
                startRecognize();
        }

        @Override
        public void onRecord(byte[] datas, int len) {
            if (recognizer != null) {
                recognizer.writeAudio(datas, 0, len);
            }
        }

        @Override
        public void onVadEnd() {
            if (recognizer != null)
                recognizer.stopListening();
        }

        @Override
        public boolean isSaveTape() {
            return false;
        }
    };

    /**
     * 识别器初始化监听器。
     */
    private InitListener rInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.i(TAG, "SpeechRecognizer init() code = " + code);
            if (code == ErrorCode.SUCCESS) {
                Log.i(TAG + ".rInitListener", "speechRecognizer init success!");
                setRecognizeParams();
            }
        }

    };

    private void setRecognizeParams() {
        if (recognizer != null) {
            recognizer.setParameter(SpeechConstant.DOMAIN, "iat");//识别：iat，search，video，poi，music
            recognizer.setParameter(SpeechConstant.ACCENT, "mandarin");//可选：mandarin，cantonese
            recognizer.setParameter(SpeechConstant.LANGUAGE, "zh_cn");//支持：zh_cn，zh_tw，en_us
            recognizer.setParameter(SpeechConstant.VAD_BOS, Integer.toString(4000));//默认值：短信转写5000，其他4000
            recognizer.setParameter(SpeechConstant.VAD_EOS, Integer.toString(1000));//默认值：短信转写1800，其他700
            recognizer.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");
            recognizer.setParameter(SpeechConstant.NET_TIMEOUT, "6000");
            recognizer.setParameter(SpeechConstant.KEY_SPEECH_TIMEOUT, "6000");
            recognizer.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "0");
            recognizer.setParameter(SpeechConstant.PARAMS, "asr_ptt=1,asr_nbest=1");
            recognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            Log.e(TAG, "first search contacts!!!!!!!!!!!!!!!!!!!!!!!!");
            //        recognizer.updateLexicon("userword", AppConfig.getContactsText(), lexiconListener);
        }
    }

    private void resetParams() {
        if (recognizer != null) {
            recognizer.setParameter(SpeechConstant.AUDIO_SOURCE, null);
        }
    }

    private LexiconListener lexiconListener = new LexiconListener() {

        @Override
        public void onLexiconUpdated(String id, SpeechError e) {
            if (e == null) {
                Log.d("Setting.lexiconListener", "id=" + id + ",e=" + e + ",词典更新成功");
            } else {
                Log.d("Setting.lexiconListener", "id=" + id + "," + e.getErrorDescription());
            }
        }
    };

    private OnTouchListener defaultTounchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Log.w(TAG, "voiceInputDefaultBT.setOnTouchListener event:" + event.describeContents());
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Log.i(TAG, "action_down");
                switchRecord();
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                Log.i(TAG, "action_up");
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                Log.i(TAG, "action_move");
                return true;
            }
            return false;
        }
    };

    /**
     * 切换录音状态
     **/
    public void switchRecord() {
        if (recording) {
            stopRecord();
        } else {
            prepareToRecord();
        }
    }

    public boolean isRecording() {
        return recording;
    }

    private void stopWakeup() {
        Intent intent = new Intent(context, AssistantService.class);
        intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_WAKEUP_LISTEN);
        context.startService(intent);
    }

    private void stopRecognize() {
        Log.i("LingJu", "VoiceInputComponent stopRecognize() start");
        if (recognizer.isListening()) {
            mRecorder.stop(VoiceMediator.get().isWakeUpMode());
            recognizer.cancel();
            Log.i("LingJu", "VoiceInputComponent stopRecognize() end");
        }
    }


    private void startRecognize() {
        recognizer.startListening(rListener);
        mRecorder.start();
    }

    /**
     * 停止录音
     */
    public void stopRecord() {
        if (recording) {
            recording = false;
            setVoiceButton(0);
            stopRecognize();
            Intent intent = new Intent(context, AssistantService.class);
            intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_RECOGNIZE);
            context.startService(intent);
        }
    }

    /**
     * 准备进入录音状态
     */
    private void prepareToRecord() {
        stopWakeup();
        initComponent(context);
        if (SynthesizerBase.isInited()) {
            Intent intent = new Intent(context, AssistantService.class);
            intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PAUSE_PLAY);
            context.startService(intent);
            recording = true;
            setVoiceButton(1);
            mPlayer.playAssetsFile(context.getResources().getString(R.string.audio_start), false,
                    new PlayerListener() {

                        @Override
                        public void onPlayComplete() {
                            startRecognize();
                        }
                    });
        }
    }


    public void setVoiceButton(int status) {
        Log.i("LingJu", "setVoiceButton  .....STATUE:" + status);
        if (status == current_state)
            return;
        current_state = status;
        switch (status) {
            case 0:
            case 2:
                mIvMicDefault.setVisibility(View.VISIBLE);
                mIvCircleRecord.clearAnimation();
                mIvCircleRecord.setVisibility(View.GONE);
                recording = false;
                break;
            case 1:
                mIvMicDefault.setVisibility(GONE);
                mIvCircleRecord.setVisibility(VISIBLE);
                mIvCircleRecord.startAnimation(micAn1);
                break;
        }

    }

    /**
     * 是否有录音内容标记
     **/
    private boolean hasValue = false;
    private RecognizerListener rListener = new RecognizerListener() {

        @Override
        public void onVolumeChanged(int v, byte[] bytes) {

        }

        @Override
        public void onResult(RecognizerResult result, boolean isLast) {
            if (null != result && result.getResultString().length() > 2) {
                // 显示
                Log.i(TAG + ".RecognizerListener", "recognizer result：" + result.getResultString());
                temp.append(AudioJsonParser.parseIatResult(result.getResultString()));
                hasValue = true;
                if (listener != null) {
                    listener.onResult(temp.toString().trim());
                    if (!isLast)
                        lastText = temp.toString().trim();
                    temp.setLength(0);
                }
            } else {
                Log.i(TAG + ".RecognizerListener", "recognizer result : null");
            }
            if (isLast) {
                //Log.i("LingJu", "正常接收识别。。");
                if ((mSaveKeywords.contains(lastText) || mQuitKeywords.contains(lastText)) && "。".equals(temp.toString())) {
                    onSuccess();
                    return;
                }
                if (mRecorder == null)
                    return;
                if (mRecorder.isRecord()) {
                    recognizer.startListening(rListener);
                    return;
                }
                //				temp.append('。');
                Log.i(TAG + ".RecognizerListener", "result:" + temp);
                onSuccess();
            }
        }

        @Override
        public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {

        }

        @Override
        public void onError(SpeechError e) {
            //Log.i("LingJu", "识别出错:" + e.getErrorDescription());
            if (mRecorder.isRecord() && e.getErrorCode() == ErrorCode.MSP_ERROR_NO_DATA) {
                recognizer.startListening(rListener);
            } else if (hasValue) {
                onSuccess();
            } else {
                mPlayer.playAssetsFile(context.getResources().getString(R.string.audio_error), false, null);
                if (listener != null) {
                    listener.onError(e.getErrorCode(), e.getErrorDescription());
                }
                mRecorder.stop(VoiceMediator.get().isWakeUpMode());
                setVoiceButton(0);
                temp.setLength(0);
            }
        }

        @Override
        public void onEndOfSpeech() {
            if (mRecorder != null && !mRecorder.isRecord())
                mPlayer.playAssetsFile(context.getResources().getString(R.string.audio_wait), false, null);
        }

        @Override
        public void onBeginOfSpeech() {
            temp.setLength(0);
        }
    };

    private void onSuccess() {
        // mPlayer.playAssetsFile(context.getResources().getString(R.string.audio_result), false, null);
        setVoiceButton(0);
        hasValue = false;
    }

    public interface OnResultListener {
        public void onResult(String text);

        public void onError(int errorCode, String description);
    }

    public interface PlayerListener {
        /**
         * 播放完成
         */
        public void onPlayComplete();
    }

    class SpeechPlayer implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {
        private final String TAG = "SpeechPlayer";
        // 播放器
        private MediaPlayer mPlayer = new MediaPlayer();

        private AssetManager mAssetManager;
        /**
         * 播放器回调
         */

        private PlayerListener mPlayListener;

        private Timer timer;

        /**
         * 构造函数
         */
        public SpeechPlayer() {
            mPlayer = new MediaPlayer();
            timer = new Timer(true);
            mAssetManager = context.getAssets();
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnCompletionListener(this);
        }

        /**
         * 播放assets目录下的音频文件
         *
         * @param file     音频文件，含路径
         * @param repeat   是否循环
         * @param listener 播放回调
         */
        public void playAssetsFile(String file, boolean repeat, PlayerListener listener) {
            mPlayListener = listener;
            playAssetsFile(file, repeat);
        }

        /**
         * 播放音频文件
         *
         * @param file
         * @param repeat
         */
        private void playAssetsFile(String file, boolean repeat) {
            try {
                Log.i(TAG, "file:" + file);
                AssetFileDescriptor fd = mAssetManager.openFd(file);
                if (mPlayer == null)
                    mPlayer = new MediaPlayer();
                mPlayer.reset();
                mPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.setLooping(repeat);
                mPlayer.prepareAsync();
                fd.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * 停止播放
         */
        public void stopPlay() {
            if (mPlayer != null && mPlayer.isPlaying()) {
                mPlayListener = null;
                mPlayer.stop();
            }
        }

        //long b;
        @Override
        public void onCompletion(MediaPlayer mp) {
            Log.d(TAG, "onCompletion");
            //Log.e(TAG, "onCompletion::::"+(System.currentTimeMillis()-b));
        /*if(mPlayListener!=null&&Setting.MediaPlayerFast&&!LingjuRecognizer.getInstance().isLocalEngine()){
            mPlayListener.onPlayComplete();
		}*/
        }

        public boolean isPlaying() {
            return mPlayer.isPlaying();
        }

        /**
         * 销毁播放器
         */
        public void onDestroy() {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            Log.i(TAG, "onPrepared duration:" + mp.getDuration());
            mp.start();
            if (mPlayListener != null/*&&((!Setting.MediaPlayerFast)||LingjuRecognizer.getInstance().isLocalEngine())*/) {
                timer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        if (mPlayListener != null)
                            mPlayListener.onPlayComplete();
                    }
                }, mp.getDuration() - 100);

            }
        }
    }

    /**
     * 控件脱离窗口时调用。当该话筒作为列表item中的控件时，刷新item会执行该方法，从而将录音组件资源清空；
     * 需要在开始录音时重新初始化录音组件，保证录音成功。
     **/
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        resetParams();
        if (mRecorder != null && recognizer != null) {
            stopRecord();
            recognizer.destroy();
            mPlayer.onDestroy();
            mRecorder = null;
            recognizer = null;
            Log.i("LingJu", "VoiceInputComponent onDetachedFromWindow()");
        }
    }
}
