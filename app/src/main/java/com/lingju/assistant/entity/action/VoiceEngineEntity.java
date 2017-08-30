package com.lingju.assistant.entity.action;

/**
 * Created by Ken on 2017/5/17.
 */
public class VoiceEngineEntity {

    private int id = 311;
    private String language;    //语言（CHINESE, ENGLISH）
    private int type;           //切换类型（0：仅一次 1：永久切换） 
    private String role;        //发音人
    private String recordmode;  //录音模式（DEFAULT：讯飞默认，LONG：自定义长录音）

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getRecordmode() {
        return recordmode;
    }

    public void setRecordmode(String recordmode) {
        this.recordmode = recordmode;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
