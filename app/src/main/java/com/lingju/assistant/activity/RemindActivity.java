package com.lingju.assistant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.BaseAssistActivity;
import com.lingju.assistant.activity.index.IRemind;
import com.lingju.assistant.activity.index.presenter.RemindPresenter;
import com.lingju.assistant.activity.index.view.ChatListFragment;
import com.lingju.assistant.entity.TaskCard;
import com.lingju.assistant.service.RemindService;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.assistant.view.RemindFrDialog;
import com.lingju.assistant.view.SlidingItem;
import com.lingju.assistant.view.VoiceInputComponent;
import com.lingju.model.Remind;
import com.lingju.model.SimpleDate;
import com.lingju.model.dao.AssistEntityDao;
import com.lingju.util.AssistUtils;
import com.lingju.util.TimeUtils;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2016/12/19.
 */
public class RemindActivity extends BaseAssistActivity implements IRemind.IRemindView {

    private IRemind.IPresenter mPresenter;
    private RemindAdapter mRemindAdapter;
    private boolean moveable;   //是否移动标记
    private int remindFr;
    private String dateText = "";
    private String timeText = "";
    private String remindContent = "";
    private AssistEntityDao.RemindEntityDao mRemindEntityDao;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPresenter = new RemindPresenter(this);
        showDateTimeMsg();
        initView();
        mRemindEntityDao = AssistEntityDao.create().getDao(AssistEntityDao.RemindEntityDao.class);
    }

    @Override
    protected void onAssistCreate() {
        moveToPosition(1);
        mPresenter.getShowDatas().add(0, new TaskCard<>(new Remind(), TaskCard.TaskState.ACTIVE));
        mRemindAdapter.notifyItemInserted(1);
        isEditPosition = 1;
    }

    @Override
    public boolean hadEdit() {
        if (isEditPosition != 0) {
            Remind remind = mPresenter.getShowDatas().get(isEditPosition - 1).t;
            return (!remindContent.equals(remind.getContent() == null ? "" : remind.getContent())
                    || !timeText.equals(TextUtils.isEmpty(remind.getRtime()) ? "" : remind.getRtime())
                    || !dateText.equals(remind.getRdate() == null ? "" : TimeUtils.format(remind.getRdate()))
                    || remindFr != remind.getFrequency());
        } else {
            return false;
        }
    }

    @Override
    public void collapseCard(int position) {
        if (position != 0) {
            timeText = "";
            dateText = "";
            remindContent = "";
            remindFr = 0;
            Remind cRemind = mPresenter.getShowDatas().get(position - 1).t;
            if (cRemind.getId() == null) {      //新建取消，移除视图
                mPresenter.getShowDatas().remove(position - 1);
                mRemindAdapter.notifyItemRemoved(position);
                //取消新建，添加图标重新显示
                mFabAdd.show();
            } else {        //隐藏编辑视图
                View itemView = mLayoutManager.findViewByPosition(position);
                if (itemView != null) {
                    View delItem = itemView.findViewById(R.id.del_item);
                    View editRemindItem = itemView.findViewById(R.id.edit_remind_item);
                    VoiceInputComponent vicInput = (VoiceInputComponent) itemView.findViewById(R.id.vic_input);
                    editRemindItem.setVisibility(View.GONE);
                    delItem.setVisibility(View.VISIBLE);
                    vicInput.stopRecord();
                }
            }
        }
    }

    @Override
    protected void onCancel() {
        notifyListView();
    }

    @Override
    protected void onDeleteChecked() {
        notifyListView();
        // delete();
        /* 移除选择项 */
        Set<Map.Entry<Integer, TaskCard>> entrySet = checkMap.entrySet();
        for (Map.Entry<Integer, TaskCard> entry : entrySet) {
            entry.getValue().taskState = TaskCard.TaskState.REVOCABLE;
            mRemindAdapter.notifyItemChanged(entry.getKey());
        }
        checkMap.clear();
    }

    @Override
    protected void onEdit() {
        notifyListView();
    }


    @Override
    protected void onDownRefresh() {
        boolean hasData = mPresenter.loadDatas(pageNum++);
       /* mLayoutRefresh.setRefreshing(false);*/
        if (!hasData)
            //Toast.makeText(this, "没有更多数据了", Toast.LENGTH_SHORT).show();
            Snackbar.make(mAssistRecyclerview, "没有更多数据了", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void showDateTimeMsg() {
        mIvHeader.setImageResource(R.drawable.pic_remind_bar);
        android.support.design.widget.CollapsingToolbarLayout.LayoutParams params = (CollapsingToolbarLayout.LayoutParams) mIvHeader.getLayoutParams();
        params.rightMargin = 0;
        params.topMargin = 16;
        mIvHeader.setLayoutParams(params);
        mLlAccount.setVisibility(View.GONE);
        mRlDatetime.setVisibility(View.VISIBLE);
        mTbAssist.setTitle("提醒");
        mTvDate.setText(new StringBuilder().append(mDateTimes[0]).append("     ").append(AssistUtils.getWeekDay()));
        mTvTime.setText(mDateTimes[1]);
    }

    @Override
    public void initView() {
        mRemindAdapter = new RemindAdapter();
        mAssistRecyclerview.setAdapter(mRemindAdapter);
        mPresenter.initDatas(getIntent().getLongExtra("id", 0));
    }

    @Override
    public void notifyListView() {
        mRemindAdapter.notifyDataSetChanged();
    }

    @Override
    public void showProgressBar() {
        mCpbLoad.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgressBar() {
        mCpbLoad.setVisibility(View.GONE);
    }

    class RemindAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RecyclerView.ViewHolder holder;
            View itemView;
            if (viewType == HEADER_VIEW) {
                itemView = mInflater.inflate(R.layout.task_list_header, parent, false);
                holder = new HeaderHolder(itemView);
            } else {
                itemView = mInflater.inflate(R.layout.item_remind_view, parent, false);
                holder = new RemindHolder(itemView);
            }
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderHolder) {
                ((HeaderHolder) holder).mTvHeader.setText("您今天有" + mPresenter.getTodayCount() + "条提醒");
            } else {
                TaskCard<Remind> taskCard = mPresenter.getShowDatas().get(position - 1);
                Remind remind = taskCard.t;
                RemindHolder remindHolder = (RemindHolder) holder;
                if (position == isEditPosition) {       //新建编辑态
                    remindHolder.mDelItem.setVisibility(View.GONE);
                    remindHolder.mTvState.setVisibility(View.GONE);
                    remindHolder.mCbDel.setVisibility(View.GONE);
                    remindHolder.mEditRemindItem.setVisibility(View.VISIBLE);
                    remindHolder.mTvCancel.setVisibility(View.VISIBLE);
                    remindHolder.mTvSave.setVisibility(View.VISIBLE);
                    remindHolder.mEditRemindContent.setText(remindContent);
                    remindHolder.mEditRemindDate.setText(TextUtils.isEmpty(dateText) ? TimeUtils.format(new Date()) : dateText);
                    remindHolder.mEditRemindTime.setText(TextUtils.isEmpty(timeText) ? new SimpleDate().toString() : timeText);
                    remindHolder.mEditRemindFr.setText(AssistUtils.translateRemindFrequency(remindFr));
                    if (remind.getId() == null) {
                        remindHolder.mTvSave.setText("创建");
                    } else {
                        remindHolder.mTvSave.setText("保存");

                    }
                } else {        //完成态视图
                    remindHolder.mDelItem.setVisibility(View.VISIBLE);
                    remindHolder.mTvCancel.setVisibility(View.GONE);
                    remindHolder.mTvSave.setVisibility(View.GONE);
                    remindHolder.mEditRemindItem.setVisibility(View.GONE);
                    remindHolder.mRemindContent.setText(remind.getContent());
                    remindHolder.mRemindContent.setTextColor(getResources().getColor(R.color.new_text_color_first));
                    Calendar cl = Calendar.getInstance();
                    cl.setTime(remind.getRdate());
                    remindHolder.mRemindDatetime.setText(new StringBuilder().append(remind.getFrequency() == 0 ? TimeUtils.format(remind.getRdate()) : AssistUtils.translateRemindFrequency(remind.getFrequency(), cl))
                            .append("    ").append(new SimpleDate(remind.getRtime()).toString()).toString());
                    refreshCard(taskCard.taskState, remindHolder.mTvState, remindHolder.mRemindContent, remindHolder.mDelItem, null);
                    /*if (mCurrentState == STATE_DELETE && taskCard.taskState != TaskCard.TaskState.REVOCABLE) {
                        remindHolder.mCbDel.setVisibility(View.VISIBLE);
                        remindHolder.mCbDel.setChecked(checkMap.containsKey(position));
                        showKeyboard(remindHolder.mDelItem, false);
                    } else {
                        remindHolder.mCbDel.setVisibility(View.GONE);
                    }
                    if (init) {
                        initWidth = remindHolder.mDelItem.getParentWidth();
                        if (initWidth != 0)
                            init = false;
                    }
                    remindHolder.mDelItem.resetParentWidth(initWidth);*/
                    // remindFr = taskCard.t.getFrequency();
                }
            }
        }

        @Override
        public int getItemCount() {
            return mPresenter.getShowDatas().size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? HEADER_VIEW : CONTENT_VIEW;
        }

        class HeaderHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView mTvHeader;

            public HeaderHolder(View itemView) {
                super(itemView);
                mTvHeader = (TextView) itemView.findViewById(R.id.tv_header);
                mTvHeader.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                mPresenter.scrollToTodayFirst();
            }
        }

        class RemindHolder extends RecyclerView.ViewHolder implements SlidingItem.OnSlidingItemListener, DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener, VoiceInputComponent.OnResultListener {
            @BindView(R.id.cb_del)
            CheckBox mCbDel;
            @BindView(R.id.remind_datetime)
            TextView mRemindDatetime;
            @BindView(R.id.remind_content)
            TextView mRemindContent;
            @BindView(R.id.del_item)
            SlidingItem mDelItem;
            @BindView(R.id.edit_remind_content)
            EditText mEditRemindContent;
            @BindView(R.id.vic_input)
            VoiceInputComponent mVicInput;
            @BindView(R.id.edit_remind_date)
            TextView mEditRemindDate;
            @BindView(R.id.edit_remind_time)
            TextView mEditRemindTime;
            @BindView(R.id.edit_remind_fr)
            TextView mEditRemindFr;
            @BindView(R.id.tv_save)
            TextView mTvSave;
            @BindView(R.id.tv_cancel)
            TextView mTvCancel;
            @BindView(R.id.edit_remind_item)
            LinearLayout mEditRemindItem;
            @BindView(R.id.tv_state)
            TextView mTvState;
            @BindView(R.id.til_remind)
            TextInputLayout mTilRemind;

            public RemindHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
                mDelItem.setOnSlidingItemListener(this);
                mVicInput.setOnResultListener(this);
                mEditRemindContent.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        remindContent = mEditRemindContent.getText().toString();
                        if (TextUtils.isEmpty(remindContent.trim())) {
                            mTvSave.setTextColor(getResources().getColor(R.color.forbid_click_color));
                        } else {
                            mTvSave.setTextColor(getResources().getColor(R.color.white));
                        }
                    }
                });
                mCbDel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = RemindHolder.this.getAdapterPosition();
                        mCbDel.setChecked(mCbDel.isChecked());
                        if (mCbDel.isChecked())
                            checkMap.put(position, mPresenter.getShowDatas().get(position - 1));
                        else
                            checkMap.remove(position);
                    }
                });
            }

            @OnClick({R.id.edit_remind_date_box, R.id.edit_remind_time, R.id.ll_remind_fr,
                    R.id.tv_save, R.id.tv_cancel, R.id.tv_state})
            public void onClick(View view) {
                mVicInput.stopRecord();
                switch (view.getId()) {
                    case R.id.edit_remind_date_box:
                        Date rDate = TimeUtils.parse(mEditRemindDate.getText().toString());
                        Calendar now = Calendar.getInstance();
                        now.setTime(rDate);
                        //将当前日期传入日期选择器，并显示
                        DatePickerDialog datePicker = DatePickerDialog.newInstance(
                                this,
                                now.get(Calendar.YEAR),
                                now.get(Calendar.MONTH),
                                now.get(Calendar.DAY_OF_MONTH)
                        );
                        datePicker.setAccentColor(getResources().getColor(R.color.base_blue));
                        datePicker.show(getFragmentManager(), "DatePicker");
                        break;
                    case R.id.edit_remind_time:
                        SimpleDate rTime = new SimpleDate(mEditRemindTime.getText().toString());
                        TimePickerDialog timePicker = TimePickerDialog.newInstance(
                                this,
                                rTime.getHour(),
                                rTime.getMinute(),
                                true);
                        timePicker.setAccentColor(getResources().getColor(R.color.base_blue));
                        timePicker.show(getFragmentManager(), "TimePicker");
                        break;
                    case R.id.ll_remind_fr:
                        final Remind rRemind = (Remind) ((TaskCard) mPresenter.getShowDatas().get(getAdapterPosition() - 1)).t;
                        Calendar cl = Calendar.getInstance();
                        if (rRemind.getRdate() != null)
                            cl.setTime(rRemind.getRdate());
                        RemindFrDialog frDialog = new RemindFrDialog(RemindActivity.this, rRemind.getFrequency(), cl, new RemindFrDialog.OnResultListener() {
                            @Override
                            public void onResult(int fr) {
                                remindFr = fr;
                                // rRemind.setFrequency(fr);
                                mEditRemindFr.setText(AssistUtils.translateRemindFrequency(fr));
                            }
                        });
                        frDialog.show();
                        break;
                    case R.id.tv_save:
                        saveHandle();
                        break;
                    case R.id.tv_cancel:
                        cancelHandle();
                        break;
                    case R.id.tv_state:
                        TaskCard<Remind> taskCard = mPresenter.getShowDatas().get(getAdapterPosition() - 1);
                        if (taskCard.taskState == TaskCard.TaskState.REVOCABLE) {    //处于可撤销状态，点击撤销删除
                            if (taskCard.t.getRdate().compareTo(TimeUtils.getTodayDate()) != -1 && taskCard.t.getRdate().before(TimeUtils.getTomorrow())) {
                                mPresenter.setTodayCount(mPresenter.getTodayCount() + 1);
                                notifyItemChanged(0);
                            }
                            taskCard.taskState = TaskCard.TaskState.ACTIVE;
                            if (taskCard.t.getFrequency() == 0 && taskCard.t.getRdate().before(new Date())) {
                                taskCard.taskState = TaskCard.TaskState.INVALID;
                            }
                            taskCard.t.setSynced(false);
                            mPresenter.operateData(IRemind.INSERT_TYPE, taskCard.t);
                            mPresenter.switchRemind(taskCard.t, RemindService.ADD);
                            notifyItemChanged(getAdapterPosition());
                            //与服务器同步数据
                            AssistEntityDao.create().sync(mRemindEntityDao);
                        }
                        break;
                }
            }

            private void cancelHandle() {
                isEditPosition = 0;
                timeText = "";
                dateText = "";
                remindContent = "";
                remindFr = 0;
                Remind cRemind = (Remind) ((TaskCard) mPresenter.getShowDatas().get(getAdapterPosition() - 1)).t;
                if (cRemind.getId() == null) {      //新建取消，移除视图
                    mPresenter.getShowDatas().remove(getAdapterPosition() - 1);
                    notifyItemRemoved(getAdapterPosition());
                    //取消新建，添加图标重新显示
                    mFabAdd.show();
                } else {        //隐藏编辑视图
                    mDelItem.setVisibility(View.VISIBLE);
                    mEditRemindItem.setVisibility(View.GONE);
                }
            }

            private void saveHandle() {
                String content = mEditRemindContent.getText().toString().trim();
                if (TextUtils.isEmpty(content)) {
                    mTilRemind.setErrorEnabled(true);
                    mTilRemind.setError("提醒内容不能为空");
                    if (mTilRemind.getEditText().getBackground() != null)
                        mTilRemind.getEditText().getBackground().clearColorFilter();
                    return;
                }
                mTilRemind.setError("");
                mTilRemind.setErrorEnabled(false);
                isEditPosition = 0;
                timeText = "";
                dateText = "";
                remindContent = "";
                mDelItem.setVisibility(View.VISIBLE);
                mEditRemindItem.setVisibility(View.GONE);
                //保存记录
                Remind sRemind = (Remind) ((TaskCard) mPresenter.getShowDatas().get(getAdapterPosition() - 1)).t;
                mRemindContent.setText(content);
                sRemind.setContent(content);
                sRemind.setFrequency(remindFr);
                String date = mEditRemindDate.getText().toString();
                String time = mEditRemindTime.getText().toString();
                sRemind.setRdate(TimeUtils.parseDateTime(date + " " + time));
                sRemind.setRtime(time);
                Calendar cl = Calendar.getInstance();
                cl.setTime(sRemind.getRdate());
                mRemindDatetime.setText(new StringBuilder().append(remindFr == 0 ? date : AssistUtils.translateRemindFrequency(remindFr, cl)).append("    ").append(time).toString());
                remindFr = 0;
                sRemind.setValid(1);
                sRemind.setSynced(false);
                if (sRemind.getId() == null) {
                    if (sRemind.getRdate().compareTo(TimeUtils.getTodayDate()) != -1 && sRemind.getRdate().before(TimeUtils.getTomorrow())) {
                        mPresenter.setTodayCount(mPresenter.getTodayCount() + 1);
                        notifyItemChanged(0);
                    }
                    sRemind.setCreated(new Date());
                    mPresenter.operateData(IRemind.INSERT_TYPE, sRemind);
                    //创建完成，添加图标重新显示
                    mFabAdd.show();
                } else {
                    mPresenter.switchRemind(sRemind, RemindService.CANCEL);
                    mPresenter.operateData(IRemind.UPDATE_TYPE, sRemind);
                }
                mPresenter.switchRemind(sRemind, RemindService.ADD);
                //与服务器同步数据
                AssistEntityDao.create().sync(mRemindEntityDao);
            }

            @Override
            public void onSliding(SlidingItem item) {
                if (lastItem != null && lastItem != item)
                    lastItem.hide();
            }

            @Override
            public void onBtnClick(View v) {
                List<TaskCard<Remind>> showDatas = mPresenter.getShowDatas();
                for (int i = 0; i < showDatas.size(); i++) {
                    if (showDatas.get(i).taskState == TaskCard.TaskState.REVOCABLE) {
                        if (isEditPosition > i + 1)
                            isEditPosition--;
                        showDatas.remove(i);
                        notifyItemRemoved(i + 1);
                        break;      //结束循环
                    }
                }
                TaskCard<Remind> taskCard = mPresenter.getShowDatas().get(getAdapterPosition() - 1);
                if (taskCard.t.getRdate().compareTo(TimeUtils.getTodayDate()) != -1 && taskCard.t.getRdate().before(TimeUtils.getTomorrow())) {
                    mPresenter.setTodayCount(mPresenter.getTodayCount() - 1);
                    notifyItemChanged(0);
                }
                //修改状态
                taskCard.taskState = TaskCard.TaskState.REVOCABLE;
                //删除记录
                taskCard.t.setSynced(false);
                mPresenter.operateData(IRemind.DELETE_TYPE, taskCard.t);
                mPresenter.switchRemind(taskCard.t, RemindService.CANCEL);
                //刷新视图
                notifyItemChanged(getAdapterPosition());
                //与服务器同步数据
                AssistEntityDao.create().sync(mRemindEntityDao);
            }

            @Override
            public void onContentClick(View v) {
                /*if (mDelItem.isExanded()) {
                    mDelItem.hide();
                    return;
                }*/
                if (isEditPosition != 0 && hadEdit()) {
                    showEditTips();
                    return;
                }
                collapseCard(isEditPosition);
                isEditPosition = getAdapterPosition();
                /* 显示编辑态视图 */
                Remind remind = mPresenter.getShowDatas().get(getAdapterPosition() - 1).t;
                mDelItem.setVisibility(View.GONE);
                mEditRemindItem.setVisibility(View.VISIBLE);
                mTvCancel.setVisibility(View.VISIBLE);
                mTvSave.setVisibility(View.VISIBLE);
                mEditRemindContent.setText(remind.getContent());
                mEditRemindContent.setSelection(mEditRemindContent.length());
                mEditRemindDate.setText(TimeUtils.format(remind.getRdate()));
                mEditRemindTime.setText(new SimpleDate(remind.getRtime()).toString());
                mEditRemindFr.setText(AssistUtils.translateRemindFrequency(remind.getFrequency()));
                mTvSave.setText("保存");
                timeText = mEditRemindTime.getText().toString();
                dateText = mEditRemindDate.getText().toString();
                remindContent = mEditRemindContent.getText().toString();
                remindFr = remind.getFrequency();
            }

            @Override
            public void onExpanded(SlidingItem item) {
                lastItem = item;
            }

            @Override
            public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                String month = monthOfYear + 1 < 10 ? "0" + (monthOfYear + 1) : "" + (monthOfYear + 1);
                String day = dayOfMonth < 10 ? "0" + dayOfMonth : "" + dayOfMonth;
                mEditRemindDate.setText(new StringBuilder().append(year).append("年").append(month).append("月").append(day).append("日").toString());
                dateText = mEditRemindDate.getText().toString();
            }

            @Override
            public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
                String hour = hourOfDay >= 10 ? "" + hourOfDay : "0" + hourOfDay;
                String min = minute >= 10 ? "" + minute : "0" + minute;
                mEditRemindTime.setText(hour + ":" + min);
                timeText = mEditRemindTime.getText().toString();
            }

            @Override
            public void onResult(String text) {
                if (text.length() < 6 && (text.startsWith(ChatListFragment.SAVE_KEYWORDS[0])
                        || text.startsWith(ChatListFragment.SAVE_KEYWORDS[1])
                        || text.startsWith(ChatListFragment.SAVE_KEYWORDS[2]))) {     //保存
                    mVicInput.stopRecord();
                    saveHandle();
                } else if (text.length() < 6 && (text.startsWith(ChatListFragment.QUIT_KEYWORDS[0])
                        || text.startsWith(ChatListFragment.QUIT_KEYWORDS[1]))) {     //取消
                    mVicInput.stopRecord();
                    cancelHandle();
                } else {
                    mEditRemindContent.getText().insert(mEditRemindContent.getSelectionStart(), text);
                    mEditRemindContent.setSelection(mEditRemindContent.length());
                }
            }

            @Override
            public void onError(int errorCode, String description) {
                new CommonDialog(RemindActivity.this, "温馨提示", description, "确定").show();
            }
        }

    }
}
