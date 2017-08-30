package com.lingju.assistant.activity;

import android.content.Intent;
import android.graphics.drawable.LevelListDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.activity.index.IAccountEdit;
import com.lingju.assistant.activity.index.presenter.AccountingEditPresenter;
import com.lingju.assistant.view.CaculatorDialog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2016/12/17.
 */
public class AccountingEditActivity extends GoBackActivity implements IAccountEdit.IAccountingEditView {

    public final static String ID = "id";
    public final static String TYPE = "type";
    public final static int FOR_EDIT = 1;
    public final static int UPDATE = 1;
    public final static int INSERT = 2;
    @BindView(R.id.aib_type1)
    ImageButton mAibType1;
    @BindView(R.id.aib_amount1)
    TextView mAibAmount1;
    @BindView(R.id.aib_project1)
    TextView mAibProject1;
    @BindView(R.id.aib_time)
    TextView mAibTime;
    @BindView(R.id.aib_memo)
    EditText mAibMemo;

    private IAccountEdit.IPresenter mPresenter;
    private CaculatorDialog caculatorDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounting_edit);
        ButterKnife.bind(this);
        mPresenter = new AccountingEditPresenter(this);
        mPresenter.initData(getIntent());
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mPresenter.updateAccounting(data, resultCode);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @OnClick({R.id.aae_back, R.id.aae_del, R.id.aib_type1, R.id.aae_confirm, R.id.aae_cancel
            , R.id.aib_amount_box1, R.id.aib_project_box1, R.id.aib_time})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.aae_back:
            case R.id.aae_cancel:
                mPresenter.quit();
                break;
            case R.id.aae_del:
                mPresenter.deleteAccount();
                break;
            case R.id.aib_type1:
                mPresenter.changeAccountType(mPresenter.getType());
                break;
            case R.id.aae_confirm:
                mPresenter.saveAccount();
                break;
            case R.id.aib_amount_box1:
                mPresenter.toSetAmount();
                break;
            case R.id.aib_project_box1:
                mPresenter.toSetProject();
                break;
            case R.id.aib_time:
                mPresenter.toSetDate();
                break;
        }
    }

    @Override
    public void setAmount(String amount) {
        mAibAmount1.setText(amount);
    }

    @Override
    public void setProject(String project) {
        mAibProject1.setText(project);
    }

    @Override
    public void setDateText(String date) {
        mAibTime.setText(date);
    }

    @Override
    public void setMemoContent(String memo) {
        mAibMemo.setText(memo);
    }

    @Override
    public String getMemo() {
        return mAibMemo.getText().toString().trim();
    }

    @Override
    public void switchAccountType(int type) {
        LevelListDrawable ld = (LevelListDrawable) mAibType1.getBackground();
        ld.setLevel(type);
    }

    @Override
    public void showDialog(double amount, CaculatorDialog.OnResultListener listener) {
        caculatorDialog = new CaculatorDialog(this, amount, listener);
        caculatorDialog.show();
    }

    @Override
    public void cancelDialog() {
        if (caculatorDialog != null) {
            caculatorDialog.cancel();
            caculatorDialog = null;
        }
    }
}
