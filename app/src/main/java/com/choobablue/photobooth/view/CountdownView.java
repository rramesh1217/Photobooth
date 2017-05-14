package com.choobablue.photobooth.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.choobablue.photobooth.R;

import java.util.Formatter;
import java.util.Locale;


/**
 * TODO: document your custom view class.
 */
public class CountdownView extends TextView {

    private long mDurationMillis;
    private long mEndMillis;
    private boolean mRunning;
    private boolean mStarted;
    private boolean mVisible;

    private StringBuilder mRecycle = new StringBuilder(8);

    private static final int TICK_WHAT = 2;
    private static final long REDRAW_PERIOD_MS = 200;
    private static final int DEFAULT_BACKGROUND = Color.argb(96, 0, 0, 0);


    public CountdownView(Context context) {
        super(context);
        init(null, 0);
    }

    public CountdownView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public CountdownView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.CountdownView, defStyle, 0);
        // not implemented
        a.recycle();

        super.setBackgroundColor(DEFAULT_BACKGROUND);
        super.setTextSize(48);
        super.setTextColor(Color.WHITE);

//        // Set up a default TextPaint object
//        mTextPaint = new TextPaint();
//        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
//        mTextPaint.setTextAlign(Paint.Align.LEFT);
//
//        // Update TextPaint and text measurements from attributes
//        invalidateTextPaintAndMeasurements();
    }



//    private void invalidateTextPaintAndMeasurements() {
//        mTextPaint.setTextSize(mExampleDimension);
//        mTextPaint.setColor(mColor);
//        mTextWidth = mTextPaint.measureText("0");
//
//        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
//        mTextHeight = fontMetrics.bottom;
//    }

//    @Override
//    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//
//        // TODO: consider storing these as member variables to reduce
//        // allocations per draw cycle.
//        int paddingLeft = getPaddingLeft();
//        int paddingTop = getPaddingTop();
//        int paddingRight = getPaddingRight();
//        int paddingBottom = getPaddingBottom();
//
//        int contentWidth = getWidth() - paddingLeft - paddingRight;
//        int contentHeight = getHeight() - paddingTop - paddingBottom;
//
////        // Draw the example drawable on top of the text.
////        if (mExampleDrawable != null) {
////            mExampleDrawable.setBounds(paddingLeft, paddingTop,
////                    paddingLeft + contentWidth, paddingTop + contentHeight);
////            mExampleDrawable.draw(canvas);
////        }
//
//        // Draw the text.
//        canvas.drawText(mExampleString,
//                paddingLeft + (contentWidth - mTextWidth) / 2,
//                paddingTop + (contentHeight + mTextHeight) / 2,
//                mTextPaint);
//    }


    public long getDurationMillis() {
        return this.mDurationMillis;
    }

    public void setDurationMillis(final long millis) {
        this.mDurationMillis = millis;
    }

    public void setEndMillis(final long millis) {
        this.mEndMillis = millis;
        // dispatchChronometerTick();
        updateCountdown(SystemClock.elapsedRealtime());
    }


    public void start() {
        if (this.mEndMillis == 0) {
            this.mEndMillis = SystemClock.elapsedRealtime() + mDurationMillis;
        }
        mStarted = true;
        updateRunning();
    }

    public void stop() {
        this.mEndMillis = 0;
        mStarted = false;
        updateRunning();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mVisible = false;
        updateRunning();
    }

    @Override
    protected void onWindowVisibilityChanged(final int visibility) {
        super.onWindowVisibilityChanged(visibility);
        mVisible = (visibility == VISIBLE);
        updateRunning();
    }


    private synchronized void updateCountdown(final long now) {
        final long diff = (mEndMillis != 0) ? (mEndMillis - now) : mDurationMillis;
        final long seconds = diff / 1000 + 1;

        String format = "%1$02d"; // load from resources for i18n?

        StringBuilder sb = mRecycle;
        sb.setLength(0);
        Formatter f = new Formatter(sb, Locale.getDefault());

        setText(f.format(format, seconds).toString());
    }


    private void updateRunning() {
        boolean running = mVisible && mStarted;
        if (running != mRunning) {
            if (running) {
                updateCountdown(SystemClock.elapsedRealtime());
                // dispatchChronometerTick();
                mHandler.sendMessageDelayed(Message.obtain(mHandler, TICK_WHAT), REDRAW_PERIOD_MS);
            } else {
                mHandler.removeMessages(TICK_WHAT);
            }
            mRunning = running;
        }
    }


    private Handler mHandler = new Handler() {
        public void handleMessage(Message m) {
            if (mRunning) {
                updateCountdown(SystemClock.elapsedRealtime());
                // dispatchChronometerTick();
                sendMessageDelayed(Message.obtain(this, TICK_WHAT), REDRAW_PERIOD_MS);
            }
        }
    };

}
