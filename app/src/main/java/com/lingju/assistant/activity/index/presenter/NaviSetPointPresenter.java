package com.lingju.assistant.activity.index.presenter;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;

import com.baidu.location.BDLocation;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.utils.DistanceUtil;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.NaviSetLineActivity;
import com.lingju.assistant.activity.NaviSetPointActivity;
import com.lingju.assistant.activity.SetFavoriteMapActivity;
import com.lingju.assistant.activity.index.INaviSetPoint;
import com.lingju.assistant.baidunavi.adapter.BaiduNaviSuperManager;
import com.lingju.assistant.view.CommonAlertDialog;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.common.log.Log;
import com.lingju.lbsmodule.location.Address;
import com.lingju.lbsmodule.location.BaiduLocateManager;
import com.lingju.lbsmodule.location.LocateListener;
import com.lingju.lbsmodule.proxy.BNRoutePlanObserverProxy;
import com.lingju.lbsmodule.proxy.BNRoutePlanerProxy;
import com.lingju.lbsmodule.proxy.RoutePlanModelProxy;
import com.lingju.model.BaiduAddress;
import com.lingju.model.NaviRecord;
import com.lingju.model.dao.BaiduNaviDao;
import com.lingju.model.dao.RecordDao;
import com.lingju.util.NetUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Ken on 2016/12/21.
 */
public class NaviSetPointPresenter implements INaviSetPoint.IPresenter {
    private static final String TAG = "NaviSetPointPresenter";
    private INaviSetPoint.INaviSetPointView mSetPointView;
    private BaiduNaviSuperManager mNaviSuperManager;
    private Handler mHandler;
    private NaviSetPointActivity mContext;
    private AppConfig mAppConfig;
    private BaiduAddress startAddress;
    private BaiduAddress endAddress;
    private BaiduAddress homeAddress;
    private BaiduAddress companyAddress;
    private int preference;
    private Address address;
    private BaiduAddress initAddress;
    private BaiduNaviDao mNaviDao;
    private RecordDao mRecordDao;
    private CommonAlertDialog cDialog;
    private int calculateType = -1;     //计算类型 0为家，1为单位


    /**
     * 从当前地点到家的节点（起点、（中间可能包括的途经点）、终点）集合
     **/
    private ArrayList<BNRoutePlanNode> goHomeNodes = new ArrayList<>();
    /**
     * 同上
     **/
    private ArrayList<BNRoutePlanNode> goCompanyNodes = new ArrayList<>();
    /**
     * 导航记录集合
     */
    private List<NaviRecord> recordList = new ArrayList<>();

    public NaviSetPointPresenter(INaviSetPoint.INaviSetPointView view, AppConfig app) {
        this.mSetPointView = view;
        this.mAppConfig = app;
        this.mContext = (NaviSetPointActivity) view;
    }

    @Override
    public void setLocation() {
        /* 获取当前所定位的地址 */
        address = mAppConfig.address;
        /* 重新定位 */
        BaiduLocateManager.get().addObserver(locateListener);
        BaiduLocateManager.get().start();

    }

    @Override
    public void initData() {
        mNaviDao = BaiduNaviDao.getInstance();
        mRecordDao = RecordDao.getInstance();
        preference = AppConfig.dPreferences.getInt(NaviSetLineActivity.CALCULATE_MODE, BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND);
        System.out.println("这里初始化出发地地址");
        initStartAddress();

    }

