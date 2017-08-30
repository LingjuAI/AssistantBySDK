package com.lingju.assistant.activity.index.presenter;

import android.content.Intent;
import android.text.TextUtils;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.activity.AccountingActivity;
import com.lingju.assistant.activity.MainActivity;
import com.lingju.assistant.activity.event.AlarmStateEvent;
import com.lingju.assistant.activity.event.ChatMsgEvent;
import com.lingju.assistant.activity.event.RobotTipsEvent;
import com.lingju.assistant.activity.event.SynthesizeEvent;
import com.lingju.assistant.activity.event.UpdateTaskCardEvent;
import com.lingju.assistant.activity.index.IAdditionAssist;
import com.lingju.assistant.entity.RobotConstant;
import com.lingju.assistant.entity.TaskCard;
import com.lingju.assistant.entity.action.DialogEntity;
import com.lingju.assistant.service.AssistantService;
import com.lingju.assistant.service.RemindService;
import com.lingju.assistant.service.process.DefaultProcessor;
import com.lingju.assistant.view.MultiChoiceDialog;
import com.lingju.assistant.view.SingleChooseDialog;
import com.lingju.audio.engine.IflyRecognizer;
import com.lingju.audio.engine.IflySynthesizer;
import com.lingju.audio.engine.base.SpeechMsg;
import com.lingju.audio.engine.base.SpeechMsgBuilder;
import com.lingju.context.entity.AlarmClockEntity;
import com.lingju.context.entity.BillEntity;
import com.lingju.context.entity.Command;
import com.lingju.context.entity.MemoEntity;
import com.lingju.context.entity.RemindEntity;
import com.lingju.context.entity.Scheduler;
import com.lingju.context.entity.SyncSegment;
import com.lingju.model.Accounting;
import com.lingju.model.AlarmClock;
import com.lingju.model.Memo;
import com.lingju.model.Remind;
import com.lingju.model.Tape;
import com.lingju.model.dao.AssistDao;
import com.lingju.model.dao.AssistEntityDao;
import com.lingju.model.dao.DaoManager;
import com.lingju.model.dao.TapeEntityDao;
import com.lingju.model.temp.speech.ResponseMsg;
import com.lingju.util.AssistUtils;
import com.lingju.util.JsonUtils;
import com.lingju.util.TimeUtils;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ken on 2016/11/23.
 */
public class AssistPresenter implements IAdditionAssist.Presenter {
    private final static String TAG = "AssistPresenter";
    private MainActivity assistView;
    private MultiChoiceDialog mMultiDialog;
    private AssistDao mAssistDao;
    private SingleChooseDialog toggleDialog;
    private boolean is_memo_edit;      //新建备忘是否展开编辑中标记
    private boolean is_alarm_edit;     //新建闹钟是否展开编辑中标记
    private boolean is_remind_edit;    //新建提醒是否展开编辑中标记
    private boolean is_account_edit;   //新建记账是否展开编辑中标记

    public AssistPresenter(IAdditionAssist.AssistView assistView) {
        this.assistView = (MainActivity) assistView;
        /* 在主页面再次初始化 */
        DaoManager.create(this.assistView);
        mAssistDao = AssistDao.getInstance();
    }

