package com.lingju.assistant.activity.index;

import android.support.v4.view.ViewPager;
import android.support.v4.widget.LingjuSwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lingju.assistant.view.DrawForExpandLayout;
import com.lingju.assistant.view.LocationSuggestListView;
import com.lingju.assistant.view.NaviVoiceInputDialog;
import com.lingju.assistant.view.RealTimeUpdateSearchBox;

/**
 * Created by Ken on 2016/12/20.
 */
public interface ITrafficShow {


    interface INaviSetPointView {
        void setRefreshLayout(boolean b);

        DrawForExpandLayout getPoiListBox();

        ViewGroup getPoiDetialBox();

        ViewPager getPoiDetailPager();

        ViewGroup getPoiDetialDotList();

        LocationSuggestListView getSuggestListView();

        RealTimeUpdateSearchBox getedit();

        void showSnackBar(String s);

//        TextView getGoHomeText();
//
//        TextView getGoCompanyText();
//
//        LinearLayout getGoWhereBox();

        RecyclerView getPoiListView();

        LingjuSwipeRefreshLayout getRefreshLayout();

        NaviVoiceInputDialog getVoiceDialog();

        TextView getSearchBt();

//        ImageButton getSearchVoiceBt();

    }

    interface IPresenter {
        /**
         * 初始化页面数据
         **/
        void initData();
        /**
         * 城市内检索目标
         **/
        void goSearch();
        /**
         * 地图定位
         **/
        void location();
        /**
         * 跳转地址详情页上一页
         */
        void setPoiDetailPagerToPre();
        /**
         * 跳转地址详情页下一页
         */
        void setPoiDetailPagerToNext();
        /**
         * 跳转地址详情页下一页
         */
        void destroy();

        /**
         * 定位开始
         */
        void locateManagerStart();


    }
}
