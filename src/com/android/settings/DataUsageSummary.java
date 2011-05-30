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

package com.android.settings;

import static com.android.settings.widget.ChartView.buildChartParams;
import static com.android.settings.widget.ChartView.buildSweepParams;

import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.INetworkStatsService;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.settings.widget.ChartAxis;
import com.android.settings.widget.ChartGridView;
import com.android.settings.widget.ChartNetworkSeriesView;
import com.android.settings.widget.ChartSweepView;
import com.android.settings.widget.ChartSweepView.OnSweepListener;
import com.android.settings.widget.ChartView;
import com.android.settings.widget.InvertedChartAxis;
import com.google.android.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;

public class DataUsageSummary extends Fragment {
    private static final String TAG = "DataUsage";

    // TODO: teach about wifi-vs-mobile data with tabs

    private static final long KB_IN_BYTES = 1024;
    private static final long MB_IN_BYTES = KB_IN_BYTES * 1024;
    private static final long GB_IN_BYTES = MB_IN_BYTES * 1024;

    private INetworkStatsService mStatsService;

    private ViewGroup mChartContainer;
    private ListView mList;

    private ChartAxis mAxisTime;
    private ChartAxis mAxisData;

    private ChartView mChart;
    private ChartNetworkSeriesView mSeries;

    private ChartSweepView mSweepTime1;
    private ChartSweepView mSweepTime2;
    private ChartSweepView mSweepDataWarn;
    private ChartSweepView mSweepDataLimit;

    private DataUsageAdapter mAdapter;

    // TODO: persist warning/limit into policy service
    private static final long DATA_WARN = (long) 3.2 * GB_IN_BYTES;
    private static final long DATA_LIMIT = (long) 4.8 * GB_IN_BYTES;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final Context context = inflater.getContext();
        final long now = System.currentTimeMillis();

        mStatsService = INetworkStatsService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_STATS_SERVICE));

        mAxisTime = new TimeAxis();
        mAxisData = new InvertedChartAxis(new DataAxis());

        mChart = new ChartView(context, mAxisTime, mAxisData);
        mChart.setPadding(20, 20, 20, 20);

        mChart.addView(new ChartGridView(context, mAxisTime, mAxisData), buildChartParams());

        mSeries = new ChartNetworkSeriesView(context, mAxisTime, mAxisData);
        mChart.addView(mSeries, buildChartParams());

        mSweepTime1 = new ChartSweepView(context, mAxisTime, now - DateUtils.DAY_IN_MILLIS * 14,
                Color.parseColor("#ffffff"));
        mSweepTime2 = new ChartSweepView(context, mAxisTime, now - DateUtils.DAY_IN_MILLIS * 7,
                Color.parseColor("#ffffff"));
        mSweepDataWarn = new ChartSweepView(
                context, mAxisData, DATA_WARN, Color.parseColor("#f7931d"));
        mSweepDataLimit = new ChartSweepView(
                context, mAxisData, DATA_LIMIT, Color.parseColor("#be1d2c"));

        mChart.addView(mSweepTime1, buildSweepParams());
        mChart.addView(mSweepTime2, buildSweepParams());
        mChart.addView(mSweepDataWarn, buildSweepParams());
        mChart.addView(mSweepDataLimit, buildSweepParams());

        mSeries.bindSweepRange(mSweepTime1, mSweepTime2);

        mSweepTime1.addOnSweepListener(mSweepListener);
        mSweepTime2.addOnSweepListener(mSweepListener);

        mAdapter = new DataUsageAdapter();

        final View view = inflater.inflate(R.layout.data_usage_summary, container, false);

        mChartContainer = (ViewGroup) view.findViewById(R.id.chart_container);
        mChartContainer.addView(mChart);

        mList = (ListView) view.findViewById(R.id.list);
        mList.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        updateSummaryData();
        updateDetailData();

    }

    private void updateSummaryData() {
        try {
            final NetworkStatsHistory history = mStatsService.getHistoryForNetwork(
                    TrafficStats.TEMPLATE_MOBILE_ALL);
            mSeries.bindNetworkStats(history);
        } catch (RemoteException e) {
            Log.w(TAG, "problem reading stats");
        }
    }

    private void updateDetailData() {
        final long sweep1 = mSweepTime1.getValue();
        final long sweep2 = mSweepTime2.getValue();

        final long start = Math.min(sweep1, sweep2);
        final long end = Math.max(sweep1, sweep2);

        try {
            final NetworkStats stats = mStatsService.getSummaryForAllUid(
                    start, end, TrafficStats.TEMPLATE_MOBILE_ALL);
            mAdapter.bindStats(stats);
        } catch (RemoteException e) {
            Log.w(TAG, "problem reading stats");
        }
    }

    private OnSweepListener mSweepListener = new OnSweepListener() {
        public void onSweep(ChartSweepView sweep, boolean sweepDone) {
            // always update graph clip region
            mSeries.invalidate();

            // update detail list only when done sweeping
            if (sweepDone) {
                updateDetailData();
            }
        }
    };


    /**
     * Adapter of applications, sorted by total usage descending.
     */
    public static class DataUsageAdapter extends BaseAdapter {
        private ArrayList<UsageRecord> mData = Lists.newArrayList();

        private static class UsageRecord implements Comparable<UsageRecord> {
            public int uid;
            public long total;

            /** {@inheritDoc} */
            public int compareTo(UsageRecord another) {
                return Long.compare(another.total, total);
            }
        }

        public void bindStats(NetworkStats stats) {
            mData.clear();

            for (int i = 0; i < stats.length(); i++) {
                final UsageRecord record = new UsageRecord();
                record.uid = stats.uid[i];
                record.total = stats.rx[i] + stats.tx[i];
                mData.add(record);
            }

            Collections.sort(mData);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(
                        android.R.layout.simple_list_item_2, parent, false);
            }

            final Context context = parent.getContext();
            final PackageManager pm = context.getPackageManager();

            final TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            final TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

            final UsageRecord record = mData.get(position);
            text1.setText(pm.getNameForUid(record.uid));
            text2.setText(Formatter.formatFileSize(context, record.total));

            return convertView;
        }

    }


    public static class TimeAxis implements ChartAxis {
        private static final long TICK_INTERVAL = DateUtils.DAY_IN_MILLIS * 7;

        private long mMin;
        private long mMax;
        private float mSize;

        public TimeAxis() {
            // TODO: hook up these ranges to policy service
            mMax = System.currentTimeMillis();
            mMin = mMax - DateUtils.DAY_IN_MILLIS * 30;
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

    // TODO: make data axis log-scale

    public static class DataAxis implements ChartAxis {
        private long mMin;
        private long mMax;
        private float mSize;

        public DataAxis() {
            // TODO: adapt ranges to show when history >5GB, and handle 4G
            // interfaces with higher limits.
            mMin = 0;
            mMax = 5 * GB_IN_BYTES;
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
