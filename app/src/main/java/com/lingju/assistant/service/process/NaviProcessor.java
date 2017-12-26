package com.lingju.assistant.service.process;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.navi.BaiduMapNavigation;
import com.baidu.mapapi.navi.NaviParaOption;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.NaviConfirmPointActivity;
import com.lingju.assistant.activity.NaviSetLineActivity;
import com.lingju.assistant.activity.TrafficShowActivity;
import com.lingju.assistant.activity.event.ChatMsgEvent;
import com.lingju.assistant.activity.event.DialogEvent;
import com.lingju.assistant.activity.event.MapCmdEvent;
import com.lingju.assistant.activity.event.NaviRouteCalculateEvent;
import com.lingju.assistant.activity.event.NaviShowPointsEvent;
import com.lingju.assistant.activity.event.NaviSwitchRouteLineEvent;
import com.lingju.assistant.activity.event.NavigateEvent;
import com.lingju.assistant.activity.event.RobotTipsEvent;
import com.lingju.assistant.activity.event.SelectCityEvent;
import com.lingju.assistant.baidunavi.adapter.BaiduNaviSuperManager;
import com.lingju.assistant.baidunavi.adapter.BaiduTTSPlayerCallBack;
import com.lingju.assistant.entity.RobotConstant;
import com.lingju.assistant.service.AssistantService;
import com.lingju.assistant.service.NavigatorService;
import com.lingju.assistant.service.process.base.BaseProcessor;
import com.lingju.audio.SystemVoiceMediator;
import com.lingju.audio.engine.base.SpeechMsg;
import com.lingju.audio.engine.base.SpeechMsgBuilder;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.common.log.Log;
import com.lingju.context.entity.Address;
import com.lingju.context.entity.Command;
import com.lingju.context.entity.Get;
import com.lingju.context.entity.Navigation;
import com.lingju.context.entity.Plat;
import com.lingju.context.entity.Route;
import com.lingju.context.entity.Routenode;
import com.lingju.context.entity.SyncSegment;
import com.lingju.lbsmodule.constant.RouteGuideParams;
import com.lingju.lbsmodule.entity.RouteModel;
import com.lingju.lbsmodule.location.BaiduLocateManager;
import com.lingju.lbsmodule.proxy.BNRoutePlanerProxy;
import com.lingju.lbsmodule.proxy.BNSysLocationManagerProxy;
import com.lingju.lbsmodule.proxy.BNavigatorProxy;
import com.lingju.lbsmodule.proxy.RGAssistGuideModelProxy;
import com.lingju.lbsmodule.proxy.RGSimpleGuideModelProxy;
import com.lingju.lbsmodule.proxy.RoutePlanModelProxy;
import com.lingju.lbsmodule.proxy.RoutePlanResultItemProxy;
import com.lingju.model.BaiduAddress;
import com.lingju.model.NaviRecord;
import com.lingju.model.dao.BaiduNaviDao;
import com.lingju.model.dao.RecordDao;
import com.lingju.model.temp.speech.ResponseMsg;
import com.lingju.util.JsonUtils;
import com.lingju.util.StringUtils;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Administrator on 2016/11/5.
 */
public class NaviProcessor extends BaseProcessor {

    private static final String TAG = "LingJu";
    private BaiduNaviDao mNaviDao;
    private AppConfig mAppConfig;

    public NaviProcessor(Context mContext, SystemVoiceMediator mediator) {
        super(mContext, mediator);
        mNaviDao = BaiduNaviDao.getInstance();
        this.mAppConfig = (AppConfig) mContext.getApplicationContext();
    }

    @Override
    public int aimCmd() {
        return CMD_NAVI;
    }

