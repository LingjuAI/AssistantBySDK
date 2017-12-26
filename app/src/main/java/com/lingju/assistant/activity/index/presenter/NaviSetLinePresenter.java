package com.lingju.assistant.activity.index.presenter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;

import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.baidu.navisdk.adapter.impl.RouteGuider;
import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.NaviSetLineActivity;
import com.lingju.assistant.activity.event.MapCmdEvent;
import com.lingju.assistant.activity.event.NaviRouteCalculateEvent;
import com.lingju.assistant.activity.event.NaviSwitchRouteLineEvent;
import com.lingju.assistant.activity.index.INaviSetLine;
import com.lingju.assistant.baidunavi.adapter.BaiduNaviSuperManager;
import com.lingju.assistant.entity.RobotConstant;
import com.lingju.assistant.service.AssistantService;
import com.lingju.assistant.service.NavigatorService;
import com.lingju.assistant.service.VoiceMediator;
import com.lingju.audio.engine.base.SpeechMsg;
import com.lingju.audio.engine.base.SpeechMsgBuilder;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.common.log.Log;
import com.lingju.context.entity.Navigation;
import com.lingju.context.entity.Route;
import com.lingju.context.entity.Routenode;
import com.lingju.lbsmodule.adapter.BNRoutePlanNodeGenerator;
import com.lingju.lbsmodule.entity.RouteModel;
import com.lingju.lbsmodule.location.Address;
import com.lingju.lbsmodule.location.BaiduLocateManager;
import com.lingju.lbsmodule.location.LocateListener;
import com.lingju.lbsmodule.proxy.BNMapControllerProxy;
import com.lingju.lbsmodule.proxy.BNRoutePlanerProxy;
import com.lingju.lbsmodule.proxy.BNStatisticsManagerProxy;
import com.lingju.lbsmodule.proxy.BNSysLocationManagerProxy;
import com.lingju.lbsmodule.proxy.BNavigatorProxy;
import com.lingju.lbsmodule.proxy.GeoPointProxy;
import com.lingju.lbsmodule.proxy.IBNavigatorListenerProxy;
import com.lingju.lbsmodule.proxy.LocData;
import com.lingju.lbsmodule.proxy.MapParams;
import com.lingju.lbsmodule.proxy.MapStatusProxy;
import com.lingju.lbsmodule.proxy.OnNavigationListenerProxy;
import com.lingju.lbsmodule.proxy.RGAssistGuideModelProxy;
import com.lingju.lbsmodule.proxy.RGHighwayModelProxy;
import com.lingju.lbsmodule.proxy.RGMapModeViewControllerProxy;
import com.lingju.lbsmodule.proxy.RoutePlanModelProxy;
import com.lingju.lbsmodule.proxy.RoutePlanResultItemProxy;
import com.lingju.model.BaiduAddress;
import com.lingju.robot.AndroidChatRobotBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ken on 2016/12/23.
 */
public class NaviSetLinePresenter implements INaviSetLine.IPresenter {

    private static final String TAG = "NaviSetLine";
    private INaviSetLine.INaviSetLineView mSetLineView;
    private Activity mContext;
    private Address address;    //当前定位位置对象
    private int calculatePreference;    //算路偏好
    private String[] preStartTips;      //算路成功后的语音提示数组
    private Intent intent;
    private BaiduNaviSuperManager naviManager;
    private String passPoint;   //途经点名称
    /**
     * 路线规划类型，对应NaviRouteCalculateEvent.getType();
     */
    private int calculateType;
    private int mixCalculateType;   //混合规划类型
    /**
     * 路线规划状态
     **/
    private int calculateRoad = NaviSetLineActivity.UN_CALCULATE;
    private Handler mHandler;
    private GLSurfaceView mMapView;     //导航路线图层
    private ArrayList<BNRoutePlanNode> routeInputs;     //算路节点（起点、终点、可能包含途经点）
    private int calculateScheme;    //同一算路偏好下的算路方案
    private boolean naviCompleted;  //导航是否完成标记
    private boolean isGuiding;      //是否正在导航标记
    private boolean reCalculateInNavigate;  //切换路线后重新算路
    private int system_is_wakeup_mode = 0;
    /**
     * 切换路线完成后马上开始
     */
    private boolean switchFinishedStartImmediately = true;
    private boolean countDownStartNavi = true;     //是否倒计时
    private Timer countDownTimer;   //导航开始倒计时计时器
    private Bundle mLaunchConfigParams;     //导航信息参数
    private boolean stop_in_backgound;
    private final AppConfig appConfig;
    private View mNaviView;
    private boolean showTraffic = false;
    private Navigation mNavigation = new Navigation();         //导航引擎对象
    private Disposable mDisposable;

    public NaviSetLinePresenter(INaviSetLine.INaviSetLineView view) {
        this.mSetLineView = view;
        mContext = (Activity) view;
        appConfig = (AppConfig) mContext.getApplication();
    }

    @Override
    public void initData(Intent intent, Handler handler) {
        this.intent = intent;
        mSetLineView.initView();

        calculatePreference = AppConfig.dPreferences.getInt(NaviSetLineActivity.CALCULATE_MODE, BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND);
        preStartTips = mContext.getResources().getStringArray(R.array.navi_pre_start_tips);
        address = ((AppConfig) mContext.getApplication()).address;

        /* 初始化地图、导航引擎 */
        naviManager = new BaiduNaviSuperManager(mContext, naviInitListener, handler);
        this.mHandler = handler;
        if (this.intent != null) {
            int calculateMode = intent.getIntExtra("calculateMode", 0);
            if (calculateMode != 0)
                calculatePreference = calculateMode;
            passPoint = intent.getStringExtra("passPoint");
            mixCalculateType = (calculateType = intent.getIntExtra("type", 0));
            calculateRoad = intent.getIntExtra(NaviSetLineActivity.CALCULATE_ROAD, NaviSetLineActivity.UN_CALCULATE);
        }
    }


