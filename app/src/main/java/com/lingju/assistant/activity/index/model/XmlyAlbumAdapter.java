package com.lingju.assistant.activity.index.model;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.lingju.assistant.activity.TingAlbumDetailActivity;
import com.lingju.assistant.activity.index.model.base.BaseAlbumAdapter;
import com.lingju.assistant.service.process.TingPlayProcessor;
import com.lingju.util.StringUtils;
import com.lingju.util.TimeUtils;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.album.LastUpTrack;

import java.util.Date;
import java.util.List;

/**
 * Created by Ken on 2017/6/8.
 */
public class XmlyAlbumAdapter extends BaseAlbumAdapter<Album> {

    public XmlyAlbumAdapter(Context context) {
        super(context);
    }

    @Override
    public void fillDatas(List<Album> datas) {
        mDatas.addAll(datas);
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(BaseAlbumAdapter.AlbumHolder holder, int position) {
        Album album = mDatas.get(position);
        Glide.with(mContext).load(album.getCoverUrlMiddle()).into(holder.mIvAlbum);
        holder.mTvAlbumTitle.setText(album.getAlbumTitle());
        LastUpTrack lastUptrack = album.getLastUptrack();
        holder.mTvLastTrack.setText(new StringBuilder().append("更新至").append(TimeUtils.formatDate(new Date(lastUptrack.getCreatedAt()))).append("  ").append(lastUptrack.getTrackTitle()).toString());
        holder.mTvPlayCount.setText(StringUtils.formPlayCount(album.getPlayCount()) + "次播放");
        holder.mTvTrackCount.setText(album.getIncludeTrackCount() + "集");
    }

    @Override
    public long getItemId(int position) {
        return mDatas.get(position).getId();
    }

    @Override
    public int getAlbumType(int position) {
        return mDatas.get(position).getCoverUrlMiddle().contains(TingPlayProcessor.KAOLA_FM) ? TingAlbumDetailActivity.KAOLA : TingAlbumDetailActivity.XIMALAYA;
    }
}
