package com.lingju.assistant.activity.event;


import com.lingju.model.BaiduAddress;

import java.util.ArrayList;

/**
 * Created by Administrator on 2015/8/6.
 * <p/>
 * 导航路线计算事件
 */
public class NaviRouteCalculateEvent {
    /**
     * 包含完整的规划路线的点集合，规划完倒数10秒开始
     */
    public final static int FULL = 0;
    /**
     * 包含单个点，并且这个点是途径地点
     */
    public final static int SINGLE_PASS = 1;
    public final static int SINGLE_TARGET = 2;
    /**
     * 完整的的规划路线的点集合，并且{@linkplain NaviRouteCalculateEvent#passPoint passPoint}的值不能为空（必须经过该点）
     */
    public final static int FULL_CONTAINS_PASS_POINT = 4;
    /**
     * 完整的的规划路线的点集合，并且{@linkplain NaviRouteCalculateEvent#passPoint passPoint}的值不能为空（必须经过该点）
     */
    public final static int FULL_CONTAINS_EXISTENT_PASS_POINT = 8;
    /**
     * 完整的的规划路线的点集合，并且{@linkplain NaviRouteCalculateEvent#passPoint passPoint}的值不能为空（不能经过该点）
     */
    public final static int FULL_CONTAINS_NOT_PASS_POINT = 16;
    /**
     * 完整的的规划路线的点集合，并且{@linkplain NaviRouteCalculateEvent#calculateMode calculateMode}的值不能为空
     */
    public final static int FULL_WITH_CALCULATE_MODE = 32;
    /**
     * 完整的的规划路线的点集合,路线规划完成后询问是否需要导航到目的地
     */
    public final static int FULL_CHECK_ROUTE_LINE_FOR_NAVIGATE = 64;
    /**
     * 在导航中添加途经点，{@linkplain NaviRouteCalculateEvent#passPoint passPoint}的值不能为空（经过该点）
     */
    public final static int RECALCULATE_ADD_POINT_IN_NAVIGATE = 128;
    /**
     * 完整的的规划路线的点集合,目的地为收藏的地点
     */
    public final static int TARGET_IS_FAVORITE_POINT = 256;


    private ArrayList<BaiduAddress> points;
    private int calculateMode;
    private String passPoint;
    /**
     * 取值：{@linkplain NaviRouteCalculateEvent#FULL FULL},{@linkplain NaviRouteCalculateEvent#FULL_CONTAINS_NOT_PASS_POINT FULL_CONTAINS_NOT_PASS_POINT},{@linkplain NaviRouteCalculateEvent#FULL_CONTAINS_PASS_POINT FULL_CONTAINS_PASS_POINT},
     * {@linkplain NaviRouteCalculateEvent#FULL_WITH_CALCULATE_MODE FULL_WITH_CALCULATE_MODE},{@linkplain NaviRouteCalculateEvent#FULL_CHECK_ROUTE_LINE_FOR_NAVIGATE FULL_CHECK_ROUTE_LINE_FOR_NAVIGATE},
     * 默认值=FULL
     */
    private int type = FULL;

    public NaviRouteCalculateEvent(){}

    public NaviRouteCalculateEvent(ArrayList<BaiduAddress> points) {
        this.points = points;
    }

    public NaviRouteCalculateEvent(BaiduAddress point, int type) {
        this.points = new ArrayList<BaiduAddress>();
        this.points.add(point);
        this.type = type;
    }

    public NaviRouteCalculateEvent(String passPoint, int type) {
        this.passPoint = passPoint;
        this.type = type;
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

    public int getCalculateMode() {
        return calculateMode;
    }

    public void setCalculateMode(int calculateMode) {
        this.calculateMode = calculateMode;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getPassPoint() {
        return passPoint;
    }

    public void setPassPoint(String passPoint) {
        this.passPoint = passPoint;
    }
}
