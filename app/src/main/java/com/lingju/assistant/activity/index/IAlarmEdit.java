package com.lingju.assistant.activity.index;

import android.content.Intent;

/**
 * Created by Ken on 2016/12/3.
 */
public interface IAlarmEdit {

    interface AlarmEditView {
        /**
         * 设置时间文本
         **/
        void setTime(String time);

        /**
         * 设置周期文本
         **/
        void setFrequency(String fr);

        /**
         * 设置铃声文本
         **/
        void setRing(String ring);
    }

    interface IPresenter {
        /**
         * 初始化数据
         **/
        void initData(Intent intent);

        /**
         * 设置闹钟属性文本
         **/
        void setAlarmText(Intent intent, int resultCode);

        /**
         * 退出页面
         **/
        void cancelEdit();

        /**
         * 删除闹钟
         **/
        void deleteAlarm();

        /**
         * 保存闹钟
         **/
        void saveAlarm();

        /**
         * 设置时间
         **/
        void toSetTime();

        /**
         * 设置周期
         **/
        void toSetFr();

        /**
         * 设置铃声
         **/
        void toSetRing();
    }
}
