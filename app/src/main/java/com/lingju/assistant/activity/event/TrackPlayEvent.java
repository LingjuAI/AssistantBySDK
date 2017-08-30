package com.lingju.assistant.activity.event;

import com.ximalaya.ting.android.opensdk.model.PlayableModel;

/**
 * Created by Ken on 2017/6/13.
 */
public class TrackPlayEvent {
    private boolean isPlaying;
    private PlayableModel playModel;

    public TrackPlayEvent(boolean isPlaying, PlayableModel playModel) {
        this.isPlaying = isPlaying;
        this.playModel = playModel;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public PlayableModel getPlayTrack() {
        return playModel;
    }

    public void setPlayTrack(PlayableModel playModel) {
        this.playModel = playModel;
    }
}
