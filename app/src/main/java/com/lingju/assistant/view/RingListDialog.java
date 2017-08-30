package com.lingju.assistant.view;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.event.OnItemClickListener;
import com.lsjwzh.widget.materialloadingprogressbar.CircleProgressBar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Ken on 2017/2/18.
 */
public class RingListDialog extends Dialog {

    @BindView(R.id.arl_list)
    RecyclerView mArlList;
    @BindView(R.id.ll_loading)
    CircleProgressBar mLlLoading;
    private RingAdapter mAdapter;
    private LoadRingTask mRingTask;
    private List<Music> mRings;
    private int mCurrentPosition = -1;
    private String selectUrl;
    private String selectTitle;
    private Context mContext;

    public RingListDialog(Context context, String ring, String path) {
        super(context, R.style.lingju_commond_dialog);
        this.mContext = context;
        this.selectTitle = ring;
        this.selectUrl = path;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ring_list);
        ButterKnife.bind(this);
        mLlLoading.setColorSchemeResources(R.color.red_style, R.color.second_base_color, R.color.base_blue, R.color.colorPrimary);
        mRings = new ArrayList<>();
        mAdapter = new RingAdapter();
        mArlList.setAdapter(mAdapter);
        mArlList.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });
        //如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
        mArlList.setHasFixedSize(true);
        mArlList.setLayoutManager(new LinearLayoutManager(mContext));
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onClick(View itemView, int position) {
                mCurrentPosition = position;
                Music music = mRings.get(position);
                selectTitle = music.getTitle();
                selectUrl = music.getUrl();
                mAdapter.play(selectUrl);
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLongClick(View intemView, int position) {

            }
        });
        mRingTask = new LoadRingTask();
        mRingTask.execute();
    }

    @Override
    public void dismiss() {
        if (mRingTask != null) {
            mRingTask.cancel(true);
            mRingTask = null;
        }
        mAdapter.cancelPlay();
        super.dismiss();
    }


    @OnClick({R.id.ring_cancel, R.id.ring_confirm})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ring_confirm:
                if (mSelectedListener != null) {
                    mSelectedListener.onSelected(selectTitle, selectUrl);
                }
                break;
        }
        dismiss();
    }

    /**
     * 异步加载铃声数据任务
     **/
    class LoadRingTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLlLoading.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            ContentResolver resolver = mContext.getContentResolver();
            if (resolver != null) {
                /*MediaStore.Audio.Media.EXTERNAL_CONTENT_URI*/
                Cursor cursor = resolver.query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, new String[]{
                                MediaStore.Audio.Media._ID,
                                MediaStore.Audio.Media.TITLE,
                                MediaStore.Audio.Media.DATA,
                                MediaStore.Audio.Media.DURATION,
                                MediaStore.Audio.Media.SIZE
                        },
                        MediaStore.Audio.Media.DURATION + ">?",
                        new String[]{"10000"}, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
                if (null != cursor) {
                    Music m;
                    try {
                        while (cursor.moveToNext()) {
                            m = new Music();
                            m.setId(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
                            m.setTitle(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                            m.setUrl(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
                            m.setDuration(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)));
                            m.setSize(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)));
                            mRings.add(m);
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mLlLoading.setVisibility(View.GONE);
            mAdapter.notifyDataSetChanged();
        }
    }

    public class RingAdapter extends RecyclerView.Adapter<RingAdapter.RingHolder> {
        private OnItemClickListener mClickListener;
        private MediaPlayer player;

        @Override
        public RingHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ViewGroup itemView = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.speaker_line, parent, false);
            return new RingHolder(itemView);
        }

        @Override
        public void onBindViewHolder(RingHolder holder, int position) {
            Music music = mRings.get(position);
            holder.tvRingName.setText(music.getTitle());
            if (position == mCurrentPosition || music.getUrl().equals(selectUrl)) {
                holder.btnFavor.setChecked(true);
            } else {
                holder.btnFavor.setChecked(false);
            }
            final int pos = position;
            if (mClickListener != null) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mClickListener.onClick(v, pos);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return mRings.size();
        }

        public void play(String path) {
            try {
                if (player == null)
                    player = new MediaPlayer();
                player.reset();
                /* 设置播放资源路径 */
                player.setDataSource(path);
                /* 准备 */
                player.prepare();
                /* 播放 */
                player.start();
                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        player.reset();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancelPlay() {
            if (player != null && player.isPlaying()) {
                player.stop();
                player.release();
                player = null;
            }
        }

        class RingHolder extends RecyclerView.ViewHolder {
            public AppCompatRadioButton btnFavor;
            public TextView tvRingName;

            public RingHolder(View itemView) {
                super(itemView);
                tvRingName = (TextView) itemView.findViewById(R.id.list_speaker_text);
                btnFavor = (AppCompatRadioButton) itemView.findViewById(R.id.list_favorite_bt);
            }
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            mClickListener = listener;
        }
    }

    public RingListDialog setOnRingSelectedListener(OnRingSelectedListener listener) {
        mSelectedListener = listener;
        return this;
    }

    private OnRingSelectedListener mSelectedListener;

    public interface OnRingSelectedListener {
        void onSelected(String ring, String path);
    }

    public static class Music {
        private int id;
        private String title;
        private String url;
        private int duration;
        private int size;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int getDuration() {
            return duration;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        @Override
        public String toString() {
            return "{id=" + id + ",title=" + title + ",url=" + url + ",duration=" + duration + ",size=" + size + "}";
        }
    }
}
