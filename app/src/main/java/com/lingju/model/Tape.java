package com.lingju.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

import java.util.Date;

/**
 * Created by Ken on 2017/6/2.
 */
@Entity
public class Tape {
    @Id(autoincrement = true)
    private Long id;
    private String text;
    private String url;
    private Date modified;
    private Date created;

    //同步相关属性
    private String sid;
    private long timestamp;
    private int recyle;
    private  boolean synced = true;
    @Generated(hash = 1448652314)
    public Tape(Long id, String text, String url, Date modified, Date created,
            String sid, long timestamp, int recyle, boolean synced) {
        this.id = id;
        this.text = text;
        this.url = url;
        this.modified = modified;
        this.created = created;
        this.sid = sid;
        this.timestamp = timestamp;
        this.recyle = recyle;
        this.synced = synced;
    }
    @Generated(hash = 439713945)
    public Tape() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getText() {
        return this.text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public String getUrl() {
        return this.url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public Date getModified() {
        return this.modified;
    }
    public void setModified(Date modified) {
        this.modified = modified;
    }
    public Date getCreated() {
        return this.created;
    }
    public void setCreated(Date created) {
        this.created = created;
    }
    public String getSid() {
        return this.sid;
    }
    public void setSid(String sid) {
        this.sid = sid;
    }
    public long getTimestamp() {
        return this.timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    public int getRecyle() {
        return this.recyle;
    }
    public void setRecyle(int recyle) {
        this.recyle = recyle;
    }
    public boolean getSynced() {
        return this.synced;
    }
    public void setSynced(boolean synced) {
        this.synced = synced;
    }


}
