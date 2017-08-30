package com.lingju.assistant.player.event;

public class UpdatePlayBarEvent {
    private int currentTime;
    private int totalTime;
    
	public UpdatePlayBarEvent(int ct,int tt) {
		this.currentTime=ct;
		this.totalTime=tt;
	}

	public int getCurrentTime() {
		return currentTime;
	}

	public void setCurrentTime(int currentTime) {
		this.currentTime = currentTime;
	}

	public int getTotalTime() {
		return totalTime;
	}

	public void setTotalTime(int totalTime) {
		this.totalTime = totalTime;
	}
	
	

}
