package com.lingju.assistant.activity.index;

import android.app.Activity;
import android.os.Handler;
import android.widget.TextView;

/**
 * Created by Ken on 2016/12/20.
 */
public interface INaviSetPoint {
    /**
     * 搜索栏为空，隐藏搜索结果列表和删除搜索按钮
     **/
    int STATE_SEARCH_HIDE = 0;
    /**
     * 搜索栏有内容，显示搜索结果列表和删除搜索按钮
     **/
    int STATE_SEARCH_SHOW = 1;

    interface INaviSetPointView {
        /**
         * 设置出发地文本
         */
        void setStartAddrText(String startAddr);

        String getStartAddrText();
        /**
         * 设置目的地文本
         */
        void setEndAddrText(String endAddr);

        String getEndAddrText();

        /**
         * 获取回家时间文本
         */
        TextView getGoHomeTextView();
        /**
         * 获取回家时间文本
         */
        TextView getGoCompanyTextView();

        void showSnackBar(String s);
        /** 刷新导航记录 **/
        void refresh();

    }

    interface IPresenter {

        /**
         * 定位当前位置
         **/
        void setLocation();
        /**
         * 初始化初始位置
         **/
        void initStartAddress();

        void setCalculated(boolean isCalculated);

        /**
         * 初始化页面数据
         **/
        void initData();
        /**
         * 更新家视图
         **/
        void updateHome();
        /**
         * 更新公司视图
         **/
        void updateCompany();

        /**
         * 初始化百度导航引擎
         **/
        void initBaiduNaiv(Activity activity, Handler handler);

        /**
         * 刷新页面数据并更新视图
         **/
        void updateData(int resultCode);
        /**
         * 更新历史记录列表
         **/
        void updateHistoryList();

        /**
         * 清空历史记录
         **/
        void cleanHistory();

        /**
         * 是否刷新历史记录
         **/
        boolean isUpdateHistory();
        /**
         * 设置刷新历史记录标记
         **/
        void setUpdateHistory(boolean updateHistory);
        /**
         * 设置出发地
         */
        void toSetStartAddr();
        /**
         * 设置目的地
         */
        void toSetEndAddr();
        /**
         * 设置回家的导航路线
         **/
        void toSetHomeAddr();
        /**
         * 进入回家的导航
         **/
        void toHomeNavi();

        /**
         * 设置公司导航路线
         **/
        void toSetCompanyAddr();
        /**
         * 进入去公司导航
         **/
        void toCompanyNavi();

        /** 销毁相关监听器 **/
        void destoryListener();
        /** 交换出发地与目的地地址 **/
        void exchangeAddress();

        void destroy();

        /**
         * 计算回家和去公司的时间
         */
        void setGoCompanyAndGoHomeCalculate();

    }
}
