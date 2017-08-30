package com.lingju.assistant.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.BaseAssistActivity;
import com.lingju.assistant.activity.index.model.FullyLinearLayoutManager;
import com.lingju.assistant.activity.index.model.MonthAccountAdapter;
import com.lingju.assistant.entity.MonthAccount;
import com.lingju.assistant.entity.TaskCard;
import com.lingju.assistant.view.ItemExpenseDialog;
import com.lingju.assistant.view.ItemIncomeDialog;
import com.lingju.assistant.view.SlidingItem;
import com.lingju.assistant.view.YearPickerDialog;
import com.lingju.model.Accounting;
import com.lingju.model.SimpleDate;
import com.lingju.model.dao.AssistEntityDao;
import com.lingju.util.AssistUtils;
import com.lingju.util.TimeUtils;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.reactivestreams.Subscription;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Observer;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class AccountingActivity extends BaseAssistActivity implements Observer {

    public static final int TYPE_ADD = 0;
    public static final int TYPE_DELETE = 1;
    public static final int TYPE_UPDATE = 2;
    private float balance;
    private AccountAdapter mAdapter;
    //private Map<Integer, List<TaskCard<Accounting>>> accountMaps = new HashMap<>();
    private List<MonthAccount> showMonthDatas = new ArrayList<>();
    private double todayIncome;
    private double todayExpend;
    private SlidingItem lastItem;
    private int expendPos;
    private Calendar mCalendar;
    private String amount;
    private String memo;
    private InputMethodManager mInputMethodManager;
    private boolean isEdit;
    private int subEditPos = -1;
    private AssistEntityDao.BillEntityDao mBillEntityDao;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        mCalendar = Calendar.getInstance();
        loadData(mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH) + 1);
        mBillEntityDao = AssistEntityDao.create().getDao(AssistEntityDao.BillEntityDao.class);
    }

    private void initView() {
        mLlAccount.setVisibility(View.VISIBLE);
        mTvBalance.setVisibility(View.VISIBLE);
        mRlDatetime.setVisibility(View.GONE);
        mEtBalance.setVisibility(View.GONE);
        mIvHeader.setImageResource(R.drawable.pic_account_bar);
        android.support.design.widget.CollapsingToolbarLayout.LayoutParams params = (CollapsingToolbarLayout.LayoutParams) mIvHeader.getLayoutParams();
        params.rightMargin = 64;
        params.topMargin = 16;
        mIvHeader.setLayoutParams(params);
        mEtBalance.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    //关闭软键盘
                    if (mInputMethodManager != null) {
                        mInputMethodManager.hideSoftInputFromWindow(mEtBalance.getWindowToken(), 0);
                    }
                    String money = mEtBalance.getText().toString();
                    if (TextUtils.isEmpty(money)) {
                        mTvBalance.setText("点击设置");
                        balance = 0;
                        AppConfig.dPreferences.edit().putBoolean(AppConfig.HAS_AMOUNT, false).commit();
                    } else if (AssistUtils.isNumber(money)) {
                        balance = Float.valueOf(money);
                        DecimalFormat adf = new DecimalFormat("##0.00");
                        mEtBalance.setText(adf.format(balance));
                        mTvBalance.setText("￥" + AssistUtils.formatAmount(balance));
                        AppConfig.dPreferences.edit().putFloat(AppConfig.ACCOUNT_AMOUNT, balance).commit();
                        AppConfig.dPreferences.edit().putBoolean(AppConfig.HAS_AMOUNT, true).commit();
                    } else {
                        return true;
                    }
                    mEtBalance.setVisibility(View.GONE);
                    mTvBalance.setVisibility(View.VISIBLE);
                    return true;
                }
                return true;
            }
        });

        mEtBalance.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    String money = mEtBalance.getText().toString();
                    if (TextUtils.isEmpty(money)) {
                        mTvBalance.setText("点击设置");
                        balance = 0;
                        AppConfig.dPreferences.edit().putBoolean(AppConfig.HAS_AMOUNT, false).commit();
                    } else if (AssistUtils.isNumber(money)) {
                        balance = Float.valueOf(money);
                        DecimalFormat adf = new DecimalFormat("##0.00");
                        mEtBalance.setText(adf.format(balance));
                        mTvBalance.setText("￥" + AssistUtils.formatAmount(balance));
                        AppConfig.dPreferences.edit().putFloat(AppConfig.ACCOUNT_AMOUNT, balance).commit();
                        AppConfig.dPreferences.edit().putBoolean(AppConfig.HAS_AMOUNT, true).commit();
                    }
                    mEtBalance.setVisibility(View.GONE);
                    mTvBalance.setVisibility(View.VISIBLE);
                }
            }
        });
        mTvBalance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTvBalance.setVisibility(View.GONE);
                mEtBalance.setVisibility(View.VISIBLE);
                DecimalFormat adf = new DecimalFormat("##0.00");
                mEtBalance.setText(adf.format(balance));
                //                mBalance.setText(String.valueOfText(String.valueOf(balance));
                //一定要按一下这个顺序写，重新使edittext获取焦点：API中的解释：
                //--设置edittext是否可以获得焦点
                mEtBalance.setFocusable(true);
                //--设置edittext在touch模式下是否可以获得焦点
                mEtBalance.setFocusableInTouchMode(true);
                //--调用这个给指定的view或者它的子view焦点。如果这个view在isFocusable()方法下返回false，或者isFocusableInTouchMode()方法下返回false，这个view不会真正获得焦点
                mEtBalance.requestFocus();
                if (mInputMethodManager == null)
                    mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                mInputMethodManager.showSoftInput(mEtBalance, 0);
            }
        });
        if (AppConfig.dPreferences.getBoolean(AppConfig.HAS_AMOUNT, false)) {
            balance = AppConfig.dPreferences.getFloat(AppConfig.ACCOUNT_AMOUNT, 0);
            mTvBalance.setText("￥" + AssistUtils.formatAmount(balance));
        } else {
            mTvBalance.setText("点击设置");
        }
        mAdapter = new AccountAdapter();
        mAssistRecyclerview.setAdapter(mAdapter);
    }


    public void loadData(final int year, final int expendMonth) {
        mCpbLoad.setVisibility(View.VISIBLE);
        Flowable.create(new FlowableOnSubscribe<Object>() {
            @Override
            public void subscribe(FlowableEmitter<Object> e) throws Exception {
                /* io线程加载数据库数据 */
                mAssistDao.findAccountAllYear(showMonthDatas, year);
                if (showMonthDatas.size() > 0) {
                    for (MonthAccount ma : showMonthDatas) {
                        //统计每个月份的收支情况
                        for (TaskCard<Accounting> card : ma.taskCards) {
                            if (card.t.getAtype() == 0) {    //支出
                                ma.expense += card.t.getAmount();
                            } else {
                                ma.income += card.t.getAmount();
                            }
                        }
                    }

                    //设置指定月份数据展开标记
                    if (expendMonth != 0) {
                        for (int i = 0; i < showMonthDatas.size(); i++) {
                            MonthAccount ma = showMonthDatas.get(i);
                            if (expendMonth == ma.month) {
                                ma.isExpended = true;
                                expendPos = i + 1;
                                break;
                            }
                        }
                    }
                }
                CountToday();
                e.onNext(0);
            }
        }, BackpressureStrategy.BUFFER)
                .subscribeOn(Schedulers.io())   //执行订阅（subscribe()）所在线程
                .doOnSubscribe(new Consumer<Subscription>() {
                    @Override
                    public void accept(Subscription subscription) throws Exception {
                        mCpbLoad.setVisibility(View.VISIBLE);
                        reset();
                        mAdapter.notifyDataSetChanged();
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
                        moveToPosition(expendPos == 1 ? 0 : expendPos);
                    }
                });
    }

    /**
     * 更新余额、当日收支
     **/
    public void updateBalance(final int type, final List<TaskCard<Accounting>> taskcards) {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                CountToday();
                CountBalance(type, taskcards);
                e.onNext(0);
            }
        })
                .subscribeOn(Schedulers.io())   //执行订阅（subscribe()）所在线程
                .observeOn(AndroidSchedulers.mainThread())  //响应订阅（Sbscriber）所在线程
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        if (AppConfig.dPreferences.getBoolean(AppConfig.HAS_AMOUNT, false))
                            mTvBalance.setText("￥" + AssistUtils.formatAmount(balance));
                        mAdapter.notifyItemChanged(0);
                    }
                });
    }

    /**
     * 计算当天消费
     **/
    private void CountToday() {
        //统计今日消费
        todayIncome = 0;
        todayExpend = 0;
        List<Accounting> accountings = mAssistDao.findAccountToday();
        //        for (Accounting account : accountings) {
        //            if (account.getAtype() == 0) {    //支出
        //                todayAmount -= account.getAmount();
        //            } else {
        //                todayAmount += account.getAmount();
        //            }
        //        }
        for (Accounting account : accountings) {
            if (account.getAtype() == 0) {
                todayExpend += account.getAmount();
            } else {
                todayIncome += account.getAmount();
            }
        }
    }

    /**
     * 计算余额
     **/
    private void CountBalance(int type, List<TaskCard<Accounting>> taskcards) {
        balance = AppConfig.dPreferences.getFloat(AppConfig.ACCOUNT_AMOUNT, 0);
        switch (type) {
            case TYPE_ADD:
            case TYPE_UPDATE:
                for (TaskCard<Accounting> taskcard : taskcards) {
                    if (taskcard.t.getAtype() == 0) {    //支出
                        balance -= taskcard.t.getAmount();
                    } else {
                        balance += taskcard.t.getAmount();
                    }
                }
                break;
            case TYPE_DELETE:
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

    /**
     * 更新月账单金额
     **/
    public void updateMonthAccount(MonthAccount ma) {
        View monthView = mLayoutManager.findViewByPosition(showMonthDatas.indexOf(ma) + 1);
        if (monthView != null) {
            ((TextView) monthView.findViewById(R.id.tv_income)).setText("+" + AssistUtils.formatAmount(ma.income));
            ((TextView) monthView.findViewById(R.id.tv_expense)).setText("-" + AssistUtils.formatAmount(ma.expense));
        }
    }

    private void reset() {
        showMonthDatas.clear();
        todayIncome = 0;
        todayExpend = 0;
        expendPos = 0;
    }

    public int getEditPosition() {
        return isEditPosition;
    }

    public void setEditPosition(MonthAccount ma, int subPosition) {
        subEditPos = subPosition;
        if (ma == null)
            isEditPosition = 0;
        else
            isEditPosition = showMonthDatas.indexOf(ma) + 1;
    }

    @Override
    protected void onAssistCreate() {
        mEtBalance.setFocusable(false);
        moveToPosition(1);
        MonthAccount monthAccount = new MonthAccount();
        monthAccount.taskCards.add(new TaskCard<>(new Accounting(), TaskCard.TaskState.ACTIVE));
        showMonthDatas.add(0, monthAccount);
        mAdapter.notifyItemInserted(1);
        isEditPosition = 1;
    }

    @Override
    public boolean hadEdit() {
        if (isEditPosition != 0) {
            MonthAccount monthAccount = showMonthDatas.get(isEditPosition - 1);
            if (monthAccount.month == 0) {  //新建展开
                Accounting account = monthAccount.taskCards.get(0).t;
                return (!TextUtils.isEmpty("0.00".equals(amount) ? "" : amount)
                        || account.getAtype() != 0
                        || account.getEtype() != null
                        || account.getRdate() != null
                        || !TextUtils.isEmpty(memo));
            } else {       //修改展开
                return isEdit;
            }
        }
        return false;
    }

    @Override
    public void collapseCard(int position) {
        if (position != 0) {
            MonthAccount monthAccount = showMonthDatas.get(position - 1);
            if (monthAccount.month == 0) {      //新建卡片
                showMonthDatas.remove(position - 1);
                mAdapter.notifyItemRemoved(position);
                mFabAdd.show();
            } else {        //月账单展开卡片
                View itemView = mLayoutManager.findViewByPosition(position);
                if (itemView != null) {
                    RecyclerView rvMonthDetail = (RecyclerView) itemView.findViewById(R.id.rv_month_detail);
                    View subItemView = rvMonthDetail.getLayoutManager().findViewByPosition(subEditPos);
                    if (subItemView != null) {
                        View editAccountItem = subItemView.findViewById(R.id.edit_account_item);
                        View delItem = subItemView.findViewById(R.id.del_item);
                        editAccountItem.setVisibility(View.GONE);
                        delItem.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }

    @Override
    public void selectedYear() {
        new YearPickerDialog(this, new Date().getTime(), new YearPickerDialog.OnYearPickedListener() {
            @Override
            public void onPicked(int year) {
                mFabAdd.show();
                isEditPosition = 0;
                loadData(year, year == mCalendar.get(Calendar.YEAR) ? (mCalendar.get(Calendar.MONTH) + 1) : 0);
                Snackbar.make(mAssistRecyclerview, "已选择" + year + "年账单", Snackbar.LENGTH_SHORT).show();
            }
        }).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_menu, menu);
        menu.findItem(R.id.delete).setVisible(false);
        menu.findItem(R.id.edit).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void update(java.util.Observable observable, Object data) {
        isEdit = (boolean) data;
    }

    class AccountAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RecyclerView.ViewHolder holder;
            View itemView;
            if (viewType == HEADER_VIEW) {
                itemView = mInflater.inflate(R.layout.task_list_header, parent, false);
                holder = new HeaderHolder(itemView);
            } else if (viewType == ACCOUNT_CREATE_VIEW) {
                itemView = mInflater.inflate(R.layout.item_accountlist_view, parent, false);
                holder = new NewAccountHolder(itemView);
            } else {
                itemView = mInflater.inflate(R.layout.item_account_view, parent, false);
                holder = new AccountHolder(itemView);
            }
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderHolder) {
                //                StringBuilder sb = new StringBuilder("您今天");
                //                sb.append(todayAmount > 0 ? "收入了" : "支出了");
                //                sb.append(AssistUtils.formatAmount(Math.abs(todayAmount))).append("元");
                StringBuilder sb = new StringBuilder("您今天");
                sb.append("收入了").append(AssistUtils.formatAmount(todayIncome)).append("元，").append("支出了").append(AssistUtils.formatAmount(todayExpend)).append("元");
                ((HeaderHolder) holder).mTvHeader.setText(sb.toString());
            } else if (holder instanceof NewAccountHolder) {        //新建卡片视图
                NewAccountHolder naHolder = (NewAccountHolder) holder;
                Accounting account = showMonthDatas.get(position - 1).taskCards.get(0).t;
                naHolder.mDelItem.setVisibility(View.GONE);
                naHolder.mEditAccountItem.setVisibility(View.VISIBLE);
                naHolder.mEditAccountAmount.getEditText().setText(TextUtils.isEmpty(amount) ? "0.00" : amount);
                if (account.getAtype() == 0) {     // 支出
                    naHolder.mEditAccountAtype.setText("支出");
                } else {
                    naHolder.mEditAccountAtype.setText("收入");
                }
                naHolder.mEditAccountEtype.setText(TextUtils.isEmpty(account.getEtype()) ? "其他" : account.getEtype());
                if (account.getRdate() == null) {
                    naHolder.mEditAccountDate.setText(TimeUtils.format(new Date()));
                    naHolder.mEditAccountTime.setText(new SimpleDate().toString());
                } else {
                    naHolder.mEditAccountDate.setText(TimeUtils.format(account.getRdate()));
                    naHolder.mEditAccountTime.setText(TimeUtils.getTime(account.getRdate()));
                }
                naHolder.mEditAccountMemo.setText(memo);
                naHolder.mTvSave.setText("创建");
            } else {        //月账单视图
                AccountHolder accountHolder = (AccountHolder) holder;
                MonthAccount monthMsg = showMonthDatas.get(position - 1);
                accountHolder.mAccountMonth.setText(monthMsg.month + "月");
                accountHolder.mTvIncome.setText("+" + AssistUtils.formatAmount(monthMsg.income));
                accountHolder.mTvExpense.setText("-" + AssistUtils.formatAmount(monthMsg.expense));
                if (monthMsg.isExpended) {
                    accountHolder.mIvShowMore.setImageLevel(1);
                    accountHolder.mRvMonthDetail.setVisibility(View.VISIBLE);
                    accountHolder.mRvMonthDetail.setHasFixedSize(true);
                    accountHolder.mRvMonthDetail.setNestedScrollingEnabled(false);
                    accountHolder.mRvMonthDetail.setItemAnimator(new DefaultItemAnimator());
                    accountHolder.mRvMonthDetail.setLayoutManager(new FullyLinearLayoutManager(AccountingActivity.this));
                    MonthAccountAdapter monthAccountAdapter = new MonthAccountAdapter(AccountingActivity.this, monthMsg);
                    accountHolder.mRvMonthDetail.setAdapter(monthAccountAdapter);
                } else {
                    accountHolder.mIvShowMore.setImageLevel(0);
                    accountHolder.mRvMonthDetail.setVisibility(View.GONE);
                }
                switch (monthMsg.state) {
                    case TaskCard.TaskState.ACTIVE:
                        accountHolder.mTvState.setVisibility(View.GONE);
                        break;
                    case TaskCard.TaskState.REVOCABLE:
                        accountHolder.mTvState.setVisibility(View.VISIBLE);
                        accountHolder.mTvState.setText("撤销");
                        accountHolder.mTvState.setTextColor(getResources().getColor(R.color.base_blue));
                        break;
                }
            }
        }

        @Override
        public int getItemCount() {
            return 1 + showMonthDatas.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return HEADER_VIEW;
            } else if (position == 1) {
                return showMonthDatas.get(0).month == 0 ? ACCOUNT_CREATE_VIEW : CONTENT_VIEW;
            } else {
                return CONTENT_VIEW;
            }
        }

        @Override
        public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
            //当有展开卡片的月账单item不可见时，会自动收起展开卡片，需要手动重置展开索引
            if (holder instanceof AccountHolder && holder.getAdapterPosition() == isEditPosition) {
                isEditPosition = 0;
                isEdit = false;
                subEditPos = -1;
            }
            super.onViewDetachedFromWindow(holder);
        }

        /**
         * 头部标题视图
         **/
        class HeaderHolder extends RecyclerView.ViewHolder {
            TextView mTvHeader;

            public HeaderHolder(View itemView) {
                super(itemView);
                mTvHeader = (TextView) itemView.findViewById(R.id.tv_header);
            }
        }

        /**
         * 新建账单视图
         **/
        class NewAccountHolder extends RecyclerView.ViewHolder implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

            @BindView(R.id.edit_account_amount)
            TextInputLayout mEditAccountAmount;
            @BindView(R.id.amount_text)
            EditText mAmountText;
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
            @BindView(R.id.tv_save)
            TextView mTvSave;
            @BindView(R.id.edit_account_item)
            LinearLayout mEditAccountItem;
            @BindView(R.id.del_item)
            SlidingItem mDelItem;
            @BindView(R.id.dash_divider)
            View mDashDivider;
            private Accounting account;

            public NewAccountHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
                mDashDivider.setVisibility(View.GONE);
                mEditAccountAmount.getEditText().addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        amount = mEditAccountAmount.getEditText().getText().toString();
                        if (TextUtils.isEmpty(amount) || !AssistUtils.isNumber(amount) || Double.valueOf(amount) == 0) {
                            mTvSave.setTextColor(getResources().getColor(R.color.forbid_click_color));
                        } else {
                            mTvSave.setTextColor(getResources().getColor(R.color.white));
                        }
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
                        memo = mEditAccountMemo.getText().toString().trim();
                    }
                });
            }

            @OnClick({R.id.ll_account_atype, R.id.ll_account_etype, R.id.edit_account_date_box, R.id.edit_account_time, R.id.tv_save, R.id.tv_cancel})
            public void onClick(View view) {
                mEtBalance.setFocusable(false);
                account = showMonthDatas.get(0).taskCards.get(0).t;
                switch (view.getId()) {
                    case R.id.ll_account_atype:
                        if (account.getAtype() == 0) {   //支出
                            account.setAtype(1);
                            mEditAccountAtype.setText("收入");
                        } else {
                            account.setAtype(0);
                            mEditAccountAtype.setText("支出");
                        }
                        break;
                    case R.id.ll_account_etype:
                        String cEtype = mEditAccountEtype.getText().toString();
                        if (account.getAtype() == 0) {
                            ItemExpenseDialog expenseDialog = new ItemExpenseDialog(AccountingActivity.this, cEtype, new ItemExpenseDialog.OnItemExpenseListener() {
                                @Override
                                public void onItemSelected(String item) {
                                    mEditAccountEtype.setText(item);
                                }
                            });
                            expenseDialog.show();
                        } else {
                            ItemIncomeDialog incomeDialog = new ItemIncomeDialog(AccountingActivity.this, cEtype, new ItemIncomeDialog.OnItemIncomeListener() {
                                @Override
                                public void onIncomeSelected(String item) {
                                    mEditAccountEtype.setText(item);
                                }
                            });
                            incomeDialog.show();
                        }
                        account.setEtype(mEditAccountEtype.getText().toString());
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
                        datePicker.setAccentColor(getResources().getColor(R.color.base_blue));
                        datePicker.show(AccountingActivity.this.getFragmentManager(), "DatePicker");
                        break;
                    case R.id.edit_account_time:
                        SimpleDate rTime = new SimpleDate(mEditAccountTime.getText().toString());
                        TimePickerDialog timePicker = TimePickerDialog.newInstance(
                                this,
                                rTime.getHour(),
                                rTime.getMinute(),
                                true);
                        timePicker.setAccentColor(getResources().getColor(R.color.base_blue));
                        timePicker.show(AccountingActivity.this.getFragmentManager(), "TimePicker");
                        break;
                    case R.id.tv_save:
                        if (TextUtils.isEmpty(amount) || !AssistUtils.isNumber(amount) || Double.valueOf(amount) == 0) {
                            mEditAccountAmount.setErrorEnabled(true);
                            mEditAccountAmount.setError("请输入金额");
                            return;
                        }
                        mEditAccountAmount.setErrorEnabled(false);
                        //创建完成，添加图标重新显示
                        mFabAdd.show();
                        //设置数据
                        account.setAmount(Double.valueOf(amount));
                        account.setMemo(memo);
                        account.setEtype(mEditAccountEtype.getText().toString());
                        String date = mEditAccountDate.getText().toString();
                        String time = mEditAccountTime.getText().toString();
                        account.setRdate(TimeUtils.parseDateTime(date + " " + time));
                        Calendar cl = Calendar.getInstance();
                        cl.setTime(account.getRdate());
                        account.setMonth(cl.get(Calendar.MONTH) + 1);
                        account.setCreated(new Date());
                        account.setSynced(false);
                        //保存账单记录
                        mAssistDao.insertAccount(account);
                        List<TaskCard<Accounting>> list = new ArrayList<>();
                        list.add(new TaskCard<>(account, TaskCard.TaskState.ACTIVE));
                        updateBalance(TYPE_ADD, list);
                        loadData(cl.get(Calendar.YEAR), account.getMonth());
                        //同步账单记录
                        AssistEntityDao.create().sync(mBillEntityDao);
                        memo = "";
                        amount = "";
                        isEditPosition = 0;
                        break;
                    case R.id.tv_cancel:
                        //取消新建，添加图标重新显示
                        mFabAdd.show();
                        isEditPosition = 0;
                        memo = "";
                        amount = "";
                        showMonthDatas.remove(getAdapterPosition() - 1);
                        notifyItemRemoved(getAdapterPosition());
                        break;
                }
            }

            @Override
            public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                String month = monthOfYear + 1 < 10 ? "0" + (monthOfYear + 1) : "" + (monthOfYear + 1);
                String day = dayOfMonth < 10 ? "0" + dayOfMonth : "" + dayOfMonth;
                mEditAccountDate.setText(new StringBuilder().append(year).append("年").append(month).append("月").append(day).append("日").toString());

                String date = mEditAccountDate.getText().toString();
                String time = mEditAccountTime.getText().toString();
                account.setRdate(TimeUtils.parseDateTime(date + " " + time));
            }

            @Override
            public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
                String hour = hourOfDay >= 10 ? "" + hourOfDay : "0" + hourOfDay;
                String min = minute >= 10 ? "" + minute : "0" + minute;
                mEditAccountTime.setText(hour + ":" + min);

                String date = mEditAccountDate.getText().toString();
                String time = mEditAccountTime.getText().toString();
                account.setRdate(TimeUtils.parseDateTime(date + " " + time));
            }
        }

        /**
         * 月份账单记录视图
         **/
        class AccountHolder extends RecyclerView.ViewHolder implements SlidingItem.OnSlidingItemListener {

            @BindView(R.id.account_month)
            TextView mAccountMonth;
            @BindView(R.id.tv_income)
            TextView mTvIncome;
            @BindView(R.id.tv_expense)
            TextView mTvExpense;
            @BindView(R.id.iv_show_more)
            ImageView mIvShowMore;
            @BindView(R.id.account_item)
            SlidingItem mAccountItem;
            @BindView(R.id.tv_state)
            TextView mTvState;
            @BindView(R.id.rv_month_detail)
            RecyclerView mRvMonthDetail;

            public AccountHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
                mAccountItem.setOnSlidingItemListener(this);
            }

            @OnClick(R.id.tv_state)
            public void onClick() {
                MonthAccount ma = showMonthDatas.get(getAdapterPosition() - 1);
                if (ma.state == TaskCard.TaskState.REVOCABLE) {
                    ma.state = TaskCard.TaskState.ACTIVE;
                    //恢复数据
                    updateData(ma, true);
                    notifyItemChanged(getAdapterPosition());
                    updateBalance(TYPE_ADD, ma.taskCards);
                }
            }

            @Override
            public void onSliding(SlidingItem item) {
                if (lastItem != null && lastItem != item)
                    lastItem.hide();
            }

            @Override
            public void onBtnClick(View v) {
                for (int i = 0; i < showMonthDatas.size(); i++) {
                    MonthAccount ma = showMonthDatas.get(i);
                    if (ma.state == TaskCard.TaskState.REVOCABLE) {
                        //移除该月数据
                        showMonthDatas.remove(i);
                        notifyItemRemoved(i + 1);
                        break;
                    }
                }
                MonthAccount monthMsg = showMonthDatas.get(getAdapterPosition() - 1);
                monthMsg.state = TaskCard.TaskState.REVOCABLE;
                monthMsg.isExpended = false;
                //从数据库中删除记录
                updateData(monthMsg, false);
                notifyItemChanged(getAdapterPosition());
                updateBalance(TYPE_DELETE, monthMsg.taskCards);
            }

            /**
             * 操作数据库
             *
             * @param isAdd true:插入数据  false:删除数据
             **/
            private void updateData(MonthAccount monthMsg, boolean isAdd) {
                List<Accounting> list = new ArrayList<>();
                List<TaskCard<Accounting>> taskCards = monthMsg.taskCards;
                for (TaskCard<Accounting> card : taskCards) {
                    card.taskState = TaskCard.TaskState.ACTIVE;
                    card.t.setSynced(false);
                    card.t.setRecyle(isAdd ? 0 : 1);
                    list.add(card.t);
                }
                if (isAdd)
                    mAssistDao.insertAccountInIx(list);
                else
                    mAssistDao.deleteAccountInIx(list);
                //同步账单记录
                AssistEntityDao.create().sync(mBillEntityDao);
            }

            @Override
            public void onContentClick(View v) {
                mEtBalance.setFocusable(false);
                MonthAccount monthMsg = showMonthDatas.get(getAdapterPosition() - 1);
                // 收起月账单卡片时清除编辑卡片索引
                if (monthMsg.isExpended && isEditPosition == getAdapterPosition() && showMonthDatas.get(0).month != 0) {      //展开中，点击收起
                    isEditPosition = 0;
                }
                monthMsg.isExpended = !monthMsg.isExpended;
                notifyItemChanged(getAdapterPosition());
            }

            @Override
            public void onExpanded(SlidingItem item) {
                lastItem = item;
            }
        }
    }

    @Override
    public void setEditTextFocusable(boolean focusable) {
        mEtBalance.setFocusable(focusable);
    }


}
