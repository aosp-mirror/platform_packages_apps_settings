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
import android.content.res.Resources;
import android.net.NetworkPolicy;
import android.net.NetworkStatsHistory;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
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

    private ChartGridView mGrid;
    private ChartNetworkSeriesView mSeries;
    private ChartNetworkSeriesView mDetailSeries;

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
        mDetailSeries = (ChartNetworkSeriesView) findViewById(R.id.detail_series);
        mDetailSeries.setVisibility(View.GONE);

        mSweepLeft = (ChartSweepView) findViewById(R.id.sweep_left);
        mSweepRight = (ChartSweepView) findViewById(R.id.sweep_right);
        mSweepLimit = (ChartSweepView) findViewById(R.id.sweep_limit);
        mSweepWarning = (ChartSweepView) findViewById(R.id.sweep_warning);

        // prevent sweeps from crossing each other
        mSweepLeft.setClampBefore(mSweepRight);
        mSweepRight.setClampAfter(mSweepLeft);
        mSweepLimit.setClampBefore(mSweepWarning);
        mSweepWarning.setClampAfter(mSweepLimit);

        mSweepLeft.addOnSweepListener(mSweepListener);
        mSweepRight.addOnSweepListener(mSweepListener);
        mSweepWarning.addOnSweepListener(mWarningListener);
        mSweepLimit.addOnSweepListener(mLimitListener);

        // tell everyone about our axis
        mGrid.init(mHoriz, mVert);
        mSeries.init(mHoriz, mVert);
        mDetailSeries.init(mHoriz, mVert);
        mSweepLeft.init(mHoriz);
        mSweepRight.init(mHoriz);
        mSweepWarning.init(mVert);
        mSweepLimit.init(mVert);

        setActivated(false);
    }

    public void setListener(DataUsageChartListener listener) {
        mListener = listener;
    }

    public void bindNetworkStats(NetworkStatsHistory stats) {
        mSeries.bindNetworkStats(stats);
        updatePrimaryRange();
        requestLayout();
    }

    public void bindDetailNetworkStats(NetworkStatsHistory stats) {
        mDetailSeries.bindNetworkStats(stats);
        mDetailSeries.setVisibility(stats != null ? View.VISIBLE : View.GONE);
        updatePrimaryRange();
        requestLayout();
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
            mSweepLimit.setVisibility(View.VISIBLE);
            mSweepLimit.setEnabled(false);
        }

        if (policy.warningBytes != NetworkPolicy.WARNING_DISABLED) {
            mSweepWarning.setVisibility(View.VISIBLE);
            mSweepWarning.setValue(policy.warningBytes);
        } else {
            mSweepWarning.setVisibility(View.INVISIBLE);
        }

        requestLayout();
    }

    private OnSweepListener mSweepListener = new OnSweepListener() {
        public void onSweep(ChartSweepView sweep, boolean sweepDone) {
            updatePrimaryRange();

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

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isActivated()) {
            return true;
        } else {
            return super.onInterceptTouchEvent(ev);
        }
    }

    public long getInspectStart() {
        return mSweepLeft.getValue();
    }

    public long getInspectEnd() {
        return mSweepRight.getValue();
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
        updatePrimaryRange();

        requestLayout();
        mSeries.generatePath();
        mSeries.invalidate();
    }

    private void updatePrimaryRange() {
        final long left = mSweepLeft.getValue();
        final long right = mSweepRight.getValue();

        // prefer showing primary range on detail series, when available
        if (mDetailSeries.getVisibility() == View.VISIBLE) {
            mDetailSeries.setPrimaryRange(left, right);
            mSeries.setPrimaryRange(0, 0);
        } else {
            mSeries.setPrimaryRange(left, right);
        }
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
        public void buildLabel(Resources res, SpannableStringBuilder builder, long value) {
            // TODO: convert to better string
            builder.replace(0, builder.length(), Long.toString(value));
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
        private float mSize;

        public DataAxis() {
            // TODO: adapt ranges to show when history >5GB, and handle 4G
            // interfaces with higher limits.
            setBounds(0, 5 * GB_IN_BYTES);
        }

        /** {@inheritDoc} */
        public void setBounds(long min, long max) {
            mMin = min;
            mMax = max;
        }

        /** {@inheritDoc} */
        public void setSize(float size) {
            mSize = size;
        }

        /** {@inheritDoc} */
        public float convertToPoint(long value) {
            // TODO: this assumes range of [0,5]GB
            final double fraction = Math.pow(
                    10, 0.36884343106175160321 * Math.log10(value) + -3.62828151137812282556);
            return (float) fraction * mSize;
        }

        /** {@inheritDoc} */
        public long convertToValue(float point) {
            final double y = point / mSize;
            // TODO: this assumes range of [0,5]GB
            final double fraction = 6.869341163271789302 * Math.pow(10, 9)
                    * Math.pow(y, 2.71117746931646030774);
            return (long) fraction;
        }

        private static final Object sSpanSize = new Object();
        private static final Object sSpanUnit = new Object();

        /** {@inheritDoc} */
        public void buildLabel(Resources res, SpannableStringBuilder builder, long value) {

            float result = value;
            final CharSequence unit;
            if (result <= 100 * MB_IN_BYTES) {
                unit = res.getText(com.android.internal.R.string.megabyteShort);
                result /= MB_IN_BYTES;
            } else {
                unit = res.getText(com.android.internal.R.string.gigabyteShort);
                result /= GB_IN_BYTES;
            }

            final CharSequence size;
            if (result < 10) {
                size = String.format("%.1f", result);
            } else {
                size = String.format("%.0f", result);
            }

            final int[] sizeBounds = findOrCreateSpan(builder, sSpanSize, "^1");
            builder.replace(sizeBounds[0], sizeBounds[1], size);
            final int[] unitBounds = findOrCreateSpan(builder, sSpanUnit, "^2");
            builder.replace(unitBounds[0], unitBounds[1], unit);
        }

        /** {@inheritDoc} */
        public float[] getTickPoints() {
            final float[] tickPoints = new float[16];

            final long jump = ((mMax - mMin) / tickPoints.length);
            long value = mMin;
            for (int i = 0; i < tickPoints.length; i++) {
                tickPoints[i] = convertToPoint(value);
                value += jump;
            }

            return tickPoints;
        }
    }

    private static int[] findOrCreateSpan(
            SpannableStringBuilder builder, Object key, CharSequence bootstrap) {
        int start = builder.getSpanStart(key);
        int end = builder.getSpanEnd(key);
        if (start == -1) {
            start = TextUtils.indexOf(builder, bootstrap);
            end = start + bootstrap.length();
            builder.setSpan(key, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        return new int[] { start, end };
    }

}
