package com.lingju.assistant.activity;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.base.GoBackActivity;
import com.lingju.assistant.activity.event.OnItemClickListener;
import com.lingju.assistant.view.DividerItemDecoration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Ken on 2016/12/1.
 */
public class RingListActivity extends GoBackActivity {

    public final static int FOR_RING_SELECTE=7;
    public final static String URL = "url";
    public final static String RING = "ring";
    @BindView(R.id.arl_list)
    RecyclerView mArlList;
    @BindView(R.id.ll_loading)
    LinearLayout mLlLoading;
    private RingAdapter mAdapter;
    private LoadRingTask mRingTask;
    private List<Music> mRings;
    private int mCurrentPosition = -1;
    private Intent intent;
    private String selectUrl;
    private String selectTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ring_list);
        ButterKnife.bind(this);
        mRings = new ArrayList<>();
        mAdapter = new RingAdapter();
        mArlList.setAdapter(mAdapter);
        //如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
        mArlList.setHasFixedSize(true);
        mArlList.setLayoutManager(new LinearLayoutManager(this));
        mArlList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
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

        intent = getIntent();
        if (intent != null) {
            selectUrl = intent.getStringExtra(URL);
            selectTitle = intent.getStringExtra(RING);
        }
        mRingTask = new LoadRingTask();
        mRingTask.execute();
    }

    /*@OnClick(R.id.arl_back)
    public void selectAndBack() {
        if(intent != null) {
            intent.putExtra(URL, selectUrl);
            intent.putExtra(RING, selectTitle);
            if(intent.getIntExtra(MainActivity.RESULT_CODE, 0) == 0) {
                setResult(FOR_RING_SELECTE, intent);
            }else{
                intent.setClass(RingListActivity.this, MainActivity.class);
                startActivity(intent);
            }
        }
        goBack();
    }*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRingTask != null) {
            mRingTask.cancel(true);
            mRingTask = null;
        }
        mAdapter.cancelPlay();
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
            ContentResolver resolver = RingListActivity.this.getContentResolver();
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
                        while (cursor.moveToNext()){
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
            ViewGroup itemView = (ViewGroup) LayoutInflater.from(RingListActivity.this).inflate(R.layout.speaker_line, parent, false);
            return new RingHolder(itemView);
        }

        @Override
        public void onBindViewHolder(RingHolder holder, int position) {
            Music music = mRings.get(position);
            holder.tvRingName.setText(music.getTitle());
            if (position == mCurrentPosition || music.getUrl().equals(selectUrl)) {
                holder.btnFavor.setVisibility(View.VISIBLE);
            } else {
                holder.btnFavor.setVisibility(View.GONE);
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
            public Button btnFavor;
            public TextView tvRingName;

            public RingHolder(View itemView) {
                super(itemView);
                tvRingName = (TextView) itemView.findViewById(R.id.list_speaker_text);
                btnFavor = (Button) itemView.findViewById(R.id.list_favorite_bt);
            }
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            mClickListener = listener;
        }
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
