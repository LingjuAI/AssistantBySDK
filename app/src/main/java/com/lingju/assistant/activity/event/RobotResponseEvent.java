package com.lingju.assistant.activity.event;


import com.lingju.context.entity.Command;

public class RobotResponseEvent {
	public final static int TYPE_DEFAULT=-1;
	public final static int TYPE_NO_SYNTHESIZE=-2;
	public final static int TYPE_APPEND_SYNTHESIZE=0;

	private String text;
	private Command cmd;
	private int type;
	
	public RobotResponseEvent(String text, int type) {
		this(text,null,type);
	}

	public RobotResponseEvent(String text,Command cmd, int type) {
		this.text = text;
		this.cmd = cmd;
		this.type = type;
	}
	
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}

	public Command getCmd() {
		return cmd;
	}

	public void setCmd(Command cmd) {
		this.cmd = cmd;
	}
}
