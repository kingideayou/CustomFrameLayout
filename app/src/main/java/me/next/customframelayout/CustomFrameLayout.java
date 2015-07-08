package me.next.customframelayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

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

    private int mDuration;

    private boolean isFade;

    private boolean mBoundary;

    private int mChildCount;

    private long mPressStartTime;

    private float downY;
    private float firstDownX;
    private float firstDownY;

    private VelocityTracker mVelocityTracker;

    private static final int DEFAULT_TITLE_BAR_HEIGHT_DISPLAY = 20;
    private static final int DEFAULT_TITLE_BAR_HEIGHT_NO_DISPLAY = 60;
    private static final int DEFAULT_CARD_MARGIN_TOP = 0;
    private static final int DEFAULT_MOVE_DISTANCE_TO_TRIGGER = 30;
    private static final int DEFAULT_ANIM_DURATION = 250;

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
                isConsume = handleActonDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
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

            //
            int t = (int) (getMeasuredHeight() - (mChildCount - i) * mTitleBarHeightNoDisplay);
            childView.layout(0, t, childView.getMeasuredWidth(), childView.getMeasuredHeight() + t);

        }
    }

    private boolean handleActonDown(MotionEvent event) {
        boolean isConsume = false;
        mPressStartTime = System.currentTimeMillis();
        firstDownY = downY = event.getY();
        firstDownX = event.getX();

        int childCount = isExistBackground ? mChildCount - 1 : mChildCount;

        // 判断点击那个 childView

        return isConsume;
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
