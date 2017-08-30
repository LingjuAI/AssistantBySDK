package com.lingju.assistant.player.event;

public class UpdateWaittingSeekBarEvent {

    private boolean isPhone = true;

    public UpdateWaittingSeekBarEvent(boolean isPhone) {
        this.isPhone = isPhone;
    }

    public boolean isPhone() {
        return isPhone;
    }

}
