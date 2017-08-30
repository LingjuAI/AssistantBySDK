package com.lingju.model.dao;

import android.util.Log;

import com.lingju.assistant.entity.MonthAccount;
import com.lingju.assistant.entity.RobotConstant;
import com.lingju.assistant.entity.TaskCard;
import com.lingju.context.entity.AlarmClockEntity;
import com.lingju.context.entity.BillEntity;
import com.lingju.context.entity.MemoEntity;
import com.lingju.context.entity.RemindEntity;
import com.lingju.context.entity.Scheduler;
import com.lingju.model.Accounting;
import com.lingju.model.AccountingDao;
import com.lingju.model.AlarmClock;
import com.lingju.model.AlarmClockDao;
import com.lingju.model.Memo;
import com.lingju.model.MemoDao;
import com.lingju.model.Remind;
import com.lingju.model.RemindDao;
import com.lingju.model.SimpleDate;
import com.lingju.util.AssistUtils;
import com.lingju.util.TimeUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Ken on 2016/11/24.
 */
public class AssistDao {

    private static AssistDao dao;
    private AlarmClockDao mClockDao;
    private RemindDao mRemindDao;
    private final MemoDao mMemoDao;
    private final AccountingDao mAccountingDao;

    private AssistDao() {
        mClockDao = DaoManager.get().getDaoSession().getAlarmClockDao();
        mRemindDao = DaoManager.get().getDaoSession().getRemindDao();
        mMemoDao = DaoManager.get().getDaoSession().getMemoDao();
        mAccountingDao = DaoManager.get().getDaoSession().getAccountingDao();
    }

    public static synchronized AssistDao getInstance() {
        if (dao == null) {
            dao = new AssistDao();
        }
        return dao;
    }

    /**
     * 清空数据库中的无效记录
     **/
    public void clearRecyleData() {
        List<Remind> reminds = mRemindDao.queryBuilder().where(RemindDao.Properties.Recyle.eq(1)).list();
        mRemindDao.deleteInTx(reminds);

        List<AlarmClock> alarmClocks = mClockDao.queryBuilder().where(AlarmClockDao.Properties.Recyle.eq(1)).list();
        mClockDao.deleteInTx(alarmClocks);

        List<Memo> memos = mMemoDao.queryBuilder().where(MemoDao.Properties.Recyle.eq(1)).list();
        mMemoDao.deleteInTx(memos);

        List<Accounting> accountings = mAccountingDao.queryBuilder().where(AccountingDao.Properties.Recyle.eq(1)).list();
        mAccountingDao.deleteInTx(accountings);

    }

    /**
     * 插入一条闹钟记录
     **/
    public boolean insertAlarm(AlarmClock alarm) {
        if (alarm.getId() != null)
            alarm = findAlarmById(alarm.getId());
        alarm.setRecyle(0);
        return mClockDao.insertOrReplace(alarm) > 0;
    }

    /**
     * 插入一条提醒
     **/
    public boolean insertRemind(Remind remind) {
        if (remind.getId() != null)
            remind = findRemindById(remind.getId());
        remind.setRecyle(0);
        return mRemindDao.insertOrReplace(remind) > 0;
    }

    /**
     * 插入一条备忘记录
     **/
    public boolean insertMemo(Memo memo) {
        if (memo.getId() != null)
            memo = findMemoById(memo.getId());
        memo.setRecyle(0);
        return mMemoDao.insertOrReplace(memo) > 0;
    }

    /**
     * 插入一条账单记录
     **/
    public void insertAccount(Accounting account) {
        if (account.getId() != null)
            account = findAccountById(account.getId());
        account.setRecyle(0);
        mAccountingDao.save(account);
    }

    /**
     * 插入多条账单记录
     **/
    public void insertAccountInIx(List<Accounting> list) {
        mAccountingDao.saveInTx(list);
    }

    /**
     * 删除一条闹钟记录
     **/
    public void deleteAlarm(AlarmClock alarm) {
        alarm = findAlarmById(alarm.getId());
        alarm.setRecyle(1);
        mClockDao.update(alarm);
    }

    /**
     * 删除一条提醒(不能直接删除，需要暂时保持缓存。规定在每次进入应用时清理)
     **/
    public void deleteRemind(Remind remind) {
        Remind realRemind = findRemindById(remind.getId());
        realRemind.setRecyle(1);
        mRemindDao.update(realRemind);
    }

