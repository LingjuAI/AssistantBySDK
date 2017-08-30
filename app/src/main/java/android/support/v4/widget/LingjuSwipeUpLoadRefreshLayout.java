package android.support.v4.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;

import com.lingju.common.log.Log;


/**
 * Created by Administrator on 2015/7/21.
 */
public class LingjuSwipeUpLoadRefreshLayout extends ViewGroup {
    private final static String TAG = "LingjuSwipeRefreshLayout";
    // Maps to ProgressBar.Large style
    public static final int LARGE = MaterialProgressDrawable.LARGE;
    // Maps to ProgressBar default style
    public static final int DEFAULT = MaterialProgressDrawable.DEFAULT;

    private static final String LOG_TAG = SwipeRefreshLayout.class.getSimpleName();

    private static final int MAX_ALPHA = 255;
    private static final int STARTING_PROGRESS_ALPHA = (int) (.3f * MAX_ALPHA);

    private static final int CIRCLE_DIAMETER = 40;
    private static final int CIRCLE_DIAMETER_LARGE = 56;

    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;
    private static final float DRAG_RATE = .5f;

    // Max amount of circle that can be filled by progress during swipe gesture,
    // where navi.0 is a full circle
    private static final float MAX_PROGRESS_ANGLE = .8f;

    private static final int SCALE_DOWN_DURATION = 150;

    private static final int ALPHA_ANIMATION_DURATION = 300;

    private static final int ANIMATE_TO_TRIGGER_DURATION = 200;

    private static final int ANIMATE_TO_START_DURATION = 200;

    // Default background for the progress spinner
    private static final int CIRCLE_BG_LIGHT = 0xFFFAFAFA;
    // Default offset in dips from the top of the view to where the progress spinner should stop
    private static final int DEFAULT_CIRCLE_TARGET = 64;

    private View mTarget; // the target of the gesture
    private OnRefreshListener mListener;
    private boolean tRefreshing = false;
    private boolean bRefreshing = false;
    private int mTouchSlop;
    private float mTotalDragDistance = -1;
    private int mMediumAnimationDuration;
    private int tCurrentTargetOffsetTop;
    private int bCurrentTargetOffsetTop;
    // Whether or not the starting offset has been determined.
    private boolean tOriginalOffsetCalculated = false;
    private boolean bOriginalOffsetCalculated = false;

    private float mInitialMotionY;
    private float mInitialDownY;
    private boolean tIsBeingDragged;
    private boolean bIsBeingDragged;
    private int mActivePointerId = INVALID_POINTER;
    // Whether this item is scaled up rather than clipped
    private boolean mScale;

    // Target is returning to its start offset because it was cancelled or a
    // refresh was triggered.
    private boolean mReturningToStart;
    private final DecelerateInterpolator mDecelerateInterpolator;
    private static final int[] LAYOUT_ATTRS = new int[]{
            android.R.attr.enabled
    };

    private CircleImageView bCircleView;
    private int tCircleViewIndex = -1;
    private int bCircleViewIndex = -1;

    protected int mFrom;

    private float mStartingScale;

    protected int tOriginalOffsetTop;
    protected int bOriginalOffsetTop;

    private MaterialProgressDrawable bProgress;

    private Animation mScaleAnimation;

    private Animation mScaleDownAnimation;

    private Animation mAlphaStartAnimation;

    private Animation mAlphaMaxAnimation;

    private Animation mScaleDownToStartAnimation;

    private float mSpinnerFinalOffset;

    private boolean mNotify;

    private int mCircleWidth;

    private int mCircleHeight;

    // Whether the client has set a custom starting position;
    private boolean mUsingCustomStart;

    private boolean allowDrag = true;

