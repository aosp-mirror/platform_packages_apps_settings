/*
 * Copyright (C) 2021 The Android Open Source Project
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
 *
 *
 */

package com.android.settings.fuelgauge;

import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnPause;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Controls the update for chart graph and the list items. */
public class BatteryChartPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnPause, OnDestroy,
                BatteryChartView.OnSelectListener {
    private static final String TAG = "BatteryChartPreferenceController";
    private static final int CHART_KEY_ARRAY_SIZE = 25;
    private static final int CHART_LEVEL_ARRAY_SIZE = 13;

    @VisibleForTesting
    Map<Integer, List<BatteryDiffEntry>> mBatteryIndexedMap;

    @VisibleForTesting Context mPrefContext;
    @VisibleForTesting PreferenceGroup mAppListPrefGroup;
    @VisibleForTesting BatteryChartView mBatteryChartView;

    @VisibleForTesting int[] mBatteryHistoryLevels;
    @VisibleForTesting long[] mBatteryHistoryKeys;
    @VisibleForTesting int mTrapezoidIndex = BatteryChartView.SELECTED_INDEX_INVALID;

    private final String mPreferenceKey;
    private final SettingsActivity mActivity;
    private final InstrumentedPreferenceFragment mFragment;

    public BatteryChartPreferenceController(
            Context context, String preferenceKey,
            Lifecycle lifecycle, SettingsActivity activity,
            InstrumentedPreferenceFragment fragment) {
        super(context);
        mActivity = activity;
        mFragment = fragment;
        mPreferenceKey = preferenceKey;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onDestroy() {
        if (mActivity.isChangingConfigurations()) {
            BatteryDiffEntry.clearCache();
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPrefContext = screen.getContext();
        mAppListPrefGroup = screen.findPreference(mPreferenceKey);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return mPreferenceKey;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public void onSelect(int trapezoidIndex) {
        Log.d(TAG, "onChartSelect:" + trapezoidIndex);
        refreshUi(trapezoidIndex, /*isForce=*/ false);
    }

    void setBatteryHistoryMap(Map<Long, List<BatteryHistEntry>> batteryHistoryMap) {
        // Resets all battery history data relative variables.
        if (batteryHistoryMap == null) {
            mBatteryIndexedMap = null;
            mBatteryHistoryKeys = null;
            mBatteryHistoryLevels = null;
            return;
        }
        // Generates battery history keys.
        final List<Long> batteryHistoryKeyList =
            new ArrayList<Long>(batteryHistoryMap.keySet());
        Collections.sort(batteryHistoryKeyList);
        mBatteryHistoryKeys = new long[CHART_KEY_ARRAY_SIZE];
        final int elementSize = Math.min(batteryHistoryKeyList.size(), CHART_KEY_ARRAY_SIZE);
        final int offset = CHART_KEY_ARRAY_SIZE - elementSize;
        for (int index = 0; index < elementSize; index++) {
            mBatteryHistoryKeys[index + offset] = batteryHistoryKeyList.get(index);
        }

        // Generates the battery history levels.
        mBatteryHistoryLevels = new int[CHART_LEVEL_ARRAY_SIZE];
        for (int index = 0; index < CHART_LEVEL_ARRAY_SIZE; index++) {
            final Long timestamp = Long.valueOf(mBatteryHistoryKeys[index * 2]);
            final List<BatteryHistEntry> entryList = batteryHistoryMap.get(timestamp);
            if (entryList != null && !entryList.isEmpty()) {
                // All battery levels are the same in the same timestamp snapshot.
                mBatteryHistoryLevels[index] = entryList.get(0).mBatteryLevel;
            } else if (entryList != null && entryList.isEmpty()) {
                Log.e(TAG, "abnormal entry list in the timestamp:" +
                    ConvertUtils.utcToLocalTime(timestamp));
            }
        }
        // Generates indexed usage map for chart.
        mBatteryIndexedMap =
            ConvertUtils.getIndexedUsageMap(
                mPrefContext, /*timeSlotSize=*/ CHART_LEVEL_ARRAY_SIZE - 1,
                mBatteryHistoryKeys, batteryHistoryMap,
                /*purgeLowPercentageData=*/ true);
        forceRefreshUi();

        Log.d(TAG, String.format(
            "setBatteryHistoryMap() size=%d\nkeys=%s\nlevels=%s",
            batteryHistoryKeyList.size(),
            utcToLocalTime(mBatteryHistoryKeys),
            Arrays.toString(mBatteryHistoryLevels)));
    }

    void setBatteryChartView(BatteryChartView batteryChartView) {
        mBatteryChartView = batteryChartView;
        mBatteryChartView.setOnSelectListener(this);
        forceRefreshUi();
    }

    private void forceRefreshUi() {
        final int refreshIndex =
            mTrapezoidIndex == BatteryChartView.SELECTED_INDEX_INVALID
                ? BatteryChartView.SELECTED_INDEX_ALL
                : mTrapezoidIndex;
        refreshUi(refreshIndex, /*isForce=*/ true);
    }

    @VisibleForTesting
    boolean refreshUi(int trapezoidIndex, boolean isForce) {
        // Invalid refresh condition.
        if (mBatteryIndexedMap == null
                || mBatteryChartView == null
                || (mTrapezoidIndex == trapezoidIndex && !isForce)) {
            return false;
        }
        mTrapezoidIndex = trapezoidIndex;
        Log.d(TAG, String.format("refreshUi: index=%d batteryIndexedMap.size=%d",
            mTrapezoidIndex, mBatteryIndexedMap.size()));
        return true;
    }

    private static String utcToLocalTime(long[] timestamps) {
        final StringBuilder builder = new StringBuilder();
        for (int index = 0; index < timestamps.length; index++) {
            builder.append(String.format("%s| ",
                  ConvertUtils.utcToLocalTime(timestamps[index])));
        }
        return builder.toString();
    }
}
