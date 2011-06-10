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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.net.NetworkStatsHistory;
import android.util.Log;
import android.view.View;

import com.google.common.base.Preconditions;

/**
 * {@link NetworkStatsHistory} series to render inside a {@link ChartView},
 * using {@link ChartAxis} to map into screen coordinates.
 */
public class ChartNetworkSeriesView extends View {
    private static final String TAG = "ChartNetworkSeriesView";
    private static final boolean LOGD = true;

    private final ChartAxis mHoriz;
    private final ChartAxis mVert;

    private final Paint mPaintStroke;
    private final Paint mPaintFill;
    private final Paint mPaintFillDisabled;

    private NetworkStatsHistory mStats;

    private Path mPathStroke;
    private Path mPathFill;

    private ChartSweepView mSweep1;
    private ChartSweepView mSweep2;

    public ChartNetworkSeriesView(Context context, ChartAxis horiz, ChartAxis vert) {
        super(context);

        mHoriz = Preconditions.checkNotNull(horiz, "missing horiz");
        mVert = Preconditions.checkNotNull(vert, "missing vert");

        mPaintStroke = new Paint();
        mPaintStroke.setStrokeWidth(6.0f);
        mPaintStroke.setColor(Color.parseColor("#24aae1"));
        mPaintStroke.setStyle(Style.STROKE);
        mPaintStroke.setAntiAlias(true);

        mPaintFill = new Paint();
        mPaintFill.setColor(Color.parseColor("#c050ade5"));
        mPaintFill.setStyle(Style.FILL);
        mPaintFill.setAntiAlias(true);

        mPaintFillDisabled = new Paint();
        mPaintFillDisabled.setColor(Color.parseColor("#88566abc"));
        mPaintFillDisabled.setStyle(Style.FILL);
        mPaintFillDisabled.setAntiAlias(true);

        mPathStroke = new Path();
        mPathFill = new Path();
    }

    public void bindNetworkStats(NetworkStatsHistory stats) {
        mStats = stats;

        mPathStroke.reset();
        mPathFill.reset();
    }

    public void bindSweepRange(ChartSweepView sweep1, ChartSweepView sweep2) {
        // TODO: generalize to support vertical sweeps
        // TODO: enforce that both sweeps are along same dimension

        mSweep1 = Preconditions.checkNotNull(sweep1, "missing sweep1");
        mSweep2 = Preconditions.checkNotNull(sweep2, "missing sweep2");
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
            Log.d(TAG, "onLayout() rendered with bounds=" + bounds.toString());
        }

        // drop to bottom of graph from current location
        mPathFill.lineTo(lastX, height);
        mPathFill.lineTo(firstX, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        // clip to sweep area
        final float sweep1 = mSweep1.getPoint();
        final float sweep2 = mSweep2.getPoint();
        final float sweepLeft = Math.min(sweep1, sweep2);
        final float sweepRight = Math.max(sweep1, sweep2);

        int save;

        save = canvas.save();
        canvas.clipRect(0, 0, sweepLeft, getHeight());
        canvas.drawPath(mPathFill, mPaintFillDisabled);
        canvas.restoreToCount(save);

        save = canvas.save();
        canvas.clipRect(sweepRight, 0, getWidth(), getHeight());
        canvas.drawPath(mPathFill, mPaintFillDisabled);
        canvas.restoreToCount(save);

        save = canvas.save();
        canvas.clipRect(sweepLeft, 0, sweepRight, getHeight());
        canvas.drawPath(mPathFill, mPaintFill);
        canvas.drawPath(mPathStroke, mPaintStroke);
        canvas.restoreToCount(save);

    }
}
