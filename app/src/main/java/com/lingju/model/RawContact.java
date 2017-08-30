package com.lingju.model;

public class RawContact{
	
	private int id;
	private String displayName;
	private String number;
	private long lastContacted;
	private int timesContacted;
    private int version=0;
    
    public RawContact(){
    	
    }
    
	public RawContact(int id, String number,String displayName,
			long lastContacted, int timesContacted, int version) {
		this.id = id;
		this.number=number;
		this.displayName = displayName;
		this.lastContacted = lastContacted;
		this.timesContacted = timesContacted;
		this.version = version;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public void setNumber(String number) {
		this.number = number;
	}
	
	public String getNumber() {
		return number;
	}


	public String getDisplayName() {
		return displayName;
	}



	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}



	public long getLastContacted() {
		return lastContacted;
	}



	public void setLastContacted(long lastContacted) {
		this.lastContacted = lastContacted;
	}



	public int getTimesContacted() {
		return timesContacted;
	}



	public void setTimesContacted(int timesContacted) {
		this.timesContacted = timesContacted;
	}



	public int getVersion() {
		return version;
	}



	public void setVersion(int version) {
		this.version = version;
	}
}
