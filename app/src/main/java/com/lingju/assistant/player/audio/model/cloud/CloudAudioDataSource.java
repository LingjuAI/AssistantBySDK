package com.lingju.assistant.player.audio.model.cloud;

import com.lingju.assistant.player.audio.IBatchPlayer;
import com.lingju.assistant.player.audio.model.AudioDataSource;
import com.lingju.model.PlayMusic;
import com.lingju.util.MusicUtils;
import com.lingju.util.PlayList;

import org.greenrobot.greendao.Property;

import java.util.List;

/**
 * Created by Administrator on 2016/11/18.
 */
public class CloudAudioDataSource implements AudioDataSource {

    private final PlayList<PlayMusic> pushList = new PlayList<>();
    private final PlayList<PlayMusic> requestList = new PlayList<>();

    public CloudAudioDataSource() {

    }

    public void getPushListFromClod() {

    }

    public void resetRquestList(List<PlayMusic> list) {
        requestList.clear();
        requestList.addAll(list);
    }

    @Override
    public void delete(PlayMusic music) {
        if (music.getFavorite()) {
            //TODO 移除云端的收藏歌曲
        }
    }

    @Override
    public void save(PlayMusic music) {
        if (music.getFavorite()) {
            //TODO 发送到云端收藏列表
        }
    }

    @Override
    public void insert(PlayMusic music) {
        if (music.getFavorite()) {
            //TODO 发送到云端收藏列表
        }
    }

    @Override
    public void update(PlayMusic music) {
        //do nothing
    }

    @Override
    public PlayMusic find(String musicId) {
        PlayMusic r = findRequestList(musicId);
        return r == null ? findPushList(musicId) : r;
    }

    public PlayMusic findRequestList(String musicId) {
        for (PlayMusic m : requestList) {
            if (musicId.equals(m.getMusicid()))
                return m;
        }
        return null;
    }

    public PlayMusic findPushList(String musicId) {
        for (PlayMusic m : pushList) {
            if (musicId.equals(m.getMusicid()))
                return m;
        }
        return null;
    }

    public PlayMusic findRequestMusic(String title) {
        for (PlayMusic m : requestList) {
            if (title.equals(m.getTitle()))
                return m;
        }
        return null;
    }

    public PlayMusic findPushMusic(String title) {
        for (PlayMusic m : pushList) {
            if (title.equals(m.getTitle()))
                return m;
        }
        return null;
    }

    public PlayMusic findRequestList(String name, String singer) {
        for (PlayMusic m : requestList) {
            if (name.equals(m.getTitle()) && singer.equals(m.getSinger()))
                return m;
        }
        return null;
    }

    public PlayMusic findPushList(String name, String singer) {
        for (PlayMusic m : pushList) {
            if (name.equals(m.getTitle()) && singer.equals(m.getSinger()))
                return m;
        }
        return null;
    }

    @Override
    public PlayMusic find(String name, String singer) {
        PlayMusic r = findRequestList(name, singer);
        return r == null ? findPushList(name, singer) : r;
    }

    @Override
    public PlayMusic findByName(String name) {
        PlayMusic r = findRequestMusic(name);
        return r == null ? findPushMusic(name) : r;
    }

    @Override
    public PlayMusic find(long id) {
        for (PlayMusic m : pushList) {
            if (id == m.getId())
                return m;
        }
        for (PlayMusic m : requestList) {
            if (id == m.getId())
                return m;
        }
        return null;
    }

    @Override
    public int getCount(int playListType) {
        if (playListType == IBatchPlayer.PlayListType.PUSH)
            return pushList.size();
        else if (playListType == IBatchPlayer.PlayListType.REQUEST)
            return requestList.size();
        return 0;
    }

    @Deprecated
    @Override
    public PlayList<PlayMusic> findByListType(int playListType) {
        return null;
    }

    public PlayList<PlayMusic> getPushList() {
        return pushList;
    }

    public PlayList<PlayMusic> getRequestList() {
        return requestList;
    }

    @Override
    public void addFavorites(PlayMusic music) {
        //TODO 向云端添加收藏歌曲
    }

    @Override
    public void removeFavorites(PlayMusic music) {
        //TODO 向云端请求移除收藏歌曲
    }

    @Override
    public int getMaxIntField(Property property) {
        return 0;
    }

    @Override
    public String getUrl(String musicId) {
        return MusicUtils.getMusicOnlineUri(musicId);
    }

    @Override
    public String getLyric(String musicId) {
        return MusicUtils.queryByMusicid(musicId);
    }

    @Override
    public String getLyric(String title, String singer) {
        return MusicUtils.searchLyric(title, singer);
    }

}
