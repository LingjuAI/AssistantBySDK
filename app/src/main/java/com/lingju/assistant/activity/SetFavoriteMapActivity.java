package com.lingju.assistant.activity;

import android.content.Intent;
import android.graphics.Point;
import android.graphics.drawable.LevelListDrawable;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.baidu.location.BDLocation;
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
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
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
import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.activity.event.NaviShowPointsEvent;
import com.lingju.assistant.activity.event.OnItemClickListener;
import com.lingju.assistant.activity.index.model.FullyLinearLayoutManager;
import com.lingju.assistant.activity.index.presenter.NaviSetPointPresenter;
import com.lingju.assistant.view.AdaptHeightListView;
import com.lingju.assistant.view.CommonDeleteDialog;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.assistant.view.CommonEditDialog;
import com.lingju.assistant.view.DrawForExpandLayout;
import com.lingju.assistant.view.LocationSearchSuggestListView;
import com.lingju.assistant.view.RealTimeUpdateSearchBox;
import com.lingju.assistant.view.SelectCityDialog;
import com.lingju.common.log.Log;
import com.lingju.lbsmodule.location.Address;
import com.lingju.lbsmodule.location.BaiduLocateManager;
import com.lingju.model.BaiduAddress;
import com.lingju.model.dao.BaiduNaviDao;
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
public class SetFavoriteMapActivity extends GoBackActivity implements OnGetPoiSearchResultListener, OnGetSuggestionResultListener, LingjuSwipeRefreshLayout.OnRefreshListener {

    public final static String MODE = "mode";
    public final static String FAVORITE_TO_SET_MODE = "favoriteToSetMode";
    public final static int SET_FAVORITE = 0;
    public final static int SET_HOME = 1;
    public final static int SET_COMPANY = 2;
    public final static int SET_START = 3;
    public final static int SET_END = 4;
    public final static int FAVORITE_SET_HOME = 5;
    public final static int FAVORITE_SET_COMPANY = 6;
    public final static int RESULT_REMARK = 1;
    private final static int PER_PAGE = 20;
    private static final String TAG = "LingJu";

    @BindView(R.id.amosf_search_bt)
    TextView mAmosfSearchBt;
    @BindView(R.id.amosf_search_edit)
    RealTimeUpdateSearchBox mAmosfSearchEdit;
    @BindView(R.id.amosf_map)
    MapView mAmosfMap;
    @BindView(R.id.amosf_map_box)
    FrameLayout mAmosfMapBox;
    @BindView(R.id.amosf_poi_list)
    RecyclerView mAmosfPoiList;
    @BindView(R.id.amosf_poi_list_box)
    LingjuSwipeRefreshLayout mAmosfPoiListBox;
    @BindView(R.id.amosf_poi_box)
    DrawForExpandLayout mAmosfPoiBox;
    @BindView(R.id.amosf_poi_detail_pager)
    ViewPager mAmosfPoiDetailPager;
    @BindView(R.id.amosf_poi_detial_dot_list)
    LinearLayout mAmosfPoiDetialDotList;
    @BindView(R.id.amosf_poi_detail_box)
    LinearLayout mAmosfPoiDetailBox;
    @BindView(R.id.tpid_name_text)
    TextView mTpidNameText;
    @BindView(R.id.tpid_address_text)
    TextView mTpidAddressText;
    @BindView(R.id.tpid_tips_text)
    TextView mTpidTipsText;
    @BindView(R.id.tpid_target_confirm_text)
    TextView mTpidTargetConfirmText;
    @BindView(R.id.amosf_single_poi_detail_box)
    LinearLayout mAmosfSinglePoiDetailBox;
    @BindView(R.id.amosf_suggest_list)
    LocationSearchSuggestListView mAmosfSuggestList;
    @BindView(R.id.amosf_suggest_list_box)
    ScrollView mAmosfSuggestListBox;
    @BindView(R.id.tpid_target_confirm_bt)
    LinearLayout mTpidTargetConfirmBt;
    @BindView(R.id.navi_favorite_and_history)
    RecyclerView mNaviFavoriteAndHistory;
    @BindView(R.id.status_bar)
    View mStatusBar;

    private BaiduNaviDao mNaviDao;
    private AppConfig mAppConfig;
    private BaiduMap baiduMap;
    private PoiSearch pSearch;
    private SuggestionSearch sSearch;
    private LayoutInflater mInflater;
    private List<PoiInfo> list = new ArrayList<>();   //Poi检索信息集合
    private List<BaiduAddress> aList = new ArrayList<>();     //检索地址集合
    private FavorPointAdapter mListAdapter;
    private FavorPointPagerAdapter mPagerAdapter;

