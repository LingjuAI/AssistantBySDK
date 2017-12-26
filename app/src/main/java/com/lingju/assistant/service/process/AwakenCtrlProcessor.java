package com.lingju.assistant.service.process;

import android.content.Context;
import android.util.Log;

import com.lingju.assistant.activity.event.ChatMsgEvent;
import com.lingju.assistant.activity.event.SynthesizeEvent;
import com.lingju.assistant.service.process.base.BaseProcessor;
import com.lingju.audio.SystemVoiceMediator;
import com.lingju.audio.engine.base.SpeechMsg;
import com.lingju.audio.engine.base.SpeechMsgBuilder;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.context.entity.Command;
import com.lingju.model.temp.speech.ResponseMsg;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Administrator on 2016/11/5.
 */
public class AwakenCtrlProcessor extends BaseProcessor {

    public AwakenCtrlProcessor(Context mContext, SystemVoiceMediator mediator) {
        super(mContext, mediator);
    }

    @Override
    public int aimCmd() {
        return CMD_AWAKEN;
    }

    @Override
    public void handle(Command cmd, String text, int inputType) {
        super.handle(cmd, text, inputType);
        /* 创建合成语音信息建造者对象 */
        final SpeechMsgBuilder builder = SpeechMsgBuilder.create(text);
        if (cmd.getOutc() == DefaultProcessor.OUTC_ASK)      //说完话后自动开启识别
            builder.setContextMode(SpeechMsg.CONTEXT_KEEP_RECOGNIZE);
        try {
            JSONArray actions = new JSONArray(cmd.getActions());
            JSONObject target = actions.getJSONObject(0).getJSONObject("target");
            if (!target.isNull("mode")) {
                String mode = target.getString("mode");
                voiceMediator.setWakeUpMode("ON".equals(mode));
            }
            if (!target.isNull("status")) {
                Log.i("LingJu", "status:" + target.getString("status"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        /* 将回复文本发送到聊天视图 */
        EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(text), null, null, null));
        /* 合成回复文本声音 */
        SynthesizerBase.get().startSpeakAbsolute(builder.setText(text).build())
                .doOnNext(new Consumer<SpeechMsg>() {
                    @Override
                    public void accept(SpeechMsg speechMsg) throws Exception {
                        if (speechMsg.state() == SpeechMsg.State.OnBegin) {
                            EventBus.getDefault().post(new SynthesizeEvent(SynthesizeEvent.SYNTH_START));
                        }
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
}
