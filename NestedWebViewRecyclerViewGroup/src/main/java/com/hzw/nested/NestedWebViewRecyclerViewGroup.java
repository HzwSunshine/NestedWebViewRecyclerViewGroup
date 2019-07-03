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

    private final int switchDuration;
    private int webViewContentHeight;
    private int currentScrollType;
    private int rvContentHeight;
    private int maximumVelocity;
    private final float density;
    private int webViewHeight;
    private int totalHeight;
    private int maxScrollY;

    //是否在上下切换滑动中...
    private boolean isSwitching;
    private boolean hasFling;
    private boolean isRvFlingDown;
    private boolean isRvShow;


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
        switchDuration = array.getInteger(R.styleable.NestedWebViewRecyclerViewGroup_switchDuration, 300);
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
        if (getChildCount() < 1 || getChildCount() > 3) {
            if (BuildConfig.DEBUG) {
                throw new IllegalStateException("Please check child layout, " +
                        "child must be NestedScrollWebView and RecyclerView");
            }
            return;
        }
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
        int measureHeight = MeasureSpec.getSize(heightMeasureSpec);
        int width = measureWidth;
        if (widthMode != MeasureSpec.EXACTLY) {
            width = getContext().getResources().getDisplayMetrics().widthPixels;
        }
        int count = getChildCount();
        totalHeight = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            if (child.getVisibility() != GONE && !(child instanceof ScrollBarView)) {
                totalHeight += child.getMeasuredHeight();
            }
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
            childHeight = child.getVisibility() != GONE ? childHeight : 0;
            if (child instanceof ScrollBarView) {
                bringChildToFront(child);
                child.layout(getMeasuredWidth() - childWidth, 0, childHeight, totalHeight);
            } else {
                child.layout(0, childTotalHeight, childWidth, childTotalHeight + childHeight);
            }
            childTotalHeight += childHeight;
        }
        fixScroll();
        if (!isSwitching) {
            fixError();
        }
    }

    private void fixScroll() {
        rvContentHeight = 0;
        webViewContentHeight = 0;
        if (webView != null) {
            webViewHeight = webView.getMeasuredHeight();
        }
        if (getMaxScrollY() < getScrollY() && getScrollY() > 0) {
            scrollTo(0, maxScrollY);
        }
    }

    private int getMaxScrollY() {
        //maxScrollY==0说明RV高度为0
        //maxScrollY<0说明内容不足父控件高度
        maxScrollY = totalHeight - getMeasuredHeight();
        maxScrollY = maxScrollY < 0 ? 0 : maxScrollY;
        return maxScrollY;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() < 1) return;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child instanceof NestedScrollWebView) {
                webView = (NestedScrollWebView) child;
            } else if (child instanceof RecyclerView) {
                recyclerView = (RecyclerView) child;
            }
        }
        if (scrollbar != null && scrollbar.getParent() == null) {
            addView(scrollbar);
        }
        runnable = new Runnable() {
            @Override
            public void run() {
                if (webView != null) {
                    webViewHeight = webView.getHeight();
                } else {
                    findWebView(NestedWebViewRecyclerViewGroup.this);
                }
                //找不到RecyclerView时，会在界面显示时再次尝试重新获取
                findRecyclerView(NestedWebViewRecyclerViewGroup.this);
            }
        };
        post(runnable);
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
            if (child instanceof ViewGroup && recyclerView == null) {
                findRecyclerView((ViewGroup) child);
            }
        }
    }

    private void findWebView(ViewGroup parent) {
        if (webView != null) return;
        int count = parent.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = parent.getChildAt(i);
            if (child instanceof NestedScrollWebView) {
                webView = (NestedScrollWebView) child;
                break;
            }
            if (child instanceof ViewGroup && webView == null) {
                findWebView((ViewGroup) child);
            }
        }
        if (webView != null) {
            webViewHeight = webView.getHeight();
            super.requestLayout();
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
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                webViewContentHeight = 0;
                rvContentHeight = 0;
                hasFling = false;
                isRvFlingDown = false;
                initOrResetVelocityTracker();
                resetScroller();
                getMaxScrollY();
                fixError();
                isRVShow();
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
        if (webView != null) {
            webView.stopScroll();
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
        recycleVelocityTracker();
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
        } else if (target instanceof RecyclerView && velocityY < 0 && getScrollY() == maxScrollY) {
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

    @Override
    public void onNestedPreScroll(@NonNull View view, int dx, int dy, @NonNull int[] ints, int type) {
        boolean isWebViewBottom = !canWebViewScrollDown();
        boolean isParentCenter = isParentCenter();
        if (dy > 0 && getScrollY() < maxScrollY && isWebViewBottom) {
            //为了WebView滑动到底部，继续向下滑动父控件
            scrollBy(0, dy);
            ints[1] = dy;
        } else if (dy < 0 && isParentCenter) {
            //为了RecyclerView滑动到顶部时，继续向上滑动父控件
            scrollBy(0, dy);
            ints[1] = dy;
        }
        if (isParentCenter && !isWebViewBottom) {
            //异常情况的处理
            scrollToWebViewBottom();
        }
        if (isParentCenter) {
            checkRvTop();
        }
    }

    @Override
    public void onNestedScroll(@NonNull View view, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed, int type) {
        if (dyUnconsumed < 0) {
            //RecyclerView向父控件的滑动衔接处
            scrollBy(0, dyUnconsumed);
        }
        if (!isParentCenter()) {
            //用于绘制进度条
            invalidate();
        }
    }

    /**
     * 判断连接处的显示
     */
    private boolean isParentCenter() {
        return getScrollY() > 0 && getScrollY() < webViewHeight;
    }

    @Override
    public void onStopNestedScroll(@NonNull View view, int i) {
        helper.onStopNestedScroll(view);
    }

    @Override
    public void scrollTo(int x, int y) {
        if (y > maxScrollY) {
            y = maxScrollY;
        }
        if (y < 0) {
            y = 0;
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
                    if (!isSwitching) {
                        invalidate();//切换结束，显示一下进度条
                    }
                    break;
                case SCROLL_WEB_PARENT://WebView向父控件滑动
                    if (isRvFlingDown) {
                        //RecyclerView的区域的fling由自己完成
                        break;
                    }
                    scrollTo(0, currY);
                    invalidate();
                    if (isParentCenter()) {
                        checkRvTop();
                    }
                    if (getScrollY() == maxScrollY && !hasFling && recyclerView != null) {
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
        if (webView == null) {
            return false;
        }
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
        if (scrollbar == null && listener == null || webView == null || isSwitching) {
            return;
        }
        int webViewContentHeight = getWebViewContentHeight();
        if (webViewContentHeight == 0) return;
        int totalHeight = webViewContentHeight + getRVContentHeight();
        if (totalHeight < getHeight()) {
            return;
        }
        int scrollY = getCurrentScrollY();
        if (scrollbar != null) {
            scrollbar.setScrollBar(totalHeight, this.getScrollY(), scrollY);
        }
        if (listener != null) {
            listener.scroll(scrollY);
        }
    }

    private int getWebViewContentHeight() {
        if (webViewContentHeight == 0 && webView != null) {
            webViewContentHeight = (int) (webView.getContentHeight() * density);
        }
        return webViewContentHeight;
    }

    private int getRVContentHeight() {
        if (recyclerView == null || !isRvShow) {
            return 0;
        }
        if (rvContentHeight == 0 || getScrollY() == maxScrollY) {
            //在RecyclerView区域时实时获取内容高度
            rvContentHeight = recyclerView.computeVerticalScrollRange();
        }
        return rvContentHeight;
    }

    private int getRVScrollY() {
        if (recyclerView == null || !isRvShow) return 0;
        //RecyclerView滑动时再计算滑动距离
        if (getScrollY() != maxScrollY) return 0;
        return recyclerView.computeVerticalScrollOffset();
    }

    private void isRVShow() {
        if (recyclerView == null) {
            isRvShow = false;
            return;
        }
        isRvShow = recyclerView.isShown();
    }

    public void setOnScrollListener(onScrollListener listener) {
        this.listener = listener;
    }

    /**
     * 获取当前的滑动距离
     */
    public int getCurrentScrollY() {
        if (webView == null) return 0;
        int wbScrollY = getWebViewScrollY();
        if (maxScrollY != 0 && getScrollY() == maxScrollY) {
            wbScrollY = getWebViewContentHeight() - webView.getHeight();
        }
        return getScrollY() + wbScrollY + getRVScrollY();
    }

    /**
     * 获取当前WebView的滑动距离
     */
    public int getWebViewScrollY() {
        return webView.getScrollY();
    }

    /**
     * 如果WebView为MatchParent并且内容存在不满一屏的情况，需要手动设置WebView的内容高度
     * 如果WebView为WrapContent时，通常并不需要，如果存在高度不准确的情况，可以手动设置
     * WebView的内容高度可让前端同学通过js传递给你
     */
    public void setWebViewContentHeight(int contentHeight) {
        int height = getMeasuredHeight();
        if (contentHeight > 0 && height > 0) {
            rvScrollToPosition(0);
            //手动设置的WebView内容高度不为0时，重新布局页面
            webViewHeight = contentHeight;
            if (webViewHeight >= height) {
                webViewHeight = height;
                resetWebViewHeight(LayoutParams.MATCH_PARENT);
            } else {
                resetWebViewHeight(webViewHeight);
            }
            super.requestLayout();
        }
    }

    private void resetWebViewHeight(int height) {
        if (webView != null) {
            ViewGroup.LayoutParams params = webView.getLayoutParams();
            params.height = height;
            webView.setLayoutParams(params);
        }
    }

    /**
     * 滚动到某个位置
     */
    public void scrollToPosition(int y) {
        if (webView == null) return;
        int webViewContentHeight = getWebViewContentHeight();
        if (webViewContentHeight == 0) return;
        int webH = webViewContentHeight - webViewHeight;
        if (y <= webH) {
            webView.scrollTo(0, y);
        } else if (y <= webH + webViewHeight) {
            scrollToWebViewBottom();
            scrollTo(0, y - webH);
        } else {
            scrollToWebViewBottom();
            scrollTo(0, webViewHeight);
        }
    }

    /**
     * WebView和RecyclerView的上下切换
     *
     * @param rvPosition 切换到RecyclerView时需要定位到的位置
     */
    public void switchView(int rvPosition) {
        if (isSwitching || maxScrollY == 0) {
            return;
        }
        isSwitching = true;
        resetScroller();
        currentScrollType = SCROLL_SWITCH;
        if (getScrollY() == 0) {//滑向Bottom
            if (maxScrollY != getHeight()) {
                //向下切换时，如果rv的高度不足一屏，那么切换到rv时会处于连接处，需要将WebView滑动到底部
                scrollToWebViewBottom();
            }
            rvScrollToPosition(rvPosition);
            scroller.startScroll(0, 0, 0, maxScrollY, switchDuration);
        } else if (getScrollY() == maxScrollY) {//滑向Top
            if (webViewHeight < getHeight()) {
                rvScrollToPosition(0);
            }
            scroller.startScroll(0, maxScrollY, 0, -maxScrollY, switchDuration);
        } else {//在交界处优先滑向Top
            if (webViewHeight < getHeight()) {
                rvScrollToPosition(0);
            }
            scroller.startScroll(0, getScrollY(), 0, -getScrollY(), switchDuration);
        }
        scroller.computeScrollOffset();
        invalidate();
    }

    private void rvScrollToPosition(int position) {
        if (recyclerView == null) return;
        recyclerView.scrollToPosition(position);
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if (manager instanceof LinearLayoutManager) {
            ((LinearLayoutManager) manager).scrollToPositionWithOffset(position, 0);
        }
    }

    /**
     * 当父控件有偏移时，检查RecyclerView是否在顶部
     */
    private void checkRvTop() {
        if (isRvNotTop()) {
            rvScrollToPosition(0);
        }
    }

    private boolean isRvNotTop() {
        if (recyclerView == null) return true;
        return recyclerView.canScrollVertically(-1);
    }

    /**
     * 处理未知的错误情况
     */
    private void fixError() {
        //当父控件有偏移，但是WebView却不在底部时，或者RecyclerView不在顶部时，属于异常情况，进行修复
        //目前的测试中没有出现这种异常，此代码作为异常防御
        if (isParentCenter()) {
            if (canWebViewScrollDown()) {
                scrollToWebViewBottom();
            }
            if (isRvNotTop()) {
                rvScrollToPosition(0);
            }
        }
    }


}
