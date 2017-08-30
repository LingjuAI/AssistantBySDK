package com.lingju.assistant.service.process;

import android.content.Context;
import android.text.TextUtils;

import com.lingju.assistant.activity.event.ChatMsgEvent;
import com.lingju.assistant.activity.event.DialogEvent;
import com.lingju.assistant.activity.event.RobotTipsEvent;
import com.lingju.assistant.activity.event.SynthesizeEvent;
import com.lingju.assistant.service.AssistantService;
import com.lingju.assistant.service.process.base.BaseProcessor;
import com.lingju.audio.SystemVoiceMediator;
import com.lingju.audio.engine.IflySynthesizer;
import com.lingju.audio.engine.base.SpeechMsg;
import com.lingju.audio.engine.base.SpeechMsgBuilder;
import com.lingju.audio.engine.base.SynthesizerBase;
import com.lingju.context.entity.Command;
import com.lingju.model.temp.speech.ResponseMsg;
import com.lingju.model.temp.speech.ResponseSetionsMsg;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;

import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Administrator on 2016/11/5.
 */
public class DefaultProcessor extends BaseProcessor {
    private final static String TAG = "DefaultProcessor";
    public final static int OUTC_ASK = 1;
    private final static int OUTC_SYNTHESIZE_PRIORITY = 2;
    private final static int OUTC_NOANSWER = 4;

    public DefaultProcessor(Context mContext, SystemVoiceMediator mediator) {
        super(mContext, mediator);
    }

    @Override
    public int aimCmd() {
        return CMD_DEFAULT;
    }

    @Override
    public void handle(Command cmd, final String text, int inputType) {
        super.handle(cmd, text, inputType);
        SynthesizerBase synthesizer = IflySynthesizer.getInstance();
        if (cmd.getOutc() == OUTC_NOANSWER) {
            // EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(text), null, null, null));
            return;
        }
        final SpeechMsgBuilder builder = SpeechMsgBuilder.create(text);
        ResponseMsg responseMsg = null;
        EventBus.getDefault().post(new RobotTipsEvent(cmd.getTtext()));
        if (!TextUtils.isEmpty(cmd.getSynthetise())) {     //（针对讲笑话）变声
            synthesizer.setForceLocalEngine(false);
            try {
                responseMsg = new ResponseSetionsMsg(text);
                ((ResponseSetionsMsg) responseMsg).setSetions(new JSONArray(cmd.getSynthetise()));
                builder.setSections(((ResponseSetionsMsg) responseMsg).getSetions());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            if ((cmd.getOutc() & OUTC_ASK) == OUTC_ASK) {
                builder.setContextMode(SpeechMsg.CONTEXT_KEEP_RECOGNIZE);
            } else if ((cmd.getOutc() & OUTC_SYNTHESIZE_PRIORITY) == OUTC_SYNTHESIZE_PRIORITY) {
                builder.setPriority(SpeechMsg.PRIORITY_ABOVE_RECOGNIZE);
            }
        }
        if (TextUtils.isEmpty(text)) {
            builder.setText("非常抱歉，我没听懂你在说什么");
        }
        responseMsg = responseMsg == null ? new ResponseMsg(builder.getText()) : responseMsg;
        responseMsg.setSimpleChat(true);
        if (builder.build().contextMode() != SpeechMsg.CONTEXT_KEEP_RECOGNIZE)
            EventBus.getDefault().post(new DialogEvent(DialogEvent.CANCEL_TOGGLE_TYPE));
        EventBus.getDefault().post(new ChatMsgEvent(responseMsg, null, null, null));
        if (inputType == AssistantService.INPUT_VOICE) {
            synthesizer.startSpeakAbsolute(builder.build())
                    .doOnNext(new Consumer<SpeechMsg>() {
                        @Override
                        public void accept(SpeechMsg speechMsg) throws Exception {
                            if (speechMsg.state() == SpeechMsg.State.OnBegin && !TextUtils.isEmpty(builder.getText())) {
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
}
