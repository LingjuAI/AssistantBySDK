package com.lingju.assistant.activity.index.model;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.AccountingActivity;
import com.lingju.assistant.activity.event.UpdateTaskCardEvent;
import com.lingju.assistant.entity.TaskCard;
import com.lingju.assistant.service.RemindService;
import com.lingju.assistant.view.AlarmFrDialog;
import com.lingju.assistant.view.AlarmItemDialog;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.assistant.view.ItemExpenseDialog;
import com.lingju.assistant.view.ItemIncomeDialog;
import com.lingju.assistant.view.MemoEditDialog;
import com.lingju.assistant.view.RemindFrDialog;
import com.lingju.assistant.view.RingListDialog;
import com.lingju.assistant.view.SlidingItem;
import com.lingju.assistant.view.SwitchButton;
import com.lingju.model.Accounting;
import com.lingju.model.AlarmClock;
import com.lingju.model.Memo;
import com.lingju.model.Remind;
import com.lingju.model.SimpleDate;
import com.lingju.model.dao.AssistDao;
import com.lingju.model.dao.AssistEntityDao;
import com.lingju.util.AssistUtils;
import com.lingju.util.TimeUtils;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ken on 2016/12/15.
 */
public class TaskListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Observer {
    private final static int HEADER_VIEW = 0;
    private final static int ITEM_VIEW = 1;
    private List<TaskCard> mdatas;
    private Context mContext;
    private LayoutInflater mInflater;
    private final AssistDao mAssistDao;
    private int remindFr;   //提醒周期
    private SlidingItem lastItem;
    private int editPosition = -1;
    private TaskListObservable mObservable;
    private boolean isEdit;
    private RecyclerView.ViewHolder mEditHolder;
    private String lastRemindContent = "";  //用于卡片刚展开时提醒的内容
    private String lastMemoContent = "";    //用于卡片刚展开时备忘的内容
    private String lastAmount = "";         //记录展开时的金额
    private String lastMemo = "";           //记录展开时的账单备注

    public TaskListAdapter(Context context, List<TaskCard> datas) {
        this.mContext = context;
        this.mdatas = datas;
        mInflater = LayoutInflater.from(mContext);
        mAssistDao = AssistDao.getInstance();
        mObservable = new TaskListObservable();
        mObservable.addObserver(this);
    }

