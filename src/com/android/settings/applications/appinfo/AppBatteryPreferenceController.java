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
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.BatteryUsageStats;
import android.os.Bundle;
import android.os.UidBatteryConsumer;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.fuelgauge.AdvancedPowerUsageDetail;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.fuelgauge.batteryusage.BatteryChartPreferenceController;
import com.android.settings.fuelgauge.batteryusage.BatteryDiffEntry;
import com.android.settings.fuelgauge.batteryusage.BatteryEntry;
import com.android.settings.fuelgauge.batteryusage.BatteryUsageStatsLoader;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import java.util.List;

public class AppBatteryPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnResume, OnPause {

    private static final String TAG = "AppBatteryPreferenceController";
    private static final String KEY_BATTERY = "battery";

    @VisibleForTesting
    final BatteryUsageStatsLoaderCallbacks mBatteryUsageStatsLoaderCallbacks =
            new BatteryUsageStatsLoaderCallbacks();
    @VisibleForTesting
    BatteryUtils mBatteryUtils;
    @VisibleForTesting
    BatteryUsageStats mBatteryUsageStats;
    @VisibleForTesting
    UidBatteryConsumer mUidBatteryConsumer;
    @VisibleForTesting
    BatteryDiffEntry mBatteryDiffEntry;
    @VisibleForTesting
    boolean mIsChartGraphEnabled;
    @VisibleForTesting
    final AppInfoDashboardFragment mParent;

    private Preference mPreference;
    private String mBatteryPercent;
    private final String mPackageName;
    private final int mUid;
    private final int mUserId;
    private boolean mBatteryUsageStatsLoaded = false;
    private boolean mBatteryDiffEntriesLoaded = false;

    public AppBatteryPreferenceController(Context context, AppInfoDashboardFragment parent,
            String packageName, int uid, Lifecycle lifecycle) {
        super(context, KEY_BATTERY);
        mParent = parent;
        mBatteryUtils = BatteryUtils.getInstance(mContext);
        mPackageName = packageName;
        mUid = uid;
        mUserId = mContext.getUserId();
        refreshFeatureFlag(mContext);
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
        if (!AppUtils.isAppInstalled(mParent.getAppEntry())) {
            mPreference.setSummary("");
            return;
        }

        loadBatteryDiffEntries();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!KEY_BATTERY.equals(preference.getKey())) {
            return false;
        }

        if (mBatteryDiffEntry != null) {
            Log.i(TAG, "handlePreferenceTreeClick():\n" + mBatteryDiffEntry);
            AdvancedPowerUsageDetail.startBatteryDetailPage(
                    mParent.getActivity(),
                    mParent,
                    mBatteryDiffEntry,
                    Utils.formatPercentage(
                            mBatteryDiffEntry.getPercentOfTotal(), /* round */ true),
                    /*isValidToShowSummary=*/ true,
                    /*slotInformation=*/ null);
            return true;
        }

