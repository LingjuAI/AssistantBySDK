package com.lingju.assistant.activity;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.LingjuSwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import com.baidu.mapapi.utils.DistanceUtil;
import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.activity.event.NaviShowPointsEvent;
import com.lingju.assistant.activity.event.OnItemClickListener;
import com.lingju.assistant.service.AssistantService;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.assistant.view.DrawForExpandLayout;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.lbsmodule.location.Address;
import com.lingju.model.BaiduAddress;
import com.lingju.model.dao.BaiduNaviDao;
import com.lingju.common.log.Log;
import com.lingju.util.NetUtil;
import com.lingju.util.ScreenUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2016/12/22.
 */
public class NaviConfirmPointActivity extends GoBackActivity implements LingjuSwipeRefreshLayout.OnRefreshListener, OnGetPoiSearchResultListener {

    /**
     * 一次poi搜索返回结果数量
     **/
    private final static int PER_PAGE = 10;
    private static final String TAG = "ConfirmPoint";

    @BindView(R.id.ancp_map)
    MapView mAncpMap;
    @BindView(R.id.ancp_map_its_bt)
    ImageButton mAncpMapItsBt;
    @BindView(R.id.ancp_poi_list)
    RecyclerView mAncpPoiList;
    @BindView(R.id.ancp_poi_list_box)
    LingjuSwipeRefreshLayout mAncpPoiListBox;
    @BindView(R.id.ancp_poi_box)
    DrawForExpandLayout mAncpPoiBox;
    @BindView(R.id.ancp_poi_detail_pager)
    ViewPager mAncpPoiDetailPager;
    @BindView(R.id.ancp_poi_detial_dot_list)
    LinearLayout mAncpPoiDetialDotList;
    @BindView(R.id.ancp_map_locate_bt)
    ImageButton mAncpMapLocateBt;
    @BindView(R.id.ancp_map_box)
    FrameLayout mAncpMapBox;
    @BindView(R.id.ancp_poi_detail_box)
    LinearLayout mAncpPoiDetailBox;
    @BindView(R.id.status_bar)
    View mStatusBar;

