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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.net.NetworkStatsHistory;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.android.settings.R;
import com.google.common.base.Preconditions;

/**
 * {@link NetworkStatsHistory} series to render inside a {@link ChartView},
 * using {@link ChartAxis} to map into screen coordinates.
 */
public class ChartNetworkSeriesView extends View {
    private static final String TAG = "ChartNetworkSeriesView";
    private static final boolean LOGD = true;

    private ChartAxis mHoriz;
    private ChartAxis mVert;

    private Paint mPaintStroke;
    private Paint mPaintFill;
    private Paint mPaintFillSecondary;

    private NetworkStatsHistory mStats;

    private Path mPathStroke;
    private Path mPathFill;

    private long mPrimaryLeft;
    private long mPrimaryRight;

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

        setChartColor(stroke, fill, fillSecondary);
        setWillNotDraw(false);

        a.recycle();

        mPathStroke = new Path();
        mPathFill = new Path();
    }

    void init(ChartAxis horiz, ChartAxis vert) {
        mHoriz = Preconditions.checkNotNull(horiz, "missing horiz");
        mVert = Preconditions.checkNotNull(vert, "missing vert");
    }

    public void setChartColor(int stroke, int fill, int fillSecondary) {
        mPaintStroke = new Paint();
        mPaintStroke.setStrokeWidth(6.0f);
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
    }

    public void bindNetworkStats(NetworkStatsHistory stats) {
        mStats = stats;

        mPathStroke.reset();
        mPathFill.reset();
        invalidate();
    }

    /**
     * Set the range to paint with {@link #mPaintFill}, leaving the remaining
     * area to be painted with {@link #mPaintFillSecondary}.
     */
    public void setPrimaryRange(long left, long right) {
        mPrimaryLeft = left;
        mPrimaryRight = right;
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        generatePath();
    }

    /**
     * Erase any existing {@link Path} and generate series outline based on
     * currently bound {@link NetworkStatsHistory} data.
     */
    public void generatePath() {
        if (LOGD) Log.d(TAG, "generatePath()");

        mPathStroke.reset();
        mPathFill.reset();

        // bail when not enough stats to render
        if (mStats == null || mStats.bucketCount < 2) return;

        final int width = getWidth();
        final int height = getHeight();

        boolean started = false;
        float firstX = 0;
        float lastX = 0;
        float lastY = 0;

        // TODO: count fractional data from first bucket crossing start;
        // currently it only accepts first full bucket.

        long totalData = 0;

        for (int i = 0; i < mStats.bucketCount; i++) {
            final float x = mHoriz.convertToPoint(mStats.bucketStart[i]);
            final float y = mVert.convertToPoint(totalData);

            // skip until we find first stats on screen
            if (i > 0 && !started && x > 0) {
                mPathStroke.moveTo(lastX, lastY);
                mPathFill.moveTo(lastX, lastY);
                started = true;
                firstX = x;
            }

            if (started) {
                mPathStroke.lineTo(x, y);
                mPathFill.lineTo(x, y);
                totalData += mStats.rx[i] + mStats.tx[i];
            }

            // skip if beyond view
            if (x > width) break;

            lastX = x;
            lastY = y;
        }

        if (LOGD) {
            final RectF bounds = new RectF();
            mPathFill.computeBounds(bounds, true);
            Log.d(TAG, "onLayout() rendered with bounds=" + bounds.toString() + " and totalData="
                    + totalData);
        }

        // drop to bottom of graph from current location
        mPathFill.lineTo(lastX, height);
        mPathFill.lineTo(firstX, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int save;

        final float primaryLeftPoint = mHoriz.convertToPoint(mPrimaryLeft);
        final float primaryRightPoint = mHoriz.convertToPoint(mPrimaryRight);

        save = canvas.save();
        canvas.clipRect(0, 0, primaryLeftPoint, getHeight());
        canvas.drawPath(mPathFill, mPaintFillSecondary);
        canvas.restoreToCount(save);

        save = canvas.save();
        canvas.clipRect(primaryRightPoint, 0, getWidth(), getHeight());
        canvas.drawPath(mPathFill, mPaintFillSecondary);
        canvas.restoreToCount(save);

        save = canvas.save();
        canvas.clipRect(primaryLeftPoint, 0, primaryRightPoint, getHeight());
        canvas.drawPath(mPathFill, mPaintFill);
        canvas.drawPath(mPathStroke, mPaintStroke);
        canvas.restoreToCount(save);

    }
}
