package com.lingju.assistant.view;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.lingju.assistant.R;
import com.lingju.common.log.Log;

/**
 * Created by Administrator on 2015/7/22.
 */
public class DrawForExpandLayout extends LinearLayout {
    private final static String TAG="";

    private ImageButton drawBar;
    private int mActivePointerId;
    private float lastPosition;
    private int sHeight;
    private float density;

    private int minHeight;

    private int fromPosition;
    private int targetPosition;

    private ScaleChangedListener scListener;

    public DrawForExpandLayout(Context context) {
        super(context);
        init();
    }

    public DrawForExpandLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawForExpandLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private int dp2px(int dp){
        return (int)(0.5F + density*(float)dp);
    }

    public void setMinHeight(int height){
        if(height>0&&height<sHeight){
            this.minHeight=height;
        }
    }

    public void setMaxHeight(int height){
        if(height>0){
            Log.e(TAG,"setMaxHeight="+height);
            this.sHeight=height;
        }
    }

    public void setScaleChangeListener(ScaleChangedListener listener){
        this.scListener=listener;
    }

    /**
     * <ImageButton
     android:id="@+id/ancp_poi_list_drap_bar"
     android:layout_width="match_parent"
     android:layout_height="wrap_content"
     android:background="@color/new_line_border"
     android:src="@drawable/bnav_poi_list_drag"
     android:paddingBottom="10dp"
     android:paddingTop="10dp"
     />
     */
    private void init(){
        DisplayMetrics metrics=getContext().getResources().getDisplayMetrics();
        density=metrics.density;
        sHeight=metrics.heightPixels;
        minHeight=dp2px(50);
        Log.i(TAG,"sHeight="+sHeight+",minHeight="+50);
        drawBar=new ImageButton(getContext());
        LayoutParams layoutParams=new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
        drawBar.setBackgroundResource(R.color.base_blue_translucent);
        drawBar.setImageResource(R.drawable.bnav_poi_list_drag);
        drawBar.setPadding(0, dp2px(10), 0, dp2px(10));
        addView(drawBar, 0, layoutParams);

        drawBar.setOnTouchListener(drawTouchListener);
    }

    private void setOffsetTopAndBottom(int offset, boolean requiresUpdate) {
        bringToFront();
        getLayoutParams().height=getMeasuredHeight()-offset;
        //Log.e(TAG, "setOffsetTopAndBottom>>height="+getLayoutParams().height);
        //offsetTopAndBottom(offset);
        requestLayout();
        if (requiresUpdate && android.os.Build.VERSION.SDK_INT < 11) {
            invalidate();
        }

    }

    private void moveToStart(float interpolatedTime) {
        int targetTop = fromPosition+(int)((targetPosition-fromPosition)*interpolatedTime);
        setOffsetTopAndBottom(targetTop-getTop(),false);
    }

    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            //Log.e(TAG,"applyTransformation>>"+interpolatedTime);
            moveToStart(interpolatedTime);
        }
    };

    private final Animation.AnimationListener animationListener=new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            Log.e(TAG,"onAnimationEnd");
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    };


    OnTouchListener drawTouchListener=new OnTouchListener(){

        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            final int action = MotionEventCompat.getActionMasked(ev);

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    try {
                        mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                        lastPosition = MotionEventCompat.getY(ev, mActivePointerId);
                    }catch (Exception e){
                        e.printStackTrace();
                        return false;
                    }
                    break;
                case MotionEvent.ACTION_MOVE: {
                    final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                    if (pointerIndex < 0) {
                        Log.e(TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                        return false;
                    }
                    final float y = MotionEventCompat.getY(ev, pointerIndex);
                    setOffsetTopAndBottom((int)(y-lastPosition),true);
                    break;
                }
                case MotionEventCompat.ACTION_POINTER_DOWN: {
                    final int index = MotionEventCompat.getActionIndex(ev);
                    mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                    break;
                }
                case MotionEventCompat.ACTION_POINTER_UP:
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                    if (pointerIndex < 0) {
                        Log.e(TAG, "Got ACTION_UP ||ACTION_CANCEL event but have an invalid active pointer id.");
                        return false;
                    }
                    Log.e(TAG,"MotionEvent.ACTION_UP>>height"+getMeasuredHeight());
                    if (getMeasuredHeight()>=sHeight) {
                        getLayoutParams().height = sHeight;
                        invalidate();
                        if(scListener!=null){
                            scListener.max();
                        }
                    } else if (getMeasuredHeight() <= minHeight) {
                        getLayoutParams().height = sHeight / 2;
                        setVisibility(View.GONE);
                        if(scListener!=null){
                            scListener.min();
                        }
                    } else {
                        fromPosition = getTop();
                        targetPosition = sHeight / 2;
                        mAnimateToStartPosition.reset();
                        mAnimateToStartPosition.setInterpolator(new AccelerateInterpolator());
                        mAnimateToStartPosition.setDuration(200);
                        mAnimateToStartPosition.setAnimationListener(animationListener);
                        startAnimation(mAnimateToStartPosition);
                    }
                    break;
                }
            }
            return true;
        }
    };


    public interface ScaleChangedListener{
        public void max();
        public void min();
    }

}