    @Override
    public void onAdditionResponse(Command cmd, String text, int inputType) {
        try {
            cancelToggleDialog();
            SpeechMsgBuilder builder = SpeechMsgBuilder.create(text);
            if (cmd.getOutc() == DefaultProcessor.OUTC_ASK)      //说完话后自动开启识别
                builder.setContextMode(SpeechMsg.CONTEXT_KEEP_RECOGNIZE);
            JSONArray actions = new JSONArray(cmd.getActions());
            JSONObject lastAction = actions.getJSONObject(actions.length() - 1);
            Integer action = RobotConstant.ActionMap.get(lastAction.getString("action"));
            //发送对话提示语
            EventBus.getDefault().post(new RobotTipsEvent(cmd.getTtext()));
            if (action != RobotConstant.READ) {  //发送回复文本视图（播报除外）
                showAndSpeak(builder);
            }
            JSONObject lastTarget = lastAction.getJSONObject("target");
            switch (lastTarget.getInt("id")) {
                case RobotConstant.ACTION_REMIND:   //提醒部分
                    switch (action) {
                        case RobotConstant.CREATE:     //新建
                            if (is_remind_edit) {
                                EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_CARD_STATE, Remind.class));
                            }
                            is_remind_edit = true;
                            dismiss();
                            List<TaskCard> newRemindCards = new ArrayList<>();
                            for (int i = 0; i < actions.length(); i++) {
                                Remind newRemind = new Remind();
                                newRemind.setValid(1);
                                //解析提醒动作对象
                                RemindEntity entity = SyncSegment.fromJson(actions.getJSONObject(i).getJSONObject("target").toString(), RemindEntity.class);
                                newRemind.setContent(entity.getContent());
                                List<Scheduler> schedulers = entity.getScheduler();
                                if (schedulers != null && schedulers.size() > 0) {
                                    Scheduler scheduler = schedulers.get(0);
                                    newRemind.setRdate(new Date(scheduler.getWhen()));
                                    newRemind.setRtime(TimeUtils.getTime(newRemind.getRdate()));
                                    AssistUtils.setRemindFr(newRemind, scheduler);
                                }
                                //保存云端同步信息
                                newRemind.setSid(entity.getSid());
                                newRemind.setRecyle(entity.getRecyle());
                                newRemind.setTimestamp(entity.getTimestamp());
                                if (!TextUtils.isEmpty(newRemind.getContent()) && newRemind.getRdate() != null) {    //条件齐全，直接创建
                                    is_remind_edit = false;
                                    newRemind.setCreated(entity.getCreated());
                                    mAssistDao.insertRemind(newRemind);
                                    //通知提醒服务
                                    switchRemind(newRemind, RemindService.ADD);
                                }
                                newRemindCards.add(new TaskCard<>(newRemind, TaskCard.TaskState.ACTIVE));
                            }
                            if (newRemindCards.size() == 1)
                                EventBus.getDefault().post(new ChatMsgEvent(null, newRemindCards.get(0), null, null));
                            else
                                EventBus.getDefault().post(new ChatMsgEvent(null, new TaskCard(newRemindCards), null, null));
                            break;
                        case RobotConstant.MODIFY:      //编辑修改
                            RemindEntity modifyEntity = SyncSegment.fromJson(lastTarget.toString(), RemindEntity.class);
                            Remind uRemind = mAssistDao.findRemindBySid(modifyEntity.getSid());
                            uRemind.setContent(modifyEntity.getContent());
                            Scheduler scheduler = modifyEntity.getScheduler().get(0);
                            uRemind.setRdate(new Date(scheduler.getWhen()));
                            uRemind.setRtime(TimeUtils.getTime(new Date(scheduler.getWhen())));
                            uRemind.setTimestamp(modifyEntity.getTimestamp());
                            AssistUtils.setRemindFr(uRemind, scheduler);
                            mAssistDao.updateRemind(uRemind);
                            /* 重新设置新的时间提醒 */
                            switchRemind(uRemind, RemindService.ADD);
                            EventBus.getDefault().post(new UpdateTaskCardEvent<>(uRemind, TaskCard.TaskState.INVALID));
                            EventBus.getDefault().post(new ChatMsgEvent(null, new TaskCard<>(uRemind, TaskCard.TaskState.ACTIVE), null, null));
                            break;
                        case RobotConstant.DELETE:      //删除
                            List<TaskCard> delRemindCards = new ArrayList<>();
                            for (int i = 0; i < actions.length(); i++) {
                                JSONObject target = actions.getJSONObject(i).getJSONObject("target");
                                Remind remind = mAssistDao.findRemindBySid(target.getString("sid"));
                                delRemindCards.add(new TaskCard<>(remind, TaskCard.TaskState.ACTIVE));
                                /* 从数据库中删除提醒记录 */
                                mAssistDao.deleteRemind(remind);
                                /* 通知提醒服务取消 */
                                switchRemind(remind, RemindService.CANCEL);
                            }
                            //移除上一个卡片
                            EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_CARD_STATE, Remind.class));
                            if (delRemindCards.size() == 1) {
                                /* 单条记录，直接添加可撤销态卡片视图 */
                                EventBus.getDefault().post(new ChatMsgEvent(null, delRemindCards.get(0), null, null));
                                //将处于可撤销状态的卡片设为已删除状态
                                EventBus.getDefault().post(new UpdateTaskCardEvent<>(delRemindCards.get(0).t, TaskCard.TaskState.DELETED));
                            }
                            break;
                        case RobotConstant.RETURN:      //撤销
                            if (actions.length() == 1) {
                                JSONObject target = actions.getJSONObject(0).getJSONObject("target");
                                Remind remind = mAssistDao.findRemindBySid(target.getString("sid"));
                                mAssistDao.insertRemind(remind);
                                /* 开启提醒服务 */
                                switchRemind(remind, RemindService.ADD);
                                // 单条记录，直接添加完成态卡片视图
                                EventBus.getDefault().post(new ChatMsgEvent(null, new TaskCard<>(remind, TaskCard.TaskState.ACTIVE), null, null));
                                EventBus.getDefault().post(new UpdateTaskCardEvent<>(remind, TaskCard.TaskState.ACTIVE));
                            }
                            break;
                        case RobotConstant.VIEW:        //查看
                            List<TaskCard> remindCards = new ArrayList<>();
                            for (int i = 0; i < actions.length(); i++) {
                                JSONObject target = actions.getJSONObject(i).getJSONObject("target");
                                Remind remind = mAssistDao.findRemindBySid(target.getString("sid"));
                                remindCards.add(new TaskCard<>(remind, TaskCard.TaskState.ACTIVE));
                            }
                            //移除聊天列表中旧的提醒列表
                            EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_LIST_STATE, Remind.class));
                            //添加新的提醒列表
                            EventBus.getDefault().post(new ChatMsgEvent(null, new TaskCard<>(remindCards), null, null));
                            break;
                        case RobotConstant.CANCEL:      //取消
                            dismiss();
                            is_remind_edit = false;
                            //移除编辑状态的视图
                            EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_CARD_STATE, Remind.class));
                            break;
                        case RobotConstant.READ:        //播报
                            StringBuilder sb = new StringBuilder(text);
                            sb.append("\n");
                            for (int i = 0; i < actions.length(); i++) {
                                JSONObject target = actions.getJSONObject(i).getJSONObject("target");
                                RemindEntity remindEntity = SyncSegment.fromJson(target.toString(), RemindEntity.class);
                                Long when = remindEntity.getScheduler().get(0).getWhen();
                                sb.append("第").append(i + 1).append("条：")
                                        .append(TimeUtils.formatDateTime(new Date(when))).append(" ")
                                        .append(remindEntity.getContent()).append("\n");
                            }
                            text = sb.deleteCharAt(sb.length() - 1).toString();
                            builder.setText(text);
                            showAndSpeak(builder);
                            break;
                    }
                    break;
                case RobotConstant.ACTION_ALARM:   //闹钟部分
                    switch (action) {
                        case RobotConstant.CREATE:
                            AlarmClock alarm = new AlarmClock();
                            if (is_alarm_edit)
                                EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_CARD_STATE, AlarmClock.class));
                            is_alarm_edit = true;
                            AlarmClockEntity clockEntity = SyncSegment.fromJson(lastTarget.toString(), AlarmClockEntity.class);
                            alarm.setValid(1);
                            alarm.setItem(clockEntity.getItem());
                            alarm.setRecyle(clockEntity.getRecyle());
                            if (!TextUtils.isEmpty(clockEntity.getSid())) {
                                is_alarm_edit = false;
                                alarm.setSid(clockEntity.getSid());
                                alarm.setTimestamp(clockEntity.getTimestamp());
                                alarm.setCreated(clockEntity.getCreated());
                                List<Scheduler> schedulers = clockEntity.getScheduler();
                                AssistUtils.setAlarmFr(alarm, schedulers);
                                mAssistDao.insertAlarm(alarm);
                            }
                            // 通知视图刷新
                            EventBus.getDefault().post(new ChatMsgEvent(null, new TaskCard<>(alarm, TaskCard.TaskState.ACTIVE), null, null));
                            break;
                        case RobotConstant.DELETE:
                            List<TaskCard> delCards = new ArrayList<>();
                            for (int i = 0; i < actions.length(); i++) {
                                JSONObject target = actions.getJSONObject(i).getJSONObject("target");
                                AlarmClock delAlarm = mAssistDao.findAlarmBySid(target.getString("sid"));
                                /* 删除数据库记录 */
                                mAssistDao.deleteAlarm(delAlarm);
                                /* 通知提醒服务取消闹钟 */
                                switchAlarm(delAlarm, RemindService.CANCEL);
                                delCards.add(new TaskCard<>(delAlarm, TaskCard.TaskState.ACTIVE));
                            }
                            if (delCards.size() == 1) {
                                //移除上一个卡片
                                EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_CARD_STATE, AlarmClock.class));
                                //单条记录，直接添加可撤销态卡片视图
                                EventBus.getDefault().post(new ChatMsgEvent(null, delCards.get(0), null, null));
                                //将处于可撤销状态的卡片设为已删除状态
                                EventBus.getDefault().post(new UpdateTaskCardEvent<>(delCards.get(0).t, TaskCard.TaskState.DELETED));
                            }

                            break;
                        case RobotConstant.RETURN:
                            List<TaskCard> rCards = new ArrayList<>();
                            for (int i = 0; i < actions.length(); i++) {
                                JSONObject target = actions.getJSONObject(i).getJSONObject("target");
                                AlarmClock rAlarm = mAssistDao.findAlarmBySid(target.getString("sid"));
                                /* 删除数据库记录 */
                                mAssistDao.insertAlarm(rAlarm);
                                /* 通知提醒服务取消闹钟 */
                                switchAlarm(rAlarm, RemindService.ADD);
                                rCards.add(new TaskCard<>(rAlarm, TaskCard.TaskState.ACTIVE));
                            }
                            if (rCards.size() == 1) {
                                //移除上一个卡片
                                // EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_CARD_STATE, AlarmClock.class));
                                //单条记录，直接添加可撤销态卡片视图
                                EventBus.getDefault().post(new ChatMsgEvent(null, rCards.get(0), null, null));
                                //将处于可撤销状态的卡片设为已删除状态
                                EventBus.getDefault().post(new UpdateTaskCardEvent<>(rCards.get(0).t, TaskCard.TaskState.ACTIVE));
                            }
                            break;
                        case RobotConstant.MODIFY:
                            AlarmClockEntity entity = SyncSegment.fromJson(lastTarget.toString(), AlarmClockEntity.class);
                            AlarmClock mAlarm = mAssistDao.findAlarmBySid(entity.getSid());
                            mAlarm.setItem(entity.getItem());
                            mAlarm.setTimestamp(entity.getTimestamp());
                            AssistUtils.setAlarmFr(mAlarm, entity.getScheduler());
                            mAssistDao.updateAlarm(mAlarm);
                            /* 取消旧时间闹钟 */
                            switchAlarm(mAlarm, RemindService.ADD);
                            EventBus.getDefault().post(new UpdateTaskCardEvent<>(mAlarm, TaskCard.TaskState.INVALID));
                            EventBus.getDefault().post(new ChatMsgEvent(null, new TaskCard<>(mAlarm, TaskCard.TaskState.ACTIVE), null, null));
                            break;
                        case RobotConstant.VIEW:
                            List<TaskCard> alarmCards = new ArrayList<>();
                            for (int i = 0; i < actions.length(); i++) {
                                JSONObject target = actions.getJSONObject(i).getJSONObject("target");
                                AlarmClock vAlarm = mAssistDao.findAlarmBySid(target.getString("sid"));
                                alarmCards.add(new TaskCard<>(vAlarm, TaskCard.TaskState.ACTIVE));
                            }
                            EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_LIST_STATE, AlarmClock.class));
                            EventBus.getDefault().post(new ChatMsgEvent(null, new TaskCard<>(alarmCards), null, null));
                            break;
                        case RobotConstant.OPEN:
                        case RobotConstant.CLOSE:
                            List<AlarmClock> alarms = new ArrayList<>();
                            for (int i = 0; i < actions.length(); i++) {
                                JSONObject target = actions.getJSONObject(i).getJSONObject("target");
                                AlarmClock oAlarm = mAssistDao.findAlarmBySid(target.getString("sid"));
                                oAlarm.setValid(action == RobotConstant.OPEN ? 1 : 0);
                                if (oAlarm.getValid() == 1) {
                                    AssistUtils.setAlarmRdate(oAlarm);
                                    oAlarm.setSynced(false);
                                }
                                mAssistDao.updateAlarm(oAlarm);
                                switchAlarm(oAlarm, action == RobotConstant.OPEN ? RemindService.ADD : RemindService.CANCEL);
                                alarms.add(oAlarm);
                            }
                            EventBus.getDefault().post(new AlarmStateEvent(alarms));
                            if (action == RobotConstant.OPEN) {      //同步修改响铃日期
                                final AssistEntityDao.AlarmEntityDao alarmEntityDao = AssistEntityDao.create().getDao(AssistEntityDao.AlarmEntityDao.class);
                                AssistEntityDao.create().sync(alarmEntityDao);
                            }
                            break;
                        case RobotConstant.CANCEL:
                            is_alarm_edit = false;
                            //移除编辑状态的视图
                            EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_CARD_STATE, AlarmClock.class));
                            break;
                        case RobotConstant.READ:
                            StringBuilder sb = new StringBuilder(text);
                            sb.append("\n");
                            for (int i = 0; i < actions.length(); i++) {
                                JSONObject target = actions.getJSONObject(i).getJSONObject("target");
                                AlarmClockEntity alarmEntity = SyncSegment.fromJson(target.toString(), AlarmClockEntity.class);
                                Long when = alarmEntity.getScheduler().get(0).getWhen();
                                sb.append("第").append(i + 1).append("条：")
                                        .append(TimeUtils.getTime(new Date(when))).append("\n");
                            }
                            text = sb.deleteCharAt(sb.length() - 1).toString();
                            builder.setText(text);
                            showAndSpeak(builder);
                            break;
                    }
                    break;
                case RobotConstant.ACTION_MEMO:   //备忘部分
                    switch (action) {
                        case RobotConstant.CREATE:
                            if (is_memo_edit)
                                EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_CARD_STATE, Memo.class));
                            Memo memo = new Memo();
                            MemoEntity memoEntity = SyncSegment.fromJson(lastTarget.toString(), MemoEntity.class);
                            memo.setContent(memoEntity.getContent());
                            memo.setCreated(memoEntity.getCreated());
                            memo.setRecyle(memoEntity.getRecyle());
                            memo.setTimestamp(memoEntity.getTimestamp());
                            memo.setSid(memoEntity.getSid());
                            if (TextUtils.isEmpty(memo.getContent())) {     //编辑中
                                switchRecordMode(true, IflyRecognizer.CREATE_MEMO_MODE);
                                is_memo_edit = true;
                                cmd.setTtext("您处于输入文本模式，所说的话将转为备忘内容/说“创建”或“取消”结束任务");
                                //发送对话提示语
                                EventBus.getDefault().post(new RobotTipsEvent(cmd.getTtext()));
                            } else {
                                is_memo_edit = false;
                                switchRecordMode(false, IflyRecognizer.CREATE_MEMO_MODE);
                                mAssistDao.insertMemo(memo);
                            }
                            EventBus.getDefault().post(new ChatMsgEvent(null, new TaskCard<>(memo, TaskCard.TaskState.ACTIVE), null, null));
                            break;
                        case RobotConstant.DELETE:
                            Memo delMemo = mAssistDao.findMemoBySid(lastTarget.getString("sid"));
                            // 从数据库中删除备忘记录
                            mAssistDao.deleteMemo(delMemo);
                            //移除上一个卡片
                            EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_CARD_STATE, Memo.class));
                            //单条记录，直接添加可撤销态卡片视图
                            EventBus.getDefault().post(new ChatMsgEvent(null, new TaskCard<>(delMemo, TaskCard.TaskState.ACTIVE), null, null));
                            //将处于可撤销状态的卡片设为已删除状态
                            EventBus.getDefault().post(new UpdateTaskCardEvent<>(delMemo, TaskCard.TaskState.DELETED));
                            break;
                        case RobotConstant.RETURN:
                            if (actions.length() == 1) {
                                Memo rMemo = mAssistDao.findMemoBySid(lastTarget.getString("sid"));
                                //从数据库中恢复上一次删除的备忘记录
                                mAssistDao.insertMemo(rMemo);
                                 /* 单条记录，直接添加完成态卡片视图 */
                                EventBus.getDefault().post(new ChatMsgEvent(null, new TaskCard<>(rMemo, TaskCard.TaskState.ACTIVE), null, null));
                                EventBus.getDefault().post(new UpdateTaskCardEvent<>(rMemo, TaskCard.TaskState.ACTIVE));
                            }
                            break;
                        case RobotConstant.MODIFY:
                            MemoEntity mMemoEntity = SyncSegment.fromJson(lastTarget.toString(), MemoEntity.class);
                            Memo mMemo = mAssistDao.findMemoBySid(mMemoEntity.getSid());
                            mMemo.setContent(mMemoEntity.getContent());
                            mMemo.setTimestamp(mMemoEntity.getTimestamp());
                            mMemo.setModified(mMemoEntity.getModified());
                            mAssistDao.updateMemo(mMemo);
                            //让上一个卡片处于作废状态
                            EventBus.getDefault().post(new UpdateTaskCardEvent<>(mMemo, TaskCard.TaskState.INVALID));
                            EventBus.getDefault().post(new ChatMsgEvent(null, new TaskCard<>(mMemo, TaskCard.TaskState.ACTIVE), null, null));
                            break;
                        case RobotConstant.VIEW:
                            List<TaskCard> memoCards = new ArrayList<>();
                            for (int i = 0; i < actions.length(); i++) {
                                JSONObject target = actions.getJSONObject(i).getJSONObject("target");
                                Memo vMemo = mAssistDao.findMemoBySid(target.getString("sid"));
                                memoCards.add(new TaskCard<>(vMemo, TaskCard.TaskState.ACTIVE));
                            }
                            EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_LIST_STATE, Memo.class));
                            EventBus.getDefault().post(new ChatMsgEvent(null, new TaskCard<>(memoCards), null, null));
                            break;
                        case RobotConstant.CANCEL:
                            is_memo_edit = false;
                            switchRecordMode(false, IflyRecognizer.CREATE_MEMO_MODE);
                            EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_CARD_STATE, Memo.class));
                            break;
                        case RobotConstant.READ:
                            StringBuilder sb = new StringBuilder(text);
                            sb.append("\n");
                            for (int i = 0; i < actions.length(); i++) {
                                JSONObject target = actions.getJSONObject(i).getJSONObject("target");
                                String content = target.getString("content");
                                sb.append("第").append(i + 1).append("条：").append(content).append("\n");
                            }
                            text = sb.deleteCharAt(sb.length() - 1).toString();
                            builder.setText(text);
                            showAndSpeak(builder);
                            break;
                    }
                    break;
                case RobotConstant.ACTION_ACCOUNTING:   //记账部分
                    switch (action) {
                        case RobotConstant.CREATE:
                            if (is_account_edit)
                                EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_CARD_STATE, Accounting.class));
                            is_account_edit = true;
                            if (actions.length() > 1) {    //同时新建多条记账
                                is_account_edit = false;
                                List<TaskCard> accountCards = new ArrayList<>();
                                for (int i = 0; i < actions.length(); i++) {
                                    Accounting accounting = new Accounting();
                                    JSONObject target = actions.getJSONObject(i).getJSONObject("target");
                                    BillEntity billEntity = SyncSegment.fromJson(target.toString(), BillEntity.class);
                                    accounting.fromBill(billEntity);
                                    mAssistDao.insertAccount(accounting);
                                    accountCards.add(new TaskCard<>(accounting, TaskCard.TaskState.ACTIVE));
                                }
                                countBalance(AccountingActivity.TYPE_ADD, accountCards);
                                EventBus.getDefault().post(new ChatMsgEvent(null, new TaskCard(accountCards), null, null));
                            } else {     //单条记账
                                Accounting accounting = new Accounting();
                                BillEntity billEntity = SyncSegment.fromJson(lastTarget.toString(), BillEntity.class);
                                accounting.fromBill(billEntity);
                                if (accounting.getAmount() > 0 && !TextUtils.isEmpty(accounting.getMemo()) && !TextUtils.isEmpty(accounting.getEtype())) {
                                    //账单信息完整，保存新建
                                    is_account_edit = false;
                                    mAssistDao.insertAccount(accounting);
                                    List<TaskCard> cardLists = new ArrayList<>();
                                    cardLists.add(new TaskCard<>(accounting, TaskCard.TaskState.ACTIVE));
                                    countBalance(AccountingActivity.TYPE_ADD, cardLists);
                                }
                                EventBus.getDefault().post(new ChatMsgEvent(null, new TaskCard<>(accounting, TaskCard.TaskState.ACTIVE), null, null));
                            }
                            break;
                        case RobotConstant.DELETE:
                            Accounting delAccount = mAssistDao.findAccountBySid(lastTarget.getString("sid"));
                            // 从数据库中删除账单记录
                            mAssistDao.deleteAccount(delAccount);
                            //移除上一个卡片
                            EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_CARD_STATE, Accounting.class));
                            //单条记录，直接添加可撤销态卡片视图
                            EventBus.getDefault().post(new ChatMsgEvent(null, new TaskCard<>(delAccount, TaskCard.TaskState.ACTIVE), null, null));
                            //将处于可撤销状态的卡片设为已删除状态
                            EventBus.getDefault().post(new UpdateTaskCardEvent<>(delAccount, TaskCard.TaskState.DELETED));
                            break;
                        case RobotConstant.RETURN:
                            if (actions.length() == 1) {
                                Accounting rAccount = mAssistDao.findAccountBySid(lastTarget.getString("sid"));
                                //从数据库中恢复上一次删除的备忘记录
                                mAssistDao.insertAccount(rAccount);
                                 /* 单条记录，直接添加完成态卡片视图 */
                                EventBus.getDefault().post(new ChatMsgEvent(null, new TaskCard<>(rAccount, TaskCard.TaskState.ACTIVE), null, null));
                                EventBus.getDefault().post(new UpdateTaskCardEvent<>(rAccount, TaskCard.TaskState.ACTIVE));
                            }
                            break;
                        case RobotConstant.MODIFY:
                            BillEntity billEntity = SyncSegment.fromJson(lastTarget.toString(), BillEntity.class);
                            Accounting uAccount = mAssistDao.findAccountBySid(billEntity.getSid());
                            //获取旧记录金额（用于重置余额）
                            double oldAmount = uAccount.getAmount();
                            if (uAccount.getAtype() == 0)
                                oldAmount = -oldAmount;
                            float balance = AppConfig.dPreferences.getFloat(AppConfig.ACCOUNT_AMOUNT, 0);
                            AppConfig.dPreferences.edit().putFloat(AppConfig.ACCOUNT_AMOUNT, (float) (balance - oldAmount)).commit();
                            uAccount.fromBill(billEntity);
                            mAssistDao.updateAccount(uAccount);
                            TaskCard<Accounting> taskCard = new TaskCard<>(uAccount, TaskCard.TaskState.ACTIVE);
                            List<TaskCard> list = new ArrayList<>();
                            list.add(taskCard);
                            countBalance(AccountingActivity.TYPE_UPDATE, list);
                            EventBus.getDefault().post(new UpdateTaskCardEvent<>(uAccount, TaskCard.TaskState.INVALID));
                            EventBus.getDefault().post(new ChatMsgEvent(null, taskCard, null, null));
                            break;
                        case RobotConstant.VIEW:
                            List<TaskCard> accountCards = new ArrayList<>();
                            for (int i = 0; i < actions.length(); i++) {
                                JSONObject target = actions.getJSONObject(i).getJSONObject("target");
                                Accounting vAccount = mAssistDao.findAccountBySid(target.getString("sid"));
                                accountCards.add(new TaskCard<>(vAccount, TaskCard.TaskState.ACTIVE));
                            }
                            EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_LIST_STATE, Accounting.class));
                            EventBus.getDefault().post(new ChatMsgEvent(null, new TaskCard(accountCards), null, null));
                            break;
                        case RobotConstant.CANCEL:
                            is_account_edit = false;
                            EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_CARD_STATE, Accounting.class));
                            break;
                        case RobotConstant.READ:
                            StringBuilder sb = new StringBuilder(text);
                            sb.append("\n");
                            for (int i = 0; i < actions.length(); i++) {
                                JSONObject target = actions.getJSONObject(i).getJSONObject("target");
                                BillEntity bill = SyncSegment.fromJson(target.toString(), BillEntity.class);
                                sb.append("第").append(i + 1).append("条：").append(bill.getPay() == 0 ? "支出" : "收入")
                                        .append(bill.getMoney()).append("元，项目是").append(bill.getType())
                                        .append(TextUtils.isEmpty(bill.getItem()) ? "" : "，" + bill.getItem()).append("\n");
                            }
                            text = sb.deleteCharAt(sb.length() - 1).toString();
                            builder.setText(text);
                            showAndSpeak(builder);
                            break;
                    }
                    break;
                case RobotConstant.ACTION_TAPE:     //录音部分
                    switch (action) {
                        case RobotConstant.DELETE:      //删除录音
                            for (int i = 0; i < actions.length(); i++) {
                                JSONObject target = actions.getJSONObject(i).getJSONObject("target");
                                Tape tape = TapeEntityDao.getInstance().findTapeSid(target.getString("sid"));
                                tape.setTimestamp(target.getLong("timestamp"));
                                TapeEntityDao.getInstance().deleteTape(tape);
                            }
                            break;
                        case RobotConstant.CANCEL:     //取消删除（无操作）

                            break;
                    }
                    break;
                case RobotConstant.ACTION_DIALOG:   //选择对话框部分
                    DialogEntity dialogEntity = JsonUtils.getObj(lastTarget.toString(), DialogEntity.class);
                    String[] data = dialogEntity.getData();
                    if (DialogEntity.DialogType.SINGLE.name().equals(dialogEntity.getType())) {
                        toggleDialog = new SingleChooseDialog(assistView, dialogEntity.getLabel(), data)
                                .SetOnChooseListener(new SingleChooseDialog.ChooseListener() {
                                    @Override
                                    public void onChoose(String content) {
                                        sendMessageToRobot(content);
                                    }

                                    @Override
                                    public void onCancel() {

                                    }
                                });
                        toggleDialog.show();
                    } else {
                        mMultiDialog = new MultiChoiceDialog(assistView, dialogEntity.getLabel(), data)
                                .SetOnChooseListener(new SingleChooseDialog.ChooseListener() {
                                    @Override
                                    public void onChoose(String content) {
                                        sendMessageToRobot(content);
                                    }

                                    @Override
                                    public void onCancel() {
                                        sendMessageToRobot("取消");
                                    }
                                });
                        mMultiDialog.show();
                    }
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 取消选择框
     **/
    @Override
    public void cancelToggleDialog() {
        if (toggleDialog != null && toggleDialog.isShowing()) {
            toggleDialog.cancel();
            toggleDialog = null;
        }
    }

    /**
     * 发送回复文本文本视图并合成声音
     **/
    private void showAndSpeak(SpeechMsgBuilder builder) {
        EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(builder.getText()), null, null, null));
        IflySynthesizer.getInstance().startSpeakAbsolute(builder.build())
                .doOnNext(new Consumer<SpeechMsg>() {
                    @Override
                    public void accept(SpeechMsg speechMsg) throws Exception {
                        if (speechMsg.state() == SpeechMsg.State.OnBegin)
                            EventBus.getDefault().post(new SynthesizeEvent(SynthesizeEvent.SYNTH_START));
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

    /**
     * 计算余额
     **/
    private void countBalance(final int type, final List<TaskCard> taskcards) {
        Single.just(0)
                .doOnSuccess(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        float balance = AppConfig.dPreferences.getFloat(AppConfig.ACCOUNT_AMOUNT, 0);
                        switch (type) {
                            case AccountingActivity.TYPE_ADD:
                            case AccountingActivity.TYPE_UPDATE:
                                for (TaskCard<Accounting> taskcard : taskcards) {
                                    if (taskcard.t.getAtype() == 0) {    //支出
                                        balance -= taskcard.t.getAmount();
                                    } else {
                                        balance += taskcard.t.getAmount();
                                    }
                                }
                                break;
                            case AccountingActivity.TYPE_DELETE:
                                for (TaskCard<Accounting> taskcard : taskcards) {
                                    if (taskcard.t.getAtype() == 0) {    //支出
                                        balance += taskcard.t.getAmount();
                                    } else {
                                        balance -= taskcard.t.getAmount();
                                    }
                                }
                                break;
                        }
                        AppConfig.dPreferences.edit().putFloat(AppConfig.ACCOUNT_AMOUNT, balance).commit();
                    }
                })
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    /**
     * 切换录音识别模式
     **/
    public void switchRecordMode(boolean mode, int long_record_mode) {
        if (IflyRecognizer.isInited()) {
            if (!mode) {
                Intent intent = new Intent(assistView, AssistantService.class);
                intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_RECOGNIZE);
                assistView.startService(intent);
            }
            IflyRecognizer.getInstance().setLong_record_mode(long_record_mode);
            IflyRecognizer.getInstance().setRecognizeMode(mode);
        }
    }

    /**
     * 通知提醒服务开/关提醒
     **/
    private void switchRemind(Remind remind, int cmd) {
        Intent rIntent = new Intent(assistView, RemindService.class);
        rIntent.putExtra(RemindService.CMD, (RemindService.REMIND << 4) + cmd);
        rIntent.putExtra(RemindService.ID, remind.getId());
        assistView.startService(rIntent);
    }

    /**
     * 通知提醒服务开/关闹钟
     **/
    private void switchAlarm(AlarmClock alarm, int cmd) {
        Intent rIntent = new Intent(assistView, RemindService.class);
        rIntent.putExtra(RemindService.CMD, (RemindService.ALARM << 4) + cmd);
        rIntent.putExtra(RemindService.ID, alarm.getId());
        assistView.startService(rIntent);
    }

    /**
     * 已结束任务，销毁对话框
     **/
    private void dismiss() {
        if (mMultiDialog != null && mMultiDialog.isShowing()) {
            mMultiDialog.cancel();
            mMultiDialog = null;
        }
    }

    @Override
    public void setMemoEditState(boolean isEdit) {
        is_memo_edit = isEdit;
    }

    @Override
    public void setAlarmEditState(boolean isEdit) {
        is_alarm_edit = isEdit;
    }

    @Override
    public void setRemindEditState(boolean isEdit) {
        is_remind_edit = isEdit;
    }

    @Override
    public void setAccountEditState(boolean isEdit) {
        is_account_edit = isEdit;
    }

/*    @Override
    public void onDialogCancel() {
        VoiceMediator.create(assistView).setRemindDialogFlag(0);
        EventBus.getDefault().post(new BottomBoxStateEvent(true));
    }

    @Override
    public void onDialogShow() {
        VoiceMediator.create(assistView).setRemindDialogFlag(1);
        EventBus.getDefault().post(new BottomBoxStateEvent(false));
    }*/

    /**
     * 向机器人发送语音信息
     **/
    private void sendMessageToRobot(String text) {
        Intent intent = new Intent(assistView, AssistantService.class);
        intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.SEND_TO_ROBOT);
        intent.putExtra(AssistantService.TEXT, text);
        intent.putExtra(AssistantService.INPUT_TYPE, AssistantService.INPUT_VOICE);
        assistView.startService(intent);
    }

    /**
     * 结束任务流
     **/
    private void sendMessageToRobotForEndTask() {
        Intent intent = new Intent(assistView, AssistantService.class);
        intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.SEND_TO_ROBOT_FOR_END_TASK);
        assistView.startService(intent);
    }

    @Override
    public void subscribe() {
        EventBus.getDefault().register(assistView);
    }

    @Override
    public void unsubscribe() {
        /** 音乐播放器在应用退出后应该以通知栏的形式存在，继续播放歌曲。
         * 由于没有实现通知栏，暂时设定为退出应用停止播放歌曲。 **/
        /*if (LingjuAudioPlayer.get() != null) {
            LingjuAudioPlayer.get().release();
        }*/
        Intent intent = new Intent(assistView, AssistantService.class);
        intent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.STOP_VOICE_MODE);
        assistView.startService(intent);
        EventBus.getDefault().unregister(assistView);
    }
}