    @Override
    public void handle(Command cmd, String text, int inputType) {
        super.handle(cmd, text, inputType);
        try {
            final SpeechMsgBuilder msgBuilder = SpeechMsgBuilder.create("");
            if (cmd.getOutc() == DefaultProcessor.OUTC_ASK)
                msgBuilder.setContextMode(SpeechMsg.CONTEXT_KEEP_RECOGNIZE);
            if (EventBus.getDefault().hasSubscriberForEvent(RobotTipsEvent.class))
                EventBus.getDefault().post(new RobotTipsEvent(cmd.getTtext()));
            JSONArray actions = new JSONArray(cmd.getActions());
            JSONObject lastAction = actions.getJSONObject(actions.length() - 1);
            JSONObject lastTarget = lastAction.getJSONObject("target");
            Integer action = RobotConstant.ActionMap.get(lastAction.getString("action"));
            switch (lastTarget.getInt("id")) {
                case RobotConstant.ACTION_NAVIGATION:
                    switch (action) {
                        case RobotConstant.SET:
                            Navigation navigation = SyncSegment.fromJson(lastTarget.toString(), Navigation.class);
                            if (!TextUtils.isEmpty(navigation.getStatus())) {
                                RobotConstant.NaviStatus status = RobotConstant.NaviStatus.valueOf(navigation.getStatus());
                                switch (status) {
                                    case CONTINUE:     //开始/继续导航
                                        EventBus.getDefault().post(new DialogEvent(DialogEvent.CANCEL_WALK_TYPE));
                                        if (BaiduNaviSuperManager.isNaviInited() && EventBus.getDefault().hasSubscriberForEvent(NavigateEvent.class)) {
                                            if (BNavigatorProxy.getInstance().isNaviBegin())
                                                EventBus.getDefault().post(new NavigateEvent(NavigateEvent.RESUME_NAVI));
                                            else if (BaiduNaviSuperManager.isSelectLineState()) {
                                                EventBus.getDefault().post(new NavigateEvent(NavigateEvent.START_NAVI));
                                                text = "导航开始";
                                            }
                                        } else {
                                            if ((System.currentTimeMillis() - NavigatorService.get().getLastCalculateTime()) < 60000) {
                                                Intent intent = new Intent(mContext, NaviSetLineActivity.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                intent.putExtra(NaviSetLineActivity.CALCULATE_ROAD, NaviSetLineActivity.CALCULATED);
                                                mContext.startActivity(intent);
                                            } else
                                                text = "你当前不在导航中,请先让我位你导航吧";
                                        }
                                        break;
                                    case TIME_CONTINUE:     //10秒后继续导航
                                        executeAfterSpeak(text, "您已退出导航页面，重新来一次吧！",
                                                NavigateEvent.RESUME_TO_START_COUNTDOWN, msgBuilder);
                                        return;
                                    case PAUSE:     //暂停导航
                                        if (BaiduNaviSuperManager.isNaviInited() && BNavigatorProxy.getInstance().isNaviBegin()) {
                                            EventBus.getDefault().post(new NavigateEvent(NavigateEvent.PAUSE_NAVI));
                                        } else {
                                            text = "你当前不在导航中,请先让我位你导航吧";
                                        }
                                        break;
                                    case CLOSE:
                                        //若导航已开始，则停止导航
                                        if (EventBus.getDefault().hasSubscriberForEvent(NavigateEvent.class)) {
                                            EventBus.getDefault().post(new NavigateEvent(NavigateEvent.STOP_NAVI));
                                        }
                                        //若在确认目的地页面则关闭该页面
                                        if (EventBus.getDefault().hasSubscriberForEvent(NaviShowPointsEvent.class)) {
                                            EventBus.getDefault().post(new NaviShowPointsEvent(null, NaviShowPointsEvent.CANCEL_TASK));
                                        }
                                        break;
                                }
                            } else if (!TextUtils.isEmpty(navigation.getIscongestion())) {      //切换躲避拥堵路线
                                if ("ON".equals(navigation.getIscongestion())) {
                                    if (BNRoutePlanerProxy.getInstance().getCalcPreference() != BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND &&
                                            (BNRoutePlanerProxy.getInstance().getCalcPreference() & BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM) !=
                                                    BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM) {
                                        if (EventBus.getDefault().hasSubscriberForEvent(NaviSwitchRouteLineEvent.class)) {
                                            EventBus.getDefault().post(new NaviSwitchRouteLineEvent((BNRoutePlanerProxy.getInstance().getCalcPreference() | BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM) << 3, true));
                                            return;
                                        } else {
                                            text = "很抱歉，您已离开导航页面。";
                                        }
                                    }else {
                                        text = "在规划的路线中已为您躲避拥堵，继续为您导航";
                                    }
                                } else {
                                    if (BNRoutePlanerProxy.getInstance().getCalcPreference() != BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND &&
                                            (BNRoutePlanerProxy.getInstance().getCalcPreference() & BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM) ==
                                                    BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM) {
                                        if (EventBus.getDefault().hasSubscriberForEvent(NaviSwitchRouteLineEvent.class)) {
                                            EventBus.getDefault().post(new NaviSwitchRouteLineEvent((BNRoutePlanerProxy.getInstance().getCalcPreference() &
                                                    (BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM ^ 31)) << 3, true));
                                            return;
                                        } else {
                                            text = "很抱歉，您已离开导航页面。";
                                        }
                                    }
                                }
                            } else if (navigation.getNaviplan() != 0) {     // 选择一个方案(路线)
                                if (NavigatorService.get().isNavigation()) {
                                    text = "抱歉，导航中，暂不支持更改路线方案。";
                                } else {
                                    if (EventBus.getDefault().hasSubscriberForEvent(NaviSwitchRouteLineEvent.class)) {
                                        RouteModel.setCalculateScheme(navigation.getNaviplan() - 1);
                                        EventBus.getDefault().post(new NaviSwitchRouteLineEvent((BNRoutePlanerProxy.getInstance().getCalcPreference() << 3) | RouteModel.getCalculateScheme(), true));
                                    } else {
                                        text = "很抱歉，您还未规划好路线！";
                                    }
                                }
                            }
                            break;
                        case RobotConstant.QUERY:
                            Get get = JsonUtils.getObj(lastAction.optJSONObject("get").toString(), Get.class);
                            String property = get.getProperty();
                            if ("currentspeed".equals(property)) {      //查询当前车速 
                                text = null;
                                if (BaiduNaviSuperManager.isNaviInited() && BNavigatorProxy.getInstance().isNaviBegin()) {
                                    if (BNSysLocationManagerProxy.getInstance().getCurLocation() != null) {
                                        StringBuilder sb = new StringBuilder("当前车速");
                                        sb.append(RGAssistGuideModelProxy.getInstance().getCurCarSpeed()).append("公里");
                                        if (RGAssistGuideModelProxy.getInstance().isOverSpeed()) {
                                            sb.append("您已超速，限速");
                                            sb.append(StringUtils.numberToChineseWord(RGAssistGuideModelProxy.getInstance().getCurLimitSpeed()));
                                            sb.append("公里。");
                                        }
                                        text = sb.toString();
                                    }
                                } else {
                                    BaiduLocateManager.get().start();
                                    if (mAppConfig.address.getSpeed() > 0) {
                                        text = "当前车速大概是" + ((int) (mAppConfig.address.getSpeed() * 3.6D)) + "公里";
                                    }
                                }
                                if (text == null)
                                    text = "没有获取到车速信息,请稍后再试。";
                            } else if ("ratelimite".equals(property)) {         // 查询当前限速信息
                                if (BaiduNaviSuperManager.isNaviInited() && BNavigatorProxy.getInstance().isNaviBegin()) {
                                    int ls = RGAssistGuideModelProxy.getInstance().getCurLimitSpeed();
                                    if (ls <= 0) {
                                        text = "当前道路没有限速数据";
                                    } else {
                                        StringBuilder sb = new StringBuilder();
                                        sb.append("当前道路限速")
                                                .append(StringUtils.numberToChineseWord(RGAssistGuideModelProxy.getInstance().getCurLimitSpeed()))
                                                .append("公里。");
                                        if (BNSysLocationManagerProxy.getInstance().getCurLocation() != null) {
                                            sb.append("当前车速").append(RGAssistGuideModelProxy.getInstance().getCurCarSpeed()).append("公里。");
                                            if (RGAssistGuideModelProxy.getInstance().isOverSpeed()) {
                                                sb.append("您已超速。");
                                            }
                                        }
                                        text = sb.toString();
                                    }
                                } else {
                                    text = "您当前不再导航中，无法获取限速信息。";
                                }
                            } else if ("currentroad".equals(property)) {        //查询当前所在路段
                                text = null;
                                if (BaiduNaviSuperManager.isNaviInited() && BNavigatorProxy.getInstance().isNaviBegin()) {
                                    text = RGSimpleGuideModelProxy.getInstance().getCurRoadName();
                                    if ("当前道路".equals(text)) {
                                        text = null;
                                    }
                                }
                                if (text == null) {
                                    BaiduLocateManager.get().start();
                                    text = TextUtils.isEmpty(mAppConfig.address.getStreet()) ? mAppConfig.address.getAddressDetail() : mAppConfig.address.getStreet();
                                }
                            } else if ("remaindistance".equals(property)) {     //剩余路程和时间
                                if (BaiduNaviSuperManager.isNaviInited() && BNavigatorProxy.getInstance().isNaviBegin()) {
                                    text = "大概" + RGSimpleGuideModelProxy.getInstance().getTotalRemainDistString() + RGSimpleGuideModelProxy.getInstance().getArriveTimeString();
                                } else {
                                    text = "你当前不在导航中,请先让我为你导航吧";
                                }
                            }
                            break;
                        case RobotConstant.READ:        //播报上一次导航内容
                            text = BaiduTTSPlayerCallBack.NaviText;
                            break;
                        case RobotConstant.CANCEL:      //取消导航
                            //若导航已开始，则停止导航
                            if (EventBus.getDefault().hasSubscriberForEvent(NavigateEvent.class)) {
                                EventBus.getDefault().post(new NavigateEvent(NavigateEvent.STOP_NAVI));
                            }
                            //若在确认目的地页面则关闭该页面
                            if (EventBus.getDefault().hasSubscriberForEvent(NaviShowPointsEvent.class)) {
                                EventBus.getDefault().post(new NaviShowPointsEvent(null, NaviShowPointsEvent.CANCEL_TASK));
                            }
                            break;
                    }
                    break;
                case RobotConstant.ACTION_ROUTE:
                    switch (action) {
                        case RobotConstant.QUERY:       //确定目的地，规划导航路线
                            //关闭选择城市对话框
                            EventBus.getDefault().post(new DialogEvent(DialogEvent.CANCEL_TOGGLE_TYPE));
                            //关闭步行导航询问对话框
                            EventBus.getDefault().post(new DialogEvent(DialogEvent.CANCEL_WALK_TYPE));
                            if (lastAction.isNull("get")) {
                                Route route = SyncSegment.fromJson(lastTarget.toString(), Route.class);
                                if ("DRIVELIEN".equals(route.getType())) {      //驾车
                                    NaviRouteCalculateEvent event = new NaviRouteCalculateEvent();
                                    //默认非复合条件导航
                                    event.setType(NaviRouteCalculateEvent.FULL);
                                    List<BaiduAddress> passPoints = null;
                                    if (actions.length() > 1) {
                                        passPoints = BaiduAddress.createFromAddress(route.getTransitpoint());
                                        JSONObject firstAction = actions.optJSONObject(0);
                                        JSONObject firstTarget = firstAction.optJSONObject("target");
                                        switch (firstTarget.getInt("id")) {
                                            case RobotConstant.ACTION_NAVIGATION:
                                                String iscongestion = firstTarget.getString("iscongestion");
                                                if ("ON".equals(iscongestion)) {     //躲避拥堵
                                                    event.setType(NaviRouteCalculateEvent.FULL_WITH_CALCULATE_MODE);
                                                    event.setCalculateMode(BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM);
                                                }
                                                break;
                                            case RobotConstant.ACTION_ADDRESS:
                                                Address passAddress = SyncSegment.fromJson(firstTarget.toString(), Address.class);
                                                if ("INSERT".equals(firstAction.getString("action"))) {      //经过指定途经点
                                                    event.setType(NaviRouteCalculateEvent.FULL_CONTAINS_EXISTENT_PASS_POINT);
                                                } else if ("DELETE".equals(firstAction.getString("action"))) {        //不经过
                                                    event.setType(NaviRouteCalculateEvent.FULL_CONTAINS_NOT_PASS_POINT);
                                                }
                                                event.setPassPoint(passAddress.getName());
                                                break;
                                        }
                                    } else if (!lastAction.isNull("sort")) {
                                        String sort = lastAction.optJSONObject("sort").getString("orderby");
                                        if ("routetime".equals(sort)) {      //高速优先，时间最短
                                            event.setType(NaviRouteCalculateEvent.FULL_WITH_CALCULATE_MODE);
                                            event.setCalculateMode(BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TIME | BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM);
                                        } else if ("routeroll".equals(sort)) {        //不走高速，费用最少
                                            event.setType(NaviRouteCalculateEvent.FULL_WITH_CALCULATE_MODE);
                                            event.setCalculateMode(BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TOLL | BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM);
                                        } else if ("routedistance".equals(sort)) {        //路程最短
                                            event.setType(NaviRouteCalculateEvent.FULL_WITH_CALCULATE_MODE);
                                            event.setCalculateMode(BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_DIST | BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM);
                                        }
                                    }
                                    if (!TextUtils.isEmpty(route.getEndaddress().getAlias()))
                                        event.setType(event.getType() | NaviRouteCalculateEvent.TARGET_IS_FAVORITE_POINT);

                                    ArrayList<BaiduAddress> routeNodes = new ArrayList<>();
                                    BaiduAddress endAddress = BaiduAddress.createFromAddress(route.getEndaddress(), false);
                                    //获取起点
                                    BaiduAddress start = mNaviDao.get(mAppConfig.address);

                                    //保存导航记录
                                    NaviRecord record = new NaviRecord();
                                    record.setStartLatitude(start.getLatitude());
                                    record.setStartLongitude(start.getLongitude());
                                    record.setStartName(start.getName());
                                    record.setEndLatitude(endAddress.getLatitude());
                                    record.setEndLongitude(endAddress.getLongitude());
                                    record.setEndName(endAddress.getName());
                                    record.setCreated(new Date(System.currentTimeMillis()));
                                    RecordDao.getInstance().insertRecord(record);
                                    start.setCGJ02();
                                    routeNodes.add(start);
                                    //设置途经点
                                    if (passPoints != null && passPoints.size() > 0) {
                                        routeNodes.addAll(passPoints);
                                    }
                                    endAddress.setCGJ02();
                                    routeNodes.add(endAddress);
                                    event.setPoints(routeNodes);

                                    if (EventBus.getDefault().hasSubscriberForEvent(NaviShowPointsEvent.class)) {
                                        //开始进入导航路线规划页面，关闭目的地确认页面
                                        EventBus.getDefault().post(new NaviShowPointsEvent(null, NaviShowPointsEvent.CLOSE_ACTIVITY));
                                    }
                                    if (EventBus.getDefault().hasSubscriberForEvent(NaviRouteCalculateEvent.class)) {
                                        EventBus.getDefault().post(event);
                                    } else {
                                        Intent naviIntent = new Intent(mContext, NaviSetLineActivity.class);
                                        naviIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        naviIntent.putParcelableArrayListExtra("addresses", routeNodes);
                                        naviIntent.putExtra("voice", true);
                                        naviIntent.putExtra("type", event.getType());
                                        naviIntent.putExtra("calculateMode", event.getCalculateMode());
                                        if (!TextUtils.isEmpty(event.getPassPoint()))
                                            naviIntent.putExtra("passPoint", event.getPassPoint());
                                        mContext.startActivity(naviIntent);
                                    }
                                    return;
                                } else {        //步行
                                    EventBus.getDefault().post(new DialogEvent(DialogEvent.CANCEL_WALK_TYPE));
                                    if (EventBus.getDefault().hasSubscriberForEvent(NaviShowPointsEvent.class)) {
                                        //开始进入导航路线规划页面，关闭目的地确认页面
                                        EventBus.getDefault().post(new NaviShowPointsEvent(null, NaviShowPointsEvent.CLOSE_ACTIVITY));
                                    }
                                    try {
                                        msgBuilder.setContextMode(SpeechMsg.CONTEXT_AUTO);
                                        voiceMediator.recordCurrentVolume();
                                        BaiduAddress ad = BaiduAddress.createFromAddress(route.getEndaddress(), false);
                                        //起点(百度地图API使用坐标需要转换为BD09LL类型)
                                        // com.lingju.lbsmodule.location.Address tmp = mAppConfig.address.clone().setBD09LL();
                                        // Log.i("LingJu", "步行起点：" + tmp.getAddressDetail() + " " + tmp.getLatitude() + " " + tmp.getLongitude());
                                        LatLng start = new LatLng(mAppConfig.address.getLatitude(), mAppConfig.address.getLongitude());
                                        //终点
                                        LatLng end = new LatLng(ad.getLatitude(), ad.getLongitude());
                                        // 构建导航参数
                                        NaviParaOption para = new NaviParaOption()
                                                .startPoint(start)
                                                .startName(mAppConfig.address.getAddressDetail())
                                                .endPoint(end)
                                                .endName(ad.getName() + ad.getAddress());
                                        // 调起步行导航
                                        if (!BaiduMapNavigation.openBaiduMapWalkNavi(para, mContext)) {
                                            voiceMediator.resumeMediaVolume();
                                            text = "很抱歉，你还未安装百度地图，无法为你步行导航。";
                                        } else {
                                            text = "开始进入步行导航";
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        text = "系统出错了！";
                                    }
                                }
                            } else {
                                Get get = JsonUtils.getObj(lastAction.optJSONObject("get").toString(), Get.class);
                                if ("routetraffic".equals(get.getProperty())) {
                                    text = NavigatorService.get().pointTraffic(7, null, msgBuilder);
                                }
                            }
                            break;
                        case RobotConstant.VIEW:        //查看全程
                            if (BaiduNaviSuperManager.isNaviInited() && BNavigatorProxy.getInstance().isNaviBegin()) {
                                EventBus.getDefault().post(new NavigateEvent(NavigateEvent.NAVI_SHOW_FULL_LINE));
                            } else {
                                text = "你当前不在导航中，无法查看全程，请先让我位你导航吧";
                            }
                            break;
                        case RobotConstant.SELECT:      //切换导航路线
                            if (lastAction.isNull("sort")) {
                                Route newRoute = SyncSegment.fromJson(lastTarget.toString(), Route.class);
                                if (EventBus.getDefault().hasSubscriberForEvent(NaviSwitchRouteLineEvent.class)) {
                                    RouteModel.setCalculateScheme(newRoute.getRouteid());
                                    EventBus.getDefault().post(new NaviSwitchRouteLineEvent((BNRoutePlanerProxy.getInstance().getCalcPreference() << 3) | RouteModel.getCalculateScheme(), true));
                                } else {
                                    text = "很抱歉，您还未规划好路线！";
                                }
                            } else {
                                String sort = lastAction.optJSONObject("sort").getString("orderby");
                                if ("routetime".equals(sort)) {      //高速优先，时间最短
                                    if ((BNRoutePlanerProxy.getInstance().getCalcPreference() & BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TIME) == BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TIME) {
                                        text = "当前已经使用了时间最短路线";
                                        speakAndAheadReturn(text, msgBuilder);
                                        return;
                                    } else {
                                        if (EventBus.getDefault().hasSubscriberForEvent(NaviSwitchRouteLineEvent.class)) {
                                            EventBus.getDefault().post(new NaviSwitchRouteLineEvent(
                                                    (BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TIME |
                                                            BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM) << 3));
                                            return;
                                            //RoutePlanModelProxy minTimeModel = BNRoutePlanerProxy.getInstance().routePlans.get(
                                            //        BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TIME |
                                            //                BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM).get(0);
                                            //text = "已为您切换到时间最短路线，全程" + minTimeModel.getDistance() + "，预计" + minTimeModel.getTotalTime();
                                        } else {
                                            text = "你已离开导航页面，无法完成你的要求。";
                                        }
                                    }
                                } else if ("routeroll".equals(sort)) {        //不走高速，费用最少
                                    if ((RouteModel.getCurrent() != null && RouteModel.getCurrent().getToll() == 0) ||
                                            (BNRoutePlanerProxy.getInstance().getCalcPreference() & BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TOLL) == BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TOLL) {
                                        text = "当前已经使用了收费最少路线";
                                        // sendNoNeedBackground();
                                        speakAndAheadReturn(text, msgBuilder);
                                        return;
                                    } else {
                                        if (EventBus.getDefault().hasSubscriberForEvent(NaviSwitchRouteLineEvent.class)) {
                                            EventBus.getDefault().post(new NaviSwitchRouteLineEvent(
                                                    BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TOLL << 3));
                                            return;
                                            //RoutePlanModelProxy routePlanModelProxy = BNRoutePlanerProxy.getInstance().routePlans.get(BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TOLL).get(0);
                                            //text = "已为您切换到收费最少路线，" + (routePlanModelProxy.getTollFees() == 0 ? "预计无收费" : "最少收费预计"
                                            //        + routePlanModelProxy.getTollFees() + "元") + "，全程" + routePlanModelProxy.getDistance() + "，预计" + routePlanModelProxy.getTotalTime();
                                        } else {
                                            text = "你已离开导航页面，无法完成你的要求。";
                                        }
                                    }
                                } else if ("routedistance".equals(sort)) {        //路程最短
                                    if ((BNRoutePlanerProxy.getInstance().getCalcPreference() & BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_DIST) == BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_DIST) {
                                        text = "当前已经使用了路程最短路线";
                                        // sendNoNeedBackground();
                                        speakAndAheadReturn(text, msgBuilder);
                                        return;
                                    } else {
                                        if (EventBus.getDefault().hasSubscriberForEvent(NaviSwitchRouteLineEvent.class)) {
                                            EventBus.getDefault().post(new NaviSwitchRouteLineEvent(
                                                    (BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_DIST |
                                                            BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM) << 3));
                                            return;
                                            // RoutePlanModelProxy minDistanceModel = BNRoutePlanerProxy.getInstance().routePlans.get(
                                            //         BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_DIST |
                                            //                 BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM).get(0);
                                            // text = "已为您切换到路程最短路线，全程" + minDistanceModel.getDistance() + "，预计" + minDistanceModel.getTotalTime();
                                        } else {
                                            text = "你已离开导航页面，无法完成你的要求。";
                                        }
                                    }
                                }
                            }
                            break;
                    }
                    break;
                case RobotConstant.ACTION_ADDRESS:
                    switch (action) {
                        case RobotConstant.SELECT:      //选择地址（目的地、途经点、家和单位）
                        case RobotConstant.APPEND:
                            EventBus.getDefault().post(new DialogEvent(DialogEvent.CANCEL_TOGGLE_TYPE));
                            if (EventBus.getDefault().hasSubscriberForEvent(SelectCityEvent.class))
                                EventBus.getDefault().post(new SelectCityEvent(lastTarget.getString("city")));
                            if (BNavigatorProxy.getInstance().isNaviBegin())
                                EventBus.getDefault().post(new NavigateEvent(NavigateEvent.STOP_NAVI_BACKGROUND));
                            ArrayList<BaiduAddress> list = new ArrayList<>();
                            //填充导航点对象集合
                            for (int i = 0; i < actions.length(); i++) {
                                JSONObject target = actions.optJSONObject(i).optJSONObject("target");
                                Address address = SyncSegment.fromJson(target.toString(), Address.class);
                                list.add(BaiduAddress.createFromAddress(address, false));
                            }
                            if (action == RobotConstant.SELECT) {
                                //保存首个位置作为历史记录
                                BaiduAddress baiduAddress = list.get(0);
                                baiduAddress.setSearchKeyWord(baiduAddress.getName());
                                baiduAddress.setCreated(new Date());
                                mNaviDao.insertAddress(baiduAddress);
                            }
                            //进入确定导航点目的地
                            Intent intent = new Intent(mContext, NaviConfirmPointActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra("addresses", list);
                            intent.putExtra("add", action != RobotConstant.SELECT);
                            mContext.startActivity(intent);
                            break;
                        case RobotConstant.QUERY:       //查询路况（未导航）
                            ArrayList<BaiduAddress> addresses = new ArrayList<>();
                            for (int i = 0; i < actions.length(); i++) {
                                JSONObject target = actions.optJSONObject(i).optJSONObject("target");
                                Address address = SyncSegment.fromJson(target.toString(), Address.class);
                                addresses.add(BaiduAddress.createFromAddress(address, false));
                            }
                            if (EventBus.getDefault().hasSubscriberForEvent(MapCmdEvent.class)) {
                                EventBus.getDefault().post(new MapCmdEvent(MapCmdEvent.SHOW_TRAFFIC, addresses));
                            } else {
                                Intent showTrafficIntent = new Intent(mContext, TrafficShowActivity.class);
                                showTrafficIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                showTrafficIntent.putParcelableArrayListExtra("addresses", addresses);
                                mContext.startActivity(showTrafficIntent);
                            }
                            break;
                        case RobotConstant.SET:         //设置家或公司地址
                            //关闭常用地址设置页面
                            if (EventBus.getDefault().hasSubscriberForEvent(NaviShowPointsEvent.class))
                                EventBus.getDefault().post(new NaviShowPointsEvent(null, NaviShowPointsEvent.CLOSE_ACTIVITY));
                            EventBus.getDefault().post(new DialogEvent(DialogEvent.CANCEL_TOGGLE_TYPE));
                            Address favorAddress = SyncSegment.fromJson(lastTarget.toString(), Address.class);
                            BaiduAddress bdAddress = BaiduAddress.createFromAddress(favorAddress, false);
                            bdAddress.setFavoritedTime(new Date());
                            bdAddress.setCreated(new Date());
                            if (mContext.getResources().getString(R.string.home).equals(favorAddress.getAlias())) {
                                mNaviDao.removeHomeOrCompany(mContext.getResources().getString(R.string.home));
                                bdAddress.setRemark(mContext.getResources().getString(R.string.home));
                                mNaviDao.insertAddress(bdAddress);
                            } else {
                                mNaviDao.removeHomeOrCompany(mContext.getResources().getString(R.string.company));
                                bdAddress.setRemark(mContext.getResources().getString(R.string.company));
                                mNaviDao.insertAddress(bdAddress);
                            }
                            break;
                        case RobotConstant.INSERT:
                            EventBus.getDefault().post(new DialogEvent(DialogEvent.CANCEL_TOGGLE_TYPE));
                            Address passAddress = SyncSegment.fromJson(lastTarget.toString(), Address.class);
                            BaiduAddress passPoint = BaiduAddress.createFromAddress(passAddress, true);
                            if (passAddress.getLongitude() == 0 && passAddress.getLatitude() == 0) {    //添加途经点（在导航路线中）
                                int calculatePrefrence = BNRoutePlanerProxy.getInstance().getCalcPreference();
                                NavigatorService.get().passPoint(passPoint.getName());
                                int selectPassPointLine = NavigatorService.get().getSelectPassPointLine();
                                if (!BNavigatorProxy.getInstance().isNaviBegin()) {
                                    if (selectPassPointLine == (calculatePrefrence << 3) && (selectPassPointLine & 7) == BNRoutePlanerProxy.getInstance().getCalculateScheme()) {
                                        if (EventBus.getDefault().hasSubscriberForEvent(NavigateEvent.class)) {
                                            EventBus.getDefault().post(new NavigateEvent(NavigateEvent.START_NAVI));
                                        } else {
                                            text = "你已离开导航页面，无法完成你的要求。";
                                        }
                                    } else {
                                        if (EventBus.getDefault().hasSubscriberForEvent(NaviSwitchRouteLineEvent.class)) {
                                            EventBus.getDefault().post(new NaviSwitchRouteLineEvent(selectPassPointLine, true));
                                            text = "已为你规划路线，途经" + passPoint.getName() + ",全程" + RouteModel.getLastRouteModels().get(selectPassPointLine >> 3).get(selectPassPointLine & 7).getDistanceStr() + "。";
                                            executeAfterSpeak(text, "小样你还要不要导航了，记住，动嘴不动手动嘴不动手动嘴不动手" +
                                                    "，重要的事情说三遍，你还想导航重新来一遍吧！", NavigateEvent.START_NAVI, msgBuilder);
                                            return;
                                        } else {
                                            text = "你已离开导航页面，无法完成你的要求。";
                                        }
                                    }
                                }
                            } else {        //添加途经点(不在导航路线中)
                                if (EventBus.getDefault().hasSubscriberForEvent(NaviShowPointsEvent.class)) {
                                    EventBus.getDefault().post(new NaviShowPointsEvent(null, NaviShowPointsEvent.CLOSE_ACTIVITY));
                                }
                                NaviRouteCalculateEvent event;
                                if (EventBus.getDefault().hasSubscriberForEvent(NaviRouteCalculateEvent.class)) {
                                    if (BNavigatorProxy.getInstance().isNaviBegin())
                                        event = new NaviRouteCalculateEvent(passPoint.getName(), NaviRouteCalculateEvent.RECALCULATE_ADD_POINT_IN_NAVIGATE);
                                    else
                                        event = new NaviRouteCalculateEvent(passPoint, NaviRouteCalculateEvent.SINGLE_PASS);
                                    EventBus.getDefault().post(event);
                                    return;
                                } else {
                                    RoutePlanModelProxy passPointModel = RoutePlanModelProxy.getCacheRoutePlanModelProxy("RoutePlanModel");
                                    BNRoutePlanNode endNode = passPointModel.getEndNode();
                                    //将导航节点添加到节点集合
                                    if (endNode != null) {
                                        BaiduAddress end = new BaiduAddress();
                                        end.setName(endNode.getName());
                                        end.setCreated(new Date());
                                        end.setLatitude(endNode.getLatitude());
                                        end.setLongitude(endNode.getLongitude());
                                        end.setAddress(endNode.getDescription());
                                        BaiduAddress start = mNaviDao.get(mAppConfig.address);
                                        start.setCGJ02();
                                        ArrayList<BaiduAddress> naviNodes = new ArrayList<>();
                                        naviNodes.add(start);
                                        naviNodes.add(passPoint);
                                        naviNodes.add(end);
                                        event = new NaviRouteCalculateEvent(naviNodes);

                                        Intent passIntent = new Intent(mContext, NaviSetLineActivity.class);
                                        passIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        passIntent.putParcelableArrayListExtra("addresses", naviNodes);
                                        passIntent.putExtra("voice", true);
                                        passIntent.putExtra("type", NaviRouteCalculateEvent.FULL_CONTAINS_EXISTENT_PASS_POINT);
                                        passIntent.putExtra("calculateMode", event.getCalculateMode());
                                        mContext.startActivity(passIntent);
                                        return;
                                    } else {
                                        text = "系统出错了";
                                    }
                                }
                            }
                            break;
                    }
                    break;
                case RobotConstant.ACTION_ROUTENODE:
                    switch (action) {
                        case RobotConstant.QUERY:
                            if (!lastAction.isNull("get")) {
                                Get get = JsonUtils.getObj(lastAction.optJSONObject("get").toString(), Get.class);
                                if ("roadcondition".equals(get.getProperty())) {     //查看路况(导航中)
                                    Routenode routenode = SyncSegment.fromJson(lastTarget.toString(), Routenode.class);
                                    text = NavigatorService.get().pointTraffic(routenode.getRoadcondition(), routenode.getName(), msgBuilder);
                                } else if ("turnstring".equals(get.getProperty())) {        //询问前面怎么走
                                    if (BaiduNaviSuperManager.isNaviInited() && BNavigatorProxy.getInstance().isNaviBegin()) {
                                        StringBuilder sb = new StringBuilder();
                                        Bundle nb = RGSimpleGuideModelProxy.getInstance().getNextGuideInfo();
                                        if (null != nb) {
                                            int remainDist = nb.getInt("remain_dist", 0);
                                            if (remainDist >= 0) {
                                                sb.append(StringUtils.formatDistanceToChineseString(remainDist));
                                                sb.append("后");
                                            }
                                            String iconName = nb.getString("icon_name");
                                            if (!TextUtils.isEmpty(iconName)) {
                                                int i = 0;
                                                for (; i < RouteGuideParams.gTurnIconName.length; i++) {
                                                    if (iconName.equals(RouteGuideParams.gTurnIconName[i])) {
                                                        break;
                                                    }
                                                }
                                                if (i < RouteGuideParams.gTurnTypeDescForFollowInfo.length) {
                                                    sb.append(i == 0 ? "掉头" : RouteGuideParams.gTurnTypeDescForFollowInfo[i]);
                                                }
                                            }
                                            String nextRoad = nb.getString("road_name");
                                            if (!TextUtils.isEmpty(nextRoad)) {
                                                sb.append("进入");
                                                sb.append(nextRoad);
                                            }
                                        }
                                        text = sb.toString();
                                    } else {
                                        text = "你不在导航中，我无法给你建议";
                                    }
                                }
                            }
                            break;
                    }
                    break;
                case RobotConstant.ACTION_PLAT:
                    if (action == RobotConstant.SET) {
                        Plat plat = SyncSegment.fromJson(lastTarget.toString(), Plat.class);
                        switch (plat.getScale().getType()) {
                            case 1:     //放大地图
                                if (EventBus.getDefault().hasSubscriberForEvent(MapCmdEvent.class)) {
                                    EventBus.getDefault().post(new MapCmdEvent(MapCmdEvent.ZOOM_IN));
                                } else {
                                    text = "你已关闭地图，无法完成你的要求";
                                }
                                break;
                            case 3:     //缩小地图
                                if (EventBus.getDefault().hasSubscriberForEvent(MapCmdEvent.class)) {
                                    EventBus.getDefault().post(new MapCmdEvent(MapCmdEvent.ZOOM_OUT));
                                } else {
                                    text = "你已关闭地图，无法完成你的要求";
                                }
                                break;
                        }
                    }
                    break;
            }

            //将回复文本发送到聊天列表
            EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(text), null, null, null));
            //合成回复文本
            if (inputType == AssistantService.INPUT_VOICE) {
                msgBuilder.setText(text);
                Log.i("", "回复文本>>>>>>>>:" + text);
                SynthesizerBase.get().startSpeakAbsolute(msgBuilder.build())
                        .doOnComplete(new Action() {
                            @Override
                            public void run() throws Exception {
                                if (!TextUtils.isEmpty(NavigatorService.get().getPushText())) {
                                    Intent intent = new Intent(mContext, AssistantService.class);
                                    intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PUSH_ROUTE_CACULATE);
                                    intent.putExtra(AssistantService.TEXT, NavigatorService.get().getPushText());
                                    mContext.startService(intent);
                                    NavigatorService.get().resetPushText();
                                }
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation())
                        .subscribe();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 验证是否安装百度地图
     **/
    private boolean installBaiduMap() {
        try {
            mContext.getPackageManager().getApplicationInfo("com.baidu.BaiduMap", PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * 获取路线中需要避开的途经点
     **/
    private int getLineAvoidPassPoint(String point) {
        int cPrefrence = BNRoutePlanerProxy.getInstance().getCalcPreference();
        int cScheme = -1;
        Map<Integer, Vector<RoutePlanModelProxy>> routePlans = BNRoutePlanerProxy.getInstance().routePlans;
        Vector<RoutePlanModelProxy> routePlanModels = routePlans.get(cPrefrence);
        int l = routePlanModels.size();
        if (l > 1) {
            for (int i = 0; i < l; i++) {//检查当前线路的其它方案是否包含该point
                if (i != RouteModel.getCalculateScheme()) {
                    for (RoutePlanResultItemProxy item : routePlanModels.get(i).getRouteNodeData()) {
                        if (item.getNextRoadName().contains(point)) {
                            if (cScheme < 0)
                                cScheme = i;
                            break;
                        } else {
                            cScheme = -1;
                        }
                    }
                    if (cScheme < 0) {
                        return cPrefrence << 3 | i;
                    }
                }
            }
        }
        for (Integer key : routePlans.keySet()) {
            if (key != BNRoutePlanerProxy.getInstance().getCalcPreference()) {
                cPrefrence = key;
                routePlanModels = routePlans.get(cPrefrence);
                l = routePlanModels.size();
                for (int i = 0; i < l; i++) {//检查当前线路的其它方案是否包含该point
                    for (RoutePlanResultItemProxy item : routePlanModels.get(i).getRouteNodeData()) {
                        if (item.getNextRoadName().contains(point)) {
                            if (cScheme < 0)
                                cScheme = i;
                            break;
                        } else {
                            cScheme = -1;
                        }
                    }
                    if (cScheme < 0) {
                        return cPrefrence << 3 | i;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * 语音合成后伴随后续动作
     **/
    private void executeAfterSpeak(String preText, final String afterText, final int type, SpeechMsgBuilder msgBuilder) {
        msgBuilder.setText(preText).setForceLocalEngine(true);
        SynthesizerBase.get().startSpeakAbsolute(msgBuilder.build())
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        if (EventBus.getDefault().hasSubscriberForEvent(NavigateEvent.class)) {
                            EventBus.getDefault().post(new NavigateEvent(type));
                        } else {
                            SynthesizerBase.get().startSpeakAbsolute(SpeechMsgBuilder.create(afterText).build())
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(Schedulers.computation())
                                    .subscribe();
                            EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(afterText), null, null, null));
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe();
        EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(preText), null, null, null));
    }

    /**
     * 合成文本并提前返回
     **/

    private void speakAndAheadReturn(String text, SpeechMsgBuilder msgBuilder) {
    /* 将回复文本发送到聊天列表 */
        EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(text), null, null, null));
        /* 合成回复文本 */
        msgBuilder.setText(text).setForceLocalEngine(true);
        SynthesizerBase.get().startSpeakAbsolute(msgBuilder.build())
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        EventBus.getDefault().post(new NavigateEvent(NavigateEvent.START_NAVI));
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe();
    }

    /**
     * 在后台线程向机器人发送不需要回复的信息
     **/
    private void sendNoNeedBackground() {
        Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> e) throws Exception {
                Intent intent = new Intent(mContext, AssistantService.class);
                intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PUSH_ROUTE_CACULATE);
                intent.putExtra(AssistantService.TEXT, "不需要");
                intent.putExtra(AssistantService.CALLBACK, false);
            }
        })
                .subscribeOn(Schedulers.io())
                .subscribe();
    }
}
