/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.BatteryStats;
import android.preference.Preference;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;

/**
 * Custom preference for displaying power consumption as a bar and an icon on the left for the
 * subsystem/app type.
 *
 */
public class BatteryHistoryPreference extends Preference {

    final private BatteryStats mStats;
    final private Intent mBatteryBroadcast;

    private boolean mHideLabels;
    private View mLabelHeader;
    private BatteryHistoryChart mChart;

    public BatteryHistoryPreference(Context context, BatteryStats stats, Intent batteryBroadcast) {
        super(context);
        setLayoutResource(R.layout.preference_batteryhistory);
        mStats = stats;
        mBatteryBroadcast = batteryBroadcast;
    }

    BatteryStats getStats() {
        return mStats;
    }

    public void setHideLabels(boolean hide) {
        if (mHideLabels != hide) {
            mHideLabels = hide;
            if (mLabelHeader != null) {
                mLabelHeader.setVisibility(hide ? View.GONE : View.VISIBLE);
            }
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        BatteryHistoryChart chart = (BatteryHistoryChart)view.findViewById(
                R.id.battery_history_chart);
        if (mChart == null) {
            // First time: use and initialize this chart.
            chart.setStats(mStats, mBatteryBroadcast);
            mChart = chart;
        } else {
            // All future times: forget the newly inflated chart, re-use the
            // already initialized chart from last time.
            ViewGroup parent = (ViewGroup)chart.getParent();
            int index = parent.indexOfChild(chart);
            parent.removeViewAt(index);
            if (mChart.getParent() != null) {
                ((ViewGroup)mChart.getParent()).removeView(mChart);
            }
            parent.addView(mChart, index);
        }
        mLabelHeader = view.findViewById(R.id.labelsHeader);
        mLabelHeader.setVisibility(mHideLabels ? View.GONE : View.VISIBLE);
    }
}