    private int mode = -1;      //地址类型：0：常用地址 1：家  2：单位 3：出发地 4：目的地
    private int favoriteToSetmode;      //地址类型：0：常用地址 1：家  2：单位 3：出发地 4：目的地
    private int updateCode;
    private BaiduAddress receiveAddress;
    private Address mAddress;
    private GeoCoder geoCoder;
    private BaiduAddress remarkAddress;
    private String keyword;     //地址搜索关键词
    private int poiPageCode = 0;    //地址搜索页面
    private SelectCityDialog selectCityDialog;
    private OverlayManager poiOverlay;
    private int totalPageNum;
    private boolean updateHistory = false;
    LevelListDrawable ld;
    private FavoriteAndHistoryAdapter mFavAndHisAdapter;
    private final List<BaiduAddress> favoriteList = new ArrayList<>();
    private final List<BaiduAddress> historyList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_of_set_favorite);
        ButterKnife.bind(this);
        mNaviDao = BaiduNaviDao.getInstance();
        mAppConfig = (AppConfig) getApplication();
        ScreenUtil.getInstance().init(this);
        mInflater = LayoutInflater.from(this);
        EventBus.getDefault().register(this);
        //设置模拟状态栏的高度
        ViewGroup.LayoutParams layoutParams = mStatusBar.getLayoutParams();
        layoutParams.height = ScreenUtil.getStatusBarHeight(this);
        mStatusBar.setLayoutParams(layoutParams);

        /* 更新定位 */
        BaiduLocateManager.get().start();
        /* 设置百度地图图层 */
        mAmosfMap.showScaleControl(true);
        mAmosfMap.showZoomControls(false);
        baiduMap = mAmosfMap.getMap();
        baiduMap.setMyLocationEnabled(true);
        /* 初始化百度地址搜索 */
        pSearch = PoiSearch.newInstance();
        pSearch.setOnGetPoiSearchResultListener(this);
        sSearch = SuggestionSearch.newInstance();
        sSearch.setOnGetSuggestionResultListener(this);

        /* 初始化视图 */
        // mAmosfSearchBt.setVisibility(View.GONE);
        mAmosfPoiListBox.setColorSchemeResources(R.color.base_blue);
        mListAdapter = new FavorPointAdapter();
        mAmosfPoiList.setHasFixedSize(true);
        mAmosfPoiList.setLayoutManager(new LinearLayoutManager(this));
        //mAmosfPoiList.addItemDecoration(new SimpleLineDivider(R.color.new_music_bg_color));
        mAmosfPoiList.setAdapter(mListAdapter);
        mAmosfPoiList.post(new Runnable() {
            @Override
            public void run() {
                mAmosfPoiBox.setMaxHeight(findViewById(R.id.amosf_map_box).getMeasuredHeight() - (int) (getResources().getDisplayMetrics().density * 48.0f + 0.5f));
                resetScaleToolPosition();
            }
        });

        /* 设置相关监听器 */
        mAmosfSearchEdit.setSearchListener(searchListener);
        mAmosfSuggestList.setItemClickListener(suggestItemClickListener);
        mAmosfPoiListBox.setOnRefreshListener(this);
        mListAdapter.setOnItemClickListener(listItemClickListener);

        /* 设置地址详情ViewPager参数 */
        mPagerAdapter = new FavorPointPagerAdapter();
        mAmosfPoiDetailPager.setAdapter(mPagerAdapter);
        mAmosfPoiBox.setScaleChangeListener(scaleChangedListener);
        mAmosfPoiDetailPager.addOnPageChangeListener(pageChangeListener);

        mFavAndHisAdapter = new FavoriteAndHistoryAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mNaviFavoriteAndHistory.setLayoutManager(layoutManager);
        mNaviFavoriteAndHistory.setAdapter(mFavAndHisAdapter);
        mAddress = mAppConfig.address;
        if (getIntent() != null) {
            Intent intent = getIntent();
            mode = intent.getIntExtra(MODE, -1);
            favoriteToSetmode = intent.getIntExtra(FAVORITE_TO_SET_MODE, -1);
            receiveAddress = intent.getParcelableExtra("address");
            setInputBoxHint(intent);
        }

        if (receiveAddress != null) {
            receiveAddress.print();
            if (mode == -1) {
                if (receiveAddress.getRemark() != null) {
                    if (receiveAddress.getRemark().equals("家")) {
                        mode = SET_HOME;
                    } else if (receiveAddress.getRemark().equals("单位")) {
                        mode = SET_COMPANY;
                    } else if (receiveAddress.getRemark().equals("出发地")) {
                        mode = SET_START;
                    } else if (receiveAddress.getRemark().equals("目的地")) {
                        mode = SET_END;
                    }
                }
                mode = SET_FAVORITE;
            }
            setSinglePoiDetail();
            locationReceiveAddress();
        } else {
            if (mode == -1)
                mode = SET_FAVORITE;
            location();
        }
    }

    private void setInputBoxHint(Intent intent) {
        int setType = intent.getIntExtra(SetFavoriteMapActivity.MODE, 0);
        if (setType == SetFavoriteMapActivity.SET_START) {
            mAmosfSearchEdit.setEditHint(R.string.inputStart);
        } else if (setType == SetFavoriteMapActivity.SET_END) {
            mAmosfSearchEdit.setEditHint(R.string.inputEnd);
        } else if (setType == SetFavoriteMapActivity.SET_COMPANY) {
            mAmosfSearchEdit.setEditHint(R.string.inputGoCompany);
        } else if (setType == SetFavoriteMapActivity.SET_HOME) {
            mAmosfSearchEdit.setEditHint(R.string.inputGoHome);
        } else if (setType == SetFavoriteMapActivity.SET_FAVORITE) {
            mAmosfSearchEdit.setEditHint(R.string.inputFavorite);
        }

    }

    /**
     * 设置地图比例尺图标的位置
     **/
    private void resetScaleToolPosition() {
        View v = findViewById(R.id.amosf_map_locate_bt);
        mAmosfMap.setScaleControlPosition(new Point((int) (v.getX() + v.getWidth() + 20), (int) v.getY() + v.getHeight() / 2));
    }

    /**
     * 定位检索结果
     **/
    private void locationPoi(PoiInfo pi) {
        baiduMap.setMapStatus(MapStatusUpdateFactory.newLatLngZoom(pi.location, 17F));
    }

    /**
     * 定位接收到的地址
     **/
    private void locationReceiveAddress() {
        if (receiveAddress == null) {
            return;
        }
        LatLng p = new LatLng(receiveAddress.getLatitude(), receiveAddress.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        /* 设置覆盖物图标 */
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_openmap_focuse_mark))
                .position(p)
                .visible(true);
        baiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                setSinglePoiDetail();
                return true;
            }
        });
        if (NetUtil.getInstance(SetFavoriteMapActivity.this).getCurrentNetType().equals(NetUtil.NetType.NETWORK_TYPE_NONE)) {
            // Snackbar.make(mAmosfPoiList,mAppConfig.getResources().getString(R.string.no_network), Snackbar.LENGTH_SHORT).show();
            return;
        }
        /* 添加覆盖图层 */
        baiduMap.addOverlay(markerOptions);
        baiduMap.setMapStatus(MapStatusUpdateFactory.newLatLngZoom(p, 17F));
    }

    /**
     * 百度地图定位
     **/
    private void location() {
        BDLocation location = new BDLocation();
        location.setLatitude(mAddress.getLatitude());
        location.setLongitude(mAddress.getLongitude());
        location.setRadius(mAddress.getRadius());
        location.setSpeed(mAddress.getSpeed());
        location.setSatelliteNumber(mAddress.getSatelliteNumber());
        // location = LocationClient.getBDLocationInCoorType(location, BDLocation.BDLOCATION_GCJ02_TO_BD09LL);
        // 开启定位图层
        baiduMap.setMyLocationEnabled(true);
        // 设置定位图层的配置（定位模式，是否允许方向信息，用户自定义定位图标）
        baiduMap.setMyLocationConfigeration(new MyLocationConfiguration(MyLocationConfiguration.LocationMode.FOLLOWING, true, null));
        // 设置定位数据
        baiduMap.setMyLocationData(new MyLocationData.Builder().latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .accuracy(location.getRadius())
                .direction(location.getDirection())
                .satellitesNum(location.getSatelliteNumber())
                .speed(location.getSpeed())
                .build());
        if (receiveAddress == null) {
            BaiduAddress temp = new BaiduAddress();
            temp.setAddress(mAddress.getAddressDetail());
            temp.setLatitude(location.getLatitude());
            temp.setLongitude(location.getLongitude());
            BaiduAddress temp2 = mNaviDao.get(temp);
            if (temp2 != null && temp2 != temp) {
                receiveAddress = temp2;
            } else {
                geoCoder = GeoCoder.newInstance();
                /* 设置地理编码查询监听者 */
                geoCoder.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {

                    @Override
                    public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {

                    }

                    @Override
                    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult r) {
                        List<PoiInfo> ls = r.getPoiList();
                        if (ls != null && ls.size() > 0) {
                            receiveAddress = new BaiduAddress();
                            setBaiduAddressFromPoiInfo(receiveAddress, ls.get(0));
                            receiveAddress.setName(receiveAddress.getName() + "附近");
                            receiveAddress.setAddress(mAddress.getAddressDetail());
                            receiveAddress.setLatitude(baiduMap.getLocationData().latitude);
                            receiveAddress.setLongitude(baiduMap.getLocationData().longitude);
                            if (TextUtils.isEmpty(receiveAddress.getCity()))
                                receiveAddress.setCity(mAddress.getCity());
                            geoCoder.destroy();
                        }
                        setSinglePoiDetail();
                        locationReceiveAddress();
                    }
                });
                geoCoder.reverseGeoCode(new ReverseGeoCodeOption().
                        location(new LatLng(location.getLatitude(), location.getLongitude())));
                return;
            }
        }
        setSinglePoiDetail();
        locationReceiveAddress();
    }

    /**
     * 显示检索详情（存在多个结果，根据索引更新）
     **/
    private void showPoiDetail(final int position) {
        mAmosfPoiDetailPager.setCurrentItem(position);
        updatePoiDetialDotList(list.size(), position);
        mAmosfPoiBox.setVisibility(View.GONE);
        mAmosfSinglePoiDetailBox.setVisibility(View.GONE);
        mAmosfPoiDetailBox.setVisibility(View.VISIBLE);
    }

    /**
     * 更新Viewpager小圆点下标样式
     **/
    private void updatePoiDetialDotList(int total, int selectNum) {
        int l = mAmosfPoiDetialDotList.getChildCount();
        for (int i = 0; i < l; i++) {
            if (i == selectNum) {
                ((ImageButton) mAmosfPoiDetialDotList.getChildAt(i)).setImageResource(R.drawable.bnav_poi_detail_ic_tag_select);
            } else {
                ((ImageButton) mAmosfPoiDetialDotList.getChildAt(i)).setImageResource(R.drawable.bnav_poi_detail_ic_tag_normal);
            }
            if (i >= total) {
                mAmosfPoiDetialDotList.getChildAt(i).setVisibility(View.GONE);
            } else {
                mAmosfPoiDetialDotList.getChildAt(i).setVisibility(View.VISIBLE);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(NaviShowPointsEvent e) {
        Log.i(TAG, "onEventMainThread>>NaviShowPointsEvent");
        if (e.getType() != NaviShowPointsEvent.CLOSE_ACTIVITY) {
            mAmosfPoiListBox.setAllowDrag(false);
            if (e.getPoints() != null) {
                aList.clear();
                aList.addAll(e.getPoints());
                setListReverse();
                mListAdapter.notifyDataSetChanged();
                mPagerAdapter.notifyDataSetChanged();
                mAmosfPoiBox.setVisibility(View.VISIBLE);

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
            onFinish();
        } else if (e.getType() == NaviShowPointsEvent.CANCEL_TASK) {
            onBackPressed();
        }
    }

    /**
     * 设置单个检索结果详情
     **/
    private void setSinglePoiDetail() {
        if (receiveAddress == null)
            return;
        /* 设置地址详情文本 */
        mTpidNameText.setText(getAddressShowName(receiveAddress));
        mTpidAddressText.setText(receiveAddress.getAddress());
        switch (mode) {
            case SET_FAVORITE:
                setFavorite(mTpidTipsText, mTpidTargetConfirmText, receiveAddress, false);
                break;
            case SET_HOME:
                setHome(mTpidTipsText, mTpidTargetConfirmText, receiveAddress, false);
                break;
            case SET_COMPANY:
                setCompany(mTpidTipsText, mTpidTargetConfirmText, receiveAddress, false);
                break;
            case SET_START:
                setStart(mTpidTipsText, mTpidTargetConfirmText, receiveAddress, false);
                break;
            case SET_END:
                setEnd(mTpidTipsText, mTpidTargetConfirmText, receiveAddress, false);
                break;
        }
        /* 隐藏地址详情ViewPager区域和地址列表区域视图，显示单个地址详情视图 */
        mAmosfPoiDetailBox.setVisibility(View.GONE);
        mAmosfPoiBox.setVisibility(View.GONE);
        mAmosfSinglePoiDetailBox.setVisibility(View.VISIBLE);
    }

    /**
     * 设置常用地址
     **/
    private void setFavorite(TextView tipsView, final TextView confirmView, final BaiduAddress ad, boolean update) {
        tipsView.setVisibility(View.GONE);
        if (update) {
            if (ad.getFavoritedTime() != null) {
                new CommonDialog(SetFavoriteMapActivity.this, "删除确认", "您确定要删除收藏" + getAddressShowName(ad) + "？", "取消", "确定")
                        .setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                            @Override
                            public void onConfirm() {
                                ad.setFavoritedTime(null);
                                ad.setSynced(false);
                                confirmView.setText("收藏");
                                mNaviDao.insertAddress(ad);
                                updateCode = NaviSetPointPresenter.Msg.UPDATE_FAVORITE;
                                Snackbar.make(mAmosfPoiList, "删除成功", Snackbar.LENGTH_SHORT).show();
                                mNaviDao.sync();
                                // Toast.makeText(SetFavoriteMapActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                                onFinish();
                            }
                        }).show();
            } else {
                ad.setFavoritedTime(new Date());
                confirmView.setText("删除");
                mNaviDao.insertAddress(ad);
                Snackbar.make(mAmosfPoiList, "收藏成功", Snackbar.LENGTH_SHORT).show();
                updateCode = NaviSetPointPresenter.Msg.UPDATE_FAVORITE;
                mNaviDao.sync();
                onFinish();
            }
        } else {
            if (ad.getFavoritedTime() != null) {
                confirmView.setText("删除");
            } else {
                confirmView.setText("收藏");
            }
        }
    }

    /**
     * 设置“家”地址
     **/
    private void setHome(final TextView tipsView, final TextView confirmView, final BaiduAddress ad, boolean update) {
        if (update) {
            if (ad.getRemark() != null && ad.getRemark().equals("家")) {
                new CommonDialog(SetFavoriteMapActivity.this, "清除确认", "您确定要清除家的位置：" + ad.getName() + "？", "取消", "确定")
                        .setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                            @Override
                            public void onConfirm() {
                                confirmView.setText("设为家");
                                tipsView.setVisibility(View.GONE);
                                ad.setRemark(null);
                                mNaviDao.insertAddress(ad);
                                // mNaviDao.removeHomeOrCompany("家");
                                updateCode = NaviSetPointPresenter.Msg.UPDATE_HOME;
                                // Toast.makeText(SetFavoriteMapActivity.this, "清除成功", Toast.LENGTH_SHORT).show();
                                Snackbar.make(mAmosfPoiList, "清除成功", Snackbar.LENGTH_SHORT).show();
                                mNaviDao.sync();
                                onFinish();
                            }
                        }).show();
            } else {
                BaiduAddress homeAddr = new BaiduAddress();
                mNaviDao.removeHomeOrCompany("家");
                homeAddr.reset(ad);
                homeAddr.setFavoritedTime(new Date());
                homeAddr.setRemark("家");
                homeAddr.setId(null);
                //                ad.setFavoritedTime(new Date());
                //                ad.setRemark("家");
                tipsView.setText("已设为家");
                tipsView.setVisibility(View.VISIBLE);
                confirmView.setText("清空");
                //                mNaviDao.insertAddress(ad);
                mNaviDao.insertAddress(homeAddr);
                // BaiduAddress homeAddr = new BaiduAddress();
                //  homeAddr.reset(ad);
                //   Toast.makeText(SetFavoriteMapActivity.this, "已设为家", Toast.LENGTH_SHORT).show();

                Snackbar.make(mAmosfPoiList, "已设为家", Snackbar.LENGTH_SHORT).show();
                updateCode = NaviSetPointPresenter.Msg.UPDATE_HOME;
                mNaviDao.sync();
                onFinish();
            }
        } else {
            if (ad.getRemark() != null && ad.getRemark().equals("家")) {
                tipsView.setText("已设为家");
                tipsView.setVisibility(View.VISIBLE);
                confirmView.setText("清空");
            } else {
                confirmView.setText("设为家");
                tipsView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 设置"单位"地址
     **/
    private void setCompany(final TextView tipsView, final TextView confirmView, final BaiduAddress ad, boolean update) {
        if (update) {
            if (ad.getRemark() != null && ad.getRemark().equals("单位")) {
                new CommonDialog(SetFavoriteMapActivity.this, "清除确认", "您确定要清除单位的位置：" + ad.getName() + "？", "取消", "确定")
                        .setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                            @Override
                            public void onConfirm() {
                                confirmView.setText("设为单位");
                                tipsView.setVisibility(View.GONE);
                                ad.setRemark(null);
                                ad.setSynced(false);
                                mNaviDao.insertAddress(ad);
                                updateCode = NaviSetPointPresenter.Msg.UPDATE_COMPANY;
                                // Toast.makeText(SetFavoriteMapActivity.this, "清除成功", Toast.LENGTH_SHORT).show();
                                Snackbar.make(mAmosfPoiList, "清除成功", Snackbar.LENGTH_SHORT).show();
                                mNaviDao.sync();
                                onFinish();
                            }
                        }).show();
            } else {
                BaiduAddress companyAddr = new BaiduAddress();
                mNaviDao.removeHomeOrCompany("单位");
                companyAddr.reset(ad);
                companyAddr.setRemark("单位");
                companyAddr.setFavoritedTime(new Date());
                companyAddr.setId(null);
                //                ad.setFavoritedTime(new Date());
                //                ad.setRemark("单位");
                tipsView.setText("已设为单位");
                tipsView.setVisibility(View.VISIBLE);
                confirmView.setText("清空");
                //                mNaviDao.insertAddress(ad);
                mNaviDao.insertAddress(companyAddr);
                //                BaiduAddress companyAddr = new BaiduAddress();
                //                companyAddr.reset(ad);
                //                mAppConfig.companyAddress = companyAddr;
                // Toast.makeText(SetFavoriteMapActivity.this, "已设为单位", Toast.LENGTH_SHORT).show();
                Snackbar.make(mAmosfPoiList, "已设为单位", Snackbar.LENGTH_SHORT).show();
                updateCode = NaviSetPointPresenter.Msg.UPDATE_COMPANY;
                mNaviDao.sync();
                onFinish();
            }
        } else {
            if (ad.getRemark() != null && ad.getRemark().equals("单位")) {
                tipsView.setText("已设为单位");
                tipsView.setVisibility(View.VISIBLE);
                confirmView.setText("清空");
            } else {
                confirmView.setText("设为单位");
                tipsView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 设置"出发地"地址
     **/
    private void setStart(final TextView tipsView, final TextView confirmView, final BaiduAddress ad, boolean update) {
        if (update) {
            if (ad.getRemark() != null && ad.getRemark().equals("出发地")) {
                new CommonDialog(SetFavoriteMapActivity.this, "清除确认", "您确定要清除出发地位置：" + ad.getName() + "？", "取消", "确定")
                        .setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                            @Override
                            public void onConfirm() {
                                confirmView.setText("设为出发地");
                                tipsView.setVisibility(View.GONE);
                                ad.setRemark(null);
                                ad.setFavoritedTime(null);
                                mAppConfig.startAddress = null;
                                mNaviDao.insertAddress(ad);
                                updateCode = NaviSetPointPresenter.Msg.UPDATE_START;
                                //Toast.makeText(SetFavoriteMapActivity.this, "清除成功", Toast.LENGTH_SHORT).show();
                                Snackbar.make(mAmosfPoiList, "清除成功", Snackbar.LENGTH_SHORT).show();
                                onFinish();
                            }
                        }).show();
            } else {
                BaiduAddress startAddr = new BaiduAddress();
                mNaviDao.removeHomeOrCompany("出发地");
                startAddr.reset(ad);
                startAddr.setRemark("出发地");
                //清除id值，插入数据才能成功
                startAddr.setId(null);
                startAddr.setFavoritedTime(null);
                tipsView.setText("已设为出发地");
                tipsView.setVisibility(View.VISIBLE);
                confirmView.setText("清空");
                mNaviDao.insertAddress(startAddr);
                mAppConfig.startAddress = startAddr;
                updateCode = NaviSetPointPresenter.Msg.UPDATE_START;
                onFinish();
            }
        } else {
            if (ad.getRemark() != null && ad.getRemark().equals("出发地")) {
                tipsView.setText("已设为出发地");
                tipsView.setVisibility(View.VISIBLE);
                confirmView.setText("清空");
            } else {
                confirmView.setText("设为出发地");
                tipsView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 设置"目的地"地址
     **/
    private void setEnd(final TextView tipsView, final TextView confirmView, final BaiduAddress ad, boolean update) {
        if (update) {
            if (ad.getRemark() != null && ad.getRemark().equals("目的地")) {
                new CommonDialog(SetFavoriteMapActivity.this, "清除确认", "您确定要清除目的地位置：" + ad.getName() + "？", "取消", "确定")
                        .setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                            @Override
                            public void onConfirm() {
                                confirmView.setText("设为目的地");
                                tipsView.setVisibility(View.GONE);
                                ad.setRemark(null);
                                ad.setFavoritedTime(null);
                                ad.setSynced(false);
                                mAppConfig.endAddress = null;
                                mNaviDao.insertAddress(ad);
                                updateCode = NaviSetPointPresenter.Msg.UPDATE_END;
                                //Toast.makeText(SetFavoriteMapActivity.this, "清除成功", Toast.LENGTH_SHORT).show();
                                Snackbar.make(mAmosfPoiList, "清除成功", Snackbar.LENGTH_SHORT).show();
                                mNaviDao.sync();
                                onFinish();
                            }
                        }).show();
            } else {
                BaiduAddress endAddr = new BaiduAddress();
                mNaviDao.removeHomeOrCompany("目的地");
                endAddr.reset(ad);
                endAddr.setRemark("目的地");
                endAddr.setId(null);
                endAddr.setFavoritedTime(null);
                tipsView.setText("已设为目的地");
                tipsView.setVisibility(View.VISIBLE);
                confirmView.setText("清空");
                mNaviDao.insertAddress(endAddr);
                mAppConfig.endAddress = endAddr;
                updateCode = NaviSetPointPresenter.Msg.UPDATE_END;
                onFinish();
            }
        } else {
            if (ad.getRemark() != null && ad.getRemark().equals("目的地")) {
                tipsView.setText("已设为目的地");
                tipsView.setVisibility(View.VISIBLE);
                confirmView.setText("清空");
            } else {
                confirmView.setText("设为目的地");
                tipsView.setVisibility(View.GONE);
            }
        }
    }

    @OnClick({R.id.amosf_back_bt, R.id.amosf_search_bt, R.id.amosf_map_locate_bt, R.id.amosf_map_its_bt,
            R.id.amosf_zoom_in_bt, R.id.amosf_zoom_out_bt, R.id.amosf_poi_detial_pre_bt,
            R.id.amosf_poi_detial_next_bt, R.id.tpid_target_confirm_bt})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.amosf_back_bt:
                goBack();
                break;
            case R.id.amosf_search_bt:
                if (NetUtil.getInstance(SetFavoriteMapActivity.this).getCurrentNetType().equals(NetUtil.NetType.NETWORK_TYPE_NONE) && !TextUtils.isEmpty(mAmosfSearchEdit.getText())) {
                    if (getCurrentFocus() != null) {
                        //隐藏输入键盘
                        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                                .hideSoftInputFromWindow(getCurrentFocus()
                                                .getWindowToken(),
                                        InputMethodManager.HIDE_NOT_ALWAYS);
                    }
                    //弹出没有网络的提示
                    final CommonDialog commonDialog = new CommonDialog(SetFavoriteMapActivity.this, "网络错误", "网络状态不佳，请检查网络设置", "确定");
                    commonDialog.setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                        @Override
                        public void onConfirm() {
                            commonDialog.cancel();
                        }
                    }).show();
                } else if (!TextUtils.isEmpty(mAmosfSearchEdit.getText())) {
                    goSearch();
                }
                break;
            case R.id.amosf_map_locate_bt:
                location();
                break;
            case R.id.amosf_map_its_bt:
                if (baiduMap.isTrafficEnabled()) {
                    baiduMap.setTrafficEnabled(false);
                    ((ImageButton) view).setImageResource(R.drawable.bnav_common_ic_map_its_off);
                } else {
                    baiduMap.setTrafficEnabled(true);
                    ((ImageButton) view).setImageResource(R.drawable.bnav_common_ic_map_its_on);
                }
                break;
            case R.id.amosf_zoom_in_bt:
                baiduMap.setMapStatus(MapStatusUpdateFactory.zoomIn());
                break;
            case R.id.amosf_zoom_out_bt:
                baiduMap.setMapStatus(MapStatusUpdateFactory.zoomOut());
                break;
            case R.id.amosf_poi_detial_pre_bt:
                if (mAmosfPoiDetailPager.getCurrentItem() > 0 && mAmosfPoiDetailPager.getCurrentItem() < list.size()) {
                    showPoiDetail(mAmosfPoiDetailPager.getCurrentItem() - 1);
                }
                break;
            case R.id.amosf_poi_detial_next_bt:
                if (mAmosfPoiDetailPager.getCurrentItem() >= 0 && mAmosfPoiDetailPager.getCurrentItem() < list.size() - 1) {
                    showPoiDetail(mAmosfPoiDetailPager.getCurrentItem() + 1);
                }
                break;
            //            case R.id.amosf_search_voice_bt:
            //                new NaviVoiceInputDialog(SetFavoriteMapActivity.this).show();
            //                break;
            case R.id.tpid_target_confirm_bt:
                System.out.println("onclick" + receiveAddress);
                if (receiveAddress == null)
                    return;
                switch (mode) {
                    case SET_FAVORITE:
                        if (receiveAddress.getFavoritedTime() == null)
                            new CommonEditDialog(SetFavoriteMapActivity.this, "填写备注名", receiveAddress.getName(), "保存")
                                    .setOnConfirmListener(new CommonEditDialog.OnConfirmListener() {
                                        @Override
                                        public void onConfirm(String text) {
                                            Log.i(TAG, "onConfirm>>text=" + text);
                                            if (!text.equals(receiveAddress.getName()))
                                                receiveAddress.setRemark(text);
                                            setFavorite(mTpidTipsText, mTpidTargetConfirmText, receiveAddress, true);
                                        }
                                    })
                                    .setInputLimits(100)
                                    .show();
                        else {
                            setFavorite(mTpidTipsText, mTpidTargetConfirmText, receiveAddress, true);
                        }
                        break;
                    case SET_HOME:
                        setHome(mTpidTipsText, mTpidTargetConfirmText, receiveAddress, true);
                        break;
                    case SET_COMPANY:
                        setCompany(mTpidTipsText, mTpidTargetConfirmText, receiveAddress, true);
                        break;
                    case SET_START:
                        setStart(mTpidTipsText, mTpidTargetConfirmText, receiveAddress, true);
                        break;
                    case SET_END:
                        setEnd(mTpidTipsText, mTpidTargetConfirmText, receiveAddress, true);
                        break;
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_REMARK) {
            BaiduAddress bd;
            if (receiveAddress == null || data == null || (bd = data.getParcelableExtra("address")) == null)
                return;
            remarkAddress.reset(bd);
            if (remarkAddress == receiveAddress) {
                setSinglePoiDetail();

            } else {
                mPagerAdapter.notifyDataSetChanged();
            }
            updateCode = NaviSetPointPresenter.Msg.UPDATE_FAVORITE;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        mAmosfMap.onResume();
        super.onResume();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onPause() {
        mAmosfMap.onPause();
        super.onPause();
    }

    public void onFinish() {
        setResult(updateCode);
        goBack();
    }

    @Override
    protected void onDestroy() {
        pSearch.destroy();
        sSearch.destroy();
        mAmosfMap.onDestroy();
        EventBus.getDefault().unregister(this);
        super.onDestroy();
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
     * 设置地图比例，保证所有poi点可视
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
            if (poiInfo.location == null)
                continue;
            BaiduAddress address = new BaiduAddress();
            setBaiduAddressFromPoiInfo(address, poiInfo);
            aList.add(address);
        }
    }

    /**
     * 列表item点击监听器
     **/
    private OnItemClickListener listItemClickListener = new OnItemClickListener() {
        @Override
        public void onClick(View itemView, int position) {
            showPoiDetail(position);
        }

        @Override
        public void onLongClick(View intemView, int position) {

        }
    };

    /**
     * ViewPager页面更改监听器
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

    /**
     * 地址列表高度变化监听器
     **/
    private DrawForExpandLayout.ScaleChangedListener scaleChangedListener = new DrawForExpandLayout.ScaleChangedListener() {

        @Override
        public void max() {

        }

        @Override
        public void min() {
            if (list.size() == 0)
                return;
            int p = ((LinearLayoutManager) mAmosfPoiList.getLayoutManager()).findFirstVisibleItemPosition();
            p = Math.max(p, 0);
            showPoiDetail(p);
        }
    };

    /**
     * 搜索输入栏监听器
     **/
    private RealTimeUpdateSearchBox.OnSearchListener searchListener = new RealTimeUpdateSearchBox.OnSearchListener() {

        /** editBox栏点击时调用 **/
        @Override
        public void editClick() {
            if (TextUtils.isEmpty(mAmosfSearchEdit.getText()) && favoriteToSetmode != FAVORITE_SET_HOME
                    && favoriteToSetmode != FAVORITE_SET_COMPANY && mode != SET_FAVORITE) {
                mAmosfMapBox.setVisibility(View.GONE);
                mAmosfPoiBox.setVisibility(View.GONE);
                mAmosfPoiDetailBox.setVisibility(View.GONE);
                mFavAndHisAdapter.notifyDataSetChanged();
                mNaviFavoriteAndHistory.setVisibility(View.VISIBLE);
            }
        }

        /** 文本更新时调用 **/
        @Override
        public void onSearchTextUpdate(String text) {
            mNaviFavoriteAndHistory.setVisibility(View.GONE);
            mAmosfMapBox.setVisibility(View.VISIBLE);
            keyword = text;
            if (!TextUtils.isEmpty(text)) {
                /* 建议请求入口 */
                sSearch.requestSuggestion(new SuggestionSearchOption().city(mAppConfig.selectedCityInSearchPoi == null ?
                        mAddress.getCity() : mAppConfig.selectedCityInSearchPoi).keyword(text));
            } else {
                mAppConfig.selectedCityInSearchPoi = null;
                findViewById(R.id.amosf_suggest_list_box).setVisibility(View.GONE);
                mAmosfSearchEdit.setSearchIdleState();
                //  mAmosfSearchBt.setVisibility(View.GONE);
                //  mAmosfSearchVoiceBt.setVisibility(View.VISIBLE);
            }
        }

        /** 搜索结果回调完成时调用  **/
        @Override
        public void onSearchSuggestCompleted() {
            // mAmosfSearchBt.setVisibility(View.VISIBLE);
            // mAmosfSearchVoiceBt.setVisibility(View.GONE);
        }

        /** 点击手机软件盘搜索按钮时调用 **/
        @Override
        public void onSearch(String text) {
            keyword = text;
            goSearch();
        }
    };

    private void goSearch() {
        goSearch(null);
    }

    /**
     * 根据输入地址搜索
     **/
    private void goSearch(BaiduAddress bd) {
        if (bd == null) {
            bd = new BaiduAddress();
            bd.setName(keyword);
            bd.setAddress("");
            bd.setCity(mAddress.getCity());
        }
        bd.setSearchKeyWord(keyword);
        bd.setFavoritedTime(null);
        /* 保存搜索记录 */
        mNaviDao.insertAddress(bd);
        updateHistory = true;
        if (!TextUtils.isEmpty(bd.getCity())) {
            mAppConfig.selectedCityInSearchPoi = bd.getCity();
        }
        if (getCurrentFocus() != null) {
            //隐藏输入键盘
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(getCurrentFocus()
                                    .getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);
        }
        //隐藏搜索建议列表
        findViewById(R.id.amosf_suggest_list_box).setVisibility(View.GONE);
        /* 城市内检索 */
        pSearch.searchInCity(new PoiCitySearchOption().city(mAppConfig.selectedCityInSearchPoi == null ?
                mAddress.getCity() : mAppConfig.selectedCityInSearchPoi).pageCapacity(PER_PAGE).pageNum(poiPageCode).keyword(bd == null ? keyword : bd.getName()));

    }

    /**
     * 建议搜索列表item点击监听器
     **/
    AdaptHeightListView.OnItemClickListener suggestItemClickListener = new AdaptHeightListView.OnItemClickListener() {

        /** 列表item视图点击时调用 **/
        @Override
        public void onClick(BaiduAddress address) {
            mAmosfSearchEdit.setTextNoUpdate(address.getName());
            keyword = address.getName();
            goSearch(address);
        }

        /** item右侧箭头图标点击时调用 **/
        @Override
        public void onSelect(BaiduAddress address) {
            this.onClick(address);
        }
    };

    /**
     * Poi检索结果回调
     **/
    @Override
    public void onGetPoiResult(PoiResult result) {
        mAmosfPoiListBox.setRefreshing(false);
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            // Toast.makeText(this, "搜索发生错误：" + result.error, Toast.LENGTH_LONG).show();
            Snackbar.make(mAmosfPoiList, SetFavoriteMapActivity.this.getResources().getString(R.string.navi_no_result), Snackbar.LENGTH_SHORT).show();
            mAmosfSearchEdit.setSearchCompletedState();
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            if (poiOverlay != null) {
                poiOverlay.removeFromMap();
            }
            if (poiOverlay == null || !(poiOverlay instanceof ConfirmPoiOverlay)) {
                poiOverlay = new ConfirmPoiOverlay(baiduMap);
                baiduMap.setOnMarkerClickListener(poiOverlay);
            }

            ((ConfirmPoiOverlay) poiOverlay).setData(result);
            poiOverlay.addToMap();

            totalPageNum = result.getTotalPageNum();
            /* 设置检索结果列表高度（手机屏幕的一半） */
            mAmosfPoiBox.getLayoutParams().height = ScreenUtil.getInstance().getHeightPixels() / 2;
            list.clear();
            list.addAll(result.getAllPoi());
            setAlist(list);
            mListAdapter.notifyDataSetChanged();
            mPagerAdapter.notifyDataSetChanged();
            mAmosfPoiDetailBox.setVisibility(View.GONE);
            mAmosfSinglePoiDetailBox.setVisibility(View.GONE);
            mAmosfPoiBox.setVisibility(View.VISIBLE);
            setPoiPosition();
            return;
        }
    }

    @Override
    public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

    }

    @Override
    public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

    }

    /**
     * 建议检索结果回调
     **/
    @Override
    public void onGetSuggestionResult(SuggestionResult sr) {
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
            Map<String, Integer> cityMap = new Hashtable<>();
            /* 填充搜索结果城市数据 */
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
            /* 选出结果中出现最多的城市 */
            for (String key : cityMap.keySet()) {
                System.out.println(key + "," + cityMap.get(key));
                if (maxCity == null) {
                    maxCity = key;
                    cityCount = cityMap.get(key);
                } else if (cityMap.get(key) > cityCount) {
                    maxCity = key;
                    cityCount = cityMap.get(key);
                }
            }

            if (cityCount / l < 0.5) {
                selectCityDialog = new SelectCityDialog(SetFavoriteMapActivity.this, cityMap.keySet().toArray(new String[cityMap.size()]), new SelectCityDialog.OnSelectListener() {
                    @Override
                    public void onSelect(String city) {
                        mAppConfig.selectedCityInSearchPoi = city;
                        sSearch.requestSuggestion(new SuggestionSearchOption().city(mAppConfig.selectedCityInSearchPoi).keyword(keyword));
                    }
                });
                selectCityDialog.show();
                return;
            } else {
                mAppConfig.selectedCityInSearchPoi = maxCity;
            }
        }


        BaiduAddress temp;
        List<BaiduAddress> suggests = new ArrayList<>();
        for (SuggestionResult.SuggestionInfo info : list) {
            if (mAppConfig.selectedCityInSearchPoi != null) {
                if (!info.city.equals(mAppConfig.selectedCityInSearchPoi))
                    continue;
            }
            temp = new BaiduAddress();
            temp.setAddress(info.city + info.district);
            temp.setName(info.key);
            temp.setUid(info.uid);
            suggests.add(temp);
        }
        mAmosfSuggestList.loadDate(suggests, keyword);
        mAmosfSuggestListBox.setVisibility(View.VISIBLE);
        mAmosfSearchEdit.setSearchCompletedState();
    }

    /**
     * 下拉刷新
     **/
    @Override
    public void onDownPullRefresh() {
        if (poiPageCode == 0) {
            mAmosfPoiListBox.setRefreshing(false);
            // Toast.makeText(this, "当前已是第一页了", Toast.LENGTH_LONG).show();
            Snackbar.make(mAmosfPoiList, "当前已是第一页了", Snackbar.LENGTH_SHORT).show();
        } else
            pSearch.searchInCity(new PoiCitySearchOption().city(mAppConfig.selectedCityInSearchPoi == null ?
                    mAddress.getCity() : mAppConfig.selectedCityInSearchPoi).keyword(keyword).pageCapacity(PER_PAGE).pageNum(--poiPageCode));
    }

    /**
     * 上拉刷新
     **/
    @Override
    public void onUpPullRefresh() {
        if (poiPageCode == (totalPageNum - 1)) {
            mAmosfPoiListBox.setRefreshing(false);
            //Toast.makeText(this, "没有符合要求的地点了", Toast.LENGTH_LONG).show();
            Snackbar.make(mAmosfPoiList, "没有符合要求的地点了", Snackbar.LENGTH_SHORT).show();
        } else
            pSearch.searchInCity(new PoiCitySearchOption().city(mAppConfig.selectedCityInSearchPoi == null ?
                    mAddress.getCity() : mAppConfig.selectedCityInSearchPoi).keyword(keyword).pageCapacity(PER_PAGE).pageNum(++poiPageCode));

    }

    /**
     * 搜索结果地址列表适配器
     **/
    class FavorPointAdapter extends RecyclerView.Adapter<FavorPointAdapter.FavorPointHolder> {


        @Override
        public FavorPointHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = mInflater.inflate(R.layout.target_poi_item_favorite, parent, false);
            return new FavorPointHolder(itemView);
        }

        @Override
        public void onBindViewHolder(FavorPointHolder holder, int position) {
            PoiInfo poiInfo = list.get(position);
            BaiduAddress baiduAddress = aList.get(position);
            holder.mTpiNameText.setText(new StringBuilder().append(position + 1).append(".").append(poiInfo.name).toString());
            holder.mTpiAddressText.setText(poiInfo.address);

            holder.mTpiTargetConfirmBt.setTag(position);
            holder.itemView.setTag(position);
            switch (mode) {
                case SET_FAVORITE:
                    holder.mTpiItemConfirmText.setText(baiduAddress.getFavoritedTime() == null ? "收藏" : "删除");
                    // holder.mTpiItemConfirmText.setVisibility(View.GONE);
                    //  holder.mTpiFavoriteStarBt.setVisibility(View.VISIBLE);
                    break;
                case SET_HOME:
                    holder.mTpiItemConfirmText.setText(baiduAddress.getRemark() == null || !baiduAddress.getRemark().equals("家") ? "设为家" : "清除");
                    break;
                case SET_COMPANY:
                    holder.mTpiItemConfirmText.setText(baiduAddress.getRemark() == null || !baiduAddress.getRemark().equals("单位") ? "设为单位" : "清除");
                    break;
                case SET_START:
                    holder.mTpiItemConfirmText.setText(baiduAddress.getRemark() == null || !baiduAddress.getRemark().equals("出发地") ? "设为出发地" : "清除");
                    //  holder.mTpiItemConfirmText.setVisibility(View.GONE);
                    //  holder.mTpiFavoriteStarBt.setVisibility(View.VISIBLE);
                    break;
                case SET_END:
                    holder.mTpiItemConfirmText.setText(baiduAddress.getRemark() == null || !baiduAddress.getRemark().equals("目的地") ? "设为目的地" : "清除");
                    // holder.mTpiItemConfirmText.setVisibility(View.GONE);
                    //  holder.mTpiFavoriteStarBt.setVisibility(View.VISIBLE);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        private OnItemClickListener itemClickListener;

        public void setOnItemClickListener(OnItemClickListener listener) {
            itemClickListener = listener;
        }

        class FavorPointHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.tpi_name_text)
            TextView mTpiNameText;
            @BindView(R.id.tpi_address_text)
            TextView mTpiAddressText;
            @BindView(R.id.tpi_item_confirm_text)
            TextView mTpiItemConfirmText;
            //            @BindView(R.id.tpi_favorite_star_bt)
            //            ImageButton mTpiFavoriteStarBt;
            @BindView(R.id.tpi_target_confirm_bt)
            LinearLayout mTpiTargetConfirmBt;

            public FavorPointHolder(final View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = (int) itemView.getTag();
                        if (itemClickListener != null) {
                            itemClickListener.onClick(v, position);
                        }
                    }
                });
                //                if(mTpiFavoriteStarBt!=null){
                //                    //如果收藏按钮显示则提供点击事件
                //                    mTpiFavoriteStarBt.setOnClickListener(new View.OnClickListener() {
                //                        @Override
                //                        public void onClick(View v) {
                //                            //收藏地址
                //                            int position = (int) mTpiTargetConfirmBt.getTag();
                //                            BaiduAddress baiduAddress = aList.get(position);
                //                            baiduAddress.setFavoritedTime(new Date());
                //                            mNaviDao.insertAddress(baiduAddress);
                //                            updateCode = NaviSetPointPresenter.Msg.UPDATE_FAVORITE;
                //                            Toast.makeText(SetFavoriteMapActivity.this, "已收藏", Toast.LENGTH_SHORT).show();
                //                            onFinish();
                //                        }
                //                    });
                //                }
                mTpiTargetConfirmBt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = (int) mTpiTargetConfirmBt.getTag();
                        final BaiduAddress baiduAddress = aList.get(position);
                        switch (mode) {
                            case SET_FAVORITE:
                                if (baiduAddress.getFavoritedTime() == null) {
                                    new CommonEditDialog(SetFavoriteMapActivity.this, "填写备注名", baiduAddress.getName(), "保存")
                                            .setOnConfirmListener(new CommonEditDialog.OnConfirmListener() {
                                                @Override
                                                public void onConfirm(String text) {
                                                    if (!text.equals(baiduAddress.getName())) {
                                                        baiduAddress.setRemark(text);
                                                    }
                                                    baiduAddress.setFavoritedTime(new Date());
                                                    mNaviDao.insertAddress(baiduAddress);
                                                    updateCode = NaviSetPointPresenter.Msg.UPDATE_FAVORITE;
                                                    mNaviDao.sync();
                                                    onFinish();
                                                }
                                            })
                                            .setInputLimits(100)
                                            .show();
                                } else {
                                    new CommonDialog(SetFavoriteMapActivity.this,
                                            "删除确认",
                                            "您确定要删除收藏" + getAddressShowName(baiduAddress) + "?",
                                            "取消", "确认")
                                            .setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                                                @Override
                                                public void onConfirm() {
                                                    baiduAddress.setFavoritedTime(null);
                                                    baiduAddress.setSynced(false);
                                                    baiduAddress.setRecyle(1);
                                                    mNaviDao.insertAddress(baiduAddress);
                                                    updateCode = NaviSetPointPresenter.Msg.UPDATE_FAVORITE;
                                                    mNaviDao.sync();
                                                    onFinish();
                                                }
                                            }).show();
                                }
                                break;
                            case SET_HOME:
                                if (baiduAddress.getRemark() == null || !baiduAddress.getRemark().equals("家")) {
                                    baiduAddress.setFavoritedTime(new Date());
                                    baiduAddress.setRemark("家");
                                    /* 删除原来家地址的标记 */
                                    mNaviDao.removeHomeOrCompany("家");
                                    /* 保存新的家的地址 */
                                    mNaviDao.insertAddress(baiduAddress);
                                    //   mAppConfig.homeAddress = baiduAddress;
                                    // Toast.makeText(SetFavoriteMapActivity.this, "已设为家", Toast.LENGTH_SHORT).show();
                                    Snackbar.make(mAmosfPoiList, "已设为家", Snackbar.LENGTH_SHORT).show();
                                    updateCode = NaviSetPointPresenter.Msg.UPDATE_HOME;
                                    mNaviDao.sync();
                                    onFinish();
                                } else {
                                    new CommonDialog(SetFavoriteMapActivity.this,
                                            "删除确认",
                                            "您确定要清除家的位置：" + getAddressShowName(baiduAddress) + "?",
                                            "取消", "确认")
                                            .setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                                                @Override
                                                public void onConfirm() {
                                                    baiduAddress.setRemark(null);
                                                    baiduAddress.setSynced(false);
                                                    mNaviDao.insertAddress(baiduAddress);
                                                    notifyDataSetChanged();
                                                    updateCode = NaviSetPointPresenter.Msg.UPDATE_HOME;
                                                    mNaviDao.sync();
                                                    onFinish();
                                                }
                                            }).show();
                                }
                                break;
                            case SET_COMPANY:
                                if (baiduAddress.getRemark() == null || !baiduAddress.getRemark().equals("单位")) {
                                    baiduAddress.setFavoritedTime(new Date());
                                    baiduAddress.setRemark("单位");
                                    mNaviDao.removeHomeOrCompany("单位");
                                    mNaviDao.insertAddress(baiduAddress);
                                    //  Toast.makeText(SetFavoriteMapActivity.this, "已设为单位", Toast.LENGTH_SHORT).show();
                                    Snackbar.make(mAmosfPoiList, "已设为单位", Snackbar.LENGTH_SHORT).show();
                                    updateCode = NaviSetPointPresenter.Msg.UPDATE_COMPANY;
                                    mNaviDao.sync();
                                    onFinish();
                                } else {
                                    new CommonDialog(SetFavoriteMapActivity.this,
                                            "删除确认",
                                            "您确定要清除公司的位置：" + getAddressShowName(baiduAddress) + "?",
                                            "取消", "确认")
                                            .setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                                                @Override
                                                public void onConfirm() {
                                                    baiduAddress.setRemark(null);
                                                    baiduAddress.setSynced(false);
                                                    mNaviDao.insertAddress(baiduAddress);
                                                    notifyDataSetChanged();
                                                    updateCode = NaviSetPointPresenter.Msg.UPDATE_COMPANY;
                                                    mNaviDao.sync();
                                                    onFinish();
                                                }
                                            }).show();
                                }
                                break;
                            case SET_START:
                                if (baiduAddress.getRemark() == null || !baiduAddress.getRemark().equals("出发地")) {
                                    baiduAddress.setFavoritedTime(new Date());
                                    baiduAddress.setRemark("出发地");
                                    baiduAddress.setFavoritedTime(null);
                                    mNaviDao.removeHomeOrCompany("出发地");
                                    mNaviDao.insertAddress(baiduAddress);
                                    mAppConfig.startAddress = baiduAddress;
                                    // Toast.makeText(SetFavoriteMapActivity.this, "已设为出发地", Toast.LENGTH_SHORT).show();
                                    Snackbar.make(mAmosfPoiList, "已设为出发地", Snackbar.LENGTH_SHORT).show();
                                    updateCode = NaviSetPointPresenter.Msg.UPDATE_START;
                                    onFinish();
                                } else {
                                    new CommonDialog(SetFavoriteMapActivity.this,
                                            "删除确认",
                                            "您确定要清除出发地位置：" + getAddressShowName(baiduAddress) + "?",
                                            "取消", "确认")
                                            .setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                                                @Override
                                                public void onConfirm() {
                                                    baiduAddress.setRemark(null);
                                                    mNaviDao.insertAddress(baiduAddress);
                                                    notifyDataSetChanged();
                                                    mAppConfig.startAddress = null;
                                                    updateCode = NaviSetPointPresenter.Msg.UPDATE_START;
                                                    onFinish();
                                                }
                                            }).show();
                                }
                                break;
                            case SET_END:
                                if (baiduAddress.getRemark() == null || !baiduAddress.getRemark().equals("目的地")) {
                                    baiduAddress.setFavoritedTime(new Date());
                                    baiduAddress.setRemark("目的地");
                                    baiduAddress.setFavoritedTime(null);
                                    mNaviDao.removeHomeOrCompany("目的地");
                                    mNaviDao.insertAddress(baiduAddress);
                                    mAppConfig.endAddress = baiduAddress;
                                    // Toast.makeText(SetFavoriteMapActivity.this, "已设为目的地", Toast.LENGTH_SHORT).show();
                                    Snackbar.make(mAmosfPoiList, "已设为目的地", Snackbar.LENGTH_SHORT).show();
                                    updateCode = NaviSetPointPresenter.Msg.UPDATE_END;
                                    onFinish();
                                } else {
                                    new CommonDialog(SetFavoriteMapActivity.this,
                                            "删除确认",
                                            "您确定要清除目的地位置：" + getAddressShowName(baiduAddress) + "?",
                                            "取消", "确认")
                                            .setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                                                @Override
                                                public void onConfirm() {
                                                    baiduAddress.setRemark(null);
                                                    mNaviDao.insertAddress(baiduAddress);
                                                    mAppConfig.endAddress = null;
                                                    notifyDataSetChanged();
                                                    updateCode = NaviSetPointPresenter.Msg.UPDATE_END;
                                                    onFinish();
                                                }
                                            }).show();
                                }
                                break;
                        }
                    }
                });
            }
        }

    }

    /**
     * 获取地址的显示名称
     **/
    private String getAddressShowName(BaiduAddress ba) {
        if (TextUtils.isEmpty(ba.getRemark())) {
            return ba.getName();
        } else if (ba.getRemark().equals("家") || ba.getRemark().equals("单位") || ba.getRemark().equals("出发地") || ba.getRemark().equals("目的地")) {
            return ba.getName();
        } else {
            return ba.getRemark();
        }
    }

    /**
     * ViewPager适配器
     **/
    class FavorPointPagerAdapter extends PagerAdapter {

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
            PagerHolder holder;
            /* 加载视图 */
            if (itemView == null) {
                itemView = mInflater.inflate(R.layout.target_poi_item_favorite_detail, null);
                chilren.put(position, itemView);
            }
            /* 绑定数据 */
            holder = new PagerHolder(itemView, position);
            holder.tpidFavoriteBt.setTag(position);
            holder.tpidSetTargetBt.setTag(position);
            holder.tpidTargetConfirmBt.setTag(position);
            holder.tpidNameText.setText((position + 1) + "." + getAddressShowName(address));
            holder.tpidAddressText.setText(address.getAddress());
            switch (mode) {
                case SET_FAVORITE:
                    holder.tpidSetTargetBt.setText("收藏");
                    break;
                case SET_HOME:
                    holder.tpidSetTargetBt.setText("设为家");
                    break;
                case SET_COMPANY:
                    holder.tpidSetTargetBt.setText("设为单位");
                    break;
                case SET_START:
                    holder.tpidSetTargetBt.setText("设为出发地");
                    break;
                case SET_END:
                    holder.tpidSetTargetBt.setText("设为目的地");
                    break;
            }
            Double distance = DistanceUtil.getDistance(new LatLng(mAddress.getLatitude(), mAddress.getLongitude()),
                    new LatLng(address.getLatitude(), address.getLongitude())) / 1000;
            holder.mTpidDistanceText.setText(String.format("%.1f", distance) + "km");
            if (address.getFavoritedTime() != null) {
                holder.tpidFavoriteBt.setText("已收藏");
                holder.tpidFavoriteBt.getBackground().setLevel(1);
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
            TextView tpidNameText;
            @BindView(R.id.tpid_address_text)
            TextView tpidAddressText;
            @BindView(R.id.tpid_target_confirm_bt)
            LinearLayout tpidTargetConfirmBt;
            @BindView(R.id.tpid_favorite_bt)
            TextView tpidFavoriteBt;
            @BindView(R.id.tpid_set_target_bt)
            TextView tpidSetTargetBt;
            @BindView(R.id.tpid_distance_text)
            TextView mTpidDistanceText;
            private int position;

            public PagerHolder(View itemView, int position) {
                ButterKnife.bind(this, itemView);
                this.position = position;
            }

            @OnClick({R.id.tpid_set_target_bt, R.id.tpid_favorite_bt})
            public void onClick(View view) {
                final BaiduAddress baiduAddress = aList.get(position);
                switch (view.getId()) {
                    case R.id.tpid_set_target_bt:
                        switch (mode) {
                            case SET_FAVORITE:
                                if (baiduAddress.getFavoritedTime() == null)
                                    new CommonEditDialog(SetFavoriteMapActivity.this, "填写备注名", list.get(position).name, "保存")
                                            .setOnConfirmListener(new CommonEditDialog.OnConfirmListener() {
                                                @Override
                                                public void onConfirm(String text) {
                                                    if (!text.equals(baiduAddress.getName())) {
                                                        baiduAddress.setRemark(text);
                                                    }
                                                    setFavorite(mTpidTipsText, mTpidTargetConfirmText, baiduAddress, true);
                                                }
                                            })
                                            .setInputLimits(100)
                                            .show();
                                else {
                                    setFavorite(mTpidTipsText, mTpidTargetConfirmText, baiduAddress, true);
                                }
                                break;
                            case SET_HOME:
                                setHome(mTpidTipsText, mTpidTargetConfirmText, baiduAddress, true);
                                break;
                            case SET_COMPANY:
                                setCompany(mTpidTipsText, mTpidTargetConfirmText, baiduAddress, true);
                                break;
                            case SET_START:
                                setStart(mTpidTipsText, mTpidTargetConfirmText, baiduAddress, true);
                                break;
                            case SET_END:
                                setEnd(mTpidTipsText, mTpidTargetConfirmText, baiduAddress, true);
                                break;
                        }
                        break;
                    case R.id.tpid_favorite_bt:
                        TextView favor = (TextView) view;
                        if (baiduAddress.getFavoritedTime() != null) {
                            baiduAddress.setFavoritedTime(null);
                            baiduAddress.setSynced(false);
                            favor.setText("未收藏");
                            favor.getBackground().setLevel(0);
                        } else {
                            baiduAddress.setFavoritedTime(new Date());
                            favor.setText("已收藏");
                            favor.getBackground().setLevel(1);
                        }
                        mNaviDao.insertAddress(baiduAddress);
                        mNaviDao.sync();
                        break;
                }


            }
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
            if (mAmosfMap != null) {
                try {
                    mAmosfMap.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ConfirmCustomPoiOverlay.super.zoomToSpan();
                            if (mAmosfPoiBox.getVisibility() == View.VISIBLE) {
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


    class ConfirmPoiOverlay extends PoiOverlay {

        public ConfirmPoiOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public boolean onPoiClick(int i) {
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
            if (mAmosfMap != null) {
                try {
                    mAmosfMap.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ConfirmPoiOverlay.super.zoomToSpan();
                            if (mAmosfPoiBox.getVisibility() == View.VISIBLE) {
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

    public class FavoriteAndHistoryAdapter extends RecyclerView.Adapter {
        private final static int FAVORITE_TYPE = 0;
        private final static int HISTORY_TYPE = 1;
        private final static int CLEAR_TYPE = 2;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView;
            RecyclerView.ViewHolder viewHolder = null;
            if (viewType == FAVORITE_TYPE) {
                itemView = mInflater.inflate(R.layout.navi_favorite_list, parent, false);
                viewHolder = new FavoriteHolder(itemView);
            } else if (viewType == HISTORY_TYPE) {
                itemView = mInflater.inflate(R.layout.navi_history_location_item, parent, false);
                viewHolder = new HistoryHolder(itemView);
            } else if (viewType == CLEAR_TYPE) {
                itemView = mInflater.inflate(R.layout.navi_search_clear_record, parent, false);
                viewHolder = new ClearHistoryHolder(itemView);
            }
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof FavoriteHolder) {
                FavoriteHolder favoriteHolder = (FavoriteHolder) holder;
                favoriteHolder.updateFavoriteList();
            } else if (holder instanceof HistoryHolder) {
                BaiduAddress address = historyList.get(position - 1);
                ((HistoryHolder) holder).mHistoryLocationItem.setTag(position - 1);
                if (address != null) {
                    ((HistoryHolder) holder).mLocationItemName.setText(address.getName());
                    ((HistoryHolder) holder).mLocationItemAddress.setText(address.getAddress());
                }
            } else if (holder instanceof ClearHistoryHolder) {
                ((ClearHistoryHolder) holder).updateClearHistoryBox();
            }

        }

        @Override
        public int getItemCount() {
            mNaviDao.getHistoryList(historyList, historyList.size());
            return historyList.size() + 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return FAVORITE_TYPE;
            } else if (position == getItemCount() - 1) {
                return CLEAR_TYPE;
            } else {
                return HISTORY_TYPE;
            }
        }

        class FavoriteHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.navi_favorite_list)
            RelativeLayout mNaviFavoriteList;
            @BindView(R.id.image_more_down)
            ImageButton mImageMoreDown;
            @BindView(R.id.image_more_up)
            ImageButton mImageMoreUp;
            @BindView(R.id.favorite_recyclerview)
            RecyclerView mFavoriteRecyclerView;
            @BindView(R.id.navi_favorite_tips)
            RelativeLayout mNaviFavoriteTips;
            @BindView(R.id.favorite_box)
            FrameLayout mFavoriteBox;

            public FavoriteHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }

            public void updateFavoriteList() {
                //获取收藏地址
                mNaviDao.getFavorList(favoriteList, favoriteList.size());
                if (mFavoriteRecyclerView.getVisibility() == View.VISIBLE) {
                    mFavoriteRecyclerView.setHasFixedSize(true);
                    mFavoriteRecyclerView.setLayoutManager(new FullyLinearLayoutManager(SetFavoriteMapActivity.this));
                    mFavoriteRecyclerView.setAdapter(new FavoriteListAdapter());
                }

            }

            @OnClick({R.id.navi_favorite_list})
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.navi_favorite_list:
                        if (mFavoriteBox.getVisibility() == View.GONE) {
                            mFavoriteBox.setVisibility(View.VISIBLE);
                            mImageMoreDown.setVisibility(View.GONE);
                            mImageMoreUp.setVisibility(View.VISIBLE);
                            if (mFavoriteRecyclerView.getVisibility() == View.GONE) {
                                if (favoriteList != null && favoriteList.size() != 0) {
                                    mFavoriteRecyclerView.setVisibility(View.VISIBLE);
                                    updateFavoriteList();
                                } else {
                                    mNaviFavoriteTips.setVisibility(View.VISIBLE);
                                }
                            } else {
                                mFavoriteRecyclerView.setVisibility(View.GONE);
                                mNaviFavoriteTips.setVisibility(View.GONE);
                            }
                        } else {
                            mFavoriteBox.setVisibility(View.GONE);
                            mFavoriteRecyclerView.setVisibility(View.GONE);
                            mNaviFavoriteTips.setVisibility(View.GONE);
                            mImageMoreDown.setVisibility(View.VISIBLE);
                            mImageMoreUp.setVisibility(View.GONE);
                        }
                        break;
                }
            }
        }

        class HistoryHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.history_location_item)
            RelativeLayout mHistoryLocationItem;
            @BindView(R.id.location_item_name_text)
            TextView mLocationItemName;
            @BindView(R.id.location_item_address_text)
            TextView mLocationItemAddress;

            public HistoryHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }

            @OnClick(R.id.history_location_item)
            public void onItemClick() {
                int index = (int) mHistoryLocationItem.getTag();
                BaiduAddress address = historyList.get(index);
                mAmosfSearchEdit.setText(address.getName());
            }
        }

        class ClearHistoryHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.navi_record_list_box)
            RelativeLayout mNaviRecordListBox;
            @BindView(R.id.navi_clear_record_list_bt)
            TextView mNaviClearRecordBt;

            public ClearHistoryHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }

            public void updateClearHistoryBox() {
                mNaviDao.getHistoryList(historyList, historyList.size());
                if (historyList.size() > 0) {
                    mNaviRecordListBox.setVisibility(View.VISIBLE);
                } else {
                    mNaviRecordListBox.setVisibility(View.GONE);
                }
            }

            public void cleanHistory() {
                mNaviDao.removeHistoryAddress();
                updateClearHistoryBox();
            }

            @OnClick({R.id.navi_record_list_box})
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.navi_record_list_box:
                        final CommonDeleteDialog commonDeleteDialog = new CommonDeleteDialog(SetFavoriteMapActivity.this, "清空", "确定要清空历史记录吗", "取消", "清空");
                        commonDeleteDialog.setOnConfirmListener(new CommonDeleteDialog.OnConfirmListener() {
                            @Override
                            public void onConfirm(String text) {
                                cleanHistory();
                                commonDeleteDialog.cancel();
                            }
                        }).show();
                        break;
                }
            }
        }

    }

    public class FavoriteListAdapter extends RecyclerView.Adapter {
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = mInflater.inflate(R.layout.location_search_item_favorite, null, false);
            RecyclerView.ViewHolder viewHolder = new FavoriteItemHolder(itemView);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof FavoriteItemHolder) {
                if (favoriteList != null && favoriteList.size() != 0) {
                    BaiduAddress address = favoriteList.get(position);
                    ((FavoriteItemHolder) holder).mFavoriteItem.setTag(position);
                    if (address != null) {
                        ((FavoriteItemHolder) holder).mLocationItemFavoriteName.setText(address.getName());
                        ((FavoriteItemHolder) holder).mLocationItemFavoriteAddress.setText(address.getAddress());
                    }
                }

            }

        }

        @Override
        public int getItemCount() {
            mNaviDao.getFavorList(favoriteList, favoriteList.size());
            return favoriteList.size();
        }

        class FavoriteItemHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.favorite_item)
            RelativeLayout mFavoriteItem;
            @BindView(R.id.loc_item_favorite_name_text)
            TextView mLocationItemFavoriteName;
            @BindView(R.id.loc_item_favorite_address_text)
            TextView mLocationItemFavoriteAddress;

            public FavoriteItemHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }

            @OnClick({R.id.favorite_item})
            public void onClick(View view) {
                int index = (int) mFavoriteItem.getTag();
                BaiduAddress address = favoriteList.get(index);
                //选择收藏地址的时候，直接将收藏地址设置目标地址
                switch (mode) {
                    case SET_HOME:
                        BaiduAddress homeAddr = new BaiduAddress();
                        mNaviDao.removeHomeOrCompany("家");
                        homeAddr.reset(address);
                        homeAddr.setRemark("家");
                        homeAddr.setId(null);
                        homeAddr.setFavoritedTime(new Date());
                        mNaviDao.insertAddress(homeAddr);
                        updateCode = NaviSetPointPresenter.Msg.UPDATE_HOME;
                        mNaviDao.sync();
                        onFinish();
                        break;
                    case SET_COMPANY:
                        BaiduAddress companyAddr = new BaiduAddress();
                        mNaviDao.removeHomeOrCompany("单位");
                        companyAddr.reset(address);
                        companyAddr.setRemark("单位");
                        companyAddr.setId(null);
                        companyAddr.setFavoritedTime(new Date());
                        mNaviDao.insertAddress(companyAddr);
                        updateCode = NaviSetPointPresenter.Msg.UPDATE_COMPANY;
                        mNaviDao.sync();
                        onFinish();
                        break;
                    case SET_START:
                        BaiduAddress startAddr = new BaiduAddress();
                        mNaviDao.removeHomeOrCompany("出发地");
                        startAddr.reset(address);
                        startAddr.setRemark("出发地");
                        startAddr.setId(null);
                        startAddr.setFavoritedTime(null);
                        mNaviDao.insertAddress(startAddr);
                        mAppConfig.startAddress = startAddr;
                        updateCode = NaviSetPointPresenter.Msg.UPDATE_START;
                        onFinish();
                        break;
                    case SET_END:
                        BaiduAddress endAddr = new BaiduAddress();
                        mNaviDao.removeHomeOrCompany("目的地");
                        endAddr.reset(address);
                        endAddr.setRemark("目的地");
                        endAddr.setId(null);
                        endAddr.setFavoritedTime(null);
                        mNaviDao.insertAddress(endAddr);
                        mAppConfig.endAddress = endAddr;
                        updateCode = NaviSetPointPresenter.Msg.UPDATE_END;
                        onFinish();
                        break;
                }
            }
        }
    }
}
