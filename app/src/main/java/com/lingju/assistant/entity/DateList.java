package com.lingju.assistant.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

public class DateList<T> extends Observable {
	private String date;
	private List<T> list;
	
	public DateList() {
	}
	
	public DateList(String date) {
		this.date=date;
		this.list=new ArrayList<T>();
	}
	
	public DateList(List<T> list, String date) {
		this.list = list;
		this.date=date;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public List<T> getList() {
		return list;
	}

	public void setList(List<T> list) {
		this.list = list;
	}
	
	public void notifyDatasetChanged(){
		setChanged();
		notifyObservers();
	}

}
