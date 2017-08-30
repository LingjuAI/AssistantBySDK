package com.lingju.assistant.activity.index;

import android.content.Intent;

/**
 * Created by Ken on 2016/12/2.
 */
public interface IAlarmRinging {

    interface AlarmRingingView {
        /**
         * 设置闹钟时间
         **/
        void setRingRime(String time);

        void destoryView();
    }

    interface IPresenter {
        /**
         * 响铃
         **/
        void goRing(Intent intent);

        /**
         * 停止响铃
         **/
        void stopRing();

        /**
         * 延迟响铃
         **/
        void delayRing();
    }
}
