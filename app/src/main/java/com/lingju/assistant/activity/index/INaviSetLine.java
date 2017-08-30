package com.lingju.assistant.activity.index;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Handler;
import android.view.View;

import com.lingju.assistant.activity.event.MapCmdEvent;
import com.lingju.assistant.activity.event.NaviRouteCalculateEvent;
import com.lingju.assistant.activity.event.NaviSwitchRouteLineEvent;
import com.lingju.lbsmodule.proxy.RoutePlanModelProxy;
import com.lingju.lbsmodule.proxy.RoutePlanResultItemProxy;

import java.util.List;

/**
 * Created by Ken on 2016/12/23.
 */
public interface INaviSetLine {

    interface INaviSetLineView {
        /**
         * 返回按钮
         */
        void goBack();

        /**
         * 初始化视图
         **/
        void initView();

        /**
         * 将导航路线图层添加到视图中/移除
         **/
        void setMapView(View mapView, boolean isAdd);

        /**
         * 显示路线规划对话框
         **/
        void showLoading(String title, String message);

        /**
         * 销毁对话框
         **/
        void closeLoading();

        /**
         * 设置标题文本
         **/
        void setTitle(int second);

        /**
         * 设置导航路线模型文本
         *
         * @param routeMode 路线信息模型
         * @param index     路线索引
         **/
        void setRouteLineModeText(RoutePlanModelProxy routeMode, int index);

        /**
         * 设置导航路线模型显示风格
         *
         * @param index    路线索引
         * @param selected 是否被选中
         **/
        void setRouteLineModeStyle(int index, boolean selected);

        /**
         * 设置“模拟导航”视图文本
         **/
        void setVirtualNaviText(String text);

        /**
         * 设置指定路线信息(即路线行驶详情列表头部信息)
         **/
        void setLineMsg(RoutePlanModelProxy routeMode);

        /**
         * 填充列表数据，并刷新视图
         **/
        void updateRouteDetailList(List<RoutePlanResultItemProxy> datas);

        /**
         * 显示/隐藏导航路线模型信息区域视图
         **/
        void showRouteLineBox(boolean show);

        /**
         * 显示/隐藏话筒
         **/
        void showVoiceBtn(boolean show);

        /** 设置话筒底部居中 **/
        void setVoiceBtnCenter();

        /**
         * 设置路况图标样式
         **/
        void setTrafficIcon(boolean isShow);

        /**
         * 显示/隐藏导航路线详情列表视图
         **/
        void switchShowRouteLineDetail();

        /**
         * 设置话筒空闲状态
         **/
        void setVoiceBtnIdleState();

        void showSnackBar(String s);


        boolean progressDialogisShow();

    }

    interface IPresenter {
        /**
         * 初始化数据
         **/
        void initData(Intent intent, Handler handler);

        /**
         * 初始化导航路线地图
         **/
        void initMapView();

        /** 获取算路偏好 **/
        int getPreference();

        /**
         * 设置路线模型
         **/
        void setRouteMode();

        /**
         * 设置指定路线详情
         **/
        void setRouteModeDetail(int index);

        /**
         * 开始倒计时
         **/
        void startCountDown();

        /**
         * 结束倒计时
         **/
        void stopCountDown();

        /**
         * 取消倒计时计计时器
         **/
        void cancelCountDownTimer();

        /**
         * 获取导航节点，开始算路
         **/
        void startCalculateRoad();

        /** 根据指定算路偏好重新算路 **/
        void reCalculateRoad(int preference);

        /**
         * 填充对应算路偏好的算路模型
         *
         * @param routeModel 算路模型（包含导航路线的各种信息）
         * @param preference 算路偏好
         **/
        void fillRouteModel(Object routeModel, int preference);

        /**
         * 获取所有算路模型后初始化导航信息，准备导航
         *
         * @param preference         算路偏好
         * @param switchLineCaculate 0=非切换路线，1=未导航中切换路线，2=导航中切换路线
         **/
        void initNavi(int preference, int switchLineCaculate);

        void setNaviStatus(String status);

        /**
         * 开始导航
         **/
        void startNavi(Context context, boolean isGPSNav);

        /**
         * 移除导航路线图，设置进入导航视图
         **/
        void hanldeNaviView();

        /**
         * 定位
         **/
        void location();

        /**
         * 放大地图
         **/
        void MapZoomIn();

        /**
         * 缩小地图
         **/
        void MapZoomOut();

        /**
         * 显示路况
         **/
        void showTraffic();

        /**
         * 验证已经经过的途经点
         **/
        void checkPassedPassPoint();

        /** 查看全程导航路线 **/
        void showFullLine();

        /** 设置后台停止导航标记 **/
        void setStopInBackGround(boolean isNavi);

        /** 显示导航引导路线 **/
        void showNaviGuide();

        /** 设置唤醒模式标记 **/
        void setWakeupFlag();

        /** 导航路线计算 **/
        void routeCalculate(NaviRouteCalculateEvent event);

        /** 切换导航路线 **/
        void switchRouteLine(NaviSwitchRouteLineEvent e);

        /** 处理地图操作事件 **/
        void handleMapCmd(MapCmdEvent e);

        /**
         * 在Activity对应的生命周期方法中调用，用于处理相关资源、参数的释放和调整（主要是导航引擎）
         **/
        void onStop();

        /**
         * 同onStop()
         **/
        void onResume();

        /**
         * 同onStop()
         **/
        void onPause();

        /**
         * 同onStop()
         **/
        void backPressed();

        /**
         * 同onStop()
         **/
        void onConfigurationChanged(Configuration newConfig);

        /**
         * 同onStop()
         **/
        void onDestroy();


    }
}
