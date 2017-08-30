package com.lingju.assistant.activity.index.view;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lingju.assistant.R;
import com.lingju.assistant.player.audio.LingjuAudioPlayer;
import com.lingju.assistant.view.PlayerListItemView;
import com.lingju.assistant.view.SimpleLineDivider;
import com.lingju.model.PlayMusic;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2016/11/23.
 */
public class PlayerListFragment extends Fragment {

    @BindView(R.id.player_listView)
    protected RecyclerView recyclerView;
    @BindView(R.id.player_list_title)
    protected TextView titleText;
    @BindView(R.id.player_list_tips)
    protected TextView tipsText;

    protected List<PlayMusic> list;
    protected LinearLayoutManager linearLayoutManager;
    protected PlayerListItemClickListener playerListItemClickListener;
    protected int playingPosition = -1;
    protected ColorStateList current_color;
    protected ColorStateList light_color;
    protected String title;
    protected String tips;
    protected int playMode;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_player_music_list, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    protected void init() {
        current_color = getResources().getColorStateList(R.color.base_blue);
        light_color = getResources().getColorStateList(R.color.music_first_text_color);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager((linearLayoutManager = new LinearLayoutManager(getActivity())));
        recyclerView.addItemDecoration(new SimpleLineDivider(getActivity().getResources().getColor(R.color.new_line_white_border)));
        recyclerView.setAdapter(playerListAdapter);
        if (title != null)
            titleText.setText(title);
        if (tips != null)
            tipsText.setText(tips);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    /**
     * 设置播放列表的标题
     *
     * @param title
     * @return
     */
    public PlayerListFragment setTitle(String title) {
        this.title = title;
        if (this.titleText != null)
            this.titleText.setText(title);
        return this;
    }

    public PlayerListFragment setTips(String tips) {
        this.tips = tips;
        if (tipsText != null) {
            tipsText.setText(tips);
        }
        return this;
    }

    public PlayerListFragment setMusicPlayMode(int playMode){
        this.playMode = playMode;
        return this;
    }

    /**
     * 设置播放列表
     *
     * @param list
     * @return
     */
    public PlayerListFragment setPlayMusicList(List<PlayMusic> list) {
        this.list = list;
        return this;
    }

    /**
     * 设置当前正在播放的歌曲所在列表的位置，base:0
     *
     * @param playingPosition
     */
    public void setPlayingPosition(int playingPosition) {
        this.playingPosition = playingPosition;
    }

    /**
     * 刷新视图
     */
    public void refresh() {
        if (this.list == null || this.list.size() == 0) {
            showList(false);
        } else {
            showList(true);
            playerListAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 显示播放列表
     *
     * @param show true=显示
     */
    public void showList(boolean show) {
        if (recyclerView != null) {
            recyclerView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * 设置播放列表的子项的点击监听器
     *
     * @param itemClickListener
     */
    public PlayerListFragment setPlayerListItemClickListener(PlayerListItemClickListener itemClickListener) {
        this.playerListItemClickListener = itemClickListener;
        return this;
    }


    protected static String toTime(int time) {
        time /= 1000;
        int minute = time / 60;
        //int hour = minute / 60;
        int second = time % 60;
        minute %= 60;
        return String.format("%02d:%02d", minute, second);
    }

    /**
     * 设置收藏按钮状态(当前只针对于在线播放列表)
     **/
    public void setFavoriteState(PlayMusic music) {
    }

    protected class PlayerListItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        PlayerListItemView itemView;
        int position;

        public PlayerListItemViewHolder(PlayerListItemView itemView) {
            super(itemView);
            this.itemView = itemView;
            this.itemView.setOnClickListener(this);
            this.itemView.setFavoriteButtonOnclickListener(this);
        }

        void bind(int position, PlayMusic music) {
            this.position = position;
            itemView.setDuration(music.getDuration() == 0 ? "--:--" : toTime(music.getDuration()));
            itemView.setFavorite(music.getFavorite());
            itemView.setState(music.isCloud() ? "在线" : "本地");
            itemView.setTitle(music.getTitle());
            itemView.setSinger(music.getSinger());
            itemView.setTitleColor(position == playingPosition ? current_color : light_color);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() != R.id.player_list_item_favorite_bt) {
                if (playerListItemClickListener != null)
                    playerListItemClickListener.onItemClick(position, v);
            } else {
                if (playerListItemClickListener != null) {
                    playerListItemClickListener.onItemFavoriteClick(position, v);
                    if (position == playingPosition)
                        setFavoriteState(LingjuAudioPlayer.get().currentPlayMusic());
                }
            }
        }
    }

    protected RecyclerView.Adapter<PlayerListItemViewHolder> playerListAdapter = new RecyclerView.Adapter<PlayerListItemViewHolder>() {

        @Override
        public PlayerListItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            PlayerListItemViewHolder holder = new PlayerListItemViewHolder(new PlayerListItemView(getActivity()));
            parent.addView(holder.itemView, layoutParams);
            return holder;
        }

        @Override
        public void onBindViewHolder(PlayerListItemViewHolder holder, int position) {
            holder.bind(position, list.get(position));
        }

        @Override
        public int getItemCount() {
            return list == null ? 0 : list.size();
        }
    };

    /**
     * 播放列表子项的点击监听接口
     */
    public interface PlayerListItemClickListener {
        /**
         * 子项点击回调
         *
         * @param position 子项所在列表的位置
         * @param view     子项对应的view
         */
        void onItemClick(int position, View view);

        /**
         * 子项收藏按钮的点击回调
         *
         * @param position 子项所在列表的位置
         * @param view     收藏按钮的view
         */
        void onItemFavoriteClick(int position, View view);
    }

}
