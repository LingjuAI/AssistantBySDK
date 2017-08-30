package com.lingju.assistant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.LingjuSwipeUpLoadRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.activity.event.TrackPlayEvent;
import com.lingju.assistant.activity.index.model.TingAlbumDetailAdapter;
import com.lingju.assistant.view.TingPlayerComponent;
import com.lingju.util.ScreenUtil;
import com.lsjwzh.widget.materialloadingprogressbar.CircleProgressBar;
import com.ximalaya.ting.android.opensdk.model.track.Track;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2017/6/12.
 */
public class TingAlbumDetailActivity extends GoBackActivity implements LingjuSwipeUpLoadRefreshLayout.OnRefreshListener, TingPlayerComponent.OnPlayStateListener {

    public static final String ALBUM_ID = "album_id";
    public static final String ALBUM_TYPE = "album_type";
    public static final int XIMALAYA = 0;
    public static final int KAOLA = 1;
    @BindView(R.id.status_bar)
    View mStatusBar;
    @BindView(R.id.rv_track)
    RecyclerView mRvTrack;
    @BindView(R.id.upload_more)
    LingjuSwipeUpLoadRefreshLayout mUploadMore;
    @BindView(R.id.ting_player_box)
    TingPlayerComponent mTingPlayerBox;
    @BindView(R.id.cpb_loading)
    CircleProgressBar mCpbLoading;

    private TingAlbumDetailAdapter mDetailAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ting_detail);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        //设置模拟状态栏的高度
        ViewGroup.LayoutParams layoutParams = mStatusBar.getLayoutParams();
        layoutParams.height = ScreenUtil.getStatusBarHeight(this);
        mStatusBar.setLayoutParams(layoutParams);
        initView();
        loadData();
    }

    private void initView() {
        long albumId = 0;
        int type=0;
        if (getIntent() != null){
            albumId = getIntent().getLongExtra(ALBUM_ID, 0);
            type = getIntent().getIntExtra(ALBUM_TYPE, XIMALAYA);
        }
        mDetailAdapter = new TingAlbumDetailAdapter(this, albumId, type);
        mRvTrack.setHasFixedSize(true);
        mRvTrack.setLayoutManager(new LinearLayoutManager(this));
        mRvTrack.setAdapter(mDetailAdapter);
        mUploadMore.setOnRefreshListener(this);
        mTingPlayerBox.initPlayerBox();
        mTingPlayerBox.setOnPlayStateListener(this);
    }

    private void loadData() {
        mCpbLoading.setVisibility(View.VISIBLE);
        mDetailAdapter.getAlbumInfo();
    }

    public void hiddenLoading() {
        mCpbLoading.setVisibility(View.GONE);
    }

    public void setLastTrack(Track track){mTingPlayerBox.setLastTrack(track);}

    @OnClick(R.id.tv_back)
    public void onClick() {
        goBack();
    }

    @Override
    public void onUpPullRefresh() {
        mUploadMore.setRefreshing(false);
        mDetailAdapter.fillData(true);
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        long albumId = intent.getLongExtra(ALBUM_ID, 0);
        int type = intent.getIntExtra(ALBUM_TYPE, XIMALAYA);
        mDetailAdapter.setAlbumId(albumId);
        mDetailAdapter.setAlbumType(type);
        mDetailAdapter.getAlbumInfo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onTrackPause() {
        mDetailAdapter.notifyDataSetChanged();
    }

    @Override
    public void onTrackPlay(long id) {
        mDetailAdapter.setPlayTrackId(id);
        mDetailAdapter.notifyDataSetChanged();
    }
}
