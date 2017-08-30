package com.lingju.assistant.activity.index.presenter;

import android.content.Context;
import android.view.View;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.event.ChatMsgEvent;
import com.lingju.assistant.activity.event.SynthesizeEvent;
import com.lingju.assistant.activity.index.IChatList;
import com.lingju.audio.engine.IflySynthesizer;
import com.lingju.audio.engine.base.SpeechMsgBuilder;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.model.temp.speech.ResponseMsg;
import com.lingju.model.temp.speech.ResponseSetionsMsg;
import com.lingju.model.temp.speech.SpeechMsg;
import com.lingju.common.log.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Administrator on 2016/11/4.
 */
public class ChatListPresenter implements IChatList.Presenter {

    public static final String OPEN_TIMES = "open_times";
    private IChatList.ChatListView chatListView;
    private String mInputText = "";
    private Context mContext;

    public ChatListPresenter(IChatList.ChatListView chatListView, Context context) {
        this.chatListView = chatListView;
        this.chatListView.setPresenter(this);
        this.mContext = context;
    }

    @Override
    public void subscribe() {
        EventBus.getDefault().register(this.chatListView);
    }

    @Override
    public void unsubscribe() {
        EventBus.getDefault().unregister(this.chatListView);
    }


    @Override
    public void synthesize(SpeechMsg msg, final View speakervView) {
        if (SynthesizerBase.isInited()) {
            SpeechMsgBuilder builder = new SpeechMsgBuilder(msg.text)
                    .setOrigin(com.lingju.audio.engine.base.SpeechMsg.ORIGIN_COMMON);
            if (msg instanceof ResponseSetionsMsg) {
                SynthesizerBase.get().setForceLocalEngine(false);
                builder.setSections(((ResponseSetionsMsg) msg).getSetions());
            }

            SynthesizerBase.get().startSpeakAbsolute(builder.build())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext(new Consumer<com.lingju.audio.engine.base.SpeechMsg>() {
                        @Override
                        public void accept(com.lingju.audio.engine.base.SpeechMsg speechMsg) throws Exception {
                            if (speechMsg.state() == com.lingju.audio.engine.base.SpeechMsg.State.Idle) {
                                Log.i("LingJu", "doOnNext>>>startSpeakerAnimation>>>" + speakervView);
                                chatListView.startSpeakerAnimation(speakervView);
                            }
                        }
                    })
                    .doOnComplete(new Action() {
                        @Override
                        public void run() throws Exception {
                            Log.i("ChatListPresenter", "doOnComplete>>stopSpeakerAnimation");
                            chatListView.stopSpeakerAnimation(speakervView);
                        }
                    })
                    .subscribe();
        }
    }

    @Override
    public void stopSpeaker() {
        if (SynthesizerBase.isInited()) {
            SynthesizerBase.get().stopSpeakingAbsolte();
        }
    }

    @Override
    public void showOpenTips() {
        String openTips;
        int times;
        if (AppConfig.NewInstallFirstOpen) {    //首次打开
            AppConfig.NewInstallFirstOpen=false;
            openTips = mContext.getResources().getString(R.string.first_welcome);
        } else {
            openTips = mContext.getResources().getString(R.string.welcome);
        }
        chatListView.addMessage(new ChatMsgEvent(new ResponseMsg(openTips), null, null, null));
        if ((times = AppConfig.dPreferences.getInt(OPEN_TIMES, 0)) < 3) {   //前三次进行语音播报
            AppConfig.dPreferences.edit().putInt(OPEN_TIMES, ++times).commit();
            if (IflySynthesizer.isInited()) {
                IflySynthesizer.getInstance().startSpeakAbsolute(openTips)
                        .doOnNext(new Consumer<com.lingju.audio.engine.base.SpeechMsg>() {
                            @Override
                            public void accept(com.lingju.audio.engine.base.SpeechMsg speechMsg) throws Exception {
                                if (speechMsg.state() == com.lingju.audio.engine.base.SpeechMsg.State.OnBegin)
                                    EventBus.getDefault().post(new SynthesizeEvent(SynthesizeEvent.SYNTH_START));
                            }
                        })
                        .doOnComplete(new Action() {
                            @Override
                            public void run() throws Exception {
                                EventBus.getDefault().post(new SynthesizeEvent(SynthesizeEvent.SYNTH_END));
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation())
                        .subscribe();
            }
        } else {
            openTips = setWelcome();
            chatListView.addMessage(new ChatMsgEvent(new ResponseMsg(openTips), null, null, null));
        }
    }

    private String setWelcome() {
        Calendar cl = Calendar.getInstance();
        int hour = cl.get(Calendar.HOUR_OF_DAY);
        String welcome = null;
        if (hour < 5) {
            welcome = "主人好，夜深了，要注意休息！";
        } else if (hour < 6) {
            welcome = "主人好，你是起床了还是没睡呢！";
        } else if (hour < 9) {
            welcome = "主人早上好！";
        } else if (hour < 12) {
            welcome = "主人上午好！";
        } else if (hour < 13) {
            welcome = "主人中午好！";
        } else if (hour < 19) {
            welcome = "主人下午好！";
        } else if (hour < 24) {
            welcome = "主人晚上好！";
        }
        return welcome;
    }

    @Override
    public void setInputText(SpeechMsg msg) {
        if (msg instanceof ResponseMsg)
            return;
        mInputText = msg.text;
    }

    @Override
    public String getInputText() {
        return mInputText;
    }

}
