package com.lingju.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

import java.util.Date;


@Entity(nameInDb = "t_alarm")
public class AlarmClock {
    @Id(autoincrement = true)
    private Long id;
    private int rtime;
    /**
     * 重复设置：
     * 数字值的定义A={0=仅一次，1=周一，2=周二...7=周日}
     * 该属性值的取值必须由A中的数字组成：
     * 如:1237=周一周二周三周日重复
     * 0=仅一次，0和A中其他任何数字互斥
     * 12345=周一周二周三周四周五（工作日）重复
     */
    private int frequency;
    /**
     * 1:有效  0：无效
     **/
    private int valid;
    private String ring;
    private String path;
    private Date created;
    private String item;        //闹钟类型
    private boolean repeat;     //是否重复
    private Date rdate;

    //以下与服务器同步标记
    private String sid;        //服务器记录id,该记录的唯一标示
    private long timestamp;    //时间戳，记录的时效性标记
    private int recyle;     //0：有效， 1：回收
    private boolean synced = true;  //false表示未同步，待更新

    public AlarmClock() {
    }

    public AlarmClock(int rtime, int frequency) {
        this.rtime = rtime;
        this.frequency = frequency;
        this.created = new Date(System.currentTimeMillis());
    }

    @Generated(hash = 1309460463)
    public AlarmClock(Long id, int rtime, int frequency, int valid, String ring,
            String path, Date created, String item, boolean repeat, Date rdate,
            String sid, long timestamp, int recyle, boolean synced) {
        this.id = id;
        this.rtime = rtime;
        this.frequency = frequency;
        this.valid = valid;
        this.ring = ring;
        this.path = path;
        this.created = created;
        this.item = item;
        this.repeat = repeat;
        this.rdate = rdate;
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

    public int getRtime() {
        return this.rtime;
    }

    public void setRtime(int rtime) {
        this.rtime = rtime;
    }

    public int getFrequency() {
        return repeat ? frequency : 0;
    }
    public int getFrequency(boolean flag) {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public int getValid() {
        return this.valid;
    }

    public void setValid(int valid) {
        this.valid = valid;
    }

    public String getRing() {
        return this.ring;
    }

    public void setRing(String ring) {
        this.ring = ring;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Date getCreated() {
        return this.created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getItem() {
        return this.item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public boolean getRepeat() {
        return this.repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public Date getRdate() {
        return this.rdate;
    }

    public void setRdate(Date rdate) {
        this.rdate = rdate;
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
