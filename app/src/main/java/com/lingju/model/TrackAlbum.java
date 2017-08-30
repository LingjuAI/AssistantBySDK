package com.lingju.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

import java.util.Date;

/**
 * Created by Ken on 2017/6/7.
 */
@Entity
public class TrackAlbum {
    @Id
    private long id;                //专辑id
    private String albumTitle;      //专辑名称
    private String albumPicUrl;     //专辑图片地址
    private long trackId;           //声音id
    private String trackTitle;      //声音名称
    private String trackPicUrl;     //声音图片地址
    private String trackUrl;        //声音播放地址
    private int orderNum;           //声音在专辑中的位置（从0开始）
    private String announcer;       //主播名称
    private int duration;           //声音时长
    private int breakPos;           //上一次中断播放时间点（与声音时长同义，非Date类型时间；用于查找播放记录）
    private Date breakTime;         //保存播放记录的时间
    private Date updateTime;        //最新一条声音更新时间（用于比较该专辑是否有更新）
    private Date subscribeTime;     //订阅时间（用于查找订阅专辑）
    private String keyword;         //搜索关键词（用于记录搜索历史）
    private Date searchTime;        //搜索时间
    @Generated(hash = 1391814952)
    public TrackAlbum(long id, String albumTitle, String albumPicUrl, long trackId,
            String trackTitle, String trackPicUrl, String trackUrl, int orderNum,
            String announcer, int duration, int breakPos, Date breakTime,
            Date updateTime, Date subscribeTime, String keyword, Date searchTime) {
        this.id = id;
        this.albumTitle = albumTitle;
        this.albumPicUrl = albumPicUrl;
        this.trackId = trackId;
        this.trackTitle = trackTitle;
        this.trackPicUrl = trackPicUrl;
        this.trackUrl = trackUrl;
        this.orderNum = orderNum;
        this.announcer = announcer;
        this.duration = duration;
        this.breakPos = breakPos;
        this.breakTime = breakTime;
        this.updateTime = updateTime;
        this.subscribeTime = subscribeTime;
        this.keyword = keyword;
        this.searchTime = searchTime;
    }
    @Generated(hash = 1128628639)
    public TrackAlbum() {
    }
    public long getId() {
        return this.id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getAlbumTitle() {
        return this.albumTitle;
    }
    public void setAlbumTitle(String albumTitle) {
        this.albumTitle = albumTitle;
    }
    public String getAlbumPicUrl() {
        return this.albumPicUrl;
    }
    public void setAlbumPicUrl(String albumPicUrl) {
        this.albumPicUrl = albumPicUrl;
    }
    public long getTrackId() {
        return this.trackId;
    }
    public void setTrackId(long trackId) {
        this.trackId = trackId;
    }
    public String getTrackTitle() {
        return this.trackTitle;
    }
    public void setTrackTitle(String trackTitle) {
        this.trackTitle = trackTitle;
    }
    public String getTrackPicUrl() {
        return this.trackPicUrl;
    }
    public void setTrackPicUrl(String trackPicUrl) {
        this.trackPicUrl = trackPicUrl;
    }
    public String getTrackUrl() {
        return this.trackUrl;
    }
    public void setTrackUrl(String trackUrl) {
        this.trackUrl = trackUrl;
    }
    public int getOrderNum() {
        return this.orderNum;
    }
    public void setOrderNum(int orderNum) {
        this.orderNum = orderNum;
    }
    public String getAnnouncer() {
        return this.announcer;
    }
    public void setAnnouncer(String announcer) {
        this.announcer = announcer;
    }
    public int getDuration() {
        return this.duration;
    }
    public void setDuration(int duration) {
        this.duration = duration;
    }
    public int getBreakPos() {
        return this.breakPos;
    }
    public void setBreakPos(int breakPos) {
        this.breakPos = breakPos;
    }
    public Date getBreakTime() {
        return this.breakTime;
    }
    public void setBreakTime(Date breakTime) {
        this.breakTime = breakTime;
    }
    public Date getUpdateTime() {
        return this.updateTime;
    }
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
    public Date getSubscribeTime() {
        return this.subscribeTime;
    }
    public void setSubscribeTime(Date subscribeTime) {
        this.subscribeTime = subscribeTime;
    }
    public String getKeyword() {
        return this.keyword;
    }
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    public Date getSearchTime() {
        return this.searchTime;
    }
    public void setSearchTime(Date searchTime) {
        this.searchTime = searchTime;
    }
   

}
