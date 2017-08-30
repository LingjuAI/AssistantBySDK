package com.lingju.audio;


import android.content.Context;
import android.media.AudioRecord;
import android.os.AsyncTask;
import android.util.Log;

import com.lingju.audio.base.IRecognizer;
import com.lingju.audio.buffer.AudioVadContext;
import com.lingju.util.FileUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Administrator on 2016/11/22.
 */
public class PcmRecorder implements Runnable {

    public final static String TAPE_SRC = FileUtil.DEFAULT_DIR + "/tape/";
    public final static String TEMP_FILE = TAPE_SRC + "temp.pcm";
    private IRecognizer.RecognizeStatus status = IRecognizer.RecognizeStatus.Idle;
    private AtomicBoolean running = new AtomicBoolean(false);
    private LingjuRecorder mRecorder;
    private int bufferLength = 320;
    private AudioVadContext vadContext;

    public PcmRecorder(Context context) {
        mRecorder = LingjuRecorder.create(context);
    }


    public void start() {
        running.set(true);
        AsyncTask.THREAD_POOL_EXECUTOR.execute(this);
    }

    public void stop(boolean isWakeMode) {
        if (running.get()) {
            running.set(false);
            if (!isWakeMode)
                mRecorder.stop();
            Log.i("LingJu", "PcmRecorder stop()");
        }
    }

    public boolean isRecord() {
        return running.get() && mRecorder.getStatus() == AudioRecord.RECORDSTATE_RECORDING;
    }

    @Override
    public void run() {
        if (mRecorder.getStatus() != AudioRecord.RECORDSTATE_RECORDING) {
            mRecorder.start();
        }
        byte buffer[] = new byte[bufferLength];
        int l;
        vadContext = mRecorder.getVadContext();
        BufferedOutputStream bos = null;
        try {
            if (mRecordListener != null && mRecordListener.isSaveTape()) {
                File tapeDir = new File(TAPE_SRC);
                if (!tapeDir.exists())
                    tapeDir.mkdirs();
                bos = new BufferedOutputStream(new FileOutputStream(TEMP_FILE));
            }
            while (running.get() && (l = mRecorder.read(buffer, 0, bufferLength)) != -1) {
                switch (status) {
                    case Idle://当前没有启动语音识别引擎
                        if (vadContext.vadFrontActivate(500)) {//检测发现最近500ms是有效音频
                            if (mRecordListener != null)
                                mRecordListener.onStart();
                            int rl = 50 * 320;
                            mRecorder.rollback(rl);
                            byte buf[] = new byte[rl];
                            mRecorder.read(buf, 0, rl);
                            status = IRecognizer.RecognizeStatus.Recording;
                            if (mRecordListener != null)
                                mRecordListener.onRecord(buf, rl);//保存本地录音
                            saveTape(buf, rl, bos);
                        }else {
                            checkVad(2000);
                        }
                        break;
                    case Recording://当前语音识别引擎正在录音/写入音频数据
                        if (mRecordListener != null) {
                            mRecordListener.onRecord(buffer, l);
                        }
                        saveTape(buffer, l, bos);
                        checkVad(700);
                        break;
                }
            }
            status = IRecognizer.RecognizeStatus.Idle;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /** 端点检测 **/
    private void checkVad(int timeout) {
        if (vadContext.vadRearTimeout(timeout)) {   //检测发现最近timeout ms是无效音频，停止识别
            status = IRecognizer.RecognizeStatus.Idle;
            if (mRecordListener != null)
                mRecordListener.onVadEnd();
        }
    }

    /**
     * 保存录音文件
     **/
    private void saveTape(byte[] buffer, int l, BufferedOutputStream bos) throws IOException {
        int t;
        if (bos != null) {
            t = 0;
            while (t < l) {
                bos.write(buffer, t, 2);
                t += 4;
            }
        }
    }

    private RecordListener mRecordListener;

    public void setOnRecordListener(RecordListener listener) {
        this.mRecordListener = listener;
    }

    public interface RecordListener {
        /**
         * 开始录音
         **/
        void onStart();

        /**
         * 录音中触发
         **/
        void onRecord(byte[] datas, int len);

        /**
         * 录音开始时静音超时触发
         **/
        void onVadEnd();

        /**
         * 是否本地保存录音
         **/
        boolean isSaveTape();
    }
}
