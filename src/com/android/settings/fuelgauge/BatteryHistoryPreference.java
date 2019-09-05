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
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.widget.UsageView;

/**
 * Custom preference for displaying power consumption as a bar and an icon on the left for the
 * subsystem/app type.
 */
public class BatteryHistoryPreference extends Preference {
    private static final String TAG = "BatteryHistoryPreference";

    private CharSequence mSummary;
    private TextView mSummaryView;

    @VisibleForTesting
    boolean hideSummary;
    @VisibleForTesting
    BatteryInfo mBatteryInfo;

    public BatteryHistoryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.battery_usage_graph);
        setSelectable(false);
    }

    public void setStats(BatteryStatsHelper batteryStats) {
        BatteryInfo.getBatteryInfo(getContext(), info -> {
            mBatteryInfo = info;
            notifyChanged();
        }, batteryStats, false);
    }

    public void setBottomSummary(CharSequence text) {
        mSummary = text;
        if (mSummaryView != null) {
            mSummaryView.setVisibility(View.VISIBLE);
            mSummaryView.setText(mSummary);
        }
        hideSummary = false;
    }

    public void hideBottomSummary() {
        if (mSummaryView != null) {
            mSummaryView.setVisibility(View.GONE);
        }
        hideSummary = true;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        final long startTime = System.currentTimeMillis();
        if (mBatteryInfo == null) {
            return;
        }

        ((TextView) view.findViewById(R.id.charge)).setText(mBatteryInfo.batteryPercentString);
        mSummaryView = (TextView) view.findViewById(R.id.bottom_summary);
        if (mSummary != null) {
            mSummaryView.setText(mSummary);
        }
        if (hideSummary) {
            mSummaryView.setVisibility(View.GONE);
        }
        UsageView usageView = (UsageView) view.findViewById(R.id.battery_usage);
        usageView.findViewById(R.id.label_group).setAlpha(.7f);
        mBatteryInfo.bindHistory(usageView);
        BatteryUtils.logRuntime(TAG, "onBindViewHolder", startTime);
    }
}
