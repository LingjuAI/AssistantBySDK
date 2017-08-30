package com.lingju.assistant.activity.index;

import com.lingju.assistant.IPresenter;
import com.lingju.assistant.IView;

/**
 * Created by Administrator on 2016/11/4.
 */
public interface IVoiceInput {

     interface VoiceInputView extends IView<VoicePresenter>{

        void setMicButtonState(int state);

        void showMicButtonTips(String tipsText);

        void switch2TextInput();

        void switchTipsText(boolean isShow);

        void showIntroduce();

        void switchView(boolean show);

    }

    interface VoicePresenter extends IPresenter{

         void startRecognize();

         void stopRecognize();


    }

}
