package com.lingju.assistant.service;

import android.util.Log;

import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.lingju.common.adapter.LocationAdapter;
import com.lingju.lbsmodule.location.Address;

/**
 * Created by Ken on 2017/5/9.<br/>
 */
public class LocationAccessAdapter extends LocationAdapter {

    private Address mAddress;

    public LocationAccessAdapter(Address address) {
        setAddress(address);
    }

    public void setAddress(Address address) {
        if (address != null) {
            mAddress = address.clone();
            BDLocation bdLocation = new BDLocation();
            bdLocation.setLatitude(mAddress.getLatitude());
            bdLocation.setLongitude(mAddress.getLongitude());
            bdLocation = LocationClient.getBDLocationInCoorType(bdLocation, BDLocation.BDLOCATION_BD09LL_TO_GCJ02);
            mAddress.setLatitude(bdLocation.getLatitude());
            mAddress.setLongitude(bdLocation.getLongitude());
        }
        Log.i("LingJu", "LocationAccessAdapter setAddress() 最终位置：>> " + mAddress);
    }

    @Override
    public double getCurLng() {
        return mAddress == null ? 113.365471 : mAddress.getLongitude();
    }

    @Override
    public double getCurLat() {
        return mAddress == null ? 23.095618 : mAddress.getLatitude();
    }

    @Override
    public String getCurCity() {
        return mAddress == null ? "广州市" : mAddress.getCity();
    }

    @Override
    public String getCurAddressDetail() {
        return mAddress == null ? "中国广东省广州市海珠区凤浦中路" : mAddress.getAddressDetail();
    }
}
