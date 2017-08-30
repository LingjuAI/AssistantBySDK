package com.lingju.assistant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ToxicBakery.viewpager.transforms.AccordionTransformer;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.activity.event.TrackPlayEvent;
import com.lingju.assistant.activity.index.model.TingPagerAdapter;
import com.lingju.assistant.view.TingPlayerComponent;
import com.lingju.util.ScreenUtil;
import com.ximalaya.ting.android.opensdk.model.track.Track;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Ken on 2017/6/8.
 */
public class TingAudioActivity extends GoBackActivity {

    @BindView(R.id.tb_ting)
    Toolbar mTbTing;
    @BindView(R.id.tab_ting_title)
    TabLayout mTabTingTitle;
    @BindView(R.id.pager_ting_audio)
    ViewPager mPagerTingAudio;
    @BindView(R.id.status_bar)
    View mStatusBar;
    @BindView(R.id.ting_player_box)
    TingPlayerComponent mTingPlayerBox;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ting_audio);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        //设置模拟状态栏的高度
        ViewGroup.LayoutParams layoutParams = mStatusBar.getLayoutParams();
        layoutParams.height = ScreenUtil.getStatusBarHeight(this);
        mStatusBar.setLayoutParams(layoutParams);
        setSupportActionBar(mTbTing);
        // Navigation Icon 要設定在 setSupoortActionBar 才有作用
        mTbTing.setNavigationIcon(R.drawable.back_arrow);
        mTbTing.inflateMenu(R.menu.search_menu);
        mTbTing.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBack();
            }
        });
        mTbTing.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.ting_search) {
                    Intent intent = new Intent(TingAudioActivity.this, TingSearchActivity.class);
                    startActivity(intent);
                    goInto();
                }
                return true;
            }
        });
        initPager();
    }


    private void initPager() {
        TingPagerAdapter adapter = new TingPagerAdapter(getSupportFragmentManager(), this);
        mPagerTingAudio.setAdapter(adapter);
        mPagerTingAudio.setPageTransformer(true, new AccordionTransformer());
        mTabTingTitle.setTabMode(TabLayout.MODE_SCROLLABLE);
        mTabTingTitle.setupWithViewPager(mPagerTingAudio);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTrackPlayEvent(TrackPlayEvent e) {
        if (e.isPlaying()) {
            // TODO: 2017/6/14 只针对Track类型进行处理
            mTingPlayerBox.Play((Track) e.getPlayTrack());
        } else {
            mTingPlayerBox.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTingPlayerBox.initPlayerBox();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
}
