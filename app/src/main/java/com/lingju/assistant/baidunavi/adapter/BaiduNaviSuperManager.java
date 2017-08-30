package com.lingju.assistant.baidunavi.adapter;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import com.baidu.navisdk.adapter.BNOuterTTSPlayerCallback;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.baidu.navisdk.adapter.base.BaiduNaviSDKLoader;
import com.baidu.navisdk.adapter.impl.RouteGuider;
import com.baidu.vi.VDeviceAPI;
import com.lingju.lbsmodule.entity.RouteModel;
import com.lingju.lbsmodule.proxy.BNMapControllerProxy;
import com.lingju.lbsmodule.proxy.BNRoutePlanObserverProxy;
import com.lingju.lbsmodule.proxy.BNRoutePlanerProxy;
import com.lingju.lbsmodule.proxy.BNavigatorProxy;
import com.lingju.lbsmodule.proxy.MapParams;
import com.lingju.lbsmodule.proxy.MapStatusProxy;
import com.lingju.lbsmodule.proxy.RoutePlanModelProxy;
import com.lingju.lbsmodule.proxy.ZeroZeroProxy;
import com.lingju.common.log.Log;
import com.lingju.util.ScreenUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Administrator on 2015/7/15.
 */
public class BaiduNaviSuperManager {
    private final static String TAG = "BaiduNaviSuperManager";

    public static final int[] LEVELS = {0, 6000000, 4000000, 2000000, 1000000, 500000, 200000, 100000,
            50000, 25000, 20000, 10000, 50000, 2000, 1000, 500, 200, 100, 50, 20, 10};

    public final static int MIN_TRAFFIC_DIST = 150;

    private Activity context;
    private String mSDCardPath;
    private String authinfo = null;
    public static final String APP_FOLDER_NAME = "lingjuNaviDemo";
    private Queue<Integer> routeModes = new ConcurrentLinkedQueue<Integer>();
    private ArrayList<BNRoutePlanNode> nodes;
    private int preference;
    private int preferenceErrorCount;
    private Handler handler;
    public final static int MSG_GET_ROUTE_LINE = 1;
    public final static int MSG_GET_LAST_ROUTE_LINE = 2;
    public final static int MSG_GET_NO_NETWORK = 3;

    private static boolean selectLineState = false;
    private static BNOuterTTSPlayerCallback defaultPlayerCallback;
    /**
     * 0=非切换路线，1=未导航中切换路线，2=导航中切换路线
     */
    private int switchLineCaculate = 0;
    private GLSurfaceView mMapView;


    public BaiduNaviSuperManager(Activity activity, final BaiduNaviManager.NaviInitListener naviInitListener, Handler handler) {
        this(activity, naviInitListener, handler, true);
    }

