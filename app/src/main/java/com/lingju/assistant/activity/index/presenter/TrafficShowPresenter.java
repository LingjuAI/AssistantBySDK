package com.lingju.assistant.activity.index.presenter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.drawable.LevelListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.LingjuSwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.OverlayManager;
import com.baidu.mapapi.overlayutil.PoiOverlay;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.baidu.mapapi.utils.DistanceUtil;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.NaviSetLineActivity;
import com.lingju.assistant.activity.event.MapCmdEvent;
import com.lingju.assistant.activity.event.NaviShowPointsEvent;
import com.lingju.assistant.activity.event.OnItemClickListener;
import com.lingju.assistant.activity.index.ITrafficShow;
import com.lingju.assistant.baidunavi.adapter.BaiduNaviSuperManager;
import com.lingju.assistant.service.AssistantService;
import com.lingju.assistant.view.AdaptHeightListView;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.assistant.view.DrawForExpandLayout;
import com.lingju.assistant.view.RealTimeUpdateSearchBox;
import com.lingju.assistant.view.SelectCityDialog;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.common.log.Log;
import com.lingju.lbsmodule.location.Address;
import com.lingju.lbsmodule.location.BaiduLocateManager;
import com.lingju.lbsmodule.location.LocateListener;
import com.lingju.lbsmodule.proxy.BNRoutePlanObserverProxy;
import com.lingju.lbsmodule.proxy.BNRoutePlanerProxy;
import com.lingju.lbsmodule.proxy.RoutePlanModelProxy;
import com.lingju.model.BaiduAddress;
import com.lingju.model.dao.BaiduNaviDao;
import com.lingju.util.NetUtil;
import com.lingju.util.ScreenUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Dyy on 2017/3/10.
 */
public class TrafficShowPresenter implements ITrafficShow.IPresenter, OnGetPoiSearchResultListener, OnGetSuggestionResultListener, LingjuSwipeRefreshLayout.OnRefreshListener {
    private final static String TAG = "TrafficShowPresenter";
    private final static int MSG_SHOW_GO_WHERE = 1;
    private ITrafficShow.INaviSetPointView mTrafficShowView;
    private NaviPointAdapter mListAdapter;
    private Activity mContext;
    private final AppConfig mAppConfig;
    private final static int PER_PAGE = 10;
    private int poiPageCode = 0;
    private MapView mapView;
    private BaiduMap baiduMap;
    private PoiSearch pSearch;
    private SuggestionSearch sSearch;
    private String keyword;
    private Address address;
    private BaiduNaviDao mNaviDao;
    private OverlayManager poiOverlay;
    private int totalPageNum = Integer.MAX_VALUE;
    private SelectCityDialog selectCityDialog;
    private LayoutInflater inflater;
    private BaiduAddress homeAddress;
    private BaiduAddress companyAddress;
    private boolean voiceMode;
    private int calculateType = -1;
    private int preference;
    private BaiduNaviSuperManager naviSuperManager;
    final List<PoiInfo> poiInfoList = new ArrayList<PoiInfo>();
    final List<BaiduAddress> aList = new ArrayList<BaiduAddress>();
    private ArrayList<BNRoutePlanNode> goHomeNodes = new ArrayList<>();
    private ArrayList<BNRoutePlanNode> goCompanyNodes = new ArrayList<>();

    public TrafficShowPresenter(ITrafficShow.INaviSetPointView view, MapView mapView) {
        this.mTrafficShowView = view;
        this.mapView = mapView;
        mContext = (Activity) view;
        mAppConfig = (AppConfig) mContext.getApplication();
    }


    @Override
    public void initData() {
        EventBus.getDefault().register(this);
        mNaviDao = BaiduNaviDao.getInstance();
        baiduMap = mapView.getMap();
          /* 是否允许定位图层 */
        baiduMap.setMyLocationEnabled(true);
        /* 设置缩放级别，改变地图状态 */
        baiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(17f));
          /* 获取家和单位地址 */
        homeAddress = mNaviDao.getHomeOrCompanyAddress(mContext.getResources().getString(R.string.home));
        companyAddress = mNaviDao.getHomeOrCompanyAddress(mContext.getResources().getString(R.string.company));
        address = mAppConfig.address.clone();
        pSearch = PoiSearch.newInstance();
        pSearch.setOnGetPoiSearchResultListener(this);
        sSearch = SuggestionSearch.newInstance();
        sSearch.setOnGetSuggestionResultListener(this);
        // mTrafficShowView.getSearchBt().setVisibility(View.GONE);
        preference = mAppConfig.dPreferences.getInt(NaviSetLineActivity.CALCULATE_MODE, BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND);
        BaiduLocateManager.get().addObserver(locateListener);
        mTrafficShowView.getSuggestListView().setItemClickListener(suggestItemClickListener);
        inflater = LayoutInflater.from(mContext);
        ScreenUtil.getInstance().init(mContext);
        mListAdapter = new NaviPointAdapter();
        mTrafficShowView.getPoiListView().setAdapter(mListAdapter);
        // mTrafficShowView.getPoiListView().addItemDecoration(new SimpleLineDivider(R.color.new_line_border));
        mTrafficShowView.getPoiListView().setLayoutManager(new LinearLayoutManager(mContext));
        //  mTrafficShowView.getPoiListView().setOnItemClickListener(listItemClickListener);
        mListAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onClick(View itemView, int position) {
                showPoiDetail(position);
            }

