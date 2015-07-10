package me.next.customframelayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by NeXT on 15/7/8.
 */
public class CustomFrameLayout extends FrameLayout {

    private Context context;

    private int mMaxVelocity;
    private int mMinVelocity;
    private int mTouchSlop;

    private float mDensity;

    private float mTitleBarHeightDisplay;
    private float mTitleBarHeightNoDisplay;

    private float mMarginTop;
    private float mMoveDistanceToTrigger;

    private int mBackGroundRid;
    private boolean isExistBackground;

    // 动画的 Duration
    private int mDuration;

    private boolean isFade;

    private boolean mBoundary;

    private int mChildCount;

    private long mPressStartTime;

    private float downY;
    private float firstDownX;
    private float firstDownY;

    private float yVelocity;
    private float xVelocity;

    private boolean isDisplaying;
    private boolean isDragging;
    private boolean isTouchOnCard;
    private boolean isAnimating;

    private float deltaY;

    private int mDisplayingCard = -1;

    private int whichCardOnTouch;
    private float mTouchingViewOrignY;

    private VelocityTracker mVelocityTracker;
    private OnDisplayOrHideListener onDisplayOrHideListener;

    private static final int DEFAULT_TITLE_BAR_HEIGHT_DISPLAY = 20;
    private static final int DEFAULT_TITLE_BAR_HEIGHT_NO_DISPLAY = 60;
    private static final int DEFAULT_CARD_MARGIN_TOP = 0;
    private static final int DEFAULT_MOVE_DISTANCE_TO_TRIGGER = 30;
    private static final int DEFAULT_ANIM_DURATION = 250;
    private static final int MAX_CLICK_TIME = 300;
    private static final int MAX_CLICK_DISTANCE = 5;

    private AccelerateInterpolator mCloseAnimatorInterpolator = new AccelerateInterpolator();
    private AccelerateInterpolator mOpenAnimatorInterpolator = new AccelerateInterpolator();

    public CustomFrameLayout(Context context) {
        this(context, null);
    }

    public CustomFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;

        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        mMaxVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        mMinVelocity = viewConfiguration.getScaledMinimumFlingVelocity() * 8;
        mTouchSlop = viewConfiguration.getScaledTouchSlop();

