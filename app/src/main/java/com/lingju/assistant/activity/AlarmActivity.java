package com.lingju.assistant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.BaseAssistActivity;
import com.lingju.assistant.entity.TaskCard;
import com.lingju.assistant.service.RemindService;
import com.lingju.assistant.view.AlarmFrDialog;
import com.lingju.assistant.view.AlarmItemDialog;
import com.lingju.assistant.view.RingListDialog;
import com.lingju.assistant.view.SlidingItem;
import com.lingju.assistant.view.SwitchButton;
import com.lingju.model.AlarmClock;
import com.lingju.model.SimpleDate;
import com.lingju.model.dao.AssistDao;
import com.lingju.model.dao.AssistEntityDao;
import com.lingju.util.AssistUtils;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class AlarmActivity extends BaseAssistActivity {

    private List<TaskCard<AlarmClock>> alarmDatas = new ArrayList<>();
    private AlarmAdapter mAdapter;
    private int tomorrowCount;     //明天闹钟记录数
    private String alarmTime = "";   //闹钟时间文本
    private String desc = "";
    private String ring = "默认";
    private int alarmFr;
    private int week;
    private AssistEntityDao.AlarmEntityDao mAlarmEntityDao;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        initView();
        loadData();
        mAlarmEntityDao = AssistEntityDao.create().getDao(AssistEntityDao.AlarmEntityDao.class);
    }

    private void initView() {
        mIvHeader.setImageResource(R.drawable.pic_alarm_bar);
        mLlAccount.setVisibility(View.GONE);
        mRlDatetime.setVisibility(View.VISIBLE);
        mTbAssist.setTitle("闹钟");
        mTvDate.setText(new StringBuilder().append(mDateTimes[0]).append("     ").append(AssistUtils.getWeekDay()));
        mTvTime.setText(mDateTimes[1]);

        mAdapter = new AlarmAdapter();
        mAssistRecyclerview.setAdapter(mAdapter);
    }


    @Override
    protected void onAssistCreate() {
        moveToPosition(1);
        alarmDatas.add(0, new TaskCard<>(new AlarmClock(), TaskCard.TaskState.ACTIVE));
        mAdapter.notifyItemInserted(1);
        isEditPosition = 1;
    }

    @Override
    public boolean hadEdit() {
        if (isEditPosition != 0) {
            AlarmClock alarm = alarmDatas.get(isEditPosition - 1).t;
            return (!alarmTime.equals(alarm.getRtime() == 0 ? "" : new SimpleDate(alarm.getRtime()).toString())
                    || !desc.equals(alarm.getItem() == null ? "" : alarm.getItem())
                    || !ring.equals(alarm.getRing() == null ? "默认" : alarm.getRing())
                    || alarmFr != alarm.getFrequency());
        }
        return false;
    }

    @Override
    public void collapseCard(int position) {
        if (position != 0) {
            alarmTime = "";
            desc = "";
            ring = "默认";
            alarmFr = 0;
            AlarmClock alarm = alarmDatas.get(position - 1).t;
            if (alarm.getId() == null) {
                alarmDatas.remove(position - 1);
                mAdapter.notifyItemRemoved(position);
                mFabAdd.show();
            } else {
                View itemView = mLayoutManager.findViewByPosition(position);
                if (itemView != null) {
                    View editAlarmItem = itemView.findViewById(R.id.edit_alarm_item);
                    View delItem = itemView.findViewById(R.id.del_item);
                    editAlarmItem.setVisibility(View.GONE);
                    delItem.setVisibility(View.VISIBLE);
                }
            }
        }
    }


    /**
     * 加载数据，刷新视图
     **/
    private void loadData() {
        Flowable.create(new FlowableOnSubscribe<Object>() {
            @Override
            public void subscribe(FlowableEmitter<Object> e) throws Exception {
                reset();
                /* io线程加载数据库数据 */
                List<AlarmClock> alarmClocks = mAssistDao.findAllAlarmAsc(false);
                week = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
                week = week - 1 == 0 ? 7 : week - 1;    //计算当天是星期几
                week = week + 1 == 8 ? 1 : week + 1;    // 计算明天是星期几
                for (AlarmClock alarm : alarmClocks) {
                    TaskCard<AlarmClock> card = new TaskCard<>(alarm, TaskCard.TaskState.ACTIVE);
                    if (alarm.getValid() == 0)
                        card.taskState = TaskCard.TaskState.INVALID;
                    updateAlarmCount(AccountingActivity.TYPE_ADD, alarm);
                    alarmDatas.add(card);
                }
                e.onNext(0);
            }
        }, BackpressureStrategy.BUFFER)
                .subscribeOn(Schedulers.io())   //执行订阅（subscribe()）所在线程
                .doOnSubscribe(new Consumer<Subscription>() {
                    @Override
                    public void accept(Subscription subscription) throws Exception {
                        mCpbLoad.setVisibility(View.VISIBLE);
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())  //响应订阅（Sbscriber）所在线程
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {
                        /* 回到主线程刷新列表 */
                        mCpbLoad.setVisibility(View.GONE);
                        mAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void reset() {
        alarmDatas.clear();
        tomorrowCount = 0;
    }

    private void updateAlarmCount(int type, AlarmClock alarm) {
        List<AlarmClock> alarms = new ArrayList<>();
        if (type == AccountingActivity.TYPE_UPDATE) {
            tomorrowCount = 0;
            alarms.addAll(mAssistDao.findAllAlarmAsc(false));
        } else
            alarms.add(alarm);
        for (AlarmClock alarmClock : alarms) {
            if (alarmClock.getValid() == 1) {
                if (!alarmClock.getRepeat() && new SimpleDate().toValue() >= alarmClock.getRtime()) {
                    if (type == AccountingActivity.TYPE_DELETE)
                        tomorrowCount--;
                    else
                        tomorrowCount++;
                } else if (alarmClock.getRepeat()) {
                    int[] weekDays = AssistUtils.transalteWeekDays(alarmClock.getFrequency(true));
                    for (int weekday : weekDays) {
                        if (weekday == week) {
                            if (type == AccountingActivity.TYPE_DELETE)
                                tomorrowCount--;
                            else
                                tomorrowCount++;
                            break;
                        }
                    }
                }
            }
        }
    }

    class AlarmAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RecyclerView.ViewHolder holder;
            View itemView;
            if (viewType == HEADER_VIEW) {
                itemView = mInflater.inflate(R.layout.task_list_header, parent, false);
                holder = new HeaderHolder(itemView);
            } else {
                itemView = mInflater.inflate(R.layout.item_alarmlist_view, parent, false);
                holder = new AlarmHolder(itemView);
            }
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderHolder) {
                ((HeaderHolder) holder).mTvHeader.setText("您明天有" + tomorrowCount + "个闹钟");
            } else {
                AlarmHolder alarmHolder = (AlarmHolder) holder;
                TaskCard<AlarmClock> taskCard = alarmDatas.get(position - 1);
                alarmHolder.mDashDivider.setVisibility(View.GONE);
                if (position == isEditPosition) {       //编辑状态视图
                    alarmHolder.mDelItem.setVisibility(View.GONE);
                    alarmHolder.mTvState.setVisibility(View.GONE);
                    alarmHolder.mEditAlarmItem.setVisibility(View.VISIBLE);
                    alarmHolder.mTvCancel.setVisibility(View.VISIBLE);
                    alarmHolder.mTvSave.setVisibility(View.VISIBLE);
                    alarmHolder.mEditAlarmTime.setText(TextUtils.isEmpty(alarmTime) ? new SimpleDate().toString() : alarmTime);
                    alarmHolder.mEditAlarmDesc.setText(TextUtils.isEmpty(desc) ? "闹钟" : desc);
                    alarmHolder.mEditAlarmRing.setText(TextUtils.isEmpty(ring) ? "默认" : ring);
                    alarmHolder.mEditAlarmFr.setText(AssistUtils.transalteWeekDayString(taskCard.t.getRepeat() ? alarmFr : 0));
                    alarmHolder.mTvSave.setText(taskCard.t.getId() == null ? "创建" : "保存");
                } else {     //完成状态视图
                    alarmHolder.mTvCancel.setVisibility(View.GONE);
                    alarmHolder.mTvSave.setVisibility(View.GONE);
                    alarmHolder.mEditAlarmItem.setVisibility(View.GONE);
                    alarmHolder.mDelItem.setVisibility(View.VISIBLE);
                    alarmHolder.mAlarmTime.setText(new SimpleDate(taskCard.t.getRtime()).toString());
                    alarmHolder.mAlarmDesc.setText(taskCard.t.getItem());
                    alarmHolder.mAlarmFr.setText(AssistUtils.transalteWeekDayString(taskCard.t.getFrequency()));
                    alarmHolder.mAlarmSwitchBtn.setChecked(taskCard.t.getValid() == 1);
                    refreshCard(taskCard.taskState, alarmHolder.mTvState, alarmHolder.mAlarmTime, alarmHolder.mDelItem, alarmHolder.mAlarmSwitchBtn);
                }
            }
        }

        @Override
        public int getItemCount() {
            return 1 + alarmDatas.size();
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? HEADER_VIEW : CONTENT_VIEW;
        }

        /**
         * 头部视图
         **/
        class HeaderHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView mTvHeader;

            public HeaderHolder(View itemView) {
                super(itemView);
                mTvHeader = (TextView) itemView.findViewById(R.id.tv_header);
                mTvHeader.setOnClickListener(this);
            }

            /**
             * 点击滚动到明天闹钟的第一条记录
             **/
            @Override
            public void onClick(View v) {
                out:
                for (int i = 0; i < alarmDatas.size(); i++) {
                    TaskCard<AlarmClock> taskCard = alarmDatas.get(i);
                    if (!taskCard.t.getRepeat() && new SimpleDate().toValue() >= taskCard.t.getRtime()) {
                        moveToPosition(i + 1);
                        break out;
                    } else if (taskCard.t.getRepeat()) {
                        int[] weekDays = AssistUtils.transalteWeekDays(taskCard.t.getFrequency(true));
                        in:
                        for (int weekday : weekDays) {
                            if (weekday == week) {
                                moveToPosition(i + 1);
                                break out;
                            }
                        }
                    }
                }
            }
        }

        /**
         * 闹钟记录视图
         **/
        class AlarmHolder extends RecyclerView.ViewHolder implements SlidingItem.OnSlidingItemListener, TimePickerDialog.OnTimeSetListener {

            @BindView(R.id.alarm_time)
            TextView mAlarmTime;
            @BindView(R.id.alarm_switch_btn)
            SwitchButton mAlarmSwitchBtn;
            @BindView(R.id.alarm_desc)
            TextView mAlarmDesc;
            @BindView(R.id.alarm_fr)
            TextView mAlarmFr;
            @BindView(R.id.del_item)
            SlidingItem mDelItem;
            @BindView(R.id.edit_alarm_time)
            TextView mEditAlarmTime;
            @BindView(R.id.edit_alarm_desc)
            TextView mEditAlarmDesc;
            @BindView(R.id.edit_alarm_ring)
            TextView mEditAlarmRing;
            @BindView(R.id.edit_alarm_fr)
            TextView mEditAlarmFr;
            @BindView(R.id.tv_save)
            TextView mTvSave;
            @BindView(R.id.tv_cancel)
            TextView mTvCancel;
            @BindView(R.id.edit_alarm_item)
            LinearLayout mEditAlarmItem;
            @BindView(R.id.tv_state)
            TextView mTvState;
            @BindView(R.id.dash_divider)
            View mDashDivider;

            public AlarmHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
                mDelItem.setOnSlidingItemListener(this);
            }

            @OnClick({R.id.ll_edit_time, R.id.ll_edit_desc, R.id.ll_edit_ring, R.id.ll_edit_fr,
                    R.id.tv_save, R.id.tv_cancel, R.id.alarm_switch_btn, R.id.tv_state})
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.ll_edit_time:
                        SimpleDate rTime = new SimpleDate(mEditAlarmTime.getText().toString());
                        TimePickerDialog timePicker = TimePickerDialog.newInstance(
                                this,
                                rTime.getHour(),
                                rTime.getMinute(),
                                true);
                        timePicker.setAccentColor(getResources().getColor(R.color.base_blue));
                        timePicker.show(getFragmentManager(), "TimePicker");
                        break;
                    case R.id.ll_edit_desc:
                        AlarmItemDialog itemDialog = new AlarmItemDialog(AlarmActivity.this, mEditAlarmDesc.getText().toString(), new AlarmItemDialog.OnItemSelectedListener() {
                            @Override
                            public void onSelected(String item) {
                                mEditAlarmDesc.setText(item);
                                desc = item;
                            }
                        });
                        itemDialog.show();
                        break;
                    case R.id.ll_edit_ring:
                        final AlarmClock alarm = alarmDatas.get(getAdapterPosition() - 1).t;
                        new RingListDialog(AlarmActivity.this, mEditAlarmRing.getText().toString(), alarm.getPath())
                                .setOnRingSelectedListener(new RingListDialog.OnRingSelectedListener() {
                                    @Override
                                    public void onSelected(String ring, String path) {
                                        mEditAlarmRing.setText(ring);
                                        alarm.setPath(path);
                                        AlarmActivity.this.ring = ring;
                                    }
                                }).show();
                        break;
                    case R.id.ll_edit_fr:
                        final AlarmClock eAlarm = alarmDatas.get(getAdapterPosition() - 1).t;
                        AlarmFrDialog alarmFrDialog = new AlarmFrDialog(AlarmActivity.this, eAlarm.getFrequency(true), eAlarm.getRepeat(), new AlarmFrDialog.OnResultListener() {
                            @Override
                            public void onResult(int fr, boolean repeat) {
                                mEditAlarmFr.setText(AssistUtils.transalteWeekDayString(repeat ? fr : 0));
                                eAlarm.setFrequency(fr);
                                eAlarm.setRepeat(repeat);
                                alarmFr = fr;
                            }
                        });
                        alarmFrDialog.show();
                        break;
                    case R.id.tv_save:
                        reset();
                        mDelItem.setVisibility(View.VISIBLE);
                        mTvCancel.setVisibility(View.GONE);
                        mTvSave.setVisibility(View.GONE);
                        mEditAlarmItem.setVisibility(View.GONE);

                        AlarmClock sAlarm = alarmDatas.get(getAdapterPosition() - 1).t;
                        String time = mEditAlarmTime.getText().toString();
                        String desc = mEditAlarmDesc.getText().toString();
                        mAlarmTime.setText(time);
                        mAlarmTime.setTextColor(getResources().getColor(R.color.new_text_color_first));
                        mAlarmDesc.setText(desc);
                        mAlarmFr.setText(AssistUtils.transalteWeekDayString(sAlarm.getFrequency()));
                        mAlarmSwitchBtn.setChecked(true);
                        //保存闹钟记录
                        sAlarm.setRtime(new SimpleDate(time).toValue());
                        sAlarm.setRing(mEditAlarmRing.getText().toString());
                        sAlarm.setItem(desc);
                        sAlarm.setValid(1);
                        AssistUtils.setAlarmRdate(sAlarm);
                        sAlarm.setSynced(false);
                        if (sAlarm.getId() == null) {
                            sAlarm.setCreated(new Date());
                            mAssistDao.insertAlarm(sAlarm);
                            //创建完成，添加图标重新显示
                            mFabAdd.show();
                            updateAlarmCount(AccountingActivity.TYPE_ADD, sAlarm);
                        } else {
                            switchAlarm(sAlarm, RemindService.CANCEL);
                            mAssistDao.updateAlarm(sAlarm);
                            updateAlarmCount(AccountingActivity.TYPE_UPDATE, sAlarm);
                        }
                        //开启闹钟服务
                        switchAlarm(sAlarm, RemindService.ADD);
                        notifyItemChanged(0);
                        //与服务器同步数据
                        AssistEntityDao.create().sync(mAlarmEntityDao);
                        break;
                    case R.id.tv_cancel:
                        reset();
                        AlarmClock cAlarm = alarmDatas.get(getAdapterPosition() - 1).t;
                        if (cAlarm.getId() == null) {   //取消新建
                            alarmDatas.remove(getAdapterPosition() - 1);
                            notifyItemRemoved(getAdapterPosition());
                            //取消新建，添加图标重新显示
                            mFabAdd.show();
                        } else {        //取消编辑
                            mDelItem.setVisibility(View.VISIBLE);
                            mTvCancel.setVisibility(View.GONE);
                            mTvSave.setVisibility(View.GONE);
                            mEditAlarmItem.setVisibility(View.GONE);
                        }
                        break;
                    case R.id.alarm_switch_btn:
                        mDelItem.hide();
                        TaskCard<AlarmClock> card = alarmDatas.get(getAdapterPosition() - 1);
                        AlarmClock swiAlarm = card.t;
                        swiAlarm.setValid(mAlarmSwitchBtn.isChecked() ? 1 : 0);
                        card.taskState = mAlarmSwitchBtn.isChecked() ? TaskCard.TaskState.ACTIVE : TaskCard.TaskState.INVALID;
                        notifyItemChanged(getAdapterPosition());
                        if (swiAlarm.getValid() == 1){
                            AssistUtils.setAlarmRdate(swiAlarm);
                            swiAlarm.setSynced(false);
                        }
                        AssistDao.getInstance().updateAlarm(swiAlarm);
                        Intent aIntent = new Intent(AlarmActivity.this, RemindService.class);
                        aIntent.putExtra(RemindService.CMD, (RemindService.ALARM << 4) + (mAlarmSwitchBtn.isChecked() ? RemindService.ADD : RemindService.CANCEL));
                        aIntent.putExtra(RemindService.ID, swiAlarm.getId());
                        AlarmActivity.this.startService(aIntent);
                        updateAlarmCount(AccountingActivity.TYPE_UPDATE, swiAlarm);
                        notifyItemChanged(0);
                        if (swiAlarm.getValid() == 1) {      //同步修改响铃日期
                            AssistEntityDao.create().sync(mAlarmEntityDao);
                        }
                        break;
                    case R.id.tv_state:
                        TaskCard<AlarmClock> taskCard = alarmDatas.get(getAdapterPosition() - 1);
                        if (taskCard.taskState == TaskCard.TaskState.REVOCABLE) {
                            taskCard.taskState = TaskCard.TaskState.ACTIVE;
                            taskCard.t.setValid(1);
                            taskCard.t.setSynced(false);
                            mAssistDao.insertAlarm(taskCard.t);
                            switchAlarm(taskCard.t, RemindService.ADD);
                            notifyItemChanged(getAdapterPosition());
                            updateAlarmCount(AccountingActivity.TYPE_ADD, taskCard.t);
                            notifyItemChanged(0);
                            //与服务器同步数据
                            AssistEntityDao.create().sync(mAlarmEntityDao);
                        }
                        break;
                }
            }

            private void reset() {
                alarmTime = "";
                desc = "";
                ring = "默认";
                alarmFr = 0;
                isEditPosition = 0;
            }

            @Override
            public void onSliding(SlidingItem item) {
                if (lastItem != null && lastItem != item)
                    lastItem.hide();
            }

            @Override
            public void onBtnClick(View v) {
                //移除可撤销状态记录
                for (int i = 0; i < alarmDatas.size(); i++) {
                    if (alarmDatas.get(i).taskState == TaskCard.TaskState.REVOCABLE) {
                        if (isEditPosition > i + 1)
                            isEditPosition--;
                        alarmDatas.remove(i);
                        notifyItemRemoved(i + 1);
                        break;      //结束循环
                    }
                }
                TaskCard<AlarmClock> taskCard = alarmDatas.get(getAdapterPosition() - 1);
                //修改状态
                taskCard.taskState = TaskCard.TaskState.REVOCABLE;
                //删除记录
                taskCard.t.setSynced(false);
                mAssistDao.deleteAlarm(taskCard.t);
                //刷新视图
                updateAlarmCount(AccountingActivity.TYPE_DELETE, taskCard.t);
                notifyItemChanged(0);
                notifyItemChanged(getAdapterPosition());
                //与服务器同步数据
                AssistEntityDao.create().sync(mAlarmEntityDao);
            }

            @Override
            public void onContentClick(View v) {
                if (isEditPosition != 0 && hadEdit()) {
                    showEditTips();
                    return;
                }
                collapseCard(isEditPosition);
                isEditPosition = getAdapterPosition();
                AlarmClock alarm = alarmDatas.get(getAdapterPosition() - 1).t;
                mDelItem.setVisibility(View.GONE);
                mEditAlarmItem.setVisibility(View.VISIBLE);
                mTvCancel.setVisibility(View.VISIBLE);
                mTvSave.setVisibility(View.VISIBLE);
                mEditAlarmTime.setText(new SimpleDate(alarm.getRtime()).toString());
                mEditAlarmDesc.setText(TextUtils.isEmpty(alarm.getItem()) ? "闹钟" : alarm.getItem());
                mEditAlarmRing.setText(TextUtils.isEmpty(alarm.getRing()) ? "默认" : alarm.getRing());
                mEditAlarmFr.setText(AssistUtils.transalteWeekDayString(alarm.getFrequency()));
                mTvSave.setText("保存");
                alarmTime = mEditAlarmTime.getText().toString();
                desc = mEditAlarmDesc.getText().toString();
                ring = mEditAlarmRing.getText().toString();
                alarmFr = alarm.getFrequency();
            }

            @Override
            public void onExpanded(SlidingItem item) {
                lastItem = item;
            }

            @Override
            public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
                String hour = hourOfDay >= 10 ? "" + hourOfDay : "0" + hourOfDay;
                String min = minute >= 10 ? "" + minute : "0" + minute;
                mEditAlarmTime.setText(hour + ":" + min);
                alarmTime = mEditAlarmTime.getText().toString();
            }
        }
    }

    /**
     * 通知提醒服务开/关闹钟
     **/
    private void switchAlarm(AlarmClock alarm, int cmd) {
        Intent rIntent = new Intent(this, RemindService.class);
        rIntent.putExtra(RemindService.CMD, (RemindService.ALARM << 4) + cmd);
        rIntent.putExtra(RemindService.ID, alarm.getId());
        startService(rIntent);
    }
}
