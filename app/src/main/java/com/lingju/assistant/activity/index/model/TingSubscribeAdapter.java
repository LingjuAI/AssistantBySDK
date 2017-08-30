package com.lingju.assistant.activity.index.model;

import android.content.Context;

import com.lingju.assistant.activity.index.model.base.BaseAlbumAdapter;
import com.lingju.model.TrackAlbum;

import java.util.List;

/**
 * Created by Ken on 2017/6/8.
 */
public class TingSubscribeAdapter extends BaseAlbumAdapter<TrackAlbum> {

    public TingSubscribeAdapter(Context context) {
        super(context);
    }

    @Override
    public void fillDatas(List<TrackAlbum> datas) {
        mDatas.addAll(datas);
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(BaseAlbumAdapter.AlbumHolder holder, int position) {

    }
}
