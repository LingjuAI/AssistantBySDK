package com.lingju.assistant.activity.index.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.LingjuSwipeUpLoadRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.TingAlbumDetailActivity;
import com.lingju.assistant.activity.TingAudioActivity;
import com.lingju.assistant.activity.event.OnItemClickListener;
import com.lingju.assistant.activity.index.model.XmlyAlbumAdapter;
import com.lingju.assistant.activity.index.model.base.BaseAlbumAdapter;
import com.lingju.assistant.service.process.TingPlayProcessor;
import com.lingju.model.TrackAlbum;
import com.lingju.model.dao.TingAlbumDao;
import com.lingju.util.XmlyManager;
import com.lsjwzh.widget.materialloadingprogressbar.CircleProgressBar;
import com.ximalaya.ting.android.opensdk.constants.DTransferConstants;
import com.ximalaya.ting.android.opensdk.datatrasfer.CommonRequest;
import com.ximalaya.ting.android.opensdk.datatrasfer.IDataCallBack;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.album.AlbumList;
import com.ximalaya.ting.android.opensdk.model.album.BatchAlbumList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ken on 2017/6/8.
 */
public class TingAlbumFragment extends Fragment implements LingjuSwipeUpLoadRefreshLayout.OnRefreshListener, OnItemClickListener {

