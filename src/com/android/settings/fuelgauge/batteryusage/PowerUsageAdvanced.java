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
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.BatteryManager;
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
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
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
    private static final String KEY_BATTERY_GRAPH = "battery_graph";
    private static final String KEY_APP_LIST = "app_list";

    @VisibleForTesting
    BatteryHistoryPreference mHistPref;
    @VisibleForTesting
    Map<Long, Map<String, BatteryHistEntry>> mBatteryHistoryMap;
    @VisibleForTesting
    final BatteryHistoryLoaderCallbacks mBatteryHistoryLoaderCallbacks =
            new BatteryHistoryLoaderCallbacks();

    private boolean mIsChartDataLoaded = false;
    private boolean mIsChartGraphEnabled = false;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;
    private BatteryChartPreferenceController mBatteryChartPreferenceController;
    private BatteryAppListPreferenceController mBatteryAppListPreferenceController;

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
        final Context context = getContext();
        refreshFeatureFlag(context);
        mHistPref = (BatteryHistoryPreference) findPreference(KEY_BATTERY_GRAPH);
        if (mIsChartGraphEnabled) {
            setBatteryChartPreferenceController();
        } else {
            updateHistPrefSummary(context);
        }
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
        final Uri uri = mPowerUsageFeatureProvider.getBatteryHistoryUri();
        if (uri != null) {
            getContext().getContentResolver().unregisterContentObserver(mBatteryObserver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        final Uri uri = mPowerUsageFeatureProvider.getBatteryHistoryUri();
        if (uri != null) {
            getContext().getContentResolver().registerContentObserver(
                    uri, /*notifyForDescendants*/ true, mBatteryObserver);
        }
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        refreshFeatureFlag(context);
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        // Creates based on the chart design is enabled or not.
        if (mIsChartGraphEnabled) {
            mBatteryChartPreferenceController =
                    new BatteryChartPreferenceController(context, KEY_APP_LIST,
                            getSettingsLifecycle(), (SettingsActivity) getActivity(), this);
            controllers.add(mBatteryChartPreferenceController);
            setBatteryChartPreferenceController();
        } else {
            mBatteryAppListPreferenceController =
                    new BatteryAppListPreferenceController(context, KEY_APP_LIST,
                            getSettingsLifecycle(), (SettingsActivity) getActivity(), this);
            controllers.add(mBatteryAppListPreferenceController);
        }
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
        if (mBatteryAppListPreferenceController != null && mBatteryUsageStats != null) {
            updateHistPrefSummary(context);
            mBatteryAppListPreferenceController.refreshAppListGroup(
                    mBatteryUsageStats, /* showAllApps */true);
        }
        if (mBatteryChartPreferenceController != null && mBatteryHistoryMap != null) {
            mBatteryChartPreferenceController.setBatteryHistoryMap(mBatteryHistoryMap);
        }
    }

    @Override
    protected void restartBatteryStatsLoader(int refreshType) {
        final Bundle bundle = new Bundle();
        bundle.putInt(KEY_REFRESH_TYPE, refreshType);
        // Uses customized battery history loader if chart design is enabled.
        if (mIsChartGraphEnabled && !mIsChartDataLoaded) {
            mIsChartDataLoaded = true;
            restartLoader(LoaderIndex.BATTERY_HISTORY_LOADER, bundle,
                    mBatteryHistoryLoaderCallbacks);
        } else if (!mIsChartGraphEnabled) {
            super.restartBatteryStatsLoader(refreshType);
        }
    }

    private void updateHistPrefSummary(Context context) {
        final Intent batteryIntent =
                context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        final boolean plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0;
        if (mPowerUsageFeatureProvider.isEnhancedBatteryPredictionEnabled(context) && !plugged) {
            mHistPref.setBottomSummary(
                    mPowerUsageFeatureProvider.getAdvancedUsageScreenInfoString());
        } else {
            mHistPref.hideBottomSummary();
        }
    }

    private void refreshFeatureFlag(Context context) {
        if (mPowerUsageFeatureProvider == null) {
            mPowerUsageFeatureProvider = FeatureFactory.getFactory(context)
                    .getPowerUsageFeatureProvider(context);
            mIsChartGraphEnabled = mPowerUsageFeatureProvider.isChartGraphEnabled(context);
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
                    controllers.add(new BatteryAppListPreferenceController(context,
                            KEY_APP_LIST, null /* lifecycle */, null /* activity */,
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