    @Override
    public void initStartAddress() {
        BDLocation location = new BDLocation();
        location.setLatitude(address.getLatitude());
        location.setLongitude(address.getLongitude());
        location.setRadius(address.getRadius());
        location.setSpeed(address.getSpeed());
        location.setSatelliteNumber(address.getSatelliteNumber());
        // location = LocationClient.getBDLocationInCoorType(location, BDLocation.BDLOCATION_GCJ02_TO_BD09LL);

        //更新家和公司视图
        updateCompany();
        updateHome();
        final GeoCoder geoCoder = GeoCoder.newInstance();
        /* 设置地理编码查询监听者 */
        final BDLocation finalLocation = location;
        geoCoder.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
            @Override
            public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {

            }

            @Override
            public void onGetReverseGeoCodeResult(ReverseGeoCodeResult r) {
                List<PoiInfo> ls = r.getPoiList();
                if (ls != null && ls.size() > 0) {
                    initAddress = new BaiduAddress();
                    // String myLocation;
                    setBaiduAddressFromPoiInfo(initAddress, ls.get(0));
                    // myLocation = initAddress.getName();
                    initAddress.setAddress(address.getAddressDetail());
                    initAddress.setLatitude(finalLocation.getLatitude());
                    initAddress.setLongitude(finalLocation.getLongitude());
                    if (TextUtils.isEmpty(initAddress.getCity()))
                        initAddress.setCity(address.getCity());
                    BaiduAddress myStartAddr = new BaiduAddress();
                    myStartAddr.setLatitude(initAddress.getLatitude());
                    myStartAddr.setLongitude(initAddress.getLongitude());
                    myStartAddr.setName("我的位置" + "(" + initAddress.getName() + ")" + "附近");
                    if (mSetPointView.getStartAddrText().startsWith("我的位置")) {
                        myStartAddr.setRemark("出发地");
                        myStartAddr.setId(null);
                        myStartAddr.setFavoritedTime(null);
                        mAppConfig.startAddress = myStartAddr;
                    }
                    //设置默认回家和去公司的出发地址
                    // mAppConfig.myAddress = myLocation;
                    //                    updateCompany();
                    //                    updateHome();
                    //由init进入该方法时不触发导航
                    updateStart(false);
                    updateEnd(false);
                    updateHistoryList();
                    geoCoder.destroy();
                }
            }
        });
        geoCoder.reverseGeoCode(new ReverseGeoCodeOption().
                location(new LatLng(location.getLatitude(), location.getLongitude())));
    }

    /**
     * 将poi搜索信息填充到BaiduAddress对象中
     **/
    private void setBaiduAddressFromPoiInfo(BaiduAddress ad, PoiInfo poiInfo) {
        ad.setAddress(poiInfo.address);
        ad.setUid(poiInfo.uid);
        ad.setName(poiInfo.name);
        ad.setCity(poiInfo.city);
        ad.setCreated(new Date(System.currentTimeMillis()));
        ad.setHasCaterDetails(poiInfo.hasCaterDetails ? 1 : 0);
        ad.setIsPano(poiInfo.isPano ? 1 : 0);
        ad.setLatitude(poiInfo.location.latitude);
        ad.setLongitude(poiInfo.location.longitude);
        ad.setPhoneNum(poiInfo.phoneNum);
        ad.setPostCode(poiInfo.postCode);
    }

    /**
     * 更新回家视图
     */
    @Override
    public void updateHome() {
        homeAddress = mNaviDao.getHomeOrCompanyAddress(mAppConfig.getResources().getString(R.string.home));
        goHomeNodes.clear();
        if (homeAddress != null) {
            BNRoutePlanNode start = new BNRoutePlanNode(address.getLongitude(), address.getLatitude(), address.getAddressDetail(), "");
            BNRoutePlanNode end = new BNRoutePlanNode(homeAddress.getLongitude(), homeAddress.getLatitude(), homeAddress.getName(), "");
            goHomeNodes.add(start);
            goHomeNodes.add(end);
        }
    }

    /**
     * 更新去公司视图
     */
    @Override
    public void updateCompany() {
        companyAddress = mNaviDao.getHomeOrCompanyAddress(mAppConfig.getResources().getString(R.string.company));
        goCompanyNodes.clear();
        if (companyAddress != null) {
            // 计算两点之间的距离
           /* boolean countable = DistanceUtil.getDistance(new LatLng(address.getLatitude(), address.getLongitude()),
                    new LatLng(companyAddress.getLatitude(), companyAddress.getLongitude())) > 1000;*/
            BNRoutePlanNode start = new BNRoutePlanNode(address.getLongitude(), address.getLatitude(), address.getAddressDetail(), "");
            BNRoutePlanNode end = new BNRoutePlanNode(companyAddress.getLongitude(), companyAddress.getLatitude(), companyAddress.getName(), "");
            goCompanyNodes.add(start);
            goCompanyNodes.add(end);
        }

    }

    /**
     * 更新出发地视图
     */
    private void updateStart(boolean toNavi) {
        startAddress = mAppConfig.startAddress;
        if (startAddress != null) {
            System.out.println("updateStart，出发地地址不为空" + startAddress.getName());
            String start = startAddress.getName();
            mSetPointView.setStartAddrText(start);
            //获取数据库中的目的地地址
            BaiduAddress endAddr = mAppConfig.endAddress;
            if (endAddr != null && toNavi) {
                double dt = DistanceUtil.getDistance(new LatLng(startAddress.getLatitude(), startAddress.getLongitude()),
                        new LatLng(endAddr.getLatitude(), endAddr.getLongitude()));
                if (NetUtil.getInstance(mContext).getCurrentNetType().equals(NetUtil.NetType.NETWORK_TYPE_NONE)) {
                    final CommonDialog commonDialog = new CommonDialog(mContext, "网络错误", "网络状态不佳，请检查网络设置", "确定");
                    commonDialog.setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                        @Override
                        public void onConfirm() {
                            commonDialog.cancel();
                        }
                    }).show();
                    return;
                }
                if (dt > 50) {
                    //                        final CommonAlertDialog progressDialog = new CommonAlertDialog(mContext, "导航引擎", "线路规划中...");
                    //                        progressDialog.show();
                    final Intent intent = new Intent(mContext, NaviSetLineActivity.class); //你要转向的Activity
                    intent.putExtra("start_latitude", startAddress.getLatitude());
                    intent.putExtra("start_longitude", startAddress.getLongitude());
                    intent.putExtra("start_address", startAddress.getName());
                    intent.putExtra("end_latitude", endAddr.getLatitude());
                    intent.putExtra("end_longitude", endAddr.getLongitude());
                    intent.putExtra("end_address", endAddr.getName());
                    NaviRecord record = new NaviRecord();
                    record.setStartLatitude(startAddress.getLatitude());
                    record.setStartLongitude(startAddress.getLongitude());
                    record.setStartName(startAddress.getName());
                    record.setEndLatitude(endAddr.getLatitude());
                    record.setEndLongitude(endAddr.getLongitude());
                    record.setEndName(endAddr.getName());
                    record.setCreated(new Date(System.currentTimeMillis()));
                    mRecordDao.insertRecord(record);
                    //                        Timer timer = new Timer();
                    //                        TimerTask task = new TimerTask() {
                    //                            @Override
                    //                            public void run() {
                    mContext.startActivity(intent); //执行
                    mContext.goInto();
                    //                                progressDialog.cancel();
                    //                            }
                    //                        };
                    //                        timer.schedule(task, 2000); //2秒后
                } else {
                    // Toast.makeText(mContext, mAppConfig.getResources().getString(R.string.tooShortForCalculate), Toast.LENGTH_SHORT).show();
                    mSetPointView.showSnackBar(mAppConfig.getResources().getString(R.string.tooShortForCalculate));
                }
            }
        } else {
            mSetPointView.setStartAddrText("选择出发地");
        }

    }

    /**
     * 更新目的地视图
     */
    private void updateEnd(boolean toNavi) {
        endAddress = mAppConfig.endAddress;
        if (endAddress != null) {
            String end = endAddress.getName();
            mSetPointView.setEndAddrText(end);
            BaiduAddress startAddr = mAppConfig.startAddress;
            if (startAddr != null && toNavi) {
                double dt = DistanceUtil.getDistance(new LatLng(startAddr.getLatitude(), startAddr.getLongitude()),
                        new LatLng(endAddress.getLatitude(), endAddress.getLongitude()));
                if (NetUtil.getInstance(mContext).getCurrentNetType().equals(NetUtil.NetType.NETWORK_TYPE_NONE)) {
                    final CommonDialog commonDialog = new CommonDialog(mContext, "网络错误", "网络状态不佳，请检查网络设置", "确定");
                    commonDialog.setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                        @Override
                        public void onConfirm() {
                            commonDialog.cancel();
                        }
                    }).show();
                    return;
                }
                if (dt > 50) {
                    //                    final CommonAlertDialog progressDialog = new CommonAlertDialog(mContext, "导航引擎", "线路规划中...");
                    //                    progressDialog.show();
                    final Intent intent = new Intent(mContext, NaviSetLineActivity.class); //你要转向的Activity
                    intent.putExtra("start_latitude", startAddr.getLatitude());
                    intent.putExtra("start_longitude", startAddr.getLongitude());
                    intent.putExtra("start_address", startAddr.getName());
                    intent.putExtra("end_latitude", endAddress.getLatitude());
                    intent.putExtra("end_longitude", endAddress.getLongitude());
                    intent.putExtra("end_address", endAddress.getName());
                    NaviRecord record = new NaviRecord();
                    record.setStartLatitude(startAddr.getLatitude());
                    record.setStartLongitude(startAddr.getLongitude());
                    record.setStartName(startAddr.getName());
                    record.setEndLatitude(endAddress.getLatitude());
                    record.setEndLongitude(endAddress.getLongitude());
                    record.setEndName(endAddress.getName());
                    record.setCreated(new Date(System.currentTimeMillis()));
                    mRecordDao.insertRecord(record);
                    Timer timer = new Timer();
                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            mContext.startActivity(intent); //执行
                            mContext.goInto();
                            //                            progressDialog.cancel();
                        }
                    };
                    timer.schedule(task, 1000); //1秒后
                } else {
                    mSetPointView.showSnackBar(mAppConfig.getResources().getString(R.string.tooShortForCalculate));
                    //  Toast.makeText(mContext, mAppConfig.getResources().getString(R.string.tooShortForCalculate), Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            mSetPointView.setEndAddrText("选择目的地");
        }
    }

    @Override
    public void initBaiduNaiv(Activity activity, Handler handler) {
        if (!BaiduNaviSuperManager.isNaviInited()) {
            this.mHandler = handler;
            mNaviSuperManager = new BaiduNaviSuperManager(activity, naviInitListener, handler, false);
        }

    }

    @Override
    public void updateData(int resultCode) {

        if (resultCode > 16) {
            if (resultCode == Msg.UPDATE_HOME) {
                homeAddress = mNaviDao.getHomeOrCompanyAddress(mAppConfig.getResources().getString(R.string.home));
                updateHome();
            }
            if (resultCode == Msg.UPDATE_COMPANY) {
                companyAddress = mNaviDao.getHomeOrCompanyAddress(mAppConfig.getResources().getString(R.string.company));
                updateCompany();
            }
            if (resultCode == Msg.UPDATE_START) {
                startAddress = mAppConfig.startAddress;
                updateStart(true);
            }
            if (resultCode == Msg.UPDATE_END) {
                endAddress = mAppConfig.endAddress;
                updateEnd(true);
            }

            /*if ((resultCode & Msg.UPDATE_HISTORY) == Msg.UPDATE_HISTORY) {
                updateHistoryList();
            }*/
        }
    }


    @Override
    public void updateHistoryList() {

    }

    @Override
    public void cleanHistory() {

    }

    @Override
    public boolean isUpdateHistory() {
        return false;
    }

    @Override
    public void setUpdateHistory(boolean updateHistory) {

    }

    @Override
    public void toSetStartAddr() {
        Intent intent = new Intent(mContext, SetFavoriteMapActivity.class);
        if (startAddress != null) {
            System.out.println("设置出发地地址" + startAddress);
            intent.putExtra("address", startAddress);
        }
        intent.putExtra(SetFavoriteMapActivity.MODE, SetFavoriteMapActivity.SET_START);
        mContext.startActivityForResult(intent, Msg.UPDATE_START);
        mContext.goInto();
    }

    @Override
    public void toSetEndAddr() {
        Intent intent = new Intent(mContext, SetFavoriteMapActivity.class);
        if (endAddress != null) {
            System.out.println("设置目的地地址" + endAddress);
            intent.putExtra("address", endAddress);
        }
        intent.putExtra(SetFavoriteMapActivity.MODE, SetFavoriteMapActivity.SET_END);
        mContext.startActivityForResult(intent, Msg.UPDATE_END);
        mContext.goInto();
    }

    @Override
    public void toSetHomeAddr() {
        Intent intent = new Intent(mContext, SetFavoriteMapActivity.class);
        //homeAddress = mNaviDao.getHomeOrCompanyAddress(mAppConfig.getResources().getString(R.string.home));
        if (homeAddress != null) {
            intent.putExtra("address", homeAddress);
        }
        intent.putExtra(SetFavoriteMapActivity.MODE, SetFavoriteMapActivity.SET_HOME);
        mContext.startActivityForResult(intent, Msg.UPDATE_HOME);
        mContext.goInto();
    }

    @Override
    public void toHomeNavi() {
        BDLocation bl = new BDLocation();
        bl.setLatitude(address.getLatitude());
        bl.setLongitude(address.getLongitude());
        // bl = LocationClient.getBDLocationInCoorType(bl, BDLocation.BDLOCATION_GCJ02_TO_BD09LL);
        double dt = DistanceUtil.getDistance(new LatLng(bl.getLatitude(), bl.getLongitude()), new LatLng(homeAddress.getLatitude(), homeAddress.getLongitude()));
        if (dt < 50) {
            mSetPointView.showSnackBar(mAppConfig.getResources().getString(R.string.inHome));
            // Toast.makeText(mContext, mAppConfig.getResources().getString(R.string.inHome), Toast.LENGTH_SHORT).show();
        }//else if(dt>100000){
        //            Toast.makeText(mContext, mAppConfig.getResources().getString(R.string.tooLongForCalculate), Toast.LENGTH_SHORT).show();
        //        }
        else {
            //没有网络进行提示
            if (NetUtil.getInstance(mContext).getCurrentNetType().equals(NetUtil.NetType.NETWORK_TYPE_NONE)) {
                final CommonDialog commonDialog = new CommonDialog(mContext, "网络错误", "网络状态不佳，请检查网络设置", "确定");
                commonDialog.setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                    @Override
                    public void onConfirm() {
                        commonDialog.cancel();
                    }
                }).show();
                return;
            }
            Intent intent = new Intent(mContext, NaviSetLineActivity.class);
            intent.putExtra("latitude", homeAddress.getLatitude());
            intent.putExtra("longitude", homeAddress.getLongitude());
            intent.putExtra("address", homeAddress.getName());
            mContext.startActivity(intent);
            mContext.goInto();
        }
    }

    @Override
    public void toSetCompanyAddr() {
        Intent intent = new Intent(mContext, SetFavoriteMapActivity.class);
        // companyAddress = mNaviDao.getHomeOrCompanyAddress(mAppConfig.getResources().getString(R.string.company));
        if (companyAddress != null) {
            companyAddress.print();
            intent.putExtra("address", companyAddress);
        }
        intent.putExtra(SetFavoriteMapActivity.MODE, SetFavoriteMapActivity.SET_COMPANY);
        mContext.startActivityForResult(intent, Msg.UPDATE_COMPANY);
        mContext.goInto();
    }

    @Override
    public void toCompanyNavi() {
        BDLocation bl = new BDLocation();
        bl.setLatitude(address.getLatitude());
        bl.setLongitude(address.getLongitude());
        // bl = LocationClient.getBDLocationInCoorType(bl, BDLocation.BDLOCATION_GCJ02_TO_BD09LL);
        double dt = DistanceUtil.getDistance(new LatLng(bl.getLatitude(), bl.getLongitude()), new LatLng(companyAddress.getLatitude(), companyAddress.getLongitude()));
        if (dt < 50) {
            mSetPointView.showSnackBar(mAppConfig.getResources().getString(R.string.inCompany));
            //  Toast.makeText(mContext, mAppConfig.getResources().getString(R.string.inCompany), Toast.LENGTH_SHORT).show();

        } else {
            //没有网络进行提示
            if (NetUtil.getInstance(mContext).getCurrentNetType().equals(NetUtil.NetType.NETWORK_TYPE_NONE)) {
                final CommonDialog commonDialog = new CommonDialog(mContext, "网络错误", "网络状态不佳，请检查网络设置", "确定");
                commonDialog.setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                    @Override
                    public void onConfirm() {
                        commonDialog.cancel();
                    }
                }).show();
                return;
            }
            Intent intent = new Intent(mContext, NaviSetLineActivity.class);
            intent.putExtra("latitude", companyAddress.getLatitude());
            intent.putExtra("longitude", companyAddress.getLongitude());
            intent.putExtra("address", companyAddress.getName());
            mContext.startActivity(intent);
            mContext.goInto();
        }

    }


    @Override
    public void destoryNaviManager() {
        if (mNaviSuperManager != null)
            mNaviSuperManager.destory();
    }

    @Override
    public void destoryListener() {

    }

    /**
     * 交换出发地与目的地
     */
    @Override
    public void exchangeAddress() {
        BaiduAddress temp = mAppConfig.startAddress;
        mAppConfig.startAddress = mAppConfig.endAddress;
        mAppConfig.endAddress = temp;
        if (mAppConfig.startAddress != null) {
            mAppConfig.startAddress.setRemark("出发地");
            mAppConfig.startAddress.setFavoritedTime(null);
        }
        if (mAppConfig.endAddress != null) {
            mAppConfig.endAddress.setRemark("目的地");
            mAppConfig.endAddress.setFavoritedTime(null);
        }
        startAddress = mAppConfig.startAddress;
        endAddress = mAppConfig.endAddress;
        String start;
        String end;
        if (startAddress != null) {
            start = startAddress.getName();
        } else {
            start = "选择出发地";
        }
        if (endAddress != null) {
            end = endAddress.getName();
        } else {
            end = "选择目的地";
        }
        mSetPointView.setStartAddrText(start);
        mSetPointView.setEndAddrText(end);
    }

    /**
     * 导航引擎初始化回调
     **/
    private BaiduNaviManager.NaviInitListener naviInitListener = new BaiduNaviManager.NaviInitListener() {

        public void initSuccess() {
            Log.e(TAG, "百度导航引擎初始化成功");
            if (goHomeNodes.size() > 0) {
                setGoHomeCalculate();
            } else if (goCompanyNodes.size() > 0) {
                setGoCompanyCalculate();
            }
            //测试,下载百度离线导航资源包
            //BNOfflineDataManager.getInstance().startDownloadRequest(19);
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

    public void setGoCompanyAndGoHomeCalculate() {
        if (BaiduNaviSuperManager.isNaviInited()) {
            if (goHomeNodes.size() > 0) {
                setGoHomeCalculate();
            } else if (goCompanyNodes.size() > 0) {
                setGoCompanyCalculate();
            }
        }
    }

    /**
     * 计算回家时间
     **/
    private void setGoHomeCalculate() {
        if (goHomeNodes.size() > 0) {
            BNRoutePlanerProxy.getInstance().setObserver(observer);
            BNRoutePlanerProxy.getInstance().setCalcPrference(preference);
            calculateType = 0;
            BNRoutePlanerProxy.getInstance().setPointsToCalcRoute(goHomeNodes, 1, false, null, 0);
        }
    }

    /**
     * 计算去公司时间
     **/
    private void setGoCompanyCalculate() {
        if (goCompanyNodes.size() > 0) {
            BNRoutePlanerProxy.getInstance().setObserver(observer);
            BNRoutePlanerProxy.getInstance().setCalcPrference(preference);
            calculateType = 1;
            BNRoutePlanerProxy.getInstance().setPointsToCalcRoute(goCompanyNodes, 1, false, null, 0);
        }
    }

    /**
     * 算路结果回调
     **/
    BNRoutePlanObserverProxy observer = new BNRoutePlanObserverProxy() {

        @Override
        public void update(Object bnSubject, int type, int event, Object object) {
            if (cDialog != null) {
                cDialog.cancel();
            }
            if (type == 1) {
                if (event == 2) {
                    Log.e(TAG, "百度导航算路成功");
                    /* 获取路线规划模型对象 */
                    RoutePlanModelProxy routePlanModel = RoutePlanModelProxy.getCacheRoutePlanModelProxy("RoutePlanModel");
                    /* 获取起点 */
                    BNRoutePlanNode startNode = routePlanModel.getStartNode();
                    /* 获取终点 */
                    BNRoutePlanNode endNode = routePlanModel.getEndNode();
                    if (startNode == null || endNode == null) {
                        return;
                    }
                    Log.e(TAG, "start:" + startNode.getName() + ",end:" + endNode.getName());
                    if (calculateType == 0) {    //计算回家时间
                        if (mSetPointView.getGoHomeTextView() != null) {
                            mSetPointView.getGoHomeTextView().setText(routePlanModel.getTotalTime());
                        }
                        System.out.println("回家需要" + routePlanModel.getTotalTime());
                        routePlanModel.clearRouteResult();
                        if (goCompanyNodes.size() > 0) {
                            setGoCompanyCalculate();
                        }
                    } else if (calculateType == 1) {      //去单位
                        if (mSetPointView.getGoCompanyTextView() != null) {
                            mSetPointView.getGoCompanyTextView().setText(routePlanModel.getTotalTime());
                        }
                        System.out.println("去单位需要" + routePlanModel.getTotalTime());
                        routePlanModel.clearRouteResult();
                    }

                } else if (event == 3 || event == 6 || event == 18 || event == 19) {
                    Log.e(TAG, "百度导航算路失败");
                    if (calculateType == 1) {
                        boolean countable = DistanceUtil.getDistance(new LatLng(address.getLatitude(), address.getLongitude()),
                                new LatLng(companyAddress.getLatitude(), companyAddress.getLongitude())) > 1000;
                        if (mSetPointView.getGoCompanyTextView() != null && !countable)
                            mSetPointView.getGoCompanyTextView().setText(mContext.getResources().getString(R.string.inCompany));
                    } else {
                        boolean countable = DistanceUtil.getDistance(new LatLng(address.getLatitude(), address.getLongitude()),
                                new LatLng(homeAddress.getLatitude(), homeAddress.getLongitude())) > 1000;
                        if (mSetPointView.getGoHomeTextView() != null && !countable)
                            mSetPointView.getGoHomeTextView().setText(mContext.getResources().getString(R.string.inHome));
                    }
                }
            }
        }
    };


    /**
     * 定位结果回调
     **/
    private LocateListener locateListener = new LocateListener(LocateListener.CoorType.BD09LL) {
        @Override
        public void update(Address address) {
            NaviSetPointPresenter.this.address = address;
            mAppConfig.address = address;
        }

    };

    public interface Msg {
        int UPDATE_HOME = 17;
        int UPDATE_COMPANY = 18;
        /*int UPDATE_HISTORY = 20;*/
        int UPDATE_FAVORITE = 24;
        int UPDATE_START = 25;
        int UPDATE_END = 26;
    }

    @Override
    public void destroy() {
        Log.i(TAG, "estroy");
        if (mNaviSuperManager != null)
            mNaviSuperManager.destory();
        if (BNRoutePlanerProxy.getInstance().isCalculatingRoute()) {
            Log.i(TAG, "isCalculatingRoute");
            BNRoutePlanerProxy.getInstance().cancleCalcRouteRequest();
        }
        BaiduLocateManager.get().deleteObserver(locateListener);
        mContext = null;
    }
}