    private Animation.AnimationListener mRefreshListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            Log.e(TAG, "mRefreshListener>>onAnimationEnd>>" + Boolean.toString(tRefreshing) + "," + Boolean.toString(bRefreshing));
                if (bRefreshing) {
                    // Make sure the progress view is fully visible
                    bProgress.setAlpha(MAX_ALPHA);
                    bProgress.start();
                    if (mNotify) {
                        if (mListener != null) {
                            mListener.onUpPullRefresh();
                        }
                    }
                } else {
                    bProgress.stop();
                    bCircleView.setVisibility(View.GONE);
                    setColorViewAlpha(bCircleView, MAX_ALPHA);
                    // Return the circle to its start position
                    if (mScale) {
                        setAnimationProgress(bCircleView, 0);
                    } else {
                        setTargetOffsetTopAndBottom(bCircleView, bOriginalOffsetTop - bCurrentTargetOffsetTop, true /* requires update */);
                    }

            }
            bCurrentTargetOffsetTop = bCircleView.getTop();
        }
    };

    public void setColorViewAlpha(CircleImageView view, int targetAlpha) {
            bCircleView.getBackground().setAlpha(targetAlpha);
            bProgress.setAlpha(targetAlpha);

    }

    public void setbRefreshing(boolean refreshing) {
        this.bRefreshing = refreshing;
    }

    /**
     * The refresh indicator starting and resting position is always positioned
     * near the top of the refreshing content. This position is a consistent
     * location, but can be adjusted in either direction based on whether or not
     * there is a toolbar or actionbar present.
     *
     * @param scale Set to true if there is no view at a higher z-order than
     *              where the progress spinner is set to appear.
     * @param start The offset in pixels from the top of this view at which the
     *              progress spinner should appear.
     * @param end   The offset in pixels from the top of this view at which the
     *              progress spinner should come to rest after a successful swipe
     *              gesture.
     */
    public void setProgressViewOffset(boolean scale, int start, int end) {
        mScale = scale;
        tOriginalOffsetTop = tCurrentTargetOffsetTop = start;
        mSpinnerFinalOffset = end;
        mUsingCustomStart = true;

        bCircleView.setVisibility(View.GONE);
        bOriginalOffsetTop = bCurrentTargetOffsetTop = getMeasuredHeight() + start - bCircleView.getMeasuredHeight();
        bCircleView.invalidate();
    }

    /**
     * The refresh indicator resting position is always positioned near the top
     * of the refreshing content. This position is a consistent location, but
     * can be adjusted in either direction based on whether or not there is a
     * toolbar or actionbar present.
     *
     * @param scale Set to true if there is no view at a higher z-order than
     *              where the progress spinner is set to appear.
     * @param end   The offset in pixels from the top of this view at which the
     *              progress spinner should come to rest after a successful swipe
     *              gesture.
     */
    public void setProgressViewEndTarget(boolean scale, int end) {
        mSpinnerFinalOffset = end;
        mScale = scale;
        bCircleView.invalidate();
    }

    /**
     * One of DEFAULT, or LARGE.
     */
    public void setSize(int size) {
        if (size != MaterialProgressDrawable.LARGE && size != MaterialProgressDrawable.DEFAULT) {
            return;
        }
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        if (size == MaterialProgressDrawable.LARGE) {
            mCircleHeight = mCircleWidth = (int) (CIRCLE_DIAMETER_LARGE * metrics.density);
        } else {
            mCircleHeight = mCircleWidth = (int) (CIRCLE_DIAMETER * metrics.density);
        }

        bCircleView.setImageDrawable(null);
        bProgress.updateSizes(size);
        bCircleView.setImageDrawable(bProgress);
    }

    /**
     * Simple constructor to use when creating a SwipeRefreshLayout from code.
     *
     * @param context
     */
    public LingjuSwipeUpLoadRefreshLayout(Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating SwipeRefreshLayout from XML.
     *
     * @param context
     * @param attrs
     */
    public LingjuSwipeUpLoadRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mMediumAnimationDuration = getResources().getInteger(
                android.R.integer.config_mediumAnimTime);

        setWillNotDraw(false);
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mCircleWidth = (int) (CIRCLE_DIAMETER * metrics.density);
        mCircleHeight = (int) (CIRCLE_DIAMETER * metrics.density);

        createProgressView();
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
        // the absolute offset has to take into account that the circle starts at an offset
        mSpinnerFinalOffset = DEFAULT_CIRCLE_TARGET * metrics.density;
        mTotalDragDistance = mSpinnerFinalOffset;
    }

    protected int getChildDrawingOrder(int childCount, int i) {
        if (tCircleViewIndex < 0) {
            return i;
        } else if (i == childCount - 1) {
            // Draw the selected child last
            return tCircleViewIndex;
        } else if (i >= tCircleViewIndex) {
            // Move the children after the selected child earlier one
            return i + 1;
        } else {
            // Keep the children before the selected child the same
            return i;
        }
    }

    private void createProgressView() {

        bCircleView = new CircleImageView(getContext(), CIRCLE_BG_LIGHT, CIRCLE_DIAMETER / 2);

        bProgress = new MaterialProgressDrawable(getContext(), this);

        bProgress.setBackgroundColor(CIRCLE_BG_LIGHT);

        bCircleView.setImageDrawable(bProgress);
        bCircleView.setVisibility(View.GONE);

        addView(bCircleView);
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mListener = listener;
    }

    /**
     * Pre API 11, alpha is used to make the progress circle appear instead of scale.
     */
    private boolean isAlphaUsedForScale() {
        return android.os.Build.VERSION.SDK_INT < 11;
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setRefreshing(boolean refreshing) {
        if (refreshing) {
            if (bRefreshing != refreshing) {
                bRefreshing = refreshing;
                int endTarget = 0;
                endTarget = bOriginalOffsetTop - (int) mSpinnerFinalOffset;
                setTargetOffsetTopAndBottom(bCircleView, endTarget - bCircleView.getTop(),
                        true /* requires update */);
                mNotify = false;
                startScaleUpAnimation(bCircleView, mRefreshListener);
            }
        } else {
            setRefreshing(bCircleView, refreshing, false /* notify */);
        }
    }

    /** 取消刷新功能 **/
    public void setReturningToStart(boolean isAllowRefresh){
        this.mReturningToStart = isAllowRefresh;
    }


    private void startScaleUpAnimation(final CircleImageView view, Animation.AnimationListener listener) {
        view.setVisibility(View.VISIBLE);
        if (android.os.Build.VERSION.SDK_INT >= 11) {
                bProgress.setAlpha(MAX_ALPHA);
        }
        mScaleAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setAnimationProgress(view, interpolatedTime);
            }
        };
        mScaleAnimation.setDuration(mMediumAnimationDuration);
        if (listener != null) {
            view.setAnimationListener(listener);
        }
        view.clearAnimation();
        view.startAnimation(mScaleAnimation);
    }

    /**
     * Pre API 11, this does an alpha animation.
     *
     * @param progress
     */
    private void setAnimationProgress(CircleImageView view, float progress) {
        if (isAlphaUsedForScale()) {
            setColorViewAlpha(view, (int) (progress * MAX_ALPHA));
        } else {
            ViewCompat.setScaleX(view, progress);
            ViewCompat.setScaleY(view, progress);
        }
    }

    private void setRefreshing(CircleImageView view, boolean refreshing, final boolean notify) {
      if (view == bCircleView && bRefreshing != refreshing) {
            mNotify = notify;
            ensureTarget();
            bRefreshing = refreshing;
            if (bRefreshing) {
                animateOffsetToCorrectPosition(view, bCurrentTargetOffsetTop, mRefreshListener);
            } else {
                startScaleDownAnimation(view, mRefreshListener);
            }
        }
    }

    private void startScaleDownAnimation(final CircleImageView view, Animation.AnimationListener listener) {
        mScaleDownAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setAnimationProgress(view, 1 - interpolatedTime);
            }
        };
        mScaleDownAnimation.setDuration(SCALE_DOWN_DURATION);
        view.setAnimationListener(listener);
        view.clearAnimation();
        view.startAnimation(mScaleDownAnimation);
    }

    private void startProgressAlphaStartAnimation(MaterialProgressDrawable progress) {
        mAlphaStartAnimation = startAlphaAnimation(progress, progress.getAlpha(), STARTING_PROGRESS_ALPHA);
    }

    private void startProgressAlphaMaxAnimation(MaterialProgressDrawable progress) {
        mAlphaMaxAnimation = startAlphaAnimation(progress, progress.getAlpha(), MAX_ALPHA);
    }

    private Animation startAlphaAnimation(final MaterialProgressDrawable progress, final int startingAlpha, final int endingAlpha) {
        // Pre API 11, alpha is used in place of scale. Don't also use it to
        // show the trigger point.
        if (mScale && isAlphaUsedForScale()) {
            return null;
        }
        Animation alpha = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                progress.setAlpha((int) (startingAlpha + ((endingAlpha - startingAlpha)
                        * interpolatedTime)));
            }
        };
        alpha.setDuration(ALPHA_ANIMATION_DURATION);
        // Clear out the previous animation listeners.
            bCircleView.setAnimationListener(null);
            bCircleView.clearAnimation();
            bCircleView.startAnimation(alpha);
        return alpha;
    }

    /**
     * @deprecated Use {@link #setProgressBackgroundColorSchemeResource(int)}
     */
    @Deprecated
    public void setProgressBackgroundColor(int colorRes) {
        setProgressBackgroundColorSchemeResource(colorRes);
    }

    /**
     * Set the background color of the progress spinner disc.
     *
     * @param colorRes Resource id of the color.
     */
    public void setProgressBackgroundColorSchemeResource(int colorRes) {
        setProgressBackgroundColorSchemeColor(getResources().getColor(colorRes));
    }

    /**
     * Set the background color of the progress spinner disc.
     *
     * @param color
     */
    public void setProgressBackgroundColorSchemeColor(int color) {
        bCircleView.setBackgroundColor(color);
        bProgress.setBackgroundColor(color);

    }

    /**
     * @deprecated Use {@link #setColorSchemeResources(int...)}
     */
    @Deprecated
    public void setColorScheme(int... colors) {
        setColorSchemeResources(colors);
    }

    /**
     * Set the color resources used in the progress animation from color resources.
     * The first color will also be the color of the bar that grows in response
     * to a user swipe gesture.
     *
     * @param colorResIds
     */
    public void setColorSchemeResources(int... colorResIds) {
        final Resources res = getResources();
        int[] colorRes = new int[colorResIds.length];
        for (int i = 0; i < colorResIds.length; i++) {
            colorRes[i] = res.getColor(colorResIds[i]);
        }
        setColorSchemeColors(colorRes);
    }

    /**
     * Set the colors used in the progress animation. The first
     * color will also be the color of the bar that grows in response to a user
     * swipe gesture.
     *
     * @param colors
     */
    public void setColorSchemeColors(int... colors) {
        ensureTarget();
        bProgress.setColorSchemeColors(colors);
    }

    /**
     * @return Whether the SwipeRefreshWidget is actively showing refresh
     * progress.
     */
    public boolean isRefreshing() {
        return tRefreshing;
    }

    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid
        // out yet.
        if (mTarget == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(bCircleView)) {
                    mTarget = child;
                    break;
                }
            }
        }
    }

    /**
     * Set the distance to trigger a sync in dips
     *
     * @param distance
     */
    public void setDistanceToTriggerSync(int distance) {
        mTotalDragDistance = distance;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        //Log.i(TAG,"onLayout>>"+Boolean.toString(changed)+",left="+left+",top="+top+",right="+right+",bottom="+bottom);
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        final View child = mTarget;
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        if (bOriginalOffsetCalculated) {
           int circleWidth = bCircleView.getMeasuredWidth();
           int circleHeight = bCircleView.getMeasuredHeight();

            bCircleView.layout((width / 2 - circleWidth / 2), bCurrentTargetOffsetTop,
                    (width / 2 + circleWidth / 2), bCurrentTargetOffsetTop + circleHeight);
        }
        if (changed) {
            bOriginalOffsetTop = getMeasuredHeight();
        }
        // Log.e(TAG, "onLayout>>>height=" + height+",tCurrentTargetOffsetTop="+tCurrentTargetOffsetTop+",bCurrentTargetOffsetTop="+bCurrentTargetOffsetTop);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        mTarget.measure(MeasureSpec.makeMeasureSpec(
                getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
        bCircleView.measure(MeasureSpec.makeMeasureSpec(mCircleWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mCircleHeight, MeasureSpec.EXACTLY));
        if (!mUsingCustomStart && !tOriginalOffsetCalculated) {
            tOriginalOffsetCalculated = true;
            bCurrentTargetOffsetTop = bOriginalOffsetTop = getMeasuredHeight();
            Log.e(TAG, "onMeasure>>>tCurrentTargetOffsetTop=" + tCurrentTargetOffsetTop + ",bCurrentTargetOffsetTop=" + bCurrentTargetOffsetTop);
        }
        bCircleViewIndex = -1;
        // Get the index of the circleview.
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == bCircleView) {
                bCircleViewIndex = index;
                break;
            }
        }
    }

    public boolean canChildScrollDown() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return absListView.getChildCount() > 0
                        && absListView.getLastVisiblePosition() == absListView.getAdapter().getCount() - 1;
            } else {
                return mTarget.getScrollY() < 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, 1);
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mTarget, -1) || mTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, -1);
        }
    }

    public void setAllowDrag(boolean allowDrag) {
        this.allowDrag = allowDrag;
    }

    public boolean isAllowDrag() {
        return allowDrag;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!allowDrag)
            return false;
        ensureTarget();

        final int action = MotionEventCompat.getActionMasked(ev);

        /*if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }*/
        final boolean isTop = !canChildScrollUp();
        final boolean isBottom = !canChildScrollDown();

        if (!isEnabled() || mReturningToStart || (!isTop && !isBottom) || tRefreshing || bRefreshing) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!bOriginalOffsetCalculated) {
                    bCurrentTargetOffsetTop = bOriginalOffsetTop = getMeasuredHeight();
                    bOriginalOffsetCalculated = true;
                    Log.e(TAG, "onLayout>>set bOriginalOffsetTop=" + bOriginalOffsetTop);
                }
                tIsBeingDragged = false;
                bIsBeingDragged = false;
                if (isBottom) {
                    setTargetOffsetTopAndBottom(bCircleView, bOriginalOffsetTop - bCircleView.getTop(), true);
                } else
                    return false;
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                final float initialDownY = getMotionEventY(ev, mActivePointerId);
                if (initialDownY == -1) {
                    return false;
                }
                mInitialDownY = initialDownY;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    android.util.Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }

                final float y = getMotionEventY(ev, mActivePointerId);
                if (y == -1) {
                    return false;
                }
                final float yDiff = y - mInitialDownY;
                if (isBottom && yDiff < 0 && Math.abs(yDiff) > mTouchSlop && !bIsBeingDragged) {
                    mInitialMotionY = mInitialDownY - mTouchSlop;
                    bIsBeingDragged = true;
                    bProgress.setAlpha(STARTING_PROGRESS_ALPHA);
                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                tIsBeingDragged = false;
                bIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
        }

        return tIsBeingDragged || bIsBeingDragged;
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
        if (index < 0) {
            return -1;
        }
        return MotionEventCompat.getY(ev, index);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // Nope.
    }

    private boolean isAnimationRunning(Animation animation) {
        return animation != null && animation.hasStarted() && !animation.hasEnded();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        /*if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }*/

        final boolean isTop = !canChildScrollUp();
        final boolean isBottom = !canChildScrollDown();

        if (!isEnabled() || mReturningToStart || (!isTop && !isBottom)) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                tIsBeingDragged = false;
                bIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    android.util.Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
                if (bIsBeingDragged) {
                    bProgress.showArrow(true);
                    float originalDragPercent = overscrollTop / mTotalDragDistance;
                    if (originalDragPercent > 0) {
                        return false;
                    }
                    float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
                    float adjustedPercent = (float) Math.max(dragPercent - .4, 0) * 5 / 3;
                    float extraOS = Math.abs(overscrollTop) - mTotalDragDistance;
                    float slingshotDist = mUsingCustomStart ? bOriginalOffsetTop + getMeasuredHeight() - bOriginalOffsetTop : mSpinnerFinalOffset;
                    float tensionSlingshotPercent = Math.max(0,
                            Math.min(extraOS, slingshotDist * 2) / slingshotDist);
                    float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow(
                            (tensionSlingshotPercent / 4), 2)) * 2f;
                    float extraMove = (slingshotDist) * tensionPercent * 2;

                    int targetY = bOriginalOffsetTop - (int) ((slingshotDist * dragPercent) + extraMove);
                    // where navi.0f is a full circle
                    if (bCircleView.getVisibility() != View.VISIBLE) {
                        bCircleView.setVisibility(View.VISIBLE);
                    }
                    if (!mScale) {
                        ViewCompat.setScaleX(bCircleView, 1f);
                        ViewCompat.setScaleY(bCircleView, 1f);
                    }
                    if (overscrollTop < mTotalDragDistance) {
                        if (mScale) {
                            setAnimationProgress(bCircleView, overscrollTop / mTotalDragDistance);
                        }
                        if (bProgress.getAlpha() > STARTING_PROGRESS_ALPHA
                                && !isAnimationRunning(mAlphaStartAnimation)) {
                            // Animate the alpha
                            startProgressAlphaStartAnimation(bProgress);
                        }
                        float strokeStart = adjustedPercent * .8f;
                        bProgress.setStartEndTrim(0f, Math.min(MAX_PROGRESS_ANGLE, strokeStart));
                        bProgress.setArrowScale(Math.min(1f, adjustedPercent));
                    } else {
                        if (bProgress.getAlpha() < MAX_ALPHA
                                && !isAnimationRunning(mAlphaMaxAnimation)) {
                            // Animate the alpha
                            startProgressAlphaMaxAnimation(bProgress);
                        }
                    }
                    float rotation = (-0.25f + .4f * adjustedPercent + tensionPercent * 2) * .5f;
                    bProgress.setProgressRotation(rotation);
                    setTargetOffsetTopAndBottom(bCircleView, targetY - bCurrentTargetOffsetTop, true);
                }
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mActivePointerId == INVALID_POINTER) {
                    if (action == MotionEvent.ACTION_UP) {
                        android.util.Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id.");
                    }
                    return false;
                }
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
                Log.e(TAG, "onTouchEvent>>overscrollTop=" + overscrollTop + ",mTotalDragDistance=" + mTotalDragDistance);
                if (Math.abs(overscrollTop) > mTotalDragDistance) {
                    setRefreshing(bCircleView, true, true /* notify */);
                } else {
                    // cancel refresh
//                    tRefreshing = false;
//                    tProgress.setStartEndTrim(0f, 0f);
                    bRefreshing = false;
                    bProgress.setStartEndTrim(0f, 0f);
                    Animation.AnimationListener listener = null;
                    if (!mScale) {
                        listener = new Animation.AnimationListener() {

                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                if (!mScale) {
                                    startScaleDownAnimation(bCircleView, null);
                                }
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }

                        };
                    }
                    animateOffsetToStartPosition( bCircleView, bCurrentTargetOffsetTop, listener);
                  //  tProgress.showArrow(false);
                    bProgress.showArrow(false);
                }
                tIsBeingDragged = false;
                bIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                return false;
            }
        }

        return true;
    }

    private void animateOffsetToCorrectPosition(CircleImageView view, int from, Animation.AnimationListener listener) {
        mFrom = from;
        mAnimateToCorrectPosition.reset();
        mAnimateToCorrectPosition.setDuration(ANIMATE_TO_TRIGGER_DURATION);
        mAnimateToCorrectPosition.setInterpolator(mDecelerateInterpolator);
        if (listener != null) {
            view.setAnimationListener(listener);
        }
        view.clearAnimation();
        view.startAnimation(mAnimateToCorrectPosition);
    }

    private void animateOffsetToStartPosition(CircleImageView view, int from, Animation.AnimationListener listener) {
        if (mScale) {
            // Scale the item back down
            startScaleDownReturnToStartAnimation(view, from, listener);
        } else {
            mFrom = from;
            mAnimateToStartPosition.reset();
            mAnimateToStartPosition.setDuration(ANIMATE_TO_START_DURATION);
            mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
            if (listener != null) {
                view.setAnimationListener(listener);
            }
            view.clearAnimation();
            view.startAnimation(mAnimateToStartPosition);
        }
    }

    private final Animation mAnimateToCorrectPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop = 0;
            int endTarget = 0;
           if (bRefreshing) {
                endTarget = bOriginalOffsetTop - (int) mSpinnerFinalOffset;
                targetTop = (mFrom + (int) ((endTarget - mFrom) * interpolatedTime));
                int offset = targetTop - bCircleView.getTop();
                setTargetOffsetTopAndBottom(bCircleView, offset, false /* requires update */);
                bProgress.setArrowScale(1 - interpolatedTime);
            }
        }
    };

    private void moveToStart(float interpolatedTime) {
        int targetTop = 0;
            targetTop = (mFrom + (int) ((bOriginalOffsetTop - mFrom) * interpolatedTime));
            int offset = targetTop - bCircleView.getTop();
            setTargetOffsetTopAndBottom(bCircleView, offset, false /* requires update */);

    }

    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveToStart(interpolatedTime);
        }
    };

    private void startScaleDownReturnToStartAnimation(final CircleImageView view, int from,
                                                      Animation.AnimationListener listener) {
        mFrom = from;
        if (isAlphaUsedForScale()) {
            mStartingScale = bProgress.getAlpha();
        } else {
            mStartingScale = ViewCompat.getScaleX(view);
        }
        mScaleDownToStartAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                float targetScale = (mStartingScale + (-mStartingScale * interpolatedTime));
                setAnimationProgress(view, targetScale);
                moveToStart(interpolatedTime);
            }
        };
        mScaleDownToStartAnimation.setDuration(SCALE_DOWN_DURATION);
        if (listener != null) {
            view.setAnimationListener(listener);
        }
        view.clearAnimation();
        view.startAnimation(mScaleDownToStartAnimation);
    }

    private void setTargetOffsetTopAndBottom(View target, int offset, boolean requiresUpdate) {
        target.bringToFront();
        //Log.e(TAG, "setTargetOffsetTopAndBottom>>target.getTop="+target.getTop());
        target.offsetTopAndBottom(offset);
            // Log.e(TAG,"setTargetOffsetTopAndBottom>>bCircleView,offset="+offset+",top="+target.getTop());
            bCurrentTargetOffsetTop = target.getTop();
        if (requiresUpdate && android.os.Build.VERSION.SDK_INT < 11) {
            invalidate();
        }
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    /**
     * Classes that wish to be notified when the swipe gesture correctly
     * triggers a refresh should implement this interface.
     */
    public interface OnRefreshListener {
       // public void onDownPullRefresh();

        public void onUpPullRefresh();
    }
}
