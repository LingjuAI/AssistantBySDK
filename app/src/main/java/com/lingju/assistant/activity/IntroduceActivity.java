package com.lingju.assistant.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;

import java.util.List;

public class IntroduceActivity extends GoBackActivity implements View.OnClickListener {
    /**
     * 已发布本APP的应用市场的包名
     */
    String pkgs[] = new String[]{"com.tencent.android.qqdownloader"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introduce);

        findViewById(R.id.rl_grade).setOnClickListener(this);
        findViewById(R.id.rl_feedback).setOnClickListener(this);
        findViewById(R.id.rl_about).setOnClickListener(this);
        findViewById(R.id.rl_connect).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_grade:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri;
                if (checkMarketInstall()) {
                    uri = Uri.parse("market://details?id=com.lingju.assistant");
                    intent.setPackage(pkgs[0]);
                } else {
                    uri = Uri.parse("http://android.myapp.com/myapp/detail.htm?apkName=com.lingju.assistant");
                }
                intent.setData(uri);
                startActivity(intent);
                break;
            case R.id.rl_feedback:
                startActivity(new Intent(this, FeedbackActivity.class));
                break;
            case R.id.rl_about:
                startActivity(new Intent(this, AboutLJActivity.class));
                break;
            case R.id.rl_connect:
                startActivity(new Intent(this, ConnectUsActivity.class));
                break;
        }
        goInto();
    }

    /**
     * 验证手机是否安装了发布了本APP的应用市场
     **/
    private boolean checkMarketInstall() {
        List<PackageInfo> packageInfos = getPackageManager().getInstalledPackages(0);
        if (packageInfos.size() == 0) {
            // TODO: 2017/4/11 读取应用列表失败（提醒用户检查是否允许读取应用列表权限）
            return false;
        }
        for (PackageInfo info : packageInfos) {
            for (String packageName : pkgs) {
                if (info.packageName.equals(packageName))
                    return true;
            }
        }
        return false;
    }
}
