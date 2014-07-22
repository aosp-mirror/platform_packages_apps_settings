/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.widget;

import static android.text.format.DateUtils.DAY_IN_MILLIS;
import static android.text.format.DateUtils.WEEK_IN_MILLIS;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.net.NetworkStatsHistory;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.android.internal.util.Preconditions;
import com.android.settings.R;

/**
 * {@link NetworkStatsHistory} series to render inside a {@link ChartView},
 * using {@link ChartAxis} to map into screen coordinates.
 */
public class ChartNetworkSeriesView extends View {
    private static final String TAG = "ChartNetworkSeriesView";
    private static final boolean LOGD = false;

    private static final boolean ESTIMATE_ENABLED = false;

    private ChartAxis mHoriz;
    private ChartAxis mVert;

    private Paint mPaintStroke;
    private Paint mPaintFill;
    private Paint mPaintFillSecondary;
    private Paint mPaintEstimate;

    private NetworkStatsHistory mStats;

    private Path mPathStroke;
    private Path mPathFill;
    private Path mPathEstimate;

    private int mSafeRegion;

    private long mStart;
    private long mEnd;

    /** Series will be extended to reach this end time. */
    private long mEndTime = Long.MIN_VALUE;

    private boolean mPathValid = false;
    private boolean mEstimateVisible = false;
    private boolean mSecondary = false;

    private long mMax;
    private long mMaxEstimate;

    public ChartNetworkSeriesView(Context context) {
        this(context, null, 0);
    }

