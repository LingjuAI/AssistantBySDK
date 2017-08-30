package com.lingju.assistant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.LingjuSwipeUpLoadRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.activity.event.OnItemClickListener;
import com.lingju.assistant.activity.event.TrackPlayEvent;
import com.lingju.assistant.activity.index.ITingSearch;
import com.lingju.assistant.activity.index.model.XmlyAlbumAdapter;
import com.lingju.assistant.activity.index.model.base.BaseAlbumAdapter;
import com.lingju.assistant.activity.index.presenter.TingSearchPresenter;
import com.lingju.assistant.view.RealTimeUpdateSearchBox;
import com.lingju.assistant.view.TingPlayerComponent;
import com.lingju.util.ScreenUtil;
import com.lsjwzh.widget.materialloadingprogressbar.CircleProgressBar;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.zhy.view.flowlayout.FlowLayout;
import com.zhy.view.flowlayout.TagAdapter;
import com.zhy.view.flowlayout.TagFlowLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2017/6/8.
 */
public class TingSearchActivity extends GoBackActivity implements ITingSearch.ITingSearchView, LingjuSwipeUpLoadRefreshLayout.OnRefreshListener, RealTimeUpdateSearchBox.OnSearchListener, TagFlowLayout.OnTagClickListener {

    public final static String ALBUM_LIST = "album_list";
    @BindView(R.id.status_bar)
    View mStatusBar;
    @BindView(R.id.ting_search_box)
    RealTimeUpdateSearchBox mTingSearchBox;
    @BindView(R.id.fl_history)
    TagFlowLayout mFlHistory;
    @BindView(R.id.rl_history_box)
    RelativeLayout mRlHistoryBox;
    @BindView(R.id.fl_hot_tag)
    TagFlowLayout mFlHotTag;
    @BindView(R.id.rv_search_album)
    RecyclerView mRvSearchAlbum;
    @BindView(R.id.upload_more)
    LingjuSwipeUpLoadRefreshLayout mUploadMore;
    @BindView(R.id.ting_player_box)
    TingPlayerComponent mTingPlayerBox;
    @BindView(R.id.cpb_loading)
    CircleProgressBar mCpbLoading;
    @BindView(R.id.search_divider)
    View mSearchDivider;
    @BindView(R.id.rl_hot_tag_box)
    RelativeLayout mRlHotTagBox;
    private ITingSearch.IPresenter mSearchPresenter;
    private BaseAlbumAdapter mAlbumAdapter;
    private boolean isNext;
    private int pageNum = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ting_search);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        initView();
        mSearchPresenter = new TingSearchPresenter(this);
        mSearchPresenter.loadAllDataFromChat(getIntent());
        mSearchPresenter.loadHistory();
        mSearchPresenter.loadHotTag(false);

    }

    private void initView() {
        //设置模拟状态栏的高度
        ViewGroup.LayoutParams layoutParams = mStatusBar.getLayoutParams();
        layoutParams.height = ScreenUtil.getStatusBarHeight(this);
        mStatusBar.setLayoutParams(layoutParams);

        mCpbLoading.setVisibility(View.GONE);
        mTingSearchBox.setSearchListener(this);
        mTingSearchBox.showKeyboard();
        mFlHistory.setOnTagClickListener(this);
        mFlHotTag.setOnTagClickListener(this);

        //初始化搜索记录列表
        mUploadMore.setOnRefreshListener(this);
        mUploadMore.setVisibility(View.GONE);
        mAlbumAdapter = new XmlyAlbumAdapter(this);
        mAlbumAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onClick(View itemView, int position) {
                long albumId = mAlbumAdapter.getItemId(position);
                int albumType = mAlbumAdapter.getAlbumType(position);
                Intent intent = new Intent(TingSearchActivity.this, TingAlbumDetailActivity.class);
                intent.putExtra(TingAlbumDetailActivity.ALBUM_ID, albumId);
                intent.putExtra(TingAlbumDetailActivity.ALBUM_TYPE, albumType);
                startActivity(intent);
                goInto();
            }

            @Override
            public void onLongClick(View intemView, int position) {

            }
        });
        mRvSearchAlbum.setHasFixedSize(true);
        mRvSearchAlbum.setLayoutManager(new LinearLayoutManager(this));
        mRvSearchAlbum.setAdapter(mAlbumAdapter);
    }

    @OnClick({R.id.search_back_bt, R.id.tv_ting_search, R.id.tv_clear_history, R.id.tv_change})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.search_back_bt:
                goBack();
                break;
            case R.id.tv_ting_search:
                String text = mTingSearchBox.getText().trim();
                if (!TextUtils.isEmpty(text)) {
                    pageNum = 1;
                    mSearchPresenter.goSearch(text, pageNum);
                } else {
                    String hint = mTingSearchBox.getEditHint();
                    if (!TextUtils.isEmpty(hint)) {
                        pageNum = 1;
                        mTingSearchBox.setText(hint);
                        mSearchPresenter.goSearch(hint, pageNum);
                    }
                }
                break;
            case R.id.tv_clear_history:
                mSearchPresenter.clearHistory();
                break;
            case R.id.tv_change:
                mSearchPresenter.loadHotTag(isNext = !isNext);
                break;
        }
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
    public void setSearchHint(String hint) {
        mTingSearchBox.setEditHint(hint);
    }

    @Override
    public void switchProgressBar(boolean isShow) {
        mCpbLoading.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showSearchTagBox(int tagType, List<String> tags) {
        MyTagAdapter tagAdapter = new MyTagAdapter(tags);
        if (tagType == ITingSearch.HISTORY_TYPE) {
            if (tags != null && tags.size() > 0) {
                mRlHistoryBox.setVisibility(View.VISIBLE);
                mSearchDivider.setVisibility(View.VISIBLE);
            } else {
                mRlHistoryBox.setVisibility(View.GONE);
            }
            mFlHistory.setAdapter(tagAdapter);
        } else {
            if (tags != null && tags.size() > 0) {
                setSearchHint(tags.get(0));
                mRlHotTagBox.setVisibility(View.VISIBLE);
            } else {
                mSearchDivider.setVisibility(View.GONE);
                mRlHotTagBox.setVisibility(View.GONE);
            }
            mFlHotTag.setAdapter(tagAdapter);
        }
    }


    @Override
    public void switchSearchListBox(List<Album> datas) {
        switchProgressBar(false);
        if (pageNum == 1)
            mAlbumAdapter.clear();
        if (datas != null && datas.size() > 0) {
            mUploadMore.setVisibility(View.VISIBLE);
            mAlbumAdapter.fillDatas(datas);
        } else {
            if (pageNum == 1) {
                mAlbumAdapter.notifyDataSetChanged();
                mUploadMore.setVisibility(View.GONE);
            } else {
                Toast.makeText(this, "没有更多数据了", Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    public void onUpPullRefresh() {
        mUploadMore.setRefreshing(false);
        String text = mTingSearchBox.getText();
        if (!TextUtils.isEmpty(text))
            mSearchPresenter.goSearch(text, ++pageNum);
    }

    /**
     * editBox栏点击时调用
     **/
    @Override
    public void editClick() {

    }

    /**
     * 文本更新时调用
     **/
    @Override
    public void onSearchTextUpdate(String text) {
        if (TextUtils.isEmpty(text)) {       //清空搜索栏
            // 隐藏搜索列表
            pageNum = 1;
            switchSearchListBox(null);
            // 重新加载搜索标签
            mSearchPresenter.loadHistory();
            mSearchPresenter.loadHotTag(isNext);
        } else {
            mTingSearchBox.setSearchCompletedState();
        }
    }

    /**
     * 完成搜索,结果回调时调用
     **/
    @Override
    public void onSearchSuggestCompleted() {

    }

    /**
     * 点击手机软件盘搜索按钮时调用
     **/
    @Override
    public void onSearch(String text) {
        pageNum = 1;
        mSearchPresenter.goSearch(text, pageNum);
    }

    @Override
    public boolean onTagClick(View view, int position, FlowLayout parent) {
        String keyword = ((TextView) view).getText().toString();
        mTingSearchBox.setText(keyword);
        pageNum = 1;
        mSearchPresenter.goSearch(keyword, pageNum);
        return true;
    }

    class MyTagAdapter extends TagAdapter<String> {

        public MyTagAdapter(List<String> datas) {
            super(datas);
        }

        @Override
        public View getView(FlowLayout parent, int position, String s) {
            TextView tvTag = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.view_search_tag, parent, false);
            tvTag.setText(s);
            return tvTag;
        }
    }
}
