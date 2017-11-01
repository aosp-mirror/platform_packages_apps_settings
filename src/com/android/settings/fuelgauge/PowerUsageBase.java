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

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.view.Menu;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.utils.AsyncLoader;

/**
 * Common base class for things that need to show the battery usage graph.
 */
public abstract class PowerUsageBase extends DashboardFragment
        implements LoaderManager.LoaderCallbacks<BatteryStatsHelper> {

    // +1 to allow ordering for PowerUsageSummary.
    @VisibleForTesting
    static final int MENU_STATS_REFRESH = Menu.FIRST + 1;
    private static final String TAG = "PowerUsageBase";

    protected BatteryStatsHelper mStatsHelper;
    protected UserManager mUm;
    private BatteryBroadcastReceiver mBatteryBroadcastReceiver;

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
        mBatteryBroadcastReceiver.setBatteryChangedListener(() -> {
            restartBatteryStatsLoader();
        });

        getLoaderManager().initLoader(0, icicle, this);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();

        BatteryStatsHelper.dropFile(getActivity(), BatteryHistoryDetail.BATTERY_HISTORY_FILE);
        mBatteryBroadcastReceiver.register();
    }

    @Override
    public void onPause() {
        super.onPause();
        mBatteryBroadcastReceiver.unRegister();
    }

    protected void restartBatteryStatsLoader() {
        getLoaderManager().restartLoader(0, Bundle.EMPTY, this);
    }

    protected abstract void refreshUi();

    protected void updatePreference(BatteryHistoryPreference historyPref) {
        final long startTime = System.currentTimeMillis();
        historyPref.setStats(mStatsHelper);
        BatteryUtils.logRuntime(TAG, "updatePreference", startTime);
    }

    @Override
    public Loader<BatteryStatsHelper> onCreateLoader(int id,
            Bundle args) {
        return new BatteryStatsHelperLoader(getContext());
    }

    @Override
    public void onLoadFinished(Loader<BatteryStatsHelper> loader,
            BatteryStatsHelper statsHelper) {
        mStatsHelper = statsHelper;
        refreshUi();
    }

    @Override
    public void onLoaderReset(Loader<BatteryStatsHelper> loader) {

    }
}
