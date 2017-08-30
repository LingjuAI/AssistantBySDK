package com.lingju.assistant.activity.event;

import com.lingju.assistant.entity.CallAndSmsMsg;
import com.lingju.assistant.entity.TaskCard;
import com.lingju.assistant.entity.TingAlbumMsg;
import com.lingju.model.Memo;
import com.lingju.model.temp.speech.SpeechMsg;

/**
 * Created by Ken on 2016/12/13.
 */
public class ChatMsgEvent {
    public final static int REMOVE_CARD_STATE = 0;
    public final static int REMOVE_LIST_STATE = 1;
    public final static int UPDATE_CALL_SMS_STATE = 2;
    public final static int REMOVE_TING_ALBUM_STATE = 3;
    public final static int REMOVE_TING_TRACK_STATE = 4;
    /**
     * 文本对话对象
     **/
    public SpeechMsg textMsg;
    /**
     * 任务流对象
     **/
    public TaskCard taskCard;
    /**
     * 系统提示文本
     **/
    public String sysTips;
    /**
     * 待修改备忘对象
     **/
    public Memo memo;
    /**
     * 视图刷新标记
     **/
    public int refresh = -1;
    public Class clazz;

    public CallAndSmsMsg callAndSmsMsg;

    public TingAlbumMsg tingMsg;

    public ChatMsgEvent() {
    }

    public ChatMsgEvent(TingAlbumMsg tingMsg) {
        this.tingMsg = tingMsg;
    }

    public ChatMsgEvent(CallAndSmsMsg callAndSmsMsg) {
        this.callAndSmsMsg = callAndSmsMsg;
    }

    public ChatMsgEvent(int state) {
        this.refresh = state;
    }

    public ChatMsgEvent(int state, Class cla) {
        this.refresh = state;
        this.clazz = cla;
    }

    public ChatMsgEvent(SpeechMsg msg, TaskCard card, String tips, Memo memo) {
        this.textMsg = msg;
        this.taskCard = card;
        this.sysTips = tips;
        this.memo = memo;
    }

    @Override
    public String toString() {
        return "ChatMsgEvent{" +
                "clazz=" + clazz +
                ", textMsg=" + textMsg +
                ", taskCard=" + taskCard +
                ", sysTips='" + sysTips + '\'' +
                ", memo=" + memo +
                ", refresh=" + refresh +
                '}';
    }
}
