package me.next.customframelayout;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * Created by NeXT on 15-7-21.
 */
public class DarkFrameLayout extends FrameLayout{

    public final static int MAX_ALPHA = 0x9f;

    private Paint mFadePaint;
    private int alpha = 0x00;

    private CustomFrameLayout customFrameLayout;

    public DarkFrameLayout(Context context) {
        this(context, null);
    }

    public DarkFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DarkFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mFadePaint = new Paint();
    }

    //onInterceptTouchEvent默认返回值是false，这样touch事件会传递到View控件，
    // ViewGroup里的onTouchEvent默认返回值是false；
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return customFrameLayout.isDisplaying();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        drawFade(canvas);
    }

    private void drawFade(Canvas canvas) {
        mFadePaint.setColor(Color.argb(alpha, 0, 0, 0));
        canvas.drawRect(0, 0, getMeasuredWidth(), getHeight(), mFadePaint);
    }

    public void fade(boolean fade) {
        this.alpha = fade ? 0x8f : 0x00;
        invalidate();
    }

    public void fade(int alpha) {
        this.alpha = alpha;
        invalidate();
    }

    public void setCustomFrameLayout(CustomFrameLayout customFrameLayout) {
        this.customFrameLayout = customFrameLayout;
    }
}
