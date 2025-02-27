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

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

/** Controller to update the app background usage mode state in Allow background usage page */
public class BatteryOptimizationModePreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin {

    @VisibleForTesting
    static final String KEY_BACKGROUND_USAGE_ALLOWABILITY_SWITCH =
            "background_usage_allowability_switch";

    @VisibleForTesting static final String KEY_OPTIMIZED_PREF = "optimized_preference";
    @VisibleForTesting static final String KEY_UNRESTRICTED_PREF = "unrestricted_preference";

    private final BatteryOptimizeUtils mBatteryOptimizeUtils;
    @Nullable @VisibleForTesting MainSwitchPreference mBackgroundUsageAllowabilityPreference;
    @Nullable @VisibleForTesting SelectorWithWidgetPreference mOptimizedPreference;
    @Nullable @VisibleForTesting SelectorWithWidgetPreference mUnrestrictedPreference;

    public BatteryOptimizationModePreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey,
            @NonNull BatteryOptimizeUtils batteryOptimizeUtils) {
        super(context, preferenceKey);
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
        mOptimizedPreference = screen.findPreference(KEY_OPTIMIZED_PREF);
        mUnrestrictedPreference = screen.findPreference(KEY_UNRESTRICTED_PREF);
        initPreferences();
    }

    @VisibleForTesting
    void initPreferences() {
        if (mBackgroundUsageAllowabilityPreference == null
                || mOptimizedPreference == null
                || mUnrestrictedPreference == null) {
            return;
        }
        final boolean isEnabled = mBatteryOptimizeUtils.isOptimizeModeMutable();
        mBackgroundUsageAllowabilityPreference.setEnabled(isEnabled);
        mOptimizedPreference.setEnabled(isEnabled);
        mUnrestrictedPreference.setEnabled(isEnabled);
        if (isEnabled) {
            mBackgroundUsageAllowabilityPreference.setOnPreferenceChangeListener(
                    (preference, isAllowBackground) -> {
                        handleBatteryOptimizeModeUpdated(
                                (boolean) isAllowBackground
                                        ? BatteryOptimizeUtils.MODE_OPTIMIZED
                                        : BatteryOptimizeUtils.MODE_RESTRICTED);
                        return true;
                    });
            mOptimizedPreference.setOnPreferenceClickListener(
                    preference -> {
                        handleBatteryOptimizeModeUpdated(BatteryOptimizeUtils.MODE_OPTIMIZED);
                        return true;
                    });
            mUnrestrictedPreference.setOnPreferenceClickListener(
                    preference -> {
                        handleBatteryOptimizeModeUpdated(BatteryOptimizeUtils.MODE_UNRESTRICTED);
                        return true;
                    });
        }
    }

    @VisibleForTesting
    void updatePreferences(int optimizationMode) {
        if (mBackgroundUsageAllowabilityPreference == null
                || mOptimizedPreference == null
                || mUnrestrictedPreference == null) {
            return;
        }
        final boolean isAllowBackground = optimizationMode != BatteryOptimizeUtils.MODE_RESTRICTED;
        mBackgroundUsageAllowabilityPreference.setChecked(isAllowBackground);
        mOptimizedPreference.setEnabled(isAllowBackground);
        mUnrestrictedPreference.setEnabled(isAllowBackground);
        mOptimizedPreference.setChecked(optimizationMode == BatteryOptimizeUtils.MODE_OPTIMIZED);
        mUnrestrictedPreference.setChecked(
                optimizationMode == BatteryOptimizeUtils.MODE_UNRESTRICTED);
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
}
