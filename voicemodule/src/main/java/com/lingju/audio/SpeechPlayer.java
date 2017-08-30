package com.lingju.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.util.Log;

import java.util.Timer;

public class SpeechPlayer implements OnCompletionListener, OnPreparedListener {
    private final String TAG = "SpeechPlayer";

    private final Context mContext;
    private final SystemVoiceMediator mediator;
    // 播放器
    private MediaPlayer mPlayer;

    private AssetManager mAssetManager;
    private static SpeechPlayer instance;

    /**
     * 播放器回调
     */
    public static interface PlayerListener {
        /**
         * 播放完成
         */
        void onPlayComplete();
    }

    private PlayerListener mPlayListener;

    private Timer timer;
    private String currentFile;

    /**
     * 构造函数
     */
    private SpeechPlayer(Context context, SystemVoiceMediator mediator) {
        this.mContext = context;
        this.mediator = mediator;
        mPlayer = new MediaPlayer();
        timer = new Timer(true);
        mAssetManager = context.getAssets();
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
    }

    public static SpeechPlayer createInstance(Context context, SystemVoiceMediator mediator) {
        if (instance == null) {
            instance = new SpeechPlayer(context, mediator);
        }
        return instance;
    }

    public static SpeechPlayer getInstance() {
        return instance;
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
            currentFile = file;
            AssetFileDescriptor fd = mAssetManager.openFd(file);
            mPlayer.reset();
            mPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            mPlayer.setAudioStreamType(mediator.isBlueToothHeadSet() ? AudioManager.STREAM_VOICE_CALL : AudioManager.STREAM_MUSIC);
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
    public void stopPlay(String file) {
        Log.i("LingJu", "SpeechPlayer stopPlay()>>" + file + " " + (mPlayer != null));
        if (file.equals(currentFile) && mPlayer != null && mPlayer.isPlaying()) {
            Log.i("LingJu", "stopPlay()");
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
        if (mPlayListener != null)
            mPlayListener.onPlayComplete();
    }

    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    /**
     * 销毁播放器
     */
    public void onDestroy() {
        try {
            if (mPlayer.isPlaying())
                mPlayer.stop();
            mPlayer.release();
            instance = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.i(TAG, "onPrepared duration:" + mp.getDuration());
        mp.start();
        //		if(mPlayListener!=null/*&&((!Setting.MediaPlayerFast)||LingjuRecognizer.getInstance().isLocalEngine())*/){
        //			timer.schedule(new TimerTask() {
        //
        //				@Override
        //				public void run() {
        //					mPlayListener.onPlayComplete();
        //				}
        //			},mp.getDuration()-100);
        //			/*try {
        //				Thread.sleep(mp.getDuration()-100);
        //			} catch (InterruptedException e) {
        //				// TODO Auto-generated catch block
        //				e.printStackTrace();
        //			}
        //			//Log.e(TAG, "onPrepared:"+(System.currentTimeMillis()-b));
        //			mPlayListener.onPlayComplete();*/
        //		}
    }
}
