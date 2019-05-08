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

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.widget.SeekBarPreference;

/**
 * Responds to user actions in the Settings > Battery > Set a Schedule Screen for the seekbar.
 * Note that this seekbar is only visible when the radio button selected is "Percentage".
 *
 * Note that this is not a preference controller since that screen does not inherit from
 * DashboardFragment.
 *
 * Will call the appropriate power manager APIs and modify the correct settings to enable
 * users to control their automatic battery saver toggling preferences.
 * See {@link Settings.Global#AUTOMATIC_POWER_SAVE_MODE} for more details.
 */
public class BatterySaverScheduleSeekBarController implements
        OnPreferenceChangeListener {

    public static final int MAX_SEEKBAR_VALUE = 15;
    public static final int MIN_SEEKBAR_VALUE = 1;
    public static final String KEY_BATTERY_SAVER_SEEK_BAR = "battery_saver_seek_bar";

    @VisibleForTesting
    public SeekBarPreference mSeekBarPreference;
    private Context mContext;

    public BatterySaverScheduleSeekBarController(Context context) {
        mContext = context;
        mSeekBarPreference = new SeekBarPreference(context);
        mSeekBarPreference.setOnPreferenceChangeListener(this);
        mSeekBarPreference.setContinuousUpdates(true);
        mSeekBarPreference.setMax(MAX_SEEKBAR_VALUE);
        mSeekBarPreference.setMin(MIN_SEEKBAR_VALUE);
        mSeekBarPreference.setKey(KEY_BATTERY_SAVER_SEEK_BAR);
        updateSeekBar();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // The nits are in intervals of 5%
        final int percentage = ((Integer) newValue) * 5;
        Settings.Global.putInt(mContext.getContentResolver(), Global.LOW_POWER_MODE_TRIGGER_LEVEL,
                percentage);
        preference.setTitle(mContext.getString(
                R.string.battery_saver_seekbar_title, Utils.formatPercentage(percentage)));
        return true;
    }

    public void updateSeekBar() {
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
                mSeekBarPreference.setVisible(false);
            } else {
                final int currentSeekbarValue = Math.max(threshold / 5, MIN_SEEKBAR_VALUE);
                mSeekBarPreference.setVisible(true);
                mSeekBarPreference.setProgress(currentSeekbarValue);
                mSeekBarPreference.setTitle(mContext.getString(
                        R.string.battery_saver_seekbar_title,
                        Utils.formatPercentage(currentSeekbarValue * 5)));
            }
        } else {
            mSeekBarPreference.setVisible(false);
        }
    }

    /**
     * Adds the seekbar to the end of the provided preference screen
     *
     * @param screen The preference screen to add the seekbar to
     */
    public void addToScreen(PreferenceScreen screen) {
        // makes sure it gets added after the preferences if called due to first time battery
        // saver message
        mSeekBarPreference.setOrder(100);
        screen.addPreference(mSeekBarPreference);
    }
}
