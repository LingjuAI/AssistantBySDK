package com.lingju.assistant.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.LingjuSwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.activity.index.ITrafficShow;
import com.lingju.assistant.activity.index.presenter.TrafficShowPresenter;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.assistant.view.DrawForExpandLayout;
import com.lingju.assistant.view.LocationSuggestListView;
import com.lingju.assistant.view.NaviVoiceInputDialog;
import com.lingju.assistant.view.RealTimeUpdateSearchBox;
import com.lingju.model.BaiduAddress;
import com.lingju.model.dao.BaiduNaviDao;
import com.lingju.common.log.Log;
import com.lingju.util.NetUtil;
import com.lingju.util.ScreenUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2016/12/21.
 */
public class TrafficShowActivity extends GoBackActivity implements ITrafficShow.INaviSetPointView {
    private final static String TAG = "TrafficShowActivity";


    //    @BindView(R.id.ast_go_home_bt)
    //    TextView goHomeText;
    //    @BindView(R.id.ast_go_company_bt)
    //    TextView goCompanyText;
    @BindView(R.id.ast_search_bt)
    TextView searchBt;
    //    @BindView(R.id.ast_search_voice_bt)
    //    ImageButton searchVoiceBt;
    @BindView(R.id.ast_search_edit)
    RealTimeUpdateSearchBox edit;
    @BindView(R.id.ast_suggest_list)
    LocationSuggestListView suggestListView;
    @BindView(R.id.ast_poi_box)
    DrawForExpandLayout poiListBox;
    @BindView(R.id.ast_poi_list_box)
    LingjuSwipeRefreshLayout refreshLayout;
    @BindView(R.id.ast_poi_list)
    RecyclerView poiListView;
    @BindView(R.id.ast_poi_detail_pager)
    ViewPager poiDetailPager;
    @BindView(R.id.ast_poi_detial_dot_list)
    ViewGroup poiDetialDotList;
    @BindView(R.id.ast_poi_detail_box)
    ViewGroup poiDetialBox;
    //    @BindView(R.id.ast_go_where_box)
    //    LinearLayout goWhereBox;
    @BindView(R.id.ast_map)
    MapView mapView;
    @BindView(R.id.status_bar)
    View mStatusBar;

    ITrafficShow.IPresenter mPresenter;
    private BaiduMap baiduMap;
    private BaiduNaviDao mNaviDao;
    private BaiduAddress homeAddress;
    private BaiduAddress companyAddress;
    private NaviVoiceInputDialog voiceDialog;
    private int updateCode = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_traffic);
        /** 必须写在setContentView()之后 **/
        ButterKnife.bind(this);
        initView();
        mPresenter = new TrafficShowPresenter(this, mapView);
        mPresenter.initData();


    }

    private void initView() {
        //设置模拟状态栏的高度
        ViewGroup.LayoutParams layoutParams = mStatusBar.getLayoutParams();
        layoutParams.height = ScreenUtil.getStatusBarHeight(this);
        mStatusBar.setLayoutParams(layoutParams);

        baiduMap = mapView.getMap();
        /* 是否显示比例尺控件 */
        mapView.showScaleControl(true);
        /* 是否显示缩放控件 */
        mapView.showZoomControls(false);
        baiduMap = mapView.getMap();
        /* 是否允许定位图层 */
        baiduMap.setMyLocationEnabled(true);
        /* 设置缩放级别，改变地图状态 */
        baiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(17f));
        mNaviDao = BaiduNaviDao.getInstance();
        /* 获取家和单位地址 */
        homeAddress = mNaviDao.getHomeOrCompanyAddress(getResources().getString(R.string.home));
        companyAddress = mNaviDao.getHomeOrCompanyAddress(getResources().getString(R.string.company));
    }

    @Override
    protected void onResume() {
        mapView.onResume();
        super.onResume();
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        getWindow().setAttributes(params);
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    public void finish() {
        Log.i(TAG, "finish......");
        setResult(updateCode);
        super.finish();
    }

    @Override
    protected void onDestroy() {
        mPresenter.destroy();
        super.onDestroy();
    }

    @Override
    public void setRefreshLayout(boolean b) {
        refreshLayout.setRefreshing(b);
    }

    @Override
    public LingjuSwipeRefreshLayout getRefreshLayout() {
        return refreshLayout;
    }

    @Override
    public DrawForExpandLayout getPoiListBox() {
        return poiListBox;
    }

    @Override
    public ViewGroup getPoiDetialBox() {
        return poiDetialBox;
    }

    @Override
    public ViewPager getPoiDetailPager() {
        return poiDetailPager;
    }

    @Override
    public ViewGroup getPoiDetialDotList() {
        return poiDetialDotList;
    }

    @Override
    public LocationSuggestListView getSuggestListView() {
        return suggestListView;
    }

    @Override
    public RealTimeUpdateSearchBox getedit() {
        return edit;
    }

    //  @Override
    //    public TextView getGoHomeText(){
    //        return goHomeText;
    //    }
    //    @Override
    //    public TextView getGoCompanyText(){
    //        return goCompanyText;
    //    }
    //    @Override
    //    public LinearLayout getGoWhereBox(){
    //        return goWhereBox;
    //    }
    @Override
    public RecyclerView getPoiListView() {
        return poiListView;
    }

    @Override
    public NaviVoiceInputDialog getVoiceDialog() {
        return voiceDialog;
    }

    @Override
    public TextView getSearchBt() {
        return searchBt;
    }

    @Override
    public void showSnackBar(String s) {
        Snackbar.make(poiListView,s, Snackbar.LENGTH_SHORT).show();
    }
//    @Override
//    public ImageButton getSearchVoiceBt(){
//        return searchVoiceBt;
//    }
    //    @Override
    //    public ImageButton getSearchVoiceBt(){
    //        return searchVoiceBt;
    //    }


    @OnClick({R.id.ast_search_bt, R.id.ast_back_bt, R.id.ast_map_its_bt,/*R.id.ast_relocation_bt,*/R.id.ast_map_locate_bt,
            R.id.ast_zoom_in_bt, R.id.ast_zoom_out_bt, R.id.ast_poi_detial_next_bt, R.id.ast_poi_detial_pre_bt,
           /* R.id.ast_go_home_bt,R.id.ast_go_company_bt,R.id.ast_search_voice_bt*/
    })
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ast_search_bt:        //搜索
                if(NetUtil.getInstance(TrafficShowActivity.this).getCurrentNetType().equals(NetUtil.NetType.NETWORK_TYPE_NONE)&&!TextUtils.isEmpty(edit.getText())){
                    if (getCurrentFocus() != null) {
                        //隐藏输入键盘
                        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                                .hideSoftInputFromWindow(getCurrentFocus()
                                                .getWindowToken(),
                                        InputMethodManager.HIDE_NOT_ALWAYS);
                    }
                    //弹出没有网络的提示
                    final CommonDialog commonDialog =  new CommonDialog(TrafficShowActivity.this,"网络错误","网络状态不佳，请检查网络设置","确定");
                    commonDialog.setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                        @Override
                        public void onConfirm() {
                            commonDialog.cancel();
                        }
                    }).show();
                } else if(!TextUtils.isEmpty(edit.getText())){
                    mPresenter.goSearch();
                }
