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
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
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
import java.util.HashMap;
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
    @VisibleForTesting BatteryUtils mBatteryUtils;
    @VisibleForTesting PreferenceGroup mAppListPrefGroup;
    @VisibleForTesting BatteryChartView mBatteryChartView;

    @VisibleForTesting int[] mBatteryHistoryLevels;
    @VisibleForTesting long[] mBatteryHistoryKeys;
    @VisibleForTesting int mTrapezoidIndex = BatteryChartView.SELECTED_INDEX_INVALID;

    private final String mPreferenceKey;
    private final SettingsActivity mActivity;
    private final InstrumentedPreferenceFragment mFragment;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    // Preference cache to avoid create new instance each time.
    @VisibleForTesting
    final Map<String, Preference> mPreferenceCache = new HashMap<>();

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
        mHandler.removeCallbacksAndMessages(/*token=*/ null);
        mPreferenceCache.clear();
        if (mAppListPrefGroup != null) {
            mAppListPrefGroup.removeAll();
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPrefContext = screen.getContext();
        mAppListPrefGroup = screen.findPreference(mPreferenceKey);
        mAppListPrefGroup.setOrderingAsAdded(false);
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
        if (!(preference instanceof PowerGaugePreference)) {
            return false;
        }
        final PowerGaugePreference powerPref = (PowerGaugePreference) preference;
        final BatteryDiffEntry diffEntry = powerPref.getBatteryDiffEntry();
        final BatteryHistEntry histEntry = diffEntry.mBatteryHistEntry;
        // Checks whether the package is installed or not.
        boolean isValidPackage = true;
        if (histEntry.isAppEntry()) {
            if (mBatteryUtils == null) {
                mBatteryUtils = BatteryUtils.getInstance(mPrefContext);
            }
            isValidPackage = mBatteryUtils.getPackageUid(histEntry.mPackageName)
                != BatteryUtils.UID_NULL;
        }
        Log.d(TAG, String.format("handleClick() label=%s key=%s isValid:%b",
            diffEntry.getAppLabel(), histEntry.getKey(), isValidPackage));
        if (isValidPackage) {
            AdvancedPowerUsageDetail.startBatteryDetailPage(
                mActivity, mFragment, diffEntry, powerPref.getPercent());
            return true;
        }
        return false;
    }

    @Override
    public void onSelect(int trapezoidIndex) {
        Log.d(TAG, "onChartSelect:" + trapezoidIndex);
        refreshUi(trapezoidIndex, /*isForce=*/ false);
    }

    void setBatteryHistoryMap(
            final Map<Long, List<BatteryHistEntry>> batteryHistoryMap) {
        mHandler.post(() -> setBatteryHistoryMapInner(batteryHistoryMap));
    }

    private void setBatteryHistoryMapInner(
            final Map<Long, List<BatteryHistEntry>> batteryHistoryMap) {
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

    void setBatteryChartView(final BatteryChartView batteryChartView) {
        mHandler.post(() -> setBatteryChartViewInner(batteryChartView));
    }

    private void setBatteryChartViewInner(final BatteryChartView batteryChartView) {
        mBatteryChartView = batteryChartView;
        mBatteryChartView.setOnSelectListener(this);
        forceRefreshUi();
    }

    private void forceRefreshUi() {
        final int refreshIndex =
            mTrapezoidIndex == BatteryChartView.SELECTED_INDEX_INVALID
                ? BatteryChartView.SELECTED_INDEX_ALL
                : mTrapezoidIndex;
        if (mBatteryChartView != null) {
            mBatteryChartView.setLevels(mBatteryHistoryLevels);
        }
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
        Log.d(TAG, String.format("refreshUi: index=%d batteryIndexedMap.size=%d",
            mTrapezoidIndex, mBatteryIndexedMap.size()));

        mTrapezoidIndex = trapezoidIndex;
        mHandler.post(() -> {
            removeAndCacheAllPrefs();
            addAllPreferences();
        });
        return true;
    }

    private void addAllPreferences() {
        final List<BatteryDiffEntry> entries =
            mBatteryIndexedMap.get(Integer.valueOf(mTrapezoidIndex));
        if (entries == null) {
            Log.w(TAG, "cannot find BatteryDiffEntry for:" + mTrapezoidIndex);
            return;
        }
        // Separates data into two groups and sort them individually.
        final List<BatteryDiffEntry> appEntries = new ArrayList<>();
        final List<BatteryDiffEntry> systemEntries = new ArrayList<>();
        entries.forEach(entry -> {
            if (entry.isSystemEntry()) {
                systemEntries.add(entry);
            } else {
                appEntries.add(entry);
            }
        });
        Collections.sort(appEntries, BatteryDiffEntry.COMPARATOR);
        Collections.sort(systemEntries, BatteryDiffEntry.COMPARATOR);
        Log.d(TAG, String.format("addAllPreferences() app=%d system=%d",
            appEntries.size(), systemEntries.size()));
        addPreferenceToScreen(appEntries);
        addPreferenceToScreen(systemEntries);
    }

    @VisibleForTesting
    void addPreferenceToScreen(List<BatteryDiffEntry> entries) {
        if (mAppListPrefGroup == null || entries.isEmpty()) {
            return;
        }
        int prefIndex = mAppListPrefGroup.getPreferenceCount();
        for (BatteryDiffEntry entry : entries) {
            final String appLabel = entry.getAppLabel();
            final Drawable appIcon = entry.getAppIcon();
            if (TextUtils.isEmpty(appLabel) || appIcon == null) {
                Log.w(TAG, "cannot find app resource:" + entry.mBatteryHistEntry);
                continue;
            }
            final String prefKey = entry.mBatteryHistEntry.getKey();
            PowerGaugePreference pref =
                (PowerGaugePreference) mPreferenceCache.get(prefKey);
            // Creates new innstance if cached preference is not found.
            if (pref == null) {
                pref = new PowerGaugePreference(mPrefContext);
                pref.setKey(prefKey);
                mPreferenceCache.put(prefKey, pref);
            }
            pref.setIcon(appIcon);
            pref.setTitle(appLabel);
            pref.setOrder(prefIndex);
            pref.setPercent(entry.getPercentOfTotal());
            // Sets the BatteryDiffEntry to preference for launching detailed page.
            pref.setBatteryDiffEntry(entry);
            mAppListPrefGroup.addPreference(pref);
            prefIndex++;
        }
    }

    private void removeAndCacheAllPrefs() {
        if (mAppListPrefGroup == null
                || mAppListPrefGroup.getPreferenceCount() == 0) {
            return;
        }
        final int prefsCount = mAppListPrefGroup.getPreferenceCount();
        for (int index = 0; index < prefsCount; index++) {
            final Preference pref = mAppListPrefGroup.getPreference(index);
            if (TextUtils.isEmpty(pref.getKey())) {
                continue;
            }
            mPreferenceCache.put(pref.getKey(), pref);
        }
        mAppListPrefGroup.removeAll();
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
