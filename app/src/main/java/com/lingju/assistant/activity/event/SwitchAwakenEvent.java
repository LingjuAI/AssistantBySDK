package com.lingju.assistant.activity.event;

public class SwitchAwakenEvent {
	private boolean on;
	public SwitchAwakenEvent(boolean on) {
		this.on=on;
	}
	
	public void setOn(boolean on) {
		this.on = on;
	}
	
	public boolean isOn() {
		return on;
	}

}