        if (isBatteryStatsAvailable()) {
            final UserManager userManager =
                    (UserManager) mContext.getSystemService(Context.USER_SERVICE);
            final BatteryEntry entry = new BatteryEntry(mContext, /* handler */null, userManager,
                    mUidBatteryConsumer, /* isHidden */ false,
                    mUidBatteryConsumer.getUid(), /* packages */ null, mPackageName);
            Log.i(TAG, "Battery consumer available, launch : "
                    + entry.getDefaultPackageName()
                    + " | uid : "
                    + entry.getUid()
                    + " with BatteryEntry data");
            AdvancedPowerUsageDetail.startBatteryDetailPage(mParent.getActivity(), mParent, entry,
                    mIsChartGraphEnabled ? Utils.formatPercentage(0) : mBatteryPercent,
                    !mIsChartGraphEnabled);
        } else {
            Log.i(TAG, "Launch : " + mPackageName + " with package name");
            AdvancedPowerUsageDetail.startBatteryDetailPage(mParent.getActivity(), mParent,
                    mPackageName, UserHandle.CURRENT);
        }
        return true;
    }

    @Override
    public void onResume() {
        mParent.getLoaderManager().restartLoader(
                AppInfoDashboardFragment.LOADER_BATTERY_USAGE_STATS, Bundle.EMPTY,
                mBatteryUsageStatsLoaderCallbacks);
    }

    @Override
    public void onPause() {
        mParent.getLoaderManager().destroyLoader(
                AppInfoDashboardFragment.LOADER_BATTERY_USAGE_STATS);
        closeBatteryUsageStats();
    }

    private void loadBatteryDiffEntries() {
        new AsyncTask<Void, Void, BatteryDiffEntry>() {
            @Override
            protected BatteryDiffEntry doInBackground(Void... unused) {
                if (mPackageName == null) {
                    return null;
                }
                final BatteryDiffEntry entry =
                        BatteryChartPreferenceController.getAppBatteryUsageData(
                                mContext, mPackageName, mUserId);
                Log.d(TAG, "loadBatteryDiffEntries():\n" + entry);
                return entry;
            }

            @Override
            protected void onPostExecute(BatteryDiffEntry batteryDiffEntry) {
                mBatteryDiffEntry = batteryDiffEntry;
                updateBatteryWithDiffEntry();
            }
        }.execute();
    }

    @VisibleForTesting
    void updateBatteryWithDiffEntry() {
        if (mIsChartGraphEnabled) {
            if (mBatteryDiffEntry != null && mBatteryDiffEntry.mConsumePower > 0) {
                mBatteryPercent = Utils.formatPercentage(
                        mBatteryDiffEntry.getPercentOfTotal(), /* round */ true);
                mPreference.setSummary(mContext.getString(
                        R.string.battery_summary, mBatteryPercent));
            } else {
                mPreference.setSummary(
                        mContext.getString(R.string.no_battery_summary));
            }
        }

        mBatteryDiffEntriesLoaded = true;
        mPreference.setEnabled(mBatteryUsageStatsLoaded);
    }

    private void onLoadFinished() {
        if (mBatteryUsageStats == null) {
            return;
        }

        final PackageInfo packageInfo = mParent.getPackageInfo();
        if (packageInfo != null) {
            mUidBatteryConsumer = findTargetUidBatteryConsumer(mBatteryUsageStats,
                    packageInfo.applicationInfo.uid);
            if (mParent.getActivity() != null) {
                updateBattery();
            }
        }
    }

    private void refreshFeatureFlag(Context context) {
        if (isWorkProfile(context)) {
            try {
                context = context.createPackageContextAsUser(
                        context.getPackageName(), 0, UserHandle.OWNER);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "context.createPackageContextAsUser() fail: " + e);
            }
        }

        final PowerUsageFeatureProvider powerUsageFeatureProvider =
                FeatureFactory.getFactory(context).getPowerUsageFeatureProvider(context);
        mIsChartGraphEnabled = powerUsageFeatureProvider.isChartGraphEnabled(context);
    }

    private boolean isWorkProfile(Context context) {
        final UserManager userManager = context.getSystemService(UserManager.class);
        return userManager.isManagedProfile() && !userManager.isSystemUser();
    }

    @VisibleForTesting
    void updateBattery() {
        mBatteryUsageStatsLoaded = true;
        mPreference.setEnabled(mBatteryDiffEntriesLoaded);
        if (mIsChartGraphEnabled) {
            return;
        }
        if (isBatteryStatsAvailable()) {
            final int percentOfMax = (int) mBatteryUtils.calculateBatteryPercent(
                    mUidBatteryConsumer.getConsumedPower(), mBatteryUsageStats.getConsumedPower(),
                    mBatteryUsageStats.getDischargePercentage());
            mBatteryPercent = Utils.formatPercentage(percentOfMax);
            mPreference.setSummary(mContext.getString(R.string.battery_summary, mBatteryPercent));
        } else {
            mPreference.setSummary(mContext.getString(R.string.no_battery_summary));
        }
    }

    @VisibleForTesting
    boolean isBatteryStatsAvailable() {
        return mUidBatteryConsumer != null;
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

    private class BatteryUsageStatsLoaderCallbacks
            implements LoaderManager.LoaderCallbacks<BatteryUsageStats> {
        @Override
        @NonNull
        public Loader<BatteryUsageStats> onCreateLoader(int id, Bundle args) {
            return new BatteryUsageStatsLoader(mContext, /* includeBatteryHistory */ false);
        }

        @Override
        public void onLoadFinished(Loader<BatteryUsageStats> loader,
                BatteryUsageStats batteryUsageStats) {
            closeBatteryUsageStats();
            mBatteryUsageStats = batteryUsageStats;
            AppBatteryPreferenceController.this.onLoadFinished();
        }

        @Override
        public void onLoaderReset(Loader<BatteryUsageStats> loader) {
        }
    }

    private void closeBatteryUsageStats() {
        if (mBatteryUsageStats != null) {
            try {
                mBatteryUsageStats.close();
            } catch (Exception e) {
                Log.e(TAG, "BatteryUsageStats.close() failed", e);
            } finally {
                mBatteryUsageStats = null;
            }
        }
    }
}
