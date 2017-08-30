package com.lingju.assistant.activity.index.model;

import android.content.Context;
import android.graphics.drawable.LevelListDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.TingAlbumDetailActivity;
import com.lingju.assistant.activity.event.OnItemClickListener;
import com.lingju.assistant.view.ChooseSetPopupWindow;
import com.lingju.model.SimpleDate;
import com.lingju.model.TrackAlbum;
import com.lingju.model.dao.TingAlbumDao;
import com.lingju.util.StringUtils;
import com.lingju.util.TimeUtils;
import com.lingju.util.XmlyManager;
import com.ximalaya.ting.android.opensdk.constants.DTransferConstants;
import com.ximalaya.ting.android.opensdk.datatrasfer.CommonRequest;
import com.ximalaya.ting.android.opensdk.datatrasfer.IDataCallBack;
import com.ximalaya.ting.android.opensdk.model.PlayableModel;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.album.BatchAlbumList;
import com.ximalaya.ting.android.opensdk.model.album.LastUpTrack;
import com.ximalaya.ting.android.opensdk.model.album.SubordinatedAlbum;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.model.track.TrackList;
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ken on 2017/6/12.
 */
public class TingAlbumDetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int HEADER_TYPE = 0;
    private static final int TRACKLIST_TYPE = 1;
    private long mAlbumId;
    private Context mContext;
    private List<Track> mTracks = new ArrayList<>();    //集合第一位用于表示列表头部，不做数据存储
    private TingAlbumDao mAlbumDao;
    private boolean isDesc;     //排序规则（false:正序，默认  true:倒序）
    private int pageNum = 1;    //数据页码，默认第一页
    private int mTotalPage;     //专辑声音数据中页数
    private int mTotalCount;
    private Album mAlbum;
    private long playTrackId;
    private XmPlayerManager mXmPlayer;
    private TrackAlbum mHistory;
    private int albumType;      //0：喜马拉雅  1：考拉

    public TingAlbumDetailAdapter(Context context, long AlbumId, int type) {
        this.mContext = context;
        this.mAlbumId = AlbumId;
        this.albumType = type;
        mAlbumDao = TingAlbumDao.getInstance();
        mXmPlayer = XmlyManager.get().getPlayer();
        playTrackId = mXmPlayer.getCurrSound() == null ? 0 : mXmPlayer.getCurrSound().getDataId();
    }

    /**
     * 加载声音数据
     **/
    public void fillData(boolean upLoadMore) {
        if (upLoadMore)
            pageNum++;
        if (albumType == TingAlbumDetailActivity.KAOLA) {
            Observable.create(new ObservableOnSubscribe<List<Track>>() {
                @Override
                public void subscribe(ObservableEmitter<List<Track>> e) throws Exception {
                    List<Track> trackList = XmlyManager.get().getKaoLaTrackByAlbumId(mAlbumId, pageNum, isDesc ? 0 : 1);
                    e.onNext(trackList);
                }
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError(new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            Toast.makeText(mContext, "网络异常", Toast.LENGTH_LONG).show();
                            notifyDataSetChanged();
                        }
                    })
                    .doOnNext(new Consumer<List<Track>>() {
                        @Override
                        public void accept(List<Track> tracks) throws Exception {
                            if (tracks.size() == 0) {
                                Toast.makeText(mContext, "没有更多数据了", Toast.LENGTH_LONG).show();
                                notifyDataSetChanged();
                            } else {
                                ((TingAlbumDetailActivity) mContext).hiddenLoading();
                                if (mTotalPage == 0) {
                                    mTotalPage = XmlyManager.get().getTotalPage();
                                    mTotalCount = XmlyManager.get().getTotalCount();
                                }
                                if (mTracks.size() > 0)     //添加头部视图占位元素
                                    mTracks.set(0, new Track());
                                else
                                    mTracks.add(new Track());
                                mTracks.addAll(tracks);
                                notifyDataSetChanged();
                            }
                        }
                    })
                    .subscribe();
        } else {
            Map<String, String> params = new HashMap<>();
            params.put(DTransferConstants.ALBUM_ID, String.valueOf(mAlbumId));
            params.put(DTransferConstants.SORT, isDesc ? "desc" : "asc");
            params.put(DTransferConstants.PAGE, String.valueOf(pageNum));
            CommonRequest.getTracks(params, new IDataCallBack<TrackList>() {
                @Override
                public void onSuccess(TrackList trackList) {
                    ((TingAlbumDetailActivity) mContext).hiddenLoading();
                    if (mTotalPage == 0) {
                        mTotalPage = trackList.getTotalPage();
                        mTotalCount = trackList.getTotalCount();
                    }
                    List<Track> tracks = trackList.getTracks();
                    if (tracks != null && tracks.size() > 0) {
                        if (mTracks.size() > 0)     //添加图标视图占位元素
                            mTracks.set(0, new Track());
                        else
                            mTracks.add(new Track());
                        mTracks.addAll(tracks);
                        notifyDataSetChanged();
                    } else {
                        Toast.makeText(mContext, "没有更多数据了", Toast.LENGTH_LONG).show();
                        notifyDataSetChanged();
                    }
                }

                @Override
                public void onError(int i, String s) {
                    Toast.makeText(mContext, i + " " + s, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public void setAlbumId(long id) {
        this.mAlbumId = id;
    }

    public void setPlayTrackId(long trackId) {
        this.playTrackId = trackId;
    }

    public void setAlbumType(int albumType) {
        this.albumType = albumType;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder;
        View itemView;
        if (viewType == HEADER_TYPE) {
            itemView = LayoutInflater.from(mContext).inflate(R.layout.item_album_header, parent, false);
            holder = new HeaderHolder(itemView);
        } else {
            itemView = LayoutInflater.from(mContext).inflate(R.layout.item_ting_track, parent, false);
            holder = new TrackDetailHolder(itemView);
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position == 0) {
            HeaderHolder headerHolder = (HeaderHolder) holder;
            //专辑详情
            Glide.with(mContext).load(mAlbum.getCoverUrlMiddle()).into(headerHolder.mIvAlbum);
            headerHolder.mTvAlbumTitle.setText(mAlbum.getAlbumTitle());
            LastUpTrack lastUptrack = mAlbum.getLastUptrack();
            StringBuilder sb = new StringBuilder();
            headerHolder.mTvLastTrack.setText(sb.append("更新至").append(TimeUtils.formatDate(new Date(lastUptrack.getCreatedAt()))).append("  ").append(lastUptrack.getTrackTitle()).toString());
            sb.setLength(0);
            headerHolder.mTvPlayCount.setText(sb.append(StringUtils.formPlayCount(mAlbum.getPlayCount())).append("次播放").toString());
            sb.setLength(0);
            headerHolder.mTvTrackCount.setText(sb.append(mAlbum.getIncludeTrackCount()).append("集").toString());
            //订阅状态
            boolean isSubscribe = mAlbumDao.isSubscribe(mAlbumId);
            headerHolder.mTvSubscribe.setText(isSubscribe ? "已订阅" : "订阅");
            LevelListDrawable ld = (LevelListDrawable) headerHolder.mTvSubscribe.getBackground();
            ld.setLevel(isSubscribe ? 1 : 0);

            if (mHistory != null) {
                headerHolder.mRlHistoryBox.setVisibility(View.VISIBLE);
                sb.setLength(0);
                headerHolder.mTvHistoryTitle.setText(sb.append("继续播放：").append(mHistory.getTrackTitle()).toString());
                sb.setLength(0);
                NumberFormat nf = NumberFormat.getPercentInstance();
                //返回数的整数部分所允许的最大位数
                nf.setMaximumIntegerDigits(3);
                //返回数的小数部分所允许的最大位数
                nf.setMaximumFractionDigits(0);
                headerHolder.mTvProgress.setText(sb.append("已播  ").append(nf.format(mHistory.getBreakPos() / (double) mHistory.getDuration())).toString());
                headerHolder.mIvTingSwitch.setImageLevel((XmlyManager.get().isPlaying() && playTrackId == mHistory.getTrackId()) ? 1 : 0);
            } else {
                headerHolder.mRlHistoryBox.setVisibility(View.GONE);
            }

        } else {
            Track track = mTracks.get(position);
            TrackDetailHolder detailHolder = (TrackDetailHolder) holder;
            detailHolder.mTvTrackTitle.setText(track.getTrackTitle());
            detailHolder.mTvTrackTitle.setTextColor(track.getDataId() == playTrackId
                    ? mContext.getResources().getColor(R.color.second_base_color)
                    : mContext.getResources().getColor(R.color.new_text_color_first));
            detailHolder.mIvTingSwitch.setImageLevel(0);
            if (track.getDataId() == playTrackId)
                detailHolder.mIvTingSwitch.setImageLevel(XmlyManager.get().isPlaying() ? 1 : 0);
            detailHolder.mTvCreated.setText(TimeUtils.getInstance().getDateString(new Date(track.getCreatedAt())));
            detailHolder.mTvDuration.setText(new SimpleDate().formDuration(track.getDuration()));
        }
    }

    @Override
    public int getItemCount() {
        return mTracks.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? HEADER_TYPE : TRACKLIST_TYPE;
    }

    /**
     * 获取专辑信息
     **/
    public void getAlbumInfo() {
        resetList();
        mTotalPage = 0;
        //播放记录
        mHistory = mAlbumDao.getHistoryById(mAlbumId);
        if (albumType == TingAlbumDetailActivity.KAOLA) {
            Observable.create(new ObservableOnSubscribe<Album>() {
                @Override
                public void subscribe(ObservableEmitter<Album> e) throws Exception {
                    Album playAlbum = XmlyManager.get().getKaoLaAlbumById(mAlbumId);
                    e.onNext(playAlbum);
                }
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError(new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            Toast.makeText(mContext, "网络异常，请重试", Toast.LENGTH_LONG).show();
                            notifyDataSetChanged();
                        }
                    })
                    .doOnNext(new Consumer<Album>() {
                        @Override
                        public void accept(Album album) throws Exception {
                            mAlbum = album;
                            fillData(false);
                        }
                    })
                    .subscribe();
        } else {
            Map<String, String> params = new HashMap<>();
            params.put(DTransferConstants.ALBUM_IDS, String.valueOf(mAlbumId));
            CommonRequest.getBatch(params, new IDataCallBack<BatchAlbumList>() {
                @Override
                public void onSuccess(BatchAlbumList batchAlbumList) {
                    List<Album> albums = batchAlbumList.getAlbums();
                    if (albums != null && albums.size() > 0) {
                        mAlbum = albums.get(0);
                        fillData(false);
                    }
                }

                @Override
                public void onError(int i, String s) {
                    Toast.makeText(mContext, i + " " + s, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    class HeaderHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.iv_album)
        ImageView mIvAlbum;
        @BindView(R.id.tv_album_title)
        TextView mTvAlbumTitle;
        @BindView(R.id.tv_last_track)
        TextView mTvLastTrack;
        @BindView(R.id.tv_play_count)
        TextView mTvPlayCount;
        @BindView(R.id.tv_track_count)
        TextView mTvTrackCount;
        @BindView(R.id.tv_subscribe)
        TextView mTvSubscribe;
        @BindView(R.id.iv_ting_switch)
        ImageView mIvTingSwitch;
        @BindView(R.id.tv_history_title)
        TextView mTvHistoryTitle;
        @BindView(R.id.tv_progress)
        TextView mTvProgress;
        @BindView(R.id.rl_history_box)
        RelativeLayout mRlHistoryBox;
        @BindView(R.id.ll_choose_box)
        LinearLayout mLlChooseBox;
        private int mAlbumType;

        public HeaderHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @OnClick({R.id.tv_subscribe, R.id.ll_choose_box, R.id.ll_sort_box, R.id.rl_history_box})
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.tv_subscribe:
                    LevelListDrawable ld = (LevelListDrawable) mTvSubscribe.getBackground();
                    if (ld.getLevel() == 0) {      //订阅
                        ld.setLevel(1);
                        mTvSubscribe.setText("已订阅");
                        mAlbumDao.insertSubscribe(mAlbum);
                    } else {
                        ld.setLevel(0);
                        mTvSubscribe.setText("订阅");
                        mAlbumDao.delSubscribeById(mAlbumId);
                    }
                    break;
                case R.id.ll_choose_box:
                    ChooseSetPopupWindow setPopupWindow = new ChooseSetPopupWindow(mContext, pageNum, mTotalPage, mTotalCount);
                    setPopupWindow.setOnItemClickListener(new OnItemClickListener() {
                        @Override
                        public void onClick(View itemView, int position) {
                            pageNum = position + 1;
                            isDesc = false;
                            mTracks.clear();
                            fillData(false);
                        }

                        @Override
                        public void onLongClick(View intemView, int position) {

                        }
                    });
                    setPopupWindow.show(mLlChooseBox);
                    break;
                case R.id.ll_sort_box:
                    isDesc = !isDesc;
                    resetList();
                    fillData(false);
                    break;
                case R.id.rl_history_box:
                    // TODO: 2017/6/14 继续播放
                    if (mIvTingSwitch.getDrawable().getLevel() == 1) {
                        XmlyManager.get().setPlaying(false);
                        mXmPlayer.pause();
                    } else {
                        List<Track> list = new ArrayList<>();
                        Track track = new Track();
                        track.setKind(PlayableModel.KIND_TRACK);
                        track.setDownloadedSaveFilePath(mHistory.getTrackUrl());
                        track.setTrackTitle(mHistory.getTrackTitle());
                        track.setDataId(mHistory.getTrackId());
                        track.setCoverUrlMiddle(mHistory.getTrackPicUrl());
                        track.setDuration(mHistory.getDuration());
                        SubordinatedAlbum album = new SubordinatedAlbum();
                        album.setAlbumId(mHistory.getId());
                        track.setAlbum(album);
                        list.add(track);
                        playTrackId = mHistory.getTrackId();
                        //将当前播放声音传给播放栏组件
                        ((TingAlbumDetailActivity) mContext).setLastTrack(track);
                        XmlyManager.get().setPlaying(true);
                        mXmPlayer.playList(list, 0);
                    }
                    notifyDataSetChanged();
                    break;
            }
        }
    }

    private void resetList() {
        mTracks.clear();
        pageNum = 1;
    }

    class TrackDetailHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.iv_ting_switch)
        ImageView mIvTingSwitch;
        @BindView(R.id.tv_track_title)
        TextView mTvTrackTitle;
        @BindView(R.id.tv_duration)
        TextView mTvDuration;
        @BindView(R.id.tv_created)
        TextView mTvCreated;

        public TrackDetailHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: 2017/6/12 togglePlay
                    int level = mIvTingSwitch.getDrawable().getLevel();
                    if (level == 0) {
                        playTrackId = mTracks.get(getAdapterPosition()).getDataId();
                        //将当前播放声音传给播放栏组件
                        ((TingAlbumDetailActivity) mContext).setLastTrack(mTracks.get(getAdapterPosition()));
                        XmlyManager.get().setPlaying(true);
                        mXmPlayer.playList(mTracks, getAdapterPosition());
                    } else {
                        XmlyManager.get().setPlaying(false);
                        mXmPlayer.pause();
                    }
                    notifyDataSetChanged();
                }
            });
        }
    }
}
