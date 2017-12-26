package com.lingju.assistant.service;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.baidu.navisdk.adapter.impl.RouteGuider;
import com.lingju.assistant.R;
import com.lingju.assistant.baidunavi.adapter.BaiduNaviSuperManager;
import com.lingju.audio.engine.base.SpeechMsg;
import com.lingju.audio.engine.base.SpeechMsgBuilder;
import com.lingju.common.log.Log;
import com.lingju.lbsmodule.entity.RoadConditionItem;
import com.lingju.lbsmodule.entity.RouteModel;
import com.lingju.lbsmodule.location.Address;
import com.lingju.lbsmodule.location.BaiduLocateManager;
import com.lingju.lbsmodule.location.LocateListener;
import com.lingju.lbsmodule.proxy.BNRoutePlanObserverProxy;
import com.lingju.lbsmodule.proxy.BNRoutePlanerProxy;
import com.lingju.lbsmodule.proxy.BNavigatorProxy;
import com.lingju.lbsmodule.proxy.RGAssistGuideModelProxy;
import com.lingju.lbsmodule.proxy.RGHighwayModelProxy;
import com.lingju.lbsmodule.proxy.RoutePlanModelProxy;
import com.lingju.lbsmodule.proxy.RoutePlanResultItemProxy;
import com.lingju.model.BaiduAddress;
import com.lingju.model.dao.BaiduNaviDao;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Administrator on 2015/8/26.
 */
public class NavigatorService/* extends NavigationDao*/ {
    private final static String TAG = "NavigatorService";
    private int avoidTrafficLine;
    private int selectPassPointLine;
    private Context context;
    private Address bdAddress;
    private Address gcAddress;
    private ThreadPoolExecutor tPools;
    private final static String SYSTEM_ERROR = "系统出错啦！";
    private final static String TRAFFIC_Straightway = "路况顺畅";
    private final static int COMPENSATION = 50;
    private long lastCalculateTime;

    public static String LastAvoidPoint = "";
    //private static ArrayList<BNRoutePlanNode> LastRouteInputs=new ArrayList<BNRoutePlanNode>();
    private BaiduAddress lastHomeAddress;
    private BaiduAddress lastCompanyAddress;
    private CalculateRouteListener calculateRouteListener;

    private static NavigatorService instance;
    private final ArrayList<BNRoutePlanNode> routeInputs = new ArrayList<BNRoutePlanNode>();
    private BaiduNaviDao addressDao;
    private SpeechMsgBuilder mMsgBuilder;
    private String pushText;
    //private static String PassPoint;
    //private static BNRoutePlanNode PassPointNode;


