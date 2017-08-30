package com.lingju.assistant.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lingju.assistant.R;

/**
 * Created by Administrator on 2016/11/23.
 */
public class PlayerListItemView extends LinearLayout {

    private TextView title;
    private TextView state;
    private TextView singer;
    private TextView duration;
    private ImageButton favoriteBt;

    public PlayerListItemView(Context context) {
        super(context);
        init(context);
    }

    public PlayerListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PlayerListItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    protected void init(Context mContext) {
        setClickable(true);
        LayoutInflater iflater=LayoutInflater.from(mContext);
        iflater.inflate(R.layout.player_list_item, this);
        title = (TextView) findViewById(R.id.player_list_item_title);
        state = (TextView) findViewById(R.id.player_list_item_state);
        singer = (TextView) findViewById(R.id.player_list_item_singer);
        duration = (TextView) findViewById(R.id.player_list_item_duration);
        favoriteBt = (ImageButton) findViewById(R.id.player_list_item_favorite_bt);
    }

    public void setState(String state) {
        this.state.setText(state);
    }

    public void setDuration(String duration) {
        this.duration.setText(duration);
    }

    public void setSinger(String singer) {
        this.singer.setText(singer);
    }

    public void setTitle(String title) {
        this.title.setText(title);
    }

    public void setTitleColor(ColorStateList color){
        this.title.setTextColor(color);
    }

    public void setTitleColor(int color){
        this.title.setTextColor(color);
    }

    public void setFavorite(boolean flag){
//        LevelListDrawable drawable=(LevelListDrawable) favoriteBt.getBackground();
        favoriteBt.setImageLevel(flag?1:0);
//        drawable.setLevel(flag?1:0);
    }

    public void setFavoriteButtonOnclickListener(OnClickListener clickListener){
        this.favoriteBt.setOnClickListener(clickListener);
    }

}
