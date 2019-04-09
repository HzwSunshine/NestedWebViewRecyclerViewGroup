package com.hzw.nestedviewgroup;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.lang.ref.WeakReference;

/**
 * author: hzw
 * time: 2019/3/5 下午3:13
 * description:{@link NestedWebViewRecyclerViewGroup}的ScrollBar
 */
class ScrollBarView extends View {

    private final static int ALPHA_OFFSET = 20;
    private final static int MSG_WHAT = 1314;
    private final static int DURATION = 300;
    private Paint paint = new Paint();
    private BarHandler handler;
    private int minBarHeight;
    private int barWidth;
    private int center;
    private int space;
    private int measureHeight;
    private int barHeight;
    private int barOffset;
    private int alpha;
    private boolean isClearBar;
    private boolean scrollOver;

    public ScrollBarView(Context context) {
        this(context, null);
    }

    public ScrollBarView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        handler = new BarHandler(this);
    }

    void init(int color, int space, int barWidth, int minBarHeight) {
        //ScrollBar颜色
        paint.setColor(color);
        //ScrollBar距离屏幕右边的距离
        this.space = space;
        //ScrollBar粗细
        this.barWidth = barWidth;
        //ScrollBar最小高度
        this.minBarHeight = minBarHeight;
        //绘制水平中心点
        center = barWidth / 2;
        paint.setStrokeWidth(barWidth);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(barWidth + space, h);
        measureHeight = h;
    }

    void setScrollBar(int contentHeight, int parentScrollY, int childScrollY) {
        if (contentHeight == 0) return;
        isClearBar = false;
        scrollOver = false;
        alpha += ALPHA_OFFSET;
        alpha = alpha > 255 ? 255 : alpha;
        //计算scrollBar的高度
        barHeight = measureHeight * measureHeight / contentHeight;
        barHeight = barHeight < minBarHeight ? minBarHeight : barHeight;
        //计算scrollBar的偏移量
        barOffset = parentScrollY + childScrollY * (measureHeight - barHeight) / (contentHeight - measureHeight);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isClearBar) {
            paint.setAlpha(alpha);
            canvas.drawLine(center, barOffset, center, barOffset + barHeight, paint);
            clearScrollBar();
        }
    }

    private void clearScrollBar() {
        if (!scrollOver) {
            handler.removeMessages(MSG_WHAT);
            handler.sendEmptyMessageDelayed(MSG_WHAT, DURATION);
        }
    }

    private static class BarHandler extends Handler {
        private WeakReference<ScrollBarView> reference;

        BarHandler(ScrollBarView barView) {
            reference = new WeakReference<>(barView);
        }

        @Override
        public void handleMessage(Message msg) {
            ScrollBarView barView = reference.get();
            if (barView != null) {
                barView.scrollOver = true;
                barView.alpha -= ALPHA_OFFSET;
                barView.alpha = barView.alpha < 0 ? 0 : barView.alpha;
                if (barView.alpha == 0) {
                    barView.isClearBar = true;
                } else {
                    sendEmptyMessageDelayed(MSG_WHAT, 30);
                }
                barView.invalidate();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }


}