            @Override
            public void onLongClick(View intemView, int position) {

            }
        });
        mTrafficShowView.getRefreshLayout().setOnRefreshListener(this);
        mTrafficShowView.getPoiListBox().setScaleChangeListener(scaleChangedListener);
        mTrafficShowView.getPoiDetailPager().setAdapter(pagerAdapter);
        mTrafficShowView.getPoiDetailPager().setOnPageChangeListener(pageChangeListener);
        mTrafficShowView.getedit().setSearchListener(searchListener);

        mTrafficShowView.getPoiListView().post(new Runnable() {
            @Override
            public void run() {
                mTrafficShowView.getPoiListBox().setMaxHeight(mContext.findViewById(R.id.ast_map_box).getMeasuredHeight() - (int) (mContext.getResources().getDisplayMetrics().density * 48.0f + 0.5f));
                resetScaleToolPosition();
            }
        });
        /* 是否打开交通路况图层 */
        baiduMap.setTrafficEnabled(true);
        ((ImageButton) mContext.findViewById(R.id.ast_map_its_bt)).setImageResource(R.drawable.bnav_common_ic_map_its_on);
        /* 算路偏好设置（默认：ROUTE_PLAN_MOD_RECOMMEND推荐模式） */
        preference = mAppConfig.dPreferences.getInt(NaviSetLineActivity.CALCULATE_MODE, BaiduNaviManager.RoutePlanPreference.ROUTE_PLAN_MOD_RECOMMEND);
        updateHomeAndCompany();
        List<BaiduAddress> list;
        if (mContext.getIntent() == null || (list = mContext.getIntent().getParcelableArrayListExtra("addresses")) == null)
            location();
        else {
            voiceMode = true;
            aList.clear();
            aList.addAll(list);
            poiOverlay = new ConfirmCustomPoiOverlay(baiduMap);
            baiduMap.setOnMarkerClickListener(poiOverlay);
            ((ConfirmCustomPoiOverlay) poiOverlay).setOverlayOptions(list);
            poiOverlay.addToMap();
            setListReverse();
            mListAdapter.notifyDataSetChanged();
            pagerAdapter.notifyDataSetChanged();

            setPoiPosition();
            //locationBounds();
        }
        if (!voiceMode) {
            if (!BaiduNaviSuperManager.isNaviInited()) {
                naviSuperManager = new BaiduNaviSuperManager(mContext, naviInitListener, hanler, false);
            } else {
                if (goHomeNodes.size() > 0) {
                    setGoHomeCalculate();
                } else if (goCompanyNodes.size() > 0) {
                    setGoCompanyCalculate();
                }
            }
        }
    }

    @Override
    public void setPoiDetailPagerToNext() {
        if (mTrafficShowView.getPoiDetailPager().getCurrentItem() >= 0 && mTrafficShowView.getPoiDetailPager().getCurrentItem() < poiInfoList.size() - 1) {
            mTrafficShowView.getPoiDetailPager().setCurrentItem(mTrafficShowView.getPoiDetailPager().getCurrentItem() + 1);
        }
    }

    @Override
    public void setPoiDetailPagerToPre() {
        if (mTrafficShowView.getPoiDetailPager().getCurrentItem() > 0 && mTrafficShowView.getPoiDetailPager().getCurrentItem() < poiInfoList.size()) {
            mTrafficShowView.getPoiDetailPager().setCurrentItem(mTrafficShowView.getPoiDetailPager().getCurrentItem() - 1);
        }
    }

    @Override
    public void goSearch() {
        goSearch(null);
    }

    /**
     * 城市内检索目标
     **/
    private void goSearch(BaiduAddress ba) {
        if (mContext.getCurrentFocus() != null) {
            ((InputMethodManager) mContext.getSystemService(mContext.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(mContext.getCurrentFocus()
                                    .getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);
        }
        mContext.findViewById(R.id.ast_suggest_list_box).setVisibility(View.GONE);
        /* 城市内检索 */
        pSearch.searchInCity(new PoiCitySearchOption().city(mAppConfig.selectedCityInSearchPoi == null ?
                address.getCity() : mAppConfig.selectedCityInSearchPoi).pageCapacity(10).pageNum(poiPageCode).keyword(ba == null ? keyword : ba.getName()));
        if (ba == null) {
            ba = new BaiduAddress();
            ba.setName(keyword);
            ba.setAddress("");
            ba.setCity(address.getCity());
        }
        ba.setSearchKeyWord(keyword);
        ba.setCreated(new Timestamp(System.currentTimeMillis()));
        mNaviDao.insertAddress(ba);
    }

    /**
     * 地图定位
     **/
    public void location() {
        location(null);
    }

    /**
     * 地图定位
     **/
    @Override
    public void locateManagerStart() {
        BaiduLocateManager.get().start();
    }


    private void location(BaiduAddress ba) {
        baiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(15F));
        BDLocation location = new BDLocation();
        if (ba == null) {
            location.setLatitude(address.getLatitude());
            location.setLongitude(address.getLongitude());
            location.setRadius(address.getRadius());
            location.setSpeed(address.getSpeed());
            location.setSatelliteNumber(address.getSatelliteNumber());
            // location = LocationClient.getBDLocationInCoorType(location, BDLocation.BDLOCATION_GCJ02_TO_BD09LL);
        } else {
            location.setLatitude(ba.getLatitude());
            location.setLongitude(ba.getLongitude());
            location.setRadius(10);
            location.setAddrStr(ba.getName());
        }
        baiduMap.setMyLocationEnabled(true);
        /* 配置定位图层显示方式（FOLLOWING 跟随态，保持定位图标在地图中心） */
        baiduMap.setMyLocationConfigeration(new MyLocationConfiguration(MyLocationConfiguration.LocationMode.FOLLOWING, true, null));
        /* 设置定位数据, 只有先允许定位图层后设置数据才会生效 */
        baiduMap.setMyLocationData(new MyLocationData.Builder().latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .accuracy(location.getRadius())
                .direction(location.getDirection())
                .satellitesNum(location.getSatelliteNumber())
                .speed(location.getSpeed())
                .build());
    }

    @Override
    public void onGetPoiResult(PoiResult result) {
        Log.e(TAG, "onGetPoiResult>>" + result.error);
        mTrafficShowView.setRefreshLayout(false);
        if (result == null || result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
            //Toast.makeText(mContext, "搜索发生错误：" + result.error, Toast.LENGTH_LONG).show();
            mTrafficShowView.showSnackBar(mContext.getResources().getString(R.string.navi_no_result));
            mTrafficShowView.getedit().setSearchCompletedState();
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            //baiduMap.clear();
            if (poiOverlay != null) {
                /* 在地图上清楚覆盖物 */
                poiOverlay.removeFromMap();
            }
            if (poiOverlay == null || !(poiOverlay instanceof ConfirmPoiOverlay)) {
                poiOverlay = new ConfirmPoiOverlay(baiduMap);
                /* 设置覆盖物被点击事件 */
                baiduMap.setOnMarkerClickListener(poiOverlay);
            }

            ((ConfirmPoiOverlay) poiOverlay).setData(result);
            /* 将覆盖物添加到地图中 */
            poiOverlay.addToMap();
            setPoiPosition();

            totalPageNum = result.getTotalPageNum();
            mTrafficShowView.getPoiListBox().getLayoutParams().height = ScreenUtil.getInstance().getHeightPixels() / 2;
            poiInfoList.clear();
            poiInfoList.addAll(result.getAllPoi());
            setAlist();
            mListAdapter.notifyDataSetChanged();
            pagerAdapter.notifyDataSetChanged();
            mTrafficShowView.getPoiDetialBox().setVisibility(View.GONE);
            mTrafficShowView.getPoiListBox().setVisibility(View.VISIBLE);
            /*MapStatus.Builder builder=new MapStatus.Builder();
            builder.target(list.get(0).location)
                    .targetScreen(new Point(ScreenUtil.getInstance().getWidthPixels()/2,ScreenUtil.getInstance().getHeightPixels()/4))
                    .zoom(15F);
            baiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));*/
            return;
        }

    }

    @Override
    public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

    }

    @Override
    public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

    }


    @Override
    public void onGetSuggestionResult(SuggestionResult sr) {
        Log.i(TAG, "onGetSuggestionResult>>");
        if (sr == null || sr.error != SearchResult.ERRORNO.NO_ERROR || TextUtils.isEmpty(keyword)) {
            Log.e(TAG, "搜索发生错误：" + sr.error);
            return;
        }
        List<SuggestionResult.SuggestionInfo> list = sr.getAllSuggestions();
        if (list == null || list.isEmpty())
            return;
        if (mAppConfig.selectedCityInSearchPoi == null) {
            float l = list.size();
            int cityCount = 0;
            String maxCity = null;
            Map<String, Integer> cityMap = new Hashtable<String, Integer>();
            for (SuggestionResult.SuggestionInfo info : list) {
                if (info.city.equals("")) {
                    l--;
                } else {
                    if (!cityMap.containsKey(info.city)) {
                        cityMap.put(info.city, 1);
                    } else {
                        cityMap.put(info.city, cityMap.get(info.city) + 1);
                    }
                }
            }
            for (String key : cityMap.keySet()) {
                if (maxCity == null) {
                    maxCity = key;
                    cityCount = cityMap.get(key);
                } else if (cityMap.get(key) > cityCount) {
                    maxCity = key;
                    cityCount = cityMap.get(key);
                }
            }

            if (cityCount / l < 0.5) {
                selectCityDialog = new SelectCityDialog(mContext, cityMap.keySet().toArray(new String[cityMap.size()]), new SelectCityDialog.OnSelectListener() {
                    @Override
                    public void onSelect(String city) {
                        Log.i(TAG, "select city=" + city);
                        mAppConfig.selectedCityInSearchPoi = city;
                        sSearch.requestSuggestion(new SuggestionSearchOption().city(mAppConfig.selectedCityInSearchPoi).keyword(keyword));
                    }
                });
                selectCityDialog.show();
                return;
            } else {
                mAppConfig.selectedCityInSearchPoi = maxCity;//maxCity.indexOf(address.getCity())==-1?maxCity:null;
            }
        }

        BaiduAddress temp;
        List<BaiduAddress> suggests = new ArrayList<BaiduAddress>();
        for (SuggestionResult.SuggestionInfo info : list) {
            Log.e(TAG, "city=" + info.city + ",strict=" + info.district + ",key=" + info.key + ",uid=" + info.uid);
            if (mAppConfig.selectedCityInSearchPoi != null) {
                if (!info.city.equals(mAppConfig.selectedCityInSearchPoi))
                    continue;
            }
            temp = new BaiduAddress();
            temp.setCity(info.city);
            temp.setAddress(info.city + info.district);
            temp.setName(info.key);
            temp.setUid(info.uid);
            suggests.add(temp);
        }
        mTrafficShowView.getSuggestListView().loadDate(suggests, keyword);
        mContext.findViewById(R.id.ast_suggest_list_box).setVisibility(View.VISIBLE);
        mTrafficShowView.getedit().setSearchCompletedState();
    }

    @Override
    public void onDownPullRefresh() {
        Log.i(TAG, "onDownPullRefresh");
        if (poiPageCode == 0) {
            mTrafficShowView.setRefreshLayout(false);
            mTrafficShowView.showSnackBar("当前已是第一页了");
            //Toast.makeText(mContext, "当前已是第一页了", Toast.LENGTH_LONG).show();
        } else
            pSearch.searchInCity(new PoiCitySearchOption().city(mAppConfig.selectedCityInSearchPoi == null ?
                    address.getCity() : mAppConfig.selectedCityInSearchPoi).keyword(keyword).pageCapacity(PER_PAGE).pageNum(--poiPageCode));
    }

    @Override
    public void onUpPullRefresh() {
        Log.i(TAG, "onUpPullRefresh");
        if (poiPageCode == (totalPageNum - 1)) {
            mTrafficShowView.setRefreshLayout(false);
            mTrafficShowView.showSnackBar("没有符合要求的地点了");
            //  Toast.makeText(mContext, "没有符合要求的地点了", Toast.LENGTH_LONG).show();
        } else
            pSearch.searchInCity(new PoiCitySearchOption().city(mAppConfig.selectedCityInSearchPoi == null ?
                    address.getCity() : mAppConfig.selectedCityInSearchPoi).keyword(keyword).pageCapacity(PER_PAGE).pageNum(++poiPageCode));

    }


    private BaiduNaviManager.NaviInitListener naviInitListener = new BaiduNaviManager.NaviInitListener() {

        public void initSuccess() {
            Log.e(TAG, "百度导航引擎初始化成功");
            if (goHomeNodes.size() > 0) {
                setGoHomeCalculate();
            } else if (goCompanyNodes.size() > 0) {
                setGoCompanyCalculate();
            }
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
     * 搜索结果地址列表适配器
     **/
    class NaviPointAdapter extends RecyclerView.Adapter<NaviPointAdapter.NaviPointHolder> {


        @Override
        public NaviPointHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = inflater.inflate(R.layout.target_poi_item, parent, false);
            return new NaviPointHolder(itemView);
        }

        @Override
        public void onBindViewHolder(NaviPointHolder holder, int position) {
            PoiInfo poiInfo = poiInfoList.get(position);
            holder.mTpiNameText.setText((position + 1) + "." + poiInfo.name);
            holder.mTpiAddressText.setText(poiInfo.address);

            holder.mTpiItemConfirmText.setText("出发");
            holder.mTpiDistanceText.setVisibility(View.VISIBLE);
            double distance = DistanceUtil.getDistance(new LatLng(address.getLatitude(), address.getLongitude()),
                    poiInfo.location) / 1000;
            holder.mTpiDistanceText.setText(String.format("%.1f", distance) + "km");

            holder.mTpiTargetConfirmBt.setTag(position);
            holder.itemView.setTag(position);
        }

        @Override
        public int getItemCount() {
            return poiInfoList.size();
        }

        private OnItemClickListener itemClickListener;

        public void setOnItemClickListener(OnItemClickListener listener) {
            itemClickListener = listener;
        }

        class NaviPointHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.tpi_name_text)
            TextView mTpiNameText;
            @BindView(R.id.tpi_address_text)
            TextView mTpiAddressText;
            @BindView(R.id.tpi_item_confirm_text)
            TextView mTpiItemConfirmText;
            @BindView(R.id.tpi_distance_text)
            TextView mTpiDistanceText;
            @BindView(R.id.tpi_target_confirm_bt)
            LinearLayout mTpiTargetConfirmBt;

            public NaviPointHolder(final View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = (int) itemView.getTag();
                        if (itemClickListener != null)
                            itemClickListener.onClick(v, position);
                    }
                });
                mTpiTargetConfirmBt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Integer position = (Integer) mTpiTargetConfirmBt.getTag();
                        if (position != null) {
                            PoiInfo poiInfo = poiInfoList.get(position);
                            intoNaviSetLine(position, poiInfo);
                        }
                    }
                });
            }
        }
    }
    //    private BaseAdapter listAdapter = new BaseAdapter() {
    //
    //        @Override
    //        public int getCount() {
    //            return poiInfoList.size();
    //        }
    //
    //        @Override
    //        public Object getItem(int position) {
    //            return position;
    //        }
    //
    //        @Override
    //        public long getItemId(int position) {
    //            return position;
    //        }
    //
    //        private OnItemClickListener itemClickListener;
    //
    //        public void setOnItemClickListener(OnItemClickListener listener) {
    //            itemClickListener = listener;
    //        }
    //
    //        @Override
    //        public View getView(int position, View convertView, ViewGroup parent) {
    //            if (convertView == null) {
    //                convertView = inflater.inflate(R.layout.target_poi_item, null);
    //                convertView.findViewById(R.id.tpi_target_confirm_bt).setOnClickListener(clickListener);
    //
    //            }
    //            convertView.findViewById(R.id.tpi_target_confirm_bt).setTag(position);
    //            ((TextView) convertView.findViewById(R.id.tpi_name_text)).setText((position + 1) + "." + poiInfoList.get(position).name);
    //            ((TextView) convertView.findViewById(R.id.tpi_address_text)).setText(poiInfoList.get(position).address);
    //            Double distance = DistanceUtil.getDistance(new LatLng(address.getLatitude(), address.getLongitude()),
    //                    poiInfoList.get(position).location) / 1000;
    //            ((TextView) convertView.findViewById(R.id.tpi_distance_text)).setText(String.format("%.1f", distance) + "km");
    //            return convertView;
    //        }
    //
    //        private View.OnClickListener clickListener = new View.OnClickListener() {
    //            @Override
    //            public void onClick(View v) {
    //                Integer position = (Integer) v.getTag();
    //                Log.i("position:",""+position);
    //                if (position != null) {
    //                    PoiInfo poiInfo = poiInfoList.get(position);
    //                    intoNaviSetLine(position, poiInfo);
    //                }
    //            }
    //        };
    //
    //    };

    /**
     * 进入设置导航路线页面
     **/
    private void intoNaviSetLine(int index, PoiInfo info) {
        if (voiceMode) {
            if (SynthesizerBase.isInited()) {
                SynthesizerBase.get().stopSpeakingAbsolte();
            }
            Intent is = new Intent(mContext, AssistantService.class);
            is.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_RECOGNIZE);
            mContext.startService(is);
            is.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PUSH_ROUTE_CACULATE);
            is.putExtra("text", "第" + (index + 1) + "个");
            is.putExtra(AssistantService.CALLBACK, true);
            mContext.startService(is);
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
            intent.putExtra("latitude", info.location.latitude);
            intent.putExtra("longitude", info.location.longitude);
            intent.putExtra("address", info.name);
            mContext.startActivity(intent);

        }
    }

    private RealTimeUpdateSearchBox.OnSearchListener searchListener = new RealTimeUpdateSearchBox.OnSearchListener() {


        @Override
        public void editClick() {

        }

        @Override
        public void onSearchTextUpdate(String text) {
            Log.i(TAG, "onSearchTextUpdate>>" + text);
            keyword = text;
            if (!TextUtils.isEmpty(text)) {
                sSearch.requestSuggestion(new SuggestionSearchOption().city(mAppConfig.selectedCityInSearchPoi == null ?
                        address.getCity() : mAppConfig.selectedCityInSearchPoi).keyword(text));
            } else {
                mAppConfig.selectedCityInSearchPoi = null;
                mContext.findViewById(R.id.ast_suggest_list_box).setVisibility(View.GONE);
                mTrafficShowView.getedit().setSearchIdleState();
                // mTrafficShowView.getSearchBt().setVisibility(View.GONE);
                //   mTrafficShowView.getSearchVoiceBt().setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onSearchSuggestCompleted() {
            // mTrafficShowView.getSearchBt().setVisibility(View.VISIBLE);
            // mTrafficShowView.getSearchVoiceBt().setVisibility(View.GONE);
        }

        @Override
        public void onSearch(String text) {
            keyword = text;
            goSearch();
        }
    };

    private ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            Log.i(TAG, "pageChangeListener>>onPageSelected>>" + position);
            updatePoiDetialDotList(poiInfoList.size(), position);
            locationPoi(poiInfoList.get(position));
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };
    AdaptHeightListView.OnItemClickListener suggestItemClickListener = new AdaptHeightListView.OnItemClickListener() {
        @Override
        public void onClick(BaiduAddress address) {
            Log.i(TAG, "suggestItemClickListener>>onClick" + address.getName());
            mTrafficShowView.getedit().setTextNoUpdate(address.getName());
            keyword = address.getName();
            goSearch(address);
        }

        @Override
        public void onSelect(BaiduAddress address) {
            Log.i(TAG, "suggestItemClickListener>>onSelect" + address.getName());
            this.onClick(address);
        }
    };


    private ListView.OnItemClickListener listItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.e(TAG, "onItemClick>>>" + position);
            showPoiDetail(position);
        }
    };
    private PagerAdapter pagerAdapter = new PagerAdapter() {

        private Map<Integer, View> chilren = new Hashtable<Integer, View>();

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (chilren.get(position) != null)
                ((ViewPager) container).removeView(chilren.remove(position));
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Log.i(TAG, "instantiateItem>>>" + position);
            View child = chilren.get(position);
            if (child == null) {
                child = inflater.inflate(R.layout.target_poi_item_detail, null);
                chilren.put(position, child);
            }
            child.findViewById(R.id.tpid_target_confirm_bt).setOnClickListener(clickListener);
            child.findViewById(R.id.tpid_target_confirm_bt).setTag(position);
            ((TextView) child.findViewById(R.id.tpid_name_text)).setText((position + 1) + "." + aList.get(position).getName());
            ((TextView) child.findViewById(R.id.tpid_address_text)).setText(aList.get(position).getAddress());
            Double distance = DistanceUtil.getDistance(new LatLng(address.getLatitude(), address.getLongitude()),
                    poiInfoList.get(position).location) / 1000;
            ((TextView) child.findViewById(R.id.tpid_distance_text)).setText(String.format("%.1f", distance) + "km");
            child.findViewById(R.id.tpid_set_target_bt).setOnClickListener(clickListener);
            child.findViewById(R.id.tpid_set_target_bt).setTag(position);
            TextView ft = (TextView) child.findViewById(R.id.tpid_favorite_bt);
            ft.setOnClickListener(clickListener);
            ft.setTag(position);
            if (aList.get(position).getFavoritedTime() != null) {
                ft.setText("已收藏");
                ((LevelListDrawable) ft.getBackground()).setLevel(1);
            }
            container.addView(child, 0);
            return chilren.get(position);
        }

        @Override
        public int getCount() {
            return aList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        private View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "pagerAdapter>>>onClick>>>>");
                if (v.getTag() == null || !(v.getTag() instanceof Integer))
                    return;
                int index = (Integer) v.getTag();
                Log.i(TAG, "pagerAdapter>>>onClick>>>>index=====" + index);
                switch (v.getId()) {
                    case R.id.tpid_target_confirm_bt:
                    case R.id.tpid_set_target_bt:
                        if (voiceMode) {
                            if (SynthesizerBase.isInited()) {
                                // SynthesizerBase.get().setWakeUpMode(SynthesizerBase.WU_WAKEMODE);
                                SynthesizerBase.get().stopSpeakingAbsolte();
                            }
                            Intent is = new Intent(mContext, AssistantService.class);
                            is.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_RECOGNIZE);
                            mContext.startService(is);

                            is.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PUSH_ROUTE_CACULATE);
                            is.putExtra("text", "第" + (index + 1) + "个");
                            is.putExtra(AssistantService.CALLBACK, true);
                            mContext.startService(is);
                        } else {
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
                            intent.putExtra("latitude", poiInfoList.get(index).location.latitude);
                            intent.putExtra("longitude", poiInfoList.get(index).location.longitude);
                            intent.putExtra("address", poiInfoList.get(index).name);
                            mContext.startActivity(intent);
                        }
                        mContext.finish();
                        break;
                    case R.id.tpid_favorite_bt:
                        TextView ft = (TextView) v;
                        if (aList.get(index).getFavoritedTime() != null) {
                            aList.get(index).setFavoritedTime(null);
                            aList.get(index).setSynced(false);
                            ft.setText("未收藏");
                            ft.getBackground().setLevel(0);
                        } else {
                            aList.get(index).setFavoritedTime(new Date());
                            ft.setText("已收藏");
                            ft.getBackground().setLevel(1);
                        }
                        mNaviDao.insertAddress(aList.get(index));
                        mNaviDao.sync();
                        break;
                }
            }
        };

    };

    /**
     * 将检索结果以百度坐标对象集合的形式保存
     **/
    private void setAlist() {
        int al = aList.size();
        int l = poiInfoList.size();
        BaiduAddress t;
        for (int i = al; i > l; --i) {
            aList.remove(i - 1);
        }
        for (int i = 0; i < l; i++) {
            t = mNaviDao.find(poiInfoList.get(i).name, poiInfoList.get(i).location.latitude, poiInfoList.get(i).location.longitude);
            if (i < al) {
                if (t != null)
                    aList.set(i, t);
                else
                    setBaiduAddressFromPoiInfo(aList.get(i), poiInfoList.get(i));
            } else {
                if (t != null)
                    aList.add(t);
                else {
                    aList.add(new BaiduAddress());
                    setBaiduAddressFromPoiInfo(aList.get(i), poiInfoList.get(i));
                }
            }
        }

    }

    private class ConfirmPoiOverlay extends PoiOverlay {

        public ConfirmPoiOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public boolean onPoiClick(int i) {
            Log.e(TAG, "onPoiClick>>>" + i);
            super.onPoiClick(i);
            showPoiDetail(i);
            return true;
        }

        @Override
        public boolean onPolylineClick(Polyline polyline) {
            return super.onPolylineClick(polyline);
        }

        @Override
        public void zoomToSpan() {
            if (mapView != null) {
                try {
                    mapView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ConfirmPoiOverlay.super.zoomToSpan();
                            if (mTrafficShowView.getPoiListBox().getVisibility() == View.VISIBLE) {
                                MapStatus ms = baiduMap.getMapStatus();
                                ms.targetScreen.set(ScreenUtil.getInstance().getWidthPixels() / 2, ScreenUtil.getInstance().getHeightPixels() / 4);
                                baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(ms));
                            }
                        }
                    }, 300);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //    private DrawForExpandLayout.ScaleChangedListener scaleChangedListener = new DrawForExpandLayout.ScaleChangedListener() {
    //        @Override
    //        public void max() {
    //
    //        }
    //
    //        @Override
    //        public void min() {
    //            if (poiInfoList.size() == 0)
    //                return;
    //            int p = mTrafficShowView.getPoiListView().getFirstVisiblePosition();
    //            p = Math.max(p, 0);
    //            showPoiDetail(p);
    //        }
    //    };
    private DrawForExpandLayout.ScaleChangedListener scaleChangedListener = new DrawForExpandLayout.ScaleChangedListener() {

        /** 视图拖动到最大高度时调用 **/
        @Override
        public void max() {

        }

        /** 视图拖动到最新高度时调用 **/
        @Override
        public void min() {
            if (poiInfoList.size() == 0)
                return;
            int p = ((LinearLayoutManager) mTrafficShowView.getPoiListView().getLayoutManager()).findFirstVisibleItemPosition();
            p = Math.max(p, 0);
            showPoiDetail(p);
        }
    };

    @Subscribe
    public void onEventMainThread(NaviShowPointsEvent e) {
        Log.i(TAG, "onEventMainThread>>NaviShowPointsEvent");
        voiceMode = true;
        if (e.getType() < NaviShowPointsEvent.CLOSE_ACTIVITY) {
            mTrafficShowView.getRefreshLayout().setAllowDrag(false);
            if (e.getPoints() != null) {
                aList.clear();
                aList.addAll(e.getPoints());
                setListReverse();
                mListAdapter.notifyDataSetChanged();
                pagerAdapter.notifyDataSetChanged();
                mTrafficShowView.getPoiListBox().setVisibility(View.VISIBLE);

                if (poiOverlay != null) {
                    poiOverlay.removeFromMap();
                }
                if (poiOverlay == null || !(poiOverlay instanceof ConfirmCustomPoiOverlay)) {
                    poiOverlay = new ConfirmCustomPoiOverlay(baiduMap);
                    baiduMap.setOnMarkerClickListener(poiOverlay);
                }

                ((ConfirmCustomPoiOverlay) poiOverlay).setOverlayOptions(aList);
                poiOverlay.addToMap();
                setPoiPosition();
            }
        } else if (e.getType() == NaviShowPointsEvent.CLOSE_ACTIVITY) {
            mContext.finish();
        } else if (e.getType() == NaviShowPointsEvent.CANCEL_TASK) {
            onBackPressed();
        }
    }


    public void setListReverse() {
        int al = aList.size();
        poiInfoList.clear();
        int l = Math.min(al, 10);
        for (int i = 0; i < l; i++) {
            PoiInfo poiInfo = new PoiInfo();
            poiInfo.name = aList.get(i).getName();
            poiInfo.uid = aList.get(i).getUid();
            poiInfo.address = aList.get(i).getAddress();
            poiInfo.city = aList.get(i).getCity();
            poiInfo.phoneNum = aList.get(i).getPhoneNum();
            poiInfo.postCode = aList.get(i).getPostCode();
            poiInfo.type = PoiInfo.POITYPE.POINT;
            poiInfo.location = new LatLng(aList.get(i).getLatitude(), aList.get(i).getLongitude());
            poiInfoList.add(poiInfo);
        }
    }

    public void onBackPressed() {
        if (voiceMode) {
            if (SynthesizerBase.isInited()) {
                SynthesizerBase.get().stopSpeakingAbsolte();
            }
            Intent intent = new Intent(mContext, AssistantService.class);
            intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_RECOGNIZE);
            mContext.startService(intent);
            intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PUSH_ROUTE_CACULATE);
            intent.putExtra("text", "取消");
            intent.putExtra(AssistantService.CALLBACK, false);
            mContext.startService(intent);
            mContext.finish();
        } else
            mContext.onBackPressed();
    }

    @Subscribe
    public void onEventMainThread(MapCmdEvent e) {
        closeDialog();
        switch (e.getCmd()) {
            case MapCmdEvent.SHOW_TRAFFIC: {
                if (e.getAddresses() == null) {
                    location();
                } else {
                    aList.clear();
                    aList.addAll(e.getAddresses());
                    setListReverse();
                    mListAdapter.notifyDataSetChanged();
                    pagerAdapter.notifyDataSetChanged();
                    if (poiOverlay == null) {
                        poiOverlay = new ConfirmCustomPoiOverlay(baiduMap);
                        baiduMap.setOnMarkerClickListener(poiOverlay);
                    } else {
                        poiOverlay.removeFromMap();
                    }
                    if (poiOverlay instanceof ConfirmCustomPoiOverlay) {
                        ((ConfirmCustomPoiOverlay) poiOverlay).setOverlayOptions(e.getAddresses());
                    }
                    poiOverlay.addToMap();
                    setPoiPosition();
                }
            }
            case MapCmdEvent.ZOOM_IN:
                baiduMap.setMapStatus(MapStatusUpdateFactory.zoomIn());
                break;
            case MapCmdEvent.ZOOM_OUT:
                baiduMap.setMapStatus(MapStatusUpdateFactory.zoomOut());
                break;
            case MapCmdEvent.ZOOM:
                if (e.getValue() > 0)
                    baiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(e.getValue()));
                break;
        }
    }

    private void closeDialog() {
        if (mTrafficShowView.getVoiceDialog() != null && mTrafficShowView.getVoiceDialog().isShowing()) {
            mTrafficShowView.getVoiceDialog().cancel();
        }
    }

    private class ConfirmCustomPoiOverlay extends OverlayManager {
        List<OverlayOptions> overlayOptionses;

        public ConfirmCustomPoiOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        public void setOverlayOptions(List<BaiduAddress> list) {
            if (list == null || list.size() == 0)
                return;
            overlayOptionses = new ArrayList<>();
            int l = Math.min(list.size(), 10);
            for (int i = 0; i < l; ++i) {
                Bundle bundle = new Bundle();
                bundle.putInt("index", i);
                overlayOptionses.add((new MarkerOptions()).icon(
                        BitmapDescriptorFactory.fromAssetWithDpi("Icon_mark" + (i + 1) + ".png")).
                        extraInfo(bundle).
                        position(new LatLng(list.get(i).getLatitude(), list.get(i).getLongitude())));
            }
        }

        @Override
        public List<OverlayOptions> getOverlayOptions() {
            return overlayOptionses;
        }

        public boolean onPoiClick(int i) {
            Log.e(TAG, "onPoiClick>>>" + i);
            //showPoiDetail(i);
            return true;
        }

        public boolean onMarkerClick(Marker var1) {
            return var1.getExtraInfo() != null ? this.onPoiClick(var1.getExtraInfo().getInt("index")) : false;
        }

        @Override
        public boolean onPolylineClick(Polyline polyline) {
            return false;
        }

        @Override
        public void zoomToSpan() {
            if (mapView != null) {
                try {
                    mapView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ConfirmCustomPoiOverlay.super.zoomToSpan();
                            if (mTrafficShowView.getPoiListBox().getVisibility() == View.VISIBLE) {
                                MapStatus ms = baiduMap.getMapStatus();
                                ms.targetScreen.set(ScreenUtil.getInstance().getWidthPixels() / 2, ScreenUtil.getInstance().getHeightPixels() / 4);
                                baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(ms));
                            }
                        }
                    }, 300);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void setPoiPosition() {
        if (aList.size() == 0 || poiOverlay == null)
            return;
        if (aList.size() == 1) {
            MapStatus.Builder builder = new MapStatus.Builder();
            builder.target(new LatLng(aList.get(0).getLatitude(), aList.get(0).getLongitude()))
                    .targetScreen(new Point(baiduMap.getMapStatus().targetScreen.x, baiduMap.getMapStatus().targetScreen.y / 4))
                    .zoom(17F);
            baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
        } else
            poiOverlay.zoomToSpan();
    }

    private void setBaiduAddressFromPoiInfo(BaiduAddress ad, PoiInfo poiInfo) {
        ad.setAddress(poiInfo.address);
        ad.setUid(poiInfo.uid);
        ad.setName(poiInfo.name);
        ad.setCity(poiInfo.city);
        ad.setCreated(new Timestamp(System.currentTimeMillis()));
        ad.setHasCaterDetails(poiInfo.hasCaterDetails ? 1 : 0);
        ad.setIsPano(poiInfo.isPano ? 1 : 0);
        ad.setLatitude(poiInfo.location.latitude);
        ad.setLongitude(poiInfo.location.longitude);
        ad.setPhoneNum(poiInfo.phoneNum);
        ad.setPostCode(poiInfo.postCode);
    }

    /**
     * 显示检索详情
     *
     * @param position：被选中的检索结果索引
     **/
    private void showPoiDetail(final int position) {
        mTrafficShowView.getPoiDetailPager().setCurrentItem(position);
        updatePoiDetialDotList(poiInfoList.size(), position);
        mTrafficShowView.getPoiListBox().setVisibility(View.GONE);
        mTrafficShowView.getPoiDetialBox().setVisibility(View.VISIBLE);
        /*poiDetailPager.post(new Runnable() {
            @Override
            public void run() {
                poiDetailPager.setCurrentItem(position);
            }
        });*/
    }

    private void updatePoiDetialDotList(int total, int selectNum) {
        int l = mTrafficShowView.getPoiDetialDotList().getChildCount();
        for (int i = 0; i < l; i++) {
            if (i == selectNum) {
                ((ImageButton) mTrafficShowView.getPoiDetialDotList().getChildAt(i)).setImageResource(R.drawable.bnav_poi_detail_ic_tag_select);
            } else {
                ((ImageButton) mTrafficShowView.getPoiDetialDotList().getChildAt(i)).setImageResource(R.drawable.bnav_poi_detail_ic_tag_normal);
            }
            if (i >= total) {
                mTrafficShowView.getPoiDetialDotList().getChildAt(i).setVisibility(View.GONE);
            } else {
                mTrafficShowView.getPoiDetialDotList().getChildAt(i).setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 根据检索结果定位
     **/
    private void locationPoi(PoiInfo pi) {
        baiduMap.setMapStatus(MapStatusUpdateFactory.newLatLngZoom(pi.location, 17F));
    }

    /**
     * 定位更新观察者
     **/
    private LocateListener locateListener = new LocateListener(LocateListener.CoorType.BD09LL) {
        @Override
        public void update(Address addr) {
            Log.i(TAG, "onReceiveLocation>>");
            if (addr == null)
                return;
            // BDLocation location = new BDLocation();
            // location.setLatitude(addr.getLatitude());
            // location.setLongitude(addr.getLongitude());
            // location = LocationClient.getBDLocationInCoorType(location, BDLocation.BDLOCATION_GCJ02_TO_BD09LL);
            LatLng now = new LatLng(addr.getLatitude(), addr.getLongitude());
            // location.setLatitude(address.getLatitude());
            // location.setLongitude(address.getLongitude());
            // location = LocationClient.getBDLocationInCoorType(location, BDLocation.BDLOCATION_GCJ02_TO_BD09LL);
            LatLng before = new LatLng(address.getLatitude(), address.getLongitude());

            address = addr;
            // ((TextView) mContext.findViewById(R.id.ast_location_text)).setText(addr.getAddressDetail());
            if (DistanceUtil.getDistance(before, now) > 2000) {
                updateHomeAndCompany();
                if (goHomeNodes.size() > 0)
                    setGoHomeCalculate();
                else
                    setGoCompanyCalculate();
            }
        }

    };


    /**
     * 更新家和公司文本信息
     **/
    private void updateHomeAndCompany() {
        Log.i(TAG, "updateHomeAndCompany()>>");
        BDLocation bl = new BDLocation();
        goHomeNodes.clear();
        if (homeAddress != null) {
            bl.setLatitude(address.getLatitude());
            bl.setLongitude(address.getLongitude());
            bl.setAddrStr(address.getAddressDetail());
            /* 根据设定的转换坐标系类型得到相应坐标系下的BDLocation */
            // bl = LocationClient.getBDLocationInCoorType(bl, BDLocation.BDLOCATION_GCJ02_TO_BD09LL);
            /* 计算俩点之间的距离 */
            double dt = DistanceUtil.getDistance(new LatLng(bl.getLatitude(), bl.getLongitude()), new LatLng(homeAddress.getLatitude(), homeAddress.getLongitude()));
            if (dt < 1000) {
                //  mTrafficShowView.getGoHomeText().setText("回家：已在家附近");
            } else {
                /* 创建导航节点坐标对象 */
                BNRoutePlanNode start = new BNRoutePlanNode(address.getLongitude(), address.getLatitude(), address.getAddressDetail(), "");
                bl.setLatitude(homeAddress.getLatitude());
                bl.setLongitude(homeAddress.getLongitude());
                bl = LocationClient.getBDLocationInCoorType(bl, BDLocation.BDLOCATION_BD09LL_TO_GCJ02);
                BNRoutePlanNode end = new BNRoutePlanNode(bl.getLongitude(), bl.getLatitude(), homeAddress.getName(), "");
                goHomeNodes.add(start);
                goHomeNodes.add(end);
            }
        } else {
            //  mTrafficShowView.getGoHomeText().setVisibility(View.GONE);
        }

        goCompanyNodes.clear();
        if (companyAddress != null) {
            bl.setLatitude(address.getLatitude());
            bl.setLongitude(address.getLongitude());
            // bl = LocationClient.getBDLocationInCoorType(bl, BDLocation.BDLOCATION_GCJ02_TO_BD09LL);
            double dt = DistanceUtil.getDistance(new LatLng(bl.getLatitude(), bl.getLongitude()), new LatLng(companyAddress.getLatitude(), companyAddress.getLongitude()));

            if (dt < 1000) {
                //  mTrafficShowView.getGoCompanyText().setText("去公司：已在单位附近");
            } else {
                BNRoutePlanNode start = new BNRoutePlanNode(address.getLongitude(), address.getLatitude(), address.getAddressDetail(), "");
                bl.setLatitude(companyAddress.getLatitude());
                bl.setLongitude(companyAddress.getLongitude());
                bl = LocationClient.getBDLocationInCoorType(bl, BDLocation.BDLOCATION_BD09LL_TO_GCJ02);
                BNRoutePlanNode end = new BNRoutePlanNode(bl.getLongitude(), bl.getLatitude(), companyAddress.getName(), "");
                goCompanyNodes.add(start);
                goCompanyNodes.add(end);
            }
        } else {
            //    mTrafficShowView.getGoCompanyText().setVisibility(View.GONE);
        }
        /* 设置当前所定位的位置文本 */
        //    ((TextView) mContext.findViewById(R.id.ast_location_text)).setText(address.getAddressDetail());
    }

    /**
     * 计算回家路线时间
     **/
    private void setGoHomeCalculate() {
        Log.i(TAG, "setGoHomeCalculate()>>");
        if (goHomeNodes.size() > 0) {
            BNRoutePlanerProxy.getInstance().setObserver(observer);
            BNRoutePlanerProxy.getInstance().setCalcPrference(preference);
            calculateType = 0;
            BNRoutePlanerProxy.getInstance().setPointsToCalcRoute(goHomeNodes, 1, false, null, 0);
        }
    }

    private void resetScaleToolPosition() {
        View v = mContext.findViewById(R.id.ast_map_locate_bt);
        mapView.setScaleControlPosition(new Point((int) (v.getX() + v.getWidth() + 20), (int) v.getY() + v.getHeight() / 2));
    }

    /**
     * 计算去公司路线时间
     **/
    private void setGoCompanyCalculate() {
        Log.i(TAG, "setGoCompanyCalculate()>>");
        if (goCompanyNodes.size() > 0) {
            BNRoutePlanerProxy.getInstance().setObserver(observer);
            BNRoutePlanerProxy.getInstance().setCalcPrference(preference);
            calculateType = 1;
            BNRoutePlanerProxy.getInstance().setPointsToCalcRoute(goCompanyNodes, 1, false, null, 0);
        }

    }


    BNRoutePlanObserverProxy observer = new BNRoutePlanObserverProxy() {
        @Override
        public void update(Object bnSubject, int type, int event, Object o) {
            if (type == 1) {
                System.out.println("event is" + event);
                if (event == 2) {
                    Log.e(TAG, "百度导航路况展示算路成功");
                    RoutePlanModelProxy mRoutePlanModel = RoutePlanModelProxy.getCacheRoutePlanModelProxy("RoutePlanModel");
                    //                    Log.i(TAG, "算路方式：" + preference + ",距离：" + mRoutePlanModel.getDistance() + ",routeCnt=" + BNRoutePlaner.getInstance().getRouteCnt() + ",currentMsrl=" + mRoutePlanModel.getMultiRouteCurrentMSRL() + "," + mRoutePlanModel.getNodeNum() + "," + BNRoutePlaner.getInstance().getRemainedDestList().size());
                    //                    Log.i(TAG, "第一段路名:" + mRoutePlanModel.getFirstRoadName() + ",时间：" + mRoutePlanModel.getTotalTime() + ",花费：" + mRoutePlanModel.getTollFees() + ",油费：" + mRoutePlanModel.getGasMoney() + ",主要道路：" + mRoutePlanModel.getMainRoads());
                    //                    ArrayList<RoutePlanResultItem> rList = mRoutePlanModel.getRouteNodeData();
                    //                    for (RoutePlanResultItem ri : rList) {
                    //                        System.out.println(ri.getNextRoadName() + ",condition=" + ri.getRoadCondition() + ",night=" + ri.getNodeDescNight() + ",////////" + ri.getNodeDesc());
                    //                    }
                    //                    System.out.println("**********************************");
                    //                    ArrayList<RoutePlanOutlineItem> oList = mRoutePlanModel.getRouteOutlineData();
                    //                    for (RoutePlanOutlineItem oi : oList) {
                    //                        System.out.println("routeId=" + oi.getRoutId() + ",totalRoadCondition=" + oi.getTotalRoadCondition() + ",length=" + oi.getLength() + ",passTime=" + oi.getPassTime()
                    //                                + ",LengthStr=" + oi.getLengthStr() + ",mainRoads=" + oi.getMainroads() + ",passTimeStr=" + oi.getPassTimeStr()
                    //                                + ",strTotalRoadCondition=" + oi.getStrTotalRoadCondition() + ",toll=" + oi.getToll());
                    //                    }

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
                    if (calculateType == 0) {
                        //    mTrafficShowView.getGoHomeText().setText("回家：" + mRoutePlanModel.getDistance() + "," + mRoutePlanModel.getTotalTime());
                        if (goCompanyNodes.size() > 0) {
                            setGoCompanyCalculate();
                        } else {
                            hanler.sendEmptyMessage(MSG_SHOW_GO_WHERE);
                        }
                    } else if (calculateType == 1) {
                        //    mTrafficShowView.getGoCompanyText().setText("去单位：" + mRoutePlanModel.getDistance() + "," + mRoutePlanModel.getTotalTime());
                        hanler.sendEmptyMessage(MSG_SHOW_GO_WHERE);
                    }
                } else if (event == 3 || event == 6 || event == 18 || event == 19) {
                    Log.e(TAG, "百度导航路况展示算路失败");
                }
            }
        }
    };

    private Handler hanler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_SHOW_GO_WHERE) {
                // mTrafficShowView.getGoWhereBox().setVisibility(View.VISIBLE);
            }
        }
    };

    @Override
    public void destroy() {
        Log.i(TAG, "onDestroy");
        mapView.onDestroy();
        if (BNRoutePlanerProxy.getInstance().isCalculatingRoute()) {
            Log.i(TAG, "isCalculatingRoute");
            BNRoutePlanerProxy.getInstance().cancleCalcRouteRequest();
        }
        BaiduLocateManager.get().deleteObserver(locateListener);
        EventBus.getDefault().unregister(this);
        mContext = null;
    }
}
