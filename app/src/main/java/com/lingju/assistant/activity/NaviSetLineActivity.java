package com.lingju.assistant.activity;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.event.MapCmdEvent;
import com.lingju.assistant.activity.event.NaviRouteCalculateEvent;
import com.lingju.assistant.activity.event.NaviSwitchRouteLineEvent;
import com.lingju.assistant.activity.event.NavigateEvent;
import com.lingju.assistant.activity.event.NetWorkEvent;
import com.lingju.assistant.activity.event.RecordUpdateEvent;
import com.lingju.assistant.activity.index.INaviSetLine;
import com.lingju.assistant.activity.index.presenter.NaviSetLinePresenter;
import com.lingju.assistant.baidunavi.adapter.BaiduNaviSuperManager;
import com.lingju.assistant.entity.RobotConstant;
import com.lingju.assistant.view.CommonAlertDialog;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.assistant.view.RoutePreferenceDialog;
import com.lingju.assistant.view.VoiceComponent;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.lbsmodule.constant.ArrowIcomMap;
import com.lingju.lbsmodule.proxy.BNavigatorProxy;
import com.lingju.lbsmodule.proxy.RoutePlanModelProxy;
import com.lingju.lbsmodule.proxy.RoutePlanResultItemProxy;
import com.lingju.util.ScreenUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2016/12/22.
 */
public class NaviSetLineActivity extends Activity implements INaviSetLine.INaviSetLineView {

    /**
     * 路线规划状态：未规划
     **/
    public final static int UN_CALCULATE = -1;
    /**
     * 路线规划状态：正在规划
     **/
    public final static int CALCULATING = 0;
    /**
     * 路线规划状态：已规划
     **/
    public final static int CALCULATED = 1;
    /**
     * 路线规划状态：已规划并显示
     **/
    public final static int CALCULATED_SHOW = 2;

    public final static int MSG_INIT_MAPVIEW = 0;
    public final static int MSG_START_NAV = 10;     //倒计时结束后开始导航
    public final static int MSG_UPDATE_COUNTDOWN = 11;
    public final static int MSG_START_NAVI = 1001;    //调用百度导航API，正式开始导航
    public final static String CALCULATE_MODE = "calculate_mode";
    public final static String CALCULATE_ROAD = "calculate_road";
    public final static String NAVI_ROUTE_CALCULATED = "导航路线规划完成";

    @BindView(R.id.ansl_count_down_text2)
    TextView mAnslCountDownText2;
    @BindView(R.id.ansl_map_scale_text)
    TextView mAnslMapScaleText;
    @BindView(R.id.ansl_route_mode1_distance)
    TextView mAnslRouteMode1Distance;
    @BindView(R.id.ansl_route_mode1_time)
    TextView mAnslRouteMode1Time;
    @BindView(R.id.ansl_route_mode1_toll)
    TextView mAnslRouteMode1Toll;
    @BindView(R.id.ansl_route_mode1)
    LinearLayout mAnslRouteMode1;
    @BindView(R.id.ansl_route_mode2_distance)
    TextView mAnslRouteMode2Distance;
    @BindView(R.id.ansl_route_mode2_time)
    TextView mAnslRouteMode2Time;
    @BindView(R.id.ansl_route_mode2_toll)
    TextView mAnslRouteMode2Toll;
    @BindView(R.id.ansl_route_mode2)
    LinearLayout mAnslRouteMode2;
    @BindView(R.id.ansl_route_mode3_distance)
    TextView mAnslRouteMode3Distance;
    @BindView(R.id.ansl_route_mode3_time)
    TextView mAnslRouteMode3Time;
    @BindView(R.id.ansl_route_mode3_toll)
    TextView mAnslRouteMode3Toll;
    @BindView(R.id.ansl_route_mode3)
    LinearLayout mAnslRouteMode3;
    @BindView(R.id.ansl_route_detail_box)
    LinearLayout mAnslRouteDetailBox;
    @BindView(R.id.ansl_route_detail_light_toll_text)
    TextView mAnslRouteDetailLightTollText;
    @BindView(R.id.ansl_route_detail_way_text)
    TextView mAnslRouteDetailWayText;
    @BindView(R.id.ansl_route_detail_list)
    RecyclerView mAnslRouteDetailList;
    @BindView(R.id.ansl_route_start_in_virtaul_bt)
    TextView mAnslRouteStartInVirtaulBt;
    @BindView(R.id.ansl_route_detail_list_box)
    LinearLayout mAnslRouteDetailListBox;
    @BindView(R.id.ansl_count_down_text)
    TextView mAnslCountDownText;
    @BindView(R.id.ansl_out_bottom_box)
    LinearLayout mAnslOutBottomBox;
    @BindView(R.id.ansl_route_detail_bottom_box)
    LinearLayout mAnslRouteDetailBottomBox;
    @BindView(R.id.ansl_route_line_box)
    RelativeLayout mAnslRouteLineBox;
    @BindView(R.id.ansl_voice_bt)
    VoiceComponent mAnslVoiceBt;
    @BindView(R.id.ansl_map_box)
    FrameLayout mAnslMapBox;
    @BindView(R.id.ansl_map_its_bt)
    ImageButton mAnslMapItsBt;
    @BindView(R.id.ansl_start_out_bt)
    TextView mAnslStartOutBt;
    @BindView(R.id.status_bar)
    View mStatusBar;

