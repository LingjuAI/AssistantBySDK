package com.lingju.assistant.activity.event;

/**
 * Created by Dyy on 2017/navi/17.
 */
public class IntroduceShowEvent {
    boolean isIntroduceShow;

    public IntroduceShowEvent(boolean isIntroduceShow) {
        this.isIntroduceShow = isIntroduceShow;
    }

    public boolean getIntroduceShow() {
        return isIntroduceShow;
    }

    public void setIntroduceShow(boolean introduceShow) {
        isIntroduceShow = introduceShow;
    }
}
