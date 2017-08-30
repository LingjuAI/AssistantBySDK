package com.lingju.assistant.activity.index.presenter;

import android.content.Intent;
import android.text.TextUtils;

import com.lingju.assistant.activity.AccountingEditActivity;
import com.lingju.assistant.activity.DatePickerActivity;
import com.lingju.assistant.activity.ItemExpenseActivity;
import com.lingju.assistant.activity.ItemIncomeActivity;
import com.lingju.assistant.activity.index.IAccountEdit;
import com.lingju.assistant.view.CaculatorDialog;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.model.Accounting;
import com.lingju.model.dao.AssistDao;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Ken on 2016/12/17.
 */
public class AccountingEditPresenter implements IAccountEdit.IPresenter {

    private AccountingEditActivity mAccountingEditView;
    private AssistDao mDao;
    private SimpleDateFormat sf = new SimpleDateFormat("yyyy年MM月dd日");
    /**
     * 账单图标类型 0: 支出  1：收入
     **/
    private int mAccountType;
    private Intent mForIntent;
    private Accounting mAccounting;
    private long time;
    private double amount;
    private String tempProject;

    public AccountingEditPresenter(IAccountEdit.IAccountingEditView view) {
        this.mAccountingEditView = (AccountingEditActivity) view;
        mDao = AssistDao.getInstance();
    }

    @Override
    public void quit() {
        mAccountingEditView.goBack();
    }

    @Override
    public void initData(Intent intent) {
        this.mForIntent = intent;
        long id = 0;
        if (mForIntent != null && (id = mForIntent.getLongExtra(AccountingEditActivity.ID, 0)) > 0) {
            mAccounting = mDao.findAccountById(id);
        }
        if (mAccounting != null) {
            mAccountType = mAccounting.getAtype();
            time = mAccounting.getCreated() != null ? mAccounting.getCreated().getTime() : System.currentTimeMillis();
            amount = mAccounting.getAmount();
            mAccountingEditView.switchAccountType(mAccountType);
            mAccountingEditView.setProject(mAccounting.getEtype());
            tempProject = mAccounting.getEtype();
            mAccountingEditView.setMemoContent(mAccounting.getMemo());
            mAccountingEditView.setDateText(sf.format(mAccounting.getCreated()));
            formatAndSetAmount(mAccounting.getAmount());
        } else {
            time = System.currentTimeMillis();
            mAccountingEditView.setDateText(sf.format(new Date(time)));
            initProject(0);
        }
    }

    @Override
    public void initProject(int type) {
        StringBuilder project = new StringBuilder();
        switch (type) {
            case 0:    //支出
                project.append("餐饮,");
                Calendar cl = Calendar.getInstance();
                int hour = cl.get(Calendar.HOUR_OF_DAY);
                if (3 <= hour && hour < 11) {
                    project.append("早餐");
                } else if (11 <= hour && hour < 17) {
                    project.append("午餐");
                } else if (17 <= hour && hour < 23) {
                    project.append("晚餐");
                } else {
                    project.append("夜宵");
                }
                break;
            case 1:     //收入
                project.append("工作工资");
                break;
        }
        mAccountingEditView.setProject(project.toString());
    }

    @Override
    public void toSetAmount() {
        mAccountingEditView.showDialog(amount, caculatorListener);
    }

    @Override
    public void formatAndSetAmount(double result) {
        StringBuilder amount = new StringBuilder();
        amount.append(result / 100);
        int i = (int) (result % 100);
        if (i > 0) {
            amount.append(".");
            if (i < 10)
                amount.append("0");
            amount.append(i);
        }
        amount.append("元");
        mAccountingEditView.setAmount(amount.toString());
    }

    @Override
    public void toSetProject() {
        Intent intent = new Intent(mAccountingEditView, mAccountType == 0 ? ItemExpenseActivity.class : ItemIncomeActivity.class);
        if (!TextUtils.isEmpty(tempProject)) {
            intent.putExtra(ItemExpenseActivity.ITEM, tempProject);
        }
        mAccountingEditView.startActivityForResult(intent, ItemExpenseActivity.FOR_SELECT_ITEM);
        mAccountingEditView.goInto();
    }

    @Override
    public void toSetDate() {
        Intent intent = new Intent(mAccountingEditView, DatePickerActivity.class);
        intent.putExtra(DatePickerActivity.DATE, time);
        mAccountingEditView.startActivityForResult(intent, DatePickerActivity.FOR_DATE_RESULT);
        mAccountingEditView.goInto();
    }


    @Override
    public void updateAccounting(Intent intent, int resultCode) {
        if (intent != null) {
            switch (resultCode) {
                case DatePickerActivity.FOR_DATE_RESULT:
                    time = intent.getLongExtra(DatePickerActivity.DATE, System.currentTimeMillis());
                    mAccountingEditView.setDateText(sf.format(new Date(time)));
                    break;
                case ItemExpenseActivity.FOR_SELECT_ITEM:
                    tempProject = intent.getStringExtra(ItemExpenseActivity.ITEM);
                    mAccountingEditView.setProject(tempProject);
                    break;
                case ItemIncomeActivity.FOR_SELECT_ITEM:
                    tempProject = intent.getStringExtra(ItemIncomeActivity.ITEM);
                    mAccountingEditView.setProject(tempProject);
                    break;
            }
        }
    }

    @Override
    public void deleteAccount() {
        if (mAccounting != null) {
            mDao.deleteAccount(mAccounting);
            mForIntent.putExtra(AccountingEditActivity.TYPE, AccountingEditActivity.UPDATE);
            mAccountingEditView.setResult(AccountingEditActivity.FOR_EDIT, mForIntent);
        }
        mAccountingEditView.goBack();
    }

    @Override
    public void saveAccount() {
        if (amount <= 0) {
            new CommonDialog(mAccountingEditView, "温馨提示", "请输入金额", "马上设置").setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                @Override
                public void onConfirm() {
                    mAccountingEditView.showDialog(amount, caculatorListener);
                }
            }).show();
            return;
        }
        if(TextUtils.isEmpty(tempProject)) {
            new CommonDialog(mAccountingEditView, "温馨提示", "请输入账单项目", "马上设置").show();
            return;
        }
        if(mAccounting == null)
            mAccounting = new Accounting();
        /* 填充账单信息 */
        mAccounting.setAmount(amount);
        mAccounting.setAtype(mAccountType);
        mAccounting.setEtype(tempProject);
        mAccounting.setCreated(new Date(time));
        mAccounting.setMemo(mAccountingEditView.getMemo());
        /* 将记录保存到数据库 */
        if(mAccounting.getId() != null) {
            mDao.updateAccount(mAccounting);
            mForIntent.putExtra(AccountingEditActivity.TYPE, AccountingEditActivity.UPDATE);
        }else {
            mDao.insertAccount(mAccounting);
            mForIntent.putExtra(AccountingEditActivity.TYPE, AccountingEditActivity.INSERT);
        }
        mAccountingEditView.setResult(AccountingEditActivity.FOR_EDIT, mForIntent);
        quit();
    }

    @Override
    public int getType() {
        return mAccountType = mAccountType == 0 ? 1 : 0;
    }

    @Override
    public void changeAccountType(int type) {
        mAccountingEditView.switchAccountType(type);
        initProject(type);
    }

    private CaculatorDialog.OnResultListener caculatorListener = new CaculatorDialog.OnResultListener() {

        @Override
        public void onResult(double result) {
            amount = result;
            formatAndSetAmount(result);
            mAccountingEditView.cancelDialog();
        }
    };
}
