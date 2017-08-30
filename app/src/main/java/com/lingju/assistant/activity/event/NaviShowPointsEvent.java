package com.lingju.assistant.activity.event;


import com.lingju.model.BaiduAddress;

import java.util.ArrayList;

/**
 * Created by Administrator on 2015/8/6.
 */
public class NaviShowPointsEvent {
    public final static int DESTINATION=0;
    public final static int ADD_POINT=1;
    public final static int CLOSE_ACTIVITY=2;
    public final static int CANCEL_TASK=3;


    private ArrayList<BaiduAddress> points;
    private int type=DESTINATION;

    public NaviShowPointsEvent(ArrayList<BaiduAddress> points) {
        this.points = points;
    }

    public NaviShowPointsEvent(ArrayList<BaiduAddress> points, int type) {
        this.points = points;
        this.type=type;
    }

    public ArrayList<BaiduAddress> getPoints() {
        return points;
    }

    public void setPoints(ArrayList<BaiduAddress> points) {
        this.points = points;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}

