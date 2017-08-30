package com.lingju.audio.config;

import android.util.Log;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechSynthesizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/9/12.
 */
public class IflySynConfig {

    private String role;
    private String mood;

    private String volName;
    private Integer pitch;
    private Integer speed;
    private Integer volume;

    public IflySynConfig(String role, String mood, String volName, Integer pitch, Integer speed, Integer volume) {
        this.role = role;
        this.mood = mood;
        this.volName = volName;
        this.pitch = pitch;
        this.speed = speed;
        this.volume = volume;
    }

    private final static List<IflySynConfig> ifySynConfigs = new ArrayList<>();

    static {
        init();
    }

    public static synchronized void init() {
        ifySynConfigs.add(new IflySynConfig("默认", "正常", "jiajia", 50, 50, 50));
        ifySynConfigs.add(new IflySynConfig("爷爷", "正常", "niyang", 20, 40, 80));
        ifySynConfigs.add(new IflySynConfig("爷爷", "生气", "niyang", 20, 60, 100));
        ifySynConfigs.add(new IflySynConfig("奶奶", "正常", "aismengchun", 25, 40, 80));
        ifySynConfigs.add(new IflySynConfig("奶奶", "生气", "aismengchun", 30, 60, 100));
        ifySynConfigs.add(new IflySynConfig("中年男人", "正常", "aisxiaoyu", 55, 50, 80));
        ifySynConfigs.add(new IflySynConfig("中年男人", "生气", "aisxiaoyu", 57, 70, 100));
        ifySynConfigs.add(new IflySynConfig("中年女人", "正常", "aisxmeng", 55, 50, 80));
        ifySynConfigs.add(new IflySynConfig("中年女人", "生气", "aisxmeng", 57, 70, 100));
        ifySynConfigs.add(new IflySynConfig("男孩", "正常", "aisnn", 50, 60, 80));
        ifySynConfigs.add(new IflySynConfig("男孩", "生气", "aisnn", 60, 80, 100));
        ifySynConfigs.add(new IflySynConfig("女孩", "正常", "aisnn", 60, 80, 100));
        ifySynConfigs.add(new IflySynConfig("女孩", "生气", "aisnn", 75, 80, 100));
        //临时设置，防止出错
        ifySynConfigs.add(new IflySynConfig("随机", "正常", "aisxqiang", 50, 60, 80));
    }

    public static IflySynConfig get(String role) {
        return get(role, "正常");
    }

    public static IflySynConfig get(String role, String mood) {
        if ("默认".equals(role)) {
            ifySynConfigs.get(0).volName = VoiceConfig.get().getVolName();
            ifySynConfigs.get(0).volume = VoiceConfig.get().getVolume();
            ifySynConfigs.get(0).speed = VoiceConfig.get().getVolSpeed();
            return ifySynConfigs.get(0);
        }
        for (IflySynConfig ic : ifySynConfigs) {
            if (ic.getRole().equals(role) && ic.getMood().equals(mood))
                return ic;
        }
        return null;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public String getVolName() {
        return volName;
    }

    public void setVolName(String volName) {
        this.volName = volName;
    }

    public Integer getPitch() {
        return pitch;
    }

    public void setPitch(Integer pitch) {
        this.pitch = pitch;
    }

    public Integer getSpeed() {
        return speed;
    }

    public void setSpeed(Integer speed) {
        this.speed = speed;
    }

    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }

    public void applyTo(SpeechSynthesizer ss) {
        Log.i("IfySynConif", "speed:" + speed + ",vol:" + volume + ",volname:" + volName + ",pitch:" + pitch);
        ss.setParameter(SpeechConstant.SPEED, speed.toString());
        ss.setParameter(SpeechConstant.VOLUME, volume.toString());
        ss.setParameter(SpeechConstant.VOICE_NAME, volName);
        ss.setParameter(SpeechConstant.PITCH, pitch.toString());
    }
}

