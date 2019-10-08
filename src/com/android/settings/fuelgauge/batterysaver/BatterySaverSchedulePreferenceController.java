/*
 * Copyright (C) 2018 The Android Open Source Project
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
 */
package com.android.settings.fuelgauge.batterysaver;

import android.content.ContentResolver;
import android.content.Context;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Settings.Global;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.fuelgauge.BatterySaverUtils;

/**
 * Simple controller to navigate users to the scheduling page from
 * "Settings > Battery > Battery Saver". Also updates the summary for preference based on
 * the currently selected settings.
 */
public class BatterySaverSchedulePreferenceController extends BasePreferenceController {

    @VisibleForTesting
    Preference mBatterySaverSchedulePreference;
    public static final String KEY_BATTERY_SAVER_SCHEDULE = "battery_saver_schedule";


    public BatterySaverSchedulePreferenceController(Context context) {
        super(context, KEY_BATTERY_SAVER_SCHEDULE);
        BatterySaverUtils.revertScheduleToNoneIfNeeded(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BATTERY_SAVER_SCHEDULE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mBatterySaverSchedulePreference = screen.findPreference(KEY_BATTERY_SAVER_SCHEDULE);
    }

    @Override
    public CharSequence getSummary() {
        final ContentResolver resolver = mContext.getContentResolver();
        final int mode = Settings.Global.getInt(resolver, Global.AUTOMATIC_POWER_SAVE_MODE,
                PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE);
        if (mode == PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE) {
            final int threshold =
                    Settings.Global.getInt(resolver, Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0);
            if (threshold <= 0) {
                return mContext.getText(R.string.battery_saver_auto_no_schedule);
            } else {
                return mContext.getString(R.string.battery_saver_auto_percentage_summary,
                        Utils.formatPercentage(threshold));
            }
        } else {
            return mContext.getText(R.string.battery_saver_auto_routine);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
