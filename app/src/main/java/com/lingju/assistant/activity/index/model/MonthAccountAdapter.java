package com.lingju.assistant.activity.index.model;

import android.content.Context;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.AccountingActivity;
import com.lingju.assistant.entity.MonthAccount;
import com.lingju.assistant.entity.TaskCard;
import com.lingju.assistant.view.ItemExpenseDialog;
import com.lingju.assistant.view.ItemIncomeDialog;
import com.lingju.assistant.view.SlidingItem;
import com.lingju.model.Accounting;
import com.lingju.model.SimpleDate;
import com.lingju.model.dao.AssistDao;
import com.lingju.model.dao.AssistEntityDao;
import com.lingju.util.AssistUtils;
import com.lingju.util.TimeUtils;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Observable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2017/3/8.
 */
public class MonthAccountAdapter extends RecyclerView.Adapter<MonthAccountAdapter.MonthAccountHolder> {

    private AccountingActivity mContext;
    private List<TaskCard<Accounting>> mdatas = new ArrayList<>();
    private SlidingItem lastItem;
    private MonthAccount mMonthAccount;
    private int editPosition = -1;
    private CardObservable mObservable;
    private String lastMemo = "";
    private String lastAmount = "";
    /*
    private int atype;
    private String etype;
    private String rdate;
    private String rtime;
    */

    public MonthAccountAdapter(Context context, MonthAccount ma) {
        this.mContext = (AccountingActivity) context;
        this.mMonthAccount = ma;
        this.mdatas = ma.taskCards;
        mObservable = new CardObservable();
        mObservable.addObserver(mContext);
    }


    public void setDatas(List<TaskCard<Accounting>> datas) {
        if (datas != null) {
            mdatas.clear();
            mdatas.addAll(datas);
            notifyDataSetChanged();
        }
    }

