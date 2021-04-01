/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.text.NumberFormat;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.Utils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.widget.UsageProgressBarPreference;

/**
 * Controller that update the battery header view
 */
public class BatteryHeaderPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart,
        BatteryPreferenceController {
    @VisibleForTesting
    static final String KEY_BATTERY_HEADER = "battery_header";
    private static final String ANNOTATION_URL = "url";
    private static final int BATTERY_MAX_LEVEL = 100;

    @VisibleForTesting
    BatteryStatusFeatureProvider mBatteryStatusFeatureProvider;
    @VisibleForTesting
    UsageProgressBarPreference mBatteryUsageProgressBarPref;

    private Activity mActivity;
    private PreferenceFragmentCompat mHost;
    private Lifecycle mLifecycle;
    private final PowerManager mPowerManager;

    public BatteryHeaderPreferenceController(Context context, String key) {
        super(context, key);
        mPowerManager = context.getSystemService(PowerManager.class);
        mBatteryStatusFeatureProvider = FeatureFactory.getFactory(context)
                .getBatteryStatusFeatureProvider(context);
    }

    public void setActivity(Activity activity) {
        mActivity = activity;
    }

    public void setFragment(PreferenceFragmentCompat fragment) {
        mHost = fragment;
    }

    public void setLifecycle(Lifecycle lifecycle) {
        mLifecycle = lifecycle;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mBatteryUsageProgressBarPref = screen.findPreference(getPreferenceKey());

        if (com.android.settings.Utils.isBatteryPresent(mContext)) {
            quickUpdateHeaderPreference();
        } else {
            //TODO(b/179237551): Make new progress bar widget support help message
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void onStart() {
        EntityHeaderController.newInstance(mActivity, mHost, null /* header view */)
                .setRecyclerView(mHost.getListView(), mLifecycle)
                .styleActionBar(mActivity);
    }

    private CharSequence generateLabel(BatteryInfo info) {
        if (BatteryUtils.isBatteryDefenderOn(info)) {
            return null;
        } else if (info.remainingLabel == null) {
            return info.statusLabel;
        } else {
            return info.remainingLabel;
        }
    }

    public void updateHeaderPreference(BatteryInfo info) {
        mBatteryUsageProgressBarPref.setUsageSummary(
                formatBatteryPercentageText(info.batteryLevel));
        mBatteryUsageProgressBarPref.setBottomSummary(generateLabel(info));
        mBatteryUsageProgressBarPref.setPercent(info.batteryLevel, BATTERY_MAX_LEVEL);
    }

    /**
     * Callback which receives text for the summary line.
     */
    public void updateBatteryStatus(String label, BatteryInfo info) {
        mBatteryUsageProgressBarPref.setBottomSummary(label != null ? label : generateLabel(info));
    }

    public void quickUpdateHeaderPreference() {
        Intent batteryBroadcast = mContext.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        final int batteryLevel = Utils.getBatteryLevel(batteryBroadcast);
        final boolean discharging =
                batteryBroadcast.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) == 0;

        mBatteryUsageProgressBarPref.setUsageSummary(formatBatteryPercentageText(batteryLevel));
        mBatteryUsageProgressBarPref.setPercent(batteryLevel, BATTERY_MAX_LEVEL);
    }

    private CharSequence formatBatteryPercentageText(int batteryLevel) {
        return TextUtils.expandTemplate(mContext.getText(R.string.battery_header_title_alternate),
                NumberFormat.getIntegerInstance().format(batteryLevel));
    }
}
