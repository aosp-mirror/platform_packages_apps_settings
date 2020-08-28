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
import android.os.Bundle;
import android.os.UserManager;
import android.view.Menu;

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
        mBatteryBroadcastReceiver.setBatteryChangedListener(type -> {
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

        getLoaderManager().restartLoader(0, bundle, new PowerLoaderCallback());
    }

    protected abstract void refreshUi(@BatteryUpdateType int refreshType);

    protected void updatePreference(BatteryHistoryPreference historyPref) {
        final long startTime = System.currentTimeMillis();
        historyPref.setStats(mStatsHelper);
        BatteryUtils.logRuntime(TAG, "updatePreference", startTime);
    }

    /**
     * {@link android.app.LoaderManager.LoaderCallbacks} for {@link PowerUsageBase} to load
     * the {@link BatteryStatsHelper}
     */
    public class PowerLoaderCallback implements LoaderManager.LoaderCallbacks<BatteryStatsHelper> {
        private int mRefreshType;

        @Override
        public Loader<BatteryStatsHelper> onCreateLoader(int id,
                Bundle args) {
            mRefreshType = args.getInt(KEY_REFRESH_TYPE);
            return new BatteryStatsHelperLoader(getContext());
        }

        @Override
        public void onLoadFinished(Loader<BatteryStatsHelper> loader,
                BatteryStatsHelper statsHelper) {
            mStatsHelper = statsHelper;
            refreshUi(mRefreshType);
        }

        @Override
        public void onLoaderReset(Loader<BatteryStatsHelper> loader) {

        }
    }
}
