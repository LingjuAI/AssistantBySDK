package com.lingju.assistant.player.event;


import com.lingju.model.PlayMusic;

public class InitSongEvent {
	private String singer;
	private String title;
	private int error;
	private int duration;
	
	public InitSongEvent(){}
	public InitSongEvent(PlayMusic song) {
		this.singer=song.getSinger();
		this.title=song.getTitle();
	}
	
	public InitSongEvent(String singer, String title){
		this.singer=singer;
		this.title=title;
	}
	
	public InitSongEvent(int error, int duration){
		this.error=error;
		this.duration=duration;
	}

	public int getError() {
		return error;
	}
	public void setError(int error) {
		this.error = error;
	}
	public int getDuration() {
		return duration;
	}
	public void setDuration(int duration) {
		this.duration = duration;
	}
	public String getSinger() {
		return singer;
	}

	public void setSinger(String singer) {
		this.singer = singer;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	

}
