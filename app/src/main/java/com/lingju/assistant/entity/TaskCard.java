package com.lingju.assistant.entity;

import com.lingju.model.Accounting;
import com.lingju.model.AlarmClock;
import com.lingju.model.Memo;
import com.lingju.model.Remind;

import java.util.List;

/**
 * Created by Ken on 2016/12/8.
 * <p/>
 * 任务流卡片对象
 */
public class TaskCard<T> {
    public T t;     //任务对象（闹钟、备忘等）
    public int taskState;   //任务卡片状态
    public List<TaskCard> taskDatas;
    public boolean firstTouch = true;  //用于标记该卡片在聊天列表中是否第一次被触碰
    public int atype = -1;      //用于记录消费类型（针对于账单类型卡片对象）

    public TaskCard(T t, int state) {
        this.t = t;
        this.taskState = state;
        this.taskDatas = null;
        if (t instanceof Accounting)
            this.atype = ((Accounting) t).getAtype();
    }

    public TaskCard(List<TaskCard> datas) {
        this.t = null;
        this.taskState = TaskState.ACTIVE;
        this.taskDatas = datas;
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

    public interface TaskState {
        /**
         * 有效的
         **/
        int ACTIVE = 1;
        /**
         * 最近一次删除的，可撤销的
         **/
        int REVOCABLE = 0;
        /**
         * 已删除，不可撤销的
         **/
        int DELETED = -1;
        /**
         * 已作废的
         **/
        int INVALID = -2;
    }
}
