package com.lingju.assistant.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.TingAlbumDetailActivity;
import com.lingju.assistant.service.process.TingPlayProcessor;
import com.lingju.model.TrackAlbum;
import com.lingju.model.dao.TingAlbumDao;
import com.lingju.util.XmlyManager;
import com.ximalaya.ting.android.opensdk.model.PlayableModel;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Ken on 2017/6/9.
 */
public class TingPlayerComponent extends LinearLayout {

    @BindView(R.id.tiv_track)
    TingPlayerImageView mTivTrack;
    @BindView(R.id.iv_ting_switch)
    ImageView mIvTingSwitch;
    @BindView(R.id.tv_track_title)
    TextView mTvTrackTitle;
    private Context mContext;
    private long mTrackId;
    private TrackAlbum mLastTrack;

    public TingPlayerComponent(Context context) {
        super(context);
        initView(context);
    }

    public TingPlayerComponent(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }


    private void initView(Context context) {
        this.mContext = context;
        View contentView = LayoutInflater.from(mContext).inflate(R.layout.ting_player, this, true);
        ButterKnife.bind(this, contentView);
        mIvTingSwitch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int level = mIvTingSwitch.getDrawable().getLevel();
                if (level == 0) {
                    XmlyManager.get().setPlaying(true);
                    XmPlayerManager.getInstance(mContext).play();
                    if (mStateListener != null)
                        mStateListener.onTrackPlay(mTrackId);
                } else {
                    XmlyManager.get().setPlaying(false);
                    XmPlayerManager.getInstance(mContext).pause();
                    if (mStateListener != null)
                        mStateListener.onTrackPause();
                }
            }
        });
        // initPlayerBox();
    }

    public void initPlayerBox() {
        boolean isPlaying = XmPlayerManager.getInstance(mContext).isPlaying();
        setPlayState(isPlaying);
        if (isPlaying)
            startAnim();
        else
            stopAnim();
        PlayableModel currSound = XmlyManager.get().getPlayer().getCurrSound(false);
        if (currSound == null) {    //播放器没有播放记录，则查找本地记录
            mLastTrack = TingAlbumDao.getInstance().findLastTrack();
            if (mLastTrack != null) {
                //填充播放记录到播放器
                List<Track> list = new ArrayList<>();
                Track track = XmlyManager.get().transformTrack(mLastTrack);
                list.add(track);
                XmlyManager.get().setWillPlay(false);
                Log.i("LingJu", "TingPlayerComponent findLastTrack()");
                XmPlayerManager.getInstance(mContext).setPlayList(list, 0);
            }
        } else {
            mLastTrack = new TrackAlbum();
            // TODO: 2017/6/13 暂时只针对Track类型进行转换
            Track track = (Track) currSound;
            mLastTrack.setId(track.getAlbum().getAlbumId());
            mLastTrack.setTrackPicUrl(track.getCoverUrlMiddle());
            mLastTrack.setTrackTitle(track.getTrackTitle());
            mLastTrack.setTrackId(track.getDataId());
        }
        if (mLastTrack == null) {
            setImage(null);
        } else {
            setImage(mLastTrack.getTrackPicUrl());
            setTrackTitle(mLastTrack.getTrackTitle());
            mTrackId = mLastTrack.getTrackId();
        }
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLastTrack == null)
                    Toast.makeText(mContext, "暂无播放记录", Toast.LENGTH_LONG).show();
                else {
                    Intent intent = new Intent(mContext, TingAlbumDetailActivity.class);
                    intent.putExtra(TingAlbumDetailActivity.ALBUM_ID, mLastTrack.getId());
                    int type = mLastTrack.getTrackPicUrl().contains(TingPlayProcessor.KAOLA_FM) ? TingAlbumDetailActivity.KAOLA : TingAlbumDetailActivity.XIMALAYA;
                    intent.putExtra(TingAlbumDetailActivity.ALBUM_TYPE, type);
                    mContext.startActivity(intent);
                    ((Activity) mContext).overridePendingTransition(R.anim.activity_start_in, R.anim.activity_start_out);
                }
            }
        });
    }

    public void setLastTrack(Track track) {
        this.mTrackId = track.getDataId();
        mLastTrack = new TrackAlbum();
        mLastTrack.setTrackId(track.getDataId());
        mLastTrack.setId(track.getAlbum().getAlbumId());
        mLastTrack.setTrackTitle(track.getTrackTitle());
        mLastTrack.setTrackPicUrl(track.getCoverUrlMiddle());
    }


    public void setImage(String imageUrl) {
        mTivTrack.setImage(mContext, imageUrl);
    }

    public void setTrackTitle(String title) {
        mTvTrackTitle.setText(title);
    }

    public void setPlayState(boolean isPlay) {
        mIvTingSwitch.setImageLevel(isPlay ? 1 : 0);
    }

    public void startAnim() {
        mTivTrack.startAnim();
    }

    public void stopAnim() {
        mTivTrack.stopAnim();
    }

    public void Play(Track track) {
        mTrackId = track.getDataId();
        setPlayState(true);
        setImage(track.getCoverUrlMiddle());
        setTrackTitle(track.getTrackTitle());
        startAnim();
    }

    public void pause() {
        setPlayState(false);
        stopAnim();
    }

    private OnPlayStateListener mStateListener;

    public void setOnPlayStateListener(OnPlayStateListener listener) {
        this.mStateListener = listener;
    }

    public interface OnPlayStateListener {
        void onTrackPause();

        void onTrackPlay(long id);
    }
}
