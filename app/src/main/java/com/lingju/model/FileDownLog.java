package com.lingju.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by Ken on 2017/1/6.<br />
 */
@Entity
public class FileDownLog {
    @Id(autoincrement = true)
    private Long id;
    private String downpath;
    private int threadid;
    private int downlength;
    @Generated(hash = 1353085836)
    public FileDownLog(Long id, String downpath, int threadid, int downlength) {
        this.id = id;
        this.downpath = downpath;
        this.threadid = threadid;
        this.downlength = downlength;
    }
    @Generated(hash = 940540041)
    public FileDownLog() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getDownpath() {
        return this.downpath;
    }
    public void setDownpath(String downpath) {
        this.downpath = downpath;
    }
    public int getThreadid() {
        return this.threadid;
    }
    public void setThreadid(int threadid) {
        this.threadid = threadid;
    }
    public int getDownlength() {
        return this.downlength;
    }
    public void setDownlength(int downlength) {
        this.downlength = downlength;
    }

}