    private AppConfig mAppConfig;
    private BaiduNaviDao mNaviDao;
    private LayoutInflater mInflater;
    private BaiduMap baiduMap;
    private NaviPointAdapter mListAdapter;
    private NaviPointPagerAdapter mPagerAdapter;
    private PoiSearch pSearch;
    private Address address;
    // private Address g2c_address;
    private String keyword;     //搜索关键词（搜索地址）
    private boolean addMode;    //结果地址列表右侧文本显示标记（true “添加”  false “出发”）
    private boolean firstPoiSearch;     //是否第一次poi搜索标记
    private int poiPageCode = 0; //当前页码
    private int totalPageNum;   //Poi搜索结果页数
    private boolean voiceMode;  //是否语音对话模式标记
    private List<PoiInfo> list = new ArrayList<>();   //Poi检索信息集合
    private List<BaiduAddress> aList = new ArrayList<>();     //检索地址集合
    private OverlayManager poiOverlay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navi_confirm_point);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        initConfig();
        initView();
        initData();
    }

    /**
     * 初始化相关数据
     **/
    private void initData() {
        /* 获取poi搜索对象 */
        pSearch = PoiSearch.newInstance();
        pSearch.setOnGetPoiSearchResultListener(this);
        address = mAppConfig.address;
        /* 将坐标系转换成百度坐标系 */
        // g2c_address = address.clone().setBD09LL();
        Intent intent = getIntent();
        if (intent != null) {
            keyword = intent.getStringExtra("data");
            ArrayList<BaiduAddress> addresses = intent.getParcelableArrayListExtra("addresses");
            addMode = intent.getBooleanExtra("add", false);
            if (!TextUtils.isEmpty(keyword)) {
                firstPoiSearch = true;
                /* 在城市内检索，检索结果在poi检索结果回调中显示 */
                pSearch.searchInCity(new PoiCitySearchOption().city(mAppConfig.selectedCityInSearchPoi == null ?
                        address.getCity() : mAppConfig.selectedCityInSearchPoi).keyword(keyword).pageCapacity(PER_PAGE).pageNum(poiPageCode));
            } else if (addresses != null) {
                voiceMode = true;
                /* 设置poi搜索点图层 */
                poiOverlay = new ConfirmCustomPoiOverlay(baiduMap);
                baiduMap.setOnMarkerClickListener(poiOverlay);
                if (poiOverlay instanceof ConfirmCustomPoiOverlay) {
                    ((ConfirmCustomPoiOverlay) poiOverlay).setOverlayOptions(addresses);
                }
                //添加PoiOverlay到地图中
                poiOverlay.addToMap();

                aList.clear();
                aList.addAll(addresses);
                setListReverse();
                mListAdapter.notifyDataSetChanged();
                mPagerAdapter.notifyDataSetChanged();
                firstPoiSearch = false;
                mAncpPoiBox.setVisibility(View.VISIBLE);
                mAncpPoiListBox.setAllowDrag(false);
                setPoiPosition();
            }
        }
    }

    /**
     * 初始化视图
     **/
    private void initView() {
        //设置模拟状态栏的高度
        ViewGroup.LayoutParams layoutParams = mStatusBar.getLayoutParams();
        layoutParams.height = ScreenUtil.getStatusBarHeight(this);
        mStatusBar.setLayoutParams(layoutParams);

        mAncpPoiListBox.setColorSchemeResources(R.color.base_blue);
        mAncpPoiListBox.setOnRefreshListener(this);

        /* 设置比例尺控件、缩放控件是否显示 */
        mAncpMap.showScaleControl(true);
        mAncpMap.showZoomControls(false);
        baiduMap = mAncpMap.getMap();
        baiduMap.setMyLocationEnabled(true);

        /* 设置地址列表参数 */
        mListAdapter = new NaviPointAdapter();
        mAncpPoiList.hasFixedSize();
        mAncpPoiList.setAdapter(mListAdapter);
        mAncpPoiList.setLayoutManager(new LinearLayoutManager(this));
       // mAncpPoiList.addItemDecoration(new SimpleLineDivider(R.color.new_music_bg_color));
        mAncpPoiList.post(new Runnable() {
            @Override
            public void run() {
                mAncpPoiBox.setMaxHeight(mAncpMapBox.getMeasuredHeight() - dp2px(48));
                resetScaleToolPosition();
            }
        });
        mListAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onClick(View itemView, int position) {
                showPoiDetail(position);
            }

            @Override
            public void onLongClick(View intemView, int position) {

            }
        });

        /* 设置地址详情ViewPager参数 */
        mPagerAdapter = new NaviPointPagerAdapter();
        mAncpPoiDetailPager.setAdapter(mPagerAdapter);
        mAncpPoiDetailPager.addOnPageChangeListener(pageChangeListener);
        mAncpPoiBox.setScaleChangeListener(scaleChangedListener);
    }

    /**
     * 初始化参数变量
     **/
    private void initConfig() {
        mAppConfig = (AppConfig) getApplication();
        mNaviDao = BaiduNaviDao.getInstance();
        mInflater = LayoutInflater.from(this);
        ScreenUtil.getInstance().init(this);
    }

    @Override
    protected void onResume() {
        mAncpMap.onResume();
        super.onResume();
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        getWindow().setAttributes(params);
    }

    @Override
    protected void onPause() {
        mAncpMap.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mAncpMap.onDestroy();
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (voiceMode) {
            if (SynthesizerBase.isInited()) {
                SynthesizerBase.get().stopSpeakingAbsolte();
            }
            Intent intent = new Intent(this, AssistantService.class);
            intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_RECOGNIZE);
            startService(intent);
            intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PUSH_ROUTE_CACULATE);
            intent.putExtra("text", "取消");
            intent.putExtra(AssistantService.CALLBACK, false);
            startService(intent);
        }
        goBack();
    }

    @OnClick({R.id.ancp_back_bt, R.id.ancp_map_locate_bt, R.id.ancp_zoom_in_bt, R.id.ancp_zoom_out_bt,
            R.id.ancp_poi_detial_pre_bt, R.id.ancp_poi_detial_next_bt, R.id.ancp_map_its_bt})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ancp_back_bt:
                onBackPressed();
                break;
            case R.id.ancp_map_locate_bt:
                /* 封装定位信息 */
                BDLocation location = new BDLocation();
                location.setLatitude(address.getLatitude());
                location.setLongitude(address.getLongitude());
                location.setRadius(address.getRadius());
                location.setSpeed(address.getSpeed());
                location.setSatelliteNumber(address.getSatelliteNumber());
                location = LocationClient.getBDLocationInCoorType(location, BDLocation.BDLOCATION_GCJ02_TO_BD09LL);
                /* 开启定位图层 */
                baiduMap.setMyLocationEnabled(true);
                // 设置定位图层的配置（定位模式，是否允许方向信息，用户自定义定位图标）
                baiduMap.setMyLocationConfigeration(new MyLocationConfiguration(MyLocationConfiguration.LocationMode.FOLLOWING, true, null));
                /* 构造定位数据并设置 */
                baiduMap.setMyLocationData(new MyLocationData.Builder().latitude(location.getLatitude())
                        .longitude(location.getLongitude())
                        .accuracy(location.getRadius())
                        .direction(location.getDirection())
                        .satellitesNum(location.getSatelliteNumber())
                        .speed(location.getSpeed())
                        .build());
                break;
            case R.id.ancp_zoom_in_bt:
                baiduMap.setMapStatus(MapStatusUpdateFactory.zoomIn());
                break;
            case R.id.ancp_zoom_out_bt:
                baiduMap.setMapStatus(MapStatusUpdateFactory.zoomOut());
                break;
            case R.id.ancp_poi_detial_pre_bt:
                if (mAncpPoiDetailPager.getCurrentItem() > 0 && mAncpPoiDetailPager.getCurrentItem() < list.size()) {
                    showPoiDetail(mAncpPoiDetailPager.getCurrentItem() - 1);
                }
                break;
            case R.id.ancp_poi_detial_next_bt:
                if (mAncpPoiDetailPager.getCurrentItem() >= 0 && mAncpPoiDetailPager.getCurrentItem() < list.size() - 1) {
                    showPoiDetail(mAncpPoiDetailPager.getCurrentItem() + 1);
                }
                break;
            case R.id.ancp_map_its_bt:
                if (baiduMap.isTrafficEnabled()) {
                    baiduMap.setTrafficEnabled(false);
                    ((ImageButton) view).setImageResource(R.drawable.bnav_common_ic_map_its_off);
                } else {
                    baiduMap.setTrafficEnabled(true);
                    ((ImageButton) view).setImageResource(R.drawable.bnav_common_ic_map_its_on);
                }
                break;
        }
    }

    /**
     * 确定目的地事件处理
     **/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfirmPointEvent(NaviShowPointsEvent event) {
        voiceMode = true;
        addMode = event.getType() == NaviShowPointsEvent.ADD_POINT;
        switch (event.getType()) {
            case NaviShowPointsEvent.DESTINATION:
            case NaviShowPointsEvent.ADD_POINT:
                mAncpPoiListBox.setAllowDrag(false);
                if (event.getPoints() != null) {
                    aList.clear();
                    aList.addAll(event.getPoints());
                    setListReverse();
                    mListAdapter.notifyDataSetChanged();
                    mPagerAdapter.notifyDataSetChanged();
                    mAncpPoiBox.setVisibility(View.VISIBLE);

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
                break;
            case NaviShowPointsEvent.CLOSE_ACTIVITY:
                finish();
                goInto();
                break;
            case NaviShowPointsEvent.CANCEL_TASK:
                onBackPressed();
                break;
        }
    }

    /**
     * 下拉刷新
     **/
    @Override
    public void onDownPullRefresh() {
        if (poiPageCode == 0) {
            mAncpPoiListBox.setRefreshing(false);
            //Toast.makeText(this, "当前已是第一页了", Toast.LENGTH_LONG).show();
            Snackbar.make(mAncpPoiDetailBox,"当前已是第一页了", Snackbar.LENGTH_SHORT).show();
        } else
            pSearch.searchInCity(new PoiCitySearchOption().city(mAppConfig.selectedCityInSearchPoi == null ?
                    address.getCity() : mAppConfig.selectedCityInSearchPoi).keyword(keyword).pageCapacity(PER_PAGE).pageNum(--poiPageCode));
    }

    /**
     * 上拉刷新
     **/
    @Override
    public void onUpPullRefresh() {
        if (poiPageCode == (totalPageNum - 1)) {
            mAncpPoiListBox.setRefreshing(false);
            //Toast.makeText(this, "没有符合要求的地点了", Toast.LENGTH_LONG).show();
            Snackbar.make(mAncpPoiDetailBox,"没有符合要求的地点了", Snackbar.LENGTH_SHORT).show();
        } else
            pSearch.searchInCity(new PoiCitySearchOption().city(mAppConfig.selectedCityInSearchPoi == null ?
                    address.getCity() : mAppConfig.selectedCityInSearchPoi).keyword(keyword).pageCapacity(PER_PAGE).pageNum(++poiPageCode));

    }

    /**
     * 设置比例尺位置
     **/
    private void resetScaleToolPosition() {
        mAncpMap.setScaleControlPosition(new Point((int) (mAncpMapLocateBt.getX() + mAncpMapLocateBt.getWidth() + 20),
                (int) mAncpMapLocateBt.getY() + mAncpMapLocateBt.getHeight() / 2));
    }

    /**
     * dp转换成px
     **/
    private int dp2px(int dp) {
        return (int) (0.5f + getResources().getDisplayMetrics().density * (float) dp);
    }

    /**
     * 显示指定索引的Viewpager页面
     **/
    private void showPoiDetail(final int position) {
        mAncpPoiDetailPager.setCurrentItem(position);
        updatePoiDetialDotList(list.size(), position);
        mAncpPoiBox.setVisibility(View.GONE);
        mAncpPoiDetailBox.setVisibility(View.VISIBLE);
    }

    /**
     * 更新Viewpager小圆点下标样式
     **/
    private void updatePoiDetialDotList(int total, int selectNum) {
        int l = mAncpPoiDetialDotList.getChildCount();
        for (int i = 0; i < l; i++) {
            if (i == selectNum) {
                ((ImageButton) mAncpPoiDetialDotList.getChildAt(i)).setImageResource(R.drawable.bnav_poi_detail_ic_tag_select);
            } else {
                ((ImageButton) mAncpPoiDetialDotList.getChildAt(i)).setImageResource(R.drawable.bnav_poi_detail_ic_tag_normal);
            }
            if (i >= total) {
                mAncpPoiDetialDotList.getChildAt(i).setVisibility(View.GONE);
            } else {
                mAncpPoiDetialDotList.getChildAt(i).setVisibility(View.VISIBLE);
            }
        }
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
     * 将所有BaiduAddress对象信息填充到poi搜索信息集合中
     **/
    public void setListReverse() {
        int al = aList.size();
        list.clear();
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
            list.add(poiInfo);
        }
    }

    /**
     * 设置检索结果显示索引（若有多个结果则地图缩放到保证所有结果可见）
     **/
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

    /**
     * 重置搜索地址集合
     *
     * @param list
     **/
    private void setAlist(List<PoiInfo> list) {
        aList.clear();
        for (PoiInfo poiInfo : list) {
            BaiduAddress address = new BaiduAddress();
            setBaiduAddressFromPoiInfo(address, poiInfo);
            aList.add(address);
        }
    }


    /**
     * viewPager页面改变监听器
     **/
    private ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            updatePoiDetialDotList(list.size(), position);
            baiduMap.setMapStatus(MapStatusUpdateFactory.newLatLngZoom(list.get(position).location, 17F));
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    private DrawForExpandLayout.ScaleChangedListener scaleChangedListener = new DrawForExpandLayout.ScaleChangedListener() {

        /** 视图拖动到最大高度时调用 **/
        @Override
        public void max() {

        }

        /** 视图拖动到最新高度时调用 **/
        @Override
        public void min() {
            if (list.size() == 0)
                return;
            int p = ((LinearLayoutManager) mAncpPoiList.getLayoutManager()).findFirstVisibleItemPosition();
            p = Math.max(p, 0);
            showPoiDetail(p);
        }
    };

    /**
     * poi搜索结果回调
     **/
    @Override
    public void onGetPoiResult(PoiResult result) {
        mAncpPoiListBox.setRefreshing(false);
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            //Toast.makeText(this, "搜索发生错误：" + result.error, Toast.LENGTH_LONG).show();
            Snackbar.make(mAncpPoiDetailBox, NaviConfirmPointActivity.this.getResources().getString(R.string.navi_no_result), Snackbar.LENGTH_SHORT).show();

            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            //创建PoiOverlay，添加搜索结果地点图层
            if (poiOverlay == null) {
                poiOverlay = new ConfirmPoiOverlay(baiduMap);
                baiduMap.setOnMarkerClickListener(poiOverlay);
            } else {
                poiOverlay.removeFromMap();
            }
            if (poiOverlay instanceof PoiOverlay) {
                ((PoiOverlay) poiOverlay).setData(result);
            }
            poiOverlay.addToMap();

            totalPageNum = result.getTotalPageNum();
            mAncpPoiBox.getLayoutParams().height = ScreenUtil.getInstance().getHeightPixels() / 2;
            list.clear();
            /* 填充数据 */
            list.addAll(result.getAllPoi());
            setAlist(list);
            /* 刷新视图 */
            mListAdapter.notifyDataSetChanged();
            mPagerAdapter.notifyDataSetChanged();
            if (firstPoiSearch) {
                firstPoiSearch = false;
                mAncpPoiBox.setVisibility(View.VISIBLE);
            }
            setPoiPosition();
        }
    }

    @Override
    public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

    }

    @Override
    public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

    }

    /**
     * 搜索结果地址列表适配器
     **/
    class NaviPointAdapter extends RecyclerView.Adapter<NaviPointAdapter.NaviPointHolder> {


        @Override
        public NaviPointHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = mInflater.inflate(R.layout.target_poi_item, parent, false);
            return new NaviPointHolder(itemView);
        }

        @Override
        public void onBindViewHolder(NaviPointHolder holder, int position) {
            PoiInfo poiInfo = list.get(position);
            holder.mTpiNameText.setText((position + 1) + "." + poiInfo.name);
            holder.mTpiAddressText.setText(poiInfo.address);
            if (addMode) {
                holder.mTpiItemConfirmText.setText("添加");
                holder.mTpiDistanceText.setVisibility(View.GONE);
            } else {
                holder.mTpiItemConfirmText.setText("出发");
                holder.mTpiDistanceText.setVisibility(View.VISIBLE);
                double distance = DistanceUtil.getDistance(new LatLng(address.getLatitude(), address.getLongitude()),
                        poiInfo.location) / 1000;
                holder.mTpiDistanceText.setText(String.format("%.1f", distance) + "km");
            }
            holder.mTpiTargetConfirmBt.setTag(position);
            holder.itemView.setTag(position);
        }

        @Override
        public int getItemCount() {
            return list.size();
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
                            PoiInfo poiInfo = list.get(position);
                            intoNaviSetLine(position, poiInfo);
                        }
                    }
                });
            }
        }
    }

    /**
     * ViewPager适配器
     **/
    class NaviPointPagerAdapter extends PagerAdapter {

        private Map<Integer, View> chilren = new Hashtable<>();

        @Override
        public int getCount() {
            return aList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View itemView = chilren.get(position);
            BaiduAddress address = aList.get(position);
            PagerHolder holder = null;
            /* 加载视图 */
            if (itemView == null) {
                itemView = mInflater.inflate(R.layout.target_poi_item_detail, null);
                chilren.put(position, itemView);
            }
            /* 绑定数据 */
            holder = new PagerHolder(itemView);
            holder.mTpidFavoriteBt.setTag(position);
            holder.mTpidSetTargetBt.setTag(position);
            holder.mTpidTargetConfirmBt.setTag(position);
            holder.mTpidNameText.setText((position + 1) + "." + address.getName());
            holder.mTpidAddressText.setText(address.getAddress());
            Double distance = DistanceUtil.getDistance(new LatLng(address.getLatitude(), address.getLongitude()),
                    new LatLng(address.getLatitude(), address.getLongitude())) / 1000;
            holder.mTpidDistanceText.setText(String.format("%.1f", distance) + "km");
            if (address.getFavoritedTime() != null) {
                holder.mTpidFavoriteBt.setText("已收藏");
                holder.mTpidFavoriteBt.getBackground().setLevel(1);
            }

            container.addView(itemView);
            return chilren.get(position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (chilren.get(position) != null) {
                container.removeView(chilren.remove(position));
            }
        }

        class PagerHolder {
            @BindView(R.id.tpid_name_text)
            TextView mTpidNameText;
            @BindView(R.id.tpid_address_text)
            TextView mTpidAddressText;
            @BindView(R.id.tpid_distance_text)
            TextView mTpidDistanceText;
            @BindView(R.id.tpid_target_confirm_bt)
            LinearLayout mTpidTargetConfirmBt;
            @BindView(R.id.tpid_set_target_bt)
            TextView mTpidSetTargetBt;
            @BindView(R.id.tpid_favorite_bt)
            TextView mTpidFavoriteBt;

            public PagerHolder(View itemView) {
                ButterKnife.bind(this, itemView);
                mTpidFavoriteBt.setOnClickListener(clickListener);
                mTpidSetTargetBt.setOnClickListener(clickListener);
                mTpidTargetConfirmBt.setOnClickListener(clickListener);
            }
        }

        private View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = (int) v.getTag();
                switch (v.getId()) {
                    case R.id.tpid_target_confirm_bt:
                    case R.id.tpid_set_target_bt:
                        intoNaviSetLine(index, list.get(index));
                        break;
                    case R.id.tpid_favorite_bt:
                        TextView favor = (TextView) v;
                        BaiduAddress baiduAddress = aList.get(index);
                        if (baiduAddress.getFavoritedTime() != null) {
                            baiduAddress.setFavoritedTime(null);
                            favor.setText("未收藏");
                            favor.getBackground().setLevel(0);
                        } else {
                            baiduAddress.setFavoritedTime(new Date());
                            favor.setText("已收藏");
                            favor.getBackground().setLevel(1);
                        }
                        baiduAddress.setSynced(false);
                        mNaviDao.insertAddress(baiduAddress);
                        //同步收藏地址数据
                        mNaviDao.sync();
                        break;
                }
            }
        };
    }

    /**
     * 进入设置导航路线页面
     **/
    private void intoNaviSetLine(int index, PoiInfo info) {
        if (voiceMode) {
            if (SynthesizerBase.isInited()) {
                SynthesizerBase.get().stopSpeakingAbsolte();
            }
            Intent is = new Intent(NaviConfirmPointActivity.this, AssistantService.class);
            is.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_RECOGNIZE);
            startService(is);
            is.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PUSH_ROUTE_CACULATE);
            is.putExtra("text", "第" + (index + 1) + "个");
            is.putExtra(AssistantService.CALLBACK, true);
            startService(is);
        } else {
            if(NetUtil.getInstance(NaviConfirmPointActivity.this).getCurrentNetType().equals(NetUtil.NetType.NETWORK_TYPE_NONE)){
                final CommonDialog commonDialog =  new CommonDialog(NaviConfirmPointActivity.this,"网络错误","网络状态不佳，请检查网络设置","确定");
                commonDialog.setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                    @Override
                    public void onConfirm() {
                        commonDialog.cancel();
                    }
                }).show();
                return;
            }
            Intent intent = new Intent(NaviConfirmPointActivity.this, NaviSetLineActivity.class);
            intent.putExtra("latitude", info.location.latitude);
            intent.putExtra("longitude", info.location.longitude);
            intent.putExtra("address", info.name);
            startActivity(intent);
            goInto();
        }
        finish();
    }

    class ConfirmCustomPoiOverlay extends OverlayManager {
        List<OverlayOptions> overlayOptionses;

        public ConfirmCustomPoiOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        /**
         * 设置图层参数
         **/
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
            showPoiDetail(i);
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
            if (mAncpMap != null) {
                try {
                    mAncpMap.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ConfirmCustomPoiOverlay.super.zoomToSpan();
                            if (mAncpPoiBox.getVisibility() == View.VISIBLE) {
                                MapStatus ms = baiduMap.getMapStatus();
                                ms.targetScreen.set(ScreenUtil.getInstance().getWidthPixels() / 2, ScreenUtil.getInstance().getHeightPixels() / 4);
                                baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(ms));
                            }
                        }
                    }, 500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class ConfirmPoiOverlay extends PoiOverlay {

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

        /**
         * 缩放地图，使所有Overlay都在合适的视野内
         **/
        @Override
        public void zoomToSpan() {
            if (mAncpMap != null) {
                try {
                    mAncpMap.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ConfirmPoiOverlay.super.zoomToSpan();
                            if (mAncpPoiBox.getVisibility() == View.VISIBLE) {
                                MapStatus ms = baiduMap.getMapStatus();
                                /* 设置地图操作中心点在屏幕中的坐标 */
                                ms.targetScreen.set(ScreenUtil.getInstance().getWidthPixels() / 2, ScreenUtil.getInstance().getHeightPixels() / 4);
                                /* 以动画的方式更新地图状态，耗时300ms */
                                baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(ms));
                            }
                        }
                    }, 500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
