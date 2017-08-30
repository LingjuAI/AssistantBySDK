package com.lingju.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by Ken on 2017/8/9.<br />
 */
@Entity
public class CallLogProxy {
    @Id
    private long id;

    //以下与服务器同步标记
    private String sid;        //服务器记录id,该记录的唯一标示
    private long timestamp;    //时间戳，记录的时效性标记
    private int recyle;     //0：有效， 1：回收
    private boolean synced;  //false表示未同步，待更新
    @Generated(hash = 1510980851)
    public CallLogProxy(long id, String sid, long timestamp, int recyle,
            boolean synced) {
        this.id = id;
        this.sid = sid;
        this.timestamp = timestamp;
        this.recyle = recyle;
        this.synced = synced;
    }
    @Generated(hash = 401042397)
    public CallLogProxy() {
    }
    public long getId() {
        return this.id;
    }
    public void setId(long id) {
        this.id = id;
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
