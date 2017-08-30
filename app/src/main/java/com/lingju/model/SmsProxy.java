package com.lingju.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by Ken on 2017/8/11.<br />
 */
@Entity
public class SmsProxy {
    @Id
    private long id;         //短信记录ID
    private int status=-1;     //短信状态  状态：0=已读，1=未读
    //以下与服务器同步标记
    private String sid;        //服务器记录id,该记录的唯一标示
    private long timestamp;    //时间戳，记录的时效性标记
    private int recyle;     //0：有效， 1：回收
    private boolean synced;  //false表示未同步，待更新
    @Generated(hash = 43624390)
    public SmsProxy(long id, int status, String sid, long timestamp, int recyle,
            boolean synced) {
        this.id = id;
        this.status = status;
        this.sid = sid;
        this.timestamp = timestamp;
        this.recyle = recyle;
        this.synced = synced;
    }
    @Generated(hash = 347793236)
    public SmsProxy() {
    }
    public long getId() {
        return this.id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public int getStatus() {
        return this.status;
    }
    public void setStatus(int status) {
        this.status = status;
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
