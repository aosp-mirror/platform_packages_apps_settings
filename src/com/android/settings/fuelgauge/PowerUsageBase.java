/*
 * Copyright (C) 2015 The Android Open Source Project
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

import static com.android.settings.fuelgauge.BatteryBroadcastReceiver.BatteryUpdateType;

import android.app.Activity;
import android.content.Context;
import android.os.BatteryUsageStats;
import android.os.Bundle;
import android.os.UserManager;
import android.view.Menu;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.dashboard.DashboardFragment;

/**
 * Common base class for things that need to show the battery usage graph.
 */
public abstract class PowerUsageBase extends DashboardFragment {

    // +1 to allow ordering for PowerUsageSummary.
    @VisibleForTesting
    static final int MENU_STATS_REFRESH = Menu.FIRST + 1;
    private static final String TAG = "PowerUsageBase";
    private static final String KEY_REFRESH_TYPE = "refresh_type";
    private static final String KEY_INCLUDE_HISTORY = "include_history";

    private static final int LOADER_BATTERY_STATS_HELPER = 0;
    private static final int LOADER_BATTERY_USAGE_STATS = 1;

    protected BatteryStatsHelper mStatsHelper;
    @VisibleForTesting
    BatteryUsageStats mBatteryUsageStats;

    protected UserManager mUm;
    private BatteryBroadcastReceiver mBatteryBroadcastReceiver;
    protected boolean mIsBatteryPresent = true;

    // TODO(b/180630447): switch to BatteryUsageStatsLoader and remove all references to
    // BatteryStatsHelper and BatterySipper
    @VisibleForTesting
    final BatteryStatsHelperLoaderCallbacks mBatteryStatsHelperLoaderCallbacks =
            new BatteryStatsHelperLoaderCallbacks();

    @VisibleForTesting
    final BatteryUsageStatsLoaderCallbacks mBatteryUsageStatsLoaderCallbacks =
            new BatteryUsageStatsLoaderCallbacks();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mUm = (UserManager) activity.getSystemService(Context.USER_SERVICE);
        mStatsHelper = new BatteryStatsHelper(activity, true);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mStatsHelper.create(icicle);
        setHasOptionsMenu(true);

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
    }

    protected void restartBatteryStatsLoader(int refreshType) {
        final Bundle bundle = new Bundle();
        bundle.putInt(KEY_REFRESH_TYPE, refreshType);
        bundle.putBoolean(KEY_INCLUDE_HISTORY, isBatteryHistoryNeeded());
        getLoaderManager().restartLoader(LOADER_BATTERY_STATS_HELPER, bundle,
                mBatteryStatsHelperLoaderCallbacks);
        getLoaderManager().restartLoader(LOADER_BATTERY_USAGE_STATS, bundle,
                mBatteryUsageStatsLoaderCallbacks);
    }

    private void onLoadFinished(@BatteryUpdateType int refreshType) {
        // Wait for both loaders to finish before proceeding.
        if (mStatsHelper == null || mBatteryUsageStats == null) {
            return;
        }

        refreshUi(refreshType);
    }

    protected abstract void refreshUi(@BatteryUpdateType int refreshType);
    protected abstract boolean isBatteryHistoryNeeded();

    protected void updatePreference(BatteryHistoryPreference historyPref) {
        final long startTime = System.currentTimeMillis();
        historyPref.setBatteryUsageStats(mBatteryUsageStats);
        BatteryUtils.logRuntime(TAG, "updatePreference", startTime);
    }

    private class BatteryStatsHelperLoaderCallbacks
            implements LoaderManager.LoaderCallbacks<BatteryStatsHelper> {
        private int mRefreshType;

        @Override
        public Loader<BatteryStatsHelper> onCreateLoader(int id, Bundle args) {
            mRefreshType = args.getInt(KEY_REFRESH_TYPE);
            return new BatteryStatsHelperLoader(getContext());
        }

        @Override
        public void onLoadFinished(Loader<BatteryStatsHelper> loader,
                BatteryStatsHelper batteryHelper) {
            mStatsHelper = batteryHelper;
            PowerUsageBase.this.onLoadFinished(mRefreshType);
        }

        @Override
        public void onLoaderReset(Loader<BatteryStatsHelper> loader) {
        }
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
            mBatteryUsageStats = batteryUsageStats;
            PowerUsageBase.this.onLoadFinished(mRefreshType);
        }

        @Override
        public void onLoaderReset(Loader<BatteryUsageStats> loader) {
        }
    }
}
