package com.lingju.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * pcm音频播放类
 */
public class PcmPlayer implements AudioTrack.OnPlaybackPositionUpdateListener {
    private final static String TAG = "PcmPlayer";

    //播放采样率
    protected static final int sampleRate = 16000;
    //播放最小缓存大小
    protected static final int wBufferSize = 2 * AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
    //Android播放组件
    protected final AudioTrack audioTrack;
    protected Handler handler;
    private Context mContext;
    //存放PCM音频的临时文件夹
    private File cacheDir;
    //PCM实体类的队列
    private final LinkedBlockingQueue<PcmMsg> pcms = new LinkedBlockingQueue<>();
    //是否允许播放
    private final AtomicBoolean play = new AtomicBoolean(false);
    //是否缓存中
    private final AtomicBoolean pending = new AtomicBoolean(false);
    //播放锁
    private final Lock playLock = new ReentrantLock();
    //播放锁的Condition
    private final Condition playCondition = playLock.newCondition();

    //播放监听类
    private PlayListener playListener;
    //当次播放的pcm文件的当前已播放长度
    private int pcmLength;

    public PcmPlayer(Context context, Handler handler) {
        this.mContext = context;
        this.audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, wBufferSize, AudioTrack.MODE_STREAM);
        this.handler = handler;
        audioTrack.setPlaybackPositionUpdateListener(this, handler);
        cacheDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
    }

    /**
     * 停止播放
     */
    public void stop() {
        Log.i(TAG, "stop............");
        this.pcms.clear();
        this.pending.set(false);
        this.play.set(false);
        //this.audioTrack.stop();
        notifyPlay();
    }

    public File getCacheDir() {
        return cacheDir;
    }

    /**
     * 是否允许播放
     *
     * @return
     */
    public boolean playAllowed() {
        return play.get();
    }

    /**
     * 清空等待播放队列中的pcm文件
     */
    private void clearPcmFile() {
        for (File f : cacheDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".pcm");
            }
        })) {
            f.delete();
        }
    }

    /**
     * 添加PcmMsg到播放队列
     *
     * @param pcmMsg
     * @param last   是否最后一个，如果是最后一个，pending设置为false，没有需要缓存的pcm文件了
     */
    public void append(PcmMsg pcmMsg, boolean last) {
        pcms.offer(pcmMsg);
        this.pending.set(!last);
    }

    /**
     * 通知等待中的播放器开始播放
     */
    public void notifyPlay() {
        if (playLock.tryLock()) {
            try {
                playCondition.signalAll();
            } finally {
                playLock.unlock();
            }
        }
    }

    /**
     * 异步播放
     *
     * @param playListener
     */
    public void play(PlayListener playListener) {
        this.playListener = playListener;
        clearPcmFile();
        pcmLength = 0;
        pending.set(true);
        play.set(true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                playPoll();
            }
        }).start();
    }

    /**
     * 同步播放，切记不要在主线程调用该方法
     *
     * @param playListener
     */
    public void playSync(PlayListener playListener) {
        this.playListener = playListener;
        clearPcmFile();
        pcmLength = 0;
        pending.set(true);
        play.set(true);
        playPoll();
    }

    /**
     * 遍历队列中的PCM文件并依队列顺序播放
     */
    private void playPoll() {
        PcmMsg pcmMsg = null;
        int i = 0;
        boolean paused = false;
        if (playLock.tryLock()) {
            try {
                while (play.get()) {
                    pcmMsg = pcms.peek();
                    if (pcmMsg != null && pcmMsg.getProgress() == 100) {
                        pcms.poll();
                        if (i == 0) {
                            playListener.onSpeakBegin();
                        } else if (paused) {
                            playListener.onSpeakResumed();
                        }
                        i++;
                        paused = false;
                        playListener.onSpeakProgress(-1);
                        try {
                            if (audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                                Log.i(TAG, "audioTrack play.............");
                                audioTrack.play();
                                audioTrack.setNotificationMarkerPosition(0);
                            }
                            play(pcmMsg);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            playListener.onError("文件没有找到");
                            break;
                        }
                    } else if (pending.get()) {
                        Log.i(TAG, "playPoll await..............");
                        if (i > 0) {
                            paused = true;
                            playListener.onSpeakPaused();
                        }
                        playCondition.await(3000, TimeUnit.MILLISECONDS);
                    }
                }
                if (audioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                    audioTrack.stop();
                }
            } catch (Exception e) {
                if (e instanceof InterruptedException)
                    Thread.interrupted();
                e.printStackTrace();
            } finally {
                Log.i(TAG, "play end.............");
                playLock.unlock();
            }
        }
    }

    private void play(PcmMsg pcmMsg) throws FileNotFoundException {
        File f = new File(cacheDir, pcmMsg.getPath());
        if (!f.exists()) {
            int i = 100;
            while (i-- > 0 && play.get()) {
                try {
                    Thread.sleep(50);
                    f = new File(cacheDir, pcmMsg.getPath());
                    if (f.exists())
                        break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
        try {
            byte[] buffer = new byte[wBufferSize];
            int l;
            //Log.i(TAG,"file size="+l);
            //audioTrack.setNotificationMarkerPosition((int) (l / 2));
            //audioTrack.play();

            while ((l = in.read(buffer)) != -1 && play.get()) {
                audioTrack.write(buffer, 0, l);
                pcmLength += l;
            }
            if (!pending.get() && pcms.size() == 0) {
                Log.i(TAG, "2mark......." + (pcmLength / 2));
                audioTrack.setNotificationMarkerPosition(pcmLength / 2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        /*if(!isPrecision){
            onMarkerReached(audioTrack);
        }*/
    }

    /**
     * 获取播放器的状态
     *
     * @return
     */
    public int getPlayState() {
        return audioTrack.getPlayState();
    }


    @Override
    public void onMarkerReached(AudioTrack track) {
        Log.i(TAG, "onMarkerReached>>>" + track.getNotificationMarkerPosition());
        if (playLock.tryLock()) {
            try {
                playCondition.signalAll();
            } finally {
                playLock.unlock();
            }
        }
        Log.i(TAG, "PCM SIZE=" + pcms.size());
        if (!pending.get() && pcms.size() == 0) {
            play.set(false);
            playListener.onCompleted();
        }
    }

    @Override
    public void onPeriodicNotification(AudioTrack track) {
        //Log.i(TAG,"onPeriodicNotification>>>"+(++i)+"-"+track.getPositionNotificationPeriod());
    }

    /**
     * PCM文件实体类
     */
    public static class PcmMsg {
        //pcm文件路径
        protected String path;
        //pcm文件的缓存百分比，0-100，100代表该文件已经确保所有数据均有效
        protected int progress;

        public PcmMsg() {
        }

        public PcmMsg(String path, int progress) {
            this.path = path;
            this.progress = progress;
        }

        public String getPath() {
            return path;
        }

        public PcmMsg setPath(String path) {
            this.path = path;
            return this;
        }

        public int getProgress() {
            return progress;
        }

        public PcmMsg setProgress(int progress) {
            this.progress = progress;
            return this;
        }
    }

    /**
     * 播放监听器
     */
    public interface PlayListener {
        /**
         * 播放过程中出错回调
         *
         * @param msg
         */
        public void onError(String msg);

        /**
         * 播放结束回调
         */
        public void onCompleted();

        /**
         * 播放开始回调
         */
        public void onSpeakBegin();

        /**
         * 播放暂停回调
         */
        public void onSpeakPaused();

        /**
         * @param p
         */
        @Deprecated
        public void onSpeakProgress(int p);

        /**
         * 恢复播放
         */
        public void onSpeakResumed();
    }
}
