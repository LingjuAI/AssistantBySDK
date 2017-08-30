package com.lingju.model.dao;

import android.util.Log;

import com.google.gson.JsonArray;
import com.lingju.assistant.entity.RobotConstant;
import com.lingju.common.repository.SyncDao;
import com.lingju.context.entity.AlarmClockEntity;
import com.lingju.context.entity.BillEntity;
import com.lingju.context.entity.MemoEntity;
import com.lingju.context.entity.RemindEntity;
import com.lingju.context.entity.Scheduler;
import com.lingju.model.Accounting;
import com.lingju.model.AlarmClock;
import com.lingju.model.Memo;
import com.lingju.model.Remind;
import com.lingju.robot.AndroidChatRobotBuilder;
import com.lingju.util.AssistUtils;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ken on 2017/5/11.
 */
public class AssistEntityDao {

    private static AssistEntityDao instance;
    private AssistDao mAssistDao;

    private AssistEntityDao() {
        mAssistDao = AssistDao.getInstance();
    }

    public static AssistEntityDao create() {
        if (instance == null) {
            synchronized (AssistEntityDao.class) {
                if (instance == null)
                    instance = new AssistEntityDao();
            }
        }
        return instance;
    }

    /**
     * 获取指定类型对象实例
     **/
    public <T> T getDao(Class<T> clazz) {
        T t = null;
        try {
            //内部类编译时会在构造方法中加入外部类作为参数，所以通过反射无参构造无效
            t = clazz.getDeclaredConstructor(AssistEntityDao.class).newInstance(instance);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return t;
    }

    /**
     * 同步数据
     **/
    public void sync(SyncDao syncDao) {
        Single.just(syncDao)
                .observeOn(Schedulers.io())
                .doOnSuccess(new Consumer<SyncDao>() {
                    @Override
                    public void accept(SyncDao syncDao) throws Exception {
                        AndroidChatRobotBuilder.get().robot().actionTargetAccessor().sync(syncDao);
                    }
                })
                .subscribe();
    }

    /**
     * 备忘动作对象操作模型
     **/
    public class MemoEntityDao implements SyncDao<MemoEntity> {

        public MemoEntityDao() {
        }

        private void convertEntity(List<MemoEntity> unSyncList) {
            List<Memo> memoList = mAssistDao.findAllMemoDesc(true);
            for (Memo memo : memoList) {
                if (!memo.getSynced()) {
                    MemoEntity entity = new MemoEntity();
                    entity.setLid(memo.getId().intValue());
                    entity.setCreated(memo.getCreated());
                    entity.setModified(memo.getModified());
                    entity.setContent(memo.getContent());
                    entity.setSid(memo.getSid());
                    entity.setRecyle(memo.getRecyle());
                    entity.setSynced(memo.getSynced());
                    entity.setTimestamp(memo.getTimestamp());
                    unSyncList.add(entity);
                }
            }
        }


        @Override
        public int getTargetId() {
            return RobotConstant.ACTION_MEMO;
        }

        @Override
        public Class<MemoEntity> getTargetClass() {
            return MemoEntity.class;
        }

        @Override
        public long getLastTimestamp() {
            return mAssistDao.getLastTimestamp(RobotConstant.ACTION_MEMO);
        }

        @Override
        public boolean mergeServerData(JsonArray jsonArray) {
            return false;
        }

        @Override
        public boolean mergeServerData(List<MemoEntity> list) {
            if (list != null && list.size() > 0) {
                mAssistDao.mergeMemoData(list);
                AndroidChatRobotBuilder.get().robot().actionTargetAccessor().syncUpdate(this);
            }
            return true;
        }

        @Override
        public List<MemoEntity> getUnSyncLocalData(int i) {
            List<MemoEntity> list = new ArrayList<>();
            convertEntity(list);
            return list;
        }

        @Override
        public int getUnsyncLocalDataCount() {
            return mAssistDao.getUnsyncLocalDataCount(RobotConstant.ACTION_MEMO);
        }

        @Override
        public JsonArray getUnSyncLocalDataAsJsonArray(int i) {
            return null;
        }
    }

    /**
     * 提醒动作对象同步操作模型
     **/
    public class RemindEntityDao implements SyncDao<RemindEntity> {

        public RemindEntityDao() {
        }

        private void convertEntity(List<RemindEntity> unSyncList) {
            List<Remind> allReminds = mAssistDao.findAllRemind(true);
            for (Remind remind : allReminds) {
                if (!remind.getSynced()) {
                    RemindEntity entity = new RemindEntity();
                    entity.setContent(remind.getContent());
                    entity.setCreated(remind.getCreated());
                    List<Scheduler> schedulers = new ArrayList<>();
                    Scheduler scheduler;
                    long when = remind.getRdate().getTime();
                    int fr = remind.getFrequency();
                    if (fr == 0) {
                        scheduler = new Scheduler(when, 0);
                    } else if (fr == 8) {
                        scheduler = new Scheduler(when, 1, Scheduler.Unit.D);
                    } else if (fr == 9) {
                        scheduler = new Scheduler(when, 1, Scheduler.Unit.M);
                    } else if (fr == 10) {
                        scheduler = new Scheduler(when, 1, Scheduler.Unit.Y);
                    } else {
                        scheduler = new Scheduler(when, 7, Scheduler.Unit.D);
                    }
                    schedulers.add(scheduler);
                    entity.setScheduler(schedulers);
                    //本地记录ID
                    entity.setLid(remind.getId().intValue());
                    //服务器记录ID
                    entity.setSid(remind.getSid());
                    entity.setRecyle(remind.getRecyle());
                    entity.setTimestamp(remind.getTimestamp());
                    //false表示未同步，需要同步更新
                    entity.setSynced(remind.getSynced());
                    unSyncList.add(entity);
                }
            }
        }

        @Override
        public int getTargetId() {
            return RobotConstant.ACTION_REMIND;
        }

        @Override
        public Class<RemindEntity> getTargetClass() {
            return RemindEntity.class;
        }

        @Override
        public long getLastTimestamp() {
            return mAssistDao.getLastTimestamp(RobotConstant.ACTION_REMIND);
        }

        @Override
        public boolean mergeServerData(JsonArray jsonArray) {
            return false;
        }

        @Override
        public boolean mergeServerData(List<RemindEntity> list) {
            if (list != null && list.size() > 0) {
                mAssistDao.mergeRemindData(list);
                AndroidChatRobotBuilder.get().robot().actionTargetAccessor().syncUpdate(this);
            }
            return true;
        }

        @Override
        public List<RemindEntity> getUnSyncLocalData(int i) {
            List<RemindEntity> list = new ArrayList<>();
            convertEntity(list);
            return list;
        }

        @Override
        public int getUnsyncLocalDataCount() {
            return mAssistDao.getUnsyncLocalDataCount(RobotConstant.ACTION_REMIND);
        }

        @Override
        public JsonArray getUnSyncLocalDataAsJsonArray(int i) {
            return null;
        }
    }

    /**
     * 闹钟动作对象同步操作模型
     **/
    public class AlarmEntityDao implements SyncDao<AlarmClockEntity> {

        public AlarmEntityDao() {
        }

        private void convertEntity(List<AlarmClockEntity> unSyncList) {
            List<AlarmClock> alarmClocks = mAssistDao.findAllAlarmAsc(true);
            for (AlarmClock alarm : alarmClocks) {
                if (!alarm.getSynced()) {
                    AlarmClockEntity alarmEntity = new AlarmClockEntity();
                    alarmEntity.setLid(alarm.getId().intValue());
                    alarmEntity.setRecyle(alarm.getRecyle());
                    alarmEntity.setSynced(alarm.getSynced());
                    alarmEntity.setTimestamp(alarm.getTimestamp());
                    alarmEntity.setSid(alarm.getSid());
                    alarmEntity.setItem(alarm.getItem());
                    alarmEntity.setCreated(alarm.getCreated());
                    List<Scheduler> schedulers = new ArrayList<>();
                    Scheduler scheduler;
                    if (alarm.getRepeat()) {
                        int[] weekDays = AssistUtils.transalteWeekDays(alarm.getFrequency(true));
                        if (weekDays.length == 7) {    //每天
                            scheduler = new Scheduler(alarm.getRdate().getTime(), 1, Scheduler.Unit.D);
                            schedulers.add(scheduler);
                        } else {
                            scheduler = new Scheduler(alarm.getRdate().getTime(), 7, Scheduler.Unit.D);
                            schedulers.add(scheduler);
                            for (int i = 1; i < weekDays.length; i++) {
                                long when = alarm.getRdate().getTime() + ((weekDays[i] - weekDays[0]) * 24 * 3600 * 1000);
                                scheduler = new Scheduler(when, 7, Scheduler.Unit.D);
                                schedulers.add(scheduler);
                            }
                        }
                    } else {     //仅一次
                        scheduler = new Scheduler(alarm.getRdate().getTime(), 0);
                        schedulers.add(scheduler);
                    }
                    alarmEntity.setScheduler(schedulers);
                    unSyncList.add(alarmEntity);
                }
            }
        }

        @Override
        public int getTargetId() {
            return RobotConstant.ACTION_ALARM;
        }

        @Override
        public Class<AlarmClockEntity> getTargetClass() {
            return AlarmClockEntity.class;
        }

        @Override
        public long getLastTimestamp() {
            return mAssistDao.getLastTimestamp(RobotConstant.ACTION_ALARM);
        }

        @Override
        public boolean mergeServerData(JsonArray jsonArray) {
            return false;
        }

        @Override
        public boolean mergeServerData(List<AlarmClockEntity> list) {
            if (list != null && list.size() > 0) {
                mAssistDao.mergeAlarmdData(list);
                AndroidChatRobotBuilder.get().robot().actionTargetAccessor().syncUpdate(this);
            }
            return true;
        }

        @Override
        public List<AlarmClockEntity> getUnSyncLocalData(int i) {
            List<AlarmClockEntity> list = new ArrayList<>();
            convertEntity(list);
            return list;
        }

        @Override
        public int getUnsyncLocalDataCount() {
            return mAssistDao.getUnsyncLocalDataCount(RobotConstant.ACTION_ALARM);
        }

        @Override
        public JsonArray getUnSyncLocalDataAsJsonArray(int i) {
            return null;
        }
    }

    /**
     * 记账动作对象同步操作模型
     **/
    public class BillEntityDao implements SyncDao<BillEntity> {

        public BillEntityDao() {
        }

        private void convertEntity(List<BillEntity> unSyncList) {
            List<Accounting> accountings = mAssistDao.findAllAccount(true);
            for (Accounting account : accountings) {
                if (!account.getSynced())
                    unSyncList.add(account.toBill());
            }
        }

        @Override
        public int getTargetId() {
            return RobotConstant.ACTION_ACCOUNTING;
        }

        @Override
        public Class<BillEntity> getTargetClass() {
            return BillEntity.class;
        }

        @Override
        public long getLastTimestamp() {
            return mAssistDao.getLastTimestamp(RobotConstant.ACTION_ACCOUNTING);
        }

        @Override
        public boolean mergeServerData(JsonArray jsonArray) {
            return false;
        }

        @Override
        public boolean mergeServerData(List<BillEntity> list) {
            if (list != null && list.size() > 0) {
                mAssistDao.mergeAccountData(list);
                AndroidChatRobotBuilder.get().robot().actionTargetAccessor().syncUpdate(this);
            }
            return true;
        }

        @Override
        public List<BillEntity> getUnSyncLocalData(int i) {
            List<BillEntity> list = new ArrayList<>();
            convertEntity(list);
            return list;
        }

        @Override
        public int getUnsyncLocalDataCount() {
            return mAssistDao.getUnsyncLocalDataCount(RobotConstant.ACTION_ACCOUNTING);
        }

        @Override
        public JsonArray getUnSyncLocalDataAsJsonArray(int i) {
            return null;
        }
    }
}
