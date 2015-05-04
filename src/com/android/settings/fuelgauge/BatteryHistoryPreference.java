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
import android.os.BatteryStats;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.SettingsActivity;

/**
 * Custom preference for displaying power consumption as a bar and an icon on the left for the
 * subsystem/app type.
 *
 */
public class BatteryHistoryPreference extends Preference {

    protected static final String BATTERY_HISTORY_FILE = "tmp_bat_history.bin";

    private BatteryStats mStats;
    private Intent mBatteryBroadcast;

    private BatteryHistoryChart mChart;
    private BatteryStatsHelper mHelper;

    public BatteryHistoryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void performClick(PreferenceScreen preferenceScreen) {
        if (!isEnabled()) {
            return;
        }
        mHelper.storeStatsHistoryInFile(BATTERY_HISTORY_FILE);
        Bundle args = new Bundle();
        args.putString(BatteryHistoryDetail.EXTRA_STATS, BATTERY_HISTORY_FILE);
        args.putParcelable(BatteryHistoryDetail.EXTRA_BROADCAST,
                mHelper.getBatteryBroadcast());
        if (getContext() instanceof SettingsActivity) {
            SettingsActivity sa = (SettingsActivity) getContext();
            sa.startPreferencePanel(BatteryHistoryDetail.class.getName(), args,
                    R.string.history_details_title, null, null, 0);
        }
    }

    public void setStats(BatteryStatsHelper batteryStats) {
        // Clear out the chart to receive new data.
        mChart = null;
        mHelper = batteryStats;
        mStats = batteryStats.getStats();
        mBatteryBroadcast = batteryStats.getBatteryBroadcast();
        if (getLayoutResource() != R.layout.battery_history_chart) {
            // Now we should have some data, set the layout we want.
            setLayoutResource(R.layout.battery_history_chart);
        }
        notifyChanged();
    }

    BatteryStats getStats() {
        return mStats;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        if (mStats == null) {
            return;
        }
        BatteryHistoryChart chart = (BatteryHistoryChart) view.findViewById(
                R.id.battery_history_chart);
        if (mChart == null) {
            // First time: use and initialize this chart.
            chart.setStats(mStats, mBatteryBroadcast);
            mChart = chart;
        } else {
            // All future times: forget the newly inflated chart, re-use the
            // already initialized chart from last time.
            ViewGroup parent = (ViewGroup) chart.getParent();
            int index = parent.indexOfChild(chart);
            parent.removeViewAt(index);
            if (mChart.getParent() != null) {
                ((ViewGroup) mChart.getParent()).removeView(mChart);
            }
            parent.addView(mChart, index);
        }
    }
}
