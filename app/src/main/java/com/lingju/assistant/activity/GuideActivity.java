package com.lingju.assistant.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.LevelListDrawable;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.ToxicBakery.viewpager.transforms.ZoomOutTranformer;
import com.lingju.assistant.AppConfig;
import com.lingju.assistant.R;
import com.lingju.assistant.view.CommonDialog;
import com.lingju.common.log.Log;
import com.tencent.stat.StatConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class GuideActivity extends Activity {
    private final static String TAG = "GuideActivity";
    private Timer timer;
    private ViewPager viewPager;
    private LinearLayout mllDot;
    private List<ImageView> guidePages;
    private int[] srcIds = {R.drawable.guide_navi, R.drawable.guide_call, R.drawable.guide_account, R.drawable.pic_loading};
    private final static String speechServicePackage = "com.iflytek.speechcloud";
    private Button mBeginBt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        // androidManifest.xml指定本activity最先启动
        //  ((AppConfig) getApplication()).onMainProgressCreate();
        if (!AppConfig.NewInstallFirstOpen || AppConfig.ShowGuide) {
            into();
            return;
        }
        setContentView(R.layout.activity_guide2);
        viewPager = (ViewPager) findViewById(R.id.vp_guide);
        mllDot = (LinearLayout) findViewById(R.id.ll_dot);
        initData();
        mBeginBt = (Button) findViewById(R.id.guide_begin_bt);
        mBeginBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //取消自动开启下一个页面定时器
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
                showDialog();
            }
        });
        viewPager.setAdapter(adapter);
        viewPager.setPageTransformer(true, new ZoomOutTranformer());
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int p) {
                setDot(p);
                if (p == guidePages.size() - 1) {
                    mBeginBt.setVisibility(View.VISIBLE);
                    /*if (timer == null) {
                        timer = new Timer();
                        timer.schedule(new AutoOpenTask(), 10000);
                    }*/
                }
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {

            }

            @Override
            public void onPageScrollStateChanged(int arg0) {

            }
        });
        setDot(0);
    }

    private void initData() {
        guidePages = new ArrayList<>();
        for (int i = 0; i < srcIds.length; i++) {
            ImageView iv = new ImageView(this);
            iv.setImageResource(srcIds[i]);
            iv.setScaleType(ImageView.ScaleType.CENTER);
            guidePages.add(iv);
        }
    }

    public void setDot(int position) {
        mBeginBt.setVisibility(View.GONE);
        for (int i = 0; i < mllDot.getChildCount(); i++) {
            View dotView = mllDot.getChildAt(i);
            LevelListDrawable ld = (LevelListDrawable) dotView.getBackground();
            ld.setLevel(position == i ? 1 : 0);
        }
    }

    /**
     * 进入下一个页面
     **/
    private void into() {
        startActivity(new Intent(GuideActivity.this, StartUpActivity.class));
        finish();
        overridePendingTransition(0, 0);
    }

    private void showDialog() {
        new CommonDialog(this, "温馨提示", "程序接下来会申请几个权限，我们建议您选择同意。拒绝权限可能导致程序不能正常工作，" +
                "拒绝后如果想再次赋予权限可以到系统权限设置界面设置", "我知道了")
                .setOnConfirmListener(new CommonDialog.OnConfirmListener() {
                    @Override
                    public void onConfirm() {
                        into();
                    }
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "GuideActivity onDestroy()");
        super.onDestroy();
        /* 取消自动开启下一个页面定时器 */
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void initMTAConfig(boolean config) {
        //设置最大缓存未发送消息个数（默认1024）
        StatConfig.setMaxStoreEventCount(1024);

        //缓存消息的数量超过阈值时，最早的消息会被丢弃。
        StatConfig.setMaxBatchReportCount(30);

        //（仅在发送策略为PERIOD时有效）设置间隔时间（默认为24*60，即1天）
        StatConfig.setSendPeriodMinutes(1440);

        //开启SDK LogCat开关（默认false）
        StatConfig.setDebugEnable(config);
    }

    class AutoOpenTask extends TimerTask {
        @Override
        public void run() {
            Log.i(TAG, "AutoOpenTask run");
            Single.just(0)
                    .doOnSubscribe(new Consumer<Disposable>() {
                        @Override
                        public void accept(Disposable disposable) throws Exception {
                            showDialog();
                        }
                    })
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe();
        }
    }


    PagerAdapter adapter = new PagerAdapter() {

        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(guidePages.get(position));
        }

        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(guidePages.get(position));
            return guidePages.get(position);
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public int getCount() {
            return guidePages.size();
        }
    };


}
