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
 */

package com.android.settings.fuelgauge;

import android.content.Context;
import android.content.Intent;
import android.icu.text.NumberFormat;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.Utils;
import com.android.settingslib.widget.UsageProgressBarPreference;

/** Controller that update the battery header view */
public class BatteryHeaderPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin, BatteryPreferenceController {
    private static final String TAG = "BatteryHeaderPreferenceController";

    @VisibleForTesting static final String KEY_BATTERY_HEADER = "battery_header";
    private static final int BATTERY_MAX_LEVEL = 100;

    @VisibleForTesting BatteryStatusFeatureProvider mBatteryStatusFeatureProvider;
    @VisibleForTesting UsageProgressBarPreference mBatteryUsageProgressBarPref;

    private BatteryTip mBatteryTip;
    private final PowerManager mPowerManager;

    public BatteryHeaderPreferenceController(Context context, String key) {
        super(context, key);
        mPowerManager = context.getSystemService(PowerManager.class);
        mBatteryStatusFeatureProvider =
                FeatureFactory.getFeatureFactory().getBatteryStatusFeatureProvider();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mBatteryUsageProgressBarPref = screen.findPreference(getPreferenceKey());
        // Set up loading text first to prevent layout flaky before info loaded.
        mBatteryUsageProgressBarPref.setBottomSummary(
                mContext.getString(R.string.settings_license_activity_loading));

        if (com.android.settings.Utils.isBatteryPresent(mContext)) {
            quickUpdateHeaderPreference();
        } else {
            mBatteryUsageProgressBarPref.setVisible(false);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    private CharSequence generateLabel(BatteryInfo info) {
        if (Utils.containsIncompatibleChargers(mContext, TAG)) {
            return mContext.getString(
                    com.android.settingslib.R.string.battery_info_status_not_charging);
        } else if (BatteryUtils.isBatteryDefenderOn(info)) {
            return null;
        } else if (info.remainingLabel == null
                || info.batteryStatus == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
            // Present status only if no remaining time or status anomalous
            return info.statusLabel;
        } else if (info.statusLabel != null && !info.discharging) {
            // Charging state
            return mContext.getString(
                    R.string.battery_state_and_duration, info.statusLabel, info.remainingLabel);
        } else if (mPowerManager.isPowerSaveMode()) {
            // Power save mode is on
            final String powerSaverOn =
                    mContext.getString(R.string.battery_tip_early_heads_up_done_title);
            return mContext.getString(
                    R.string.battery_state_and_duration, powerSaverOn, info.remainingLabel);
        } else if (mBatteryTip != null && mBatteryTip.getType() == BatteryTip.TipType.LOW_BATTERY) {
            // Low battery state
            final String lowBattery = mContext.getString(R.string.low_battery_summary);
            return mContext.getString(
                    R.string.battery_state_and_duration, lowBattery, info.remainingLabel);
        } else {
            // Discharging state
            return info.remainingLabel;
        }
    }

    public void updateHeaderPreference(BatteryInfo info) {
        if (!mBatteryStatusFeatureProvider.triggerBatteryStatusUpdate(this, info)) {
            mBatteryUsageProgressBarPref.setBottomSummary(generateLabel(info));
        }

        mBatteryUsageProgressBarPref.setUsageSummary(
                formatBatteryPercentageText(info.batteryLevel));
        mBatteryUsageProgressBarPref.setPercent(info.batteryLevel, BATTERY_MAX_LEVEL);
    }

    /** Callback which receives text for the summary line. */
    public void updateBatteryStatus(String label, BatteryInfo info) {
        final CharSequence summary = label != null ? label : generateLabel(info);
        mBatteryUsageProgressBarPref.setBottomSummary(summary);
        Log.d(TAG, "updateBatteryStatus: " + label + " summary: " + summary);
    }

    public void quickUpdateHeaderPreference() {
        Intent batteryBroadcast =
                com.android.settingslib.fuelgauge.BatteryUtils.getBatteryIntent(mContext);
        final int batteryLevel = Utils.getBatteryLevel(batteryBroadcast);
        final boolean discharging =
                batteryBroadcast.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) == 0;

        mBatteryUsageProgressBarPref.setUsageSummary(formatBatteryPercentageText(batteryLevel));
        mBatteryUsageProgressBarPref.setPercent(batteryLevel, BATTERY_MAX_LEVEL);
    }

    /** Update summary when battery tips changed. */
    public void updateHeaderByBatteryTips(BatteryTip batteryTip, BatteryInfo batteryInfo) {
        mBatteryTip = batteryTip;

        if (mBatteryTip != null && batteryInfo != null) {
            updateHeaderPreference(batteryInfo);
        }
    }

    private CharSequence formatBatteryPercentageText(int batteryLevel) {
        return TextUtils.expandTemplate(
                mContext.getText(R.string.battery_header_title_alternate),
                NumberFormat.getIntegerInstance().format(batteryLevel));
    }
}
