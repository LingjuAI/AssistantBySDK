package com.lingju.assistant.entity.action;

import com.lingju.context.entity.NewAudioEntity;
import com.lingju.context.entity.Progress;

import java.util.List;

/**
 * Created by Ken on 2017/5/16.
 */
public class PlayerEntity {

    private int id = 300;
    private String origin;
    private List<NewAudioEntity> object;      //播放信息，根据type而定(APP只提供音乐播放)
    private String mode;        //播放模式（顺序、随机等）
    private Progress progress;  //进度控制
    private Progress volume;    //音量控制
    private String control;     //播放控制
    private String type;        //播放信息类型（AUDIO、VEDIO）
    private String playnettype; //播放时网络类型（3G,WIFI）
    private String textshowtype;

    public String getControl() {
        return control;
    }

    public void setControl(String control) {
        this.control = control;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public List<NewAudioEntity> getObject() {
        return object;
    }

    public void setObject(List<NewAudioEntity> object) {
        this.object = object;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getPlaynettype() {
        return playnettype;
    }

    public void setPlaynettype(String playnettype) {
        this.playnettype = playnettype;
    }

    public Progress getProgress() {
        return progress;
    }

    public void setProgress(Progress progress) {
        this.progress = progress;
    }

    public String getTextshowtype() {
        return textshowtype;
    }

    public void setTextshowtype(String textshowtype) {
        this.textshowtype = textshowtype;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Progress getVolume() {
        return volume;
    }

    public void setVolume(Progress volume) {
        this.volume = volume;
    }
}
