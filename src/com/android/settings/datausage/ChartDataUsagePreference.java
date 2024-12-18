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
import android.content.res.Resources;
import android.net.NetworkPolicy;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.DataUnit;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.datausage.lib.NetworkCycleChartData;
import com.android.settings.datausage.lib.NetworkUsageData;
import com.android.settings.widget.UsageView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChartDataUsagePreference extends Preference {

    // The resolution we show on the graph so that we can squash things down to ints.
    // Set to half a meg for now.
    private static final long RESOLUTION = DataUnit.MEBIBYTES.toBytes(1) / 2;

    private final int mWarningColor;
    private final int mLimitColor;

    private final Resources mResources;
    @Nullable private NetworkPolicy mPolicy;
    private long mStart;
    private long mEnd;
    private NetworkCycleChartData mNetworkCycleChartData;

    public ChartDataUsagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mResources = context.getResources();
        setSelectable(false);
        mLimitColor = Utils.getColorAttrDefaultColor(context, android.R.attr.colorError);
        mWarningColor = Utils.getColorAttrDefaultColor(context, android.R.attr.textColorSecondary);
        setLayoutResource(R.layout.data_usage_graph);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final UsageView chart = holder.itemView.requireViewById(R.id.data_usage);
        final int top = getTop();
        chart.clearPaths();
        chart.configureGraph(toInt(mEnd - mStart), top);
        if (mNetworkCycleChartData != null) {
            calcPoints(chart, mNetworkCycleChartData.getDailyUsage());
            setupContentDescription(chart, mNetworkCycleChartData.getDailyUsage());
        }
        chart.setBottomLabels(new CharSequence[] {
                Utils.formatDateRange(getContext(), mStart, mStart),
                Utils.formatDateRange(getContext(), mEnd, mEnd),
        });

        bindNetworkPolicy(chart, mPolicy, top);
    }

    public int getTop() {
        final long totalData =
                mNetworkCycleChartData != null ? mNetworkCycleChartData.getTotal().getUsage() : 0;
        final long policyMax =
            mPolicy != null ? Math.max(mPolicy.limitBytes, mPolicy.warningBytes) : 0;
        return (int) (Math.max(totalData, policyMax) / RESOLUTION);
    }

    @VisibleForTesting
    void calcPoints(UsageView chart, @NonNull List<NetworkUsageData> usageSummary) {
        final SparseIntArray points = new SparseIntArray();
        points.put(0, 0);

        final long now = System.currentTimeMillis();
        long totalData = 0;
        for (NetworkUsageData data : usageSummary) {
            final long startTime = data.getStartTime();
            if (startTime > now) {
                break;
            }
            final long endTime = data.getEndTime();

            // increment by current bucket total
            totalData += data.getUsage();

            points.put(toInt(startTime - mStart + 1), (int) (totalData / RESOLUTION));
            points.put(toInt(endTime - mStart), (int) (totalData / RESOLUTION));
        }
        if (points.size() > 1) {
            chart.addPath(points);
        }
    }

    private void setupContentDescription(
            UsageView chart, @NonNull List<NetworkUsageData> usageSummary) {
        final Context context = getContext();
        final StringBuilder contentDescription = new StringBuilder();
        final int flags = DateUtils.FORMAT_SHOW_DATE;

        // Setup a brief content description.
        final String startDate = DateUtils.formatDateTime(context, mStart, flags);
        final String endDate = DateUtils.formatDateTime(context, mEnd, flags);
        final String briefContentDescription = mResources
                .getString(R.string.data_usage_chart_brief_content_description, startDate, endDate);
        contentDescription.append(briefContentDescription);

        if (usageSummary.isEmpty()) {
            final String noDataContentDescription = mResources
                    .getString(R.string.data_usage_chart_no_data_content_description);
            contentDescription.append(noDataContentDescription);
            chart.setContentDescription(contentDescription);
            return;
        }

        // Append more detailed stats.
        String nodeDate;
        String nodeContentDescription;
        final List<DataUsageSummaryNode> densedStatsData = getDensedStatsData(usageSummary);
        for (DataUsageSummaryNode data : densedStatsData) {
            final int dataUsagePercentage = data.getDataUsagePercentage();
            if (!data.isFromMultiNode() || dataUsagePercentage == 100) {
                nodeDate = DateUtils.formatDateTime(context, data.getStartTime(), flags);
            } else {
                nodeDate = DateUtils.formatDateRange(context, data.getStartTime(),
                        data.getEndTime(), flags);
            }
            nodeContentDescription = String.format("; %s, %d%%", nodeDate, dataUsagePercentage);

            contentDescription.append(nodeContentDescription);
        }

        chart.setContentDescription(contentDescription);
    }

    /**
     * To avoid wordy data, e.g., Aug 2: 0%; Aug 3: 0%;...Aug 22: 0%; Aug 23: 2%.
     * Collect the date of the same percentage, e.g., Aug 2 to Aug 22: 0%; Aug 23: 2%.
     */
    @VisibleForTesting
    List<DataUsageSummaryNode> getDensedStatsData(@NonNull List<NetworkUsageData> usageSummary) {
        final List<DataUsageSummaryNode> dataUsageSummaryNodes = new ArrayList<>();
        final long overallDataUsage = Math.max(1L, usageSummary.stream()
                .mapToLong(NetworkUsageData::getUsage).sum());
        long cumulatedDataUsage = 0L;

        // Collect List of DataUsageSummaryNode for data usage percentage information.
        for (NetworkUsageData data : usageSummary) {
            cumulatedDataUsage += data.getUsage();
            int cumulatedDataUsagePercentage =
                    (int) ((cumulatedDataUsage * 100) / overallDataUsage);

            final DataUsageSummaryNode node = new DataUsageSummaryNode(data.getStartTime(),
                    data.getEndTime(), cumulatedDataUsagePercentage);
            dataUsageSummaryNodes.add(node);
        }

        // Group nodes of the same data usage percentage.
        final Map<Integer, List<DataUsageSummaryNode>> nodesByDataUsagePercentage
                = dataUsageSummaryNodes.stream().collect(
                        Collectors.groupingBy(DataUsageSummaryNode::getDataUsagePercentage));

        // Collect densed nodes from collection of the same  data usage percentage
        final List<DataUsageSummaryNode> densedNodes = new ArrayList<>();
        nodesByDataUsagePercentage.forEach((percentage, nodes) -> {
            final long startTime = nodes.stream().mapToLong(DataUsageSummaryNode::getStartTime)
                    .min().getAsLong();
            final long endTime = nodes.stream().mapToLong(DataUsageSummaryNode::getEndTime)
                    .max().getAsLong();

            final DataUsageSummaryNode densedNode = new DataUsageSummaryNode(
                    startTime, endTime, percentage);
            if (nodes.size() > 1) {
                densedNode.setFromMultiNode(true /* isFromMultiNode */);
            }

            densedNodes.add(densedNode);
        });

        return densedNodes.stream()
                .sorted(Comparator.comparingInt(DataUsageSummaryNode::getDataUsagePercentage))
                .collect(Collectors.toList());
    }

    @VisibleForTesting
    class DataUsageSummaryNode {
        private long mStartTime;
        private long mEndTime;
        private int mDataUsagePercentage;
        private boolean mIsFromMultiNode;

        public DataUsageSummaryNode(long startTime, long endTime, int dataUsagePercentage) {
            mStartTime = startTime;
            mEndTime = endTime;
            mDataUsagePercentage = dataUsagePercentage;
            mIsFromMultiNode = false;
        }

        public long getStartTime() {
            return mStartTime;
        }

        public long getEndTime() {
            return mEndTime;
        }

        public int getDataUsagePercentage() {
            return mDataUsagePercentage;
        }

        public void setFromMultiNode(boolean isFromMultiNode) {
            mIsFromMultiNode = isFromMultiNode;
        }

        public boolean isFromMultiNode() {
            return mIsFromMultiNode;
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
            int dividerLoc = (int) (policy.warningBytes / RESOLUTION);
            chart.setDividerLoc(dividerLoc);
            float weight = dividerLoc / (float) top;
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
        Formatter.BytesResult result = Formatter.formatBytes(mResources, bytes,
                Formatter.FLAG_SHORTER | Formatter.FLAG_IEC_UNITS);
        CharSequence label = TextUtils.expandTemplate(getContext().getText(str),
                result.value, result.units);
        return new SpannableStringBuilder().append(label, new ForegroundColorSpan(mLimitColor), 0);
    }

    /** Sets network policy. */
    public void setNetworkPolicy(@Nullable NetworkPolicy policy) {
        mPolicy = policy;
        notifyChanged();
    }

    /** Sets time. */
    public void setTime(long start, long end) {
        mStart = start;
        mEnd = end;
        notifyChanged();
    }

    public void setNetworkCycleData(NetworkCycleChartData data) {
        mNetworkCycleChartData = data;
        notifyChanged();
    }
}
