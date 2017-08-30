package com.lingju.assistant.activity.index;

import android.content.Intent;

import com.lingju.assistant.view.CaculatorDialog;

/**
 * Created by Ken on 2016/12/17.
 */
public interface IAccountEdit {

    interface IAccountingEditView {
        /**
         * 设置金额文本
         **/
        void setAmount(String amount);

        /**
         * 设置项目文本
         **/
        void setProject(String project);

        /**
         * 设置日期
         **/
        void setDateText(String date);

        /**
         * 设置备注
         **/
        void setMemoContent(String memo);

        /**
         * 获取备注文本
         **/
        String getMemo();

        /**
         * 设置账单类型图标
         **/
        void switchAccountType(int type);

        void showDialog(double amount, CaculatorDialog.OnResultListener listener);

        void cancelDialog();
    }

    interface IPresenter {
        /**
         * 退出页面
         **/
        void quit();

        /**
         * 初始化数据
         **/
        void initData(Intent intent);

        /**
         * 初始化默认账单项目
         **/
        void initProject(int type);

        /**
         * 设置金额
         **/
        void toSetAmount();

        /**
         * 格式化并设置金额
         **/
        void formatAndSetAmount(double result);

        /**
         * 设置项目
         **/
        void toSetProject();

        /**
         * 设置日期
         **/
        void toSetDate();

        /**
         * 更新账单属性文本
         **/
        void updateAccounting(Intent intent, int resultCode);

        /**
         * 删除记账
         **/
        void deleteAccount();

        /**
         * 保存记账
         **/
        void saveAccount();


        /**
         * 获取账单图标类型
         **/
        int getType();

        /**
         * 改变账单类型
         **/
        void changeAccountType(int type);
    }
}
