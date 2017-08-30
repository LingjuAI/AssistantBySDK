package com.lingju.assistant.activity.event;

import com.lingju.model.AlarmClock;

import java.util.List;

/**
 * Created by Ken on 2016/12/16.
 */
public class AlarmStateEvent {

    private List<AlarmClock> alarms;

    public AlarmStateEvent(List<AlarmClock> alarms) {
        this.alarms = alarms;
    }

    public List<AlarmClock> getAlarms() {
        return alarms;
    }
}
