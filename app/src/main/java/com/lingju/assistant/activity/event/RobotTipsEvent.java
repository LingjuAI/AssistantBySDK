package com.lingju.assistant.activity.event;

public class RobotTipsEvent {
	private String text;

	public RobotTipsEvent(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}