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
import android.graphics.Color;
import android.net.NetworkPolicy;
import android.net.NetworkStatsHistory;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import com.android.settings.R;
import com.android.settings.widget.ChartDataUsageView;
import com.android.settings.widget.ChartNetworkSeriesView;

public class ChartDataUsagePreference extends Preference {

    private NetworkPolicy mPolicy;
    private long mStart;
    private long mEnd;
    private NetworkStatsHistory mNetwork;
    private int mSecondaryColor;
    private int mSeriesColor;

    public ChartDataUsagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.data_usage_chart);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        ChartDataUsageView chart = (ChartDataUsageView) holder.itemView;
        chart.setVisibleRange(mStart, mEnd);
        chart.bindNetworkPolicy(mPolicy);
        chart.bindNetworkStats(mNetwork);
        ChartNetworkSeriesView series = (ChartNetworkSeriesView) holder.findViewById(R.id.series);
        series.setChartColor(Color.BLACK, mSeriesColor, mSecondaryColor);
    }

    public void bindNetworkPolicy(NetworkPolicy policy) {
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

    public void bindNetworkStats(NetworkStatsHistory network) {
        mNetwork = network;
        notifyChanged();
    }

    public void setColors(int seriesColor, int secondaryColor) {
        mSeriesColor = seriesColor;
        mSecondaryColor = secondaryColor;
        notifyChanged();
    }
}
