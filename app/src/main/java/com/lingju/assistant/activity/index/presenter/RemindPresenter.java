package com.lingju.assistant.activity.index.presenter;

import android.app.Activity;
import android.content.Intent;

import com.lingju.assistant.activity.index.IRemind;
import com.lingju.assistant.entity.TaskCard;
import com.lingju.assistant.service.RemindService;
import com.lingju.model.Remind;
import com.lingju.model.dao.AssistDao;
import com.lingju.util.TimeUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.BiConsumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ken on 2016/12/19.
 */
public class RemindPresenter implements IRemind.IPresenter {

    private IRemind.IRemindView remindView;
    private AssistDao mAssistDao;
    private Activity mContext;
    private List<TaskCard<Remind>> showDatas = new ArrayList<>();
    private int pastNum;
    private long todayCount;

    public RemindPresenter(IRemind.IRemindView view) {
        this.remindView = view;
        this.mContext = (Activity) view;
        this.mAssistDao = AssistDao.getInstance();
    }

    @Override
    public void initDatas(final long id) {
        remindView.showProgressBar();
        Single.create(new SingleOnSubscribe<Integer>() {
            @Override
            public void subscribe(SingleEmitter<Integer> e) throws Exception {
                todayCount = mAssistDao.findRemindToday();
                List<Remind> allReminds = mAssistDao.findAllRemindDesc();
                for (Remind remind : allReminds) {
                    TaskCard<Remind> taskCard = new TaskCard<>(remind, TaskCard.TaskState.ACTIVE);
                    /*if (remind.getRdate().before(TimeUtils.getTodayDate())) {
                        taskCard.taskState = TaskCard.TaskState.INVALID;
                    } else if (remind.getRdate().before(TimeUtils.getTomorrow())) {
                        if (new SimpleDate(remind.getRtime()).toValue() < TimeUtils.getTimeValue(new Date())) {
                            taskCard.taskState = TaskCard.TaskState.INVALID;
                        }
                    }*/
                    if(remind.getFrequency()==0&&remind.getRdate().before(new Date())) {
                        taskCard.taskState = TaskCard.TaskState.INVALID;
                    }
                    showDatas.add(taskCard);
                }
                e.onSuccess(0);
            }
        })
                .observeOn(AndroidSchedulers.mainThread())  //响应订阅（Sbscriber）所在线程
                .subscribeOn(Schedulers.io())   //执行订阅（subscribe()）所在线程
                .subscribe(new BiConsumer<Integer, Throwable>() {
                    @Override
                    public void accept(Integer integer, Throwable throwable) throws Exception {
                        remindView.hideProgressBar();
                        remindView.notifyListView();
                        if (id > 0) {
                            for (int i = 0; i < showDatas.size(); i++) {
                                if (id == showDatas.get(i).t.getId()) {
                                    remindView.moveToPosition(i + 1);
                                    break;
                                }
                            }
                        }
                    }
                });


    }

    @Override
    public List<TaskCard<Remind>> getShowDatas() {
        return showDatas;
    }

    @Override
    public long getTodayCount() {
        return todayCount;
    }

    @Override
    public void setTodayCount(long count) {
        this.todayCount = count;
    }

    @Override
    public void scrollToTodayFirst() {
        if (todayCount > 0) {
            remindView.moveToPosition(getTodayFirstPosition());
        }
    }

    @Override
    public boolean loadDatas(int page) {
        //加载过期的数据记录
        List<Remind> reminds = mAssistDao.findRemindPast(page);
        if (reminds == null || reminds.size() == 0) {
            return false;
        }
        //记录过去记录数目
        pastNum += reminds.size();
        for (Remind remind : reminds) {
            TaskCard<Remind> taskCard = new TaskCard<>(remind, TaskCard.TaskState.INVALID);
            showDatas.add(0, taskCard);
        }
        remindView.notifyListView();
        return true;
    }

    @Override
    public int getPastNum() {
        return pastNum;
    }

    @Override
    public void operateData(int type, Remind remind) {
        switch (type) {
            case IRemind.INSERT_TYPE:
                mAssistDao.insertRemind(remind);
                break;
            case IRemind.DELETE_TYPE:
                mAssistDao.deleteRemind(remind);
                break;
            case IRemind.UPDATE_TYPE:
                mAssistDao.updateRemind(remind);
                break;
        }
    }

    @Override
    public void switchRemind(Remind remind, int cmd) {
        Intent rIntent = new Intent(mContext, RemindService.class);
        rIntent.putExtra(RemindService.CMD, (RemindService.REMIND << 4) + cmd);
        rIntent.putExtra(RemindService.ID, remind.getId());
        mContext.startService(rIntent);
    }

    /**
     * 获取当天提醒第一条记录索引
     **/
    @Override
    public int getTodayFirstPosition() {
        for (int i = 0; i < showDatas.size(); i++) {
            Date rdate = showDatas.get(i).t.getRdate();
            if (rdate.compareTo(TimeUtils.getTodayDate()) >= 0
                    && rdate.before(TimeUtils.getTomorrow())) {
                return i + 1;
            }
        }
        return -1;
    }
}