    public ChartNetworkSeriesView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartNetworkSeriesView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ChartNetworkSeriesView, defStyle, 0);

        final int stroke = a.getColor(R.styleable.ChartNetworkSeriesView_strokeColor, Color.RED);
        final int fill = a.getColor(R.styleable.ChartNetworkSeriesView_fillColor, Color.RED);
        final int fillSecondary = a.getColor(
                R.styleable.ChartNetworkSeriesView_fillColorSecondary, Color.RED);
        final int safeRegion = a.getDimensionPixelSize(
                R.styleable.ChartNetworkSeriesView_safeRegion, 0);

        setChartColor(stroke, fill, fillSecondary);
        setSafeRegion(safeRegion);
        setWillNotDraw(false);

        a.recycle();

        mPathStroke = new Path();
        mPathFill = new Path();
        mPathEstimate = new Path();
    }

    void init(ChartAxis horiz, ChartAxis vert) {
        mHoriz = Preconditions.checkNotNull(horiz, "missing horiz");
        mVert = Preconditions.checkNotNull(vert, "missing vert");
    }

    public void setChartColor(int stroke, int fill, int fillSecondary) {
        mPaintStroke = new Paint();
        mPaintStroke.setStrokeWidth(4.0f * getResources().getDisplayMetrics().density);
        mPaintStroke.setColor(stroke);
        mPaintStroke.setStyle(Style.STROKE);
        mPaintStroke.setAntiAlias(true);

        mPaintFill = new Paint();
        mPaintFill.setColor(fill);
        mPaintFill.setStyle(Style.FILL);
        mPaintFill.setAntiAlias(true);

        mPaintFillSecondary = new Paint();
        mPaintFillSecondary.setColor(fillSecondary);
        mPaintFillSecondary.setStyle(Style.FILL);
        mPaintFillSecondary.setAntiAlias(true);

        mPaintEstimate = new Paint();
        mPaintEstimate.setStrokeWidth(3.0f);
        mPaintEstimate.setColor(fillSecondary);
        mPaintEstimate.setStyle(Style.STROKE);
        mPaintEstimate.setAntiAlias(true);
        mPaintEstimate.setPathEffect(new DashPathEffect(new float[] { 10, 10 }, 1));
    }

    public void setSafeRegion(int safeRegion) {
        mSafeRegion = safeRegion;
    }

    public void bindNetworkStats(NetworkStatsHistory stats) {
        mStats = stats;
        invalidatePath();
        invalidate();
    }

    public void setBounds(long start, long end) {
        mStart = start;
        mEnd = end;
    }

    public void setSecondary(boolean secondary) {
        mSecondary = secondary;
    }

    public void invalidatePath() {
        mPathValid = false;
        mMax = 0;
        invalidate();
    }

    /**
     * Erase any existing {@link Path} and generate series outline based on
     * currently bound {@link NetworkStatsHistory} data.
     */
    private void generatePath() {
        if (LOGD) Log.d(TAG, "generatePath()");

        mMax = 0;
        mPathStroke.reset();
        mPathFill.reset();
        mPathEstimate.reset();
        mPathValid = true;

        // bail when not enough stats to render
        if (mStats == null || mStats.size() < 2) {
            return;
        }

        final int width = getWidth();
        final int height = getHeight();

        boolean started = false;
        float lastX = 0;
        float lastY = height;
        long lastTime = mHoriz.convertToValue(lastX);

        // move into starting position
        mPathStroke.moveTo(lastX, lastY);
        mPathFill.moveTo(lastX, lastY);

        // TODO: count fractional data from first bucket crossing start;
        // currently it only accepts first full bucket.

        long totalData = 0;

        NetworkStatsHistory.Entry entry = null;

        final int start = mStats.getIndexBefore(mStart);
        final int end = mStats.getIndexAfter(mEnd);
        for (int i = start; i <= end; i++) {
            entry = mStats.getValues(i, entry);

            final long startTime = entry.bucketStart;
            final long endTime = startTime + entry.bucketDuration;

            final float startX = mHoriz.convertToPoint(startTime);
            final float endX = mHoriz.convertToPoint(endTime);

            // skip until we find first stats on screen
            if (endX < 0) continue;

            // increment by current bucket total
            totalData += entry.rxBytes + entry.txBytes;

            final float startY = lastY;
            final float endY = mVert.convertToPoint(totalData);

            if (lastTime != startTime) {
                // gap in buckets; line to start of current bucket
                mPathStroke.lineTo(startX, startY);
                mPathFill.lineTo(startX, startY);
            }

            // always draw to end of current bucket
            mPathStroke.lineTo(endX, endY);
            mPathFill.lineTo(endX, endY);

            lastX = endX;
            lastY = endY;
            lastTime = endTime;
        }

        // when data falls short, extend to requested end time
        if (lastTime < mEndTime) {
            lastX = mHoriz.convertToPoint(mEndTime);

            mPathStroke.lineTo(lastX, lastY);
            mPathFill.lineTo(lastX, lastY);
        }

        if (LOGD) {
            final RectF bounds = new RectF();
            mPathFill.computeBounds(bounds, true);
            Log.d(TAG, "onLayout() rendered with bounds=" + bounds.toString() + " and totalData="
                    + totalData);
        }

        // drop to bottom of graph from current location
        mPathFill.lineTo(lastX, height);
        mPathFill.lineTo(0, height);

        mMax = totalData;

        if (ESTIMATE_ENABLED) {
            // build estimated data
            mPathEstimate.moveTo(lastX, lastY);

            final long now = System.currentTimeMillis();
            final long bucketDuration = mStats.getBucketDuration();

            // long window is average over two weeks
            entry = mStats.getValues(lastTime - WEEK_IN_MILLIS * 2, lastTime, now, entry);
            final long longWindow = (entry.rxBytes + entry.txBytes) * bucketDuration
                    / entry.bucketDuration;

            long futureTime = 0;
            while (lastX < width) {
                futureTime += bucketDuration;

                // short window is day average last week
                final long lastWeekTime = lastTime - WEEK_IN_MILLIS + (futureTime % WEEK_IN_MILLIS);
                entry = mStats.getValues(lastWeekTime - DAY_IN_MILLIS, lastWeekTime, now, entry);
                final long shortWindow = (entry.rxBytes + entry.txBytes) * bucketDuration
                        / entry.bucketDuration;

                totalData += (longWindow * 7 + shortWindow * 3) / 10;

                lastX = mHoriz.convertToPoint(lastTime + futureTime);
                lastY = mVert.convertToPoint(totalData);

                mPathEstimate.lineTo(lastX, lastY);
            }

            mMaxEstimate = totalData;
        }

        invalidate();
    }

    public void setEndTime(long endTime) {
        mEndTime = endTime;
    }

    public void setEstimateVisible(boolean estimateVisible) {
        mEstimateVisible = ESTIMATE_ENABLED ? estimateVisible : false;
        invalidate();
    }

    public long getMaxEstimate() {
        return mMaxEstimate;
    }

    public long getMaxVisible() {
        final long maxVisible = mEstimateVisible ? mMaxEstimate : mMax;
        if (maxVisible <= 0 && mStats != null) {
            // haven't generated path yet; fall back to raw data
            final NetworkStatsHistory.Entry entry = mStats.getValues(mStart, mEnd, null);
            return entry.rxBytes + entry.txBytes;
        } else {
            return maxVisible;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int save;

        if (!mPathValid) {
            generatePath();
        }

        if (mEstimateVisible) {
            save = canvas.save();
            canvas.clipRect(0, 0, getWidth(), getHeight());
            canvas.drawPath(mPathEstimate, mPaintEstimate);
            canvas.restoreToCount(save);
        }

        final Paint paintFill = mSecondary ? mPaintFillSecondary : mPaintFill;

        save = canvas.save();
        canvas.clipRect(mSafeRegion, 0, getWidth(), getHeight() - mSafeRegion);
        canvas.drawPath(mPathFill, paintFill);
        canvas.restoreToCount(save);
    }
}
