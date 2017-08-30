package com.lingju.assistant.activity.event;

/**
 * Created by Administrator on 2015/9/11.
 */
public class SelectCityEvent {

    private String city;

    public SelectCityEvent(String city){
        this.city=city;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