    /**
     * 删除一条备忘记录
     **/
    public void deleteMemo(Memo memo) {
        Memo realMemo = findMemoById(memo.getId());
        realMemo.setRecyle(1);
        mMemoDao.update(realMemo);
    }

    /**
     * 删除一条账单记录
     **/
    public void deleteAccount(Accounting account) {
        Accounting realAccount = findAccountById(account.getId());
        realAccount.setRecyle(1);
        mAccountingDao.update(realAccount);
    }

    /**
     * 删除多条账单记录
     **/
    public void deleteAccountInIx(List<Accounting> list) {
        mAccountingDao.saveInTx(list);
    }

    /**
     * 修改一条闹钟记录
     **/
    public void updateAlarm(AlarmClock alarm) {
        AlarmClock realAlarm = findAlarmById(alarm.getId());
        alarm.setTimestamp(realAlarm.getTimestamp());
        alarm.setRecyle(0);
        mClockDao.update(alarm);
    }

    /**
     * 修改一条提醒记录
     **/
    public void updateRemind(Remind remind) {
        Remind realRemind = findRemindById(remind.getId());
        remind.setTimestamp(realRemind.getTimestamp());
        remind.setRecyle(0);
        mRemindDao.update(remind);
    }

    /**
     * 修改一条备忘记录
     **/
    public void updateMemo(Memo memo) {
        Memo realMemo = findMemoById(memo.getId());
        memo.setTimestamp(realMemo.getTimestamp());
        memo.setRecyle(0);
        mMemoDao.update(memo);
    }

    /**
     * 修改一条账单记录
     **/
    public void updateAccount(Accounting account) {
        Accounting realAccount = findAccountById(account.getId());
        account.setTimestamp(realAccount.getTimestamp());
        account.setRecyle(0);
        mAccountingDao.update(account);
    }

    /**
     * 查询指定ID闹钟记录
     **/
    public AlarmClock findAlarmById(long id) {
        return mClockDao.queryBuilder().where(AlarmClockDao.Properties.Id.eq(id)).unique();
    }

    public AlarmClock findAlarmBySid(String sid) {
        return mClockDao.queryBuilder().where(AlarmClockDao.Properties.Sid.eq(sid)).unique();
    }

    public Remind findRemindById(long id) {
        return mRemindDao.queryBuilder().where(RemindDao.Properties.Id.eq(id)).unique();
    }

