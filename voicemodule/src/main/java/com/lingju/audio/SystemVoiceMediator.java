package com.lingju.audio;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.os.Handler;

import com.lingju.audio.engine.base.SpeechMsg;
import com.lingju.context.entity.base.IChatResult;

/**
 * Created by Administrator on 2016/11/1.
 */
public interface SystemVoiceMediator {


    /**
     * 获取蓝牙耳机对象
     **/
    BluetoothHeadset getBluetoothHeadset();

    /**
     * 获取可操控蓝牙设备对象
     **/
    BluetoothA2dp getBluetoothA2dp();

    /**
     * 设备是否正在播放声音
     *
     * @return
     */
    boolean isPlaying();

    /**
     * 是否正在播放有声内容
     **/
    boolean isTinging();

    void setAudioPlayType(int playType);

    int getAudioPlayType();

    /**
     * 设备是否了插入有线耳机
     *
     * @return
     */
    public boolean isHeadset();

    /**
     * 设备是否处于唤醒模式
     *
     * @return
     */
    boolean isWakeUpMode();

    /**
     * 设置唤醒模式标记
     **/
    void setWakeupModeFlag(boolean isWakeUpMode);

    /**
     * 设备是否连接了蓝牙耳机
     *
     * @return
     */
    boolean isBlueToothHeadSet();

    /**
     * 打开蓝牙耳机录音通道，拟通过蓝牙蓝牙耳机做语音识别
     */
    void openBlueHeadSet4Recognition();


    void startBluetoothSco();

    /**
     * 关闭蓝牙耳机的SCO通道
     */
    void stopBluetoothSco();

    /**
     * 关闭设备的语音合成
     */
    void stopSynthesize();

    /**
     * 修改设备media的音量
     *
     * @param percent [0-100]
     */
    void changeMediaVolume(int percent);

    void recordCurrentVolume();

    /**
     * 恢复设备media的音量，即{SystemVoiceMediator#changeMediaVolume changeMediaVolume}之前的音量
     */
    void resumeMediaVolume();

    boolean isWalkNavi();

    /**
     * 停止识别
     */
    void stopRecognize();

    /**
     * 唤醒成功回调
     *
     * @param wakeupWord 触发唤醒的唤醒词
     */
    void onWakenup(String wakeupWord);

    /**
     * 发送文本到robot
     **/
    void sendMsg2Robot(String msg);

    /**
     * 开启/关闭唤醒
     *
     * @param flag 打开(true)/关闭(false)
     **/
    void setWakeUpMode(boolean flag);

    void tryToWakeup();

    boolean allowSynthersize(SpeechMsg SpeechMsgm);

    boolean compareSpeechMsg(SpeechMsg nSpeechMsg, SpeechMsg currentSpeechMsg);

    void onRecognizeError(int code, String msg);

    void onRecoginzeWait();

    void onRecoginzeResult(String result);

    void onTapeResult(String tapeContent);

    void stopWaitPlay();

    void setRobotResponse(boolean hasResponse);

    void onLongRecoginzeResult(String result);

    void startRecognize();

    void keepVoiceCtrl(SpeechMsg msg);

    void setCalling(boolean isCalling);

    boolean isCalling();

    void setMobileRing(boolean mobileRing);

    boolean mobileRing();

    void pausePlay();

    void pauseTing();

    void startWakeup();

    void stopWakenup();

    void updateLexicon();

    void onRecoginzeBegin();

    boolean openSpeaker();

    void onRecoginzeVolumeChanged(int v);

    boolean preToCall();

    void onSynthesizerInited(int code);

    void onSynthesizerError(String errorMsg);

    Handler createHandler();


    interface ChatStateListener {

        void onInput(String text);

        void onResult(IChatResult result);
    }
}