    private INaviSetLine.IPresenter mPresenter;
    private RouteDetailAdapter mListAdapter;
    private CommonAlertDialog progressDialog;
    private int[][] routeModeTextIds;
    private int height;     //当前页面高度
    private int startHeight;    //页面底部导航模型区域+导航按钮视图初始高度
    private RoutePreferenceDialog mPreferenceDialog;

    enum ShowState {
        showing, showed, unshow, unshowing
    }

    /**
     * 导航路线详情列表状态。默认未显示
     **/
    private ShowState showState = ShowState.unshow;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navi_set_line);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        mPresenter = new NaviSetLinePresenter(this);
        mPresenter.initData(getIntent(), mHandler);
    }

    @Override
    protected void onStop() {
        mPresenter.onStop();
        super.onStop();
    }

    @Override
    protected void onResume() {
        mPresenter.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mPresenter.onPause();
        super.onPause();
    }

    public void backPressed() {
        mPresenter.backPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            backPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        mPresenter.onConfigurationChanged(newConfig);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        mPresenter.onDestroy();
        super.onDestroy();
    }

    @OnClick({R.id.ansl_back_bt, R.id.ansl_setting_bt, R.id.ansl_map_park_bt, R.id.ansl_map_locate_bt, R.id.ansl_map_its_bt,
            R.id.ansl_zoom_in_bt, R.id.ansl_zoom_out_bt, R.id.ansl_lock_bt, R.id.ansl_route_start_in_virtaul_bt,
            R.id.ansl_route_start_in_bt, R.id.ansl_start_out_bt, R.id.ansl_route_mode1, R.id.ansl_route_mode2,
            R.id.ansl_route_mode3, R.id.ansl_count_down_text})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ansl_back_bt:
                backPressed();
                break;
            case R.id.ansl_setting_bt:
                mPresenter.cancelCountDownTimer();
                SynthesizerBase.get().stopSpeakingAbsolte();
                mPreferenceDialog = new RoutePreferenceDialog(this, mPresenter.getPreference());
                mPreferenceDialog.setOnDialogListener(preferenceDialogListener);
                mPreferenceDialog.show();
                break;
            case R.id.ansl_map_park_bt:
                /* 百度导航提供的方法无效，暂不实现 */
                break;
            case R.id.ansl_map_locate_bt:
                mPresenter.location();
                break;
            case R.id.ansl_map_its_bt:
                mPresenter.showTraffic();
                break;
            case R.id.ansl_zoom_in_bt:
                mPresenter.MapZoomIn();
                break;
            case R.id.ansl_zoom_out_bt:
                mPresenter.MapZoomOut();
                break;
            case R.id.ansl_lock_bt:     //显示/隐藏路线详情图标
                mPresenter.cancelCountDownTimer();
                switchShowRouteLineDetail();
                break;
            case R.id.ansl_route_start_in_virtaul_bt:      //路线详情区域模拟导航按钮
            case R.id.ansl_count_down_text:     //页面底部模拟导航按钮
                mPresenter.cancelCountDownTimer();
                mPresenter.startNavi(this, false);
                break;
            case R.id.ansl_route_start_in_bt:       //路线详情区域开始导航按钮
            case R.id.ansl_start_out_bt:        //页面底部开始导航按钮
                mPresenter.cancelCountDownTimer();
                mPresenter.startNavi(this, true);
                break;
            case R.id.ansl_route_mode1:
                mPresenter.setRouteModeDetail(0);
                break;
            case R.id.ansl_route_mode2:
                mPresenter.setRouteModeDetail(1);
                break;
            case R.id.ansl_route_mode3:
                mPresenter.setRouteModeDetail(2);
                break;
        }
    }

    /**
     * 话筒状态更新处理
     **/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRecordUpdateEvent(RecordUpdateEvent e) {
        switch (e.getState()) {
            case RecordUpdateEvent.RECORD_IDLE:
            case RecordUpdateEvent.RECORD_IDLE_AFTER_RECOGNIZED:
                mAnslVoiceBt.setRecordIdleState();
                break;
            case RecordUpdateEvent.RECORDING:
                mAnslVoiceBt.setRecordStartState();
                break;
            case RecordUpdateEvent.RECOGNIZING:
                mAnslVoiceBt.setRecognizeCompletedState();
                break;
        }
    }

    /**
     * 导航相关指令处理
     **/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNetWorkEvent(NetWorkEvent event) {
        if(event.getType()==0){
            closeLoading();
           //  showSnackBar("网络状态不佳，请检查网络设置");
           // Toast.makeText(NaviSetLineActivity.this,"网络状态不佳，请检查网络设置",Toast.LENGTH_LONG).show();
            final CommonDialog commonDialog =  new CommonDialog(this,"网络错误","网络状态不佳，请检查网络设置","确定");
            commonDialog.setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                @Override
                public void onConfirm() {
                    commonDialog.cancel();
                }
            }).show();
            return;

        }

    }
    /**
     * 导航相关指令处理
     **/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNaviEvent(NavigateEvent event) {
        switch (event.getType()) {
            case NavigateEvent.START_NAVI:
                if (!BNavigatorProxy.getInstance().isNaviBegin()) {
                    mPresenter.cancelCountDownTimer();
                    mPresenter.startNavi(this, true);
                }
                break;
            case NavigateEvent.PAUSE_NAVI:
                BNavigatorProxy.getInstance().pause();
                mPresenter.setNaviStatus(RobotConstant.NaviStatus.PAUSE.toString());
                break;
            case NavigateEvent.RESUME_NAVI:
                BNavigatorProxy.getInstance().resume();
                mPresenter.setNaviStatus(RobotConstant.NaviStatus.OPEN.toString());
                break;
            case NavigateEvent.STOP_NAVI:
                if (BNavigatorProxy.getInstance().isNaviBegin()) {
                    mPresenter.checkPassedPassPoint();
                    BNavigatorProxy.getInstance().forceQuitWithoutDialog();
                } else
                    goBack();
                break;
            case NavigateEvent.NAVI_SHOW_FULL_LINE:     //查看全程
                mPresenter.showFullLine();
                break;
            case NavigateEvent.RESUME_TO_START_COUNTDOWN:       //重新倒数10秒
                if (!BNavigatorProxy.getInstance().isNaviBegin()) {
                    mPresenter.stopCountDown();
                    mPresenter.startCountDown();
                    //更新导航引擎状态
                    mPresenter.setNaviStatus(RobotConstant.NaviStatus.TIME_CONTINUE.toString());
                }
                break;
            case NavigateEvent.STOP_COUNTDOWN:      //停止倒数
                mPresenter.cancelCountDownTimer();
                break;
            case NavigateEvent.STOP_NAVI_BACKGROUND:    //设置在后台停止导航标记
                mPresenter.setStopInBackGround(BNavigatorProxy.getInstance().isNaviBegin());
                break;
            case NavigateEvent.SHOW_NAVI_GUIDE:     //显示导航引导路线
                mPresenter.showNaviGuide();
                break;
            case NavigateEvent.START_AWAKEN:
                mPresenter.setWakeupFlag();
                break;
        }
    }

    @Override
    public void goBack() {
        finish();
        overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
    }

    /**
     * 导航路线计算事件处理
     **/
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRouteCalEvent(NaviRouteCalculateEvent event) {
        mPresenter.routeCalculate(event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSwitchLineEvent(NaviSwitchRouteLineEvent event) {
        mPresenter.switchRouteLine(event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMapCmdEvent(MapCmdEvent e) {
        mPresenter.handleMapCmd(e);
    }

    @Override
    public void initView() {
        //设置模拟状态栏的高度
        ViewGroup.LayoutParams layoutParams = mStatusBar.getLayoutParams();
        layoutParams.height = ScreenUtil.getStatusBarHeight(this);
        mStatusBar.setLayoutParams(layoutParams);

        /* 初始化指定路线行驶列表参数 */
        mListAdapter = new RouteDetailAdapter();
        mAnslRouteDetailList.setHasFixedSize(true);
        mAnslRouteDetailList.setLayoutManager(new LinearLayoutManager(this));
        mAnslRouteDetailList.setAdapter(mListAdapter);

        mAnslVoiceBt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mPresenter.cancelCountDownTimer();
                    return true;
                }
                return false;
            }
        });

        /* 填充路线模型信息视图id数组 */
        routeModeTextIds = new int[][]{{R.id.ansl_route_mode1_distance, R.id.ansl_route_mode1_time, R.id.ansl_route_mode1_toll,R.id.route_mode1},
                {R.id.ansl_route_mode2_distance, R.id.ansl_route_mode2_time, R.id.ansl_route_mode2_toll,R.id.route_mode2},
                {R.id.ansl_route_mode3_distance, R.id.ansl_route_mode3_time, R.id.ansl_route_mode3_toll,R.id.route_mode3}};
    }

    @Override
    public void setMapView(View mapView, boolean isAdd) {
        if (isAdd)
            mAnslMapBox.addView(mapView, 0, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        else
            mAnslMapBox.removeView(mapView);
    }

    @Override
    public void setTitle(int second) {
        mAnslCountDownText2.setText(second + "秒后开始导航");
    }

    @Override
    public void showLoading(String title, String message) {
        if (null == progressDialog) {
            progressDialog = new CommonAlertDialog(this, title, message);
        }
        progressDialog.setOnCancelListener(new CommonAlertDialog.OnCancelListener() {
            @Override
            public void onCancel() {
                mPresenter.backPressed();
            }
        }).show();
    }
    @Override
    public boolean progressDialogisShow(){
        if(progressDialog.isShowing()){
            return  true;
        }else {
            return false;
        }
    }
    @Override
    public void closeLoading() {
        if (null != progressDialog && progressDialog.isShowing())
            progressDialog.cancel();
    }

    @Override
    public void setRouteLineModeText(RoutePlanModelProxy routeMode, int index) {
        switch (index) {
            case 0:     //路线一
                mAnslRouteMode1Distance.setText(routeMode.getDistance());
                mAnslRouteMode1Time.setText(routeMode.getTotalTime());
                mAnslRouteMode1Toll.setText(routeMode.getTollFees() == 0 ? "无收费" : "收费" + routeMode.getTollFees() + "元");
                break;
            case 1:     //路线二
                if (routeMode != null) {
                    mAnslRouteMode2.setVisibility(View.VISIBLE);
                    mAnslRouteMode2Distance.setText(routeMode.getDistance());
                    mAnslRouteMode2Time.setText(routeMode.getTotalTime());
                    mAnslRouteMode2Toll.setText(routeMode.getTollFees() == 0 ? "无收费" : "收费" + routeMode.getTollFees() + "元");
                } else {
                    mAnslRouteMode2.setVisibility(View.GONE);
                }
                break;
            case 2:     //路线三
                if (routeMode != null) {
                    mAnslRouteMode3.setVisibility(View.VISIBLE);
                    mAnslRouteMode3Distance.setText(routeMode.getDistance());
                    mAnslRouteMode3Time.setText(routeMode.getTotalTime());
                    mAnslRouteMode3Toll.setText(routeMode.getTollFees() == 0 ? "无收费" : "收费" + routeMode.getTollFees() + "元");
                } else {
                    mAnslRouteMode3.setVisibility(View.GONE);
                }
                break;
        }
    }

    @Override
    public void setRouteLineModeStyle(int index, boolean selected) {
        if (selected) {
            ((TextView) findViewById(routeModeTextIds[index][0])).setTextColor(getResources().getColorStateList(R.color.white));
            ((TextView) findViewById(routeModeTextIds[index][1])).setTextColor(getResources().getColorStateList(R.color.white));
            ((TextView) findViewById(routeModeTextIds[index][2])).setTextColor(getResources().getColorStateList(R.color.white));
            findViewById(routeModeTextIds[index][3]).setBackgroundResource(R.drawable.account_btn);
        } else {
            ((TextView) findViewById(routeModeTextIds[index][0])).setTextColor(getResources().getColorStateList(R.color.new_text_color_first));
            ((TextView) findViewById(routeModeTextIds[index][1])).setTextColor(getResources().getColorStateList(R.color.new_text_color_second));
            ((TextView) findViewById(routeModeTextIds[index][2])).setTextColor(getResources().getColorStateList(R.color.new_text_color_second));
            findViewById(routeModeTextIds[index][3]).setBackgroundColor(getResources().getColor(R.color.white));
        }
    }

    @Override
    public void setVirtualNaviText(String text) {
        mAnslCountDownText.setText(text);
    }

    @Override
    public void setLineMsg(RoutePlanModelProxy routeMode) {
        mAnslRouteDetailLightTollText.setText(
                new StringBuilder().append("红绿灯").append(routeMode.getTrafficLightCnt()).append("个,")
                        .append(routeMode.getTollFees() == 0 ? "无收费" : "收费："
                                + routeMode.getTollFees() + "元").toString());
        mAnslRouteDetailWayText.setText("途径：" + routeMode.getMainRoads());
    }

    @Override
    public void updateRouteDetailList(List<RoutePlanResultItemProxy> datas) {
        mListAdapter.setDatas(datas);
    }

    @Override
    public void showRouteLineBox(boolean show) {
        mAnslRouteLineBox.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showVoiceBtn(boolean show) {
        mAnslVoiceBt.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setVoiceBtnCenter() {
        FrameLayout.LayoutParams layoutParam = (FrameLayout.LayoutParams) mAnslVoiceBt.getLayoutParams();
        layoutParam.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        layoutParam.bottomMargin = 0;
        layoutParam.rightMargin = 0;
    }

    @Override
    public void setTrafficIcon(boolean isShow) {
        mAnslMapItsBt.setImageResource(isShow ? R.drawable.bnav_common_ic_map_its_on : R.drawable.bnav_common_ic_map_its_off);
    }

    @Override
    public void switchShowRouteLineDetail() {
        if (showState == ShowState.showing || showState == ShowState.unshowing)
            return;
        height = mAnslMapBox.getHeight();

        if (showState == ShowState.unshow) {
            if (showState == ShowState.showed)
                return;
            mAnslOutBottomBox.setVisibility(View.GONE);
            mAnslVoiceBt.setVisibility(View.GONE);
            findViewById(R.id.ansl_route_detail_list_box).setVisibility(View.VISIBLE);
            startHeight = mAnslRouteLineBox.getHeight();
            mAnslRouteLineBox.getLayoutParams().height = startHeight;
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mAnslRouteDetailBottomBox.getLayoutParams();
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            layoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT;
        }

        mAnimateToTargetPosition.reset();
        mAnimateToTargetPosition.setInterpolator(new AccelerateInterpolator());
        mAnimateToTargetPosition.setDuration(300);
        mAnimateToTargetPosition.setAnimationListener(animationListener);
        mAnslRouteDetailBottomBox.startAnimation(mAnimateToTargetPosition);
    }


    @Override
    public void setVoiceBtnIdleState() {
        mAnslVoiceBt.setRecordIdleState();
    }

    @Override
    public void showSnackBar(String s) {
        Snackbar.make(mAnslMapBox,s, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * 自定义动画
     **/
    private final Animation mAnimateToTargetPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveToTarget(interpolatedTime);
        }
    };

    private void moveToTarget(float interpolatedTime) {
        int offset = (int) ((height - startHeight) * interpolatedTime);
        if (showState == ShowState.showing) {
            mAnslRouteLineBox.getLayoutParams().height = startHeight + offset;
            mAnslRouteLineBox.requestLayout();
        } else if (showState == ShowState.unshowing) {
            mAnslRouteLineBox.getLayoutParams().height = height - offset;
            mAnslRouteLineBox.requestLayout();
        }
    }

    /**
     * 动画监听器
     **/
    private final Animation.AnimationListener animationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            if (showState == ShowState.unshow) {
                showState = ShowState.showing;
            } else if (showState == ShowState.showed) {
                showState = ShowState.unshowing;
            }
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (showState == ShowState.showing) {
                showState = ShowState.showed;
                mAnslRouteLineBox.setBackgroundResource(R.drawable.bnav_locker_pull_down);
            } else if (showState == ShowState.unshowing) {
                showState = ShowState.unshow;
                mAnslOutBottomBox.setVisibility(View.VISIBLE);
                mAnslVoiceBt.setVisibility(View.VISIBLE);
                mAnslRouteDetailListBox.setVisibility(View.GONE);
                mAnslRouteLineBox.setBackgroundResource(R.drawable.bnav_locker_pull_up);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    };

    private RoutePreferenceDialog.OnDialogClickListener preferenceDialogListener = new RoutePreferenceDialog.OnDialogClickListener() {
        @Override
        public void onClickBack() {
            if (mPreferenceDialog != null) {
                mPreferenceDialog.dismiss();
                mPreferenceDialog = null;
            }
        }

        @Override
        public void onClickComplete(int preference) {
            mPresenter.reCalculateRoad(preference);
            if (mPreferenceDialog != null) {
                mPreferenceDialog.dismiss();
                mPreferenceDialog = null;
            }
        }
    };

    /**
     * 主线程Handler
     **/
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INIT_MAPVIEW:      //初始化地图
                    mPresenter.initMapView();
                    break;
                case BaiduNaviSuperManager.MSG_GET_ROUTE_LINE:      //获取导航路线
                    mPresenter.fillRouteModel(msg.obj, msg.arg1);
                    break;
                case BaiduNaviSuperManager.MSG_GET_LAST_ROUTE_LINE:     //处理导航路线数据、视图，准备导航
                    mPresenter.initNavi(msg.arg1, msg.arg2);
                    break;
                case MSG_UPDATE_COUNTDOWN:      //10秒倒计时
                    int second = msg.arg1;
                   // setVirtualNaviText(second + "秒");
                    setTitle(second);
                    break;
                case MSG_START_NAV:     //计时完毕，开始导航
                    mPresenter.cancelCountDownTimer();
                    mPresenter.startNavi(NaviSetLineActivity.this, true);
                    break;
                case MSG_START_NAVI:    //调用百度导航API，进入导航视图
                    /* 隐藏非导航视图区域 */
                    mAnslRouteLineBox.setVisibility(View.GONE);
                    findViewById(R.id.ansl_top_bar_box).setVisibility(View.GONE);
                    findViewById(R.id.ansl_left_tool_box).setVisibility(View.GONE);
                    findViewById(R.id.ansl_right_tool_box).setVisibility(View.GONE);
                    //移除话筒背景
                    mAnslVoiceBt.setVoiceBtBackground(0);
                    /* 将话筒移至左下角 */
                    FrameLayout.LayoutParams layoutParam = (FrameLayout.LayoutParams) mAnslVoiceBt.getLayoutParams();
                    layoutParam.gravity = Gravity.LEFT | Gravity.BOTTOM;
                    layoutParam.bottomMargin = ScreenUtil.getInstance().dip2px(120);
                    layoutParam.leftMargin = ScreenUtil.getInstance().dip2px(8);
                    mAnslVoiceBt.setVisibility(View.VISIBLE);
                    mPresenter.hanldeNaviView();
                    break;
            }
        }
    };

    /**
     * 路线行驶详情列表适配器
     **/
    class RouteDetailAdapter extends RecyclerView.Adapter<RouteDetailAdapter.RouteDetailHolder> {

        private List<RoutePlanResultItemProxy> mDatas;

        @Override
        public RouteDetailHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(NaviSetLineActivity.this).inflate(R.layout.route_line_item, parent, false);
            return new RouteDetailHolder(itemView);
        }

        @Override
        public void onBindViewHolder(RouteDetailHolder holder, int position) {
            RoutePlanResultItemProxy routeItem = mDatas.get(position);
            String roadName = routeItem.getNextRoadName();
            roadName.replace(" - ", "行驶");
            String[] roads = roadName.split("进入", 2);
            if (roads.length == 2) {
                holder.mRliItemTextBox.setVisibility(View.VISIBLE);
                holder.mRliItemText.setText(roads[1]);

            } else {
                holder.mRliItemTextBox.setVisibility(View.GONE);
            }
            if (ArrowIcomMap.MAP.containsKey(roads[0])) {
                holder.mRliArrowIcon.setImageResource(ArrowIcomMap.MAP.get(roads[0]));
                if ("目的地".equals(roads[0])) {
                    holder.mRliItemText.setText(roads[0]);
                }
            }

        }

        @Override
        public int getItemCount() {
            return mDatas == null ? 0 : mDatas.size();
        }

        public void setDatas(List<RoutePlanResultItemProxy> datas) {
            mDatas = datas;
            notifyDataSetChanged();
        }

        class RouteDetailHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.rli_arrow_icon)
            ImageButton mRliArrowIcon;
            @BindView(R.id.rli_item_text)
            TextView mRliItemText;
            @BindView(R.id.rli_item_text_box)
            LinearLayout mRliItemTextBox;

            public RouteDetailHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }
        }
    }
}
