package com.lingju.audio.engine;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.lingju.audio.PcmRecorder;
import com.lingju.audio.SpeechPlayer;
import com.lingju.audio.SpeechPlayer.PlayerListener;
import com.lingju.audio.SystemVoiceMediator;
import com.lingju.audio.engine.base.RecognizerBase;
import com.lingju.util.AudioJsonParser;
import com.lingju.voice.R;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class IflyRecognizer extends RecognizerBase {
    private static final String TAG = "IflyRecognizer";
    private SpeechRecognizer recognizer;
    private static IflyRecognizer instance;

    /**
     * 针对于新建备忘
     **/
    public final static int CREATE_MEMO_MODE = 0;
    /**
     * 针对于备忘添加模式的模式
     **/
    public final static int MODIFY_MEMO_MODE = 1;
    /**
     * 正常录音
     **/
    public final static int DEFAULT_TAPE = 2;
    /**
     * 长录音
     **/
    public final static int LONG_TAPE = 3;

    public static boolean wake_mode_playing = false;
    AudioManager mAudioManager;
    private volatile boolean recognizing;
    private PcmRecorder mRecorder;      //本地录音器

    private boolean initParam = false;
    private boolean long_time_record = false;   //长时间录音识别标记
    private int long_record_mode = -1;
    private String tapeText = "";       //录音文本
    private PowerManager.WakeLock wl;

    private IflyRecognizer(Context context, SystemVoiceMediator mediator) {
        super.mContext = context;
        super.mediator = mediator;
        mPlayer = SpeechPlayer.createInstance(mContext, mediator);
        mRecorder = new PcmRecorder(context);
        mRecorder.setOnRecordListener(recordListener);
        Log.i(TAG, "before SpeechRecognizer.createRecognizer");
        recognizer = SpeechRecognizer.createRecognizer(context, rInitListener);
        Log.i(TAG, "after SpeechRecognizer.createRecognizer");
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (!initParam)
                    setDefaultRecognizerParam(false);
            }
        }, 3000);
    }

    public static boolean isInited() {
        if (instance == null)
            return false;
        return true;
    }

    public static IflyRecognizer getInstance() {
        if (instance == null)
            try {
                throw new Exception("初始化出错");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        return instance;
    }

    public SpeechRecognizer getRecognizer() {
        return recognizer;
    }

    public boolean isListening() {
        return isRecognizing();
    }

    public boolean isRecognizing() {
        return recognizing;
    }

    public static IflyRecognizer createInstance(Context context, SystemVoiceMediator mediator) {
        if (instance == null) {
            instance = new IflyRecognizer(context, mediator);
        }
        return instance;
    }


    public void startRecognize() {
        Log.i(TAG, "startRecognize>>>" + Thread.currentThread().getName());
        mediator.startBluetoothSco();
        if (rLock.tryLock()) {
            try {
                startRecognize(true);
            } finally {
                rLock.unlock();
            }
        }
    }

    /**
     * 开始识别
     *
     * @param audio 是否添加音效
     **/
    public void startRecognize(boolean audio) {
        if (rLock.tryLock()) {
            try {
                try {
                    if (isRecognizing()) {
                        stopRecognize();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //cancel=!audio;
                //VoiceService.get().getSynthesizer().setPausedTime(0);
                if (mediator.isPlaying() || wake_mode_playing) {
                    Log.i(TAG, "isPlaying.........");
                    playingBeforRecognize = true;
                    wake_mode_playing = false;
                    mediator.pausePlay();
                } else {
                    Log.i(TAG, "no Playing.........");
                    playingBeforRecognize = false;
                }
                // TODO: 2017/6/22 暂定单独对喜马拉雅音频进行判断，应与上面音乐播放判断合并
                if (mediator.isTinging()) {
                    tingBeforRecognize = true;
                    mediator.pauseTing();
                } else {
                    tingBeforRecognize = false;
                }
                mediator.stopWaitPlay();
                mediator.stopSynthesize();
                mediator.stopWakenup();
                mediator.openBlueHeadSet4Recognition();
            /*	if(mContext.isBlueToothHeadSet()&&mContext.getBluetoothHeadset()!=null&&!mContext.getBluetoothHeadset().getConnectedDevices().isEmpty()){
                    int c=0;
					BluetoothDevice device=mContext.getBluetoothHeadset().getConnectedDevices().get(0);
					while(mContext.isBlueToothHeadSet()&&!mContext.getBluetoothHeadset().isAudioConnected(device)){
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						Log.i(TAG, "c=" + c);
						c++;
						if(c>30){
							break;
						}
					}
				}*/

                recognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);

                cancel = false;
                if (audio) {
                    recognizing = true;
                    mPlayer.playAssetsFile(mContext.getResources().getString(R.string.audio_start), false, new PlayerListener() {

                        @Override
                        public void onPlayComplete() {
                            if (cancel)
                                return;
                            if (rLock.tryLock()) {
                                try {
                                    recognizing = true;
                                    if (long_time_record || (mediator.isWakeUpMode())) {  //录音识别模式或唤醒模式下使用“-1”
                                        recognizer.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");
                                        mRecorder.start();
                                    } else {
                                        recognizer.setParameter(SpeechConstant.AUDIO_SOURCE, null);
                                    }
                                    recognize_start_moments = System.currentTimeMillis();
                                    Log.i(TAG, "长时间录音标记: " + long_time_record + " " + long_record_mode + " 识别模式：" + recognizer.getParameter(SpeechConstant.AUDIO_SOURCE));
                                    recognizer.startListening(RecognizerListener);

                                } finally {
                                    rLock.unlock();
                                }
                            } else {
                                Log.i(TAG, "rLock.tryLock failed!");
                            }
                        }
                    });
                } else {
                    recognize_start_moments = System.currentTimeMillis();
                    recognizer.startListening(RecognizerListener);
                    recognizing = true;
                }
            } finally {
                rLock.unlock();
            }
        }
    }

    public long getRecognize_start_moments() {
        return recognize_start_moments;
    }

    public long getRecognize_end_moments() {
        return recognize_end_moments;
    }

    public boolean isPlayingBeforRecognize() {
        return playingBeforRecognize;
    }

    public boolean isTingBeforeRecognize() {
        return tingBeforRecognize;
    }

    /* 	public void stopRecordAndCallBack(){
             Log.e(TAG, "stopRecordAndCallBack");
             if(cacelCallback)return;
             cancel=true;
             cacelCallback=true;
             try{
                 if(recognizer.isListening()){
                     //if(VoiceService.get().getAwakener().isWorking()){
                         VoiceService.get().getAwakener().setRecognizeEnd();
                     //}
                     Log.e(TAG, "recognizer.stopRecordAndCallBack()");
                     recognizer.stopListening();
                    //recognizer.stopListening();
                 }
             }catch(Exception e){
                 e.printStackTrace();
             }
         }
    */

    public void stopRecognize() {
        Log.e(TAG, "stopRecognize");
        cancel = true;
        try {
            if (isRecognizing()) {
                //if(VoiceService.get().getAwakener().isWorking()/*&&VoiceService.get().getAwakener().getWakeup_type()==0*/){
                     /*VoiceService.get().getAwakener().setRecognizeEnd();
                     mContext.stopInMsgTips();*/
                //}
                Log.e(TAG, "stopRecognize recognizer.cancel()");
                if (mRecorder.isRecord())
                    mRecorder.stop(mediator.isWakeUpMode());
                /*if (long_time_record)
                    recognizer.stopListening();
                else*/
                //在会话被取消后，当前会话结束，未返回的结果将不再返回
                recognizer.cancel();
                recognize_end_moments = System.currentTimeMillis();
                recognizing = false;

                //若处于保存录音模式，停止识别时退出录音模式，保存录音文件，同步录音数据
                if (long_record_mode == LONG_TAPE) {
                    mediator.onTapeResult(tapeText);
                    tapeText = "";
                }
                //stopBluetoothSco();
                //recognizer.stopListening();
                //cacelCallback=false;
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public void reset() {
        try {
            Log.e(TAG, "stopRecognize>>destroy>>reset");
            recognizer.destroy();
            recognizer.stopListening();
            recognizer = SpeechRecognizer.createRecognizer(mContext, rInitListener);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setRecognizerListener(MyRecognizerListener recognizerListener) {
        this.RecognizerListener = recognizerListener;
    }

    public void destory() {
        recognizer.destroy();
        mPlayer.onDestroy();
        mPlayer = null;
        instance = null;
        mRecorder = null;
    }


    /**
     * 读取文件
     *
     * @return
     */
    public String readFile(String file, String code) {

        StringBuffer grammar = new StringBuffer("");
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(mContext.getAssets().open(file));
            int len = 0;
            byte[] buf = new byte[1024 * 10];
            while ((len = in.read(buf)) != -1) {
                grammar.append(new String(buf, 0, len, code));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return grammar.toString();
    }


    public void resetParam() {
        setDefaultRecognizerParam(true);
    }


    public int getLong_record_mode() {
        return long_record_mode;
    }

    public void setLong_record_mode(int long_record_mode) {
        this.long_record_mode = long_record_mode;
    }

    /**
     * 通过改变音频源类型，设置识别模式。注意：必须在开启识别前设置
     *
     * @param mode false:讯飞默认模式，将音频交给讯飞录制和识别
     *             true:自定义长时间录音模式，本地录制并以音频流的方式持续发送给讯飞识别
     **/
    public void setRecognizeMode(boolean mode) {
        Log.i("LingJu", "IflyRecognizer setRecognizeMode()>>>" + mode);
        long_time_record = mode;
        recognizer.setParameter(SpeechConstant.AUDIO_SOURCE, mode ? "-1" : null);
        // recognizer.setParameter(SpeechConstant.VAD_EOS, Integer.toString(mode ? 600 : 1000));
        if (wl == null) {
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.ON_AFTER_RELEASE | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "BRIGHT");
        }
        //是否需计算锁的数量
        // wl.setReferenceCounted(false);
        if (mode && long_record_mode < DEFAULT_TAPE) {
            //点亮屏幕
            wl.acquire();
        } else if (!mode) {
            //释放
            if (wl.isHeld())
                wl.release();
            long_record_mode = -1;
        }
    }

    public boolean isLong_time_record() {
        return long_time_record;
    }

    /**
     * 长时间录音过程监听器
     **/
    private PcmRecorder.RecordListener recordListener = new PcmRecorder.RecordListener() {
        @Override
        public void onStart() {
            if (mRecorder.isRecord() && !isListening()) {
                Log.i("LingJu", "PcmRecorder onStart()222");
                recognizer.startListening(RecognizerListener);
            }
        }

        @Override
        public void onRecord(byte[] datas, int len) {
            recognizer.writeAudio(datas, 0, len);
        }

        @Override
        public void onVadEnd() {
            recognizer.stopListening();
            Log.i("LingJu", "IflyRecognizer onVadEnd()");
        }

        @Override
        public boolean isSaveTape() {
            return long_record_mode >= DEFAULT_TAPE;
        }
    };

    @Override
    public void setParams(String params, String value) {
        recognizer.setParameter(params, value);
    }

    /**
     * 设置默认的识别引擎参数
     *
     * @param reset
     */
    private void setDefaultRecognizerParam(final boolean reset) {
        recognizer.setParameter(SpeechConstant.DOMAIN, "iat");//识别：iat，search，video，poi，music
        recognizer.setParameter(SpeechConstant.ACCENT, "mandarin");//可选：mandarin，cantonese
        recognizer.setParameter(SpeechConstant.LANGUAGE, "zh_cn");//支持：zh_cn，zh_tw，en_us
        recognizer.setParameter(SpeechConstant.VAD_BOS, Integer.toString(4000));//默认值：短信转写5000，其他4000
        recognizer.setParameter(SpeechConstant.VAD_EOS, Integer.toString(1000));//默认值：短信转写1800，其他700
        //recognizer.setParameter(SpeechConstant.ENGINE_MODE, SpeechConstant.MODE_PLUS);
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
        String grammar = recognizer.getParameter(SpeechConstant.LOCAL_GRAMMAR);
        //recognizer.setParameter(SpeechConstant.ENGINE_TYPE,SpeechConstant.TYPE_AUTO);
        recognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        /*if(PhoneContactUtils.getInstance(null).getList().size()>0) {
                Log.i(TAG, "mContext.isOnline");
				updateLexicon(PhoneContactUtils.getInstance(null).getContactText());
		}
		else{
			PhoneContactUtils.getInstance(null).setContactsNameList();
		}*/
        if (mediator != null)
            mediator.updateLexicon();
    }

    public void updateLexicon(String userwords) {
        System.out.println("updateLexicon>>>" + userwords);
        recognizer.updateLexicon("userword", userwords, lexiconListener);
    }

	/*private String[] getFields(List<PlayMusic> list){
        StringBuffer titles=new StringBuffer("");
		StringBuffer singers=new StringBuffer("");
		int l=list.size();
		titles.append(list.get(0).getTitle());
		singers.append(list.get(0).getSinger());
		char c;
		for(int i=1;i<l;i++){
			titles.append("\r\n"+list.get(i).getTitle());
			c=list.get(i).getSinger().charAt(0);
			if(!(c>=0x4e00&&c<=0x9fa5)&&!(c>=0&&c<=127)){
				list.get(i).setSinger("未知");
			}
			//singers.append("\r\n"+list.get(i).getSinger());
		}
		List<String> ts=new ArrayList<String>();
		ts.add(list.get(0).getSinger());
		int ll;
		for(int i=1;i<l;i++){
			ll=ts.size();
			while(--ll>=0){
				if(ts.get(ll).equals(list.get(i).getSinger())){
					break;
				}
			}
			if(ll<0){
				ts.add(list.get(i).getSinger());
			}
		}
		l=ts.size();
		for(int i=1;i<l;i++){
			singers.append("\r\n"+ts.get(i));
		}
		ts.clear();
		ts=null;
		titles.append("\r\nunknown");
		singers.append("\r\n未知\r\n本地\r\n手机\r\n收藏");
		return new String[]{titles.toString(),singers.toString()};
	}*/


    private com.iflytek.cloud.LexiconListener lexiconListener = new com.iflytek.cloud.LexiconListener() {

        @Override
        public void onLexiconUpdated(String id, SpeechError e) {
            if (e == null) {
                Log.d("Setting.lexiconListener", "id=" + id + ",e=" + e + ",词典更新成功");
            } else {
                Log.d("Setting.lexiconListener", "id=" + id + "," + e.getErrorDescription());
            }
        }
    };

		/*@Override
        public void onLexiconUpdated(String arg0, int arg1) throws RemoteException {
			
		}*/


    /**
     * 识别器初始化监听器。
     */
    private InitListener rInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code == ErrorCode.SUCCESS) {
                initParam = true;
                Log.i(TAG, "speechRecognizer init success!");
                setDefaultRecognizerParam(false);
            }
        }

    };


    public long getLastRecognized() {
        return lastRecognized;
    }

    public void setLastRecognized(long lastRecognized) {
        this.lastRecognized = lastRecognized;
    }

    public int getNetworkStatus() {
        return networkStatus;
    }

    public void setNetworkStatus(int networkStatus) {
        this.networkStatus = networkStatus;
    }

    public int getErrorCount() {
        return errorCount;
    }

    /**
     * 讯飞语音识别监听器
     */
    private MyRecognizerListener RecognizerListener = new MyRecognizerListener();

    public class MyRecognizerListener implements com.iflytek.cloud.RecognizerListener {
        public StringBuffer temp;

        public MyRecognizerListener() {
            setEmptyTemp();
        }

        public void setEmptyTemp() {
            temp = new StringBuffer("");
        }

        @Override
        public void onBeginOfSpeech() {
            recognizing = true;
            Log.i(TAG, "onBeginOfSpeech");
            //cacelCallback=false;
            if (temp == null)
                setEmptyTemp();
            mediator.onRecoginzeBegin();
        }

        @Override
        public void onEndOfSpeech() {
            if (mRecorder.isRecord())
                return;
            Log.i(TAG, "onEndOfSpeech" + ",cancel=" + Boolean.toString(cancel));
            recognize_end_moments = System.currentTimeMillis();
            /*if (recognizer.getParameter(SpeechConstant.LANGUAGE).equals("en_us")) {
                recognizer.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            }*/
            if (!cancel &&/*!cacelCallback&&*/!mediator.preToCall())
                mPlayer.playAssetsFile(mContext.getResources().getString(R.string.audio_wait), true, null);
            //if(cacelCallback)cacelCallback=false;
            IflyRecognizer.this.onSpeechCompleted();
        }

        @Override
        public void onError(SpeechError e) {
            Log.i("LingJu", "MyRecognizerListener onError()>>>");
            //在长时间录音模式下，如果因为无说话内容结束识别则重启
            if (mRecorder.isRecord() && long_time_record && long_record_mode != DEFAULT_TAPE && e.getErrorCode() == ErrorCode.MSP_ERROR_NO_DATA) {
                recognizer.startListening(RecognizerListener);
                return;
            }
            /*String content = temp.toString();
            if (long_time_record && long_record_mode != DEFAULT_TAPE) {
                IflyRecognizer.this.onLondRecordResult(content);
                mPlayer.playAssetsFile(mContext.getResources().getString(R.string.audio_result), false, null);
                temp.setLength(0);
                return;
            }*/
            recognizing = false;
            //识别出错时，要及时关闭本地录音器
            mRecorder.stop(mediator.isWakeUpMode());
            if (!cancel && !mediator.preToCall()) {
                mPlayer.playAssetsFile(mContext.getResources().getString(R.string.audio_error), false, null);
            }
            IflyRecognizer.this.onError(e.getErrorCode(), e.getErrorDescription());
        }


        @Override
        public void onResult(RecognizerResult result, boolean isLast) {
            recognize_end_moments = System.currentTimeMillis();
            if (null != result && result.getResultString().length() > 2) {
                // 显示
                Log.i(TAG, "recognizer result：" + result.getResultString());
                temp.append(AudioJsonParser.parseIatResult(result.getResultString()));
            } else {
                Log.i(TAG, "recognizer result : null");
            }
            if (isLast) {
                if (temp.length() <= 0) {
                    if (mRecorder.isRecord() && long_time_record && long_record_mode != DEFAULT_TAPE)
                        recognizer.startListening(RecognizerListener);
                    else {
                        Log.i("LingJu", "MyRecognizerListener onResult()>>>> error");
                        mPlayer.playAssetsFile(mContext.getResources().getString(R.string.audio_error), false, null);
                        IflyRecognizer.this.onError(ErrorCode.MSP_ERROR_NO_DATA, "");
                    }
                    return;
                }
                String content = temp.toString();
                //如果已开启长时间录音，则在识别自动结束时重启识别
                if (mRecorder.isRecord() && long_time_record && long_record_mode != DEFAULT_TAPE) {
                    onSuccess(content);
                    recognizer.startListening(RecognizerListener);
                    return;
                }
                recognizing = false;
                onSuccess(content);
            }
        }

        /**
         * 识别成功，结果处理
         **/
        private void onSuccess(String content) {
            if (long_time_record && long_record_mode < DEFAULT_TAPE) {      //无限时识别，非保存录音模式
                IflyRecognizer.this.onLondRecordResult(content);
            } else if (long_record_mode == LONG_TAPE) {     //无限时识别，保存录音模式
                tapeText += content;
            } else if (long_record_mode == DEFAULT_TAPE) {     //正常识别，保存录音模式
                //停止录音
                mRecorder.stop(mediator.isWakeUpMode());
                mediator.onTapeResult(content);
            } else if (!long_time_record) {      //讯飞默认识别模式
                if (recognizer.getParameter(SpeechConstant.LANGUAGE).equals("en_us")) {
                    recognizer.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
                }
                mRecorder.stop(mediator.isWakeUpMode());
                mPlayer.playAssetsFile(mContext.getResources().getString(R.string.audio_result), false, null);
                IflyRecognizer.this.onResult(content);
            }
            temp.setLength(0);
        }

        @Override
        public void onVolumeChanged(int v, byte[] buffer) {
            mediator.onRecoginzeVolumeChanged(v);
        }

        @Override
        public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {


        }
    }


}
