package com.lingju.assistant.activity.event;

/**
 * Created by Administrator on 2016/5/6.
 */
public class VersionUpdateEvent {

    private int percent;
    private boolean updateApk;

    public VersionUpdateEvent(int percent,boolean updateApk) {
        this.percent = percent;
        this.updateApk=updateApk;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public void setUpdateApk(boolean updateApk) {
        this.updateApk = updateApk;
    }

    public boolean isUpdateApk() {
        return updateApk;
    }
}
