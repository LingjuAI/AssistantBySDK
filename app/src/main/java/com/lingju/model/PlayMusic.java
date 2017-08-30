package com.lingju.model;


import com.lingju.context.entity.AudioEntity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Unique;

import java.util.Date;


@Entity(nameInDb = "l_audio_entity")
public class PlayMusic {
    @Id(autoincrement = true)
    private Long id;// INTEGER primary key autoincrement,
    private String lyirc;// TEXT default null,
    @Unique
    private String musicid;// TEXT not null default '',
    private String uri = "";// TEXT not null default '',
    private String title;// TEXT not null default '',
    private String singer;// TEXT not null default '',
    private String album;// TEXT not null default '',
    private String type;// TEXT default null,
    private int duration = 0;// INTEGER not null default 0,
    private int size;// REAL not null default 0,
    private boolean cloud = false;// INTEGER not null default 0,
    private boolean fetched = false;// INTEGER NOT NULL default 0,
    private int pushGroupId = 0;// INTEGER not null default 0;
    private int requestGroupId = 0;// INTEGER not null default 0;
    private boolean synchronize = false;
    private boolean favorite = false;// INTEGER NOT NULL default 0,
    private boolean formated = false;// INTEGER NOT NULL default 0,
    private Date favoritedTime;// datetime default null,
    private Date created;// Date not nullS

    public PlayMusic() {
    }

    public PlayMusic(AudioEntity ae, int requestGroupId) {
        this.musicid = ae.getMusicId();
        this.title = ae.getName();
        this.album = ae.getAlbum();
        this.cloud = true;
        this.requestGroupId = requestGroupId;
        this.created = new Date(System.currentTimeMillis());
        String[] singers = ae.getSinger();
        if (singers != null) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < singers.length; i++) {
                builder.append(singers[i]).append("，");
            }
            builder.setLength(builder.length() - 1);
            this.singer = builder.toString();
        }
    }

    @Generated(hash = 470504678)
    public PlayMusic(Long id, String lyirc, String musicid, String uri,
                     String title, String singer, String album, String type, int duration,
                     int size, boolean cloud, boolean fetched, int pushGroupId,
                     int requestGroupId, boolean synchronize, boolean favorite,
                     boolean formated, Date favoritedTime, Date created) {
        this.id = id;
        this.lyirc = lyirc;
        this.musicid = musicid;
        this.uri = uri;
        this.title = title;
        this.singer = singer;
        this.album = album;
        this.type = type;
        this.duration = duration;
        this.size = size;
        this.cloud = cloud;
        this.fetched = fetched;
        this.pushGroupId = pushGroupId;
        this.requestGroupId = requestGroupId;
        this.synchronize = synchronize;
        this.favorite = favorite;
        this.formated = formated;
        this.favoritedTime = favoritedTime;
        this.created = created;
    }


    public Long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLyirc() {
        return lyirc;
    }

    public void setLyirc(String lyirc) {
        this.lyirc = lyirc;
    }

    public String getMusicid() {
        return musicid;
    }

    public void setMusicid(String musicid) {
        this.musicid = musicid;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSinger() {
        return singer;
    }

    public void setSinger(String singer) {
        this.singer = singer;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isCloud() {
        return cloud;
    }

    public void setCloud(boolean cloud) {
        this.cloud = cloud;
    }

    public boolean isFetched() {
        return fetched;
    }

    public void setFetched(boolean fetched) {
        this.fetched = fetched;
    }

    public int getPushGroupId() {
        return pushGroupId;
    }

    public void setPushGroupId(int pushGroupId) {
        this.pushGroupId = pushGroupId;
    }

    public int getRequestGroupId() {
        return requestGroupId;
    }

    public void setRequestGroupId(int requestGroupId) {
        this.requestGroupId = requestGroupId;
    }

    public boolean isSynchronize() {
        return synchronize;
    }

    public void setSynchronize(boolean synchronize) {
        this.synchronize = synchronize;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public boolean isFormated() {
        return formated;
    }

    public void setFormated(boolean formated) {
        this.formated = formated;
    }

    public Date getFavoritedTime() {
        return favoritedTime;
    }

    public void setFavoritedTime(Date favoritedTime) {
        this.favoritedTime = favoritedTime;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public boolean getCloud() {
        return this.cloud;
    }

    public boolean getFetched() {
        return this.fetched;
    }

    public boolean getSynchronize() {
        return this.synchronize;
    }

    public boolean getFavorite() {
        return this.favorite;
    }

    public boolean getFormated() {
        return this.formated;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
