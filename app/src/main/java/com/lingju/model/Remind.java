package com.lingju.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

import java.util.Date;


@Entity(nameInDb = "t_remind")
public class Remind {
    @Id(autoincrement = true)
    private Long id;
    private String content;
    private Date rdate;
    private String rtime;
    private int frequency;
    private Date created;
    /**
     * 1:有效  0：无效
     **/
    private int valid;

    //以下与服务器同步标记
    private String sid;        //服务器记录id,该记录的唯一标示
    private long timestamp;    //时间戳，记录的时效性标记，版本号
    private int recyle;     //0：有效， 1：回收
    private boolean synced = true;  //false表示未同步，待更新

    public Remind() {
        // TODO Auto-generated constructor stub
    }

    @Generated(hash = 187513480)
    public Remind(Long id, String content, Date rdate, String rtime, int frequency,
            Date created, int valid, String sid, long timestamp, int recyle,
            boolean synced) {
        this.id = id;
        this.content = content;
        this.rdate = rdate;
        this.rtime = rtime;
        this.frequency = frequency;
        this.created = created;
        this.valid = valid;
        this.sid = sid;
        this.timestamp = timestamp;
        this.recyle = recyle;
        this.synced = synced;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getRdate() {
        return this.rdate;
    }

    public void setRdate(Date rdate) {
        this.rdate = rdate;
    }

    public String getRtime() {
        return this.rtime;
    }

    public void setRtime(String rtime) {
        this.rtime = rtime;
    }

    public int getFrequency() {
        return this.frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public Date getCreated() {
        return this.created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public int getValid() {
        return this.valid;
    }

    public void setValid(int valid) {
        this.valid = valid;
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
