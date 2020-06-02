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
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.text.TextUtils;

import com.android.settingslib.fuelgauge.BatterySaverUtils;

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
    public static final int TRIGGER_LEVEL_MIN = 5;

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
        if (key == null) {
            return false;
        }

        final ContentResolver resolver = mContext.getContentResolver();
        int mode = PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE;
        int triggerLevel = 0;
        final Bundle confirmationExtras = new Bundle(3);
        switch (key) {
            case KEY_NO_SCHEDULE:
                break;
            case KEY_PERCENTAGE:
                triggerLevel = TRIGGER_LEVEL_MIN;
                confirmationExtras.putBoolean(BatterySaverUtils.EXTRA_CONFIRM_TEXT_ONLY, true);
                confirmationExtras.putInt(BatterySaverUtils.EXTRA_POWER_SAVE_MODE_TRIGGER,
                        PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE);
                confirmationExtras.putInt(BatterySaverUtils.EXTRA_POWER_SAVE_MODE_TRIGGER_LEVEL,
                        triggerLevel);
                break;
            case KEY_ROUTINE:
                mode = PowerManager.POWER_SAVE_MODE_TRIGGER_DYNAMIC;
                confirmationExtras.putBoolean(BatterySaverUtils.EXTRA_CONFIRM_TEXT_ONLY, true);
                confirmationExtras.putInt(BatterySaverUtils.EXTRA_POWER_SAVE_MODE_TRIGGER,
                        PowerManager.POWER_SAVE_MODE_TRIGGER_DYNAMIC);
                break;
            default:
                throw new IllegalStateException(
                        "Not a valid key for " + this.getClass().getSimpleName());
        }

        if (!TextUtils.equals(key, KEY_NO_SCHEDULE)
                && BatterySaverUtils.maybeShowBatterySaverConfirmation(
                mContext, confirmationExtras)) {
            // reset this if we need to show the confirmation message
            mode = PowerManager.POWER_SAVE_MODE_TRIGGER_PERCENTAGE;
            triggerLevel = 0;
        }
        // Trigger level is intentionally left alone when going between dynamic and percentage modes
        // so that a users percentage based schedule is preserved when they toggle between the two.
        Settings.Global.putInt(resolver, Global.AUTOMATIC_POWER_SAVE_MODE, mode);
        if (mode != PowerManager.POWER_SAVE_MODE_TRIGGER_DYNAMIC) {
            Settings.Global.putInt(resolver, Global.LOW_POWER_MODE_TRIGGER_LEVEL, triggerLevel);
        }
        // Suppress battery saver suggestion notification if enabling scheduling battery saver.
        if (mode == PowerManager.POWER_SAVE_MODE_TRIGGER_DYNAMIC || triggerLevel != 0) {
            BatterySaverUtils.suppressAutoBatterySaver(mContext);
        }
        mSeekBarController.updateSeekBar();
        return true;
    }
}