    /**
     * 查询指定sid提醒记录
     **/
    public Remind findRemindBySid(String sid) {
        try {
            return mRemindDao.queryBuilder().where(RemindDao.Properties.Sid.eq(sid)).unique();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Memo findMemoById(long id) {
        return mMemoDao.queryBuilder().where(MemoDao.Properties.Id.eq(id)).unique();
    }

    public Memo findMemoBySid(String sid) {
        return mMemoDao.queryBuilder().where(MemoDao.Properties.Sid.eq(sid)).unique();
    }

    public Accounting findAccountById(long id) {
        return mAccountingDao.queryBuilder().where(AccountingDao.Properties.Id.eq(id)).unique();
    }

    public Accounting findAccountBySid(String sid) {
        return mAccountingDao.queryBuilder().where(AccountingDao.Properties.Sid.eq(sid)).unique();
    }

    /**
     * 查询所有闹钟记录
     **/
    public List<AlarmClock> findAllAlarm() {
        return mClockDao.queryBuilder().where(AlarmClockDao.Properties.Recyle.eq(0)).list();
    }

    /**
     * 查询所有闹钟记录(按创建时间倒序排列)
     **/
    public List<AlarmClock> findAllAlarmAsc(boolean recyle) {
        if (recyle)
            return mClockDao.queryBuilder().orderAsc(AlarmClockDao.Properties.Rtime).list();
        else
            return mClockDao.queryBuilder().where(AlarmClockDao.Properties.Recyle.eq(0)).orderDesc(AlarmClockDao.Properties.Rtime).list();
    }

    /**
     * 查询所有提醒记录
     *
     * @param recyle 是否包含有已删除标记的记录
     **/
    public List<Remind> findAllRemind(boolean recyle) {
        if (recyle)
            return mRemindDao.queryBuilder().list();
        else
            return mRemindDao.queryBuilder().where(RemindDao.Properties.Recyle.eq(0)).list();
    }


    /**
     * 获取当天的提醒记录数
     **/
    public long findRemindToday() {
        Date todayDate = TimeUtils.getTodayDate();
        Date tomorrow = TimeUtils.getTomorrow();
        return mRemindDao.queryBuilder().where(RemindDao.Properties.Recyle.eq(0), RemindDao.Properties.Rdate.ge(todayDate), RemindDao.Properties.Rdate.lt(tomorrow)).count();
    }

    public List<Remind> findRemindFuture() {
        Date tomorrow = TimeUtils.getTomorrow();
        return mRemindDao.queryBuilder().where(RemindDao.Properties.Recyle.eq(0), RemindDao.Properties.Rdate.ge(tomorrow)).list();
    }

    /**
     * 获取5条今天之前的提醒记录
     **/
    public List<Remind> findRemindPast(int offset) {
        Date todayDate = TimeUtils.getTodayDate();
        List<Remind> list = mRemindDao.queryBuilder().where(RemindDao.Properties.Recyle.eq(0), RemindDao.Properties.Rdate.lt(todayDate)).orderDesc(RemindDao.Properties.Rdate)
                .offset(offset * 5).limit(5).list();
        // Collections.reverse(list);
        return list;
    }

    /**
     * 查询所有备忘记录（以创建时间为标准倒序排列）
     **/
    public List<Memo> findAllMemoDesc(boolean recyle) {
        if (recyle)
            return mMemoDao.queryBuilder().orderDesc(MemoDao.Properties.Created).list();
        else
            return mMemoDao.queryBuilder().where(MemoDao.Properties.Recyle.eq(0)).orderDesc(MemoDao.Properties.Created).list();
    }

    /**
     * 查询最新修改的备忘记录
     **/
    public Memo findMemoNewModified() {
        return mMemoDao.queryBuilder().orderDesc(MemoDao.Properties.Modified).limit(1).unique();
    }

    /**
     * 查询所有提醒记录（以提醒日期为标准倒序排列）
     **/
    public List<Remind> findAllRemindDesc() {
        return mRemindDao.queryBuilder().where(RemindDao.Properties.Recyle.eq(0)).orderDesc(RemindDao.Properties.Rdate, RemindDao.Properties.Rtime).list();
    }

    /**
     * 查询所有账单记录
     **/
    public List<Accounting> findAllAccount(boolean recyle) {
        if (recyle)
            return mAccountingDao.queryBuilder().list();
        else
            return mAccountingDao.queryBuilder().where(AccountingDao.Properties.Recyle.eq(0)).list();
    }

    /**
     * 查询当前月份目前为止的账单记录
     **/
    public List<Accounting> findMonthByCreated() {
        return findAccountByCreadted(TimeUtils.thisMonth(), new Date());
    }

    /**
     * 查询最近一周的账单记录
     **/
    public List<Accounting> findLastWeek() {
        return findAccountByCreadted(TimeUtils.lastWeekBefore(), new Date());
    }

    /**
     * 查询一个时间段内创建的所有账单记录
     **/
    public List<Accounting> findAccountByCreadted(Date begin, Date end) {
        return mAccountingDao.queryBuilder().where(
                AccountingDao.Properties.Recyle.eq(0),
                AccountingDao.Properties.Created.between(begin, end))
                .orderDesc(AccountingDao.Properties.Created).list();
    }

    public List<Accounting> findAccountToday() {
        return mAccountingDao.queryBuilder().where(
                AccountingDao.Properties.Recyle.eq(0),
                AccountingDao.Properties.Rdate.ge(TimeUtils.getTodayDate()),
                AccountingDao.Properties.Rdate.lt(TimeUtils.getTomorrow())).list();
    }

    /**
     * 查询指定年份的账单记录
     **/
    public void findAccountAllYear(List<MonthAccount> showDatas, int year) {
        showDatas.clear();
        for (int i = 12; i > 0; i--) {
            List<Accounting> list = mAccountingDao.queryBuilder().where(
                    AccountingDao.Properties.Recyle.eq(0),
                    AccountingDao.Properties.Rdate.ge(TimeUtils.getYearStart(year)),
                    AccountingDao.Properties.Rdate.lt(TimeUtils.getYearStart(year + 1)),
                    AccountingDao.Properties.Month.eq(i)).orderDesc(AccountingDao.Properties.Rdate).list();
            if (list.size() > 0) {
                MonthAccount ma = new MonthAccount();
                List<TaskCard<Accounting>> cardList = new ArrayList<>();
                for (Accounting account : list) {
                    cardList.add(new TaskCard<>(account, TaskCard.TaskState.ACTIVE));
                }
                ma.month = i;
                ma.taskCards = cardList;
                showDatas.add(ma);
            }
        }
    }

    /**
     * 获取闹钟记录数
     **/
    public long getAlarmCount() {
        return mClockDao.queryBuilder().count();
    }

    public long getAccountCount() {
        return mAccountingDao.queryBuilder().count();
    }

    /**
     * 查询最新闹钟记录（最新插入）
     **/
    public AlarmClock findAlarmNewCreated() {
        return mClockDao.queryBuilder().orderDesc(AlarmClockDao.Properties.Id).limit(1).unique();
    }

    /**
     * 查询最新提醒记录
     **/
    public Remind findRemindNewCreated() {
        return mRemindDao.queryBuilder().where(RemindDao.Properties.Recyle.eq(0)).orderDesc(RemindDao.Properties.Id).limit(1).unique();
    }

    /**
     * 查询最新备忘记录
     **/
    public Memo findMemoNewCreated() {
        return mMemoDao.queryBuilder().orderDesc(MemoDao.Properties.Id).limit(1).unique();
    }

    /**
     * 获取指定类型的记录最新的时间戳
     *
     * @param targetId 动作对象id
     *                 return 该类型记录的最新时间戳
     **/
    public long getLastTimestamp(int targetId) {
        switch (targetId) {
            case RobotConstant.ACTION_MEMO:
                Memo memo = mMemoDao.queryBuilder().orderDesc(MemoDao.Properties.Timestamp).limit(1).unique();
                return memo == null ? 0 : memo.getTimestamp();
            case RobotConstant.ACTION_REMIND:
                Remind remind = mRemindDao.queryBuilder().orderDesc(RemindDao.Properties.Timestamp).limit(1).unique();
                return remind == null ? 0 : remind.getTimestamp();
            case RobotConstant.ACTION_ALARM:
                AlarmClock alarm = mClockDao.queryBuilder().orderDesc(AlarmClockDao.Properties.Timestamp).limit(1).unique();
                return alarm == null ? 0 : alarm.getTimestamp();
            case RobotConstant.ACTION_ACCOUNTING:
                Accounting accounting = mAccountingDao.queryBuilder().orderDesc(AccountingDao.Properties.Timestamp).limit(1).unique();
                return accounting == null ? 0 : accounting.getTimestamp();
            default:
                return 0;
        }
    }

    /**
     * 获取指定类型记录的未同步数量
     **/
    public int getUnsyncLocalDataCount(int targetId) {
        switch (targetId) {
            case RobotConstant.ACTION_MEMO:
                return (int) mMemoDao.queryBuilder().where(MemoDao.Properties.Synced.eq(false)).count();
            case RobotConstant.ACTION_REMIND:
                return (int) mRemindDao.queryBuilder().where(RemindDao.Properties.Synced.eq(false)).count();
            case RobotConstant.ACTION_ALARM:
                return (int) mClockDao.queryBuilder().where(AlarmClockDao.Properties.Synced.eq(false)).count();
            case RobotConstant.ACTION_ACCOUNTING:
                return (int) mAccountingDao.queryBuilder().where(AccountingDao.Properties.Synced.eq(false)).count();
            default:
                return 0;
        }
    }

    /**
     * 同步合并服务器中的备忘记录
     **/
    public void mergeMemoData(List<MemoEntity> entities) {
        for (MemoEntity entity : entities) {
            Log.i("LingJu", "AssistDao mergeMemoData()>>>" + entity.getLid() + " 时间戳：" + entity.getTimestamp());
            Memo memo = findMemoById(entity.getLid());
            if (memo == null)
                memo = findMemoBySid(entity.getSid());
            if (memo == null)
                memo = new Memo();
            memo.setCreated(entity.getCreated());
            memo.setContent(entity.getContent());
            memo.setTimestamp(entity.getTimestamp());
            memo.setSynced(true);
            memo.setRecyle(entity.getRecyle());
            memo.setSid(entity.getSid());
            mMemoDao.save(memo);
        }
    }

    /**
     * 同步合并服务器中的闹钟记录
     **/
    public void mergeAlarmdData(List<AlarmClockEntity> list) {
        for (AlarmClockEntity entity : list) {
            Log.i("LingJu", "AssistDao mergeAlarmdData()>>>" + entity.getLid() + "时间戳：" + entity.getTimestamp());
            AlarmClock alarm = findAlarmById(entity.getLid());
            if (alarm == null)
                alarm = findAlarmBySid(entity.getSid());
            if (alarm == null) {
                alarm = new AlarmClock();
                alarm.setValid(1);
            }
            alarm.setItem(entity.getItem());
            alarm.setCreated(entity.getCreated());
            alarm.setSid(entity.getSid());
            alarm.setRecyle(entity.getRecyle());
            alarm.setTimestamp(entity.getTimestamp());
            alarm.setSynced(true);
            AssistUtils.setAlarmFr(alarm, entity.getScheduler());
            if (alarm.getId() == null && alarm.getFrequency() == 0) {   //安装后第一次打开应用时同步数据，在本地都视为新建数据，但有些记录时过时的，不需要激活
                Calendar cl = Calendar.getInstance();
                SimpleDate sd = new SimpleDate();
                cl.setTime(alarm.getRdate());
                sd.setValue(alarm.getRtime());
                cl.set(Calendar.HOUR_OF_DAY, sd.getHour());
                cl.set(Calendar.MINUTE, sd.getMinute());
                cl.set(Calendar.MILLISECOND, 0);
                if (System.currentTimeMillis() > cl.getTimeInMillis()) {
                    alarm.setValid(0);
                }
            }
            mClockDao.save(alarm);
        }
    }

    /**
     * 同步合并服务器中的提醒记录
     **/
    public void mergeRemindData(List<RemindEntity> entities) {
        Remind remind;
        for (RemindEntity entity : entities) {
            Log.i("LingJu", "AssistDao mergeRemindData()>>>" + entity.getLid() + "时间戳：" + entity.getTimestamp());
            remind = findRemindById(entity.getLid());
            if (remind == null)
                remind = findRemindBySid(entity.getSid());
            if (remind == null) {
                remind = new Remind();
                remind.setValid(1);
            }
            remind.setContent(entity.getContent());
            Scheduler scheduler = entity.getScheduler().get(0);
            AssistUtils.setRemindFr(remind, scheduler);
            remind.setRdate(new Date(scheduler.getWhen()));
            remind.setRtime(TimeUtils.getTime(remind.getRdate()));
            remind.setCreated(entity.getCreated());
            remind.setSid(entity.getSid());
            remind.setRecyle(entity.getRecyle());
            remind.setTimestamp(entity.getTimestamp());
            remind.setSynced(true);
            if (remind.getId() == null && remind.getFrequency() == 0) {
                Calendar cl = Calendar.getInstance();
                SimpleDate sd = new SimpleDate();
                cl.setTime(remind.getRdate());
                sd.setValue(new SimpleDate(remind.getRtime()).toValue());
                cl.set(Calendar.HOUR_OF_DAY, sd.getHour());
                cl.set(Calendar.MINUTE, sd.getMinute());
                cl.set(Calendar.MILLISECOND, 0);
                if (System.currentTimeMillis() > cl.getTimeInMillis()) {
                    remind.setValid(0);
                }
            }
            mRemindDao.insertOrReplace(remind);
        }
    }

    public void mergeAccountData(List<BillEntity> list) {
        for (BillEntity bill : list) {
            Log.i("LingJu", "AssistDao mergeAccountData()>>>" + bill.getLid() + "时间戳：" + bill.getTimestamp());
            Accounting account = findAccountById(bill.getLid());
            if (account == null)
                account = findAccountBySid(bill.getSid());
            if (account == null)
                account = new Accounting();
            account.fromBill(bill);
            account.setSynced(true);
            mAccountingDao.save(account);
        }
    }
}
