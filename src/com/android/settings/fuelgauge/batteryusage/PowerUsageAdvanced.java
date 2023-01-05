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

import static com.android.settings.fuelgauge.BatteryBroadcastReceiver.BatteryUpdateType;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.SearchIndexableResource;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.fuelgauge.BatteryBroadcastReceiver;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Advanced power usage. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class PowerUsageAdvanced extends PowerUsageBase {
    private static final String TAG = "AdvancedBatteryUsage";
    private static final String KEY_REFRESH_TYPE = "refresh_type";
    private static final String KEY_BATTERY_CHART = "battery_chart";

    @VisibleForTesting
    BatteryHistoryPreference mHistPref;
    @VisibleForTesting
    Map<Long, Map<String, BatteryHistEntry>> mBatteryHistoryMap;
    @VisibleForTesting
    final BatteryHistoryLoaderCallbacks mBatteryHistoryLoaderCallbacks =
            new BatteryHistoryLoaderCallbacks();

    private boolean mIsChartDataLoaded = false;
    private BatteryChartPreferenceController mBatteryChartPreferenceController;

    private final ContentObserver mBatteryObserver =
            new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    Log.d(TAG, "onBatteryContentChange: " + selfChange);
                    mIsChartDataLoaded = false;
                    restartBatteryStatsLoader(
                            BatteryBroadcastReceiver.BatteryUpdateType.MANUAL);
                }
            };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mHistPref = findPreference(KEY_BATTERY_CHART);
        setBatteryChartPreferenceController();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isChangingConfigurations()) {
            BatteryEntry.clearUidCache();
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.power_usage_advanced;
    }

    @Override
    public void onPause() {
        super.onPause();
        // Resets the flag to reload usage data in onResume() callback.
        mIsChartDataLoaded = false;
        final Uri uri = DatabaseUtils.BATTERY_CONTENT_URI;
        if (uri != null) {
            getContext().getContentResolver().unregisterContentObserver(mBatteryObserver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        final Uri uri = DatabaseUtils.BATTERY_CONTENT_URI;
        if (uri != null) {
            getContext().getContentResolver().registerContentObserver(
                    uri, /*notifyForDescendants*/ true, mBatteryObserver);
        }
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        mBatteryChartPreferenceController =
                new BatteryChartPreferenceController(
                        context, getSettingsLifecycle(), (SettingsActivity) getActivity());
        ScreenOnTimeController screenOnTimeController = new ScreenOnTimeController(context);
        BatteryUsageBreakdownController batteryUsageBreakdownController =
                new BatteryUsageBreakdownController(
                        context, getSettingsLifecycle(), (SettingsActivity) getActivity(), this);

        mBatteryChartPreferenceController.setOnScreenOnTimeUpdatedListener(
                screenOnTimeController::handleSceenOnTimeUpdated);
        mBatteryChartPreferenceController.setOnBatteryUsageUpdatedListener(
                batteryUsageBreakdownController::handleBatteryUsageUpdated);

        controllers.add(mBatteryChartPreferenceController);
        controllers.add(screenOnTimeController);
        controllers.add(batteryUsageBreakdownController);
        setBatteryChartPreferenceController();
        return controllers;
    }

    @Override
    protected boolean isBatteryHistoryNeeded() {
        return true;
    }

    @Override
    protected void refreshUi(@BatteryUpdateType int refreshType) {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        updatePreference(mHistPref);
        if (mBatteryChartPreferenceController != null && mBatteryHistoryMap != null) {
            mBatteryChartPreferenceController.setBatteryHistoryMap(mBatteryHistoryMap);
        }
    }

    @Override
    protected void restartBatteryStatsLoader(int refreshType) {
        final Bundle bundle = new Bundle();
        bundle.putInt(KEY_REFRESH_TYPE, refreshType);
        if (!mIsChartDataLoaded) {
            mIsChartDataLoaded = true;
            restartLoader(LoaderIndex.BATTERY_HISTORY_LOADER, bundle,
                    mBatteryHistoryLoaderCallbacks);
        }
    }

    private void setBatteryChartPreferenceController() {
        if (mHistPref != null && mBatteryChartPreferenceController != null) {
            mHistPref.setChartPreferenceController(mBatteryChartPreferenceController);
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.power_usage_advanced;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    final List<AbstractPreferenceController> controllers = new ArrayList<>();
                    controllers.add(new BatteryChartPreferenceController(
                            context, null /* lifecycle */, null /* activity */));
                    controllers.add((new ScreenOnTimeController(context)));
                    controllers.add(new BatteryUsageBreakdownController(
                            context, null /* lifecycle */, null /* activity */,
                            null /* fragment */));
                    return controllers;
                }
            };

    private class BatteryHistoryLoaderCallbacks
            implements LoaderManager.LoaderCallbacks<Map<Long, Map<String, BatteryHistEntry>>> {
        private int mRefreshType;

        @Override
        @NonNull
        public Loader<Map<Long, Map<String, BatteryHistEntry>>> onCreateLoader(
                int id, Bundle bundle) {
            mRefreshType = bundle.getInt(KEY_REFRESH_TYPE);
            return new BatteryHistoryLoader(getContext());
        }

        @Override
        public void onLoadFinished(Loader<Map<Long, Map<String, BatteryHistEntry>>> loader,
                Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap) {
            mBatteryHistoryMap = batteryHistoryMap;
            PowerUsageAdvanced.this.onLoadFinished(mRefreshType);
        }

        @Override
        public void onLoaderReset(Loader<Map<Long, Map<String, BatteryHistEntry>>> loader) {
        }
    }

}
