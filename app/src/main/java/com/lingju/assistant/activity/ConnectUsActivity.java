package com.lingju.assistant.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.view.CommonDialog;

/**
 * Created by Ken on 2016/11/25.
 */
public class ConnectUsActivity extends GoBackActivity implements View.OnClickListener {

    private TextView mTvWebsite;
    private TextView mTvEmail;
    private TextView mTvTencent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_us);
        mTvWebsite = (TextView) findViewById(R.id.tv_website);
        mTvEmail = (TextView) findViewById(R.id.tv_email);
        mTvTencent = (TextView) findViewById(R.id.tv_qq);

        mTvWebsite.setOnClickListener(this);
        mTvEmail.setOnClickListener(this);
        mTvTencent.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_website:
                gotoWebsite(mTvWebsite.getText().toString());
                break;
            case R.id.tv_email:
                gotoEmail(mTvEmail.getText().toString());
                break;

            case R.id.tv_qq:
                joinQQGroup("1ieCg1PnEW737nxrxou-kEwSD0hBiK_m");
                break;
        }
    }

    /**
     * 跳转到官网
     **/
    private void gotoWebsite(String url) {
        try {
            Uri uri = Uri.parse("http://" + url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            new CommonDialog(this, "提示信息", "检测到您没有可用的浏览器，请先安装！", "确定").show();
        }

    }

    /****************
     * 发起添加群流程。群号：灵聚智能公开测试群(335698184) 的 key 为： 1ieCg1PnEW737nxrxou-kEwSD0hBiK_m
     * 调用 joinQQGroup(1ieCg1PnEW737nxrxou-kEwSD0hBiK_m) 即可发起手Q客户端申请加群 灵聚智能公开测试群(335698184)
     *
     * @param key 由官网生成的key
     * @return 返回true表示呼起手Q成功，返回fals表示呼起失败
     ******************/
    public void joinQQGroup(final String key) {
        new CommonDialog(this, "提示信息", "是否跳转到腾讯QQ？", "取消", "确定")
                .setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                    @Override
                    public void onConfirm() {
                        Intent intent = new Intent();
                        intent.setData(Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D" + key));
                        // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面
                        //         intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            startActivity(intent);
                        } catch (Exception e) {
                            // 未安装手Q或安装的版本不支持
                            new CommonDialog(ConnectUsActivity.this, "提示信息", "检测到您没有安装手机QQ，请先安装相关软件！", "确定").show();
                        }
                    }
                })
                .show();

    }


    /**
     * 跳转到邮箱
     **/
    private void gotoEmail(final String acceptSite) {
        /* 匹配发送消息action */
        /*Intent email = new Intent(Intent.ACTION_SEND);
        *//* 不带附件发送邮件（纯文本） *//*
        email.setType("plain/text");
        *//* 收件人（可多个） *//*
        email.putExtra(Intent.EXTRA_EMAIL, acceptSite);
        *//* 标题 *//*
        email.putExtra(Intent.EXTRA_SUBJECT, "");
        *//* 内容 *//*
        email.putExtra(Intent.EXTRA_TEXT, "");
        startActivity(Intent.createChooser(email, "请选择发送邮箱"));*/
        new CommonDialog(this, "提示信息", "是否跳转到邮箱软件？", "取消", "确定")
                .setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                    @Override
                    public void onConfirm() {
                        Uri uri = Uri.parse("mailto:" + acceptSite);
                        Intent it = new Intent(Intent.ACTION_SENDTO, uri);
                        try {

                            startActivity(it);
                        } catch (ActivityNotFoundException e) {
                            new CommonDialog(ConnectUsActivity.this, "提示信息", "检测到您没有可用的邮箱软件，请先安装！", "确定").show();
                        }
                    }
                })
                .show();

    }
}
