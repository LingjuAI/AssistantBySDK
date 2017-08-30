package com.lingju.assistant.entity;

import com.lingju.model.Accounting;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ken on 2017/3/7.
 */
public class MonthAccount {

    public int month;
    public double income;
    public double expense;
    public boolean isExpended;
    public int state = TaskCard.TaskState.ACTIVE;
    public List<TaskCard<Accounting>> taskCards = new ArrayList<>();
}
