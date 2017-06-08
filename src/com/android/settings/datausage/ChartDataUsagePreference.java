/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import android.content.Context;
import android.net.NetworkPolicy;
import android.net.NetworkStatsHistory;
import android.net.TrafficStats;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.graph.UsageView;

public class ChartDataUsagePreference extends Preference {

    // The resolution we show on the graph so that we can squash things down to ints.
    // Set to half a meg for now.
    private static final long RESOLUTION = TrafficStats.MB_IN_BYTES / 2;

    private final int mWarningColor;
    private final int mLimitColor;

    private NetworkPolicy mPolicy;
    private long mStart;
    private long mEnd;
    private NetworkStatsHistory mNetwork;
    private int mSecondaryColor;
    private int mSeriesColor;

    public ChartDataUsagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSelectable(false);
        mLimitColor = Utils.getColorAttr(context, android.R.attr.colorError);
        mWarningColor = Utils.getColorAttr(context, android.R.attr.textColorSecondary);
        setLayoutResource(R.layout.data_usage_graph);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        UsageView chart = (UsageView) holder.findViewById(R.id.data_usage);
        if (mNetwork == null) return;

        int top = getTop();
        chart.clearPaths();
        chart.configureGraph(toInt(mEnd - mStart), top);
        calcPoints(chart);
        chart.setBottomLabels(new CharSequence[] {
                Utils.formatDateRange(getContext(), mStart, mStart),
                Utils.formatDateRange(getContext(), mEnd, mEnd),
        });

        bindNetworkPolicy(chart, mPolicy, top);
    }

    public int getTop() {
        NetworkStatsHistory.Entry entry = null;
        long totalData = 0;
        final int start = mNetwork.getIndexBefore(mStart);
        final int end = mNetwork.getIndexAfter(mEnd);

        for (int i = start; i <= end; i++) {
            entry = mNetwork.getValues(i, entry);

            // increment by current bucket total
            totalData += entry.rxBytes + entry.txBytes;
        }
        long policyMax = mPolicy != null ? Math.max(mPolicy.limitBytes, mPolicy.warningBytes) : 0;
        return (int) (Math.max(totalData, policyMax) / RESOLUTION);
    }

    private void calcPoints(UsageView chart) {
        SparseIntArray points = new SparseIntArray();
        NetworkStatsHistory.Entry entry = null;

        long totalData = 0;

        final int start = mNetwork.getIndexAfter(mStart);
        final int end = mNetwork.getIndexAfter(mEnd);
        if (start < 0) return;

        points.put(0, 0);
        for (int i = start; i <= end; i++) {
            entry = mNetwork.getValues(i, entry);

            final long startTime = entry.bucketStart;
            final long endTime = startTime + entry.bucketDuration;

            // increment by current bucket total
            totalData += entry.rxBytes + entry.txBytes;

            points.put(toInt(startTime - mStart + 1), (int) (totalData / RESOLUTION));
            points.put(toInt(endTime - mStart), (int) (totalData / RESOLUTION));
        }
        if (points.size() > 1) {
            chart.addPath(points);
        }
    }

    private int toInt(long l) {
        // Don't need that much resolution on these times.
        return (int) (l / (1000 * 60));
    }

    private void bindNetworkPolicy(UsageView chart, NetworkPolicy policy, int top) {
        CharSequence[] labels = new CharSequence[3];
        int middleVisibility = 0;
        int topVisibility = 0;
        if (policy == null) {
            return;
        }

        if (policy.limitBytes != NetworkPolicy.LIMIT_DISABLED) {
            topVisibility = mLimitColor;
            labels[2] = getLabel(policy.limitBytes, R.string.data_usage_sweep_limit, mLimitColor);
        }

        if (policy.warningBytes != NetworkPolicy.WARNING_DISABLED) {
            chart.setDividerLoc((int) (policy.warningBytes / RESOLUTION));
            float weight = policy.warningBytes / RESOLUTION / (float) top;
            float above = 1 - weight;
            chart.setSideLabelWeights(above, weight);
            middleVisibility = mWarningColor;
            labels[1] = getLabel(policy.warningBytes, R.string.data_usage_sweep_warning,
                    mWarningColor);
        }

        chart.setSideLabels(labels);
        chart.setDividerColors(middleVisibility, topVisibility);
    }

    private CharSequence getLabel(long bytes, int str, int mLimitColor) {
        Formatter.BytesResult result = Formatter.formatBytes(getContext().getResources(),
                bytes, Formatter.FLAG_SHORTER);
        CharSequence label = TextUtils.expandTemplate(getContext().getText(str),
                result.value, result.units);
        return new SpannableStringBuilder().append(label, new ForegroundColorSpan(mLimitColor), 0);
    }

    public void setNetworkPolicy(NetworkPolicy policy) {
        mPolicy = policy;
        notifyChanged();
    }

    public void setVisibleRange(long start, long end) {
        mStart = start;
        mEnd = end;
        notifyChanged();
    }

    public long getInspectStart() {
        return mStart;
    }

    public long getInspectEnd() {
        return mEnd;
    }

    public void setNetworkStats(NetworkStatsHistory network) {
        mNetwork = network;
        notifyChanged();
    }

    public void setColors(int seriesColor, int secondaryColor) {
        mSeriesColor = seriesColor;
        mSecondaryColor = secondaryColor;
        notifyChanged();
    }
}
