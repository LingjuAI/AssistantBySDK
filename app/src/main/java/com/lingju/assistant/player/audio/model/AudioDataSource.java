package com.lingju.assistant.player.audio.model;

import com.lingju.model.PlayMusic;
import com.lingju.util.PlayList;

import org.greenrobot.greendao.Property;

/**
 * Created by Administrator on 2016/11/18.
 */
public interface AudioDataSource {

    public void delete(PlayMusic music);

    public void save(PlayMusic music);

    public void insert(PlayMusic music);

    public void update(PlayMusic music);

    public PlayMusic find(String musicId);

    public PlayMusic find(String name, String singer);

    public PlayMusic findByName(String name);

    public PlayMusic find(long id);

    public int getCount(int playListType);

    public PlayList<PlayMusic> findByListType(int playListType);

    public void addFavorites(PlayMusic music);

    public void removeFavorites(PlayMusic music);

    public int getMaxIntField(Property property);

    public String getUrl(String musicId);

    public String getLyric(String musicId);

    public String getLyric(String title, String singer);

}