    @Override
    public MonthAccountHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(mContext).inflate(R.layout.item_accountlist_view, null, false);
        return new MonthAccountHolder(item);
    }

    @Override
    public void onBindViewHolder(MonthAccountHolder holder, int position) {
        TaskCard<Accounting> taskCard = mdatas.get(position);
        Accounting accounting = taskCard.t;
        holder.mTvState.setVisibility(View.GONE);
        holder.mDashDivider.setVisibility(View.GONE);
        if (position == editPosition) {     //展开
            holder.mDelItem.setVisibility(View.GONE);
            holder.mEditAccountItem.setVisibility(View.VISIBLE);
            holder.mTvCancel.setVisibility(View.VISIBLE);
            holder.mTvSave.setVisibility(View.VISIBLE);
        } else {        //收起
            holder.mDelItem.setVisibility(View.VISIBLE);
            holder.mTvCancel.setVisibility(View.GONE);
            holder.mTvSave.setVisibility(View.GONE);
            holder.mEditAccountItem.setVisibility(View.GONE);
            holder.mAccountEtype.setText(accounting.getEtype());
            holder.mAccountDate.setText(TimeUtils.formatDate(accounting.getRdate()));
            if (accounting.getAtype() == 0) {
                holder.mAccountAmount.setTextColor(mContext.getResources().getColor(R.color.green));
                holder.mAccountAmount.setText("-" + AssistUtils.formatAmount(accounting.getAmount()));
            } else {
                holder.mAccountAmount.setTextColor(mContext.getResources().getColor(R.color.red_style));
                holder.mAccountAmount.setText("+" + AssistUtils.formatAmount(accounting.getAmount()));
            }
            if (taskCard.taskState == TaskCard.TaskState.REVOCABLE) {
                holder.mTvState.setVisibility(View.VISIBLE);
                holder.mTvState.setText("撤销");
                holder.mTvState.setTextColor(mContext.getResources().getColor(R.color.base_blue));
            }
        }
    }

    @Override
    public int getItemCount() {
        return mdatas.size();
    }

    public int getEditPosition() {
        return editPosition;
    }

    class MonthAccountHolder extends RecyclerView.ViewHolder implements SlidingItem.OnSlidingItemListener, DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
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
        @BindView(R.id.tv_cancel)
        TextView mTvCancel;
        @BindView(R.id.tv_save)
        TextView mTvSave;
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
        @BindView(R.id.dash_divider)
        View mDashDivider;
        private AssistEntityDao.BillEntityDao mBillEntityDao;

        public MonthAccountHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            mDelItem.setOnSlidingItemListener(this);
            mBillEntityDao = AssistEntityDao.create().getDao(AssistEntityDao.BillEntityDao.class);
            final EditText etContent = mEditAccountAmount.getEditText();
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
            mEditAccountMemo.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    return (event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
                }
            });
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
            mContext.setEditTextFocusable(false);
            switch (view.getId()) {
                case R.id.ll_account_atype:
                    Accounting t = mdatas.get(getAdapterPosition()).t;
                    mObservable.notifyObservers(true);
                    if (t.getAtype() == 0) {   //支出
                        t.setAtype(1);
                        mEditAccountAtype.setText("收入");
                    } else {
                        t.setAtype(0);
                        mEditAccountAtype.setText("支出");
                    }
                    break;
                case R.id.ll_account_etype:
                    Accounting account = mdatas.get(getAdapterPosition()).t;
                    String cEtype = mEditAccountEtype.getText().toString();
                    if (account.getAtype() == 0) {
                        ItemExpenseDialog expenseDialog = new ItemExpenseDialog(mContext, cEtype, new ItemExpenseDialog.OnItemExpenseListener() {
                            @Override
                            public void onItemSelected(String item) {
                                mEditAccountEtype.setText(item);
                                mObservable.notifyObservers(true);
                            }
                        });
                        expenseDialog.show();
                    } else {
                        ItemIncomeDialog incomeDialog = new ItemIncomeDialog(mContext, cEtype, new ItemIncomeDialog.OnItemIncomeListener() {
                            @Override
                            public void onIncomeSelected(String item) {
                                mEditAccountEtype.setText(item);
                                mObservable.notifyObservers(true);
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
                    datePicker.show(mContext.getFragmentManager(), "DatePicker");
                    break;
                case R.id.edit_account_time:
                    SimpleDate rTime = new SimpleDate(mEditAccountTime.getText().toString());
                    TimePickerDialog timePicker = TimePickerDialog.newInstance(
                            this,
                            rTime.getHour(),
                            rTime.getMinute(),
                            true);
                    timePicker.setAccentColor(mContext.getResources().getColor(R.color.base_blue));
                    timePicker.show(mContext.getFragmentManager(), "TimePicker");
                    break;
                case R.id.tv_cancel:
                    mTvCancel.setVisibility(View.GONE);
                    mTvSave.setVisibility(View.GONE);
                    mDelItem.setVisibility(View.VISIBLE);
                    mEditAccountItem.setVisibility(View.GONE);
                    //清空编辑索引
                    editPosition = -1;
                    mContext.setEditPosition(null, -1);
                    lastMemo = "";
                    lastAmount = "";
                    mObservable.notifyObservers(false);
                    break;
                case R.id.tv_save:
                    String amount = mEditAccountAmount.getEditText().getText().toString();
                    Double money;
                    if (TextUtils.isEmpty(amount) || !AssistUtils.isNumber(amount) || (money = Double.valueOf(amount)) == 0) {
                        mEditAccountAmount.setErrorEnabled(true);
                        mEditAccountAmount.setError("请输入金额");
                        return;
                    }
                    mEditAccountAmount.setErrorEnabled(false);
                    mDelItem.setVisibility(View.VISIBLE);
                    mTvCancel.setVisibility(View.GONE);
                    mTvSave.setVisibility(View.GONE);
                    mEditAccountItem.setVisibility(View.GONE);
                    //清空编辑索引
                    mContext.setEditPosition(null, -1);
                    editPosition = -1;
                    lastMemo = "";
                    lastAmount = "";
                    mObservable.notifyObservers(false);
                    //保存记录，设置完成视图数据
                    TaskCard<Accounting> card = mdatas.get(getAdapterPosition());
                    Accounting sAccount = card.t;
                    //记录旧金额
                    double oldAmount = sAccount.getAmount();
                    sAccount.setAmount(money);
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
                    AssistDao.getInstance().updateAccount(sAccount);
                    List<TaskCard<Accounting>> list = new ArrayList<>();
                    list.add(card);
                    mContext.updateBalance(AccountingActivity.TYPE_UPDATE, list);
                    mContext.loadData(cl.get(Calendar.YEAR), sAccount.getMonth());
                    //同步账单记录
                    AssistEntityDao.create().sync(mBillEntityDao);
                    break;
                case R.id.tv_state:
                    TaskCard<Accounting> taskCard = mdatas.get(getAdapterPosition());
                    if (taskCard.taskState == TaskCard.TaskState.REVOCABLE) {
                        taskCard.taskState = TaskCard.TaskState.ACTIVE;
                        taskCard.t.setSynced(false);
                        AssistDao.getInstance().insertAccount(taskCard.t);
                        notifyItemChanged(getAdapterPosition());
                        if (taskCard.t.getAtype() == 0) {
                            mMonthAccount.expense += taskCard.t.getAmount();
                        } else {
                            mMonthAccount.income += taskCard.t.getAmount();
                        }
                        mContext.updateMonthAccount(mMonthAccount);
                        List<TaskCard<Accounting>> lists = new ArrayList<>();
                        lists.add(taskCard);
                        mContext.updateBalance(AccountingActivity.TYPE_ADD, lists);
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
            for (int i = 0; i < mdatas.size(); i++) {
                if (mdatas.get(i).taskState == TaskCard.TaskState.REVOCABLE) {
                    mdatas.remove(i);
                    notifyItemRemoved(i);
                    break;
                }
            }
            TaskCard<Accounting> taskCard = mdatas.get(getAdapterPosition());
            taskCard.taskState = TaskCard.TaskState.REVOCABLE;
            taskCard.t.setSynced(false);
            AssistDao.getInstance().deleteAccount(taskCard.t);
            notifyItemChanged(getAdapterPosition());
            if (taskCard.t.getAtype() == 0) {
                mMonthAccount.expense -= taskCard.t.getAmount();
            } else {
                mMonthAccount.income -= taskCard.t.getAmount();
            }
            mContext.updateMonthAccount(mMonthAccount);
            List<TaskCard<Accounting>> list = new ArrayList<>();
            list.add(taskCard);
            mContext.updateBalance(AccountingActivity.TYPE_DELETE, list);
            //同步账单记录
            AssistEntityDao.create().sync(mBillEntityDao);
        }

        @Override
        public void onContentClick(View v) {
            mContext.setEditTextFocusable(false);
            if (mContext.getEditPosition() != 0 && mContext.hadEdit()) {
                mContext.showEditTips();
                return;
            }
            mContext.collapseCard(mContext.getEditPosition());
            editPosition = getAdapterPosition();
            mContext.setEditPosition(mMonthAccount, editPosition);
            Accounting accounting = mdatas.get(getAdapterPosition()).t;
            mDelItem.setVisibility(View.GONE);
            mEditAccountItem.setVisibility(View.VISIBLE);
            mTvCancel.setVisibility(View.VISIBLE);
            mTvSave.setVisibility(View.VISIBLE);
            lastMemo = TextUtils.isEmpty(accounting.getMemo())?"":accounting.getMemo();
            lastAmount = String.valueOf(accounting.getAmount());
            mEditAccountAmount.getEditText().setText(String.valueOf(accounting.getAmount()));
            if (accounting.getAtype() == 0) {
                mEditAccountAtype.setText("支出");
            } else {
                mEditAccountAtype.setText("收入");
            }
            mEditAccountEtype.setText(accounting.getEtype());
            mEditAccountDate.setText(TimeUtils.format(accounting.getRdate()));
            mEditAccountTime.setText(TimeUtils.getTime(accounting.getRdate()));
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
    }

    /**
     * 卡片编辑被观察者
     **/
    class CardObservable extends Observable {

        @Override
        public void notifyObservers(Object data) {
            setChanged();
            super.notifyObservers(data);
        }
    }
}
