package com.lingju.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by Ken on 2017/8/9.<br />
 */
@Entity
public class ContactsProxy {
    @Id
    private String rawContactId;    //联系人ID
    private String name;            //联系人名称
    private String nickName;        //联系人昵称
    private String company;
    private String job;
    private String codes;
    //以下与服务器同步标记
    private String sid;        //服务器记录id,该记录的唯一标示
    private long timestamp;    //时间戳，记录的时效性标记
    private int recyle;     //0：有效， 1：回收
    private boolean synced;  //false表示未同步，待更新
    @Generated(hash = 1460306873)
    public ContactsProxy(String rawContactId, String name, String nickName,
            String company, String job, String codes, String sid, long timestamp,
            int recyle, boolean synced) {
        this.rawContactId = rawContactId;
        this.name = name;
        this.nickName = nickName;
        this.company = company;
        this.job = job;
        this.codes = codes;
        this.sid = sid;
        this.timestamp = timestamp;
        this.recyle = recyle;
        this.synced = synced;
    }
    @Generated(hash = 532057348)
    public ContactsProxy() {
    }
    public String getRawContactId() {
        return this.rawContactId;
    }
    public void setRawContactId(String rawContactId) {
        this.rawContactId = rawContactId;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getNickName() {
        return this.nickName;
    }
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }
    public String getCompany() {
        return this.company;
    }
    public void setCompany(String company) {
        this.company = company;
    }
    public String getJob() {
        return this.job;
    }
    public void setJob(String job) {
        this.job = job;
    }
    public String getCodes() {
        return this.codes;
    }
    public void setCodes(String codes) {
        this.codes = codes;
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
