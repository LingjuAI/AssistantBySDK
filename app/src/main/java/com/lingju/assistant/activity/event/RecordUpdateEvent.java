package com.lingju.assistant.activity.event;

public class RecordUpdateEvent {
	
	public final static int RECORD_IDLE=0;
	public final static int RECORDING=1;
	public final static int RECOGNIZING=2;
	public final static int RECORD_IDLE_AFTER_RECOGNIZED=3;

	
	private int state;

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	private int type;

	public RecordUpdateEvent(int state) {
		this.state=state;
	}
	public RecordUpdateEvent(int state,int type) {
		this.state=state;
		this.type = type;
	}
	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}
	
	

}
