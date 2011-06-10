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
import android.graphics.Color;
import android.net.NetworkPolicy;
import android.net.NetworkStatsHistory;
import android.text.format.DateUtils;

import com.android.settings.widget.ChartSweepView.OnSweepListener;

/**
 * Specific {@link ChartView} that displays {@link ChartNetworkSeriesView} along
 * with {@link ChartSweepView} for inspection ranges and warning/limits.
 */
public class DataUsageChartView extends ChartView {

    private static final long KB_IN_BYTES = 1024;
    private static final long MB_IN_BYTES = KB_IN_BYTES * 1024;
    private static final long GB_IN_BYTES = MB_IN_BYTES * 1024;

    private ChartNetworkSeriesView mSeries;

    // TODO: limit sweeps at graph boundaries
    private ChartSweepView mSweepTime1;
    private ChartSweepView mSweepTime2;
    private ChartSweepView mSweepDataWarn;
    private ChartSweepView mSweepDataLimit;

    public interface DataUsageChartListener {
        public void onInspectRangeChanged();
        public void onLimitsChanged();
    }

    private DataUsageChartListener mListener;

    private static ChartAxis buildTimeAxis() {
        return new TimeAxis();
    }

    private static ChartAxis buildDataAxis() {
        return new InvertedChartAxis(new DataAxis());
    }

    public DataUsageChartView(Context context) {
        super(context, buildTimeAxis(), buildDataAxis());
        setPadding(20, 20, 20, 20);

        addView(new ChartGridView(context, mHoriz, mVert), buildChartParams());

        mSeries = new ChartNetworkSeriesView(context, mHoriz, mVert);
        addView(mSeries, buildChartParams());

        mSweepTime1 = new ChartSweepView(context, mHoriz, 0L, Color.parseColor("#ffffff"));
        mSweepTime2 = new ChartSweepView(context, mHoriz, 0L, Color.parseColor("#ffffff"));
        mSweepDataWarn = new ChartSweepView(context, mVert, 0L, Color.parseColor("#f7931d"));
        mSweepDataLimit = new ChartSweepView(context, mVert, 0L, Color.parseColor("#be1d2c"));

        addView(mSweepTime1, buildSweepParams());
        addView(mSweepTime2, buildSweepParams());
        addView(mSweepDataWarn, buildSweepParams());
        addView(mSweepDataLimit, buildSweepParams());

        mSeries.bindSweepRange(mSweepTime1, mSweepTime2);

        mSweepTime1.addOnSweepListener(mSweepListener);
        mSweepTime2.addOnSweepListener(mSweepListener);

    }

    public void setListener(DataUsageChartListener listener) {
        mListener = listener;
    }

    public void bindNetworkStats(NetworkStatsHistory stats) {
        mSeries.bindNetworkStats(stats);
    }

    public void bindNetworkPolicy(NetworkPolicy policy) {
        if (policy.limitBytes != -1) {
            mSweepDataLimit.setValue(policy.limitBytes);
            mSweepDataLimit.setEnabled(true);
        } else {
            mSweepDataLimit.setValue(5 * GB_IN_BYTES);
            mSweepDataLimit.setEnabled(false);
        }

        mSweepDataWarn.setValue(policy.warningBytes);
    }

    private OnSweepListener mSweepListener = new OnSweepListener() {
        public void onSweep(ChartSweepView sweep, boolean sweepDone) {
            // always update graph clip region
            mSeries.invalidate();

            // update detail list only when done sweeping
            if (sweepDone && mListener != null) {
                mListener.onInspectRangeChanged();
            }
        }
    };

    /**
     * Return current inspection range (start and end time) based on internal
     * {@link ChartSweepView} positions.
     */
    public long[] getInspectRange() {
        final long sweep1 = mSweepTime1.getValue();
        final long sweep2 = mSweepTime2.getValue();
        final long start = Math.min(sweep1, sweep2);
        final long end = Math.max(sweep1, sweep2);
        return new long[] { start, end };
    }

    public long getWarningBytes() {
        return mSweepDataWarn.getValue();
    }

    public long getLimitBytes() {
        return mSweepDataLimit.getValue();
    }