    public BaiduNaviSuperManager(Activity activity, final BaiduNaviManager.NaviInitListener naviInitListener, Handler handler, boolean havePlayerCallback) {
        this.context = activity;
        if (handler != null)
            this.handler = handler;
        BaiduTTSPlayerCallBack.haveTTS = havePlayerCallback;
        initDirs();
        if (defaultPlayerCallback == null) {
            defaultPlayerCallback = new BaiduTTSPlayerCallBack(context.getApplicationContext());
        }
        if (!BaiduNaviManager.getInstance().isNaviInited()) {
            BaiduNaviManager.getInstance().init(context, mSDCardPath, APP_FOLDER_NAME,
                    naviInitListener, defaultPlayerCallback, null, null);
        } else {
            //            BaiduNaviManager.getInstance().resetActivity(activity, defaultPlayerCallback);
            if (this.handler != null)
                this.handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        naviInitListener.initSuccess();
                    }
                }, 10);
            Log.i(TAG, "already inited .........................");
        }

    }

    public static BaiduTTSPlayerCallBack getDefaultPlayerCallback() {
        return defaultPlayerCallback == null ? null : (BaiduTTSPlayerCallBack) defaultPlayerCallback;
    }

    public static boolean isNaviInited() {
        return BaiduNaviManager.getInstance().isNaviInited();
    }


    public static String getScaleString(int level) {
        if (level <= MapParams.Const.MAX_ZOOM_LEVEL && level >= MapParams.Const.MIN_ZOOM_LEVEL) {
            if (LEVELS[level] > 500) {
                return LEVELS[level] / 1000 + "公里";
            } else
                return LEVELS[level] + "米";
        }
        return "未知";
    }

    public static boolean isSelectLineState() {
        return selectLineState;
    }

    public static void setSelectLineState(boolean state) {
        selectLineState = state;
    }

    public void destory() {
        /** uninit():调用该方法会导致下次进入导航页面时黑屏 **/
        //        BaiduNaviManager.getInstance().uninit();
        try {
            if (BaiduNaviManager.isNaviInited())
                BaiduNaviSDKLoader.getSDKClassLoader()
                        .loadClass("com.baidu.navisdk.BNaviModuleManager").getMethod("destory").invoke(null);
            VDeviceAPI.unsetNetworkChangedCallback();
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*BNMapControllerProxy.destory();
        RouteGuider.get().destory();*/
        BNavigatorProxy.destory();
    }

    /**
     * 直接调用该方法释放导航资源，在下次进入导航界面时会黑屏
     **/
    @Deprecated
    public void exit() {
        //退出应用时调用
        BaiduNaviManager.getInstance().uninit();
    }

    /**
     * 创建地图图层
     **/
    public GLSurfaceView createNMapView() {
        Log.i(TAG, "createNMapView");
        if (mMapView == null) {
            DisplayMetrics displaymetrics = context.getResources().getDisplayMetrics();
            Bundle bundle = new Bundle();
            bundle.putInt("screen_width", displaymetrics.widthPixels);
            bundle.putInt("screen_height", displaymetrics.heightPixels);
            mMapView = (GLSurfaceView) BNMapControllerProxy.getInstance().initMapView(context, bundle);
            BNMapControllerProxy.getInstance().attachMapView2Factory(mMapView);
            //            BNMapControllerProxy.getInstance().initMapStatus(new GeoPointProxy((int) (113.36699 * 1e5), (int) (23.097335 * 1e5)));
        }
        return mMapView;
    }

    public final static int BASE_CALCULATE_MODES[] = new int[]{
            BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TIME | BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM,
            BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_DIST | BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_AVOID_TAFFICJAM,
            BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_MIN_TOLL,
            BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND
    };

    /**
     * 根据导航节点和偏好计算路线
     **/
    public void routePlan(ArrayList<BNRoutePlanNode> nodes, int preference) {
        switchLineCaculate = 0;
        for (int calculateMode : BASE_CALCULATE_MODES) {
            if (preference != calculateMode) {
                /* 将算路偏好放入队列中 */
                routeModes.offer(calculateMode);
            }
        }
        /* 最后放入选中算路偏好 */
        routeModes.offer(preference);
        this.nodes = nodes;
        this.preference = routeModes.poll();
        RouteModel.clearCache();
        RouteGuider.get().routePlan(nodes, this.preference, routePlanObserver);
        zoomToRouteBound(nodes.get(0), nodes.get(nodes.size() - 1));
        selectLineState = true;
    }

    public void routePlanSingle(ArrayList<BNRoutePlanNode> nodes, int preference) {
        switchLineCaculate = BNavigatorProxy.getInstance().isNaviBegin() ? 2 : 1;
        routeModes.clear();
        this.nodes = nodes;
        this.preference = preference;
        RouteModel.clearCache();
        RouteGuider.get().routePlan(nodes, this.preference, routePlanObserver);
        //zoomToRouteBound(nodes.get(0), nodes.get(nodes.size()-1));
        selectLineState = true;
    }

    /**
     * 算路结果回调
     **/
    private BNRoutePlanObserverProxy routePlanObserver = new BNRoutePlanObserverProxy() {


        @Override
        public void update(Object o, int type, int event, Object arg) {
            Log.e(TAG, "type is " + type + "event is" + event);
            if (type == 1) {
                if (event == 2) {
                    try {
                        Log.e(TAG, "百度导航算路成功");
                        RoutePlanModelProxy mRoutePlanModel = RoutePlanModelProxy.getCacheRoutePlanModelProxy("RoutePlanModel");
                        Log.i(TAG, "算路方式：" + preference + ",距离：" + mRoutePlanModel.getDistance() + ",routeCnt=" + BNRoutePlanerProxy.getInstance().getRouteCnt() + ",currentMsrl=" + mRoutePlanModel.getMultiRouteCurrentMSRL() + "," + mRoutePlanModel.getNodeNum() + "," + BNRoutePlanerProxy.getInstance().getRemainedDestList().size());
                        Log.i(TAG, "第一段路名:" + mRoutePlanModel.getFirstRoadName() + ",时间：" + mRoutePlanModel.getTotalTime() + ",花费：" + mRoutePlanModel.getTollFees() + ",油费：" + mRoutePlanModel.getGasMoney() + ",主要道路：" + mRoutePlanModel.getMainRoads());
                        /*ArrayList<RoutePlanResultItem> rList=mRoutePlanModel.getRouteNodeData();
                        for(RoutePlanResultItem ri:rList){
                            System.out.println(ri.getNextRoadName()+",condition="+ri.getRoadCondition()+",night="+ri.getNodeDescNight()+",////////"+ri.getNodeDesc());
                        }*/
                        // 获取路线规划结果起点
                        BNRoutePlanNode startNode = mRoutePlanModel.getStartNode();
                        // 获取路线规划结果终点
                        BNRoutePlanNode endNode = mRoutePlanModel.getEndNode();
                        if (null == startNode || null == endNode) {
                            return;
                        }
                        Log.e(TAG, "start:" + startNode.getName() + ",end:" + endNode.getName());
                   /* BNMapController.getInstance().setLayerMode(
                            MapParams.Const.LayerMode.MAP_LAYER_MODE_ROUTE_DETAIL_FOR_NAVI);
                    BNMapController.getInstance().updateLayer(MapParams.Const.LayerType.MAP_LAYER_TYPE_ROUTE);*/

                        if (routeModes.size() != 0) {
                            RoutePlanModelProxy.clearModelCache("RoutePlanModel");
                            BNRoutePlanerProxy.getInstance().init(context.getApplication());
                            if (handler != null) {
                                Message msg = new Message();
                                msg.what = MSG_GET_ROUTE_LINE;
                                msg.arg1 = preference;
                                msg.obj = mRoutePlanModel;
                                handler.sendMessage(msg);
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.e(TAG, " BNRoutePlanManager.getInstance().routePlan>>>>>>start..................................");
                                        RouteGuider.get().routePlan(nodes, preference = routeModes.poll(), routePlanObserver);
                                    }
                                });
                            }
                        } else {
                            Log.e(TAG, "所有算法方法均已完成！！！！！！！！！！！！！！！！！！！！！！！");
                            preferenceErrorCount = 0;
                            selectLineState = true;
                            switchLineCaculate = BNavigatorProxy.getInstance().isNaviBegin() ? 2 : switchLineCaculate;
                            if (handler != null) {
                                Message msg = new Message();
                                msg.what = MSG_GET_LAST_ROUTE_LINE;
                                msg.arg1 = preference;
                                msg.arg2 = switchLineCaculate;
                                msg.obj = mRoutePlanModel;
                                handler.sendMessage(msg);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (event == 3 || event == 6 || event == 18 || event == 19) {
                    Log.e(TAG, "event>>>>" + event);
                    Log.e(TAG, "百度导航算路失败");
                    if (preferenceErrorCount == 0)
                        preferenceErrorCount = preference << 3 | 1;
                    else {
                        int count = preferenceErrorCount ^ (preference << 3);
                        if (count > 0 && count < 8) {
                            if (count > 2) {
                                if (routeModes != null) {
                                    preference = routeModes.poll();
                                }
                            } else {
                                preferenceErrorCount++;
                            }
                        } else {
                            preferenceErrorCount = preference << 3 | 1;
                        }
                    }

                    RoutePlanModelProxy.clearModelCache("RoutePlanModel");
                    BNRoutePlanerProxy.getInstance().init(context.getApplication());
                    RouteGuider.get().routePlan(nodes, preference, routePlanObserver);
                }
            }
        }
    };

    private boolean initDirs() {
        mSDCardPath = getSdcardDir();
        if (mSDCardPath == null) {
            return false;
        }
        File f = new File(mSDCardPath, APP_FOLDER_NAME);
        if (!f.exists()) {
            try {
                f.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private String getSdcardDir() {
        if (Environment.getExternalStorageState().equalsIgnoreCase(
                Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return null;
    }

    /*public static GeoPointProxy getRoutePlanGeoPoint(BNRoutePlanNode node) {
        if(node == null) {
            return null;
        } else {
            String LLX = "LLx";
            String LLY = "LLy";
            int ratio = 100000;
            GeoPointProxy point = new GeoPointProxy();
            Bundle bundle;
            int longtitudeE6;
            int latitudeE6;
            if(node.getCoordinateType() == BNRoutePlanNode.CoordinateType.BD09_MC) {
                bundle = JNIToolsProxy.getInstance().MC2LL((int) node.getLongitude(), (int) node.getLatitude());
                longtitudeE6 = (int)(bundle.getDouble("LLx") * 100000.0D);
                latitudeE6 = (int)(bundle.getDouble("LLy") * 100000.0D);
                point.setLatitudeE6(latitudeE6);
                point.setLongitudeE6(longtitudeE6);
                return point;
            } else {
                if(node.getCoordinateType() == BNRoutePlanNode.CoordinateType.WGS84) {
                    bundle = JNIToolsProxy.getInstance().WGS2GCJ(node.getLongitude(), node.getLatitude());
                    longtitudeE6 = (int)(bundle.getDouble("LLx") * 100000.0D);
                    latitudeE6 = (int)(bundle.getDouble("LLy") * 100000.0D);
                    point.setLatitudeE6(latitudeE6);
                    point.setLongitudeE6(longtitudeE6);
                } else {
                    point.setLatitudeE6((int)(node.getLatitude() * 100000.0D));
                    point.setLongitudeE6((int)(node.getLongitude() * 100000.0D));
                }

                return point;
            }
        }
    }*/

    public static final double R = 6378137;

    private int _Yoffest = 0;
    private int _Xoffest = 0;
    private boolean flag = true;

    public void zoomToRouteBound(BNRoutePlanNode start, BNRoutePlanNode end) {
        LatLng ws = new LatLng(start.getLatitude(), start.getLongitude());
        LatLng we = new LatLng(start.getLatitude(), end.getLongitude());
        int hd = (int) DistanceUtil.getDistance(ws, we);

        LatLng hs = new LatLng(start.getLatitude(), start.getLongitude());
        LatLng he = new LatLng(end.getLatitude(), start.getLongitude());
        int vd = (int) DistanceUtil.getDistance(hs, he);
        //Log.e(TAG,"hd="+hd+",vd="+vd);
        int level = getScales(hd, vd, Math.min(start.getLatitude(), end.getLatitude()));
        MapStatusProxy ms = BNMapControllerProxy.getInstance().getMapStatus();
        //Log.e(TAG,"level="+level);
        ms._Level = level;
        double cLatitude = Math.min(start.getLatitude(), end.getLatitude()) + Math.abs((start.getLatitude() - end.getLatitude()) / 2);
        double cLogitude = Math.min(start.getLongitude(), end.getLongitude()) + Math.abs((start.getLongitude() - end.getLongitude()) / 2);
        //Log.e(TAG,"lat="+cLatitude+",lng="+cLogitude);
        Bundle b1 = ZeroZeroProxy.bala2((int) (cLogitude * 1e5), (int) (cLatitude * 1e5));
        if (b1 != null) {
            ms._CenterPtX = b1.getInt("MCx");
            ms._CenterPtY = b1.getInt("MCy");
        }
        /*if (!BNavigatorProxy.getInstance().isNaviBegin()) {
            _Yoffest = 205;
            _Xoffest = 45;
        } else {
            _Yoffest = 60;
            _Xoffest = 30;
        }
        ms._Yoffset = ScreenUtil.getInstance().dip2px(_Yoffest);
        ms._Xoffset = 0 - ScreenUtil.getInstance().dip2px(_Xoffest);*/
        if (!BNavigatorProxy.getInstance().isNaviBegin())
            ms._Yoffset = ScreenUtil.getInstance().dip2px(100);

        BNMapControllerProxy.getInstance().setMapStatus(ms, BNMapControllerProxy.AnimationType.eAnimationLevel);
    }

    public static int getScales(int hd, int vd, double lat) {
        int w = ScreenUtil.getInstance().getWidthPixels();
        int h = ScreenUtil.getInstance().getHeightPixels();
        double latw = Math.cos(lat * Math.PI / 180) * 2 * Math.PI * R;
        //Log.e(TAG,"www="+(latw/256/2)+",20after="+(latw/(256*Math.pow(2,20))));
        int hl = (int) log(latw * w / (hd * 256), 2);
        int vl = (int) log(latw * h / (vd * 256), 2);
        //Log.e(TAG,"hl="+hl+",vl="+vl);
        return Math.min(hl, vl) - 1;
    }

    static public double log(double value, double base) {
        return Math.log(value) / Math.log(base);
    }

    public static String formatDisatance(int distance) {
        if (distance > 0) {
            if (distance < 1000) {
                return distance + "米";
            } else {
                return distance / 1000.0D + "公里";
            }
        }
        return "0";
    }



    /*public static void parseConfigParams(Bundle configParams) {
        if(configParams != null) {
            BNavConfig.pRGViewMode = configParams.getInt("routeguide_view_mode");
            BNavConfig.pRGCalcDone = configParams.getInt("calroute_done");
            BNavConfig.pRGStartX = configParams.getInt("start_x");
            BNavConfig.pRGStartY = configParams.getInt("start_y");
            BNavConfig.pRGEndX = configParams.getInt("end_x");
            BNavConfig.pRGEndY = configParams.getInt("end_y");
            BNavConfig.pRGStartName = configParams.getString("start_name");
            BNavConfig.pRGEndName = configParams.getString("end_name");
            BNavConfig.pRGLocateMode = configParams.getInt("locate_mode");
            LogUtil.e("RouteGuide", "pRGLocateMode = " + BNavConfig.pRGLocateMode);
            if(configParams.containsKey("menu_type")) {
                BNavConfig.pRGMenuType = configParams.getInt("menu_type");
            }

            if(configParams.containsKey("net_refresh")) {
                BNavConfig.pRGNetRefreshEnable = configParams.getBoolean("net_refresh");
            }

            if(configParams.containsKey("road_condition")) {
                BNavConfig.pRGRoadConditionEnable = configParams.getBoolean("road_condition");
            }

            LogUtil.e("RouteGuide", "pRGMenuType = " + BNavConfig.pRGMenuType);
        }
    }*/

}
