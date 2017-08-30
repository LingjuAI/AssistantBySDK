package com.lingju.assistant.player.event;

public class PlayStateUpdateEvent {
	private boolean play=false;
	public PlayStateUpdateEvent() {
		this.play=false;
	}
	public PlayStateUpdateEvent(boolean play) {
		this.play=play;
	}
	
	public boolean isPlay() {
		return play;
	}
	

}