    /**
     * Set the exact time range that should be displayed, updating how
     * {@link ChartNetworkSeriesView} paints. Moves inspection ranges to be the
     * last "week" of available data, without triggering listener events.
     */
    public void setVisibleRange(long start, long end, long dataBoundary) {
        mHoriz.setBounds(start, end);

        // default sweeps to last week of data
        final long halfRange = (end + start) / 2;
        final long sweepMax = Math.min(end, dataBoundary);
        final long sweepMin = Math.max(start, (sweepMax - DateUtils.WEEK_IN_MILLIS));

        mSweepTime1.setValue(sweepMin);
        mSweepTime2.setValue(sweepMax);

        requestLayout();
        mSeries.generatePath();
    }

    public static class TimeAxis implements ChartAxis {
        private static final long TICK_INTERVAL = DateUtils.DAY_IN_MILLIS * 7;

        private long mMin;
        private long mMax;
        private float mSize;

        public TimeAxis() {
            final long currentTime = System.currentTimeMillis();
            setBounds(currentTime - DateUtils.DAY_IN_MILLIS * 30, currentTime);
        }

        /** {@inheritDoc} */
        public void setBounds(long min, long max) {
            mMin = min;
            mMax = max;
        }

        /** {@inheritDoc} */
        public void setSize(float size) {
            this.mSize = size;
        }

        /** {@inheritDoc} */
        public float convertToPoint(long value) {
            return (mSize * (value - mMin)) / (mMax - mMin);
        }

        /** {@inheritDoc} */
        public long convertToValue(float point) {
            return (long) (mMin + ((point * (mMax - mMin)) / mSize));
        }

        /** {@inheritDoc} */
        public CharSequence getLabel(long value) {
            // TODO: convert to string
            return Long.toString(value);
        }

        /** {@inheritDoc} */
        public float[] getTickPoints() {
            // tick mark for every week
            final int tickCount = (int) ((mMax - mMin) / TICK_INTERVAL);
            final float[] tickPoints = new float[tickCount];
            for (int i = 0; i < tickCount; i++) {
                tickPoints[i] = convertToPoint(mMax - (TICK_INTERVAL * i));
            }
            return tickPoints;
        }
    }

    public static class DataAxis implements ChartAxis {
        private long mMin;
        private long mMax;
        private long mMinLog;
        private long mMaxLog;
        private float mSize;

        public DataAxis() {
            // TODO: adapt ranges to show when history >5GB, and handle 4G
            // interfaces with higher limits.
            setBounds(1 * MB_IN_BYTES, 5 * GB_IN_BYTES);
        }

        /** {@inheritDoc} */
        public void setBounds(long min, long max) {
            mMin = min;
            mMax = max;
            mMinLog = (long) Math.log(mMin);
            mMaxLog = (long) Math.log(mMax);
        }

        /** {@inheritDoc} */
        public void setSize(float size) {
            this.mSize = size;
        }

        /** {@inheritDoc} */
        public float convertToPoint(long value) {
            return (mSize * (value - mMin)) / (mMax - mMin);

            // TODO: finish tweaking log scale
//            if (value > mMin) {
//                return (float) ((mSize * (Math.log(value) - mMinLog)) / (mMaxLog - mMinLog));
//            } else {
//                return 0;
//            }
        }

        /** {@inheritDoc} */
        public long convertToValue(float point) {
            return (long) (mMin + ((point * (mMax - mMin)) / mSize));

            // TODO: finish tweaking log scale
//            return (long) Math.pow(Math.E, (mMinLog + ((point * (mMaxLog - mMinLog)) / mSize)));
        }

        /** {@inheritDoc} */
        public CharSequence getLabel(long value) {
            // TODO: convert to string
            return Long.toString(value);
        }

        /** {@inheritDoc} */
        public float[] getTickPoints() {
            final float[] tickPoints = new float[16];

            long value = mMax;
            float mult = 0.8f;
            for (int i = 0; i < tickPoints.length; i++) {
                tickPoints[i] = convertToPoint(value);
                value = (long) (value * mult);
                mult *= 0.9;
            }
            return tickPoints;
        }
    }

}