    public static final String CATEGORY_ID = "category_id";
    public static final String FRAG_TYPE = "frag_type";
    public static final int FRAG_SUBSCRIBE = 0;
    public static final int FRAG_CATEGORY = 1;
    @BindView(R.id.rv_album)
    RecyclerView mRvAlbum;
    @BindView(R.id.upload_more)
    LingjuSwipeUpLoadRefreshLayout mUploadMore;
    @BindView(R.id.cpb_loading)
    CircleProgressBar mCpbLoading;
    private BaseAlbumAdapter mAlbumAdapter;
    private int pageNum = 1;
    private long mCateId;
    private int mType;      //页面类型（0：收藏  1：喜马拉雅分类）
    private TingAlbumDao mAlbumDao;
    private StringBuilder mAlbumIds;
    private List<Long> mKaoLaIds = new ArrayList<>();
    private boolean hasLoad = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.frag_ting_album, container, false);
        ButterKnife.bind(this, rootView);
        mUploadMore.setOnRefreshListener(this);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAlbumDao = TingAlbumDao.getInstance();
        Bundle args = getArguments();
        mCateId = args.getLong(CATEGORY_ID);
        mType = args.getInt(FRAG_TYPE, 0);
        initView(mType);
        loadData(1, mType);
        hasLoad = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        //刷新订阅列表
        if (mType == FRAG_SUBSCRIBE && !hasLoad) {
            mAlbumAdapter.clear();
            loadData(1, mType);
        }
        hasLoad = false;
    }

    private void initView(int type) {
        /*switch (type) {
            case FRAG_SUBSCRIBE:
                mAlbumAdapter = new TingSubscribeAdapter(getActivity());
                break;
            case FRAG_CATEGORY:
                mAlbumAdapter = new XmlyAlbumAdapter(getActivity());
                break;
        }*/
        mAlbumAdapter = new XmlyAlbumAdapter(getActivity());
        mRvAlbum.setLayoutManager(new LinearLayoutManager(getContext()));
        mRvAlbum.setHasFixedSize(true);
        mRvAlbum.setAdapter(mAlbumAdapter);
        mAlbumAdapter.setOnItemClickListener(this);
    }

    private void loadData(int pageNum, int type) {
        if (pageNum == 1)
            mCpbLoading.setVisibility(View.VISIBLE);
        switch (type) {
            case FRAG_CATEGORY:
                loadXmlyAlbum();
                break;
            case FRAG_SUBSCRIBE:
                loadLocalAlbum();
                break;
        }
    }

    /**
     * 加载收藏专辑记录
     **/
    private void loadLocalAlbum() {
        mUploadMore.setReturningToStart(true);
        Observable.create(new ObservableOnSubscribe<List<Album>>() {
            @Override
            public void subscribe(ObservableEmitter<List<Album>> e) throws Exception {
                List<TrackAlbum> albumList = mAlbumDao.getAllSubscribe();
                mAlbumIds = new StringBuilder();
                mKaoLaIds.clear();
                List<Album> albums = new ArrayList<>();
                if (albumList != null && albumList.size() > 0) {
                    for (TrackAlbum album : albumList) {
                        if (album.getAlbumPicUrl().contains(TingPlayProcessor.KAOLA_FM)) {
                            mKaoLaIds.add(album.getId());
                        } else {
                            mAlbumIds.append(album.getId()).append(",");
                        }
                    }
                    if(mAlbumIds.length()>0) {
                        mAlbumIds.setLength(mAlbumIds.length() - 1);
                    }
                    if (mKaoLaIds.size() > 0) {
                        albums = XmlyManager.get().getKaoLaAlbumByIds(mKaoLaIds);
                    }
                }
                e.onNext(albums);
            }
        })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Consumer<List<Album>>() {
                    @Override
                    public void accept(List<Album> albums) throws Exception {
                        if (mKaoLaIds.size() > 0 && albums.size() > 0) {
                            mCpbLoading.setVisibility(View.GONE);
                            mAlbumAdapter.fillDatas(albums);
                        }
                        getXmlyAlbums();
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    /**
     * 获取喜马拉雅专辑集合
     **/
    private void getXmlyAlbums() {
        if (mAlbumIds.length() > 0) {
            Map<String, String> params = new HashMap<>();
            params.put(DTransferConstants.ALBUM_IDS, mAlbumIds.toString());
            CommonRequest.getBatch(params, new IDataCallBack<BatchAlbumList>() {
                @Override
                public void onSuccess(BatchAlbumList batchAlbumList) {
                    List<Album> albums = batchAlbumList.getAlbums();
                    if (albums != null && albums.size() > 0) {
                        mCpbLoading.setVisibility(View.GONE);
                        mAlbumAdapter.fillDatas(albums);
                    }
                }

                @Override
                public void onError(int i, String s) {
                    Toast.makeText(getContext(), i + " " + s, Toast.LENGTH_LONG).show();
                }
            });

        } else {     //没有订阅记录
            mCpbLoading.setVisibility(View.GONE);
            mAlbumAdapter.refresh();
        }
    }

    /**
     * 加载喜马拉雅专辑数据
     **/
    private void loadXmlyAlbum() {
        Map<String, String> params = new HashMap<>();
        params.put(DTransferConstants.CATEGORY_ID, String.valueOf(mCateId));
        params.put(DTransferConstants.CALC_DIMENSION, "3");
        params.put(DTransferConstants.PAGE, String.valueOf(pageNum));
        CommonRequest.getAlbumList(params, new IDataCallBack<AlbumList>() {
            @Override
            public void onSuccess(AlbumList albumList) {
                List<Album> albums = albumList.getAlbums();
                if (albums != null && albums.size() > 0) {
                    mCpbLoading.setVisibility(View.GONE);
                    mAlbumAdapter.fillDatas(albums);
                } else {
                    Toast.makeText(TingAlbumFragment.this.getActivity(), "没有更多数据了", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(int i, String s) {
                Toast.makeText(TingAlbumFragment.this.getActivity(), i + ": " + s, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onUpPullRefresh() {
        mUploadMore.setRefreshing(false);
        loadData(++pageNum, FRAG_CATEGORY);
    }

    @Override
    public void onClick(View itemView, int position) {
        long albumId = mAlbumAdapter.getItemId(position);
        Intent intent = new Intent(getActivity(), TingAlbumDetailActivity.class);
        intent.putExtra(TingAlbumDetailActivity.ALBUM_ID, albumId);
        intent.putExtra(TingAlbumDetailActivity.ALBUM_TYPE, mAlbumAdapter.getAlbumType(position));
        startActivity(intent);
        ((TingAudioActivity) getActivity()).goInto();
    }

    @Override
    public void onLongClick(View intemView, int position) {

    }
}
