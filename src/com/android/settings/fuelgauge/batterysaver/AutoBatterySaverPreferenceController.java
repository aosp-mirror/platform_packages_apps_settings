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
 */

package com.android.settings.fuelgauge.batterysaver;

import android.content.Context;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.TogglePreferenceController;

/**
 * Controller that update whether to turn on battery saver automatically
 */
public class AutoBatterySaverPreferenceController extends TogglePreferenceController implements
        Preference.OnPreferenceChangeListener {
    private static final int LOW_POWER_MODE_TRIGGER_THRESHOLD = 15;

    @VisibleForTesting
    static final String KEY_AUTO_BATTERY_SAVER = "auto_battery_saver";

    public AutoBatterySaverPreferenceController(Context context) {
        super(context, KEY_AUTO_BATTERY_SAVER);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL,
                isChecked
                        ? LOW_POWER_MODE_TRIGGER_THRESHOLD
                        : 0);
        return true;
    }
}
