package com.lingju.assistant.activity.index;

import android.content.Intent;

import com.ximalaya.ting.android.opensdk.model.album.Album;

import java.util.List;

/**
 * Created by Ken on 2017/6/27.
 */
public interface ITingSearch {

    int HISTORY_TYPE = 0;
    int HOT_TYPE = 1;

    interface ITingSearchView {
        /**
         * 设置搜索框提示语
         **/
        void setSearchHint(String hint);

        void switchProgressBar(boolean isShow);

        /**
         * 搜索标签（分为历史、热词）区域显示、隐藏
         **/
        void showSearchTagBox(int tagType, List<String> tags);

        /**
         * 搜索列表的显示、隐藏
         **/
        void switchSearchListBox(List<Album> datas);
    }

    interface IPresenter {
        /**
         * 加载从聊天列表中传递过来的专辑数据
         **/
        void loadAllDataFromChat(Intent intent);

        /**
         * 加载搜索历史记录
         **/
        void loadHistory();

        /**
         * 加载热词标签数据
         **/
        void loadHotTag(boolean isNext);

        /**
         * 保存搜索记录
         **/
        void saveHistory(String keyword);

        /**
         * 清空搜索历史
         **/
        void clearHistory();

        /**
         * 根据关键词搜索专辑
         **/
        void goSearch(String keyword, int pageNum);
    }

}
