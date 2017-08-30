package com.lingju.assistant.activity.event;

/**
 * Created by Ken on 2017/3/15.
 */
public class VolumeChangedEvent {
    private int volume;

    public VolumeChangedEvent(int volume) {
        this.volume = volume;
    }

    public int getVolume() {
        return volume;
    }
}
