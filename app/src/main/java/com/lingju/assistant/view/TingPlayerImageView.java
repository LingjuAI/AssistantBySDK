package com.lingju.assistant.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.lingju.assistant.R;

/**
 * Created by Ken on 2017/6/9.
 */
public class TingPlayerImageView extends ImageView {


    private Animation mTingAnim;

    public TingPlayerImageView(Context context) {
        super(context);
        mTingAnim = AnimationUtils.loadAnimation(context, R.anim.ting_listening_loading);

    }

    public TingPlayerImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTingAnim = AnimationUtils.loadAnimation(context, R.anim.ting_listening_loading);
    }

    public void setImage(Context context,String imageUrl) {
        Glide.with(context)
                .load(imageUrl)
                .fallback(R.drawable.ic_launcher)
                .transform(new CircleTransform(context))
                .into(this);
    }

    public void startAnim() {
        this.startAnimation(mTingAnim);
    }

    public void stopAnim() {
        this.clearAnimation();
    }
}
