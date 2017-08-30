package com.lingju.assistant.activity.index;

import com.lingju.assistant.IPresenter;
import com.lingju.assistant.IView;
import com.lingju.assistant.activity.event.RobotResponseEvent;
import com.lingju.context.entity.Command;

/**
 * Created by Ken on 2016/11/23.
 */
public interface IAdditionAssist {

    interface AssistView extends IView<Presenter> {
        /**
         * 刷新对话框的状态：显示/关闭
         **/
        void updateDialogState(RobotResponseEvent e);

        String getInput();

        /* 设置ToolBar Z轴的高度，即设置阴影
        void setToolBarElevation(int elevation);*/
    }


    interface Presenter extends IPresenter {
        /**
         * 处理提醒、备忘等指令
         **/
        void onAdditionResponse(Command cmd, String text, int inputType);

        void cancelToggleDialog();
        void setMemoEditState(boolean isEdit);
        void setAlarmEditState(boolean isEdit);
        void setRemindEditState(boolean isEdit);
        void setAccountEditState(boolean isEdit);

    }
}
