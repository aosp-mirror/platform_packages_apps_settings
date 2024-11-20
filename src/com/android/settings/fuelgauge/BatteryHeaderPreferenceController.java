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
import android.content.Intent;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.Utils;
import com.android.settingslib.widget.UsageProgressBarPreference;

/** Controller that update the battery header view */
public class BatteryHeaderPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin {
    private static final String TAG = "BatteryHeaderPreferenceController";
    private static final int BATTERY_MAX_LEVEL = 100;

    @VisibleForTesting UsageProgressBarPreference mBatteryUsageProgressBarPref;

    public BatteryHeaderPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mBatteryUsageProgressBarPref = screen.findPreference(getPreferenceKey());
        // Hide the bottom summary from the progress bar.
        mBatteryUsageProgressBarPref.setBottomSummary("");

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

    /** Updates {@link UsageProgressBarPreference} information. */
    public void quickUpdateHeaderPreference() {
        Intent batteryBroadcast =
                com.android.settingslib.fuelgauge.BatteryUtils.getBatteryIntent(mContext);
        final int batteryLevel = Utils.getBatteryLevel(batteryBroadcast);

        mBatteryUsageProgressBarPref.setUsageSummary(formatBatteryPercentageText(batteryLevel));
        mBatteryUsageProgressBarPref.setPercent(batteryLevel, BATTERY_MAX_LEVEL);
    }

    private CharSequence formatBatteryPercentageText(int batteryLevel) {
        return com.android.settings.Utils.formatPercentage(batteryLevel);
    }
}
