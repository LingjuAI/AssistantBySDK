package com.lingju.assistant.activity.index.model;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.lingju.assistant.R;
import com.lingju.assistant.activity.TingSearchActivity;
import com.lingju.assistant.activity.event.ChatMsgEvent;
import com.lingju.assistant.activity.event.SynthesizeEvent;
import com.lingju.assistant.entity.TingAlbumMsg;
import com.lingju.assistant.service.AssistantService;
import com.lingju.assistant.service.process.TingPlayProcessor;
import com.lingju.audio.engine.IflySynthesizer;
import com.lingju.audio.engine.base.SpeechMsg;
import com.lingju.common.adapter.ChatRobotBuilder;
import com.lingju.model.temp.speech.ResponseMsg;
import com.lingju.util.ScreenUtil;
import com.lingju.util.StringUtils;
import com.lingju.util.TimeUtils;
import com.lingju.util.XmlyManager;
import com.ximalaya.ting.android.opensdk.constants.DTransferConstants;
import com.ximalaya.ting.android.opensdk.datatrasfer.CommonRequest;
import com.ximalaya.ting.android.opensdk.datatrasfer.IDataCallBack;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.album.LastUpTrack;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.model.track.TrackList;
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Ken on 2017/6/22.
 */
public class ChatAlbumListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final static int HEADER_VIEW = 0;
    private final static int CONTENT_VIEW = 1;
    private final static int FOOTER_VIEW = 2;
    private Context mContext;
    private List<Album> mdatas;
    private int mEpisode;

    public ChatAlbumListAdapter(Context context, List<Album> datas, int episode) {
        this.mContext = context;
        this.mdatas = datas;
        this.mEpisode = episode;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder = null;
        View itemView;
        switch (viewType) {
            case HEADER_VIEW:
                itemView = LayoutInflater.from(mContext).inflate(R.layout.item_list_header, parent, false);
                holder = new HeaderHolder(itemView);
                break;
            case CONTENT_VIEW:
                itemView = LayoutInflater.from(mContext).inflate(R.layout.item_ting_album, parent, false);
                holder = new AlbumListHolder(itemView);
                break;
            case FOOTER_VIEW:
                itemView = LayoutInflater.from(mContext).inflate(R.layout.item_album_footer, parent, false);
                holder = new FooterHolder(itemView);
                break;
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).mTvListHeader.setText("有声内容");
        } else if (holder instanceof AlbumListHolder) {
            AlbumListHolder albumHolder = (AlbumListHolder) holder;
            Album album = mdatas.get(position - 1);
            ViewGroup.LayoutParams layoutParams = albumHolder.mAlbumBorder.getLayoutParams();
            layoutParams.width = layoutParams.width- ScreenUtil.getInstance().dip2px(32);
            albumHolder.mAlbumBorder.setLayoutParams(layoutParams);
            Glide.with(mContext).load(album.getCoverUrlMiddle()).into(albumHolder.mIvAlbum);
            albumHolder.mTvAlbumTitle.setText(album.getAlbumTitle());
            LastUpTrack lastUptrack = album.getLastUptrack();
            albumHolder.mTvLastTrack.setText(new StringBuilder().append("更新至").append(TimeUtils.formatDate(new Date(lastUptrack.getCreatedAt()))).append("  ").append(lastUptrack.getTrackTitle()).toString());
            albumHolder.mTvPlayCount.setText(StringUtils.formPlayCount(album.getPlayCount()) + "次播放");
            albumHolder.mTvTrackCount.setText(album.getIncludeTrackCount() + "集");
        } else {
            // TODO: 2017/6/26 设置倒计时文本，如不设置则每一次刷新都将重置为布局文件中的默认文本
        }
    }

    @Override
    public int getItemCount() {
        int count = 0;
        if (mdatas != null) {
            count = mdatas.size() > 3 ? 3 : mdatas.size();
            count = count + 1 + 1;
        }
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return HEADER_VIEW;
        else if (position == getItemCount() - 1)
            return FOOTER_VIEW;
        else
            return CONTENT_VIEW;
    }

    class HeaderHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.tv_list_header)
        TextView mTvListHeader;

        public HeaderHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    class AlbumListHolder extends RecyclerView.ViewHolder {

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
        @BindView(R.id.album_border)
        View mAlbumBorder;

        public AlbumListHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    send2EndChoose();
                    if (mListener != null)
                        mListener.onCancel();
                    Album album = mdatas.get(getAdapterPosition() - 1);
                    if (album.getCoverUrlMiddle().contains(TingPlayProcessor.KAOLA_FM)) {
                        List<Track> tracks = XmlyManager.get().getKaoLaTrackByAlbumId(album.getId(), 1, 1);
                        if (tracks == null) {
                            synthesizeAndShowResp(null, "播放异常，请重试", 0);
                        } else if (tracks.size() == 0) {
                            synthesizeAndShowResp(null, "没有更多集数了", 0);
                        } else {
                            synthesizeAndShowResp(tracks, "开始播放" + album.getAlbumTitle(), 0);
                            //移除上一个播放专辑
                            EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_TING_TRACK_STATE));
                            //显示新的播放专辑
                            EventBus.getDefault().post(new ChatMsgEvent(new TingAlbumMsg(album, tracks.get(0))));
                        }
                    } else {
                        getTrackByAlbumId(album);
                    }
                }
            });
        }
    }

    class FooterHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.tv_count_down)
        TextView mTvCountDown;

        public FooterHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @OnClick(R.id.tv_show_more)
        public void onClick() {
            if (mListener != null)
                mListener.onCancel();
            send2EndChoose();
            Intent intent = new Intent(mContext, TingSearchActivity.class);
            intent.putParcelableArrayListExtra(TingSearchActivity.ALBUM_LIST, (ArrayList<? extends Parcelable>) mdatas);
            mContext.startActivity(intent);
            ((Activity) mContext).overridePendingTransition(R.anim.activity_start_in, R.anim.activity_start_out);
        }
    }

    /**
     * 根据专辑ID查询声音并播放
     **/
    private void getTrackByAlbumId(final Album album) {
        //计算播放集数在专辑中的页数和播放索引
        int pageNum;
        int playIndex = 0;
        int episode = mEpisode == -1 ? (int) album.getIncludeTrackCount() : mEpisode;
        if (episode == 0)
            pageNum = 1;
        else {
            pageNum = episode / XmlyManager.BASE_COUNT;
            playIndex = episode % XmlyManager.BASE_COUNT;
            pageNum = playIndex == 0 ? pageNum : pageNum + 1;
            playIndex = playIndex == 0 ? XmlyManager.BASE_COUNT - 1 : playIndex - 1;
        }
        Log.i("LingJu", "TingPlayProcessor>>>页数：" + pageNum + " 索引：" + playIndex);
        Map<String, String> params = new HashMap<>();
        params.put(DTransferConstants.ALBUM_ID, String.valueOf(album.getId()));
        params.put(DTransferConstants.PAGE, String.valueOf(pageNum));
        final int finalPlayIndex = playIndex;
        final int finalEpisode = episode;
        CommonRequest.getTracks(params, new IDataCallBack<TrackList>() {
            @Override
            public void onSuccess(TrackList trackList) {
                final List<Track> tracks = trackList.getTracks();
                if (tracks != null && tracks.size() > 0) {
                    String content = "开始播放" + trackList.getAlbumTitle();
                    if (finalEpisode == 0) {
                        content += "第1集";
                    } else if (finalEpisode == trackList.getTotalCount()) {
                        content += "最新的第" + finalEpisode + "集";
                    } else {
                        content += "第" + finalEpisode + "集";
                    }
                    synthesizeAndShowResp(tracks, content, finalPlayIndex);
                    //移除上一个播放专辑
                    EventBus.getDefault().post(new ChatMsgEvent(ChatMsgEvent.REMOVE_TING_TRACK_STATE));
                    //显示新的播放专辑
                    EventBus.getDefault().post(new ChatMsgEvent(new TingAlbumMsg(album, tracks.get(finalPlayIndex))));
                } else {
                    synthesizeAndShowResp(null, "没有更多集数了", 0);
                }
            }

            @Override
            public void onError(int i, String s) {
                synthesizeAndShowResp(null, "播放异常，请重试", 0);
            }
        });
    }

    /**
     * 合成并显示回复文本
     **/
    private void synthesizeAndShowResp(final List<Track> tracks, String content, final int finalPlayIndex) {
        EventBus.getDefault().post(new ChatMsgEvent(new ResponseMsg(content), null, null, null));
        IflySynthesizer.get().startSpeakAbsolute(content)
                .doOnNext(new Consumer<SpeechMsg>() {
                    @Override
                    public void accept(SpeechMsg speechMsg) throws Exception {
                        if (speechMsg.state() == SpeechMsg.State.OnBegin)
                            EventBus.getDefault().post(new SynthesizeEvent(SynthesizeEvent.SYNTH_START));
                    }
                })
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        EventBus.getDefault().post(new SynthesizeEvent(SynthesizeEvent.SYNTH_END));
                        if (tracks != null)
                            XmPlayerManager.getInstance(mContext).playList(tracks, finalPlayIndex);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe();

    }

    /**
     * 推送NOANSWER给Robot，让其退出选择专辑环节
     **/
    private void send2EndChoose() {
        //停止合成、识别
        Intent rIntent = new Intent(mContext, AssistantService.class);
        rIntent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.SEND_TO_ROBOT_FOR_END_TASK);
        rIntent.putExtra(AssistantService.CALLBACK, false);
        mContext.startService(rIntent);
        //推送NOANSWER
        Intent pushIntent = new Intent(mContext, AssistantService.class);
        pushIntent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.PUSH_ROUTE_CACULATE);
        pushIntent.putExtra(AssistantService.TEXT, ChatRobotBuilder.NOANSWER);
        mContext.startService(pushIntent);
        //停止自动播放任务
        Intent stopIntent = new Intent(mContext, AssistantService.class);
        stopIntent.putExtra(AssistantService.CMD, AssistantService.ServiceCmd.CANCEL_TING_PLAY_TASK);
        mContext.startService(stopIntent);
    }

    private OnCountDownCancelListener mListener;

    public void setOnCountDownCancelListener(OnCountDownCancelListener listener) {
        this.mListener = listener;
    }

    /**
     * 倒计时取消回调器
     **/
    interface OnCountDownCancelListener {
        void onCancel();
    }
}
