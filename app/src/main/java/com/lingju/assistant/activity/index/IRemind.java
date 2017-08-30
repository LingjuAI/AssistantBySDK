package com.lingju.assistant.activity.index;

import com.lingju.assistant.entity.TaskCard;
import com.lingju.model.Remind;

import java.util.List;

/**
 * Created by Ken on 2016/12/19.
 */
public interface IRemind {

    interface IRemindView {
        /**
         * 显示日期时间信息
         **/
        void showDateTimeMsg();

        /**
         * 初始化视图
         **/
        void initView();

        /**
         * 刷新列表视图
         **/
        void notifyListView();

        /**
         * 列表移动置顶
         **/
        void moveToPosition(int position);

        void showProgressBar();

        /** 隐藏进度条 **/
        void hideProgressBar();
    }

    int INSERT_TYPE = 0;
    int DELETE_TYPE = 1;
    int UPDATE_TYPE = 2;

    interface IPresenter {
        /**
         * 初始化数据
         **/
        void initDatas(long id);

        /**
         * 获取提醒列表数据集合
         **/
        List<TaskCard<Remind>> getShowDatas();

        /**
         * 获取当天提醒记录数
         **/
        long getTodayCount();

        /**
         * 设置当天提醒记录数
         **/
        void setTodayCount(long count);

        /**
         * 滚动到当天提醒记录的第一个索引位置
         **/
        void scrollToTodayFirst();

        /**
         * 获取当天提醒第一条记录索引
         **/
        int getTodayFirstPosition();

        /**
         * 加载数据
         **/
        boolean loadDatas(int page);

        /**
         * 获取过去记录数
         **/
        int getPastNum();

        /**
         * 数据库操作
         **/
        void operateData(int type, Remind remind);

        /**
         * 通知提醒服务开/关提醒
         **/
        void switchRemind(Remind remind, int cmd);
    }
}