        //屏幕密度
        mDensity = context.getResources().getDisplayMetrics().density;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CardMenu, defStyleAttr, 0);

        mTitleBarHeightDisplay = a.getDimension(R.styleable.CardMenu_title_bar_height_display,
                dip2px(DEFAULT_TITLE_BAR_HEIGHT_DISPLAY));
        mTitleBarHeightNoDisplay = a.getDimension(R.styleable.CardMenu_title_bar_height_no_display,
                dip2px(DEFAULT_TITLE_BAR_HEIGHT_NO_DISPLAY));

        mMarginTop = a.getDimension(R.styleable.CardMenu_margin_top,
                dip2px(DEFAULT_CARD_MARGIN_TOP));
        mMoveDistanceToTrigger = a.getDimension(R.styleable.CardMenu_move_distance_to_trigger,
                dip2px(DEFAULT_MOVE_DISTANCE_TO_TRIGGER));

        mBackGroundRid = a.getResourceId(R.styleable.CardMenu_margin_top, -1);

        mDuration = a.getInt(R.styleable.CardMenu_animator_duration, DEFAULT_ANIM_DURATION);

        isFade = a.getBoolean(R.styleable.CardMenu_fade, true);

        mBoundary = a.getBoolean(R.styleable.CardMenu_boundary, false);

        a.recycle();

        initBackgroundView();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        initVelocityTracker(event);
        boolean isConsume = false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isConsume = handleActionDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                handleActionMove(event);
                break;
            case MotionEvent.ACTION_UP:
                handleActionUp(event);
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mChildCount = getChildCount();
        for (int i = 0; i < mChildCount; i++) {
            View childView = getChildAt(i);

            //如果子视图包含其它布局
            if (i == 0 && isExistBackground) {
                childView.layout(0, 0, childView.getMeasuredWidth(), childView.getMeasuredHeight());
                continue;
            }

            int t = (int) (getMeasuredHeight() - (mChildCount - i) * mTitleBarHeightNoDisplay);
            childView.layout(0, t, childView.getMeasuredWidth(), childView.getMeasuredHeight() + t);

        }
    }

    private void handleActionUp(MotionEvent event) {
        if (whichCardOnTouch == -1 || !isTouchOnCard) return;
        long pressDuration = System.currentTimeMillis() - mPressStartTime;
        computeVelocity();
        if (!isDisplaying && ((event.getY() - firstDownY < 0 && (Math.abs(event.getY() - firstDownY) > mMoveDistanceToTrigger))
                || (yVelocity < 0 && Math.abs(yVelocity) > mMinVelocity && Math.abs(yVelocity) > Math.abs(xVelocity)))) {
            displayCard(whichCardOnTouch);
        } else if (!isDisplaying && pressDuration > MAX_CLICK_TIME &&
                distance(firstDownX, firstDownY, event.getX(), event.getY()) < MAX_CLICK_DISTANCE) {
            displayCard(whichCardOnTouch);
        } else if (!isDisplaying && isDragging && (event.getY() - firstDownY) > 0 ||
                Math.abs(event.getY() - firstDownY) < mMoveDistanceToTrigger) {
            hideCard(whichCardOnTouch);
        } else if(isDisplaying) {
            float currentY = ViewHelper.getY(getChildAt(mDisplayingCard));
            if (currentY < mMarginTop || currentY < mMarginTop + mMoveDistanceToTrigger) {
                ObjectAnimator.ofFloat(getChildAt(mDisplayingCard), "y", currentY, mMarginTop)
                        .setDuration(mDuration).start();
            } else {
                hideCard(mDisplayingCard);
            }
        }
        isTouchOnCard = false;
        isDragging = false;
        deltaY = 0;
    }

    private void handleActionMove(MotionEvent event) {
        if (whichCardOnTouch == -1 || !isTouchOnCard) return;
        if (supportScrollInView((int)(firstDownY - event.getY()))) return;
        computeVelocity();
        if (Math.abs(xVelocity) > Math.abs(yVelocity)) return;
        if (!isDragging && Math.abs(event.getY() - firstDownY) > mTouchSlop
                && Math.abs(event.getX() - firstDownX) < mTouchSlop) {
            isDragging = true;
            downY = event.getY();
        }
        if (isDragging) {
            deltaY = event.getY() - downY;
            downY = event.getY();
            View touchingChildView = getChildAt(whichCardOnTouch);
            if (!mBoundary) {
                touchingChildView.offsetTopAndBottom((int)deltaY);
            } else { // Card 没有设置边界
                float touchingViewY = ViewHelper.getY(touchingChildView);
                if (touchingViewY + deltaY <= mMarginTop) {
                    // 向上移除屏幕
                    touchingChildView.offsetTopAndBottom((int)(mMarginTop - touchingViewY));
                } else if (touchingViewY + deltaY >= mTouchingViewOrignY) {
                    // 向下移除屏幕
                    touchingChildView.offsetTopAndBottom((int)(mTouchingViewOrignY - touchingViewY));
                } else {
                    touchingChildView.offsetTopAndBottom((int) deltaY);
                }
            }
        }
    }

    private void computeVelocity() {
        //units:  使用的速率单位.1的意思是，以一毫秒运动了多少个像素的速率， 1000表示 一秒时间内运动了多少个像素
        mVelocityTracker.computeCurrentVelocity(1000, mMaxVelocity);
        yVelocity = mVelocityTracker.getYVelocity();
        xVelocity = mVelocityTracker.getXVelocity();
    }

    private boolean handleActionDown(MotionEvent event) {
        boolean isConsume = false;
        mPressStartTime = System.currentTimeMillis();
        firstDownY = downY = event.getY();
        firstDownX = event.getX();

        int childCount = isExistBackground ? mChildCount - 1 : mChildCount;

        // 判断点击那个 childView
        if (!isDisplaying && downY > getMeasuredHeight() - mChildCount * mTitleBarHeightNoDisplay) {
            for (int i = 1; i <= mChildCount; i++) {
                if (downY < getMeasuredHeight() - mChildCount * mTitleBarHeightNoDisplay +
                        mTitleBarHeightNoDisplay * i) {
                    whichCardOnTouch = i - 1;
                    isTouchOnCard = true;
                    if (onDisplayOrHideListener != null) {
                        onDisplayOrHideListener.onTouchCard(whichCardOnTouch);
                    }
                    isConsume = true;
                    break;
                }
            }
            mTouchingViewOrignY = ViewHelper.getY(getChildAt(whichCardOnTouch));
        } else if (isDisplaying && downY > getMeasuredHeight() - (mChildCount - 1) * mTitleBarHeightDisplay) {
            hideCard(mDisplayingCard);
        } else if (isDisplaying && downY > mMarginTop && mDisplayingCard >= 0 &&
                downY < getChildAt(mDisplayingCard).getMeasuredHeight() + mMarginTop) {
            whichCardOnTouch = mDisplayingCard;
            isTouchOnCard = true;
        } else if (isDisplaying && (downY < mMarginTop
                || (mDisplayingCard >= 0) && (downY > mMarginTop + getChildAt(mDisplayingCard).getMeasuredHeight()))) {
            hideCard(mDisplayingCard);
        }

        if (isExistBackground && whichCardOnTouch == 0) {
            isTouchOnCard = false;
        }
        return isConsume;
    }


    private double distance(float x1, float y1, float x2, float y2) {
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    /**
     * @param direction SCv to check scrolling up, positive to check
     *                  scrolling down.
     * @return true if need dispatch touch event to child view,otherwise
     */
    private boolean supportScrollInView(int direction) {
        View view = getChildAt(whichCardOnTouch);
        if (view instanceof ViewGroup) {
            View childView = findTopChildUnder((ViewGroup)view, firstDownX, firstDownY);
            if (childView == null) return false;
            if (childView instanceof AbsListView) {
                AbsListView absListView = (AbsListView) childView;
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    return absListView.canScrollList(direction);
                } else {
                    return absListViewCanScrollList(absListView, direction);
                }
            } else if (childView instanceof ScrollView) {
                ScrollView scrollView = (ScrollView) childView;
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    return scrollView.canScrollVertically(direction);
                }else {
                    return scrollViewCanScrollVertically(scrollView, direction);
                }
            }
        }
        return false;
    }

    /**
     *  Copy From ScrollView (API Level >= 14)
     * @param direction Negative to check scrolling up, positive to check
     *                  scrolling down.
     *   @return true if the scrollView can be scrolled in the specified direction,
     *         false otherwise
     */
    private boolean scrollViewCanScrollVertically(ScrollView scrollView, int direction) {
        final int offset = Math.max(0, scrollView.getScrollY());
        final int range = computeVerticalScrollRange(scrollView) - scrollView.getHeight();
        if (range == 0) return false;
        if (direction < 0) { //scroll up
            return offset > 0;
        } else {//scroll down
            return offset < range - 1;
        }
    }

    /**
     * Copy From ScrollView (API Level >= 14)
     * <p>The scroll range of a scroll view is the overall height of all of its
     * children.</p>
     */
    private int computeVerticalScrollRange(ScrollView scrollView) {
        final int count = scrollView.getChildCount();
        final int contentHeight = scrollView.getHeight() - scrollView.getPaddingBottom() - scrollView.getPaddingTop();
        if (count == 0) {
            return contentHeight;
        }

        int scrollRange = scrollView.getChildAt(0).getBottom();
        final int scrollY = scrollView.getScrollY();
        final int overscrollBottom = Math.max(0, scrollRange - contentHeight);
        if (scrollY < 0) {
            scrollRange -= scrollY;
        } else if (scrollY > overscrollBottom) {
            scrollRange += scrollY - overscrollBottom;
        }

        return scrollRange;
    }

    /**
     * Copy From AbsListView (API Level >= 19)
     * @param absListView AbsListView
     * @param direction Negative to check scrolling up, positive to check
     *                  scrolling down.
     * @return true if the list can be scrolled in the specified direction,
     *         false otherwise
     */
    private boolean absListViewCanScrollList(AbsListView absListView, int direction) {
        final int childCount = absListView.getChildCount();
        if (childCount == 0) {
            return false;
        }
        final int firstPosition = absListView.getFirstVisiblePosition();
        if (direction > 0) { // can scroll down
            final int lastBottom = absListView.getChildAt(childCount - 1).getBottom();
            final int lastPosition = firstPosition + childCount;
            return lastPosition < absListView.getCount() ||
                    lastBottom > absListView.getHeight() - absListView.getPaddingTop();
        } else { // can scroll up
            final int firstTop = absListView.getChildAt(0).getTop();
            return  firstPosition > 0 || firstTop < absListView.getPaddingTop();
        }
    }

    private View findTopChildUnder(ViewGroup viewGroup, float x, float y) {
        final int childCount = viewGroup.getChildCount();
        for (int i = childCount - 1; i >= 0; i++) {
            View childView = viewGroup.getChildAt(i);
            if (x >= childView.getLeft() && x < childView.getRight() &&
                    y >= childView.getTop() && y < childView.getBottom()) {
                return childView;
            }
        }
        return null;
    }

    private void displayCard(int whichCardOnTouch) {
        if(isDisplaying || isAnimating)return;
        //TODO add darkFrameLayout
//        if(isFade && mDarkFrameLayout != null) mDarkFrameLayout.fade(true);
        List<Animator> animators = new ArrayList<>(mChildCount);
        final float distance = ViewHelper.getY(getChildAt(whichCardOnTouch)) - mMarginTop;
        ValueAnimator displayAnimator = ValueAnimator.ofFloat(ViewHelper.getY(getChildAt(whichCardOnTouch)), mMarginTop)
                .setDuration(mDuration);
        displayAnimator.setTarget(getChildAt(whichCardOnTouch));
        final View displayingView = getChildAt(whichCardOnTouch);
        displayAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float value = (float) valueAnimator.getAnimatedValue();
                ViewHelper.setY(displayingView, value);
                //TODO addDarkFrameLayout
                /*
                if(mDarkFrameLayout != null && isFade) {
                    mDarkFrameLayout.fade((int) ((1-(value - mMarginTop)/distance) * DarkFrameLayout.MAX_ALPHA));
                }
                */
            }
        });
        animators.add(displayAnimator);
        int n = isExistBackground ? (mChildCount - 1) : mChildCount;
        for(int i = 0,j = 1; i < mChildCount; i++) {
            if(i == 0 && isExistBackground) continue;
            if(i != whichCardOnTouch){
                animators.add(ObjectAnimator
                        .ofFloat(getChildAt(i), "y", ViewHelper.getY(getChildAt(i)),
                                getMeasuredHeight() - mTitleBarHeightDisplay * (n - j))
                        .setDuration(mDuration));
                j ++;
            }
        }
        AnimatorSet set = new AnimatorSet();
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                isAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                isAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                isAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        set.setInterpolator(mOpenAnimatorInterpolator);
        set.playTogether(animators);
        set.start();
        isDisplaying = true;
        mDisplayingCard = whichCardOnTouch;
        if(onDisplayOrHideListener != null)
            onDisplayOrHideListener.onDisplay(isExistBackground ? (whichCardOnTouch - 1) : whichCardOnTouch);
    }

    private void hideCard(int mDisplayingCard) {
        if(isAnimating) return;
        List<Animator> animators = new ArrayList<>(mChildCount);
        final View displayingCard = getChildAt(mDisplayingCard);
        int t = (int) (getMeasuredHeight() - (mChildCount - mDisplayingCard) * mTitleBarHeightNoDisplay);
        ValueAnimator displayAnimator = ValueAnimator.ofFloat(ViewHelper.getY(displayingCard), t).setDuration(mDuration);
        displayAnimator.setTarget(displayingCard);
        final int finalT = t;
        displayAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                ViewHelper.setY(displayingCard, value);
                //TODO add darkFrameLayout
                /*
                if(mDarkFrameLayout != null && isFade && value < finalT) {
                    mDarkFrameLayout.fade((int) ((1 - value/ finalT) * DarkFrameLayout.MAX_ALPHA));
                }
                */
            }
        });
        animators.add(displayAnimator);
        for (int i = 0; i < mChildCount; i++) {
            if (i == 0 && isExistBackground) continue;
            if (i != mDisplayingCard) {
                t = (int)(getMeasuredHeight() - (mChildCount - i) * mTitleBarHeightNoDisplay);
                animators.add(ObjectAnimator.ofFloat(getChildAt(i), "y", ViewHelper.getY(getChildAt(i)), t));
            }
        }
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                isAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimating = false;
                isDisplaying = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isAnimating = false;
                isDisplaying = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animatorSet.setInterpolator(mCloseAnimatorInterpolator);
        animatorSet.playTogether(animators);
        animatorSet.start();
        mDisplayingCard = -1;
        if(onDisplayOrHideListener != null)
            onDisplayOrHideListener.onHide(isExistBackground ? (mDisplayingCard - 1) : mDisplayingCard);
    }

    private void initVelocityTracker(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
    }

    private void initBackgroundView() {
        if (mBackGroundRid != -1) {
            isExistBackground = true;
        }
    }

    public void setOnDisplayOrHideListener(OnDisplayOrHideListener onDisplayOrHideListener) {
        this.onDisplayOrHideListener = onDisplayOrHideListener;
    }

    public interface OnDisplayOrHideListener{
        public void onDisplay(int whichCard);
        public void onHide(int whichCard);
        public void onTouchCard(int whichCard);
    }

    /**
     * @param pxVal
     * @return
     */
    private int px2dip(float pxVal) {
        return (int)(pxVal/mDensity + 0.5f);
    }

    /**
     * @param dip
     * @return
     */
    private int dip2px(float dip) {
        return (int)(dip * mDensity + 0.5f);
    }

}
