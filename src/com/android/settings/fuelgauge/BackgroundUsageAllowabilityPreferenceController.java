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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.PrimarySwitchPreference;

/** Controller to update the manage battery usage preference in App Battery Usage page */
public class BackgroundUsageAllowabilityPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin {

    @VisibleForTesting
    static final String KEY_BACKGROUND_USAGE_ALLOWABILITY_SWITCH =
            "background_usage_allowability_switch";

    private final BatteryOptimizeUtils mBatteryOptimizeUtils;
    private final DashboardFragment mDashboardFragment;
    @Nullable @VisibleForTesting PrimarySwitchPreference mBackgroundUsageAllowabilityPreference;

    public BackgroundUsageAllowabilityPreferenceController(
            @NonNull Context context,
            @NonNull DashboardFragment dashboardFragment,
            @NonNull String preferenceKey,
            @NonNull BatteryOptimizeUtils batteryOptimizeUtils) {
        super(context, preferenceKey);
        mDashboardFragment = dashboardFragment;
        mBatteryOptimizeUtils = batteryOptimizeUtils;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        updatePreferences(mBatteryOptimizeUtils.getAppOptimizationMode());
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mBackgroundUsageAllowabilityPreference =
                screen.findPreference(KEY_BACKGROUND_USAGE_ALLOWABILITY_SWITCH);
        initPreferences();
    }

    @VisibleForTesting
    void initPreferences() {
        if (mBackgroundUsageAllowabilityPreference == null) {
            return;
        }
        final String stateString;
        final String detailInfoString;
        boolean isPreferenceEnabled = true;
        if (mBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()) {
            // Present "Optimized" only string if the package name is invalid.
            stateString = mContext.getString(R.string.manager_battery_usage_optimized_only);
            detailInfoString =
                    mContext.getString(R.string.manager_battery_usage_footer_limited, stateString);
            isPreferenceEnabled = false;
        } else if (mBatteryOptimizeUtils.isSystemOrDefaultApp()) {
            // Present "Unrestricted" only string if the package is system important apps.
            stateString = mContext.getString(R.string.manager_battery_usage_unrestricted_only);
            detailInfoString =
                    mContext.getString(R.string.manager_battery_usage_footer_limited, stateString);
            isPreferenceEnabled = false;
        } else {
            // Present default string to normal app.
            detailInfoString =
                    mContext.getString(
                            R.string.manager_battery_usage_allow_background_usage_summary);
        }
        mBackgroundUsageAllowabilityPreference.setEnabled(isPreferenceEnabled);
        mBackgroundUsageAllowabilityPreference.setSwitchEnabled(isPreferenceEnabled);
        mBackgroundUsageAllowabilityPreference.setSummary(detailInfoString);
        if (isPreferenceEnabled) {
            mBackgroundUsageAllowabilityPreference.setOnPreferenceClickListener(
                    preference -> {
                        PowerBackgroundUsageDetail.startPowerBackgroundUsageDetailPage(
                                mContext, mDashboardFragment.getArguments());
                        return true;
                    });
            mBackgroundUsageAllowabilityPreference.setOnPreferenceChangeListener(
                    (preference, isAllowBackground) -> {
                        handleBatteryOptimizeModeUpdated(
                                (boolean) isAllowBackground
                                        ? BatteryOptimizeUtils.MODE_OPTIMIZED
                                        : BatteryOptimizeUtils.MODE_RESTRICTED);
                        return true;
                    });
        }
    }

    @VisibleForTesting
    void handleBatteryOptimizeModeUpdated(int newBatteryOptimizeMode) {
        if (mBatteryOptimizeUtils.getAppOptimizationMode() == newBatteryOptimizeMode) {
            Log.w(TAG, "ignore same mode for: " + mBatteryOptimizeUtils.getPackageName());
            return;
        }
        mBatteryOptimizeUtils.setAppUsageState(
                newBatteryOptimizeMode, BatteryOptimizeHistoricalLogEntry.Action.APPLY);
        updatePreferences(newBatteryOptimizeMode);
    }

    @VisibleForTesting
    void updatePreferences(int optimizationMode) {
        if (mBackgroundUsageAllowabilityPreference == null) {
            return;
        }
        mBackgroundUsageAllowabilityPreference.setChecked(
                optimizationMode != BatteryOptimizeUtils.MODE_RESTRICTED);
    }
}
