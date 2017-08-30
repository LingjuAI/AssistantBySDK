package com.lingju.audio.engine;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.lingju.audio.base.IRecognizer;
import com.lingju.util.AudioJsonParser;

import io.reactivex.Observable;

/**
 * Created by Ken on 2017/6/30.
 */
public class LingJuCloudAwakener implements IRecognizer {

    private static final String TAG = "LingJuCloudAwakener";
    private static LingJuCloudAwakener instance;
    private SpeechRecognizer recognizer;
    private volatile RecognizeStatus status = RecognizeStatus.Idle;
    private IRecognizer.RecognizeListener listener;

    private LingJuCloudAwakener(Context context) {
        recognizer = SpeechRecognizer.createRecognizer(context, rInitListener);
    }

    public static LingJuCloudAwakener createInstance(Context mContext) {
        if (instance == null)
            instance = new LingJuCloudAwakener(mContext);
        return instance;
    }

    /**
     * 识别器初始化监听器。
     */
    private InitListener rInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            if (code == ErrorCode.SUCCESS) {
                Log.i(TAG, "speechRecognizer init success!");
                setDefaultRecognizerParam();
            }
        }

    };

    private void setDefaultRecognizerParam() {
        recognizer.setParameter(SpeechConstant.DOMAIN, "iat");//识别：iat，search，video，poi，music
        recognizer.setParameter(SpeechConstant.ACCENT, "mandarin");//可选：mandarin，cantonese
        recognizer.setParameter(SpeechConstant.LANGUAGE, "zh_cn");//支持：zh_cn，zh_tw，en_us
        recognizer.setParameter(SpeechConstant.VAD_BOS, Integer.toString(4000));//默认值：短信转写5000，其他4000
        recognizer.setParameter(SpeechConstant.VAD_EOS, Integer.toString(1000));//默认值：短信转写1800，其他700
        recognizer.setParameter(SpeechConstant.AUDIO_SOURCE, null);
        recognizer.setParameter(SpeechConstant.NET_TIMEOUT, "6000");
        recognizer.setParameter(SpeechConstant.KEY_SPEECH_TIMEOUT, "6000");
        recognizer.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "0");
        /**
         * SpeechConstant.PARAMS
         * 转写支持：
         asr_ptt：是否加入标点符号，如asr_ptt=0
         asr_nbest：多候选结果，如asr_nbest=3
         asr_audio_path：保存音频路径，如asr_audio_path=/sdcard/asr.pcm
         合成支持：
         tts_buffer_time：播放缓冲时间，即缓冲多少秒音频后开始播放，如tts_buffer_time=5000
         tts_audio_path：保存音频路径，如tts_audio_path=/sdcard/tts.pcm
         */
        recognizer.setParameter(SpeechConstant.PARAMS, "asr_ptt=1,asr_nbest=1");
        recognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
    }

    @Override
    public Observable start() {
        return null;
    }

    @Override
    public void start(RecognizeListener listener) {
        // Log.i("LingJu", "LingJuCloudAwakener start()");
        this.listener = listener;
        status = RecognizeStatus.Started;
        recognizer.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");
        recognizer.startListening(recognizerListener);
    }

    @Override
    public void write(byte[] buffer, int offset, int length) {
        switch (status) {
            case Started:
                status = RecognizeStatus.Recording;
            case Recording:
                break;
            default:
                return;
        }
        recognizer.writeAudio(buffer, offset, length);
    }

    @Override
    public void finish() {
        switch (status) {
            case Idle:
                return;
            case Started:
            case Recording:
                status = RecognizeStatus.Idle;
                break;
            default:
                return;
        }
        // Log.i("LingJu", "LingJuCloudAwakener finish()");
        recognizer.setParameter(SpeechConstant.AUDIO_SOURCE, null);
        recognizer.stopListening();
    }

    @Override
    public void stop() {
        if (status != RecognizeStatus.Idle) {
           //  Log.i("LingJu", "LingJuCloudAwakener stop()");
            recognizer.setParameter(SpeechConstant.AUDIO_SOURCE, null);
            status = RecognizeStatus.Idle;
            recognizer.cancel();
        }
    }

    @Override
    public RecognizeStatus status() {
        return status;
    }

    private StringBuilder temp = new StringBuilder();
    private RecognizerListener recognizerListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int i, byte[] bytes) {
            if (listener != null)
                listener.onVolumeChanged(i);
        }

        @Override
        public void onBeginOfSpeech() {
            if (listener != null)
                listener.onBeginOfSpeech();
        }

        @Override
        public void onEndOfSpeech() {
            if (listener != null)
                listener.onEndOfSpeech();
        }

        @Override
        public void onResult(RecognizerResult result, boolean isLast) {
            if (null != result && result.getResultString().length() > 2) {
                // 显示
                Log.i(TAG, "recognizer result：" + result.getResultString());
                temp.append(AudioJsonParser.parseIatResult(result.getResultString()));
            } else {
                Log.i(TAG, "recognizer result : null");
            }
            if (isLast) {
                if (temp.length() <= 0) {
                    if (status == RecognizeStatus.Recording)
                        recognizer.startListening(recognizerListener);
                    return;
                }
                status = RecognizeStatus.Idle;
                String content = temp.toString();
                temp.setLength(0);
                if (listener != null)
                    listener.onResult(content);
            }
        }

        @Override
        public void onError(SpeechError e) {
            temp.setLength(0);
            //在长时间录音模式下，如果因为无说话内容结束识别则重启
            if (status == RecognizeStatus.Recording && e.getErrorCode() == ErrorCode.MSP_ERROR_NO_DATA)
                recognizer.startListening(recognizerListener);
            else {
                status = RecognizeStatus.Idle;
                if (listener != null)
                    listener.onError(e.getErrorCode(), e.getErrorDescription());
            }
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };
}
