package com.lingju.assistant.service;

import android.app.Application;

import com.lingju.assistant.AppConfig;
import com.lingju.common.adapter.LocationAdapter;

/**
 * Created by Ken on 2017/5/9.
 */
public class LocationAccessAdapter extends LocationAdapter {

    private AppConfig appConfig;

    public LocationAccessAdapter(Application app) {
        this.appConfig = (AppConfig) app;
    }

    @Override
    public double getCurLng() {
        return appConfig.address==null? 113.37302:appConfig.address.getLongitude();
    }

    @Override
    public double getCurLat() {
        return appConfig.address==null? 23.10376:appConfig.address.getLatitude();
    }

    @Override
    public String getCurCity() {
        return appConfig.address==null?"广州市":appConfig.address.getCity();
    }

    @Override
    public String getCurAddressDetail() {
        return appConfig.address==null?"广州市海珠区新港东路1000号":appConfig.address.getAddressDetail();
    }
}
