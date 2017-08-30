package com.lingju.assistant.activity.index.model.base;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.event.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Ken on 2017/6/8.
 */
public abstract class BaseAlbumAdapter<T> extends RecyclerView.Adapter<BaseAlbumAdapter.AlbumHolder> {

    protected Context mContext;
    protected List<T> mDatas = new ArrayList<>();
    private OnItemClickListener mClickListener;


    public BaseAlbumAdapter(Context context) {
        this.mContext = context;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mClickListener = listener;
    }

    @Override
    public AlbumHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_ting_album, parent, false);
        return new AlbumHolder(itemView);
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    public void clear() {
        mDatas.clear();
    }

    public void refresh() {
        notifyDataSetChanged();
    }

    /**
     * 填充数据，并刷新列表视图（子类必须实现）
     **/
    public abstract void fillDatas(List<T> datas);

    public int getAlbumType(int position){return 0;}

    public class AlbumHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.iv_album)
        public ImageView mIvAlbum;
        @BindView(R.id.tv_album_title)
        public TextView mTvAlbumTitle;
        @BindView(R.id.tv_last_track)
        public TextView mTvLastTrack;
        @BindView(R.id.tv_play_count)
        public TextView mTvPlayCount;
        @BindView(R.id.tv_track_count)
        public TextView mTvTrackCount;

        public AlbumHolder(final View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mClickListener != null)
                        mClickListener.onClick(itemView, getAdapterPosition());
                }
            });
        }
    }
}