    private NavigatorService(BaiduNaviDao addressDao, CalculateRouteListener calculateRouteListener, Context context) {
        this.addressDao = addressDao;
        this.context = context;
        this.calculateRouteListener = calculateRouteListener;
        tPools = new ThreadPoolExecutor(3, 5, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        BaiduLocateManager.get().addObserver(locateGCListener);
        BaiduLocateManager.get().addObserver(locateBDListener);
    }

    /*public static void setPassPoint(String passPoint,BNRoutePlanNode passPointNode) {
        if(!TextUtils.isEmpty(passPoint))
        NavigatorService.PassPoint = passPoint;
        if(passPointNode!=null){
            NavigatorService.PassPointNode=passPointNode;
            if(TextUtils.isEmpty(NavigatorService.PassPoint))
                NavigatorService.PassPoint = passPointNode.getName();
        }
    }

    public static void resetPassPoint(){
        PassPoint=null;
        PassPointNode=null;
    }

    public static String getPassPoint() {
        return PassPoint;
    }

    public static BNRoutePlanNode getPassPointNode() {
        return PassPointNode;
    }*/

    static synchronized NavigatorService createInstance(BaiduNaviDao addressDao, CalculateRouteListener calculateRouteListener, Context context) {
        if (instance == null) {
            instance = new NavigatorService(addressDao, calculateRouteListener, context);
        }
        return instance;
    }

    public static NavigatorService get() {
        if (instance == null)
            throw new NullPointerException("NavigatorService had not inited");
        return instance;
    }

    public int getSelectPassPointLine() {
        return selectPassPointLine;
    }

    public void setSelectPassPointLine(int selectPassPointLine) {
        this.selectPassPointLine = selectPassPointLine;
    }


    public long getLastCalculateTime() {
        return lastCalculateTime;
    }

    public int getAvoidTrafficLine() {
        return avoidTrafficLine;
    }

    public void setAvoidTrafficLine(int avoidTrafficLine) {
        this.avoidTrafficLine = avoidTrafficLine;
    }

   /* @Override
    public Map<String, String> getCollectPlaces() {
        Log.i(TAG, "getCollectPlaces>>");
        List<BaiduAddress> list = new ArrayList<>();
        addressDao.getFavorList(list, -1);
        Map<String, String> map = new Hashtable<>();
        for (BaiduAddress ad : list) {
            map.put(ad.getName(), ad.toWebPoiJson().toString());
        }
        lastHomeAddress = addressDao.getHomeOrCompanyAddress(context.getResources().getString(R.string.home));
        if (lastHomeAddress != null) {
            map.put(lastHomeAddress.getRemark(), lastHomeAddress.toWebPoiJson().toString());
        }
        lastCompanyAddress = addressDao.getHomeOrCompanyAddress(context.getResources().getString(R.string.company));
        if (lastCompanyAddress != null) {
            map.put(lastCompanyAddress.getRemark(), lastCompanyAddress.toWebPoiJson().toString());
            map.put("公司", lastCompanyAddress.toWebPoiJson().toString());
        }
        return map;
    }

    *//**
     * 判断当前导航路线中是否经过该途经点
     *
     * @param point 途经点
     * @return 0：有经过途经点，1：未经过途经点，2：有经过途经点而且无法避开
     *//*
    @Override
    public int isContainPoint(String point) {
        Log.i(TAG, "isContainPoint>>" + point);
        if ((BaiduNaviSuperManager.isNaviInited() && BaiduNaviSuperManager.isSelectLineState()) ||
                BNavigatorProxy.getInstance().isNaviBegin()) {
            return passPoint(point);
        }
        return -1;
    }

    */

    /**
     * 当前路线是否躲避拥堵
     *
     * @return true：躲避拥堵，false：没有躲避拥堵
     */
    public boolean isCongestion() {
        int calculatePreference = BNRoutePlanerProxy.getInstance().getCalcPreference();
        Log.i(TAG, "isCongestion>> " + calculatePreference);
        if (calculatePreference == BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND) {
            return true;
        }
        return (calculatePreference & BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM) == BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM;
    }

    public boolean isPlan() {
        Log.i(TAG, "isPlan>>" + Boolean.toString(BaiduNaviSuperManager.isNaviInited() && BaiduNaviSuperManager.isSelectLineState()));
        return BaiduNaviSuperManager.isNaviInited() && BaiduNaviSuperManager.isSelectLineState();
    }

    /**
     * 当前位置是否在某点附近
     *
     * @param point 0：家，1：公司
     * @return 在附近：true，不在附近：false
     *//*
    @Override
    public boolean isNearPoint(int point) {
        if (point == 0) {
            lastHomeAddress = lastHomeAddress == null ? addressDao.getHomeOrCompanyAddress(context.getResources().getString(R.string.home)) : lastHomeAddress;
            if (lastHomeAddress != null) {
                return DistanceUtil.getDistance(new LatLng(bdAddress.getLatitude(), bdAddress.getLongitude()),
                        new LatLng(lastHomeAddress.getLatitude(), lastHomeAddress.getLongitude())) < 1000;
            }
        } else if (point == 1) {
            lastCompanyAddress = lastCompanyAddress == null ? addressDao.getHomeOrCompanyAddress(context.getResources().getString(R.string.home)) : lastCompanyAddress;
            if (lastCompanyAddress != null) {
                return DistanceUtil.getDistance(new LatLng(bdAddress.getLatitude(), bdAddress.getLongitude()),
                        new LatLng(lastCompanyAddress.getLatitude(), lastCompanyAddress.getLongitude())) < 1000;

            }
        }
        return true;
    }

    @Override
    public double getDistance(double lat1, double lng1, double lat2, double lng2) {
        Log.i(TAG, lat1 + "," + lng1 + "," + lat2 + "," + lng2);
        return ((double) (((int) DistanceUtil.getDistance(new LatLng(lat1, lng1), new LatLng(lat2, lng2))) / 10) * 10) / 1000;
    }

    *//**
     * 获取所有途经点的名称
     *
     * @return 没有则返回空数组{""}
     *//*
    @Override
    public String[] getPointsName() {
        if (routeInputs.size() > 2) {
            String result[] = new String[routeInputs.size() - 2];
            for (int i = 1; i < routeInputs.size() - 1; ++i) {
                result[i - 1] = routeInputs.get(i).getName();
            }
            return result;
        }
        return new String[0];
    }


    */

    /**
     * 判断当前路线与传入路线是否一致
     * <p/>
     * type 0：收费最少（不走高速），1：路程最短，2：时间最短（高速优先）
     *
     * @return
     *//*
    @Override
    public boolean isPlanLoad(int type) {
        RouteModel routeModel = RouteModel.getCurrent();
        switch (type) {
            case 0:
                if ((BNRoutePlanerProxy.getInstance().getCalcPreference() & BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TOLL)
                        == BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TOLL ||
                        routeModel.getToll() == 0) {
                    return true;
                } else {
                    int toll = routeModel.getToll();
                    ArrayList<RouteModel> list;
                    for (Integer key : RouteModel.getLastRouteModels().keySet()) {
                        list = RouteModel.getLastRouteModels().get(key);
                        for (RouteModel rm : list) {
                            if (rm.getToll() < toll)
                                return false;
                        }
                    }
                    return true;
                }
            case 1:
                if ((BNRoutePlanerProxy.getInstance().getCalcPreference() & BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_DIST)
                        == BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_DIST) {
                    return true;
                } else {
                    double dist = routeModel.getDistance();
                    ArrayList<RouteModel> list;
                    for (Integer key : RouteModel.getLastRouteModels().keySet()) {
                        list = RouteModel.getLastRouteModels().get(key);
                        for (RouteModel rm : list) {
                            if (rm.getDistance() < dist)
                                return false;
                        }
                    }
                    return true;
                }
            case 2:
                if ((BNRoutePlanerProxy.getInstance().getCalcPreference() & BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TIME)
                        == BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TIME) {
                    return true;
                } else {
                    int time = routeModel.getTime();
                    ArrayList<RouteModel> list;
                    for (Integer key : RouteModel.getLastRouteModels().keySet()) {
                        list = RouteModel.getLastRouteModels().get(key);
                        for (RouteModel rm : list) {
                            if (rm.getTime() < time)
                                return false;
                        }
                    }
                    return true;
                }
        }
        return false;
    }

    @Override
    public String getTarget() {
        if (routeInputs.size() > 0) {
            BNRoutePlanNode bn = routeInputs.get(routeInputs.size() - 1);
            BaiduAddress bd = new BaiduAddress();
            bd.setLatitude(bn.getLatitude());
            bd.setLongitude(bn.getLongitude());
            bd.setName(bn.getName());
            bd.setAddress(bn.getDescription());
            bd.setBD09LL();
            return bd.toWebPoiJson().toString();
        }
        return null;
    }*/
    public boolean isNavigation() {
        Log.i(TAG, "isNavigation>>");
        return BaiduNaviSuperManager.isNaviInited() && BNavigatorProxy.getInstance().isNaviBegin();
    }

    public void resetRouteInputs(List<BNRoutePlanNode> list) {
        routeInputs.clear();
        routeInputs.addAll(list);
    }

    public void clearRouteInputs() {
        routeInputs.clear();
    }

    public void addPassPoint(BNRoutePlanNode node) {
        if (routeInputs.size() > 1) {
            for (int i = 0; i < routeInputs.size(); i++) {
                if (routeInputs.get(i).getName().indexOf(node.getName()) != -1) {
                    return;
                }
            }
            routeInputs.add(routeInputs.size() - 1, node);
        }
    }

    public void addPassPoint(String nodeName) {
        if (routeInputs.size() > 1) {
            for (int i = 0; i < routeInputs.size(); i++) {
                if (routeInputs.get(i).getName().contains(nodeName)) {
                    return;
                }
            }
            routeInputs.add(routeInputs.size() - 1, new BNRoutePlanNode(0D, 0D, nodeName, null));
        }
    }

    public void removePassPointHasCoordinate() {
        if (routeInputs.size() > 2) {
            for (int i = 0; i < routeInputs.size(); i++) {
                if (routeInputs.get(i).getLatitude() != 0) {
                    routeInputs.remove(i);
                    return;
                }
            }
        }
    }

    public void removePassPointWithoutCoordinate(String passPoint) {
        Log.i(TAG, "removePassPointWithoutCoordinate>>" + passPoint);
        if (routeInputs.size() > 2) {
            for (int i = 0; i < routeInputs.size(); i++) {
                if (routeInputs.get(i).getName().indexOf(passPoint) != -1) {
                    routeInputs.remove(i);
                    return;
                }
            }
        }
    }

    public List<BNRoutePlanNode> getRouteInputs() {
        return routeInputs;
    }

    public List<BNRoutePlanNode> getPassPointsHasCoordinate() {
        if (routeInputs.size() > 2) {
            List<BNRoutePlanNode> list = new ArrayList<BNRoutePlanNode>();
            for (int i = 1; i < routeInputs.size() - 1; ++i) {
                if (routeInputs.get(i).getLatitude() > 0) {
                    list.add(routeInputs.get(i));
                }
            }
            return list;
        }
        return null;
    }

    public List<BNRoutePlanNode> getRouteInputsHasCoordinate() {
        if (routeInputs.size() > 2) {
            List<BNRoutePlanNode> list = new ArrayList<BNRoutePlanNode>();
            for (int i = 0; i < routeInputs.size(); ++i) {
                if (routeInputs.get(i).getLatitude() > 0) {
                    list.add(routeInputs.get(i));
                }
            }
            return list;
        }
        return null;
    }

    public String getPassPointNoCoordinate() {
        for (int i = 1; i < routeInputs.size() - 1; ++i) {
            Log.i(TAG, "getPassPointNoCoordinate>>" + routeInputs.get(i).getName() + "," + routeInputs.get(i).getLatitude() + "," + routeInputs.get(i).getLongitude());
            if (routeInputs.get(i).getLatitude() == 0 && routeInputs.get(i).getLongitude() == 0) {
                return routeInputs.get(i).getName();
            }
        }
        return null;
    }

    public boolean routeInputsContains(String passPoint) {
        for (BNRoutePlanNode n : routeInputs) {
            if (n.getName().indexOf(passPoint) != -1) {
                return true;
            }
        }
        return false;
    }

    public String getPassPointString() {
        if (routeInputs.size() > 2) {
            StringBuilder sb = new StringBuilder(routeInputs.get(1).getName());
            for (int i = 2; i < routeInputs.size() - 1; ++i) {
                sb.append("和").append(routeInputs.get(i).getName());
            }
            return sb.toString();
        }
        return "";
    }

    /**
     * 判断某个途经点的路况
     *
     * @param type  0：途经点路况；1：前面路况；2：上班路况；3：回家路况；4：目的地路况；5，躲避途经点拥堵；6，躲避前方拥堵；7，规划路线上的路况
     * @param point 途经点
     * @return 直接返回路况信息
     * <p/>
     * 有上下文的地方返回的文本分别以下面几种方式结尾：
     * type=0:(1)。需要躲避拥堵吗	(2)。需要我躲避拥堵重新规划路线吗
     * type=1:。如果需要更改路线，请在10秒内告诉我需要
     * type=2或type=3:。如果需要导航，请对我说需要
     * type=5或type=6:(1)。你需要重新规划路线吗	(2)。需要我重新计算吗
     */
    public String pointTraffic(int type, String point, SpeechMsgBuilder smgBuilder) {
        Log.i(TAG, "pointTraffic>>type=" + type + ",point=" + point);
        mMsgBuilder = smgBuilder;
        pushText = "";
        int calculatePreference = BNRoutePlanerProxy.getInstance().getCalcPreference();
        switch (type) {
            case 0:
                if (BNavigatorProxy.getInstance().isNaviBegin()) {
                    return checkRoadConditionInNavigate(point);
                } else {
                    return checkRoadConditionToPointBeforeNavigate(point);
                }
            case 1:
                return getRemainRoadCondition();
            case 2: {
                BaiduAddress companyAddress = addressDao.getHomeOrCompanyAddress(context.getResources().getString(R.string.company));
                if (BNavigatorProxy.getInstance().isNaviBegin()) {
                    StringBuilder sb = new StringBuilder();
                    RoutePlanModelProxy routePlanModel = RoutePlanModelProxy.getCacheRoutePlanModelProxy("RoutePlanModel");
                    if (routePlanModel != null && routePlanModel.getEndNode() != null) {
                        if (companyAddress != null && routePlanModel.getEndNode().getName().equals(companyAddress.getName())) {
                            sb.append("去公司的路上");
                            RouteModel routeModel = RouteModel.getCurrent();
                            routeModel.refreshRoadCondition();
                            Log.i(TAG, "conditionNodes.size=" + routeModel.getConditionNodes().size());
                            double curDistance = RGAssistGuideModelProxy.getInstance().getCarProgress() * routeModel.getDistance();
                            Queue<Integer> rQueue = new LinkedBlockingQueue<Integer>();
                            int conjestionCount = 0;
                            for (int i = 0; i < routeModel.getConditionNodes().size(); ++i) {
                                if (routeModel.getConditionNodes().get(i).getDistanceFromStart() > curDistance &&
                                        routeModel.getConditionNodes().get(i).getRoadCondition() >= RouteModel.ROAD_CONDITION_TYPE_Slow) {
                                    rQueue.offer(i);
                                    if (routeModel.getConditionNodes().get(i).getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Slow) {
                                        conjestionCount++;
                                    }
                                }
                            }
                            setTrafficState(sb, routeModel, rQueue, conjestionCount, (int) curDistance);
                            return sb.toString();
                        } else {
                            sb.append("我现在正在导航去").append(routePlanModel.getEndNode().getName()).append("，");
                        }

                    } else
                        sb.append("我现在在导航中，");
                    sb.append("暂时不方便为你播报其他路线的路况，如果需要你可以唤醒我后对我说：导航去公司。");
                    return sb.toString();
                } else {
                    if (companyAddress != null) {
                        double dt = DistanceUtil.getDistance(new LatLng(bdAddress.getLatitude(), bdAddress.getLongitude()), new LatLng(companyAddress.getLatitude(), companyAddress.getLongitude()));
                        Log.i(TAG, "dt===" + dt);
                        if (dt < 1000) {
                            return "您已在单位附近。";
                        }
                        BaiduAddress homeAddress = addressDao.getHomeOrCompanyAddress(context.getResources().getString(R.string.home));
                        boolean inHome = homeAddress != null;
                        if (inHome) {
                            inHome = DistanceUtil.getDistance(new LatLng(bdAddress.getLatitude(), bdAddress.getLongitude()),
                                    new LatLng(homeAddress.getLatitude(), homeAddress.getLongitude())) < 1000;
                        }

                        RouteModel routePlanModel = calculateRoute(companyAddress);
                        if (routePlanModel == null) {
                            return "路线规划失败，请稍候再试吧";
                        } else {
                            if (calculateRouteListener != null) {
                                calculateRouteListener.onCalculateRoute2Company(!inHome);
                            }
                            StringBuilder sb = new StringBuilder("我为你规划了上班的路线，途经");
                            sb.append(routePlanModel.getMainRoads()).append(",路上");
                            setRoadTrafficNotInNavigate(point, routePlanModel, sb);
                            sb.append("全程").append(routePlanModel.getDistanceStr()).append(",预计").append(routePlanModel.getTotalTime()).append("。");
                            if (!inHome) {
                                sb.append("如果需要导航，请对我说需要");
                                pushMsg2Robot("如果需要导航，请对我说需要");
                            }
                            return sb.toString();
                        }
                    } else {
                        return "请告诉我你公司的位置";
                    }
                }
            }
            case 3: {
                BaiduAddress homeAddress = addressDao.getHomeOrCompanyAddress(context.getResources().getString(R.string.home));
                if (BNavigatorProxy.getInstance().isNaviBegin()) {
                    StringBuilder sb = new StringBuilder();
                    RoutePlanModelProxy routePlanModel = RoutePlanModelProxy.getCacheRoutePlanModelProxy("RoutePlanModel");
                    if (routePlanModel != null && routePlanModel.getEndNode() != null) {
                        if (homeAddress != null && routePlanModel.getEndNode().getName().equals(homeAddress.getName())) {
                            sb.append("回家的路上");
                            RouteModel routeModel = RouteModel.getCurrent();
                            routeModel.refreshRoadCondition();
                            Log.i(TAG, "conditionNodes.size=" + routeModel.getConditionNodes().size());
                            double curDistance = RGAssistGuideModelProxy.getInstance().getCarProgress() * routeModel.getDistance();
                            Queue<Integer> rQueue = new LinkedBlockingQueue<Integer>();
                            int conjestionCount = 0;
                            for (int i = 0; i < routeModel.getConditionNodes().size(); ++i) {
                                if (routeModel.getConditionNodes().get(i).getDistanceFromStart() > curDistance &&
                                        routeModel.getConditionNodes().get(i).getRoadCondition() >= RouteModel.ROAD_CONDITION_TYPE_Slow) {
                                    rQueue.offer(i);
                                    if (routeModel.getConditionNodes().get(i).getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Slow) {
                                        conjestionCount++;
                                    }
                                }
                            }
                            setTrafficState(sb, routeModel, rQueue, conjestionCount, (int) curDistance);
                            return sb.toString();
                        } else {
                            sb.append("我现在正在导航去").append(routePlanModel.getEndNode().getName()).append("，");
                        }

                    } else
                        sb.append("我现在在导航中，");
                    sb.append("暂时不方便为你播报其他路线的路况，如果需要你可以唤醒我后对我说：导航回家。");
                    return sb.toString();
                } else {
                    BaiduAddress companyAddress = addressDao.getHomeOrCompanyAddress(context.getResources().getString(R.string.company));
                    if (homeAddress != null) {
                        double dt = DistanceUtil.getDistance(new LatLng(bdAddress.getLatitude(), bdAddress.getLongitude()), new LatLng(homeAddress.getLatitude(), homeAddress.getLongitude()));
                        Log.i(TAG, "dt===" + dt);
                        if (dt < 1000) {
                            return "您已在家附近。";
                        }
                        boolean inCompany = companyAddress != null;
                        if (inCompany) {
                            inCompany = DistanceUtil.getDistance(new LatLng(bdAddress.getLatitude(), bdAddress.getLongitude()),
                                    new LatLng(companyAddress.getLatitude(), companyAddress.getLongitude())) < 1000;
                        }

                        RouteModel routePlanModel = calculateRoute(homeAddress);
                        if (routePlanModel == null) {
                            return "路线规划失败，请稍候再试吧";
                        } else {
                            if (calculateRouteListener != null) {
                                calculateRouteListener.onCalculateRoute2Home(!inCompany);
                            }
                            StringBuilder sb = new StringBuilder("我为你规划了回家的路线，途经");
                            sb.append(routePlanModel.getMainRoads()).append(",路上");
                            setRoadTrafficNotInNavigate(point, routePlanModel, sb);
                            sb.append("全程").append(routePlanModel.getDistanceStr()).append(",预计").append(routePlanModel.getTotalTime()).append("。");
                            if (!inCompany) {
                                sb.append("如果需要导航，请对我说需要");
                                pushMsg2Robot("如果需要导航，请对我说需要");
                            }
                            return sb.toString();
                        }
                    } else {
                        return "请告诉我你家的位置";
                    }
                }
            }
            case 4: {
                RoutePlanModelProxy routePlanModel = RoutePlanModelProxy.getCacheRoutePlanModelProxy("RoutePlanModel");
                if (routePlanModel != null) {
                    if ((routePlanModel.getEndNode().getName() != null && routePlanModel.getEndNode().getName().indexOf(point) != -1) ||
                            (routePlanModel.getEndNode().getDescription() != null && routePlanModel.getEndNode().getDescription().indexOf(point) != -1)) {
                        StringBuilder sb = new StringBuilder("去往").append(point).append("的路上");
                        RouteModel routeModel = RouteModel.getCurrent();
                        routeModel.refreshRoadCondition();
                        Log.i(TAG, "conditionNodes.size=" + routeModel.getConditionNodes().size());
                        double curDistance = RGAssistGuideModelProxy.getInstance().getCarProgress() * routeModel.getDistance();
                        Queue<Integer> rQueue = new LinkedBlockingQueue<Integer>();
                        int conjestionCount = 0;
                        for (int i = 0; i < routeModel.getConditionNodes().size(); ++i) {
                            if (routeModel.getConditionNodes().get(i).getDistanceFromStart() > curDistance &&
                                    routeModel.getConditionNodes().get(i).getRoadCondition() >= RouteModel.ROAD_CONDITION_TYPE_Slow) {
                                rQueue.offer(i);
                                if (routeModel.getConditionNodes().get(i).getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Slow) {
                                    conjestionCount++;
                                }
                            }
                        }
                        setTrafficState(sb, routeModel, rQueue, conjestionCount, (int) curDistance);
                        return sb.toString();
                    } else {
                        return "您当前正在去往" + routePlanModel.getEndNode().getName() + "的导航中,无法获知" + point + "的拥堵信息";
                    }
                } else {
                    return "您当前还未规划路线！";
                }
            }
            case 5:
                return avoidTraffic(5, point);
            case 6:
                return avoidTraffic(6, "前方");
            case 7:
                return getAllRemainRoadCondition();
            default:
                return null;
        }
    }

    /**
     * 规划从当前位置到目的地的路线并返回路书
     *
     * @param address 目标地点
     * @return 最优路书
     */
    private RouteModel calculateRoute(BaiduAddress address) {
        Future<RouteModel> future = tPools.submit(new CalculateRoutePlanManager(address));
        RouteModel routePlanModel = null;
        try {
            routePlanModel = future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        return routePlanModel;
    }

    /**
     * 规划路线并返回路书集合
     * @param prefrence 算路方式
     * @param list 规划的输入地点集合
     * @return 路书集合
     *//*
    private List<RouteModel> calculateRoute(int prefrence,ArrayList<RoutePlanNode> list){
        Future<List<RouteModel>> future = tPools.submit(new ReCalculateRoutePlanManager(prefrence,list));
        RouteModel routePlanModel = null;
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }*/


    /**
     * 检查是否经过某个点
     *
     * @param point
     * @return
     */
    public int passPoint(String point) {
        int l;
        int calculatePrefrence = BNRoutePlanerProxy.getInstance().getCalcPreference();
        int calculateScheme = BNRoutePlanerProxy.getInstance().getCalculateScheme();
        Map<Integer, Vector<RoutePlanModelProxy>> routePlans = BNRoutePlanerProxy.getInstance().routePlans;
        if ((l = routePlans.size()) > 0) {
            int count = 0;
            Vector<RoutePlanModelProxy> routePlanModels = routePlans.get(calculatePrefrence);
            l = routePlanModels.size();
            RoutePlanModelProxy routePlanModelTemp = routePlanModels.get(calculateScheme);
            for (RoutePlanResultItemProxy item : routePlanModelTemp.getRouteNodeData()) {//检查当前路线是否包含该point,一旦包含，优先使用该路线
                if (item.getNextRoadName().indexOf(point) != -1) {
                    count++;
                    calculateScheme = BNRoutePlanerProxy.getInstance().getCalculateScheme();
                    break;
                } else {
                    calculateScheme = -1;
                }
            }

            if (!BNavigatorProxy.getInstance().isNaviBegin()) {
                for (int i = 0; i < l; i++) {//检查当前线路的其它方案是否包含该point
                    if (i != BNRoutePlanerProxy.getInstance().getCalculateScheme())
                        for (RoutePlanResultItemProxy item : routePlanModels.get(i).getRouteNodeData()) {
                            if (item.getNextRoadName().indexOf(point) != -1) {
                                count++;
                                if (calculateScheme < 0)
                                    calculateScheme = i;
                                break;
                            }
                        }
                }

                for (Integer key : routePlans.keySet()) {//检查其它算路方式下的线路是否包含该point
                    if (key == BNRoutePlanerProxy.getInstance().getCalcPreference())
                        continue;
                    if (calculateScheme < 0)
                        calculatePrefrence = key;
                    l += routePlans.get(key).size();
                    for (int i = 0; i < routePlans.get(key).size(); i++) {
                        for (RoutePlanResultItemProxy item : routePlans.get(key).get(i).getRouteNodeData()) {
                            if (item.getNextRoadName().indexOf(point) != -1) {
                                count++;
                                if (calculateScheme < 0)
                                    calculateScheme = i;
                                break;
                            }
                        }
                    }
                }
            }

            if (calculatePrefrence > -1) {
                selectPassPointLine = calculatePrefrence << 3 | calculateScheme;
            }

            if (count <= 0) {
                return 1;
            } else if (count < l) {
                return 0;
            } else {
                return 2;
            }
        }
        return -1;
    }

    private String avoidTraffic(int type, String point) {
        Log.i(TAG, "avoidTraffic>>" + point);
        int cPrefrence = BNRoutePlanerProxy.getInstance().getCalcPreference();
        int cScheme = RouteModel.getCalculateScheme();
        try {
            StringBuilder sb = new StringBuilder();
            RouteModel routeModel = RouteModel.getLastRouteModels().get(cPrefrence).get(cScheme);
            routeModel.refreshRoadCondition();
            double curDistance = RGAssistGuideModelProxy.getInstance().getCarProgress() * routeModel.getDistance() + COMPENSATION;

            int ml = routeModel.getConditionNodes().size();
            Log.i(TAG, "NavigatorService avoidTraffic()>>" + ml);
            RouteModel.RouteConditionNode node = null;
            Deque<Integer> rDeque = new ArrayDeque<Integer>();
            int pIndex = -1;
            Log.i(TAG, "highwayModel.getCurRoadName=" + RGHighwayModelProxy.getInstance().getCurRoadName() + ",totalDistance=" + RGHighwayModelProxy.getInstance().getGateTotalDist() + "curDistance=" + curDistance);
            int maxDisatance = RGHighwayModelProxy.getInstance().isExists() ? 100000 : 80000;
            int i = 1;
            for (; i < ml; i++) {
                if ((node = routeModel.getConditionNodes().get(i)).getDistanceFromStart() >= curDistance) {
                    rDeque.offer(i);
                }
                if (type == 5)
                    for (String r : node.getRoads()) {
                        if (r.indexOf(point) != -1) {
                            pIndex = i;
                        }
                    }
                else if (type == 6) {
                    if (node.getDistanceFromStart() + node.getDistance() - curDistance > maxDisatance) {
                        break;
                    }
                }
            }
            if (type == 6) {
                pIndex = i;
            }

            if (pIndex == -1) {
                sb.append("导航路线中没有经过").append(point).append("，可能因为您说的地名不完整、或者识别错误，我不能很好的匹配，在导航中暂时不方便为你播报其他路线的路况，继续导航");
            } else {
                if (pIndex < rDeque.getFirst()) {
                    sb.append("你已经经过了").append(point).append("，当前在").append(RGHighwayModelProxy.getInstance().getCurRoadName()).append("行驶，继续导航");
                } else {
                    int l = rDeque.size();
                    Deque<FormatTrafficNode> deque = new ArrayDeque<FormatTrafficNode>();
                    boolean realConjestion = false;
                    while (l-- > 0) {
                        if (rDeque.getFirst() <= pIndex) {
                            node = routeModel.getConditionNodes().get(rDeque.pollFirst());
                            if (node.getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Straightway) {//将拥堵路段插入deque中
                                if (deque.size() == 0) {
                                    deque.offer(new FormatTrafficNode(node.getDistance(), node.getDistanceFromStart(), node.getRoads()));
                                } else {
                                    if ((deque.getLast().getEnd() + 100) <= node.getDistanceFromStart()) {
                                        deque.getLast().append(node);
                                    } else {
                                        deque.offer(new FormatTrafficNode(node.getDistance(), node.getDistanceFromStart(), node.getRoads()));
                                    }
                                }
                                if (!realConjestion && deque.getLast().getDistance() > 200) {
                                    realConjestion = true;
                                }
                            }
                        } else {
                            rDeque.pollFirst();
                        }
                    }
                    l = deque.size();
                    if (l > 0) {
                        avoidTrafficLine = BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND << 3;
                        LastAvoidPoint = point;
                        if (realConjestion) {
                            int conjestionDist = 0;
                            while (l-- > 0) {
                                conjestionDist += deque.poll().distance;
                            }
                            sb.append("我这里显示").append(point).append("堵车长度合计约").append(RouteModel.formatDistance(conjestionDist)).append("，我只有重新规划路线才能确认是否能躲开前面的拥堵。你需要重新规划路线吗");
                            pushMsg2Robot("你需要重新规划路线吗");
                        } else {
                            deque.clear();
                            sb.append("我这里显示").append(point).append("堵车长度合计大概不超过200米。你需要重新规划路线吗");
                            pushMsg2Robot("你需要重新规划路线吗");
                        }
                    } else {
                        sb.append("规划的路线上").append(point).append("路况畅通，继续导航");
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SYSTEM_ERROR;
    }


    private String checkRoadConditionInNavigate(String point) {
        Log.i(TAG, "checkRoadConditionInNavigate>>" + point);
        int cPrefrence = BNRoutePlanerProxy.getInstance().getCalcPreference();
        int cScheme = RouteModel.getCalculateScheme();
        try {
            StringBuilder sb = new StringBuilder();
            RouteModel routeModel = RouteModel.getLastRouteModels().get(cPrefrence).get(cScheme);
            routeModel.refreshRoadCondition();
            double curDistance = RGAssistGuideModelProxy.getInstance().getCarProgress() * routeModel.getDistance() + COMPENSATION;
            Bundle bundle = new Bundle();
            RouteGuider.get().getCurRoadName(bundle);
            Log.i(TAG, "currentRoad=" + bundle);

            int ml = routeModel.getConditionNodes().size();
            RouteModel.RouteConditionNode node = null;
            Deque<Integer> rDeque = new ArrayDeque<Integer>();
            int pIndex = -1;
            Log.i(TAG, "highwayModel.getCurRoadName=" + RGHighwayModelProxy.getInstance().getCurRoadName() + ",totalDistance=" + RGHighwayModelProxy.getInstance().getGateTotalDist() + "curDistance=" + curDistance);
            for (int i = 1; i < ml; i++) {
                if ((node = routeModel.getConditionNodes().get(i)).getDistanceFromStart() >= curDistance) {
                    rDeque.offer(i);
                }
                for (String r : node.getRoads()) {
                    if (r.indexOf(point) != -1) {
                        pIndex = i;
                    }
                }
            }

            if (pIndex == -1) {
                sb.append("导航路线中没有经过").append(point).append("，可能因为您说的地名不完整、或者识别错误，我不能很好的匹配，在导航中暂时不方便为你播报其他路线的路况，继续导航");
            } else {
                if (pIndex < rDeque.getFirst()) {
                    sb.append("你已经经过了").append(point).append("，当前在").append(bundle.getString("road_name")).append("行驶，继续导航");
                } else {
                    int l = rDeque.size();
                    int conjestionCount = 0;
                    boolean realConjestion = false;
                    Deque<FormatTrafficNode> deque = new ArrayDeque<FormatTrafficNode>();
                    List<RouteModel.RouteConditionNode> targetNodes = new ArrayList<RouteModel.RouteConditionNode>();
                    /**
                     * 1.把rDeque中pIndex之后的路段剔除，保证rDeque的队列元素<=pIndex,
                     * 2.在符合1的元素集合中的拥堵路段找出来，并插入到deque中，并保证deque中拥堵路段的间距大于100米，小于100米的两段路段将合并
                     * 3.把point所在路段剔除出来并放到targetNodes中
                     * 1和3剔除后的元素将留在rDeque中
                     */
                    while (l-- > 0) {
                        if (rDeque.getFirst() <= pIndex) {
                            node = routeModel.getConditionNodes().get(rDeque.getFirst());
                            if (node.getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Straightway) {//将拥堵路段插入deque中
                                if (deque.size() == 0) {
                                    deque.offer(new FormatTrafficNode(node.getDistance(), node.getDistanceFromStart(), node.getRoads()));
                                } else {
                                    if ((deque.getLast().getEnd() + 100) <= node.getDistanceFromStart()) {
                                        deque.getLast().append(node);
                                    } else {
                                        deque.offer(new FormatTrafficNode(node.getDistance(), node.getDistanceFromStart(), node.getRoads()));
                                    }
                                }
                                if (node.getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Slow) {
                                    Log.i(TAG, "拥堵路段：" + node.toString());
                                    conjestionCount++;
                                    if (node.getRoadsString().indexOf(point) != -1) {
                                        rDeque.pollFirst();
                                        targetNodes.add(node);
                                    } else {
                                        rDeque.offer(rDeque.pollFirst());
                                    }
                                } else {
                                    rDeque.offer(rDeque.pollFirst());
                                }
                                if (!realConjestion && deque.getLast().getDistance() > 150) {//检查是否有超过150米的拥堵
                                    realConjestion = true;
                                }
                            } else {
                                rDeque.offer(rDeque.pollFirst());
                            }
                        } else {
                            rDeque.pollFirst();
                        }
                    }
                    l = deque.size();
                    if (l > 0) {
                        if (conjestionCount > 0) {
                            if (realConjestion) {
                                sb.append("规划路线上，").append(point);
                                if (targetNodes.size() > 0) {
                                    sb.append("有").append(targetNodes.size() == 2 ? "两" : targetNodes.size());
                                    if (targetNodes.size() == 1) {
                                        sb.append("段拥堵，长度").append(RouteModel.formatDistance(targetNodes.get(0).getDistance())).append("。");
                                    } else {
                                        int max = targetNodes.get(0).getDistance();
                                        int min = max;
                                        for (int i = 1; i < targetNodes.size(); ++i) {
                                            max = Math.max(max, targetNodes.get(i).getDistance());
                                            min = Math.min(min, targetNodes.get(i).getDistance());
                                        }
                                        sb.append("段拥堵，最长").append(RouteModel.formatDistance(max)).append("最短").append(RouteModel.formatDistance(min)).append("。");
                                    }
                                } else {
                                    sb.append("没有拥堵。");
                                }
                                Log.i(TAG, "conjestionCount=" + conjestionCount + ",target.SIZE=" + targetNodes.size());
                                if (conjestionCount > targetNodes.size()) {
                                    sb.append("前往").append(point).append("途中共");
                                    setTrafficState(sb, routeModel, rDeque, conjestionCount - targetNodes.size(), (int) curDistance);
                                }
                                if (cPrefrence == BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND ||
                                        (cPrefrence & BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM) == BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM) {
                                    sb.append("继续导航");
                                } else {//躲避拥堵
                                    avoidTrafficLine = BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND << 3;
                                    sb.append("。需要我躲避拥堵重新规划路线吗");
                                    pushMsg2Robot("需要我躲避拥堵重新规划路线吗");
                                }
                            } else {
                                int d1 = 0, d2 = 0;
                                boolean h = false;
                                FormatTrafficNode n;
                                while (l-- > 0) {
                                    for (String s : (n = deque.poll()).getRoads()) {
                                        if (s.indexOf(point) != -1) {
                                            h = true;
                                            break;
                                        } else {
                                            h = false;
                                        }
                                    }
                                    if (h) {
                                        d2 += n.getDistance();
                                    } else {
                                        d1 += n.getDistance();
                                    }
                                }
                                if (d1 == 0 && d2 != 0) {
                                    sb.append("目前").append(point).append("拥堵").append(RouteModel.formatDistance(d2)).append("，通往").append(point).append("的途中暂无拥堵").append("，继续导航");
                                } else if (d1 != 0 && d2 == 0) {
                                    sb.append("目前前往").append(point).append("的途中有少量拥堵，其中").append(point).append("无拥堵").append("，继续导航");
                                } else if (d1 != 0 && d2 != 0) {
                                    sb.append("目前前往").append(point).append("的途中有少量拥堵，其中").append(point).append("拥堵").append(RouteModel.formatDistance(d2)).append("，继续导航");
                                }
                            }
                        } else {
                            sb.append("导航路线上").append(point).append("部分路段行驶缓慢，继续导航");
                        }
                    } else {
                        sb.append("规划的路线上").append(point).append("路况畅通，继续导航");
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SYSTEM_ERROR;
    }

    public static void setMaxTwoPointConjestion(StringBuilder text) {
        int cPrefrence = BNRoutePlanerProxy.getInstance().getCalcPreference();
        int cScheme = RouteModel.getCalculateScheme();
        try {
            RouteModel routeModel = RouteModel.getLastRouteModels().get(cPrefrence).get(cScheme);
            routeModel.refreshRoadCondition();
            int conjestionCount = 0;
            int max[] = new int[]{-1, -1};
            RouteModel.RouteConditionNode node;
            List<RouteModel.RouteConditionNode> list = routeModel.getConditionNodes();
            for (int i = 0; i < list.size(); ++i) {
                if ((node = list.get(i)).getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Slow) {
                    conjestionCount++;
                    if (max[0] == -1) {
                        max[0] = i;
                    } else if (max[1] == -1) {
                        max[1] = i;
                    } else {
                        if (node.getDistance() > list.get(max[0]).getDistance()) {
                            if (list.get(max[0]).getDistance() < list.get(max[1]).getDistance()) {
                                max[0] = max[1];
                            }
                            max[1] = i;
                        } else if (node.getDistance() > list.get(max[1]).getDistance()) {
                            max[1] = i;
                        }
                    }
                }
            }
            if (conjestionCount > 0) {
                text.append("有").append(conjestionCount == 2 ? "两" : conjestionCount).append("段拥堵，");
                if (max[0] >= 0) {
                    text.append("其中").append(list.get(max[0]).getRoadsString()).append("拥堵")
                            .append(RouteModel.formatDistance(list.get(max[0]).getDistance()));
                    if (max[1] >= 0) {
                        text.append(",").append(list.get(max[1]).getRoadsString()).append("拥堵")
                                .append(RouteModel.formatDistance(list.get(max[1]).getDistance()));
                    }
                    if (conjestionCount > 2) {
                        text.append(",还有").append(conjestionCount - 2).append("段拥堵分布在导航路线上");
                    }
                }
            } else {
                text.append("没有拥堵");
            }
        } catch (Exception e) {
            e.printStackTrace();
            text.setLength(0);
            text.append(SYSTEM_ERROR);
        }
    }

    public static String reCalculateRouteLineCompletedTips() {
        int cp = BNRoutePlanerProxy.getInstance().getCalcPreference();
        StringBuilder sb = new StringBuilder();
        RouteModel rm = RouteModel.getCurrent();
        if ((cp & BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_DIST) == BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_DIST) {
            sb.append("最短路线全程").append(rm.getDistanceStr()).append(",预计").append(rm.getTotalTime()).append(",途经").append(rm.getMainRoads()).append("。");
        } else if ((cp & BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TOLL) == BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TOLL) {
            sb.append(rm.getToll() == 0 ? "找到无费用路线，" : "最少费用预计" + rm.getToll() + "元，");
            sb.append("全程").append(rm.getDistanceStr()).append(",预计").append(rm.getTotalTime()).append(",途经").append(rm.getMainRoads()).append("。");
        } else if ((cp & BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TIME) == BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TIME) {
            sb.append("时间最短路线全程").append(rm.getDistanceStr()).append(",预计").append(rm.getTotalTime()).append(",途经").append(rm.getMainRoads()).append("。");
        } else {
            return "已经重新规划路线," + checkLastAvoidPointConjestion();
        }
        return sb.toString();
    }


    public static String checkLastAvoidPointConjestion() {
        StringBuffer sb = new StringBuffer();
        int cPrefrence = BNRoutePlanerProxy.getInstance().getCalcPreference();
        int cScheme = RouteModel.getCalculateScheme();
        try {
            RouteModel routeModel = RouteModel.getLastRouteModels().get(cPrefrence).get(cScheme);
            int maxDistance = -1;
            int conjestionDistance = 0;
            if (!TextUtils.isEmpty(LastAvoidPoint)) {
                if (LastAvoidPoint.equals("前方")) {
                    maxDistance = RGHighwayModelProxy.getInstance().getGateTotalDist() == -1 ? 80000 : 100000;
                    for (RouteModel.RouteNode n : routeModel.getNodes()) {
                        if (n.getDistanceFromStart() > maxDistance)
                            break;
                        if (n.getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Slow) {
                            conjestionDistance += n.getDistance();
                        }
                    }
                    if (conjestionDistance > 200) {
                        sb.append("未能躲开前方的拥堵，");
                    } else {
                        sb.append("可以躲开前方拥堵，");
                    }
                } else {
                    for (RouteModel.RouteNode n : routeModel.getNodes()) {
                        if (n.getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Slow && n.getName().indexOf(LastAvoidPoint) != -1) {
                            conjestionDistance += n.getDistance();
                        }
                    }
                    if (conjestionDistance > 0) {
                        sb.append("未能躲开").append(LastAvoidPoint).append("的拥堵，");
                    } else {
                        sb.append("可以躲开").append(LastAvoidPoint).append("的拥堵，");
                    }
                }
            }
            sb.append("全程").append(routeModel.getDistanceStr()).append(",预计").append(routeModel.getTotalTime()).append("。");
        } catch (Exception e) {

        }
        return sb.toString();
    }

    /**
     * 规划完路线但未开始导航时，检查去往某点的路况信息
     *
     * @param point
     * @return
     */
    private String checkRoadConditionToPointBeforeNavigate(String point) {
        Log.i(TAG, "checkRoadConditionToPointBeforeNavigate>>" + point);
        int cPrefrence = BNRoutePlanerProxy.getInstance().getCalcPreference();
        int cScheme = RouteModel.getCalculateScheme();
        try {
            RouteModel routeModel = RouteModel.getLastRouteModels().get(cPrefrence).get(cScheme);
            double curDistance = RGAssistGuideModelProxy.getInstance().getCarProgress() * routeModel.getDistance();
            Log.i(TAG, "curDistance=" + curDistance);

            int ml = routeModel.getNodes().size();
            RouteModel.RouteNode node = null;
            int tIndex = 0;
            int pointConjestionDistance = 0;
            for (int i = 1; i < ml; i++) {
                if ((node = routeModel.getNodes().get(i)).getName().indexOf(point) != -1) {
                    tIndex = i;
                    if (node.getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Slow) {
                        pointConjestionDistance += node.getConjestionDistance();
                    }
                }
            }
            StringBuilder sb = new StringBuilder();
            if (tIndex > 0) {//有经过point
                boolean isConjestion = false;//是否包含拥堵路段
                boolean isSlow = false;//是否包含行驶缓慢路段
                for (int i = 0; i <= tIndex; i++) {
                    node = routeModel.getNodes().get(i);
                    if (!isConjestion && node.getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Slow) {
                        isConjestion = true;
                    } else if (!isSlow && isConjestion && node.getRoadCondition() == RouteModel.ROAD_CONDITION_TYPE_Slow) {
                        isSlow = true;
                    }
                    if (isConjestion && isSlow)
                        break;
                }
                if (isConjestion) {//是否包含拥堵路段
                    Deque<FormatTrafficNode> conjestionDistances = formatConjestionDistance(routeModel.getNodes(), tIndex);
                    int l = conjestionDistances.size() / 2 + 1;//拥堵路段的数量
                    List<String> conjestRoads = new ArrayList<String>();
                    int max = getMaxAndSetTraffixRoads(conjestionDistances, conjestRoads);//最长的拥堵长度
                    boolean extConjestion = checkConjestion(conjestionDistances);
                    if (extConjestion) {//是否真正拥堵
                        sb.append("目前前往").append(point).append("的途中，有").append(l).append("段拥堵，最长").append(RouteModel.formatDistance(max)).append("，");
                        if (pointConjestionDistance > 0) {
                            sb.append("其中").append(point).append("拥堵").append(pointConjestionDistance).append("米，");
                        }
                        int i = 0;
                        if (RouteModel.getLastRouteModels().containsKey(BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND)) {
                            routeModel = RouteModel.getLastRouteModels().get(BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND).get(0);
                            sb.append("已经使用躲避拥堵规划路线，全程").append(routeModel.getDistanceStr())
                                    .append(",预计").append(routeModel.getTotalTime());
                            if (cPrefrence != BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND) {
                                cPrefrence = BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND;
                                avoidTrafficLine = cPrefrence << 3;
                                sb.append("。需要躲避拥堵吗");
                                pushMsg2Robot("需要躲避拥堵吗");
                            } else {
                                sb.append(",10秒后继续导航");
                            }
                        } else
                            sb.append("暂时找不到躲避拥堵方案，10秒后继续导航");

                    } else {//轻微拥堵
                        if (pointConjestionDistance > 0) {
                            if (l > 2)
                                sb.append("目前前往").append(point).append("的途中，有少量拥堵，其中").append(point).append("拥堵").append(RouteModel.formatDistance(pointConjestionDistance)).append(",10秒后继续导航");
                            else
                                sb.append("目前").append(point).append("拥堵").append(RouteModel.formatDistance(pointConjestionDistance)).append(",通往").append(point).append("的路上暂无拥堵").append(",10秒后继续导航");

                        } else {
                            sb.append("目前前往").append(point).append("的途中，有少量拥堵，其中").append(point).append("无拥堵").append(",10秒后继续导航");
                        }
                    }
                }//不包含拥堵路段
                else {
                    if (isSlow) {
                        sb.append("导航路线上有部分路段行驶缓慢，10秒后继续导航");
                    } else {
                        sb.append(point).append("在规划路线上，目前从当前位置到").append(point).append("路况无拥堵，10秒后继续导航");
                    }
                }
            } else {
                pointConjestionDistance = 0;
                int pointConjestionCount = 0;
                for (Integer key : RouteModel.getLastRouteModels().keySet()) {
                    if (key != cPrefrence) {
                        ArrayList<RouteModel> routeModels = RouteModel.getLastRouteModels().get(key);
                        for (int i = 0; i < routeModels.size(); i++) {
                            for (RouteModel.RouteNode n : routeModels.get(i).getNodes()) {
                                if (n.getName().indexOf(point) != -1 && n.getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Slow) {
                                    pointConjestionDistance += n.getDistance();
                                    pointConjestionCount++;
                                }
                            }
                            if (pointConjestionDistance > 0)
                                break;
                        }
                        if (pointConjestionDistance > 0)
                            break;
                    }
                }
                if (pointConjestionDistance > 0) {
                    sb.append("导航路线中没有经过").append(point).append(",").append(point).append("有")
                            .append(pointConjestionCount == 2 ? "两" : pointConjestionCount).append("段拥堵")
                            .append(",总长").append(RouteModel.formatDistance(pointConjestionDistance))
                            .append(",10秒后继续导航");
                } else
                    sb.append("导航路线中没有经过").append(point).append("，10秒后继续导航");
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SYSTEM_ERROR;
    }

    public void pushMsg2Robot(String text) {
        mMsgBuilder.setContextMode(SpeechMsg.CONTEXT_KEEP_RECOGNIZE);
        this.pushText = text;
    }

    /**
     * 获取带推送的文本
     **/
    public String getPushText() {
        return pushText;
    }

    public void resetPushText() {
        this.pushText = null;
    }

    /**
     * 检查路书中是否含有拥堵路段，所有拥堵路段都必须大于150米
     *
     * @param conditionDistances
     * @return
     */
    private boolean checkConjestion(Deque<FormatTrafficNode> conditionDistances) {
        int l = conditionDistances.size();
        if (l == 0)
            return false;
        FormatTrafficNode t;
        while ((t = conditionDistances.poll()) != null) {
            if (t.getDistance() > 150) {
                return true;
            }
            conditionDistances.poll();
        }
        return false;
    }

    private int getMaxAndSetTraffixRoads(Deque<FormatTrafficNode> deque, List<String> roads) {
        Log.i(TAG, "getMax");
        int l = deque.size();
        int max = 0;
        int i = 0;
        FormatTrafficNode temp;
        while (l-- > 0) {
            temp = deque.poll();
            Log.i(TAG, i + ">>" + temp.toString());
            if ((i & 1) == 0) {
                max = Math.max(max, temp.getDistance());
                if (temp.getDistance() > 150)
                    for (String r : temp.getRoads()) {
                        if (!roads.contains(r)) {
                            roads.add(r);
                        }
                    }
            }
            deque.offer(temp);
            i++;
        }
        return max;
    }

    private Deque<Integer> formatConjestionDistance(List<RoadConditionItem> conditionItems, int eIndex, double totalDistance) {
        Deque<Integer> dists = new ArrayDeque<Integer>();
        RoadConditionItem item;
        int t;
        double maxIndex = conditionItems.get(conditionItems.size() - 1).curItemEndIndex;
        for (RoadConditionItem conditionItem : conditionItems) {
            conditionItem.curItemEndIndex = (int) (conditionItem.curItemEndIndex / maxIndex * totalDistance);
        }
        for (int i = 0; i < eIndex; i++) {
            item = conditionItems.get(i);
            if (dists.size() == 0) {
                if (item.roadConditionType > RoadConditionItem.ROAD_CONDITION_TYPE_Straightway) {
                    dists.offer(i == 0 ? item.curItemEndIndex : item.curItemEndIndex - conditionItems.get(i - 1).curItemEndIndex);
                }
            } else {
                t = dists.size();
                if ((t & 1) == 1) {//奇数，拥堵长度
                    if (item.roadConditionType > RoadConditionItem.ROAD_CONDITION_TYPE_Straightway) {
                        dists.offer(dists.pollLast() + (item.curItemEndIndex - conditionItems.get(i - 1).curItemEndIndex));
                    } else {
                        dists.offer((item.curItemEndIndex - conditionItems.get(i - 1).curItemEndIndex));
                    }
                } else {//偶数，顺畅长度
                    if (item.roadConditionType > RoadConditionItem.ROAD_CONDITION_TYPE_Straightway) {
                        if (dists.getLast() <= 100) {
                            dists.pollLast();
                            dists.offer(dists.pollLast() + (item.curItemEndIndex - conditionItems.get(i - 1).curItemEndIndex));
                        } else {
                            dists.offer((item.curItemEndIndex - conditionItems.get(i - 1).curItemEndIndex));
                        }
                    } else {
                        dists.offer(dists.pollLast() + (item.curItemEndIndex - conditionItems.get(i - 1).curItemEndIndex));
                    }
                }
            }
        }
        return dists;
    }

    /**
     * 格式化RouteModel的路书展现形式，将间隔只有100米的行驶缓慢或拥堵的路段合并为拥堵
     *
     * @param nodes  RouteModel路书中的路段集合
     * @param tIndex 需要遍历的路段的最大索引值，tIndex<nodes.size();
     * @return 双向队列，size()=奇数，索引奇数的int代表拥堵路段的长度，索引偶数的int代表顺畅路段的长度
     */
    private Deque<FormatTrafficNode> formatConjestionDistance(List<RouteModel.RouteNode> nodes, int tIndex) {
        Deque<FormatTrafficNode> dists = new ArrayDeque<FormatTrafficNode>();
        int t;
        RouteModel.RouteNode node = null;
        for (int i = 0; i <= tIndex; ++i) {
            if (dists.size() == 0) {
                if ((node = nodes.get(i)).getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Straightway) {
                    dists.offer(new FormatTrafficNode(node.getDistance(), node.getDistanceFromStart(), node.getName(), true));
                }
            } else {
                t = dists.size();
                node = nodes.get(i);
                if ((t & 1) == 1) {//奇数，拥堵长度
                    if (node.getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Straightway) {
                        dists.getLast().append(node.getDistance(), node.getName());
                    } else {
                        dists.offer(new FormatTrafficNode(node.getDistance(), node.getDistanceFromStart(), node.getName()));
                    }
                } else {//偶数，顺畅长度
                    if (node.getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Straightway) {
                        if (dists.getLast().getDistance() <= 100) {
                            dists.pollLast();
                            dists.getLast().append(node.getDistance(), node.getName());
                        } else {
                            dists.offer(new FormatTrafficNode(node.getDistance(), node.getDistanceFromStart(), node.getName(), true));
                        }
                    } else {
                        dists.getLast().append(node.getDistance(), node.getName());
                    }
                }
            }
        }
        if ((dists.size() & 1) == 0) {
            dists.pollLast();
        }
        return dists;
    }

    /**
     * 检查路书是否堵车
     *
     * @param target     目标地点
     * @param routeModel 路书
     * @param sb         路况信息
     * @return true=是
     */
    private boolean setRoadTrafficNotInNavigate(String target, RouteModel routeModel, StringBuilder sb) {
        routeModel.refreshRoadCondition();
        Log.i(TAG, "conditionNodes.size=" + routeModel.getConditionNodes().size());
        Queue<Integer> rQueue = new LinkedBlockingQueue<Integer>();
        int conjestionCount = 0;
        for (int i = 0; i < routeModel.getConditionNodes().size(); ++i) {
            if (routeModel.getConditionNodes().get(i).getRoadCondition() >= RouteModel.ROAD_CONDITION_TYPE_Slow) {
                rQueue.offer(i);
                if (routeModel.getConditionNodes().get(i).getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Slow) {
                    conjestionCount++;
                }
            }
        }
        return setTrafficState(sb, routeModel, rQueue, conjestionCount, 0);
    }

    /**
     * 设置路况信息，返回是否堵车
     *
     * @param sb              路况信息
     * @param routeModel      路书
     * @param rQueue          路书中的拥堵/缓慢节点集合
     * @param conjestionCount 拥堵路段的数量
     * @return 是否堵车 true=是
     */
    private boolean setTrafficState(StringBuilder sb, RouteModel routeModel, Queue<Integer> rQueue, int conjestionCount, int curDistance) {
        RouteModel.RouteConditionNode node = null;
        if (routeModel.getConditionNodes() == null)
            throw new NullPointerException("RouteModel.conditionNodes is Null");
        if (rQueue.size() > 0) {
            int l = rQueue.size();
            int index;
            int longSlowCount = 0;
            if (conjestionCount > 0) {
                int longConjestionCount = 0;
                sb.append("有").append(conjestionCount == 2 ? "两" : conjestionCount).append("处拥堵：");
                boolean setFirst = false;
                int firstLongLowDistance = 0;
                int maxConjectionDistance = 0;
                RouteModel.RouteConditionNode max[] = new RouteModel.RouteConditionNode[2];
                for (int i = 0; i < l; ++i) {
                    node = routeModel.getConditionNodes().get(rQueue.poll());
                    if (node.getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Slow) {
                        if (!setFirst) {
                            sb.append("前方")
                                    .append(RouteModel.formatDistance(node.getDistanceFromStart() - curDistance))
                                    .append(node.getRoadsString())
                                    .append("有")
                                    .append(RouteModel.formatDistance(node.getDistance()))
                                    .append("拥堵；");
                            setFirst = true;
                            maxConjectionDistance = node.getDistance();
                        } else {
                            if (node.getDistance() > maxConjectionDistance) {
                                maxConjectionDistance = node.getDistance();
                            }
                            if (max[0] == null) {
                                max[0] = node;
                            } else if (max[1] == null) {
                                max[1] = node;
                            } else {
                                if (node.getDistance() > max[0].getDistance()) {
                                    if (max[1].getDistance() <= max[0].getDistance()) {
                                        if (max[1].getDistance() > 1000)
                                            longConjestionCount++;
                                        max[1] = node;
                                    } else {
                                        if (max[0].getDistance() > 1000)
                                            longConjestionCount++;
                                        max[0] = max[1];
                                        max[1] = node;
                                    }
                                } else if (node.getDistance() > max[1].getDistance()) {
                                    if (max[1].getDistance() > 1000)
                                        longConjestionCount++;
                                    max[1] = node;
                                } else if (node.getDistance() > 1000)
                                    longConjestionCount++;
                            }
                        }
                    } else if (node.getDistance() > 1000) {
                        if (!setFirst) {
                            sb.append("前方")
                                    .append(RouteModel.formatDistance(node.getDistanceFromStart() - curDistance))
                                    .append(node.getRoadsString())
                                    .append("有")
                                    .append(RouteModel.formatDistance(node.getDistance()))
                                    .append("行驶缓慢；");
                            conjestionCount++;
                            setFirst = true;
                        } else {
                            longSlowCount++;
                            if (firstLongLowDistance == 0) {
                                firstLongLowDistance = node.getDistance();
                            }
                        }
                    }
                }
                if (conjestionCount < 4) {
                    for (RouteModel.RouteConditionNode n : max) {
                        if (n != null) {
                            sb.append("前方")
                                    .append(RouteModel.formatDistance(n.getDistanceFromStart()))
                                    .append(n.getRoadsString())
                                    .append("有")
                                    .append(RouteModel.formatDistance(n.getDistance()))
                                    .append("拥堵；");
                        }
                    }
                } else {
                    sb.append("剩余拥堵中较长的两段路为：");
                    for (RouteModel.RouteConditionNode n : max) {
                        if (n != null) {
                            sb.append("前方")
                                    .append(RouteModel.formatDistance(n.getDistanceFromStart()))
                                    .append(n.getRoadsString())
                                    .append("有")
                                    .append(RouteModel.formatDistance(n.getDistance()))
                                    .append("拥堵；");
                        }
                    }
                }
                if (longConjestionCount > 0 || longSlowCount > 0) {
                    sb.append("另外还有");
                    if (longConjestionCount > 0) {
                        sb.append(longConjestionCount == 2 ? "两" : longConjestionCount).append("段超过1公里的拥堵");
                    }
                    if (longSlowCount > 0) {
                        if (longConjestionCount > 0)
                            sb.append("和");
                        if (longSlowCount == 1)
                            sb.append("一段").append(RouteModel.formatDistance(firstLongLowDistance)).append("的缓慢路段");
                        else
                            sb.append(longSlowCount == 2 ? "两" : longSlowCount).append("段超过1公里的缓慢路段");
                    }
                    sb.append("分布在导航路线上。");
                }
                if (maxConjectionDistance > 150 && !isCongestion()) {
                    sb.append("。如果需要更改路线，请告诉我需要");
                    pushMsg2Robot("如果需要更改路线，请告诉我需要");
                }
            } else {
                sb.append("有").append(l == 2 ? "两" : l).append("段路行驶缓慢，");
                for (int i = 0; i < l; ++i) {
                    node = routeModel.getConditionNodes().get((index = rQueue.poll()));
                    if (i < 4) {
                        sb.append("前方")
                                .append(RouteModel.formatDistance(node.getDistanceFromStart() - curDistance))
                                .append(node.getRoadsString())
                                .append("有")
                                .append(RouteModel.formatDistance(node.getDistance()))
                                .append("行驶缓慢。");
                    } else {
                        if (node.getDistance() > 1000) {
                            longSlowCount++;
                        }
                    }
                }
                if (longSlowCount > 0) {
                    sb.append("另外还有").append(longSlowCount).append("超过1公里的缓慢路段分布在导航路线上");
                }
            }
            return true;
        } else {
            sb.append("畅通");
            return false;
        }
    }

    private String getAllRemainRoadCondition() {
        int cPrefrence = BNRoutePlanerProxy.getInstance().getCalcPreference();
        int cScheme = RouteModel.getCalculateScheme();
        try {
            if (RouteModel.getLastRouteModels().size() == 0 ||
                    !RouteModel.getLastRouteModels().containsKey(cPrefrence)) {
                return "抱歉，目前我的导航和路况查询能力受限于第三方，还没有完善发挥出来，请你尽量告诉我一个准确的目的地，才能为你提供好的服务。";
            }
            RouteModel routeModel = RouteModel.getLastRouteModels().get(cPrefrence).get(cScheme);
            routeModel.refreshRoadCondition();
            double curDistance = RGAssistGuideModelProxy.getInstance().getCarProgress() * routeModel.getDistance();

            int ml = routeModel.getConditionNodes().size();
            RouteModel.RouteConditionNode node = null;
            Queue<Integer> rQueue = new LinkedBlockingQueue<Integer>();
            int conjestionCount = 0;
            int maxDistance = RGHighwayModelProxy.getInstance().getGateTotalDist() == -1 ? 80000 : 100000;
            Log.i(TAG, "highwayModel.getCurRoadName=" + RGHighwayModelProxy.getInstance().getCurRoadName() + ",totalDistance=" + RGHighwayModelProxy.getInstance().getGateTotalDist());
            for (int i = 1; i < ml; i++) {
                if ((node = routeModel.getConditionNodes().get(i)).getDistanceFromStart() >= curDistance
                        && node.getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Straightway) {
                    if ((node.getDistanceFromStart() - curDistance) > maxDistance)
                        break;
                    if (node.getRoadCondition() == RouteModel.ROAD_CONDITION_TYPE_Slow) {
                        if (node.getDistance() > 150)
                            rQueue.offer(i);
                    } else {
                        rQueue.offer(i);
                        conjestionCount++;
                    }
                }
            }
            StringBuilder sb = new StringBuilder("导航路线上");
            if (rQueue.size() == 0) {
                sb.append("没有拥堵。");
            } else {
                ml = rQueue.size();
                if (conjestionCount > 0)
                    sb.append("共有").append(conjestionCount == 2 ? "两" : conjestionCount).append("处拥堵，");
                else
                    sb.append("共有").append(conjestionCount == 2 ? "两" : conjestionCount).append("处行驶缓慢，");
                while (ml-- > 0) {
                    node = routeModel.getConditionNodes().get(rQueue.poll());
                    sb.append("前方").append(RouteModel.formatDistance(node.getDistanceFromStart() - (int) curDistance))
                            .append("有")
                            .append(RouteModel.formatDistance(node.getDistance()))
                            .append(node.getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Slow ? "拥堵" : "行驶缓慢")
                            .append(";");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SYSTEM_ERROR;
    }

    /**
     * 查询剩余路段的拥堵信息
     *
     * @return
     */
    private String getRemainRoadCondition() {
        int cPrefrence = BNRoutePlanerProxy.getInstance().getCalcPreference();
        int cScheme = RouteModel.getCalculateScheme();
        try {
            RouteModel routeModel = RouteModel.getLastRouteModels().get(cPrefrence).get(cScheme);
            routeModel.refreshRoadCondition();
            double curDistance = RGAssistGuideModelProxy.getInstance().getCarProgress() * routeModel.getDistance();

            int ml = routeModel.getConditionNodes().size();
            RouteModel.RouteConditionNode node = null;
            Queue<Integer> rQueue = new LinkedBlockingQueue<Integer>();
            int conjestionCount = 0;
            int maxDistance = RGHighwayModelProxy.getInstance().isExists() ? 100000 : 80000;
            Log.i(TAG, "highwayModel.getCurRoadName=" + RGHighwayModelProxy.getInstance().getCurRoadName() + ",totalDistance=" + RGHighwayModelProxy.getInstance().getGateTotalDist());
            for (int i = 1; i < ml; i++) {
                if ((node = routeModel.getConditionNodes().get(i)).getDistanceFromStart() >= curDistance) {
                    if ((node.getDistanceFromStart() - curDistance) > maxDistance)
                        break;
                    if (node.getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Straightway) {
                        rQueue.offer(i);
                        if (node.getRoadCondition() > RouteModel.ROAD_CONDITION_TYPE_Slow) {
                            conjestionCount++;
                        }
                    }
                }
            }
            StringBuilder sb = new StringBuilder("导航路线上");
            if (!setTrafficState(sb, routeModel, rQueue, conjestionCount, (int) curDistance)) {
                sb.append(",继续导航");
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SYSTEM_ERROR;
    }

    private LocateListener locateBDListener = new LocateListener(LocateListener.CoorType.BD09LL) {
        @Override
        public void update(Address ad) {
            bdAddress = ad;
        }
    };

    private LocateListener locateGCListener = new LocateListener(LocateListener.CoorType.GCJ02) {
        @Override
        public void update(Address ad) {
            gcAddress = ad;
        }
    };


    class CalculateRoutePlanManager extends BNRoutePlanObserverProxy implements Callable<RouteModel> {

        private RouteModel result;
        private BaiduAddress target;
        private Lock lock = new ReentrantLock();
        private Condition condition = lock.newCondition();

        public CalculateRoutePlanManager(BaiduAddress target) {
            this.target = target;
        }

        @Override
        public RouteModel call() throws Exception {
            calculate();
            return result;
        }

        private void calculate() {
            lock.lock();
            try {
                BNRoutePlanNode sn = new BNRoutePlanNode(gcAddress.getLongitude(), gcAddress.getLatitude(), gcAddress.getAddressDetail(), "");
                BNRoutePlanNode tn = new BNRoutePlanNode(target.getLongitude(), target.getLatitude(), target.getName(), target.getAddress());

                routeInputs.clear();
                routeInputs.add(sn);
                routeInputs.add(tn);
                BNRoutePlanerProxy.getInstance().setObserver(this);
                int prefrence = BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND;
                BNRoutePlanerProxy.getInstance().setCalcPrference(prefrence);
                RouteGuider.get().routePlan(routeInputs, prefrence, this);
                condition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                Log.i(TAG, "end..............................");
                lock.unlock();
            }
        }

        @Override
        public void update(Object bnSubject, int type, int event, Object o) {
            lock.lock();
            try {
                Log.e(TAG, "BaiduRoutePlanManager>>update");
                if (type == 1) {
                    if (event == 2) {
                        Log.e(TAG, "百度导航算路成功");
                        RoutePlanModelProxy mRoutePlanModel = RoutePlanModelProxy.getCacheRoutePlanModelProxy("RoutePlanModel");
                        Bundle bundle = new Bundle();
                        BNRoutePlanerProxy.getInstance().getRouteInfo(0, bundle);
                        result = new RouteModel(bundle);

                        ArrayList<RouteModel> list = new ArrayList<RouteModel>();
                        Vector<RoutePlanModelProxy> list2 = new Vector<RoutePlanModelProxy>();
                        list.add(result);
                        list2.add(mRoutePlanModel);
                        int cnt = BNRoutePlanerProxy.getInstance().getRouteCnt();
                        if (cnt > 1)
                            for (int i = 1; i < cnt; i++) {
                                BNRoutePlanerProxy.getInstance().getRouteInfo(0, bundle);
                                list.add(new RouteModel(bundle));
                                mRoutePlanModel = new RoutePlanModelProxy();
                                mRoutePlanModel.parseRouteResult(context, bundle);
                                list2.add(mRoutePlanModel);
                            }
                        RouteModel.getLastRouteModels().clear();
                        RouteModel.getLastRouteModels().put(BNRoutePlanerProxy.getInstance().getCalcPreference(), list);
                        RouteModel.setCalculateScheme(0);
                        BNRoutePlanerProxy.getInstance().routePlans.clear();
                        BNRoutePlanerProxy.getInstance().routePlans.put(BNRoutePlanerProxy.getInstance().getCalcPreference(), list2);
                        BNRoutePlanerProxy.getInstance().setCalculateScheme(0);
                    } else if (event == 3 || event == 6 || event == 18 || event == 19) {
                        Log.e(TAG, "百度导航算路失败");
                    }
                }
            } finally {
                lastCalculateTime = System.currentTimeMillis();
                condition.signal();
                lock.unlock();
            }
        }

    }

/*
    class ReCalculateRoutePlanManager implements Callable<List<RouteModel>>,BNRoutePlanObserver {

        private List<RouteModel> result=new ArrayList<RouteModel>();
        private Lock lock=new ReentrantLock();
        private Condition condition=lock.newCondition();
        private int calculatePrefrence;
        private ArrayList<RoutePlanNode> nodes;

        public ReCalculateRoutePlanManager(int calculatePrefrence,ArrayList<RoutePlanNode> nodes) {
            this.calculatePrefrence=calculatePrefrence;
            this.nodes=nodes;
        }

        @Override
        public List<RouteModel> call() throws Exception {
            calculate();
            return result;
        }

        private void calculate() {
            lock.lock();
            try {
                BNRoutePlanerProxy.getInstance().setObserver(this);
                BNRoutePlanerProxy.getInstance().setCalcPrference(calculatePrefrence);
                BNRoutePlanerProxy.getInstance().setPointsToCalcRoute(nodes, 1, false, (String) null, 0);
                condition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally{
                Log.i(TAG,"end..............................");
                lock.unlock();
            }
        }

        @Override
        public void update(BNSubject bnSubject, int type, int event, Object o) {
            lock.lock();
            try {
                Log.e(TAG, "BaiduRoutePlanManager>>update");
                if (type == 1) {
                    if (event == 2) {
                        Log.e(TAG, "百度导航算路成功");
                        RoutePlanModel mRoutePlanModel = (RoutePlanModel) NaviDataEngine.getInstance().getModel("RoutePlanModel");
                        Bundle bundle=new Bundle();
                        BNRoutePlanerProxy.getInstance().getRouteInfo(0, bundle);
                        result.add(new RouteModel(bundle));
                        if(mRoutePlanModel.getRouteCnt()>1){
                            for(int i=1;i<mRoutePlanModel.getRouteCnt();++i){
                                BNRoutePlanerProxy.getInstance().getRouteInfo(i, bundle);
                                result.add(new RouteModel(bundle));
                            }
                        }
                    } else if (event == 3 || event == 6 || event == 18 || event == 19) {
                        Log.e(TAG, "百度导航算路失败");
                    }
                }
            }finally {
                lastCalculateTime=System.currentTimeMillis();
                condition.signal();
                lock.unlock();
            }
        }
    }*/

    public static class FormatTrafficNode {
        private boolean conjestion;
        private int distance;
        /**
         * 该路段的起始点距离路线起始点的距离
         */
        private int distanceFromStart;
        /**
         * 该路段的结束点距离路线起始点的距离,针对合并多个拥堵路段的情况，该值可能大于distanceFromStart+distance
         */
        private int end;
        private Set<String> roads;

        public FormatTrafficNode(int distance, int distanceFromStart, String roadName) {
            this(distance, distanceFromStart, roadName, false);
        }

        public FormatTrafficNode(int distance, int distanceFromStart, List<String> roads) {
            this.distance = distance;
            this.distanceFromStart = distanceFromStart;
            this.roads = new HashSet<String>();
            this.conjestion = true;
            this.roads.addAll(roads);
            this.end = this.distance + this.distanceFromStart;
        }

        public FormatTrafficNode(int distance, int distanceFromStart, String roadName, boolean conjestion) {
            this.distance = distance;
            this.distanceFromStart = distanceFromStart;
            this.roads = new HashSet<String>();
            this.conjestion = conjestion;
            if (!TextUtils.isEmpty(roadName)) {
                this.roads.add(roadName);
            }
            this.end = this.distance + this.distanceFromStart;
        }

        public void append(int distance, String roadName) {
            this.distance += distance;
            this.roads.add(roadName);
            this.end = this.distance + this.distanceFromStart;
        }

        public void append(RouteModel.RouteConditionNode node) {
            this.distance += node.getDistance();
            roads.addAll(node.getRoads());
            this.end = node.getDistanceFromStart() + node.getDistance();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("{");
            sb.append("distance=").append(distance).append(",");
            sb.append("start=").append(distanceFromStart).append(",");
            sb.append("roads=[");
            for (String r : roads) {
                sb.append(r).append(",");
            }
            sb.setLength(sb.length() - 1);
            sb.append("]}");

            return sb.toString();
        }

        public int getEnd() {
            return end;
        }

        public void setEnd(int end) {
            this.end = end;
        }

        public boolean isConjestion() {
            return conjestion;
        }

        public void setConjestion(boolean conjestion) {
            this.conjestion = conjestion;
        }

        public int getDistance() {
            return distance;
        }

        public void setDistance(int distance) {
            this.distance = distance;
        }

        public int getDistanceFromStart() {
            return distanceFromStart;
        }

        public void setDistanceFromStart(int distanceFromStart) {
            this.distanceFromStart = distanceFromStart;
        }

        public Set<String> getRoads() {
            return roads;
        }

    }


    public interface CalculateRouteListener {
        public void onCalculateRoute2Home(boolean showInRouteGuide);

        public void onCalculateRoute2Company(boolean showInRouteGuide);
    }

}
