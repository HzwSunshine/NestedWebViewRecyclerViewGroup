package com.hzw.nested;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.NestedScrollingChild2;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.webkit.WebView;
import android.widget.Scroller;

/**
 * author: hzw
 * time: 2019/2/25 下午2:08
 * description: {@link NestedWebViewRecyclerViewGroup}的子View，用于协作和RecyclerView的联合滑动
 */
public class NestedScrollWebView extends WebView implements NestedScrollingChild2 {

    private final int[] mScrollConsumed = new int[2];
    private NestedWebViewRecyclerViewGroup parent;
    private NestedScrollingChildHelper childHelper;
    private VelocityTracker velocityTracker;
    private Scroller scroller;
    private boolean isSelfFling;
    private boolean hasFling;
    private final float density;
    private int mWebViewContentHeight;
    private int mMaximumVelocity;
    private int maxScrollY;
    private int TouchSlop;
    private int firstY;
    private int lastY;

    public NestedScrollWebView(Context context) {
        this(context, null);
    }

    public NestedScrollWebView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NestedScrollWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        childHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
        scroller = new Scroller(getContext());
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        density = getResources().getDisplayMetrics().density;
        //TouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        TouchSlop = Util.dip2px(3);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mWebViewContentHeight = 0;
                lastY = (int) event.getRawY();
                firstY = lastY;
                if (!scroller.isFinished()) {
                    scroller.abortAnimation();
                }
                initOrResetVelocityTracker();
                isSelfFling = false;
                hasFling = false;
                maxScrollY = getWebViewContentHeight() - getHeight();
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                break;
            case MotionEvent.ACTION_MOVE:
                initVelocityTrackerIfNotExists();
                velocityTracker.addMovement(event);
                int y = (int) (event.getRawY());
                int dy = y - lastY;
                lastY = y;
                if (!dispatchNestedPreScroll(0, -dy, mScrollConsumed, null)) {
                    scrollBy(0, -dy);
                }
                if (Math.abs(firstY - y) > TouchSlop) {
                    //屏蔽WebView本身的滑动，滑动事件自己处理
                    event.setAction(MotionEvent.ACTION_CANCEL);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (isParentResetScroll() && velocityTracker != null) {
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int yVelocity = (int) -velocityTracker.getYVelocity();
                    recycleVelocityTracker();
                    isSelfFling = true;
                    flingScroll(0, yVelocity);
                }
                break;
        }
        super.onTouchEvent(event);
        return true;
    }

    @Override
    public void flingScroll(int vx, int vy) {
        int startY = getWebViewContentHeight() - getHeight();
        if (getScrollY() < startY) {
            startY = getScrollY();
        }
        scroller.fling(0, startY, 0, vy, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        scroller.computeScrollOffset();
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        recycleVelocityTracker();
        stopScroll();
        childHelper = null;
        scroller = null;
        parent = null;
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            final int currY = scroller.getCurrY();
            if (!isSelfFling) {//parent fling
                scrollTo(0, currY);
                invalidate();
                return;
            }
            if (isWebViewCanScroll()) {
                scrollTo(0, currY);
                invalidate();
            }
            if (!hasFling && scroller.getStartY() < currY
                    && !canWebViewScrollDown()
                    && startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
                    && !dispatchNestedPreFling(0, scroller.getCurrVelocity())) {
                //滑动到底部时，将fling传递给父控件和RecyclerView
                hasFling = true;
                dispatchNestedFling(0, scroller.getCurrVelocity(), false);
            }
        }
    }

    @Override
    public void scrollTo(int x, int y) {
        if (isParentResetScroll()) {
            if (y < 0) {
                y = 0;
            }
            if (maxScrollY != 0 && y > maxScrollY) {
                y = maxScrollY;
            }
            super.scrollTo(x, y);

            //用于父控件不是嵌套控件时，绘制进度条，仅此而已
            if (!(getParent() instanceof NestedWebViewRecyclerViewGroup)) {
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                dispatchNestedScroll(1, 0, 0, 0, null);
            }
        }
    }

    void stopScroll() {
        if (scroller != null && !scroller.isFinished()) {
            scroller.abortAnimation();
        }
    }

    void scrollToBottom() {
        int y = computeVerticalScrollRange();
        super.scrollTo(0, y - getHeight());
    }

    private void initWebViewParent() {
        if (this.parent != null) {
            return;
        }
        View parent = (View) getParent();
        while (parent != null) {
            if (parent instanceof NestedWebViewRecyclerViewGroup) {
                this.parent = (NestedWebViewRecyclerViewGroup) parent;
                break;
            } else {
                parent = (View) parent.getParent();
            }
        }
    }

    private boolean isParentResetScroll() {
        if (parent == null) {
            initWebViewParent();
        }
        if (parent != null) {
            return parent.getScrollY() == 0;
        }
        return true;
    }

    private void initOrResetVelocityTracker() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        } else {
            velocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    private boolean canWebViewScrollDown() {
        final int offset = getScrollY();
        final int range = getWebViewContentHeight() - getHeight();
        if (range == 0) {
            return false;
        }
        return offset < range - 3;
    }

    private boolean isWebViewCanScroll() {
        final int offset = getScrollY();
        final int range = getWebViewContentHeight() - getHeight();
        if (range == 0) {
            return false;
        }
        return offset > 0 || offset < range - 3;
    }

    private int getWebViewContentHeight() {
        if (mWebViewContentHeight == 0) {
            mWebViewContentHeight = (int) (getContentHeight() * density);
        }
        return mWebViewContentHeight;
    }

    private NestedScrollingChildHelper getHelper() {
        if (childHelper == null) {
            childHelper = new NestedScrollingChildHelper(this);
        }
        return childHelper;
    }

    @Override
    public boolean startNestedScroll(int axes, int type) {
        return getHelper().startNestedScroll(axes, type);
    }

    @Override
    public void stopNestedScroll(int type) {
        getHelper().stopNestedScroll(type);
    }

    @Override
    public boolean hasNestedScrollingParent(int type) {
        return getHelper().hasNestedScrollingParent(type);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, @Nullable int[] offsetInWindow, int type) {
        return getHelper().dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow, type);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed, @Nullable int[] offsetInWindow, int type) {
        return getHelper().dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        getHelper().setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return getHelper().isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return getHelper().startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        getHelper().stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return getHelper().hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,
                                        @Nullable int[] offsetInWindow) {
        return getHelper().dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed, @Nullable int[] offsetInWindow) {
        return getHelper().dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return getHelper().dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return getHelper().dispatchNestedPreFling(velocityX, velocityY);
    }
}
