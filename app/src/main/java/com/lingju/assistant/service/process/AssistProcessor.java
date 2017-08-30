package com.lingju.assistant.service.process;

import android.content.Context;

import com.lingju.assistant.activity.event.RobotResponseEvent;
import com.lingju.assistant.service.process.base.BaseProcessor;
import com.lingju.audio.SystemVoiceMediator;
import com.lingju.context.entity.Command;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Administrator on 2016/11/5.
 */
public class AssistProcessor extends BaseProcessor {

    public AssistProcessor(Context mContext, SystemVoiceMediator mediator) {
        super(mContext, mediator);
    }

    @Override
    public int aimCmd() {
        return CMD_ADDITION;
    }

    @Override
    public void handle(Command cmd, String text, int inputType) {
        super.handle(cmd, text, inputType);
        EventBus.getDefault().post(new RobotResponseEvent(text, cmd, inputType));
    }

}