    public void setDatas(List<TaskCard> datas) {
        this.mdatas = datas;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder;
        View item;
        if (viewType == HEADER_VIEW) {
            item = mInflater.inflate(R.layout.item_list_header, parent, false);
            holder = new headerHolder(item);
        } else {
            TaskCard taskCard = mdatas.get(0);
            if (taskCard.t instanceof Remind) {
                item = mInflater.inflate(R.layout.item_remindlist_view, null, false);
                holder = new RemindListHolder(item);
            } else if (taskCard.t instanceof AlarmClock) {
                item = mInflater.inflate(R.layout.item_alarmlist_view, null, false);
                holder = new AlarmListHolder(item);
            } else if (taskCard.t instanceof Memo) {
                item = mInflater.inflate(R.layout.item_memolist_view, null, false);
                holder = new MemoListHolder(item);
            } else {
                item = mInflater.inflate(R.layout.item_accountlist_view, null, false);
                holder = new AccountListHolder(item);
            }
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        editPosition = -1;
        TaskCard taskCard = position == 0 ? mdatas.get(0) : mdatas.get(position - 1);

        if (holder instanceof RemindListHolder) {
            RemindListHolder listHolder = (RemindListHolder) holder;
            Remind itemRemind = (Remind) taskCard.t;
            listHolder.mDashDivider.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
            listHolder.mDelItem.setVisibility(View.VISIBLE);
            listHolder.mEditRemindItem.setVisibility(View.GONE);
            listHolder.mRemindContent.setText(itemRemind.getContent());
            Calendar cl = Calendar.getInstance();
            cl.setTime(itemRemind.getRdate());
            listHolder.mRemindDatetime.setText(new StringBuilder().append(itemRemind.getFrequency() == 0 ? TimeUtils.format(itemRemind.getRdate()) : AssistUtils.translateRemindFrequency(itemRemind.getFrequency(), cl))
                    .append("    ").append(new SimpleDate(itemRemind.getRtime()).toString()).toString());
            updateCardItem(taskCard.taskState, listHolder.mDelItem, listHolder.mTvState, null);
        } else if (holder instanceof AlarmListHolder) {
            AlarmListHolder alarmHolder = (AlarmListHolder) holder;
            AlarmClock alarm = (AlarmClock) taskCard.t;
            alarmHolder.mDashDivider.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
            alarmHolder.mDelItem.setVisibility(View.VISIBLE);
            alarmHolder.mEditAlarmItem.setVisibility(View.GONE);
            alarmHolder.mAlarmTime.setText(new SimpleDate(alarm.getRtime()).toString());
            alarmHolder.mAlarmDesc.setText(alarm.getItem());
            alarmHolder.mAlarmFr.setText(AssistUtils.transalteWeekDayString(alarm.getFrequency()));
            alarmHolder.mAlarmSwitchBtn.setChecked(alarm.getValid() == 1);
            updateCardItem(taskCard.taskState, alarmHolder.mDelItem, alarmHolder.mTvState, alarmHolder.mAlarmSwitchBtn);
        } else if (holder instanceof MemoListHolder) {
            MemoListHolder memoHolder = (MemoListHolder) holder;
            Memo memo = (Memo) taskCard.t;
            memoHolder.mDelItem.setVisibility(View.VISIBLE);
            memoHolder.mEditMemoItem.setVisibility(View.GONE);
            memoHolder.mMemoContent.setText(memo.getContent());
            updateCardItem(taskCard.taskState, memoHolder.mDelItem, memoHolder.mTvState, null);
        } else if (holder instanceof AccountListHolder) {
            AccountListHolder accountHolder = (AccountListHolder) holder;
            Accounting accounting = (Accounting) taskCard.t;
            accountHolder.mDashDivider.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
            accountHolder.mDelItem.setVisibility(View.VISIBLE);
            accountHolder.mEditAccountItem.setVisibility(View.GONE);
            accountHolder.mAccountEtype.setText(accounting.getEtype());
            accountHolder.mAccountDate.setText(TimeUtils.formatDate(accounting.getRdate()));
            if (accounting.getAtype() == 0) {
                accountHolder.mAccountAmount.setTextColor(mContext.getResources().getColor(R.color.green));
                accountHolder.mAccountAmount.setText("-" + AssistUtils.formatAmount(accounting.getAmount()));
            } else {
                accountHolder.mAccountAmount.setTextColor(mContext.getResources().getColor(R.color.red_style));
                accountHolder.mAccountAmount.setText("+" + AssistUtils.formatAmount(accounting.getAmount()));
            }
            updateCardItem(taskCard.taskState, accountHolder.mDelItem, accountHolder.mTvState, null);
        } else {
            String title;
            if (taskCard.t instanceof Remind)
                title = "提醒";
            else if (taskCard.t instanceof Memo)
                title = "备忘";
            else if (taskCard.t instanceof AlarmClock)
                title = "闹钟";
            else
                title = "记账";
            ((headerHolder) holder).mTvListHeader.setText(title);
        }
    }

    @Override
    public int getItemCount() {
        if (mdatas == null || mdatas.size() == 0) {
            return 0;
        } else {
            return mdatas.size() + 1;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? HEADER_VIEW : ITEM_VIEW;
    }

    /**
     * 设置可滑动item焦点
     **/
    private void setFocus(SlidingItem item, boolean focus) {
        item.setSlidable(focus);
        item.setClickable(focus);
    }

    private void showTips() {
        new CommonDialog(mContext, "温馨提示", "请先保存已打开的卡片！", "我知道了").show();
    }

    private void updateCardItem(int taskState, SlidingItem item, TextView tvState, SwitchButton sb) {
        if (sb != null)
            sb.setFocusable(false);
        tvState.setVisibility(View.VISIBLE);
        setFocus(item, false);
        switch (taskState) {
            case TaskCard.TaskState.ACTIVE:
                tvState.setVisibility(View.GONE);
                setFocus(item, true);
                if (sb != null)
                    sb.setFocusable(true);
                break;
            case TaskCard.TaskState.REVOCABLE:
                tvState.setText("撤销");
                tvState.setTextColor(mContext.getResources().getColor(R.color.base_blue));
                break;
            case TaskCard.TaskState.DELETED:
                tvState.setText("已删除");
                tvState.setTextColor(mContext.getResources().getColor(R.color.new_text_color_first));
                break;
            case TaskCard.TaskState.INVALID:
                tvState.setText("已作废");
                tvState.setTextColor(mContext.getResources().getColor(R.color.new_text_color_first));
                break;
        }
    }

    class headerHolder extends RecyclerView.ViewHolder {
        TextView mTvListHeader;

        public headerHolder(View itemView) {
            super(itemView);
            mTvListHeader = (TextView) itemView.findViewById(R.id.tv_list_header);
        }
    }

    class AccountListHolder extends RecyclerView.ViewHolder implements SlidingItem.OnSlidingItemListener, DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

        @BindView(R.id.edit_account_amount)
        TextInputLayout mEditAccountAmount;
        @BindView(R.id.edit_account_atype)
        TextView mEditAccountAtype;
        @BindView(R.id.edit_account_etype)
        TextView mEditAccountEtype;
        @BindView(R.id.edit_account_date)
        TextView mEditAccountDate;
        @BindView(R.id.edit_account_time)
        TextView mEditAccountTime;
        @BindView(R.id.edit_account_memo)
        EditText mEditAccountMemo;
        @BindView(R.id.edit_account_item)
        LinearLayout mEditAccountItem;
        @BindView(R.id.account_amount)
        TextView mAccountAmount;
        @BindView(R.id.account_etype)
        TextView mAccountEtype;
        @BindView(R.id.del_item)
        SlidingItem mDelItem;
        @BindView(R.id.tv_state)
        TextView mTvState;
        @BindView(R.id.account_date)
        TextView mAccountDate;
        @BindView(R.id.tv_save)
        TextView mTvSave;
        @BindView(R.id.dash_divider)
        View mDashDivider;
        private AssistEntityDao.BillEntityDao mBillEntityDao;

        public AccountListHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            mDelItem.setOnSlidingItemListener(this);
            mBillEntityDao = AssistEntityDao.create().getDao(AssistEntityDao.BillEntityDao.class);
            final EditText etContent = mEditAccountAmount.getEditText();
            if (etContent != null) {
                etContent.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        String amount = etContent.getText().toString();
                        if (!lastAmount.equals(amount))
                            mObservable.notifyObservers(true);
                        if (TextUtils.isEmpty(amount) || !AssistUtils.isNumber(amount) || Double.valueOf(amount) == 0)
                            mTvSave.setTextColor(mContext.getResources().getColor(R.color.forbid_click_color));
                        else
                            mTvSave.setTextColor(mContext.getResources().getColor(R.color.white));
                    }
                });
            }
            mEditAccountMemo.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (!lastMemo.equals(s.toString()))
                        mObservable.notifyObservers(true);
                }
            });
        }

        @OnClick({R.id.ll_account_atype, R.id.ll_account_etype, R.id.edit_account_date_box, R.id.edit_account_time,
                R.id.tv_cancel, R.id.tv_save, R.id.tv_state})
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.ll_account_atype:
                    Accounting aAccount = (Accounting) mdatas.get(getAdapterPosition() - 1).t;
                    mObservable.notifyObservers(true);
                    if (aAccount.getAtype() == 0) {   //支出
                        aAccount.setAtype(1);
                        mEditAccountAtype.setText("收入");
                    } else {
                        aAccount.setAtype(0);
                        mEditAccountAtype.setText("支出");
                    }
                    break;
                case R.id.ll_account_etype:
                    Accounting eAccount = (Accounting) mdatas.get(getAdapterPosition() - 1).t;
                    String cEtype = mEditAccountEtype.getText().toString();
                    mObservable.notifyObservers(true);
                    if (eAccount.getAtype() == 0) {
                        ItemExpenseDialog expenseDialog = new ItemExpenseDialog(mContext, cEtype, new ItemExpenseDialog.OnItemExpenseListener() {
                            @Override
                            public void onItemSelected(String item) {
                                mEditAccountEtype.setText(item);
                            }
                        });
                        expenseDialog.show();
                    } else {
                        ItemIncomeDialog incomeDialog = new ItemIncomeDialog(mContext, cEtype, new ItemIncomeDialog.OnItemIncomeListener() {
                            @Override
                            public void onIncomeSelected(String item) {
                                mEditAccountEtype.setText(item);
                            }
                        });
                        incomeDialog.show();
                    }
                    break;
                case R.id.edit_account_date_box:
                    Date rDate = TimeUtils.parse(mEditAccountDate.getText().toString());
                    Calendar now = Calendar.getInstance();
                    now.setTime(rDate);
                    //将当前日期传入日期选择器，并显示
                    DatePickerDialog datePicker = DatePickerDialog.newInstance(
                            this,
                            now.get(Calendar.YEAR),
                            now.get(Calendar.MONTH),
                            now.get(Calendar.DAY_OF_MONTH)
                    );
                    datePicker.setAccentColor(mContext.getResources().getColor(R.color.base_blue));
                    datePicker.show(((Activity) mContext).getFragmentManager(), "DatePicker");
                    break;
                case R.id.edit_account_time:
                    SimpleDate rTime = new SimpleDate(mEditAccountTime.getText().toString());
                    TimePickerDialog timePicker = TimePickerDialog.newInstance(
                            this,
                            rTime.getHour(),
                            rTime.getMinute(),
                            true);
                    timePicker.setAccentColor(mContext.getResources().getColor(R.color.base_blue));
                    timePicker.show(((Activity) mContext).getFragmentManager(), "TimePicker");
                    break;
                case R.id.tv_cancel:
                    editPosition = -1;
                    lastMemo = "";
                    lastAmount = "";
                    mObservable.notifyObservers(false);
                    mDelItem.setVisibility(View.VISIBLE);
                    mEditAccountItem.setVisibility(View.GONE);
                    break;
                case R.id.tv_save:
                    String amount = mEditAccountAmount.getEditText().getText().toString();
                    Double money;
                    if (TextUtils.isEmpty(amount) || !AssistUtils.isNumber(amount) || (money = Double.valueOf(amount)) == 0) {
                        mEditAccountAmount.setErrorEnabled(true);
                        mEditAccountAmount.setError("请输入金额");
                        if (mEditAccountAmount.getEditText().getBackground() != null)
                            mEditAccountAmount.getEditText().getBackground().clearColorFilter();
                        return;
                    }
                    editPosition = -1;
                    lastMemo = "";
                    lastAmount = "";
                    mObservable.notifyObservers(false);
                    mEditAccountAmount.setError("");
                    mEditAccountAmount.setErrorEnabled(false);
                    mDelItem.setVisibility(View.VISIBLE);
                    mEditAccountItem.setVisibility(View.GONE);
                    //保存记录，设置完成视图数据
                    TaskCard card = mdatas.get(getAdapterPosition() - 1);
                    Accounting sAccount = (Accounting) card.t;
                    //记录旧金额
                    double oldAmount = sAccount.getAmount();
                    sAccount.setAmount(money);
                    //重置余额
                    if (card.atype == 0)
                        oldAmount = -oldAmount;
                    card.atype = sAccount.getAtype();
                    float balance = AppConfig.dPreferences.getFloat(AppConfig.ACCOUNT_AMOUNT, 0);
                    AppConfig.dPreferences.edit().putFloat(AppConfig.ACCOUNT_AMOUNT, (float) (balance - oldAmount)).commit();
                    if (sAccount.getAtype() == 0) {
                        mAccountAmount.setTextColor(mContext.getResources().getColor(R.color.green));
                        mAccountAmount.setText("-" + AssistUtils.formatAmount(money));
                    } else {
                        mAccountAmount.setTextColor(mContext.getResources().getColor(R.color.red_style));
                        mAccountAmount.setText("+" + AssistUtils.formatAmount(money));
                    }
                    String etype = mEditAccountEtype.getText().toString();
                    mAccountEtype.setText(etype);
                    sAccount.setEtype(etype);
                    sAccount.setMemo(mEditAccountMemo.getText().toString());
                    String date = mEditAccountDate.getText().toString();
                    String time = mEditAccountTime.getText().toString();
                    sAccount.setRdate(TimeUtils.parseDateTime(date + " " + time));
                    mAccountDate.setText(TimeUtils.formatDate(sAccount.getRdate()));
                    Calendar cl = Calendar.getInstance();
                    cl.setTime(sAccount.getRdate());
                    sAccount.setMonth(cl.get(Calendar.MONTH) + 1);
                    sAccount.setModified(new Date());
                    sAccount.setSynced(false);
                    mAssistDao.updateAccount(sAccount);
                    EventBus.getDefault().post(new UpdateTaskCardEvent<>(sAccount, true));
                    List<TaskCard<Accounting>> list = new ArrayList<>();
                    list.add(card);
                    countBalance(AccountingActivity.TYPE_UPDATE, list);
                    //同步账单记录
                    AssistEntityDao.create().sync(mBillEntityDao);
                    break;
                case R.id.tv_state:
                    TaskCard taskCard = mdatas.get(getAdapterPosition() - 1);
                    if (taskCard.taskState == TaskCard.TaskState.REVOCABLE) {
                        taskCard.taskState = TaskCard.TaskState.ACTIVE;
                        ((Accounting)taskCard.t).setSynced(false);
                        mAssistDao.insertAccount((Accounting) taskCard.t);
                        EventBus.getDefault().post(new UpdateTaskCardEvent<>(taskCard.t, TaskCard.TaskState.ACTIVE));
                        List<TaskCard<Accounting>> lists = new ArrayList<>();
                        lists.add(taskCard);
                        countBalance(AccountingActivity.TYPE_ADD, lists);
                        //同步账单记录
                        AssistEntityDao.create().sync(mBillEntityDao);
                    }
                    break;
            }
        }

        @Override
        public void onSliding(SlidingItem item) {
            if (lastItem != null && lastItem != item)
                lastItem.hide();
        }

        @Override
        public void onBtnClick(View v) {
            TaskCard taskCard = mdatas.get(getAdapterPosition() - 1);
            ((Accounting)taskCard.t).setSynced(false);
            mAssistDao.deleteAccount((Accounting) taskCard.t);
            EventBus.getDefault().post(new UpdateTaskCardEvent<>(taskCard.t, TaskCard.TaskState.DELETED));
            List<TaskCard<Accounting>> list = new ArrayList<>();
            list.add(taskCard);
            countBalance(AccountingActivity.TYPE_DELETE, list);
            AssistEntityDao.create().sync(mBillEntityDao);
        }

        @Override
        public void onContentClick(View v) {
            if (editPosition != -1 && isEdit) {
                showTips();
                return;
            }
            collapseCard();
            mEditHolder = this;
            editPosition = getAdapterPosition();
            Accounting accounting = (Accounting) mdatas.get(getAdapterPosition() - 1).t;
            lastAmount = String.valueOf(accounting.getAmount());
            lastMemo = TextUtils.isEmpty(accounting.getMemo()) ? "" : accounting.getMemo();
            mDelItem.setVisibility(View.GONE);
            mEditAccountItem.setVisibility(View.VISIBLE);
            mEditAccountAmount.getEditText().setText(accounting.getAmount() + "");
            if (accounting.getAtype() == 0) {
                mEditAccountAtype.setText("支出");
            } else {
                mEditAccountAtype.setText("收入");
            }
            mEditAccountEtype.setText(accounting.getEtype());
            String dateTime = TimeUtils.formatDateTime(accounting.getRdate());
            String[] datetimes = dateTime.split(" ");
            mEditAccountDate.setText(datetimes[0]);
            mEditAccountTime.setText(datetimes[1]);
            mEditAccountMemo.setText(accounting.getMemo());
        }

        @Override
        public void onExpanded(SlidingItem item) {
            lastItem = item;
        }

        @Override
        public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
            mObservable.notifyObservers(true);
            String month = monthOfYear + 1 < 10 ? "0" + (monthOfYear + 1) : "" + (monthOfYear + 1);
            String day = dayOfMonth < 10 ? "0" + dayOfMonth : "" + dayOfMonth;
            mEditAccountDate.setText(new StringBuilder().append(year).append("年").append(month).append("月").append(day).append("日").toString());
        }

        @Override
        public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
            mObservable.notifyObservers(true);
            String hour = hourOfDay >= 10 ? "" + hourOfDay : "0" + hourOfDay;
            String min = minute >= 10 ? "" + minute : "0" + minute;
            mEditAccountTime.setText(hour + ":" + min);
        }

        /**
         * 计算余额
         **/
        private void countBalance(final int type, final List<TaskCard<Accounting>> taskcards) {
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
    }

    class MemoListHolder extends RecyclerView.ViewHolder implements SlidingItem.OnSlidingItemListener, MemoEditDialog.OnMemoEditListener {

        @BindView(R.id.memo_content)
        TextView mMemoContent;
        @BindView(R.id.del_item)
        SlidingItem mDelItem;
        @BindView(R.id.edit_memo_time)
        TextView mEditMemoTime;
        @BindView(R.id.edit_memo_count)
        TextView mEditMemoCount;
        @BindView(R.id.edit_memo_content)
        EditText mEditMemoContent;
        @BindView(R.id.edit_memo_item)
        LinearLayout mEditMemoItem;
        @BindView(R.id.tv_state)
        TextView mTvState;
        @BindView(R.id.tv_save)
        TextView mTvSave;
        private AssistEntityDao.MemoEntityDao mMemoEntityDao;

        public MemoListHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            mDelItem.setOnSlidingItemListener(this);
            mMemoEntityDao = AssistEntityDao.create().getDao(AssistEntityDao.MemoEntityDao.class);
            mEditMemoContent.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    mEditMemoCount.setText("字数" + s.length());
                    String content = mEditMemoContent.getText().toString().trim();
                    if (!lastMemoContent.equals(content))
                        mObservable.notifyObservers(true);
                    if (TextUtils.isEmpty(content))
                        mTvSave.setTextColor(mContext.getResources().getColor(R.color.forbid_click_color));
                    else
                        mTvSave.setTextColor(mContext.getResources().getColor(R.color.base_blue));
                }
            });
        }

        @OnClick({R.id.edit_memo_full_screen, R.id.tv_cancel, R.id.tv_save, R.id.tv_state})
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.edit_memo_full_screen:
                    MemoEditDialog memoEditDialog = new MemoEditDialog(mContext, mEditMemoContent.getText().toString(),
                            mEditMemoTime.getText().toString(), false, this);
                    memoEditDialog.show();
                    break;
                case R.id.tv_cancel:
                    cancelHandle();
                    break;
                case R.id.tv_save:
                    saveHandle();
                    break;
                case R.id.tv_state:
                    TaskCard taskCard = mdatas.get(getAdapterPosition() - 1);
                    if (taskCard.taskState == TaskCard.TaskState.REVOCABLE) {
                        taskCard.taskState = TaskCard.TaskState.ACTIVE;
                        ((Memo) taskCard.t).setSynced(false);
                        mAssistDao.insertMemo((Memo) taskCard.t);
                        EventBus.getDefault().post(new UpdateTaskCardEvent<>(taskCard.t, TaskCard.TaskState.ACTIVE));
                        //与服务器同步数据
                        AssistEntityDao.create().sync(mMemoEntityDao);
                    }
                    break;
            }
        }

        private void cancelHandle() {
            editPosition = -1;
            lastMemoContent = "";
            mObservable.notifyObservers(false);
            mDelItem.setVisibility(View.VISIBLE);
            mEditMemoItem.setVisibility(View.GONE);
        }

        private void saveHandle() {
            String content = mEditMemoContent.getText().toString().trim();
            if (TextUtils.isEmpty(content))
                return;
            cancelHandle();
            mMemoContent.setText(content);
            //保存记录
            Memo memo = (Memo) mdatas.get(getAdapterPosition() - 1).t;
            memo.setContent(content);
            memo.setModified(new Date());
            memo.setSynced(false);
            mAssistDao.updateMemo(memo);
            EventBus.getDefault().post(new UpdateTaskCardEvent<>(memo, true));
            //与服务器同步数据
            AssistEntityDao.create().sync(mMemoEntityDao);
        }

        @Override
        public void onSliding(SlidingItem item) {
            if (lastItem != null && lastItem != item)
                lastItem.hide();
        }

        @Override
        public void onBtnClick(View v) {
            TaskCard<Memo> memoCard = mdatas.get(getAdapterPosition() - 1);
            memoCard.t.setSynced(false);
            mAssistDao.deleteMemo(memoCard.t);
            EventBus.getDefault().post(new UpdateTaskCardEvent<>(memoCard.t, TaskCard.TaskState.DELETED));
            //与服务器同步数据
            AssistEntityDao.create().sync(mMemoEntityDao);
        }

        @Override
        public void onContentClick(View v) {
            if (editPosition != -1 && isEdit) {
                showTips();
                return;
            }
            collapseCard();
            mEditHolder = this;
            editPosition = getAdapterPosition();
            Memo memo = (Memo) mdatas.get(getAdapterPosition() - 1).t;
            mDelItem.setVisibility(View.GONE);
            mEditMemoItem.setVisibility(View.VISIBLE);
            lastMemoContent = memo.getContent();
            mEditMemoContent.setText(memo.getContent());
            String time;
            if (memo.getModified() == null) {
                time = new SimpleDate().toString();
            } else {
                time = TimeUtils.getTime(memo.getModified());
            }
            mEditMemoTime.setText("今天" + time);
            mEditMemoCount.setText("字数" + memo.getContent().length());
        }

        @Override
        public void onExpanded(SlidingItem item) {
            lastItem = item;
        }

        @Override
        public void onCancel() {
            cancelHandle();
        }

        @Override
        public void onBack(String content) {
            mEditMemoContent.setText(content);
            mEditMemoContent.setSelection(content.length());
        }

        @Override
        public void onSave(String content) {
            mEditMemoContent.setText(content);
            saveHandle();
        }
    }

    class AlarmListHolder extends RecyclerView.ViewHolder implements SlidingItem.OnSlidingItemListener, TimePickerDialog.OnTimeSetListener {

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
        @BindView(R.id.edit_alarm_item)
        LinearLayout mEditAlarmItem;
        @BindView(R.id.tv_state)
        TextView mTvState;
        @BindView(R.id.dash_divider)
        View mDashDivider;
        private AssistEntityDao.AlarmEntityDao mAlarmEntityDao;

        public AlarmListHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            mDelItem.setOnSlidingItemListener(this);
            mAlarmEntityDao = AssistEntityDao.create().getDao(AssistEntityDao.AlarmEntityDao.class);
        }

        @OnClick({R.id.ll_edit_time, R.id.ll_edit_desc, R.id.ll_edit_ring, R.id.tv_state,
                R.id.ll_edit_fr, R.id.tv_cancel, R.id.tv_save, R.id.alarm_switch_btn})
        public void onClick(View view) {
            int position = getAdapterPosition();
            switch (view.getId()) {
                case R.id.ll_edit_time:
                    SimpleDate rTime = new SimpleDate(mEditAlarmTime.getText().toString());
                    TimePickerDialog timePicker = TimePickerDialog.newInstance(
                            this,
                            rTime.getHour(),
                            rTime.getMinute(),
                            true);
                    timePicker.setAccentColor(mContext.getResources().getColor(R.color.base_blue));
                    timePicker.show(((Activity) mContext).getFragmentManager(), "TimePicker");
                    break;
                case R.id.ll_edit_desc:
                    AlarmItemDialog itemDialog = new AlarmItemDialog(mContext, mEditAlarmDesc.getText().toString(), new AlarmItemDialog.OnItemSelectedListener() {
                        @Override
                        public void onSelected(String item) {
                            mEditAlarmDesc.setText(item);
                            mObservable.notifyObservers(true);
                        }
                    });
                    itemDialog.show();
                    break;
                case R.id.ll_edit_ring:
                    final AlarmClock alarm = (AlarmClock) mdatas.get(position - 1).t;
                    new RingListDialog(mContext, mEditAlarmRing.getText().toString(), alarm.getPath())
                            .setOnRingSelectedListener(new RingListDialog.OnRingSelectedListener() {
                                @Override
                                public void onSelected(String ring, String path) {
                                    mEditAlarmRing.setText(ring);
                                    alarm.setPath(path);
                                    mObservable.notifyObservers(true);
                                }
                            }).show();
                    break;
                case R.id.ll_edit_fr:
                    final AlarmClock eAlarm = (AlarmClock) mdatas.get(position - 1).t;
                    AlarmFrDialog alarmFrDialog = new AlarmFrDialog((Activity) mContext, eAlarm.getFrequency(true), eAlarm.getRepeat(), new AlarmFrDialog.OnResultListener() {
                        @Override
                        public void onResult(int fr, boolean repeat) {
                            mEditAlarmFr.setText(AssistUtils.transalteWeekDayString(repeat ? fr : 0));
                            eAlarm.setFrequency(fr);
                            eAlarm.setRepeat(repeat);
                            mObservable.notifyObservers(true);
                        }
                    });
                    alarmFrDialog.show();
                    break;
                case R.id.tv_cancel:
                    editPosition = -1;
                    mObservable.notifyObservers(false);
                    mDelItem.setVisibility(View.VISIBLE);
                    mEditAlarmItem.setVisibility(View.GONE);
                    break;
                case R.id.tv_save:
                    editPosition = -1;
                    mObservable.notifyObservers(false);
                    mDelItem.setVisibility(View.VISIBLE);
                    mEditAlarmItem.setVisibility(View.GONE);

                    AlarmClock sAlarm = (AlarmClock) mdatas.get(position - 1).t;
                    String time = mEditAlarmTime.getText().toString();
                    String desc = mEditAlarmDesc.getText().toString();
                    mAlarmTime.setText(time);
                    mAlarmDesc.setText(desc);
                    mAlarmFr.setText(AssistUtils.transalteWeekDayString(sAlarm.getFrequency()));
                    mAlarmSwitchBtn.setChecked(true);
                    //保存闹钟记录
                    sAlarm.setRtime(new SimpleDate(time).toValue());
                    sAlarm.setRing(mEditAlarmRing.getText().toString());
                    sAlarm.setItem(desc);
                    AssistUtils.setAlarmRdate(sAlarm);
                    sAlarm.setSynced(false);
                    switchAlarm(sAlarm, RemindService.CANCEL);
                    mAssistDao.updateAlarm(sAlarm);
                    //开启闹钟服务
                    switchAlarm(sAlarm, RemindService.ADD);
                    EventBus.getDefault().post(new UpdateTaskCardEvent<>(sAlarm, true));
                    //与服务器同步
                    AssistEntityDao.create().sync(mAlarmEntityDao);
                    break;
                case R.id.alarm_switch_btn:
                    mDelItem.hide();
                    AlarmClock swiAlarm = (AlarmClock) mdatas.get(position - 1).t;
                    AlarmClock switchAlarm = mAssistDao.findAlarmById(swiAlarm.getId());
                    //防止在数据库中已删除的记录被操作
                    if (switchAlarm != null) {
                        mAlarmSwitchBtn.setChecked(mAlarmSwitchBtn.isChecked());
                        swiAlarm.setValid(mAlarmSwitchBtn.isChecked() ? 1 : 0);
                        switchAlarm.setValid(mAlarmSwitchBtn.isChecked() ? 1 : 0);
                        if (switchAlarm.getValid() == 1){
                            AssistUtils.setAlarmRdate(switchAlarm);
                            switchAlarm.setSynced(false);
                        }
                        AssistDao.getInstance().updateAlarm(switchAlarm);
                        Intent aIntent = new Intent(mContext, RemindService.class);
                        aIntent.putExtra(RemindService.CMD, (RemindService.ALARM << 4) + (mAlarmSwitchBtn.isChecked() ? RemindService.ADD : RemindService.CANCEL));
                        aIntent.putExtra(RemindService.ID, switchAlarm.getId());
                        mContext.startService(aIntent);
                        if (switchAlarm.getValid() == 1) {      //同步修改响铃日期
                            AssistEntityDao.create().sync(mAlarmEntityDao);
                        }
                    }
                    break;
                case R.id.tv_state:
                    TaskCard taskCard = mdatas.get(position - 1);
                    if (taskCard.taskState == TaskCard.TaskState.REVOCABLE) {
                        taskCard.taskState = TaskCard.TaskState.ACTIVE;
                        ((AlarmClock)taskCard.t).setSynced(false);
                        mAssistDao.insertAlarm((AlarmClock) taskCard.t);
                        switchAlarm((AlarmClock) taskCard.t, RemindService.ADD);
                        EventBus.getDefault().post(new UpdateTaskCardEvent<>(taskCard.t, TaskCard.TaskState.ACTIVE));
                        //与服务器同步
                        AssistEntityDao.create().sync(mAlarmEntityDao);
                    }
                    break;
            }
        }

        @Override
        public void onSliding(SlidingItem item) {
            if (lastItem != null && lastItem != item)
                lastItem.hide();
        }

        @Override
        public void onBtnClick(View v) {
            TaskCard<AlarmClock> taskCard = mdatas.get(getAdapterPosition() - 1);
            AlarmClock alarm = taskCard.t;
            //取消闹钟服务
            switchAlarm(alarm, RemindService.CANCEL);
            alarm.setSynced(false);
            //从数据库中删除
            mAssistDao.deleteAlarm(alarm);
            EventBus.getDefault().post(new UpdateTaskCardEvent<>(alarm, TaskCard.TaskState.DELETED));
            //与服务器同步
            AssistEntityDao.create().sync(mAlarmEntityDao);
        }

        @Override
        public void onContentClick(View v) {
            if (editPosition != -1 && isEdit) {
                showTips();
                return;
            }
            collapseCard();
            mEditHolder = this;
            editPosition = getAdapterPosition();
            AlarmClock alarm = (AlarmClock) mdatas.get(getAdapterPosition() - 1).t;
            mDelItem.setVisibility(View.GONE);
            mEditAlarmItem.setVisibility(View.VISIBLE);
            mEditAlarmTime.setText(new SimpleDate(alarm.getRtime()).toString());
            mEditAlarmDesc.setText(alarm.getItem());
            mEditAlarmRing.setText(TextUtils.isEmpty(alarm.getRing()) ? "默认" : alarm.getRing());
            mEditAlarmFr.setText(AssistUtils.transalteWeekDayString(alarm.getFrequency()));
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
            mObservable.notifyObservers(true);
        }
    }

    /**
     * 提醒列表视图
     **/
    class RemindListHolder extends RecyclerView.ViewHolder implements SlidingItem.OnSlidingItemListener, DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

        @BindView(R.id.remind_datetime)
        TextView mRemindDatetime;
        @BindView(R.id.remind_content)
        TextView mRemindContent;
        @BindView(R.id.del_item)
        SlidingItem mDelItem;
        @BindView(R.id.edit_remind_content)
        TextInputLayout mEditRemindContent;
        @BindView(R.id.edit_remind_date)
        TextView mEditRemindDate;
        @BindView(R.id.edit_remind_time)
        TextView mEditRemindTime;
        @BindView(R.id.edit_remind_fr)
        TextView mEditRemindFr;
        @BindView(R.id.edit_remind_item)
        LinearLayout mEditRemindItem;
        @BindView(R.id.tv_state)
        TextView mTvState;
        @BindView(R.id.tv_save)
        TextView mTvSave;
        @BindView(R.id.dash_divider)
        View mDashDivider;
        private AssistEntityDao.RemindEntityDao mRemindEntityDao;

        public RemindListHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            mDelItem.setOnSlidingItemListener(this);
            mRemindEntityDao = AssistEntityDao.create().getDao(AssistEntityDao.RemindEntityDao.class);
            final EditText mEtRemind = mEditRemindContent.getEditText();
            if (mEtRemind != null) {
                mEtRemind.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        String content = mEtRemind.getText().toString().trim();
                        if (!lastRemindContent.equals(content))
                            mObservable.notifyObservers(true);
                        if (TextUtils.isEmpty(content)) {
                            mTvSave.setTextColor(mContext.getResources().getColor(R.color.forbid_click_color));
                        } else {
                            mTvSave.setTextColor(mContext.getResources().getColor(R.color.white));
                        }
                    }
                });
            }
        }

        @OnClick({R.id.edit_remind_date_box, R.id.edit_remind_time, R.id.ll_remind_fr,
                R.id.tv_cancel, R.id.tv_save, R.id.tv_state})
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.edit_remind_date_box:
                    Date rDate = TimeUtils.parse(mEditRemindDate.getText().toString());
                    Calendar now = Calendar.getInstance();
                    now.setTime(rDate);
                    //将当前日期传入日期选择器，并显示
                    DatePickerDialog datePicker = DatePickerDialog.newInstance(
                            RemindListHolder.this,
                            now.get(Calendar.YEAR),
                            now.get(Calendar.MONTH),
                            now.get(Calendar.DAY_OF_MONTH)
                    );
                    datePicker.setAccentColor(mContext.getResources().getColor(R.color.base_blue));
                    datePicker.show(((Activity) mContext).getFragmentManager(), "DatePicker");
                    break;
                case R.id.edit_remind_time:
                    SimpleDate rTime = new SimpleDate(mEditRemindTime.getText().toString());
                    TimePickerDialog timePicker = TimePickerDialog.newInstance(
                            RemindListHolder.this,
                            rTime.getHour(),
                            rTime.getMinute(),
                            true);
                    timePicker.setAccentColor(mContext.getResources().getColor(R.color.base_blue));
                    timePicker.show(((Activity) mContext).getFragmentManager(), "TimePicker");
                    break;
                case R.id.ll_remind_fr:
                    Remind rRemind = (Remind) mdatas.get(getAdapterPosition() - 1).t;
                    Calendar cl = Calendar.getInstance();
                    if (rRemind.getRdate() != null)
                        cl.setTime(rRemind.getRdate());
                    RemindFrDialog frDialog = new RemindFrDialog((Activity) mContext, rRemind.getFrequency(), cl, new RemindFrDialog.OnResultListener() {
                        @Override
                        public void onResult(int fr) {
                            remindFr = fr;
                            mEditRemindFr.setText(AssistUtils.translateRemindFrequency(fr));
                            mObservable.notifyObservers(true);
                        }
                    });
                    frDialog.show();
                    break;
                case R.id.tv_cancel:
                    editPosition = -1;
                    mDelItem.setVisibility(View.VISIBLE);
                    mEditRemindItem.setVisibility(View.GONE);
                    lastRemindContent = "";
                    mObservable.notifyObservers(false);
                    break;
                case R.id.tv_save:
                    String content = mEditRemindContent.getEditText().getText().toString().trim();
                    if (TextUtils.isEmpty(content)) {
                        mEditRemindContent.setErrorEnabled(true);
                        mEditRemindContent.setError("请输入内容");
                        if (mEditRemindContent.getEditText().getBackground() != null) {
                            mEditRemindContent.getEditText().getBackground().clearColorFilter();
                        }
                        return;
                    }
                    editPosition = -1;
                    lastRemindContent = "";
                    mObservable.notifyObservers(false);
                    mEditRemindContent.setError("");
                    mEditRemindContent.setErrorEnabled(false);
                    Remind remind = (Remind) mdatas.get(getAdapterPosition() - 1).t;
                    //取消原来的提醒服务
                    switchRemind(remind, RemindService.CANCEL);
                    mDelItem.setVisibility(View.VISIBLE);
                    mEditRemindItem.setVisibility(View.GONE);
                    String rdate = mEditRemindDate.getText().toString();
                    String rtime = mEditRemindTime.getText().toString();
                    remind.setRdate(TimeUtils.parseDateTime(rdate + " " + rtime));
                    remind.setRtime(rtime);
                    remind.setFrequency(remindFr);
                    remind.setContent(content);
                    mRemindContent.setText(content);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(remind.getRdate());
                    mRemindDatetime.setText(new StringBuilder().append(remindFr == 0 ? rdate : AssistUtils.translateRemindFrequency(remindFr, calendar)).append("    ").append(rtime).toString());
                    remind.setSynced(false);
                    //更新提醒数据，并开启提醒服务
                    mAssistDao.updateRemind(remind);
                    switchRemind(remind, RemindService.ADD);
                    EventBus.getDefault().post(new UpdateTaskCardEvent<>(remind, true));
                    //与服务器同步修改
                    AssistEntityDao.create().sync(mRemindEntityDao);
                    break;
                case R.id.tv_state:
                    TaskCard taskCard = mdatas.get(getAdapterPosition() - 1);
                    if (taskCard.taskState == TaskCard.TaskState.REVOCABLE) {
                        taskCard.taskState = TaskCard.TaskState.ACTIVE;
                        ((Remind)taskCard.t).setSynced(false);
                        mAssistDao.insertRemind((Remind) taskCard.t);
                        switchRemind((Remind) taskCard.t, RemindService.ADD);
                        EventBus.getDefault().post(new UpdateTaskCardEvent<>(taskCard.t, TaskCard.TaskState.ACTIVE));
                        //与服务器同步修改
                        AssistEntityDao.create().sync(mRemindEntityDao);
                    }
                    break;
            }
        }

        @Override
        public void onSliding(SlidingItem item) {
            if (lastItem != null && lastItem != item)
                lastItem.hide();
        }

        @Override
        public void onBtnClick(View v) {
            TaskCard taskCard = mdatas.get(getAdapterPosition() - 1);
            Remind remind = (Remind) taskCard.t;
            //取消提醒服务
            switchRemind(remind, RemindService.CANCEL);
            remind.setSynced(false);
            //从数据库中删除
            mAssistDao.deleteRemind(remind);
            EventBus.getDefault().post(new UpdateTaskCardEvent<>(remind, TaskCard.TaskState.DELETED));
            //与服务器同步修改
            AssistEntityDao.create().sync(mRemindEntityDao);
        }

        @Override
        public void onContentClick(View v) {
            if (editPosition != -1 && isEdit) {
                showTips();
                return;
            }
            collapseCard();
            mEditHolder = this;
            editPosition = getAdapterPosition();
            mDelItem.setVisibility(View.GONE);
            mEditRemindItem.setVisibility(View.VISIBLE);
            Remind remind = (Remind) mdatas.get(getAdapterPosition() - 1).t;
            lastRemindContent = remind.getContent();
            mEditRemindContent.getEditText().setText(remind.getContent());
            mEditRemindDate.setText(TimeUtils.format(remind.getRdate()));
            mEditRemindTime.setText(new SimpleDate(remind.getRtime()).toString());
            mEditRemindFr.setText(AssistUtils.translateRemindFrequency(remind.getFrequency()));
            remindFr = remind.getFrequency();
        }

        @Override
        public void onExpanded(SlidingItem item) {
            lastItem = item;
        }

        @Override
        public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
            mObservable.notifyObservers(true);
            String month = monthOfYear + 1 < 10 ? "0" + (monthOfYear + 1) : "" + (monthOfYear + 1);
            String day = dayOfMonth < 10 ? "0" + dayOfMonth : "" + dayOfMonth;
            mEditRemindDate.setText(new StringBuilder().append(year).append("年").append(month).append("月").append(day).append("日").toString());
        }

        @Override
        public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
            mObservable.notifyObservers(true);
            String hour = hourOfDay >= 10 ? "" + hourOfDay : "0" + hourOfDay;
            String min = minute >= 10 ? "" + minute : "0" + minute;
            mEditRemindTime.setText(hour + ":" + min);
        }
    }

    /**
     * 通知提醒服务开/关提醒
     **/
    private void switchRemind(Remind remind, int cmd) {
        Intent rIntent = new Intent(mContext, RemindService.class);
        rIntent.putExtra(RemindService.CMD, (RemindService.REMIND << 4) + cmd);
        rIntent.putExtra(RemindService.ID, remind.getId());
        mContext.startService(rIntent);
    }

    /**
     * 通知提醒服务开/关闹钟
     **/
    private void switchAlarm(AlarmClock alarm, int cmd) {
        Intent rIntent = new Intent(mContext, RemindService.class);
        rIntent.putExtra(RemindService.CMD, (RemindService.ALARM << 4) + cmd);
        rIntent.putExtra(RemindService.ID, alarm.getId());
        mContext.startService(rIntent);
    }

    /**
     * 收起展开但未编辑的卡片
     **/
    private void collapseCard() {
        if (editPosition != -1) {
            Object obj = mdatas.get(editPosition - 1).t;
            if (obj instanceof Remind) {
                RemindListHolder editHolder = (RemindListHolder) mEditHolder;
                editHolder.mEditRemindItem.setVisibility(View.GONE);
                editHolder.mDelItem.setVisibility(View.VISIBLE);
            } else if (obj instanceof AlarmClock) {
                AlarmListHolder editHolder = (AlarmListHolder) mEditHolder;
                editHolder.mEditAlarmItem.setVisibility(View.GONE);
                editHolder.mDelItem.setVisibility(View.VISIBLE);
            } else if (obj instanceof Memo) {
                MemoListHolder editHolder = (MemoListHolder) mEditHolder;
                editHolder.mEditMemoItem.setVisibility(View.GONE);
                editHolder.mDelItem.setVisibility(View.VISIBLE);
            } else {
                AccountListHolder editHolder = (AccountListHolder) mEditHolder;
                editHolder.mEditAccountItem.setVisibility(View.GONE);
                editHolder.mDelItem.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void update(Observable observable, Object data) {
        isEdit = (boolean) data;
    }

    /**
     * 卡片编辑被观察者
     **/
    class TaskListObservable extends Observable {
        @Override
        public void notifyObservers(Object data) {
            setChanged();
            super.notifyObservers(data);
        }
    }
}
