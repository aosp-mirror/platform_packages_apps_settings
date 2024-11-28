/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.content.Context;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.Utils;

/** Controller that update the battery header view */
public class BatteryHeaderTextPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin, BatteryPreferenceController {
    private static final String TAG = "BatteryHeaderTextPreferenceController";

    private final PowerManager mPowerManager;
    private final BatterySettingsFeatureProvider mBatterySettingsFeatureProvider;

    @Nullable private BatteryTip mBatteryTip;

    @VisibleForTesting BatteryStatusFeatureProvider mBatteryStatusFeatureProvider;

    @Nullable @VisibleForTesting BatteryHeaderTextPreference mBatteryHeaderTextPreference;

    public BatteryHeaderTextPreferenceController(Context context, String key) {
        super(context, key);
        mPowerManager = context.getSystemService(PowerManager.class);
        mBatteryStatusFeatureProvider =
                FeatureFactory.getFeatureFactory().getBatteryStatusFeatureProvider();
        mBatterySettingsFeatureProvider =
                FeatureFactory.getFeatureFactory().getBatterySettingsFeatureProvider();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mBatteryHeaderTextPreference = screen.findPreference(getPreferenceKey());

        if (mBatteryHeaderTextPreference != null
                && !com.android.settings.Utils.isBatteryPresent(mContext)) {
            mBatteryHeaderTextPreference.setVisible(false);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @NonNull
    private CharSequence generateLabel(@NonNull BatteryInfo info) {
        if (Utils.containsIncompatibleChargers(mContext, TAG)) {
            return mContext.getString(
                    com.android.settingslib.R.string.battery_info_status_not_charging);
        }
        if (BatteryUtils.isBatteryDefenderOn(info)
                || FeatureFactory.getFeatureFactory()
                .getPowerUsageFeatureProvider()
                .isExtraDefend()) {
            return mContext.getString(
                    com.android.settingslib.R.string.battery_info_status_charging_on_hold);
        }
        if (info.remainingLabel != null
                && mBatterySettingsFeatureProvider.isChargingOptimizationMode(mContext)) {
            return info.remainingLabel;
        }
        if (info.batteryStatus == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
            return info.statusLabel;
        }
        if (info.pluggedStatus == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
            final CharSequence wirelessChargingLabel =
                    mBatterySettingsFeatureProvider.getWirelessChargingLabel(mContext, info);
            if (mBatteryHeaderTextPreference != null && wirelessChargingLabel != null) {
                mBatteryHeaderTextPreference.setContentDescription(
                        mBatterySettingsFeatureProvider
                                .getWirelessChargingContentDescription(mContext, info));
                return wirelessChargingLabel;
            }
        }
        if (info.remainingLabel == null) {
            return info.statusLabel;
        }
        if (info.statusLabel != null && !info.discharging) {
            // Charging state
            if (com.android.settingslib.fuelgauge.BatteryUtils.isChargingStringV2Enabled()) {
                return info.isFastCharging
                        ? mContext.getString(
                                R.string.battery_state_and_duration,
                                info.statusLabel,
                                info.remainingLabel)
                        : info.remainingLabel;
            }
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

    /** Updates the battery header text with the given BatteryInfo. */
    public void updateHeaderPreference(@NonNull BatteryInfo info) {
        if (mBatteryHeaderTextPreference != null
                && !mBatteryStatusFeatureProvider.triggerBatteryStatusUpdate(this, info)) {
            mBatteryHeaderTextPreference.setText(generateLabel(info));
        }
    }

    /** Callback which updates the battery header text with the given label. */
    @Override
    public void updateBatteryStatus(String label, BatteryInfo info) {
        if (mBatteryHeaderTextPreference == null) {
            return;
        }

        final CharSequence summary = label != null ? label : generateLabel(info);
        mBatteryHeaderTextPreference.setText(summary);
        Log.d(TAG, "updateBatteryStatus: " + label + " summary: " + summary);
    }

    /** Update summary when battery tips are changed. */
    public void updateHeaderByBatteryTips(
            @Nullable BatteryTip batteryTip, @NonNull BatteryInfo batteryInfo) {
        mBatteryTip = batteryTip;

        if (mBatteryTip != null && batteryInfo != null) {
            updateHeaderPreference(batteryInfo);
        }
    }
}
