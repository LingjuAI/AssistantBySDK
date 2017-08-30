package com.lingju.assistant.activity.event;

import com.lingju.assistant.entity.TaskCard;
import com.lingju.model.Accounting;
import com.lingju.model.AlarmClock;
import com.lingju.model.Memo;
import com.lingju.model.Remind;

/**
 * Created by Ken on 2016/12/14.
 */
public class UpdateTaskCardEvent<T> {
    /**
     * 任务卡更新状态
     * 取值：<{@linkplain com.lingju.assistant.entity.TaskCard.TaskState#ACTIVE ACTIVE}>默认值;
     * {@linkplain com.lingju.assistant.entity.TaskCard.TaskState#DELETED DELETED}
     * {@linkplain com.lingju.assistant.entity.TaskCard.TaskState#INVALID INVALID}
     */
    private int state = 1;
    private T t;
    private boolean invalidItem = true;
    private boolean invalidList = true;

    /**
     * 针对于手动操作，刷新任务卡列表中对应的记录卡片
     **/
    public UpdateTaskCardEvent(T t, boolean isList) {
        this.t = t;
        this.state = TaskCard.TaskState.INVALID;
        if (isList)
            invalidList = false;
        else
            invalidItem = false;
    }

    public UpdateTaskCardEvent(T t, int state) {
        this.t = t;
        this.state = state;
    }

    public int isState() {
        return state;
    }

    public boolean isInvalidItem() {
        return invalidItem;
    }

    public boolean isInvalidList() {
        return invalidList;
    }

    public Class getUpdateClass() {
        return t.getClass();
    }

    public Long getId() {
        if (t instanceof Remind) {
            return ((Remind) t).getId();
        } else if (t instanceof AlarmClock) {
            return ((AlarmClock) t).getId();
        } else if (t instanceof Memo) {
            return ((Memo) t).getId();
        } else {
            return ((Accounting) t).getId();
        }
    }
}