//                if (getCurrentFocus() != null) {
//                    ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
//                            .hideSoftInputFromWindow(getCurrentFocus()
//                                            .getWindowToken(),
//                                    InputMethodManager.HIDE_NOT_ALWAYS);
//                }
//                mPresenter.goSearch();
                break;
            case R.id.ast_back_bt:
                goBack();
                break;
            case R.id.ast_map_its_bt:       //地图路况图标
                if (baiduMap.isTrafficEnabled()) {
                    baiduMap.setTrafficEnabled(false);
                    ((ImageButton) v).setImageResource(R.drawable.bnav_common_ic_map_its_off);
                } else {
                    baiduMap.setTrafficEnabled(true);
                    ((ImageButton) v).setImageResource(R.drawable.bnav_common_ic_map_its_on);
                }
                break;
            // case R.id.ast_relocation_bt:
            case R.id.ast_map_locate_bt:    //定位图标
                mPresenter.locateManagerStart();
                mPresenter.location();
                //baiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(new LatLng(location.getLatitude(),location.getLongitude())));
                break;
            case R.id.ast_zoom_in_bt:
                baiduMap.setMapStatus(MapStatusUpdateFactory.zoomIn());
                break;
            case R.id.ast_zoom_out_bt:
                baiduMap.setMapStatus(MapStatusUpdateFactory.zoomOut());
                break;
            case R.id.ast_poi_detial_next_bt:
                mPresenter.setPoiDetailPagerToNext();
                break;
            case R.id.ast_poi_detial_pre_bt:
                mPresenter.setPoiDetailPagerToPre();
                break;
            //            case R.id.ast_go_home_bt: {
            ////                if (goHomeNodes.size() == 0)
            ////                    return;
            //                if(homeAddress==null){
            //                    return;
            //                }
            //                Intent intent = new Intent(TrafficShowActivity.this, NaviSetLineActivity.class);
            //                intent.putExtra("latitude", homeAddress.getLatitude());
            //                intent.putExtra("longitude", homeAddress.getLongitude());
            //                intent.putExtra("address", homeAddress.getName());
            //                startActivity(intent);
            //                break;
            //            }
            //            case R.id.ast_go_company_bt: {
            ////                if (goCompanyNodes.size() == 0)
            ////                    return;
            //                if(companyAddress==null){
            //                    return;
            //                }
            //                Intent intent = new Intent(TrafficShowActivity.this, NaviSetLineActivity.class);
            //                intent.putExtra("latitude", companyAddress.getLatitude());
            //                intent.putExtra("longitude", companyAddress.getLongitude());
            //                intent.putExtra("address", companyAddress.getName());
            //                startActivity(intent);
            //                break;
            //            }
            ////                case R.id.ast_relocation_bt:
            ////                    BaiduLocateManager.get().start();
            ////                    break;
            //            case R.id.ast_search_voice_bt:
            //                if (voiceDialog == null) {
            //                    voiceDialog = new NaviVoiceInputDialog(TrafficShowActivity.this);
            //                }
            //                voiceDialog.show();
            //                break;
        }
    }


}


