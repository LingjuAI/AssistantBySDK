package com.lingju.assistant.entity;

import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.track.Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ken on 2017/6/22.
 */
public class TingAlbumMsg {
    private List<Album> albums = new ArrayList<>();
    private Track playTrack;
    private int episode;

    public TingAlbumMsg(List<Album> albums, int episode) {
        this.albums.addAll(albums);
        this.episode = episode;
    }

    public TingAlbumMsg(Album album, Track playTrack){
        this.albums.add(album);
        this.playTrack=playTrack;
    }

    public List<Album> getAlbums() {
        return albums;
    }

    public int getEpisode() {
        return episode;
    }

    public Track getPlayTrack() {
        return playTrack;
    }
}
