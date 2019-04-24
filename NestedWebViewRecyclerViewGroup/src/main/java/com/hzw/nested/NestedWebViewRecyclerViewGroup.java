package com.hzw.nested;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.view.NestedScrollingParent2;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * author: hzw
 * time: 2019/2/25 下午2:08
 * description: {@link NestedScrollWebView}和RecyclerView的嵌套类
 */
public class NestedWebViewRecyclerViewGroup extends ViewGroup implements NestedScrollingParent2 {

    //WebView向RecyclerView滑动
    private static final int SCROLL_WEB_PARENT = 0;
    //父控件向WebView滑动，位于父控件的dispatchTouchEvent事件中
    private static final int SCROLL_PARENT_WEB = 1;
    //RecyclerView向父控件滑动，位于RecyclerView的fling事件中
    private static final int SCROLL_RV_PARENT = 2;
    //上下切换
    private static final int SCROLL_SWITCH = 3;

    private NestedScrollingParentHelper helper;
    private VelocityTracker velocityTracker;
    private NestedScrollWebView webView;
    private onScrollListener listener;
    private RecyclerView recyclerView;
    private ScrollBarView scrollbar;
    private Scroller scroller;
    private Runnable runnable;

    private final int changeDuration;
    private int webViewContentHeight;
    private int currentScrollType;
    private int rvContentHeight;
    private int maximumVelocity;
    private final float density;
    //WebView的初始高度
    private int initTopHeight;
    private int topHeight;

    //是否在上下切换滑动中...
    private boolean isSwitching;
    private boolean hasFling;
    private boolean isRvFlingDown;


    public NestedWebViewRecyclerViewGroup(Context context) {
        this(context, null);
    }

    public NestedWebViewRecyclerViewGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NestedWebViewRecyclerViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
        helper = new NestedScrollingParentHelper(this);
        scroller = new Scroller(getContext());
        density = getResources().getDisplayMetrics().density;
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        maximumVelocity = configuration.getScaledMaximumFlingVelocity();
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.NestedWebViewRecyclerViewGroup, defStyleAttr, 0);
        changeDuration = array.getInteger(R.styleable.NestedWebViewRecyclerViewGroup_changeDuration, 300);
        boolean scrollbarEnable = array.getBoolean(R.styleable.NestedWebViewRecyclerViewGroup_scrollbarEnable, true);
        if (scrollbarEnable) {
            scrollbar = new ScrollBarView(getContext());
            int color = array.getColor(R.styleable.NestedWebViewRecyclerViewGroup_scrollbarColor, Color.LTGRAY);
            float space = array.getDimension(R.styleable.NestedWebViewRecyclerViewGroup_scrollbarMarginRight, Util.dip2px(3));
            float barWidth = array.getDimension(R.styleable.NestedWebViewRecyclerViewGroup_scrollbarWidth, Util.dip2px(4));
            float barMinHeight = array.getDimension(R.styleable.NestedWebViewRecyclerViewGroup_scrollbarMinHeight, Util.dip2px(30));
            scrollbar.init(color, (int) space, (int) barWidth, (int) barMinHeight);
        }
        array.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getChildCount() < 2 || getChildCount() > 3) {
            if (BuildConfig.DEBUG) {
                throw new IllegalStateException("Please check child layout, " +
                        "child must be NestedScrollWebView and RecyclerView");
            }
            return;
        }
        int width;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
        //int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int measureHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode == MeasureSpec.EXACTLY) {
            width = measureWidth;
        } else {
            width = getContext().getResources().getDisplayMetrics().widthPixels;
        }
        int left = getPaddingLeft();
        int right = getPaddingRight();
        int count = getChildCount();
        int parentSpace = measureHeight - getPaddingTop() - getPaddingBottom();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            //将两个子View的高度设置成父控件高度
            LayoutParams params = child.getLayoutParams();
            int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, left + right, params.width);
            //设置子view的高度为父控件的高度
            int childHeightMeasureSpec;
            if (child instanceof ScrollBarView) {
                //scrollBar的测量高度为整个父控件的初始高度
                childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(parentSpace, MeasureSpec.EXACTLY);
                params.height = parentSpace;
            } else if (child instanceof NestedScrollWebView && topHeight < initTopHeight) {
                //WebView重新设置的高度不满一屏
                childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(topHeight, MeasureSpec.EXACTLY);
                params.height = topHeight;
            } else {
                childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(parentSpace, MeasureSpec.EXACTLY);
                params.height = parentSpace;
            }
            measureChild(child, childWidthMeasureSpec, childHeightMeasureSpec);
        }
        setMeasuredDimension(width, measureHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childTotalHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            if (child instanceof ScrollBarView) {
                bringChildToFront(child);
                int contentHeight = webView.getMeasuredHeight() + getMeasuredHeight();
                child.layout(getMeasuredWidth() - childWidth, 0, childHeight, contentHeight);
            } else {
                child.layout(0, childTotalHeight, childWidth, childTotalHeight + childHeight);
            }
            childTotalHeight += childHeight;
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() < 2) return;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child instanceof NestedScrollWebView) {
                webView = (NestedScrollWebView) child;
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (webView != null) {
                            topHeight = webView.getHeight();
                            initTopHeight = topHeight;
                        }
                        //找不到RecyclerView时，会在界面显示时再次尝试重新获取
                        findRecyclerView(NestedWebViewRecyclerViewGroup.this);
                    }
                };
                post(runnable);
            } else if (child instanceof RecyclerView) {
                recyclerView = (RecyclerView) child;
            } else if (child instanceof ViewGroup) {
                findRecyclerView((ViewGroup) child);
            }
        }
        if (scrollbar != null && scrollbar.getParent() == null) {
            addView(scrollbar);
        }
    }

    private void findRecyclerView(ViewGroup parent) {
        if (recyclerView != null) return;
        int count = parent.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = parent.getChildAt(i);
            if (child instanceof RecyclerView) {
                recyclerView = (RecyclerView) child;
                break;
            }
            if (child instanceof ViewGroup) {
                findRecyclerView((ViewGroup) child);
            }
        }
    }

    /**
     * 特殊的布局中，如果无法获取到RecyclerView，请手动设置
     */
    public void setRecyclerView(RecyclerView recyclerView) {
        if (this.recyclerView != null) {
            return;
        }
        this.recyclerView = recyclerView;
    }

    /**
     * 只用于处理父控件的fling事件，其他的事件不处理
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (isSwitching) {
            return true;
        }
        int pointCount = event.getPointerCount();
        if (pointCount > 1) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                webViewContentHeight = 0;
                rvContentHeight = 0;
                hasFling = false;
                isRvFlingDown = false;
                initOrResetVelocityTracker();
                resetScroller();
                dealWithError();
                break;
            case MotionEvent.ACTION_MOVE:
                initVelocityTrackerIfNotExists();
                velocityTracker.addMovement(event);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (isParentCenter() && velocityTracker != null) {
                    //处理连接处的父控件fling事件
                    velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
                    int yVelocity = (int) -velocityTracker.getYVelocity();
                    currentScrollType = yVelocity > 0 ? SCROLL_WEB_PARENT : SCROLL_PARENT_WEB;
                    recycleVelocityTracker();
                    parentFling(yVelocity);
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    private void resetScroller() {
        if (!scroller.isFinished()) {
            scroller.abortAnimation();
        }
        if (recyclerView != null) {
            recyclerView.stopScroll();
        }
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

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (scroller != null) {
            scroller.abortAnimation();
            scroller = null;
        }
        removeCallbacks(runnable);
        velocityTracker = null;
        recyclerView = null;
        scrollbar = null;
        listener = null;
        webView = null;
        helper = null;
    }

    private void parentFling(float velocityY) {
        scroller.fling(0, getScrollY(), 0, (int) velocityY, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        scroller.computeScrollOffset();
        invalidate();
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
        return (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    /**
     * 当onStartNestedScroll返回true时，该方法被回调
     */
    @Override
    public void onNestedScrollAccepted(@NonNull View view, @NonNull View view1, int axes, int type) {
        helper.onNestedScrollAccepted(view, view1, axes, type);
    }

    @Override
    public int getNestedScrollAxes() {
        return helper.getNestedScrollAxes();
    }

    @Override
    public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
        if (target instanceof NestedScrollWebView) {
            //WebView滑到底部时，继续向下滑动父控件和RV
            currentScrollType = SCROLL_WEB_PARENT;
            parentFling(velocityY);
        } else if (target instanceof RecyclerView && velocityY < 0 && getScrollY() == topHeight) {
            //RV滑动到顶部时，继续向上滑动父控件和WebView，这里用于计算到达父控件的顶部时RV的速度
            currentScrollType = SCROLL_RV_PARENT;
            parentFling((int) velocityY);
        } else if (target instanceof RecyclerView && velocityY > 0) {
            isRvFlingDown = true;
        }
        if (Util.isAboveL()) {
            return super.onNestedPreFling(target, velocityX, velocityY);
        } else {
            return false;
        }
    }

    @Override
    public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY, boolean consumed) {
        if (Util.isAboveL()) {
            return super.onNestedFling(target, velocityX, velocityY, consumed);
        } else {
            return false;
        }
    }

    /**
     * 在子View消费滑动事件前，优先响应滑动操作，消费部分或全部滑动距离
     */
    @Override
    public void onNestedPreScroll(@NonNull View view, int dx, int dy, @NonNull int[] ints, int type) {
        boolean isWebViewBottom = !canWebViewScrollDown();
        boolean isCenter = isParentCenter();
        if (dy > 0 && getScrollY() < topHeight && isWebViewBottom) {
            //为了WebView滑动到底部，继续向下滑动父控件
            scrollBy(0, dy);
            ints[1] = dy;
        }
        if (dy < 0 && isCenter) {
            //为了RecyclerView滑动到顶部时，继续向上滑动父控件
            scrollBy(0, dy);
            ints[1] = dy;
        }
        if (isCenter && !isWebViewBottom) {
            //异常情况的处理
            scrollToWebViewBottom();
        }
        checkRvTop();
    }

    /**
     * 接收子View处理完滑动后的滑动距离信息, 在这里父控件可以选择是否处理剩余的滑动距离
     */
    @Override
    public void onNestedScroll(@NonNull View view, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed, int type) {
        if (getScrollY() != 0 || getScrollY() != topHeight) {
            scrollBy(0, dyUnconsumed);
        }
        if (getScrollY() == topHeight) {
            //用于绘制进度条
            invalidate();
        }
    }

    private boolean isParentCenter() {
        return getScrollY() > 0 && getScrollY() < topHeight;
    }

    @Override
    public void onStopNestedScroll(@NonNull View view, int i) {
        helper.onStopNestedScroll(view);
    }

    @Override
    public void scrollTo(int x, int y) {
        if (y < 0) {
            y = 0;
        }
        if (y > topHeight) {
            y = topHeight;
        }
        super.scrollTo(x, y);
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            int currY = scroller.getCurrY();
            switch (currentScrollType) {
                case SCROLL_SWITCH://上下切换
                    scrollTo(0, currY);
                    invalidate();
                    isSwitching = !scroller.isFinished();
                    break;
                case SCROLL_WEB_PARENT://WebView向父控件滑动
                    if (isRvFlingDown) {
                        //RecyclerView的区域的fling由自己完成
                        break;
                    }
                    scrollTo(0, currY);
                    invalidate();
                    checkRvTop();
                    if (getScrollY() == topHeight && !hasFling) {
                        //滚动到父控件底部，滚动事件交给RecyclerView
                        hasFling = true;
                        recyclerView.fling(0, (int) scroller.getCurrVelocity());
                    }
                    break;
                case SCROLL_PARENT_WEB://父控件向WebView滑动
                    scrollTo(0, currY);
                    invalidate();
                    if (currY <= 0 && !hasFling) {
                        //滚动父控件顶部，滚动事件交给WebView
                        hasFling = true;
                        webViewFling((int) -scroller.getCurrVelocity());
                    }
                    break;
                case SCROLL_RV_PARENT://RecyclerView向父控件滑动，fling事件，单纯用于计算速度
                    if (getScrollY() != 0) {
                        invalidate();
                    } else if (!hasFling) {
                        hasFling = true;
                        //滑动到顶部时，滚动事件交给WebView
                        webViewFling((int) -scroller.getCurrVelocity());
                    }
                    break;
            }
        }
    }

    private boolean canWebViewScrollDown() {
        final int offset = webView.getScrollY();
        final int range = getWebViewContentHeight() - webView.getHeight();
        if (range == 0) {
            return false;
        }
        return offset < range - 3;
    }

    private void scrollToWebViewBottom() {
        if (webView != null) {
            webView.scrollToBottom();
        }
    }

    private void webViewFling(int v) {
        webView.flingScroll(0, v);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (scrollbar == null && listener == null) return;
        int webViewContentHeight = getWebViewContentHeight();
        if (webView == null || recyclerView == null || webViewContentHeight == 0) return;
        int totalHeight = webViewContentHeight + getRVContentHeight();
        int scrollY = getCurrentScrollY();
        if (scrollbar != null) {
            scrollbar.setScrollBar(totalHeight, this.getScrollY(), scrollY);
        }
        if (listener != null) {
            listener.scroll(scrollY);
        }
    }

    private int getWebViewContentHeight() {
        if (webViewContentHeight == 0) {
            webViewContentHeight = (int) (webView.getContentHeight() * density);
        }
        return webViewContentHeight;
    }

    private int getRVContentHeight() {
        if (rvContentHeight == 0 || getScrollY() == topHeight) {
            //在RecyclerView区域时实时获取内容高度
            rvContentHeight = recyclerView.computeVerticalScrollRange();
        }
        return rvContentHeight;
    }

    private int getRVScrollY() {
        if (recyclerView == null) return 0;
        //RecyclerView滑动时再计算滑动距离
        if (getScrollY() != topHeight) return 0;
        return recyclerView.computeVerticalScrollOffset();
    }

    public void setOnScrollListener(onScrollListener listener) {
        this.listener = listener;
    }

    /**
     * 获取当前的滑动距离
     */
    public int getCurrentScrollY() {
        return getScrollY() + getWebViewScrollY() + getRVScrollY();
    }

    /**
     * 获取当前WebView的滑动距离
     */
    public int getWebViewScrollY() {
        if (getScrollY() == topHeight) {
            return getWebViewContentHeight() - webView.getHeight();
        }
        return webView.getScrollY();
    }

    /**
     * 如果WebView的内容存在不满一屏的情况，请手动设置WebView的内容高度
     * WebView的内容高度可让前端同学通过js传递给你
     */
    public void setWebViewContentHeight(int contentHeight) {
        if (contentHeight > 0) {
            int initHeight = getMeasuredHeight();
            if (contentHeight < initHeight) {
                //情况1：内容不满一屏，情况3：再次设置高度不满一屏
                topHeight = contentHeight;
                requestLayout();
            } else if ((topHeight != initHeight && contentHeight > initHeight)) {
                //情况2：之前有过情况1但是再设置的高度却大于一屏
                topHeight = initHeight;
                requestLayout();
            }
        }
    }

    /**
     * 滚动到某个位置
     */
    public void scrollToPosition(int y) {
        int webViewContentHeight = getWebViewContentHeight();
        if (webViewContentHeight == 0) return;
        int webH = webViewContentHeight - topHeight;
        if (y <= webH) {
            webView.scrollTo(0, y);
        } else if (y <= webH + topHeight) {
            scrollToWebViewBottom();
            scrollTo(0, y - webH);
        } else {
            scrollToWebViewBottom();
            scrollTo(0, topHeight);
        }
    }

    /**
     * WebView和RecyclerView的上下切换
     *
     * @param rvPosition 切换到RecyclerView时需要定位到的位置
     */
    public void switchView(int rvPosition) {
        if (isSwitching) {
            return;
        }
        resetScroller();
        if (webView != null) {
            webView.stopScroll();
        }
        currentScrollType = SCROLL_SWITCH;
        isSwitching = true;
        if (getScrollY() == 0) {//滑向Bottom
            rvScrollToPosition(rvPosition);
            scroller.startScroll(0, 0, 0, topHeight, changeDuration);
        } else if (getScrollY() == topHeight) {//滑向Top
            if (topHeight < getHeight()) {
                rvScrollToPosition(0);
            }
            scroller.startScroll(0, topHeight, 0, -topHeight, changeDuration);
        } else {//在交界处优先滑向Top
            if (topHeight < getHeight()) {
                rvScrollToPosition(0);
            }
            scroller.startScroll(0, getScrollY(), 0, -getScrollY(), changeDuration);
        }
        scroller.computeScrollOffset();
        invalidate();
    }

    private boolean isRvTop() {
        if (recyclerView == null) return false;
        return !recyclerView.canScrollVertically(-1);
    }

    private void rvScrollToPosition(int position) {
        if (recyclerView == null) return;
        recyclerView.scrollToPosition(position);
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if (manager instanceof LinearLayoutManager) {
            ((LinearLayoutManager) manager).scrollToPositionWithOffset(position, 0);
        }
    }

    private void checkRvTop() {
        if (isParentCenter() && !isRvTop()) {
            rvScrollToPosition(0);
        }
    }

    /**
     * 处理未知的错误情况
     */
    private void dealWithError() {
        //当父控件有偏移，但是WebView却不在底部时，属于异常情况，进行修复，
        //有两种修复方案：1.将WebView手动滑动到底部，2.将父控件的scroll位置重置为0
        //目前的测试中没有出现这种异常，此代码作为异常防御
        if (isParentCenter() && canWebViewScrollDown()) {
            if (getScrollY() > getMeasuredHeight() / 4) {
                scrollToWebViewBottom();
            } else {
                scrollTo(0, 0);
            }
        }
    }


}
