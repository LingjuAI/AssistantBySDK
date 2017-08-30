package com.lingju.assistant.activity.index.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.entity.Lyric;
import com.lingju.assistant.entity.Sentence;
import com.lingju.assistant.player.audio.LingjuAudioPlayer;
import com.lingju.model.PlayMusic;
import com.lingju.common.log.Log;
import com.lingju.util.PlayList;
import com.lingju.util.ScreenUtil;

import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Administrator on 2016/11/23.
 */
public class PlayerListLrcFragment extends PlayerListFragment {
    private final static String TAG = "PlayerListLrcFragment";

    View root;
    @BindView(R.id.player_lyric_listView)
    RecyclerView lyricListView;
    @BindView(R.id.player_list_lrc_play_mode)
    ImageButton setPlayModeBt;
    @BindView(R.id.player_list_lrc_duration)
    TextView durationText;
    @BindView(R.id.player_list_lrc_favorite_bt)
    ImageButton favoriteBt;
    private Lyric lyric = new Lyric();


    private boolean move = false;
    private int mIndex = 0;

    private boolean lyricOpen;
    private int current_lrc_index = -1;


    private int lrcListViewHeight;
    private int listBoxHeight;
    private AtomicBoolean isRuning = new AtomicBoolean(false);

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.frag_player_music_list_lrc, container, false);
        ButterKnife.bind(this, root);
        init();
        return root;
    }

    protected void init() {
        super.init();
        lyricListView.setHasFixedSize(true);
        lyricListView.setLayoutManager((linearLayoutManager = new LinearLayoutManager(getActivity())));
        lyricListView.setAdapter(lyricListAdapter);
        lyricListView.addOnScrollListener(new RecyclerViewListener());

        if (title != null)
            titleText.setText(title);
        if (list != null && list.size() > 0) {
            tipsText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setPlayMode(this.playMode);
        updateFavorte();
    }

    public void updateFavorte() {
        setFavoriteState(LingjuAudioPlayer.get().currentPlayMusic());
        refresh();
    }

    @OnClick({R.id.player_list_lrc_play_mode, R.id.player_list_lrc_favorite_bt})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.player_list_lrc_play_mode:
                LingjuAudioPlayer lp = LingjuAudioPlayer.get();
                lp.setPlayMode((lp.getPlayMode() + 1) % 3);
                break;
            case R.id.player_list_lrc_favorite_bt:
                /* 在数据库中操作数据 */
                PlayMusic playMusic = LingjuAudioPlayer.get().currentPlayMusic();
                if (playMusic != null) {
                   /* playMusic.setFavorite(!playMusic.getFavorite());
                    if (playMusic.isFavorite())
                        LingjuAudioPlayer.get().addToFavoites(playMusic);
                    else {
                        LingjuAudioPlayer.get().removeFromFavoites(playMusic);
                        LingjuAudioPlayer.get().removeFavoriteFromList(playMusic);
                    }*/
                    if (playerListItemClickListener != null) {
                        playerListItemClickListener.onItemFavoriteClick(playingPosition, v);
                        setFavoriteState(playMusic);
                    }
                }

                break;
        }
    }

    @Override
    public void refresh() {
        super.refresh();
        refreshLyric();
    }

    private void refreshLyric() {
        if (lyric != null && lyricListView != null) {
            lyricListAdapter.notifyDataSetChanged();
            /*lyricListView.post(new Runnable() {
                @Override
                public void run() {
                    lyricListView.smoothScrollToPosition(location);
                }
            });*/
        }
    }

    public void setLyric(Lyric lyric) {
        this.lyric = lyric;
        refreshLyric();
    }

    public void setPlayMode(int playMode) {
        this.playMode = playMode;
        switch (playMode) {
            case PlayList.PlayMode.ORDER:
                setPlayModeBt.setImageLevel(PlayList.PlayMode.ORDER);
                return;
            case PlayList.PlayMode.RANDOM:
                setPlayModeBt.setImageLevel(PlayList.PlayMode.RANDOM);
                return;
            case PlayList.PlayMode.SINGLE:
                setPlayModeBt.setImageLevel(PlayList.PlayMode.SINGLE);
                return;
            default:
                return;
        }
    }

    public void showCurrentMusic(PlayMusic music) {
        durationText.setText(toTime(music.getDuration()));
        tipsText.setVisibility(View.GONE);      //有播放歌曲时，隐藏提示文本
        setFavoriteState(music);
    }

    /**
     * 设置收藏按钮状态
     **/
    @Override
    public void setFavoriteState(PlayMusic music) {
        if (favoriteBt != null) {
            // LevelListDrawable ld = (LevelListDrawable) favoriteBt.getBackground();
            if (music != null)
                // ld.setLevel(music.isFavorite() ? 1 : 0);
                favoriteBt.setImageLevel(music.isFavorite() ? 1 : 0);
            else
                favoriteBt.setImageLevel(0);
        }
    }

    public void updateProgress(int play_current_time, int play_all_time) {
        if (lyric != null) {
            int index = lyric.getNowSenPosition(play_current_time);
            updateLrc(index, play_all_time);
        }
    }

    private int location = 0;

    public void updateLrc(int index, int duration) {
        // lyricListView.smoothScrollToPosition(location);
        int firstPosition = linearLayoutManager.findFirstVisibleItemPosition();
        int lastPosition = linearLayoutManager.findLastVisibleItemPosition();
        int temp = (lastPosition - firstPosition) / 2;
        // Log.i("LingJu", "current_lrc_index  is :" + current_lrc_index);
        if (current_lrc_index != index) {
            if (lyricOpen) {
                if (index > temp && index < (lyricListAdapter.getItemCount() - temp)) {
                    location = index - temp;
                } else if (index <= temp) {
                    location = 0;
                } else {
                    location = lyricListAdapter.getItemCount() - 1;
                }

            /*int b=index-(l-f)/2;
            b=b<0?0:b;
            Log.i(TAG, "f="+f+",l="+l+",b="+b+",index="+index);
            if(f!=b&&(f<b||f>index)){
                Log.w(PlayerListLrcFragment.class.getName(), "scroll to "+b+">>"+lyric.getList().get(b).getContent());
                lyricListView.smoothScrollToPosition(b);
            }*/
            } else {
                if (lyricListView != null)
                    location = index == (lyric.size() - 1) ? index : index + 1;
            }
            durationText.setText(duration > 0 ? toTime(duration) : "");
            /*Log.i("LingJu", "歌词滚动>>> first:" + firstPosition
                    + " last:" + lastPosition + " index" + index + " location:" + location);*/

            current_lrc_index = index;
            if (lyricOpen) {
                move(location);
            } else {
                lyricListView.scrollToPosition(location);
            }
            lyricListAdapter.notifyDataSetChanged();
            // lyricListView.smoothScrollToPosition(location);


        }
        //        else {
        //            if (firstPosition > index)
        //                location = index - temp <= 0 ? 0 : index - temp;

        //            Log.i("LingJu", "持续滚动：" + location);
        //   lyricListAdapter.notifyDataSetChanged();
        //  lyricListView.smoothScrollToPosition(location);

        //        }
       /* refreshLyric();
        lyricListView.smoothScrollToPosition(location);*/
    }

    public void switchLyricBox() {
        if (isRuning.get())
            return;
        listBoxHeight = root.findViewById(R.id.player_list_lrc_listbox).getHeight();
        if (!lyricOpen) {       //展开
            LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) lyricListView.getLayoutParams();
            if (layout != null) {
                lrcListViewHeight = layout.height;
                Log.e(TAG, "open>>>>lrcListViewHeight====" + lrcListViewHeight);
                animation.reset();
                animation.setDuration(200);
                animation.setInterpolator(new AccelerateInterpolator());
                animation.setAnimationListener(animationListener);
                isRuning.set(true);
                lyricOpen = true;
                lyricListView.startAnimation(animation);
            }
        } else {
            if (lrcListViewHeight > 0) {
                Log.e(TAG, "close>>>>>>lrcListViewHeight====" + lrcListViewHeight);
                animation.reset();
                animation.setDuration(200);
                animation.setInterpolator(new AccelerateInterpolator());
                animation.setAnimationListener(animationListener);
                isRuning.set(true);
                lyricOpen = false;
                lyricListView.startAnimation(animation);
            }
        }
    }

    public boolean isLrcOpen() {
        return lyricOpen;
    }

    private Animation animation = new Animation() {

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            if (lyricOpen) {
                lyricListView.getLayoutParams().height = lrcListViewHeight + (int) ((listBoxHeight - lrcListViewHeight) * interpolatedTime);
            } else {
                lyricListView.getLayoutParams().height = listBoxHeight - (int) ((listBoxHeight - lrcListViewHeight) * interpolatedTime);
            }
            lyricListView.requestLayout();
            if (android.os.Build.VERSION.SDK_INT < 11) {
                lyricListView.invalidate();
            }
        }

    };

    private Animation.AnimationListener animationListener = new Animation.AnimationListener() {

        @Override
        public void onAnimationStart(Animation animation) {
            isRuning.set(true);
            Log.i(TAG, "onAnimationStart");
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            isRuning.set(false);
            Log.i(TAG, "onAnimationEnd");
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    };
    View.OnClickListener lyricClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "lyricListView.onClick.......................");
            switchLyricBox();
        }
    };

    private class LyricItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView itemView;
        int position;

        public LyricItemViewHolder(TextView itemView) {
            super(itemView);
            this.itemView = itemView;
        }

        void bind(int position, Sentence st) {
            this.position = position;
            itemView.setText(st.getContent());
            if (current_lrc_index == position) {
                itemView.setTextColor(current_color);
                //holder.itemView.setTextSize(TypedValue.COMPLEX_UNIT_SP,20);
            } else {
                itemView.setTextColor(getResources().getColorStateList(R.color.music_second_text_color));
                //holder.itemView.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);
            }
        }

        @Override
        public void onClick(View v) {
            if (v.getId() != R.id.player_list_item_favorite_bt) {
                if (playerListItemClickListener != null)
                    playerListItemClickListener.onItemClick(position, v);
            } else {
                if (playerListItemClickListener != null) {
                    playerListItemClickListener.onItemFavoriteClick(position, v);
                }
            }
        }
    }

    private RecyclerView.Adapter<LyricItemViewHolder> lyricListAdapter = new RecyclerView.Adapter<LyricItemViewHolder>() {

        /**
         * <TextView
         android:id="@+id/player_music_lyric_line_text"
         android:layout_width="match_parent"
         android:layout_height="28dp"
         android:gravity="center"
         android:textSize="14sp"
         android:textColor="@color/music_second_text_color"
         android:paddingLeft="30dp"
         android:paddingRight="30dp"
         android:clickable="false"
         />
         * @param parent
         * @param viewType
         * @return
         */
        @Override
        public LyricItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ScreenUtil.getInstance().dip2px(28));
            LyricItemViewHolder holder = new LyricItemViewHolder(new TextView(getActivity()));
            holder.itemView.setGravity(Gravity.CENTER);
            holder.itemView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            holder.itemView.setTextColor(getActivity().getResources().getColor(R.color.music_second_text_color));
            holder.itemView.setPadding(ScreenUtil.getInstance().dip2px(30), 0, ScreenUtil.getInstance().dip2px(30), 0);
            holder.itemView.setClickable(false);
            holder.itemView.setOnClickListener(lyricClickListener);
            parent.addView(holder.itemView, layoutParams);
            return holder;
        }

        @Override
        public void onBindViewHolder(LyricItemViewHolder holder, int position) {
            // Log.i("lingju", "position is :" + position);
            holder.bind(position, lyric.getList().get(position));

        }

        @Override
        public int getItemCount() {
            return lyric == null ? 0 : lyric.size();
        }
    };

    private void move(int n) {
        if (n < 0 || n >= lyricListAdapter.getItemCount()) {
            // Toast.makeText(this,"超出范围了",Toast.LENGTH_SHORT).show();
            return;
        }
        mIndex = n;
        lyricListView.stopScroll();
        moveToPosition(n);


    }

    private void moveToPosition(int n) {

        int firstItem = linearLayoutManager.findFirstVisibleItemPosition();
        int lastItem = linearLayoutManager.findLastVisibleItemPosition();
        if (n <= firstItem) {
            lyricListView.scrollToPosition(n);
        } else if (n <= lastItem) {
            int top = lyricListView.getChildAt(n - firstItem).getTop();
            lyricListView.scrollBy(0, top);
        } else {
            lyricListView.scrollToPosition(n);
            move = true;
        }

    }

    class RecyclerViewListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (move && newState == RecyclerView.SCROLL_STATE_IDLE) {
                move = false;
                int n = mIndex - linearLayoutManager.findFirstVisibleItemPosition();
                if (0 <= n && n < lyricListView.getChildCount()) {
                    int top = lyricListView.getChildAt(n).getTop();
                    lyricListView.smoothScrollBy(0, top);
                }

            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (move) {
                move = false;
                int n = mIndex - linearLayoutManager.findFirstVisibleItemPosition();
                if (0 <= n && n < lyricListView.getChildCount()) {
                    int top = lyricListView.getChildAt(n).getTop();
                    lyricListView.scrollBy(0, top);
                }
            }
        }
    }


}
