package com.lingju.assistant.activity.index;

import android.view.View;

import com.lingju.assistant.IPresenter;
import com.lingju.assistant.IView;
import com.lingju.assistant.activity.event.ChatMsgEvent;
import com.lingju.assistant.activity.event.UpdateTaskCardEvent;
import com.lingju.model.temp.speech.SpeechMsg;

/**
 * Created by Administrator on 2016/11/4.
 */
public interface IChatList {

    /**
     * 显示聊天对话记录的列表视图
     */
    interface ChatListView extends IView<Presenter> {
        /**
         * 添加新的合成文本信息到列表中
         *
         * @param msg
         */
        void addMessage(ChatMsgEvent msg);

        void scrollToLastPosition();

        /**
         * 滑动到指定索引位置
         **/
        void moveToPosition(int position);

        void showSynthErrorDialog();

        int getLastInputHeight();

        /**
         * 刷新任务卡片视图
         **/
        void refreshTaskCard(UpdateTaskCardEvent e);

        void refreshTingPlayView();

        void startSpeakerAnimation(View v);

        void stopSpeakerAnimation(View v);

        void startSpeakerAnimation(int i);

        void stopSpeakerAnimation(int i);

        void stopSpeakerAnimation();

        /** 当主界面跳转页面时，处理正在文本录音模式的备忘卡片 **/
        void handleMemoCard();
    }

    interface Presenter extends IPresenter {

        void synthesize(final SpeechMsg msg, final View speakervView);

        void stopSpeaker();

        void showOpenTips();

        void setInputText(SpeechMsg msg);

        String getInputText();
    }

}
