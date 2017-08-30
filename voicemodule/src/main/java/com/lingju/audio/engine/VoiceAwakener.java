package com.lingju.audio.engine;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;

import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.WakeuperResult;
import com.iflytek.cloud.util.ResourceUtil;
import com.lingju.audio.SystemVoiceMediator;
import com.lingju.audio.engine.base.WakeupEngineBase;
import com.lingju.common.log.Log;
import com.lingju.voice.R;

import org.json.JSONObject;

/**
 * Created by Administrator on 2015/12/8.
 */
public class VoiceAwakener extends WakeupEngineBase {
    private final static String TAG = "VoiceAwakener";

    private static VoiceAwakener instance;
    private VoiceWakeuper mIvw;
    private final static int MAX = 60;
    private final static int MIN = -20;
    private int curThresh = MIN;
    private String threshStr = "门限值：";

    public static VoiceAwakener createInstance(Context mContext, SystemVoiceMediator mediator) {
        if (instance == null)
            instance = new VoiceAwakener(mContext, mediator);
        return instance;
    }

    private VoiceAwakener(Context context, SystemVoiceMediator mediator) {
        super.mContext = context;
        super.mediator = mediator;
        at = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, wBufferSize, AudioTrack.MODE_STREAM);
        //processor= AudioProcessor_old.createInstance(threadPool);

        // 加载识唤醒地资源，resPath为本地识别资源路径
        StringBuffer param = new StringBuffer();
        String resPath = ResourceUtil.generateResourcePath(this.mContext,
                ResourceUtil.RESOURCE_TYPE.assets, "ivw/" + this.mContext.getString(R.string.app_id) + ".jet");

        param.append(SpeechConstant.IVW_RES_PATH + "=" + resPath);
        param.append("," + ResourceUtil.ENGINE_START + "=" + SpeechConstant.ENG_IVW);
        boolean ret = SpeechUtility.getUtility().setParameter(
                ResourceUtil.ENGINE_START, param.toString());

        if (!ret) {
            Log.d(TAG, "启动本地引擎失败！");
        }
        System.out.println(mIvw);
        // 初始化唤醒对象
        if (mIvw == null)
            mIvw = VoiceWakeuper.createWakeuper(this.mContext, new InitListener() {
                @Override
                public void onInit(int i) {
                    if (mIvw != null) {
                        // 清空参数
                        mIvw.setParameter(SpeechConstant.PARAMS, null);
                        mIvw.setParameter(SpeechConstant.IVW_RES_PATH, ResourceUtil.generateResourcePath(mContext,
                                ResourceUtil.RESOURCE_TYPE.assets, "ivw/" + mContext.getString(R.string.app_id) + ".jet"));
                        mIvw.setParameter(SpeechConstant.IVW_NET_MODE, "0");
                        /**
                         * 唤醒门限值，根据资源携带的唤醒词个数按照“id:门限;id:门限”的格式传入
                         * 示例demo默认设置第一个唤醒词，建议开发者根据定制资源中唤醒词个数进行设置
                         */
                        mIvw.setParameter(SpeechConstant.IVW_THRESHOLD, "0:" + curThresh + ",1:" + curThresh + ",2:" + curThresh);
                        // 设置唤醒模式
                        mIvw.setParameter(SpeechConstant.IVW_SST, "wakeup");
                        // 不持续进行唤醒
                        mIvw.setParameter(SpeechConstant.KEEP_ALIVE, "0");
                    } else {
                        Log.i(TAG, "唤醒未初始化11111111111111");
                    }
                }
            });
        if (mIvw != null) {
            // 清空参数
            mIvw.setParameter(SpeechConstant.PARAMS, null);
            mIvw.setParameter(SpeechConstant.IVW_RES_PATH, resPath);
            mIvw.setParameter(SpeechConstant.IVW_NET_MODE, "0");
            /**
             * 唤醒门限值，根据资源携带的唤醒词个数按照“id:门限;id:门限”的格式传入
             * 示例demo默认设置第一个唤醒词，建议开发者根据定制资源中唤醒词个数进行设置
             */
            mIvw.setParameter(SpeechConstant.IVW_THRESHOLD, "0:" + curThresh + ",1:" + curThresh + ",2:" + curThresh);
            // 设置唤醒模式
            mIvw.setParameter(SpeechConstant.IVW_SST, "wakeup");
            // 不持续进行唤醒
            mIvw.setParameter(SpeechConstant.KEEP_ALIVE, "0");
        } else {
            Log.i(TAG, "唤醒未初始化");
        }

    }


    private com.iflytek.cloud.WakeuperListener mWakeuperListener = new com.iflytek.cloud.WakeuperListener() {

        @Override
        public void onResult(WakeuperResult result) {
            try {
                String text = result.getResultString();
                JSONObject object;
                object = new JSONObject(text);
                StringBuffer buffer = new StringBuffer();
                buffer.append("【RAW】 " + text);
                buffer.append("\n");
                buffer.append("【操作类型】" + object.optString("sst"));
                buffer.append("\n");
                buffer.append("【唤醒词id】" + object.optString("id"));
                buffer.append("\n");
                buffer.append("【得分】" + object.optString("score"));
                buffer.append("\n");
                buffer.append("【前端点】" + object.optString("bos"));
                buffer.append("\n");
                buffer.append("【尾端点】" + object.optString("eos"));
                Log.i(TAG, "onResult>>" + buffer.toString());
                if (object.getInt("score") >= 5) {
                    VoiceAwakener.this.onResult();
                } else {
                    //start();
                    isListening();
                    mIvw.startListening(mWakeuperListener);
                }
            } catch (Exception e) {
                Log.i(TAG, "结果解析出错");
                e.printStackTrace();
            }
        }

        @Override
        public void onError(SpeechError error) {
            Log.i(TAG, error.getPlainDescription(true));
        }

        @Override
        public void onBeginOfSpeech() {
            Log.i(TAG, "开始说话");
        }

        @Override
        public void onEvent(int eventType, int isLast, int arg2, Bundle obj) {
            Log.i(TAG, "eventType=" + eventType + ",isLast=" + isLast + ",arg2=" + arg2);
        }

        @Override
        public void onVolumeChanged(int volume) {
            // TODO Auto-generated method stub
            Log.i(TAG, "onVolumeChanged" + volume);
        }
    };


    @Override
    public boolean isListening() {
        boolean listening = mIvw != null && mIvw.isListening();
        Log.e(TAG, "isListening>>" + Boolean.toString(listening));
        return listening;
    }

    @Override
    public void destory() {
        Log.d(TAG, "onDestroy WakeDemo");
        mIvw = VoiceWakeuper.getWakeuper();
        if (mIvw != null) {
            mIvw.destroy();
        } else {
            Log.i(TAG, "唤醒未初始化");
        }
    }

    @Override
    protected void start() {
        if (!isListening()) {
            Log.i("LingJu", "VoiceAwakener isStart!");
            mIvw.startListening(mWakeuperListener);
        }
    }

    @Override
    public void stopCompletely() {
        if (isListening()) {
            Log.i("LingJu", "VoiceAwakener isStop!");
            mIvw.cancel();
        }
    }
}
