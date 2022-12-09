/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage;

import android.content.Context;
import android.os.BatteryUsageStats;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.UsageView;

/**
 * Custom preference for displaying the battery level as chart graph.
 */
public class BatteryHistoryPreference extends Preference {
    private static final String TAG = "BatteryHistoryPreference";

    @VisibleForTesting
    boolean mHideSummary;
    @VisibleForTesting
    BatteryInfo mBatteryInfo;

    private boolean mIsChartGraphEnabled;

    private TextView mSummaryView;
    private CharSequence mSummaryContent;
    private BatteryChartView mDailyChartView;
    private BatteryChartView mHourlyChartView;
    private BatteryChartPreferenceController mChartPreferenceController;

    public BatteryHistoryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mIsChartGraphEnabled =
                FeatureFactory.getFactory(context).getPowerUsageFeatureProvider(context)
                        .isChartGraphEnabled(context);
        Log.i(TAG, "isChartGraphEnabled: " + mIsChartGraphEnabled);
        setLayoutResource(
                mIsChartGraphEnabled
                        ? R.layout.battery_chart_graph
                        : R.layout.battery_usage_graph);
        setSelectable(false);
    }

    /** Sets the text of bottom summary. */
    public void setBottomSummary(CharSequence text) {
        mSummaryContent = text;
        if (mSummaryView != null) {
            mSummaryView.setVisibility(View.VISIBLE);
            mSummaryView.setText(mSummaryContent);
        }
        mHideSummary = false;
    }

    /** Hides the bottom summary. */
    public void hideBottomSummary() {
        if (mSummaryView != null) {
            mSummaryView.setVisibility(View.GONE);
        }
        mHideSummary = true;
    }

    void setBatteryUsageStats(@NonNull BatteryUsageStats batteryUsageStats) {
        BatteryInfo.getBatteryInfo(getContext(), info -> {
            mBatteryInfo = info;
            notifyChanged();
        }, batteryUsageStats, false);
    }

    void setChartPreferenceController(BatteryChartPreferenceController controller) {
        mChartPreferenceController = controller;
        if (mDailyChartView != null && mHourlyChartView != null) {
            mChartPreferenceController.setBatteryChartView(mDailyChartView, mHourlyChartView);
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        final long startTime = System.currentTimeMillis();
        if (mBatteryInfo == null) {
            return;
        }
        if (mIsChartGraphEnabled) {
            final TextView companionTextView = (TextView) view.findViewById(R.id.companion_text);
            mDailyChartView = (BatteryChartView) view.findViewById(R.id.daily_battery_chart);
            mDailyChartView.setCompanionTextView(companionTextView);
            mHourlyChartView = (BatteryChartView) view.findViewById(R.id.hourly_battery_chart);
            mHourlyChartView.setCompanionTextView(companionTextView);
            if (mChartPreferenceController != null) {
                mChartPreferenceController.setBatteryChartView(mDailyChartView, mHourlyChartView);
            }
        } else {
            final TextView chargeView = (TextView) view.findViewById(R.id.charge);
            chargeView.setText(mBatteryInfo.batteryPercentString);
            mSummaryView = (TextView) view.findViewById(R.id.bottom_summary);
            if (mSummaryContent != null) {
                mSummaryView.setText(mSummaryContent);
            }
            if (mHideSummary) {
                mSummaryView.setVisibility(View.GONE);
            }
            final UsageView usageView = (UsageView) view.findViewById(R.id.battery_usage);
            usageView.findViewById(R.id.label_group).setAlpha(.7f);
            mBatteryInfo.bindHistory(usageView);
        }
        BatteryUtils.logRuntime(TAG, "onBindViewHolder", startTime);
    }
}
