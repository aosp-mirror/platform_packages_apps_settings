/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.Bundle;
import android.os.UidBatteryConsumer;
import android.os.UserManager;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.fuelgauge.AdvancedPowerUsageDetail;
import com.android.settings.fuelgauge.BatteryEntry;
import com.android.settings.fuelgauge.BatteryStatsHelperLoader;
import com.android.settings.fuelgauge.BatteryUsageStatsLoader;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.ArrayList;
import java.util.List;

public class AppBatteryPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnResume, OnPause {

    private static final String KEY_BATTERY = "battery";

    // TODO(b/180630447): switch to BatteryUsageStatsLoader and remove all references to
    // BatteryStatsHelper and BatterySipper
    @VisibleForTesting
    final BatteryStatsHelperLoaderCallbacks mBatteryStatsHelperLoaderCallbacks =
            new BatteryStatsHelperLoaderCallbacks();
    @VisibleForTesting
    final BatteryUsageStatsLoaderCallbacks mBatteryUsageStatsLoaderCallbacks =
            new BatteryUsageStatsLoaderCallbacks();

    @VisibleForTesting
    BatterySipper mSipper;
    @VisibleForTesting
    BatteryStatsHelper mBatteryHelper;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;

    @VisibleForTesting
    BatteryUsageStats mBatteryUsageStats;
    @VisibleForTesting
    UidBatteryConsumer mUidBatteryConsumer;

    private Preference mPreference;
    private final AppInfoDashboardFragment mParent;
    private String mBatteryPercent;
    private final String mPackageName;

    public AppBatteryPreferenceController(Context context, AppInfoDashboardFragment parent,
            String packageName, Lifecycle lifecycle) {
        super(context, KEY_BATTERY);
        mParent = parent;
        mBatteryUtils = BatteryUtils.getInstance(mContext);
        mPackageName = packageName;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_app_info_settings_battery)
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.setEnabled(false);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!KEY_BATTERY.equals(preference.getKey())) {
            return false;
        }
        if (isBatteryStatsAvailable()) {
            final UserManager userManager =
                    (UserManager) mContext.getSystemService(Context.USER_SERVICE);
            final BatteryEntry entry = new BatteryEntry(mContext, null, userManager, mSipper,
                    mUidBatteryConsumer);
            entry.defaultPackageName = mPackageName;
            AdvancedPowerUsageDetail.startBatteryDetailPage(mParent.getActivity(), mParent,
                    mBatteryHelper, BatteryStats.STATS_SINCE_CHARGED, entry, mBatteryPercent);
        } else {
            AdvancedPowerUsageDetail.startBatteryDetailPage(mParent.getActivity(), mParent,
                    mPackageName);
        }
        return true;
    }

    @Override
    public void onResume() {
        mParent.getLoaderManager().restartLoader(
                AppInfoDashboardFragment.LOADER_BATTERY, Bundle.EMPTY,
                mBatteryStatsHelperLoaderCallbacks);
        mParent.getLoaderManager().restartLoader(
                AppInfoDashboardFragment.LOADER_BATTERY_USAGE_STATS, Bundle.EMPTY,
                mBatteryUsageStatsLoaderCallbacks);
    }

    @Override
    public void onPause() {
        mParent.getLoaderManager().destroyLoader(AppInfoDashboardFragment.LOADER_BATTERY);
        mParent.getLoaderManager().destroyLoader(
                AppInfoDashboardFragment.LOADER_BATTERY_USAGE_STATS);
    }

    private void onLoadFinished() {
        // Wait for both loaders to finish before proceeding.
        if (mBatteryHelper == null || mBatteryUsageStats == null) {
            return;
        }

        final PackageInfo packageInfo = mParent.getPackageInfo();
        if (packageInfo != null) {
            mSipper = findTargetSipper(mBatteryHelper, packageInfo.applicationInfo.uid);
            mUidBatteryConsumer = findTargetUidBatteryConsumer(mBatteryUsageStats,
                    packageInfo.applicationInfo.uid);
            if (mParent.getActivity() != null) {
                updateBattery();
            }
        }
    }

    @VisibleForTesting
    void updateBattery() {
        mPreference.setEnabled(true);
        if (isBatteryStatsAvailable()) {
            final int dischargePercentage = mBatteryUsageStats.getDischargePercentage();

            final List<BatterySipper> usageList = new ArrayList<>(mBatteryHelper.getUsageList());
            final double hiddenAmount = mBatteryUtils.removeHiddenBatterySippers(usageList);
            final int percentOfMax = (int) mBatteryUtils.calculateBatteryPercent(
                    mUidBatteryConsumer.getConsumedPower(), mBatteryUsageStats.getConsumedPower(),
                    hiddenAmount, dischargePercentage);
            mBatteryPercent = Utils.formatPercentage(percentOfMax);
            mPreference.setSummary(mContext.getString(R.string.battery_summary, mBatteryPercent));
        } else {
            mPreference.setSummary(mContext.getString(R.string.no_battery_summary));
        }
    }

    @VisibleForTesting
    boolean isBatteryStatsAvailable() {
        return mBatteryHelper != null && mSipper != null && mUidBatteryConsumer != null;
    }

    @VisibleForTesting
    BatterySipper findTargetSipper(BatteryStatsHelper batteryHelper, int uid) {
        final List<BatterySipper> usageList = batteryHelper.getUsageList();
        for (int i = 0, size = usageList.size(); i < size; i++) {
            final BatterySipper sipper = usageList.get(i);
            if (sipper.getUid() == uid) {
                return sipper;
            }
        }
        return null;
    }

    @VisibleForTesting
    UidBatteryConsumer findTargetUidBatteryConsumer(BatteryUsageStats batteryUsageStats, int uid) {
        final List<UidBatteryConsumer> usageList = batteryUsageStats.getUidBatteryConsumers();
        for (int i = 0, size = usageList.size(); i < size; i++) {
            final UidBatteryConsumer consumer = usageList.get(i);
            if (consumer.getUid() == uid) {
                return consumer;
            }
        }
        return null;
    }

    private class BatteryStatsHelperLoaderCallbacks
            implements LoaderManager.LoaderCallbacks<BatteryStatsHelper> {
        @Override
        public Loader<BatteryStatsHelper> onCreateLoader(int id, Bundle args) {
            return new BatteryStatsHelperLoader(mContext);
        }

        @Override
        public void onLoadFinished(Loader<BatteryStatsHelper> loader,
                BatteryStatsHelper batteryHelper) {
            mBatteryHelper = batteryHelper;
            AppBatteryPreferenceController.this.onLoadFinished();
        }

        @Override
        public void onLoaderReset(Loader<BatteryStatsHelper> loader) {
        }
    }

    private class BatteryUsageStatsLoaderCallbacks
            implements LoaderManager.LoaderCallbacks<BatteryUsageStats> {
        @Override
        @NonNull
        public Loader<BatteryUsageStats> onCreateLoader(int id, Bundle args) {
            return new BatteryUsageStatsLoader(mContext);
        }

        @Override
        public void onLoadFinished(Loader<BatteryUsageStats> loader,
                BatteryUsageStats batteryUsageStats) {
            mBatteryUsageStats = batteryUsageStats;
            AppBatteryPreferenceController.this.onLoadFinished();
        }

        @Override
        public void onLoaderReset(Loader<BatteryUsageStats> loader) {
        }
    }
}
