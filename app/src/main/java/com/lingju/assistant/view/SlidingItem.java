package com.lingju.assistant.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.lingju.util.ScreenUtil;

/**
 * Created by Ken on 2017/1/19.
 */
public class SlidingItem extends LinearLayout {

    private boolean hasValue;   //相关变量是否有值，只需赋值一次即可
    private float mStartX;
    private float mStartY;
    private int mScrollMaxValue;    //最大可滑动距离
    private int mScrollDistance;    //滑动距离
    private boolean isExanded;      //右侧隐藏按钮是否展开
    private int mWidth;             //父控件宽度
    private int mStartLocationX;    //内容控件左侧位置
    private int mEndLocationX;    //内容控件右侧位置
    private boolean clickable;     //是否响应点击
    private boolean slidable = true;     //是否可滑动

    public SlidingItem(Context context) {
        super(context);
        mScrollMaxValue = ScreenUtil.getInstance().dip2px(72);
    }

    public SlidingItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScrollMaxValue = ScreenUtil.getInstance().dip2px(72);
        //        Log.i("LingJu", "最大偏移量：" + mScrollMaxValue);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        /*if (!hasValue) {
            //获取父控件的测量模式
            int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            //获取父控件宽高

            int height = MeasureSpec.getSize(heightMeasureSpec) - getPaddingBottom() - getPaddingTop();
            //获取了子控件的测量规则
            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            //            int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            //测量右侧隐藏的按钮大小
            View slidingButton = getChildAt(1);
            slidingButton.measure(childWidthMeasureSpec, childWidthMeasureSpec);//进行测量
            //就获取到了子控件宽度
            mScrollMaxValue = slidingButton.getMeasuredWidth();
            Log.i("LingJu", "最大可滑动距离：" + mScrollMaxValue);
            setMeasuredDimension(width, height);
            hasValue = true;
        }*/
        if (!hasValue) {
            mWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
            mEndLocationX = mWidth;
            hasValue = true;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        /* 布置控件显示位置 */
        if (!isExanded) {    //收起前需要重置左右两端的位置
            mStartLocationX = 0;
            mEndLocationX = mWidth;
        }
        // Log.i("LingJu", "展开" + isExanded + " 偏移量>>>：" + mScrollDistance);
        int start = mStartLocationX + mScrollDistance;
        int end = mEndLocationX + mScrollDistance;
        getChildAt(0).layout(start /*+ mScrollDistance*/, t, end /*+ mScrollDistance*/, b);
        getChildAt(1).layout(end /*+ mScrollDistance*/, t, r, b);
        // Log.i("LingJu", "起点：" + start + " 终点：" + end);
        if (isExanded) {    //展开后需要记录控件左右两端位置
            mStartLocationX = -mScrollMaxValue;
            mEndLocationX = mWidth - mScrollMaxValue;
            mScrollDistance = 0;       //展开后重置滑动距离
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        /* 请求父控件不要拦截触摸事件 */
        //        getParent().requestDisallowInterceptTouchEvent(true);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!slidable)
            return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                /* 获取滑动起点 */
                mStartX = event.getRawX();
                mStartY = event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                /* 获取滑动终点 */
                float endX = event.getRawX();
                float endY = event.getRawY();

                /* 计算偏移量 */
                int dx = (int) (endX - mStartX);
                int dy = (int) (endY - mStartY);
                // Log.i("LingJu", "水平：" + dx + ", 垂直：" + dy);
                /* 处理左右滑动动作 */
                if (Math.abs(dx) >= Math.abs(dy)) {     //左右滑动
                    clickable = true;
                    if (dx < 0 && !isExanded) {   //左滑且右侧按钮未展开
                        if (itemListener != null)
                            itemListener.onSliding(this);
                        refresh(dx);
                    } else if (dx > 0 && isExanded) {  //右滑且右侧按钮已展开
                        refresh(dx);
                    }
                } else {
                    clickable = false;
                    if (Math.abs(dy) > 10)
                        return false;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                float upEndX = event.getRawX();
                float upEndY = event.getRawY();
                float upDx = upEndX - mStartX;
                float upDy = upEndY - mStartY;
                if (clickable && Math.abs(upDx) < 5 && Math.abs(upDy) < 5) {    //偏移小于5px认为是点击事件
                    if (isExanded) {       //已展开（分内容区和按钮区）
                        float x = event.getX();     //相对于父控件的位置
                        if (x < mWidth - mScrollMaxValue) {     //内容区
                            /*if (itemListener != null) {
                                itemListener.onContentClick(getChildAt(0));
                            }*/
                            hide();
                        } else {
                            if (itemListener != null) {
                                hide();
                                itemListener.onBtnClick(getChildAt(1));
                            }
                        }
                    } else {     //未展开，全部为内容区
                        if (itemListener != null) {
                            hide();
                            itemListener.onContentClick(getChildAt(0));
                        }
                    }
                    return true;
                }


                if (Math.abs(upDx) >= Math.abs(upDy)) {  // 左右滑动
                    if (upDx < 0) {    //左滑
                        show();
                    } else {    //右滑时隐藏
                        hide();
                    }
                }
                break;
        }

        return true;
    }

    public boolean isExanded() {
        return isExanded;
    }

    @Override
    protected void onDetachedFromWindow() {
        //防止该控件在RecylerView中滑出删除区域后滚动列表，造成滑出区域混乱
        hide();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

    }

    /**
     * 展开显示右侧隐藏按钮
     **/
    public void show() {
        if (!isExanded) {
            refresh(-mScrollMaxValue);
            isExanded = true;
            if (itemListener != null)
                itemListener.onExpanded(this);
        }
    }

    /**
     * 隐藏右侧按钮
     **/
    public void hide() {
        if (isExanded) {
            refresh(0);
            isExanded = false;
        }
    }

    /**
     * 刷新视图
     **/
    private void refresh(int dx) {
        int distance = Math.abs(dx);
        /*if (distance <= mScrollMaxValue) {
            mScrollDistance = dx;
            requestLayout();
        }*/
        distance = distance > mScrollMaxValue ? mScrollMaxValue : distance;
        if (dx < 0)
            distance = -distance;
        mScrollDistance = distance;
        requestLayout();
    }

    /**
     * 用于让外界记录控件第一次正常加载时的宽度（针对页面处于删除编辑态，出现checkbox影响控件宽度）
     **/
    public int getParentWidth() {
        return mWidth;
    }

    /**
     * 用于重置控件最原始的正确宽度
     **/
    public void resetParentWidth(int width) {
        if (width != 0 && mWidth != width) {
            mWidth = width;
            mEndLocationX = width;
            isExanded = true;
            hide();
        }
    }

    public void setSlidable(boolean slidable) {
        this.slidable = slidable;
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }

    private OnSlidingItemListener itemListener;

    public void setOnSlidingItemListener(OnSlidingItemListener listener) {
        this.itemListener = listener;
    }

    /**
     * 可左滑控件事件监听器
     **/
    public interface OnSlidingItemListener {
        /**
         * 左滑时触发
         **/
        void onSliding(SlidingItem item);

        /**
         * 右侧隐藏按钮被点击时触发
         **/
        void onBtnClick(View v);

        /**
         * 内容区域点击时触发
         **/
        void onContentClick(View v);

        /**
         * 右侧隐藏按钮完全展开时触发(用于记录当前打开的item，便于下个item展开时关闭这个item)
         **/
        void onExpanded(SlidingItem item);
    }
}
