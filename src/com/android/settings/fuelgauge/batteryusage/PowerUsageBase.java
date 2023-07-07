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

import android.app.Activity;
import android.content.Context;
import android.os.BatteryUsageStats;
import android.os.Bundle;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.fuelgauge.BatteryBroadcastReceiver;
import com.android.settings.fuelgauge.BatteryUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Common base class for things that need to show the battery usage graph.
 */
public abstract class PowerUsageBase extends DashboardFragment {
    private static final String TAG = "PowerUsageBase";

    @VisibleForTesting
    static final String KEY_REFRESH_TYPE = "refresh_type";
    @VisibleForTesting
    static final String KEY_INCLUDE_HISTORY = "include_history";
    @VisibleForTesting
    BatteryUsageStats mBatteryUsageStats;

    protected UserManager mUm;
    protected boolean mIsBatteryPresent = true;
    private BatteryBroadcastReceiver mBatteryBroadcastReceiver;

    @VisibleForTesting
    final BatteryUsageStatsLoaderCallbacks mBatteryUsageStatsLoaderCallbacks =
            new BatteryUsageStatsLoaderCallbacks();

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            LoaderIndex.BATTERY_USAGE_STATS_LOADER,
            LoaderIndex.BATTERY_INFO_LOADER,
            LoaderIndex.BATTERY_TIP_LOADER,
            LoaderIndex.BATTERY_HISTORY_LOADER

    })
    public @interface LoaderIndex {
        int BATTERY_USAGE_STATS_LOADER = 0;
        int BATTERY_INFO_LOADER = 1;
        int BATTERY_TIP_LOADER = 2;
        int BATTERY_HISTORY_LOADER = 3;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mUm = (UserManager) activity.getSystemService(Context.USER_SERVICE);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mBatteryBroadcastReceiver = new BatteryBroadcastReceiver(getContext());
        mBatteryBroadcastReceiver.setBatteryChangedListener(type -> {
            if (type == BatteryBroadcastReceiver.BatteryUpdateType.BATTERY_NOT_PRESENT) {
                mIsBatteryPresent = false;
            }
            restartBatteryStatsLoader(type);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mBatteryBroadcastReceiver.register();
    }

    @Override
    public void onStop() {
        super.onStop();
        mBatteryBroadcastReceiver.unRegister();
        closeBatteryUsageStatsIfNeeded();
    }

    protected void restartBatteryStatsLoader(int refreshType) {
        final Bundle bundle = new Bundle();
        bundle.putInt(KEY_REFRESH_TYPE, refreshType);
        bundle.putBoolean(KEY_INCLUDE_HISTORY, isBatteryHistoryNeeded());
        restartLoader(LoaderIndex.BATTERY_USAGE_STATS_LOADER, bundle,
                mBatteryUsageStatsLoaderCallbacks);
    }

    protected LoaderManager getLoaderManagerForCurrentFragment() {
        return LoaderManager.getInstance(this);
    }

    protected void restartLoader(int loaderId, Bundle bundle,
            LoaderManager.LoaderCallbacks<?> loaderCallbacks) {
        LoaderManager loaderManager = getLoaderManagerForCurrentFragment();
        Loader<?> loader = loaderManager.getLoader(
                loaderId);
        if (loader != null && !loader.isReset()) {
            loaderManager.restartLoader(loaderId, bundle,
                    loaderCallbacks);
        } else {
            loaderManager.initLoader(loaderId, bundle,
                    loaderCallbacks);
        }
    }

    protected void onLoadFinished(@BatteryUpdateType int refreshType) {
        refreshUi(refreshType);
    }

    protected abstract void refreshUi(@BatteryUpdateType int refreshType);

    protected abstract boolean isBatteryHistoryNeeded();

    protected void updatePreference(BatteryHistoryPreference historyPref) {
        final long startTime = System.currentTimeMillis();
        historyPref.setBatteryUsageStats(mBatteryUsageStats);
        BatteryUtils.logRuntime(TAG, "updatePreference", startTime);
    }

    private class BatteryUsageStatsLoaderCallbacks
            implements LoaderManager.LoaderCallbacks<BatteryUsageStats> {
        private int mRefreshType;

        @Override
        @NonNull
        public Loader<BatteryUsageStats> onCreateLoader(int id, Bundle args) {
            mRefreshType = args.getInt(KEY_REFRESH_TYPE);
            return new BatteryUsageStatsLoader(getContext(), args.getBoolean(KEY_INCLUDE_HISTORY));
        }

        @Override
        public void onLoadFinished(Loader<BatteryUsageStats> loader,
                BatteryUsageStats batteryUsageStats) {
            closeBatteryUsageStatsIfNeeded();
            mBatteryUsageStats = batteryUsageStats;
            PowerUsageBase.this.onLoadFinished(mRefreshType);
        }

        @Override
        public void onLoaderReset(Loader<BatteryUsageStats> loader) {
        }
    }

    private void closeBatteryUsageStatsIfNeeded() {
        if (mBatteryUsageStats == null) {
            return;
        }
        try {
            mBatteryUsageStats.close();
        } catch (Exception e) {
            Log.e(TAG, "BatteryUsageStats.close() failed", e);
        } finally {
            mBatteryUsageStats = null;
        }
    }
}
