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
import android.net.NetworkPolicy;
import android.net.NetworkStatsHistory;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.android.settings.R;
import com.android.settings.widget.ChartSweepView.OnSweepListener;

/**
 * Specific {@link ChartView} that displays {@link ChartNetworkSeriesView} along
 * with {@link ChartSweepView} for inspection ranges and warning/limits.
 */
public class DataUsageChartView extends ChartView {

    private static final long KB_IN_BYTES = 1024;
    private static final long MB_IN_BYTES = KB_IN_BYTES * 1024;
    private static final long GB_IN_BYTES = MB_IN_BYTES * 1024;

    // TODO: enforce that sweeps cant cross each other
    // TODO: limit sweeps at graph boundaries

    private ChartGridView mGrid;
    private ChartNetworkSeriesView mSeries;

    private ChartSweepView mSweepLeft;
    private ChartSweepView mSweepRight;
    private ChartSweepView mSweepWarning;
    private ChartSweepView mSweepLimit;

    public interface DataUsageChartListener {
        public void onInspectRangeChanged();
        public void onWarningChanged();
        public void onLimitChanged();
    }

    private DataUsageChartListener mListener;

    public DataUsageChartView(Context context) {
        this(context, null, 0);
    }

    public DataUsageChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DataUsageChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(new TimeAxis(), new InvertedChartAxis(new DataAxis()));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mGrid = (ChartGridView) findViewById(R.id.grid);
        mSeries = (ChartNetworkSeriesView) findViewById(R.id.series);

        mSweepLeft = (ChartSweepView) findViewById(R.id.sweep_left);
        mSweepRight = (ChartSweepView) findViewById(R.id.sweep_right);
        mSweepLimit = (ChartSweepView) findViewById(R.id.sweep_limit);
        mSweepWarning = (ChartSweepView) findViewById(R.id.sweep_warning);

        mSweepLeft.addOnSweepListener(mSweepListener);
        mSweepRight.addOnSweepListener(mSweepListener);
        mSweepWarning.addOnSweepListener(mWarningListener);
        mSweepLimit.addOnSweepListener(mLimitListener);

        // tell everyone about our axis
        mGrid.init(mHoriz, mVert);
        mSeries.init(mHoriz, mVert);
        mSweepLeft.init(mHoriz);
        mSweepRight.init(mHoriz);
        mSweepWarning.init(mVert);
        mSweepLimit.init(mVert);

        setActivated(false);
    }

    @Override
    public void setActivated(boolean activated) {
        super.setActivated(activated);

        mSweepLeft.setEnabled(activated);
        mSweepRight.setEnabled(activated);
        mSweepWarning.setEnabled(activated);
        mSweepLimit.setEnabled(activated);
    }

    @Deprecated
    public void setChartColor(int stroke, int fill, int disabled) {
        mSeries.setChartColor(stroke, fill, disabled);
    }

    public void setListener(DataUsageChartListener listener) {
        mListener = listener;
    }

    public void bindNetworkStats(NetworkStatsHistory stats) {
        mSeries.bindNetworkStats(stats);
    }

    public void bindNetworkPolicy(NetworkPolicy policy) {
        if (policy == null) {
            mSweepLimit.setVisibility(View.INVISIBLE);
            mSweepWarning.setVisibility(View.INVISIBLE);
            return;
        }

        if (policy.limitBytes != NetworkPolicy.LIMIT_DISABLED) {
            mSweepLimit.setVisibility(View.VISIBLE);
            mSweepLimit.setValue(policy.limitBytes);
            mSweepLimit.setEnabled(true);
        } else {
            // TODO: set limit default based on axis maximum
            mSweepLimit.setVisibility(View.VISIBLE);
            mSweepLimit.setValue(5 * GB_IN_BYTES);
            mSweepLimit.setEnabled(false);
        }

        if (policy.warningBytes != NetworkPolicy.WARNING_DISABLED) {
            mSweepWarning.setVisibility(View.VISIBLE);
            mSweepWarning.setValue(policy.warningBytes);
        } else {
            mSweepWarning.setVisibility(View.INVISIBLE);
        }

        requestLayout();

        // TODO: eventually remove this; was to work around lack of sweep clamping
        if (policy.limitBytes < -1 || policy.limitBytes > 5 * GB_IN_BYTES) {
            policy.limitBytes = 5 * GB_IN_BYTES;
            mLimitListener.onSweep(mSweepLimit, true);
        }
        if (policy.warningBytes < -1 || policy.warningBytes > 5 * GB_IN_BYTES) {
            policy.warningBytes = 4 * GB_IN_BYTES;
            mWarningListener.onSweep(mSweepWarning, true);
        }

    }

    private OnSweepListener mSweepListener = new OnSweepListener() {
        public void onSweep(ChartSweepView sweep, boolean sweepDone) {
            mSeries.setPrimaryRange(mSweepLeft.getValue(), mSweepRight.getValue());

            // update detail list only when done sweeping
            if (sweepDone && mListener != null) {
                mListener.onInspectRangeChanged();
            }
        }
    };

    private OnSweepListener mWarningListener = new OnSweepListener() {
        public void onSweep(ChartSweepView sweep, boolean sweepDone) {
            if (sweepDone && mListener != null) {
                mListener.onWarningChanged();
            }
        }
    };

    private OnSweepListener mLimitListener = new OnSweepListener() {
        public void onSweep(ChartSweepView sweep, boolean sweepDone) {
            if (sweepDone && mListener != null) {
                mListener.onLimitChanged();
            }
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isActivated()) return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                return true;
            }
            case MotionEvent.ACTION_UP: {
                setActivated(true);
                return true;
            }
            default: {
                return false;
            }
        }
    }

    /**
     * Return current inspection range (start and end time) based on internal
     * {@link ChartSweepView} positions.
     */
    public long[] getInspectRange() {
        final long start = mSweepLeft.getValue();
        final long end = mSweepRight.getValue();
        return new long[] { start, end };
    }

    public long getWarningBytes() {
        return mSweepWarning.getValue();
    }

    public long getLimitBytes() {
        return mSweepLimit.getValue();
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

        mSweepLeft.setValue(sweepMin);
        mSweepRight.setValue(sweepMax);
        mSeries.setPrimaryRange(sweepMin, sweepMax);

        requestLayout();
        mSeries.generatePath();
        mSeries.invalidate();
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
        public CharSequence getShortLabel(long value) {
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

            // TODO: use exploded string here


            // TODO: convert to string
            return Long.toString(value);
        }

        /** {@inheritDoc} */
        public CharSequence getShortLabel(long value) {
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