    @Override
    public void setRouteMode() {
        /* 获取指定算路偏好的算路模型集合 */
        Vector<RoutePlanModelProxy> vs = BNRoutePlanerProxy.getInstance().routePlans.get(calculatePreference);

        /* 显示导航路线文本（最多三条显示路线） */
        if (vs != null && vs.size() > 0) {
            mSetLineView.setRouteLineModeText(vs.get(0), 0);
            if (vs.size() >= 2) {
                mSetLineView.setRouteLineModeText(vs.get(1), 1);
            } else {
                mSetLineView.setRouteLineModeText(null, 1);
            }
            if (vs.size() == 3) {
                mSetLineView.setRouteLineModeText(vs.get(2), 2);
            } else {
                mSetLineView.setRouteLineModeText(null, 2);
            }
            //BNRoutePlaner.getInstance().selectRoute(0);
            /* 默认选择路线一 *//*
            BNRoutePlanerProxy.getInstance().setCalculateScheme(calculateScheme);*/
            /* 显示路线选择，导航控制栏 */
            mSetLineView.showRouteLineBox(true);
            mSetLineView.setVoiceBtnCenter();
            /* 显示话筒 */
            mSetLineView.showVoiceBtn(true);
            /* 设置导航图层 */
            BNMapControllerProxy.getInstance().setLayerMode(
                    MapParams.LayerMode.MAP_LAYER_MODE_ROUTE_DETAIL_FOR_NAVI);
            BNMapControllerProxy.getInstance().updateLayer(MapParams.LayerType.MAP_LAYER_TYPE_ROUTE);
            //上传导航路线集合
            uploadRoute(vs);
            setRouteModeDetail(0);

            Intent intent = new Intent(mContext, AssistantService.class);
            intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PUSH_ROUTE_CACULATE);
            intent.putExtra(AssistantService.TEXT, NaviSetLineActivity.NAVI_ROUTE_CALCULATED);
            mContext.startService(intent);
        }
    }

    /**
     * 上传导航路线集合
     **/
    private void uploadRoute(Vector<RoutePlanModelProxy> vs) {
        List<Route> routes = new ArrayList<>();
        com.lingju.context.entity.Address startAddress = getAddress(routeInputs.get(0));
        com.lingju.context.entity.Address endAddress = getAddress(routeInputs.get(routeInputs.size() - 1));
        List<com.lingju.context.entity.Address> passPoints = new ArrayList<>();
        for (int i = 1; i < routeInputs.size() - 1; i++) {
            passPoints.add(getAddress(routeInputs.get(i)));
        }
        ArrayList<RouteModel> routeModels = RouteModel.getLastRouteModels().get(calculatePreference);
        for (int i = 0; i < vs.size(); i++) {
            routes.add(getRoute(vs.get(i), startAddress, endAddress, passPoints, routeModels.get(i), i));
        }
        AndroidChatRobotBuilder.get().robot().append(routes);
    }

    /**
     * 填充导航路线
     **/
    private Route getRoute(RoutePlanModelProxy routePlanModel, com.lingju.context.entity.Address startAddress, com.lingju.context.entity.Address endAddress, List<com.lingju.context.entity.Address> passPoints, RouteModel routeModel, int index) {
        Route route = new Route();
        route.setRouteid(index);
        route.setStartaddress(startAddress);
        route.setEndaddress(endAddress);
        route.setTransitpoint(passPoints);
        route.setRoutetime(routePlanModel.getTotalTimeInt() / 60);
        route.setRoutedistance(Double.valueOf(routePlanModel.getTotalDistance().substring(0, routePlanModel.getTotalDistance().length() - 2)));
        route.setRouteroll(routePlanModel.getTollFees());
        route.setMainroad(routePlanModel.getMainRoads());
        route.setType("DRIVELIEN");
        List<Routenode> routenodes = new ArrayList<>();
        //刷新路况信息
        routeModel.refreshRoadCondition();
        List<RouteModel.RouteNode> nodes = routeModel.getNodes();
        for (RouteModel.RouteNode node : nodes) {
            Routenode routenode = new Routenode();
            routenode.setDistance(node.getDistance());
            routenode.setDistancefromstart(node.getDistanceFromStart());
            routenode.setName(node.getName());
            routenode.setTurntype(node.getTurnType());
            routenode.setTurnstring(node.getTurnString());
            routenode.setRoadcondition(node.getRoadCondition());
            routenode.setLatitude(node.getLatitude() / 1e5);
            routenode.setLongitude(node.getLongitude() / 1e5);
            routenode.setConjestiondistance(node.getConjestionDistance());
            routenode.setSlowdistance(node.getSlowDistance());
            routenodes.add(routenode);
        }
        route.setRoutenodes(routenodes);
        return route;
    }

    /**
     * 填充地址对象
     **/
    private com.lingju.context.entity.Address getAddress(BNRoutePlanNode routePlanNode) {
        com.lingju.context.entity.Address address = new com.lingju.context.entity.Address();
        address.setName(routePlanNode.getName());
        address.setDetailedaddress(routePlanNode.getDescription());
        address.setLongitude(routePlanNode.getLongitude());
        address.setLatitude(routePlanNode.getLatitude());
        return address;
    }

    @Override
    public void setRouteModeDetail(int index) {
        calculateScheme = index;
        cancelCountDownTimer();
        /* 设置指定路线 */
        BNRoutePlanerProxy.getInstance().selectRoute(index);
        BNRoutePlanerProxy.getInstance().setCalculateScheme(index);
        RouteModel.setCalculateScheme(index);
        /* 获取指定算路偏好的指定路线 */
        RoutePlanModelProxy mRoutePlanModel = BNRoutePlanerProxy.getInstance().routePlans.get(calculatePreference).get(index);
        /* 绑定视图数据 */
        mSetLineView.updateRouteDetailList(mRoutePlanModel.getRouteNodeData());
        mSetLineView.setLineMsg(mRoutePlanModel);
        mSetLineView.setLineMsg(mRoutePlanModel);
        /* 设置导航路线图层 */
        BNMapControllerProxy.getInstance().setLayerMode(
                MapParams.LayerMode.MAP_LAYER_MODE_ROUTE_DETAIL_FOR_NAVI);
        BNMapControllerProxy.getInstance().updateLayer(MapParams.LayerType.MAP_LAYER_TYPE_ROUTE);
        //rLineDetailListBox.setVisibility(View.VISIBLE);

        for (int i = 0; i < 3; i++) {
            if (i == index) {   //选中路线
                mSetLineView.setRouteLineModeStyle(i, true);
            } else {            //其他导航路线
                mSetLineView.setRouteLineModeStyle(i, false);
            }
        }
        //上传导航引擎对象
        com.lingju.context.entity.Address startAddress = getAddress(routeInputs.get(0));
        com.lingju.context.entity.Address endAddress = getAddress(routeInputs.get(routeInputs.size() - 1));
        List<com.lingju.context.entity.Address> passPoints = new ArrayList<>();
        for (int i = 1; i < routeInputs.size() - 1; i++) {
            passPoints.add(getAddress(routeInputs.get(i)));
        }
        RouteModel routeModel = RouteModel.getCurrent();
        Route route = getRoute(mRoutePlanModel, startAddress, endAddress, passPoints, routeModel, calculateScheme);
        mNavigation.setRoute(route);
        mNavigation.setNaviplan(calculateScheme + 1);
        mNavigation.setIscongestion(NavigatorService.get().isCongestion() ? "ON" : "OFF");
        mNavigation.setStatus(RobotConstant.NaviStatus.PLAN.toString());
        uploadNavigation();
        keepNavigation();
        Log.i(TAG, "第一段路名:" + mRoutePlanModel.getFirstRoadName() + ",时间：" + mRoutePlanModel.getTotalTime() + ",花费：" + mRoutePlanModel.getTollFees() + ",routeNum：" + mRoutePlanModel.getNodeNum() + ",主要道路：" + mRoutePlanModel.getMainRoads());
        ArrayList<RoutePlanResultItemProxy> rList = mRoutePlanModel.getRouteNodeData();
        BDLocation tt = new BDLocation();
        for (RoutePlanResultItemProxy ri : rList) {
            tt.setLatitude(ri.getLatitude() / 1e5);
            tt.setLongitude(ri.getLongitude() / 1e5);
            tt = LocationClient.getBDLocationInCoorType(tt, BDLocation.BDLOCATION_GCJ02_TO_BD09LL);
            System.out.println(ri.getNextRoadName() + ",condition=" + ri.getRoadCondition() + ",coordinate=[" + tt.getLongitude() + "," + tt.getLatitude() + "]");
        }
    }

    /**
     * 上传导航引擎对象
     **/
    private void uploadNavigation() {
        List<Navigation> list = new ArrayList<>();
        list.add(mNavigation);
        AndroidChatRobotBuilder.get().robot().actionTargetAccessor().uploadContextObject(RobotConstant.ACTION_NAVIGATION, list);
    }

    /**
     * 由于服务端2分钟无交互则清空上下文对象，每2分钟上传一次导航引擎，保证导航状态正常
     **/
    private void keepNavigation() {
        cancelDisposable();
        mDisposable = Observable.interval(2, 2, TimeUnit.MINUTES)
                .observeOn(Schedulers.io())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        uploadNavigation();
                        if (mNavigation == null) {
                            throw new RuntimeException("导航结束了。。。");
                        }
                    }
                });
    }

    private void cancelDisposable() {
        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
            mDisposable = null;
        }
    }


    @Override
    public void startCountDown() {
        countDownTimer = new Timer();
        countDownTimer.schedule(new TimerTask() {
            private int i = 0;

            @Override
            public void run() {
                if (++i > 10) {
                    stopCountDown();
                    mHandler.sendEmptyMessage(NaviSetLineActivity.MSG_START_NAV);
                } else {
                    Message msg = new Message();
                    msg.what = NaviSetLineActivity.MSG_UPDATE_COUNTDOWN;
                    msg.arg1 = 10 - i;
                    mHandler.sendMessage(msg);
                }
            }
        }, 1000, 1000);
    }

    @Override
    public void stopCountDown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    @Override
    public void cancelCountDownTimer() {
        if (countDownTimer != null) {
            stopCountDown();
            mSetLineView.setVirtualNaviText("模拟导航");
        }
    }

    @Override
    public void startCalculateRoad() {
        if (routeInputs == null || routeInputs.size() == 0) {
            if (intent != null) {
                if (intent.getParcelableArrayListExtra("addresses") != null) {
                    if (routeInputs == null)
                        routeInputs = new ArrayList<>();
                    else
                        routeInputs.clear();
                    /* 获取intent中包含的节点地址集合，并填充到算路节点集合中 */
                    ArrayList<BaiduAddress> addresses = intent.getParcelableArrayListExtra("addresses");
                    for (BaiduAddress bd : addresses) {
                        routeInputs.add(new BNRoutePlanNode(bd.getLongitude(), bd.getLatitude(), bd.getName(), bd.getAddress()));
                    }
                } else {
                    /* intent中没有节点地址集合，获取终点地址信息 */
                    double startLat = intent.getDoubleExtra("start_latitude", 0);
                    double startlng = intent.getDoubleExtra("start_longitude", 0);
                    String startName = intent.getStringExtra("start_address");
                    double endLat = intent.getDoubleExtra("end_latitude", 0);
                    double endlng = intent.getDoubleExtra("end_longitude", 0);
                    String endName = intent.getStringExtra("end_address");
                    //获取起点位置
                    if (startLat != 0 && startlng != 0 && endLat != 0 && endlng != 0) {
                        calculateRoad = NaviSetLineActivity.CALCULATING;
                        BDLocation startBl = new BDLocation();
                        startBl.setLatitude(startLat);
                        startBl.setLongitude(startlng);
                        startBl.setAddrStr(startName);
                        startBl = LocationClient.getBDLocationInCoorType(startBl, BDLocation.BDLOCATION_BD09LL_TO_GCJ02);
                        BDLocation endBl = new BDLocation();
                        endBl.setLatitude(endLat);
                        endBl.setLongitude(endlng);
                        endBl.setAddrStr(endName);
                        endBl = LocationClient.getBDLocationInCoorType(endBl, BDLocation.BDLOCATION_BD09LL_TO_GCJ02);
                        /* 起点 */
                        BNRoutePlanNode start = new BNRoutePlanNode(startBl.getLongitude(), startBl.getLatitude(), startName, "");
                         /* 终点 */
                        BNRoutePlanNode end = new BNRoutePlanNode(endBl.getLongitude(), endBl.getLatitude(), endName, "");
                        routeInputs = new ArrayList<>();
                        routeInputs.add(start);
                        routeInputs.add(end);
                    }
                    /* intent中没有节点地址集合，获取终点地址信息 */
                    double eLat = intent.getDoubleExtra("latitude", 0);
                    double eLng = intent.getDoubleExtra("longitude", 0);
                    String eName = intent.getStringExtra("address");
                    if (eLat != 0 && eLng != 0) {
                        Log.i(TAG, "intent>>latitude=" + eLat + ",longitude=" + eLng + ",address=" + eName);
                        calculateRoad = NaviSetLineActivity.CALCULATING;
                        BDLocation startBl = new BDLocation();
                        startBl.setLatitude(address.getLatitude());
                        startBl.setLongitude(address.getLongitude());
                        startBl.setAddrStr(address.getAddressDetail());
                        startBl = LocationClient.getBDLocationInCoorType(startBl, BDLocation.BDLOCATION_BD09LL_TO_GCJ02);

                        BDLocation bl = new BDLocation();
                        bl.setLatitude(eLat);
                        bl.setLongitude(eLng);
                        bl.setAddrStr(eName);
                         bl = LocationClient.getBDLocationInCoorType(bl, BDLocation.BDLOCATION_BD09LL_TO_GCJ02);
                        /* 当前定位为起点 */
                        BNRoutePlanNode start = new BNRoutePlanNode(startBl.getLongitude(), startBl.getLatitude(), startBl.getAddrStr(), "");
                        /* 从intent接收到终点 */
                        BNRoutePlanNode end = new BNRoutePlanNode(bl.getLongitude(), bl.getLatitude(), eName, "");
                        routeInputs = new ArrayList<>();
                        routeInputs.add(start);
                        routeInputs.add(end);
                    }
                }
            } else {
                return;
            }
        }
        mSetLineView.showLoading("路线规划中", mContext.getResources().getString(R.string.navi_tips));
        if (routeInputs.size() > 1)
            naviManager.routePlan(routeInputs, calculatePreference);
    }


    @Override
    public void reCalculateRoad(int preference) {
        if (calculatePreference != preference) {
            calculatePreference = preference;
            AppConfig.dPreferences.edit().putInt(NaviSetLineActivity.CALCULATE_MODE, calculatePreference).commit();
            startCalculateRoad();
        }
    }

    @Override
    public void fillRouteModel(Object routeModel, int preference) {
        Vector<RoutePlanModelProxy> v = new Vector<RoutePlanModelProxy>();
        v.add((RoutePlanModelProxy) routeModel);
        /* 获取导航线路数量 */
        int l = BNRoutePlanerProxy.getInstance().getRouteCnt();
        if (l > 0) {
            Bundle bundle = new Bundle();
            BNRoutePlanerProxy.getInstance().getRouteInfo(0, bundle);
            ArrayList<RouteModel> routeModels = new ArrayList<>();
            routeModels.add(new RouteModel(bundle));
            for (int i = 1; i < l; i++) {
                bundle = new Bundle();
                RoutePlanModelProxy rp = new RoutePlanModelProxy();
                BNRoutePlanerProxy.getInstance().getRouteInfo(i, bundle);
                rp.parseRouteResult(mContext, bundle);
                routeModels.add(new RouteModel(bundle));
                v.add(rp);
            }
            /* 将对应算路模式的所有路线信息放入Map集合 */
            RouteModel.put(preference, routeModels);
        }
        BNRoutePlanerProxy.getInstance().routePlans.put(preference, v);
    }

    @Override
    public void initNavi(int preference, int switchLineCaculate) {
        naviCompleted = false;
        mSetLineView.closeLoading();
        Vector<RoutePlanModelProxy> v = new Vector<RoutePlanModelProxy>();
        ArrayList<RouteModel> routeModels = new ArrayList<RouteModel>();
        int l = BNRoutePlanerProxy.getInstance().getRouteCnt();
        if (l > 0) {
            for (int i = 0; i < l; i++) {
                Bundle bundle = new Bundle();
                RoutePlanModelProxy rp = new RoutePlanModelProxy();
                BNRoutePlanerProxy.getInstance().getRouteInfo(i, bundle);
                rp.parseRouteResult(mContext, bundle);
                routeModels.add(new RouteModel(bundle));
                v.add(rp);
            }
            RouteModel.put(preference, routeModels);
        }
        BNRoutePlanerProxy.getInstance().routePlans.put(preference, v);
        if (calculateScheme > (l - 1)) {
            calculateScheme = 0;
        }
        if (calculateScheme != 0) {
            setRouteModeDetail(calculateScheme);
        }
        RouteModel.setCalculateScheme(calculateScheme);
        BNRoutePlanerProxy.getInstance().setCalculateScheme(calculateScheme);
        Log.e(TAG, "calculate road finished!!!");
        calculateRoad = NaviSetLineActivity.CALCULATED;
        if (!isGuiding) {
            setRouteMode();
            /* 填充路线详情信息集合 */
            mSetLineView.updateRouteDetailList(BNRoutePlanerProxy.getInstance().routePlans.get(calculatePreference).get(calculateScheme).getRouteNodeData());
        }
        /* 重置导航节点集合 */
        if (NavigatorService.get() != null)
            NavigatorService.get().resetRouteInputs(routeInputs);
        if (switchLineCaculate > 0) {
            if (reCalculateInNavigate && (calculateType == NaviRouteCalculateEvent.FULL ||
                    (calculateType & NaviRouteCalculateEvent.FULL_WITH_CALCULATE_MODE) == NaviRouteCalculateEvent.FULL_WITH_CALCULATE_MODE)) {
                /* 设置语音文本 */
                StringBuffer sb = new StringBuffer();
                sb.append(NavigatorService.reCalculateRouteLineCompletedTips());
                SpeechMsgBuilder msgBuilder = SpeechMsgBuilder.create(sb.toString());
                msgBuilder.setForceLocalEngine(true);
                /* 合成语音 */
                SynthesizerBase.get().startSpeakAbsolute(msgBuilder.build())
                        .doOnComplete(new Action() {
                            @Override
                            public void run() throws Exception {
                                startNavi(mContext, true);
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation())
                        .subscribe();
            } else if (calculateType == NaviRouteCalculateEvent.FULL && switchFinishedStartImmediately)
                startNavi(mContext, true);
            reCalculateInNavigate = false;
            return;
        }
        StringBuilder text = new StringBuilder();
        Intent eIntent = new Intent(mContext, AssistantService.class);
        boolean switchLine = false;
        if ((calculateType & NaviRouteCalculateEvent.FULL_CHECK_ROUTE_LINE_FOR_NAVIGATE) !=
                NaviRouteCalculateEvent.FULL_CHECK_ROUTE_LINE_FOR_NAVIGATE) {
            int result = 0;
            if ((calculateType & NaviRouteCalculateEvent.FULL_CONTAINS_PASS_POINT) == NaviRouteCalculateEvent.FULL_CONTAINS_PASS_POINT && !TextUtils.isEmpty(passPoint)) {
                result = passPoint(passPoint, NaviRouteCalculateEvent.FULL_CONTAINS_PASS_POINT);
                if ((result & 7) == 1) {
                    eIntent.putExtra(AssistantService.TEXT, "导航路线中无该途经点");
                    eIntent.putExtra(AssistantService.CALLBACK, true);
                } else {
                    NavigatorService.get().addPassPoint(passPoint);
                }
            } else if ((calculateType & NaviRouteCalculateEvent.FULL_CONTAINS_EXISTENT_PASS_POINT) == NaviRouteCalculateEvent.FULL_CONTAINS_EXISTENT_PASS_POINT && !TextUtils.isEmpty(passPoint)) {
                result = passPoint(passPoint, NaviRouteCalculateEvent.FULL_CONTAINS_PASS_POINT);
                if ((result & 7) == 1) {
                    eIntent.putExtra(AssistantService.TEXT, "导航路线中无该途经点");
                    eIntent.putExtra(AssistantService.CALLBACK, true);
                } else if (!NavigatorService.get().routeInputsContains(passPoint)) {
                    NavigatorService.get().addPassPoint(passPoint);
                }
            } else if ((calculateType & NaviRouteCalculateEvent.RECALCULATE_ADD_POINT_IN_NAVIGATE) == NaviRouteCalculateEvent.RECALCULATE_ADD_POINT_IN_NAVIGATE && !TextUtils.isEmpty(passPoint)) {
                result = passPoint(passPoint, NaviRouteCalculateEvent.FULL_CONTAINS_PASS_POINT);
                if ((result & 7) == 1) {
                    eIntent.putExtra(AssistantService.TEXT, "重新规划的路线中无该途经点");
                    eIntent.putExtra(AssistantService.CALLBACK, true);
                } else {
                    NavigatorService.get().addPassPoint(passPoint);
                }
            } else if ((calculateType & NaviRouteCalculateEvent.FULL_CONTAINS_NOT_PASS_POINT) == NaviRouteCalculateEvent.FULL_CONTAINS_NOT_PASS_POINT && !TextUtils.isEmpty(passPoint)) {
                result = passPoint(passPoint, NaviRouteCalculateEvent.FULL_CONTAINS_NOT_PASS_POINT);
                if ((result & 7) == 2) {
                    eIntent.putExtra(AssistantService.TEXT, "导航路线中无法避开该途经点");
                    eIntent.putExtra(AssistantService.CALLBACK, true);
                } else {
                    NavigatorService.LastAvoidPoint = passPoint;
                }
            }
            if (result == 0) {
                eIntent.putExtra(AssistantService.TEXT, NaviSetLineActivity.NAVI_ROUTE_CALCULATED);
            } else if (TextUtils.isEmpty(eIntent.getStringExtra(AssistantService.TEXT))) {
                if (result >> 6 == calculatePreference) {
                    if (((result & 56) >> 3) != calculateScheme) {//切换方案
                        setRouteModeDetail((result & 56) >> 3);
                    }
                } else {//切换路线
                    switchLine = true;
                    calculatePreference = result >> 6;
                    calculateScheme = (result & 56) >> 3;
                }
                eIntent.putExtra(AssistantService.TEXT, NaviSetLineActivity.NAVI_ROUTE_CALCULATED);
            }
            if (!isGuiding && NaviSetLineActivity.NAVI_ROUTE_CALCULATED.equals(eIntent.getStringExtra(AssistantService.TEXT))) {

            } else {
                eIntent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PUSH_ROUTE_CACULATE);
                mContext.startService(eIntent);
            }
        }
        if (SynthesizerBase.isInited() && !isGuiding) {
            SpeechMsgBuilder msgBuilder = SpeechMsgBuilder.create("");
            /* 设置合成结束后自动开启识别 */
            msgBuilder.setContextMode(SpeechMsg.CONTEXT_KEEP_RECOGNIZE);
            if ((calculateType & NaviRouteCalculateEvent.FULL_CHECK_ROUTE_LINE_FOR_NAVIGATE) ==
                    NaviRouteCalculateEvent.FULL_CHECK_ROUTE_LINE_FOR_NAVIGATE) {//检测到达某个目的地的路况，询问是否需要导航
                text.append("去").append(routeInputs.get(routeInputs.size() - 1).getName()).append("的路");
                NavigatorService.setMaxTwoPointConjestion(text);
                text.append("。如果需要导航，请对我说需要");
                msgBuilder.setForceLocalEngine(true)
                        .setPriority(SpeechMsg.PRIORITY_ABOVE_RECOGNIZE)
                        .setRetryTimes(1)
                        .setText(text.toString());
                SynthesizerBase.get().startSpeakAbsolute(msgBuilder.build())
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation())
                        .subscribe();
                return;
            }

            if (NaviSetLineActivity.NAVI_ROUTE_CALCULATED.equals(eIntent.getStringExtra("text"))) {
                if ((calculateType & NaviRouteCalculateEvent.TARGET_IS_FAVORITE_POINT) == NaviRouteCalculateEvent.TARGET_IS_FAVORITE_POINT) {
                    text.append("正在为你规划路线，找到你收藏的").append(routeInputs.get(routeInputs.size() - 1).getName()).append(",");
                }
                switch (calculateType & (NaviRouteCalculateEvent.TARGET_IS_FAVORITE_POINT - 1)) {
                    case NaviRouteCalculateEvent.FULL_CONTAINS_EXISTENT_PASS_POINT:
                    case NaviRouteCalculateEvent.FULL_CONTAINS_PASS_POINT:
                    case NaviRouteCalculateEvent.RECALCULATE_ADD_POINT_IN_NAVIGATE:
                        text.append("路线规划成功,途经")
                                .append(NavigatorService.get().getPassPointString())
                                .append(",全程")
                                .append(BNRoutePlanerProxy.getInstance().routePlans.get(calculatePreference).get(calculateScheme).getTotalDistance())
                                .append(",预计")
                                .append(BNRoutePlanerProxy.getInstance().routePlans.get(calculatePreference).get(calculateScheme).getTotalTime())
                                .append(countDownStartNavi ? ",十秒后开始导航。" : "。");
                        break;
                    case NaviRouteCalculateEvent.FULL_CONTAINS_NOT_PASS_POINT:
                        text.append("路线规划成功,没有经过")
                                .append(passPoint)
                                .append(",全程")
                                .append(BNRoutePlanerProxy.getInstance().routePlans.get(calculatePreference).get(calculateScheme).getTotalDistance())
                                .append(",预计")
                                .append(BNRoutePlanerProxy.getInstance().routePlans.get(calculatePreference).get(calculateScheme).getTotalTime())
                                .append("。十秒后开始导航。");
                        break;
                    case NaviRouteCalculateEvent.FULL_WITH_CALCULATE_MODE: {
                        switch (calculatePreference) {
                            case BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM:
                                text.append("路线规划成功,已尽可能躲避拥堵")
                                        .append(",全程")
                                        .append(BNRoutePlanerProxy.getInstance().routePlans.get(calculatePreference).get(calculateScheme).getTotalDistance())
                                        .append(",预计")
                                        .append(BNRoutePlanerProxy.getInstance().routePlans.get(calculatePreference).get(calculateScheme).getTotalTime())
                                        .append("。十秒后开始导航。");
                                break;
                            case BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_DIST | BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM:
                                text.append("路线规划成功,已选择最短路线")
                                        .append(",全程")
                                        .append(BNRoutePlanerProxy.getInstance().routePlans.get(calculatePreference).get(calculateScheme).getTotalDistance())
                                        .append(",预计")
                                        .append(BNRoutePlanerProxy.getInstance().routePlans.get(calculatePreference).get(calculateScheme).getTotalTime())
                                        .append("。十秒后开始导航。");
                                break;
                            case BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TIME | BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM:
                                text.append("路线规划成功,已选择最快路线")
                                        .append(",全程")
                                        .append(BNRoutePlanerProxy.getInstance().routePlans.get(calculatePreference).get(calculateScheme).getTotalDistance())
                                        .append(",预计")
                                        .append(BNRoutePlanerProxy.getInstance().routePlans.get(calculatePreference).get(calculateScheme).getTotalTime())
                                        .append("。十秒后开始导航。");
                                break;
                            case BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TOLL | BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM:
                                text.append("路线规划成功,已选择收费最少的路线")
                                        .append(",全程")
                                        .append(BNRoutePlanerProxy.getInstance().routePlans.get(calculatePreference).get(calculateScheme).getTotalDistance())
                                        .append(",预计")
                                        .append(BNRoutePlanerProxy.getInstance().routePlans.get(calculatePreference).get(calculateScheme).getTotalTime())
                                        .append("。十秒后开始导航。");
                                break;
                            default:
                                break;
                        }
                        break;
                    }
                    default:
                        if (!TextUtils.isEmpty(text)) {
                            text.append("已为你规划路线，")
                                    .append(",全程")
                                    .append(BNRoutePlanerProxy.getInstance().routePlans.get(calculatePreference).get(calculateScheme).getTotalDistance())
                                    .append(",预计")
                                    .append(BNRoutePlanerProxy.getInstance().routePlans.get(calculatePreference).get(calculateScheme).getTotalTime())
                                    .append("。");
                            int tipsCount = AppConfig.getSettingInt(AppConfig.NAVI_TIPS_COUNT, 0);
                            if (tipsCount < 5) {
                                if (tipsCount % 2 == 0) {
                                    text.append("十秒后自动按推荐路线位你导航。").append(preStartTips[new Random().nextInt(preStartTips.length)]);
                                }
                                AppConfig.setSettingInt(AppConfig.NAVI_TIPS_COUNT, ++tipsCount);
                            }
                        }
                        break;
                }
            } else if (!TextUtils.isEmpty(eIntent.getStringExtra("text"))) {
                return;
            }
            /* 首次导航 */
            if (TextUtils.isEmpty(text)) {
                text.append("路线规划成功，全程").
                        append(BNRoutePlanerProxy.getInstance().routePlans.get(calculatePreference).get(calculateScheme).getTotalDistance()).
                        append(",途经").
                        append(BNRoutePlanerProxy.getInstance().routePlans.get(calculatePreference).get(calculateScheme).getMainRoads()).
                        append(",预计").
                        append(BNRoutePlanerProxy.getInstance().routePlans.get(calculatePreference).get(calculateScheme).getTotalTime()).
                        append("。");
                int tipsCount = AppConfig.getSettingInt(AppConfig.NAVI_TIPS_COUNT, 0);
                if (tipsCount < 5) {
                    if (tipsCount % 2 == 0) {
                        text.append("十秒后自动按推荐路线位你导航。").append(preStartTips[new Random().nextInt(preStartTips.length)]);
                    }
                    AppConfig.setSettingInt(AppConfig.NAVI_TIPS_COUNT, ++tipsCount);
                }
            }
            if (switchLine)
                naviManager.routePlanSingle(routeInputs, calculatePreference);
            final SpeechMsg msg = msgBuilder.setText(text.toString()).setForceLocalEngine(true).build();
            Observable<SpeechMsg> msgObservable = SynthesizerBase.get().addMessageWaitSpeak(msg);
            if (msgObservable != null) {
                msgObservable.doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        /* 语音合成结束，还未开始开始导航，开始倒计时；若计时完毕则开始导航 */
                        if (!BNavigatorProxy.getInstance().isNaviBegin() && msg.state() == SpeechMsg.State.Completed) {
                            if (countDownStartNavi)
                                startCountDown();
                            else
                                startNavi(mContext, true);
                        }
                    }
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation())
                        .subscribe();
            }
        }
    }

    @Override
    public void setNaviStatus(String status) {
        //更新导航引擎状态
        mNavigation.setStatus(status);
        uploadNavigation();
    }

    /**
     * 检查是否经过某个点
     *
     * @param point
     * @return calculateMode<<6|calculateScheme<<3|result
     */
    private int passPoint(String point, int cType) {
        int l;
        int cPrefrence = calculatePreference;
        int cScheme = calculateScheme;
        Map<Integer, Vector<RoutePlanModelProxy>> routePlans = BNRoutePlanerProxy.getInstance().routePlans;
        int count = 0;
        Vector<RoutePlanModelProxy> routePlanModels = routePlans.get(cPrefrence);
        l = routePlanModels.size();
        RoutePlanModelProxy routePlanModelTemp = routePlanModels.get(cScheme);
        for (RoutePlanResultItemProxy item : routePlanModelTemp.getRouteNodeData()) {
            if (item.getNextRoadName().contains(point)) {
                count++;
                cScheme = BNRoutePlanerProxy.getInstance().getCalculateScheme();
                break;
            } else {
                cScheme = -1;
            }
        }
        if ((cType == NaviRouteCalculateEvent.FULL_CONTAINS_PASS_POINT && cScheme != -1) ||
                (cType == NaviRouteCalculateEvent.FULL_CONTAINS_NOT_PASS_POINT && cScheme == -1)) {
            return (cPrefrence << 6) | (calculateScheme << 3) | 0;
        }

        for (int i = 0; i < l; i++) {//检查当前线路的其它方案是否包含该point
            if (i != calculateScheme) {
                for (RoutePlanResultItemProxy item : routePlanModels.get(i).getRouteNodeData()) {
                    if (item.getNextRoadName().indexOf(point) != -1) {
                        count++;
                        if (cScheme < 0)
                            cScheme = i;
                        break;
                    } else {
                        cScheme = -1;
                    }
                }
                if ((cType == NaviRouteCalculateEvent.FULL_CONTAINS_PASS_POINT && cScheme != -1) ||
                        (cType == NaviRouteCalculateEvent.FULL_CONTAINS_NOT_PASS_POINT && cScheme == -1)) {
                    return (cPrefrence << 6) | (i << 3) | 0;
                }
            }
        }


        for (Integer key : routePlans.keySet()) {//检查其它算路方式下的线路是否包含该point
            if (key == calculatePreference)
                continue;
            cPrefrence = key;
            l += routePlans.get(key).size();
            for (int i = 0; i < routePlans.get(key).size(); i++) {
                for (RoutePlanResultItemProxy item : routePlans.get(key).get(i).getRouteNodeData()) {
                    if (item.getNextRoadName().indexOf(point) != -1) {
                        count++;
                        if (cScheme < 0)
                            cScheme = i;
                        break;
                    } else {
                        cScheme = -1;
                    }
                }
                if ((cType == NaviRouteCalculateEvent.FULL_CONTAINS_PASS_POINT && cScheme != -1) ||
                        (cType == NaviRouteCalculateEvent.FULL_CONTAINS_NOT_PASS_POINT && cScheme == -1)) {
                    return (cPrefrence << 6) | (i << 3) | 0;
                }
            }

        }

        if (cScheme == -1) {
            cPrefrence = calculatePreference;
            cScheme = calculateScheme;
        }

        if (count <= 0) {
            return (cPrefrence << 6) | (cScheme << 3) | 1;
        } else if (count < l) {
            return (cPrefrence << 6) | (cScheme << 3) | 0;
        } else {
            return (cPrefrence << 6) | (cScheme << 3) | 2;
        }
    }

    @Override
    public void startNavi(Context context, boolean isGPSNav) {
        if (mLaunchConfigParams == null)
            this.mLaunchConfigParams = new Bundle();
        else
            this.mLaunchConfigParams.clear();

        /* 填充导航信息参数 */
        this.mLaunchConfigParams.putInt("routeguide_view_mode", 0);
        this.mLaunchConfigParams.putInt("calroute_done", 0);
        if (routeInputs == null) {
            if (NavigatorService.get().getRouteInputs().size() > 0) {
                routeInputs = (ArrayList<BNRoutePlanNode>) NavigatorService.get().getRouteInputs();
            }
            RoutePlanModelProxy mRoutePlanModel = RoutePlanModelProxy.getCacheRoutePlanModelProxy("RoutePlanModel");
            List<BNRoutePlanNode> list = mRoutePlanModel.getRouteInput();
            if (list.size() > 1) {
                GeoPointProxy temp = BNRoutePlanNodeGenerator.getInstance().getRoutePlanGeoPoint(routeInputs.get(0));
                this.mLaunchConfigParams.putInt("start_x", temp.getLongitudeE6());
                this.mLaunchConfigParams.putInt("start_y", temp.getLatitudeE6());
                temp = BNRoutePlanNodeGenerator.getInstance().getRoutePlanGeoPoint(routeInputs.get(routeInputs.size() - 1));
                this.mLaunchConfigParams.putInt("end_x", temp.getLongitudeE6());
                this.mLaunchConfigParams.putInt("end_y", temp.getLatitudeE6());
                this.mLaunchConfigParams.putString("start_name", routeInputs.get(0).getName());
                this.mLaunchConfigParams.putString("end_name", routeInputs.get(routeInputs.size() - 1).getName());
            } else {
                return;
            }
        } else {
            this.mLaunchConfigParams.putInt("start_x", (int) (routeInputs.get(0).getLongitude() * 1e5));
            this.mLaunchConfigParams.putInt("start_y", (int) (routeInputs.get(0).getLatitude() * 1e5));
            this.mLaunchConfigParams.putInt("end_x", (int) (routeInputs.get(routeInputs.size() - 1).getLongitude() * 1e5));
            this.mLaunchConfigParams.putInt("end_y", (int) (routeInputs.get(routeInputs.size() - 1).getLatitude() * 1e5));
            this.mLaunchConfigParams.putString("start_name", routeInputs.get(0).getName());
            this.mLaunchConfigParams.putString("end_name", routeInputs.get(routeInputs.size() - 1).getName());
        }
        this.mLaunchConfigParams.putBoolean("road_condition", true);
        if (!isGPSNav) {
            this.mLaunchConfigParams.putInt("locate_mode", 2);  //模拟导航
        } else {
            this.mLaunchConfigParams.putInt("locate_mode", 1);  //GPS导航
        }

        this.mLaunchConfigParams.putBoolean("net_refresh", true);
        if ((system_is_wakeup_mode & 2) == 0)
            system_is_wakeup_mode = VoiceMediator.get().isWakeUpMode() ? 3 : 2;
        if (!RouteGuider.getAsJar()) {
            //Toast.makeText(context, "百度导航资源加载失败！", Toast.LENGTH_LONG).show();
            mSetLineView.showSnackBar("百度导航资源加载失败");
            if (SynthesizerBase.isInited()) {
                SpeechMsgBuilder msgBuilder = SpeechMsgBuilder.create("百度导航资源加载失败！");
                msgBuilder.setForceLocalEngine(true);
                SynthesizerBase.get().startSpeakAbsolute(msgBuilder.build())
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation())
                        .subscribe();
            }
        } else {
            // LingjuAudioPlayer.create(mContext).requestAudioFocus();
            /** 导航时进入唤醒模式 **/
            if (SynthesizerBase.isInited()) {
                VoiceMediator.get().setWakeupModeFlag(true);
                /* 停止合成 */
                if (SynthesizerBase.get().isSpeaking()) {
                    SynthesizerBase.get().stopSpeakingAbsolte();
                }
                /* 停止识别并尝试打开唤醒 */
                Intent intent = new Intent(mContext, AssistantService.class);
                intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_RECOGNIZE);
                mContext.startService(intent);
                mSetLineView.setVoiceBtnIdleState();
            }
            mHandler.sendEmptyMessage(NaviSetLineActivity.MSG_START_NAVI);
        }
    }

    @Override
    public void hanldeNaviView() {
        isGuiding = true;
        //设置导航播报模式 (Veteran = 1，老手（简洁）;Novice = 0，新手（详细）;Quite = 2，静音;)。默认是新手详情播报
        // BNaviSettingManager.setVoiceMode(BNaviSettingManager.VoiceMode.Novice);
        mNaviView = getRouteGuideView(mContext, mLaunchConfigParams, new OnNavigationListenerProxy() {

            @Override
            public void onNaviGuideEnd() {
                Log.i(TAG, "onNaviGuideEnd");
                // VoiceMediator.get().setWakeupModeFlag((system_is_wakeup_mode & 1) == 1);
                if (!reCalculateInNavigate) {
                    naviCompleted = true;
                    /* 导航结束后，进行一次定位 */
                    BaiduLocateManager.get().addObserver(locateListener);
                    BaiduLocateManager.get().start();

                    if (!stop_in_backgound && !BaiduNaviSuperManager.getDefaultPlayerCallback().isInCallTask()) {
                        Intent it = new Intent(mContext, AssistantService.class);
                        it.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PUSH_ROUTE_CACULATE);
                        it.putExtra("text", "取消导航");
                        mContext.startService(it);
                    }
                    mSetLineView.goBack();
                    SynthesizerBase.get().setForceLocalEngine(false);
                } else if (mMapView != null) {
                    mSetLineView.setMapView(mMapView, true);
                    BNMapControllerProxy c = BNMapControllerProxy.getInstance();
                    c.setLayerMode(MapParams.LayerMode.MAP_LAYER_MODE_CRUISE);

                    MapStatusProxy ms = c.getMapStatus();

                    ms._Level = 17.0F;
                    c.setMapStatus(ms, BNMapControllerProxy.AnimationType.eAnimationLevel);
                }
            }

            @Override
            public void notifyOtherAction(int actionType, int arg1, int arg2, Object obj) {

            }
        });
        mSetLineView.setMapView(mNaviView, true);
        if (mNaviView != null) {
            if (mLaunchConfigParams.getInt("locate_mode", 1) == 1)
                BNStatisticsManagerProxy.getInstance().onEventWithParamList(50010, null, new ArrayList());
            /* 开始导航 */
            BNavigatorProxy.getInstance().startNav(null);
            BaiduNaviSuperManager.setSelectLineState(false);
            // NavigatorService.get().reset();
            SynthesizerBase.get().setForceLocalEngine(true);
            //该方法可以在导航界面可见时检测是否打开GPS
            //（由于路线规划和开始导航共用界面，在开始导航时没有触发该方法，需要自己调用一次）
            BNavigatorProxy.getInstance().resume();
            //更新导航引擎状态
            setNaviStatus(RobotConstant.NaviStatus.OPEN.toString());
        }
        reCalculateInNavigate = false;

        BNavigatorProxy.getInstance().setListener(bNavigatorListener);
    }

    /**
     * 获取导航引导视图
     **/
    private View getRouteGuideView(Activity activity, Bundle configParams, OnNavigationListenerProxy listener) {
        if (activity != null && configParams != null) {
            try {
                mSetLineView.setMapView(mMapView, false);
                /* 导航航视图：类似于地图SDK中的MapView，提供Navigator对象，可以方便的集成到自定义UI框架中
                   负责导航过程页中的视图管理、交互逻辑、引擎消息及数据刷新等一系列导航过程中的事务  */
                BNavigatorProxy.getInstance().setNavigationListener(listener);
                return BNavigatorProxy.getInstance().init(activity, configParams, mMapView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void location() {
        BNMapControllerProxy.getInstance().locateWithAnimation((int) (address.getLongitude() * 1e5), (int) (address.getLatitude() * 1e5));
    }

    @Override
    public void MapZoomIn() {
        BNMapControllerProxy.getInstance().zoomIn();
    }

    @Override
    public void MapZoomOut() {
        BNMapControllerProxy.getInstance().zoomOut();
    }

    @Override
    public void showTraffic() {
        showTraffic = !showTraffic;
        BNMapControllerProxy.getInstance().showTrafficMap(showTraffic);
        mSetLineView.setTrafficIcon(showTraffic);
    }

    @Override
    public void checkPassedPassPoint() {
        if (NavigatorService.get().getRouteInputs().size() > 2) {
            if (TextUtils.isEmpty(passPoint) && routeInputs.size() == 2) {
                NavigatorService.get().clearRouteInputs();
                return;
            }
            RouteModel routeModel = RouteModel.getCurrent();
            int curDistance = (int) (RGAssistGuideModelProxy.getInstance().getCarProgress() * routeModel.getDistance() + 50);
            RouteModel.RouteNode n;
            for (int i = 0; i < routeModel.getNodes().size(); i++) {
                if ((n = routeModel.getNodes().get(i)).getDistanceFromStart() >= curDistance) {
                    break;
                } else {
                    if (n.getTurnString().indexOf("途经点") != -1) {
                        NavigatorService.get().removePassPointHasCoordinate();
                    } else if (null != passPoint && n.getTurnString().indexOf(passPoint) != -1) {
                        NavigatorService.get().removePassPointWithoutCoordinate(passPoint);
                    }
                }
            }
        }
        if ((calculateType & NaviRouteCalculateEvent.FULL_CONTAINS_NOT_PASS_POINT) == NaviRouteCalculateEvent.FULL_CONTAINS_NOT_PASS_POINT
                && !TextUtils.isEmpty(passPoint)) {
            NavigatorService.LastAvoidPoint = passPoint;
        } else {
            NavigatorService.LastAvoidPoint = null;
        }
    }

    @Override
    public void showFullLine() {
        if (routeInputs.size() > 1) {
            View mTotalFullviewPanel = RGMapModeViewControllerProxy.getInstance().getView().findViewById(1711866150);
            if (mTotalFullviewPanel != null) {
                Log.i(TAG, "mTotalFullviewPanel.callOnClick()");
                mTotalFullviewPanel.callOnClick();
            } else {
                naviManager.zoomToRouteBound(routeInputs.get(0), routeInputs.get(routeInputs.size() - 1));
            }
        }
    }

    @Override
    public void setStopInBackGround(boolean isNavi) {
        stop_in_backgound = isNavi;
    }

    @Override
    public void showNaviGuide() {
        BaiduNaviSuperManager.setSelectLineState(true);
        mixCalculateType = calculateType = 0;
        calculateRoad = NaviSetLineActivity.CALCULATED_SHOW;
        calculatePreference = BNRoutePlanerProxy.getInstance().getCalcPreference();
        calculateScheme = RouteModel.getCalculateScheme();
        if (NavigatorService.get().getRouteInputs().size() > 1) {
            routeInputs = (ArrayList<BNRoutePlanNode>) NavigatorService.get().getRouteInputs();
            naviManager.zoomToRouteBound(routeInputs.get(0), routeInputs.get(routeInputs.size() - 1));
        }
        setRouteMode();
    }

    @Override
    public void setWakeupFlag() {
        system_is_wakeup_mode = VoiceMediator.get().isWakeUpMode() ? 3 : 2;
        VoiceMediator.get().setWakeupModeFlag(true);
    }

    @Override
    public void routeCalculate(NaviRouteCalculateEvent e) {
        Log.i(TAG, "路线类型>>" + e.getType());
        //voiceMode=true;
        if (e.getType() == NaviRouteCalculateEvent.SINGLE_PASS) {
            countDownStartNavi = false;
            calculateType = NaviRouteCalculateEvent.FULL_CONTAINS_EXISTENT_PASS_POINT;
            mixCalculateType |= calculateType;
            Log.i(TAG, "routeInputs.size=" + NavigatorService.get().getRouteInputs().size());
            while (NavigatorService.get().getRouteInputs().size() > 3) {
                NavigatorService.get().getRouteInputs().remove(1);
            }
            ArrayList<BaiduAddress> addresses = e.getPoints();
            if (addresses.size() == 0 || NavigatorService.get().getRouteInputs().size() < 2)
                return;
            NavigatorService.get().getRouteInputs().add(NavigatorService.get().getRouteInputs().size() - 1, new BNRoutePlanNode(addresses.get(0).getLongitude(),
                    addresses.get(0).getLatitude(),
                    addresses.get(0).getName(),
                    addresses.get(0).getAddress()));
            routeInputs.clear();
            routeInputs.addAll(NavigatorService.get().getRouteInputsHasCoordinate());
            passPoint = NavigatorService.get().getPassPointNoCoordinate();
        } else if (e.getType() == NaviRouteCalculateEvent.RECALCULATE_ADD_POINT_IN_NAVIGATE) {
            countDownStartNavi = false;
            passPoint = e.getPassPoint();
            mixCalculateType |= (calculateType = e.getType());
            if (resetRouteInputForRecalculateInNavigate()) {
                reCalculateInNavigate = true;
                countDownStartNavi = false;
                BNavigatorProxy.getInstance().forceQuitWithoutDialog();
                RouteModel.getLastRouteModels().clear();
                BNRoutePlanerProxy.getInstance().routePlans.clear();
            } else {
                if (SynthesizerBase.isInited()) {
                    speakMsg("很抱歉我暂时没法定位您的位置，请到开阔地带再试一次吧");
                }
                return;
            }
        } else if (e.getType() == NaviRouteCalculateEvent.SINGLE_TARGET) {
            countDownStartNavi = true;
            calculateType = calculateType & (NaviRouteCalculateEvent.TARGET_IS_FAVORITE_POINT - 1);
            mixCalculateType = mixCalculateType & (NaviRouteCalculateEvent.TARGET_IS_FAVORITE_POINT - 1);
            if (routeInputs == null) {
                routeInputs = new ArrayList<>();
            }
            if (routeInputs.size() == 0) {
                BNRoutePlanNode start = new BNRoutePlanNode(address.getLongitude(), address.getLatitude(), address.getAddressDetail(), address.getAddressDetail());
                routeInputs.add(start);
            } else {
                routeInputs.remove(routeInputs.size() - 1);
            }
            routeInputs.add(new BNRoutePlanNode(e.getPoints().get(0).getLongitude(),
                    e.getPoints().get(0).getLatitude(),
                    e.getPoints().get(0).getName(),
                    e.getPoints().get(0).getAddress()));
        } else {
            countDownStartNavi = true;
            mixCalculateType = calculateType = e.getType();
            if ((calculateType & NaviRouteCalculateEvent.FULL_CONTAINS_PASS_POINT) == NaviRouteCalculateEvent.FULL_CONTAINS_PASS_POINT ||
                    (calculateType & NaviRouteCalculateEvent.FULL_CONTAINS_NOT_PASS_POINT) == NaviRouteCalculateEvent.FULL_CONTAINS_NOT_PASS_POINT)
                passPoint = e.getPassPoint();
            if ((calculateType & NaviRouteCalculateEvent.FULL_WITH_CALCULATE_MODE) == NaviRouteCalculateEvent.FULL_WITH_CALCULATE_MODE) {
                calculatePreference = e.getCalculateMode();
            }

            routeInputs.clear();
            if (BNavigatorProxy.getInstance().isNaviBegin()) {
                reCalculateInNavigate = true;
                BNavigatorProxy.getInstance().forceQuitWithoutDialog();
                RouteModel.getLastRouteModels().clear();
                BNRoutePlanerProxy.getInstance().routePlans.clear();
            }
            ArrayList<BaiduAddress> addresses = e.getPoints();
            if (addresses.size() == 0)
                return;
            for (BaiduAddress bd : addresses) {
                routeInputs.add(new BNRoutePlanNode(bd.getLongitude(), bd.getLatitude(), bd.getName(), bd.getAddress()));
            }
        }
        if (SynthesizerBase.isInited() && calculateType == NaviRouteCalculateEvent.FULL) {
            speakMsg("正在计算路线");
        }
        isGuiding = false;
        mSetLineView.showLoading("路线规划中", mContext.getResources().getString(R.string.navi_tips));
        if (routeInputs.size() > 1)
            naviManager.routePlan(routeInputs, calculatePreference);
    }

    @Override
    public void switchRouteLine(NaviSwitchRouteLineEvent e) {
        mixCalculateType = calculateType = NaviRouteCalculateEvent.FULL;
        switchFinishedStartImmediately = e.isStartImmediately();
        isGuiding = false;
        if (!BNavigatorProxy.getInstance().isNaviBegin()) {
            int pi = e.getPrefrence();
            if (pi >> 3 == calculatePreference) {
                setRouteModeDetail(pi & 7);
                //待改
                if (e.isStartImmediately()) {
                    startNavi(mContext, true);
                }
            } else {
                calculatePreference = pi >> 3;
                calculateScheme = pi & 7;
                mSetLineView.showLoading("路线规划中", mContext.getResources().getString(R.string.navi_tips));
                naviManager.routePlanSingle(routeInputs, calculatePreference);
            }
        } else {
            if (resetRouteInputForRecalculateInNavigate()) {
                calculatePreference = e.getPrefrence() >> 3;
                calculateScheme = e.getPrefrence() & 7;
                reCalculateInNavigate = true;
                BNavigatorProxy.getInstance().forceQuitWithoutDialog();
                RouteModel.getLastRouteModels().clear();
                BNRoutePlanerProxy.getInstance().routePlans.clear();
                naviManager.routePlanSingle(routeInputs, calculatePreference);
            } else {
                if (SynthesizerBase.isInited()) {
                    speakMsg("很抱歉我暂时没法定位您的位置，请到开阔地带再试一次吧");
                }
            }
        }
    }

    @Override
    public void handleMapCmd(MapCmdEvent e) {
        switch (e.getCmd()) {
            case MapCmdEvent.SHOW_TRAFFIC:
                showTraffic();
                break;
            case MapCmdEvent.ZOOM_IN:
                BNMapControllerProxy.getInstance().zoomIn();
                break;
            case MapCmdEvent.ZOOM_OUT:
                BNMapControllerProxy.getInstance().zoomOut();
                break;
            case MapCmdEvent.ZOOM:
                if (e.getValue() > 0) {
                    MapStatusProxy ms = BNMapControllerProxy.getInstance().getMapStatus();
                    ms._Level = e.getValue();
                    BNMapControllerProxy.getInstance().setMapStatus(ms, BNMapControllerProxy.AnimationType.eAnimationLevel);
                }
                break;
        }
    }

    /**
     * 合成指定文本
     **/
    private void speakMsg(String text) {
        SpeechMsgBuilder msgBuilder = SpeechMsgBuilder.create(text).setForceLocalEngine(true);
        SynthesizerBase.get().startSpeakAbsolute(msgBuilder.build())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe();
    }

    private boolean resetRouteInputForRecalculateInNavigate() {
        LocData locData = BNSysLocationManagerProxy.getInstance().getCurLocation();
        if (locData != null) {
            RouteModel routeModel = RouteModel.getCurrent();
            int curDistance = (int) (RGAssistGuideModelProxy.getInstance().getCarProgress() * routeModel.getDistance() + 50);
            BNRoutePlanNode target = routeInputs.get(routeInputs.size() - 1);
            routeInputs.remove(0);
            RouteModel.RouteNode n;
            String curName = RGHighwayModelProxy.getInstance().getCurRoadName();
            if (routeInputs.size() > 1) {
                String temp = passPoint;
                for (int i = 0; i < routeModel.getNodes().size(); i++) {
                    if (TextUtils.isEmpty(temp))
                        temp = routeInputs.get(0).getName();
                    if ((n = routeModel.getNodes().get(i)).getDistanceFromStart() >= curDistance) {
                        break;
                    } else {
                        if ((n.getTurnString().indexOf("途经点") != -1 || n.getTurnString().indexOf(temp) != -1) && routeInputs.size() > 0) {
                            routeInputs.remove(0);
                            if (routeInputs.size() == 1)
                                break;
                            else
                                temp = routeInputs.get(0).getName();
                        }
                    }
                }
            }
            if (routeInputs.size() == 0) {//为防止意外把所有得点都remove
                routeInputs.add(target);
            }
            BNRoutePlanNode start = new BNRoutePlanNode(locData.longitude, locData.latitude, curName, null);
            routeInputs.add(0, start);
            return true;
        }
        return false;
    }

    @Override
    public void onStop() {
        if (BNavigatorProxy.getInstance().isNaviBegin())
            BNavigatorProxy.getInstance().stop();
    }

    @Override
    public void onResume() {
        if (BNavigatorProxy.getInstance().isNaviBegin()) {
            BNavigatorProxy.getInstance().resume();
            //更新导航引擎状态
            setNaviStatus(RobotConstant.NaviStatus.OPEN.toString());
        }
        BNMapControllerProxy.getInstance().onResume();
        WindowManager.LayoutParams params = mContext.getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        mContext.getWindow().setAttributes(params);
    }

    @Override
    public void onPause() {
        if (BNavigatorProxy.getInstance().isNaviBegin()) {
            BNavigatorProxy.getInstance().pause();
            //更新导航引擎状态
            setNaviStatus(RobotConstant.NaviStatus.PAUSE.toString());
        }
        BNMapControllerProxy.getInstance().onPause();
    }

    @Override
    public void backPressed() {
        if (BNavigatorProxy.getInstance().isNaviBegin()) {
            // 如果正在导航，关闭视图，退出导航(不弹出提示框)
            BNavigatorProxy.getInstance().onBackPressed();
        } else
            mSetLineView.goBack();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (BNavigatorProxy.getInstance().isNaviBegin())
            BNavigatorProxy.getInstance().onConfigurationChanged(newConfig);
    }

    @Override
    public void onDestroy() {
        BaiduNaviSuperManager.setSelectLineState(false);
        Intent intent = new Intent(mContext, AssistantService.class);
        intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_RECOGNIZE);
        mContext.startService(intent);
        stopCountDown();
        VoiceMediator.get().setWakeUpMode((system_is_wakeup_mode & 1) == 1);
        if (SynthesizerBase.isInited() && SynthesizerBase.get().isSpeaking() &&
                null != SynthesizerBase.get().getCurrentMessage()) {
            SynthesizerBase.get().stopSpeakingAbsolte();
        }
        if (!isGuiding) {
            System.out.println("mMapView>>>>>>>>>>>>>>>");
            mSetLineView.setMapView(mMapView, false);
        } /*else {
            LingjuAudioPlayer.get().abandonAudioFocus();
        }*/
        if (!naviCompleted) {
            Intent it = new Intent(mContext, AssistantService.class);
            it.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PUSH_ROUTE_CACULATE);
            it.putExtra("text", "取消导航");
            mContext.startService(it);
        }
        //更新导航引擎状态
        mNavigation = null;
        uploadNavigation();
        cancelDisposable();
        BaiduNaviSuperManager.destory();
    }

    /**
     * 定位结果回调
     **/
    private LocateListener locateListener = new LocateListener(LocateListener.CoorType.BD09LL) {
        @Override
        public void update(Address address) {
            NaviSetLinePresenter.this.address = address;
            appConfig.address = address;
        }

    };

    /**
     * 导航视图变化监听器
     **/
    private IBNavigatorListenerProxy bNavigatorListener = new IBNavigatorListenerProxy() {
        @Override
        public void onPageJump(int i, Object o) {
        }

        @Override
        public void notifyStartNav() {
            Log.i(TAG, "notifyStartNav");
        }

        /**  偏航规划开始 **/
        @Override
        public void onYawingRequestStart() {
            Log.i(TAG, "onYawingRequestStart");
            if (SynthesizerBase.isInited()) {
                SpeechMsgBuilder msgBuilder = new SpeechMsgBuilder("您已偏航，正在重新规划路线")
                        .setForceLocalEngine(true)
                        .setOrigin(SpeechMsg.ORIGIN_COMMON);
                SynthesizerBase.get().startSpeakAbsolute(msgBuilder.build())
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation())
                        .subscribe();

            }
        }

        /** 偏航规划结束 **/
        @Override
        public void onYawingRequestSuccess() {
            Log.i(TAG, "onYawingRequestSuccess");
            if (SynthesizerBase.isInited()) {
                SpeechMsgBuilder msgBuilder = new SpeechMsgBuilder("路线规划完毕")
                        .setForceLocalEngine(true)
                        .setOrigin(SpeechMsg.ORIGIN_COMMON);
                Observable<SpeechMsg> msgObservable = SynthesizerBase.get().addMessageWaitSpeak(msgBuilder.build());
                if (msgObservable != null) {
                    msgObservable.subscribeOn(Schedulers.io())
                            .observeOn(Schedulers.computation())
                            .subscribe();
                }
            }
            BNRoutePlanerProxy.getInstance().routePlans.clear();
            RouteModel.getLastRouteModels().clear();
            Vector<RoutePlanModelProxy> v = new Vector<RoutePlanModelProxy>();
            ArrayList<RouteModel> routeModels = new ArrayList<RouteModel>();
            int l = BNRoutePlanerProxy.getInstance().getRouteCnt();
            if (l > 0) {
                for (int i = 0; i < l; i++) {
                    Bundle bundle = new Bundle();
                    RoutePlanModelProxy rp = new RoutePlanModelProxy();
                    BNRoutePlanerProxy.getInstance().getRouteInfo(i, bundle);
                    rp.parseRouteResult(mContext, bundle);
                    routeModels.add(new RouteModel(bundle));
                    v.add(rp);
                }
                RouteModel.put(calculatePreference, routeModels);
                BNRoutePlanerProxy.getInstance().routePlans.put(calculatePreference, v);
            }
        }

        @Override
        public void notifyNmeaData(String s) {
        }

        /** 导航过程中的gps定位信息数据 **/
        @Override
        public void notifyLoacteData(LocData locData) {
            Log.i(TAG, "notifyLoacteData>>" + locData.toString());
            if (locData.isValid()) {
                appConfig.address.setLatitude(locData.latitude);
                appConfig.address.setLongitude(locData.longitude);
                BaiduLocateManager.get().updateManual(locData.latitude,
                        locData.longitude,
                        locData.altitude,
                        locData.satellitesNum,
                        locData.speed,
                        locData.direction,
                        locData.accuracy);
            }
        }

        @Override
        public void notifyGPSStatusData(int i) {
        }

        @Override
        public void notifyViewModeChanged(int i) {
        }

        @Override
        public void notifyOtherAction(int i, int i1, int i2, Object o) {
        }
    };

    /**
     * 导航引擎初始化回调
     **/
    private BaiduNaviManager.NaviInitListener naviInitListener = new BaiduNaviManager.NaviInitListener() {

        public void initSuccess() {
            Log.e(TAG, "百度导航引擎初始化成功");
            mHandler.sendEmptyMessage(NaviSetLineActivity.MSG_INIT_MAPVIEW);
        }

        @Override
        public void onAuthResult(int i, String s) {
            Log.i(TAG, i + ">>" + s);
        }

        public void initStart() {
            Log.e(TAG, "百度导航引擎初始化开始");
        }

        public void initFailed() {
            Log.e(TAG, "百度导航引擎初始化失败");
        }
    };

    /**
     * 初始化导航路线图
     **/
    @Override
    public void initMapView() {
        if (mMapView == null)
            mMapView = naviManager.createNMapView();
        /* BNMapController 地图管理控制类，控制地图的缩放、移动、旋转等操作。初始化导航地图状态 */
        BNMapControllerProxy mapController = BNMapControllerProxy.getInstance();
        /* 注意是经度在前，纬度在后 */
        mapController.initMapStatus(new GeoPointProxy((int) (address.getLongitude() * 1e5), (int) (address.getLatitude() * 1e5)));
        /* 将地图控件添加到布局中 */
        mSetLineView.setMapView(mMapView, true);
        /* 设置展示图层模式（cruise：巡航） */
        mapController.setLayerMode(MapParams.LayerMode.MAP_LAYER_MODE_CRUISE);
        mapController.setMapStatus(mapController.getMapStatus(), BNMapControllerProxy.AnimationType.eAnimationLevel);

        if (calculateRoad == NaviSetLineActivity.UN_CALCULATE) {
            startCalculateRoad();
        } else if (calculateType == 0 && calculateRoad >= NaviSetLineActivity.CALCULATED) {
            if (calculateRoad == NaviSetLineActivity.CALCULATED_SHOW) {
                BaiduNaviSuperManager.setSelectLineState(true);
                calculatePreference = BNRoutePlanerProxy.getInstance().getCalcPreference();
                calculateScheme = BNRoutePlanerProxy.getInstance().getCalculateScheme();
                if (routeInputs == null && NavigatorService.get().getRouteInputs().size() > 1) {
                    routeInputs = (ArrayList<BNRoutePlanNode>) NavigatorService.get().getRouteInputs();
                }
                setRouteMode();
            } else
                startNavi(mContext, true);
        }
    }

    @Override
    public int getPreference() {
        return calculatePreference;
    }
}
