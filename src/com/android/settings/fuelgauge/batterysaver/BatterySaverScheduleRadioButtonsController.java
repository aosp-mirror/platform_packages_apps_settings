/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.settings.fuelgauge.batterysaver;

import android.content.ContentResolver;
import android.content.Context;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Settings.Global;

/**
 * Responds to user actions in the Settings > Battery > Set a Schedule Screen
 *
 * Note that this is not a preference controller since that screen does not inherit from
 * DashboardFragment.
 *
 * Will call the appropriate power manager APIs and modify the correct settings to enable
 * users to control their automatic battery saver toggling preferences.
 * See {@link Settings.Global#AUTOMATIC_POWER_SAVE_MODE} for more details.
 */
public class BatterySaverScheduleRadioButtonsController {

    public static final String KEY_NO_SCHEDULE = "key_battery_saver_no_schedule";
    public static final String KEY_ROUTINE = "key_battery_saver_routine";
    public static final String KEY_PERCENTAGE = "key_battery_saver_percentage";

    private Context mContext;
    private BatterySaverScheduleSeekBarController mSeekBarController;

    public BatterySaverScheduleRadioButtonsController(Context context,
            BatterySaverScheduleSeekBarController seekbar) {
        mContext = context;
        mSeekBarController = seekbar;
    }

    public String getDefaultKey() {
        final ContentResolver resolver = mContext.getContentResolver();
        // Note: this can also be obtained via PowerManager.getPowerSaveModeTrigger()
        final int mode = Settings.Global.getInt(resolver, Global.AUTOMATIC_POWER_SAVE_MODE,
                PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE);
        // if mode is "dynamic" we are in routine mode, percentage with non-zero threshold is
        // percentage mode, otherwise it is no schedule mode
        if (mode == PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE) {
            final int threshold =
                    Settings.Global.getInt(resolver, Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0);
            if (threshold <= 0) {
                return KEY_NO_SCHEDULE;
            }
            return KEY_PERCENTAGE;
        }
        return KEY_ROUTINE;
    }

    public boolean setDefaultKey(String key) {
        final ContentResolver resolver = mContext.getContentResolver();
        switch(key) {
            case KEY_NO_SCHEDULE:
                Settings.Global.putInt(resolver, Global.AUTOMATIC_POWER_SAVE_MODE,
                        PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE);
                Settings.Global.putInt(resolver, Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0);
                break;
            case KEY_PERCENTAGE:
                Settings.Global.putInt(resolver, Global.AUTOMATIC_POWER_SAVE_MODE,
                        PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE);
                Settings.Global.putInt(resolver, Global.LOW_POWER_MODE_TRIGGER_LEVEL, 5);
                break;
            case KEY_ROUTINE:
                Settings.Global.putInt(resolver, Global.AUTOMATIC_POWER_SAVE_MODE,
                        PowerManager.POWER_SAVE_MODE_TRIGGER_DYNAMIC);
                break;
            default:
                throw new IllegalStateException(
                        "Not a valid key for " + this.getClass().getSimpleName());
        }
        mSeekBarController.updateSeekBar();
        return true;
    }
}
