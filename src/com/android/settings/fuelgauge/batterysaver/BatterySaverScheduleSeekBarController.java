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

import static com.android.settingslib.fuelgauge.BatterySaverUtils.KEY_PERCENTAGE;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.fuelgauge.BatterySaverUtils;

/**
 * Responds to user actions in the Settings > Battery > Set a Schedule Screen for the seekbar. Note
 * that this seekbar is only visible when the radio button selected is "Percentage".
 *
 * <p>Note that this is not a preference controller since that screen does not inherit from
 * DashboardFragment.
 *
 * <p>Will call the appropriate power manager APIs and modify the correct settings to enable users
 * to control their automatic battery saver toggling preferences. See {@link
 * Settings.Global#AUTOMATIC_POWER_SAVE_MODE} for more details.
 */
public class BatterySaverScheduleSeekBarController
        implements OnPreferenceChangeListener, OnSeekBarChangeListener {

    public static final int MAX_SEEKBAR_VALUE = 15;
    public static final int MIN_SEEKBAR_VALUE = 2;
    public static final String KEY_BATTERY_SAVER_SEEK_BAR = "battery_saver_seek_bar";
    private static final int LEVEL_UNIT_SCALE = 5;

    @VisibleForTesting public SeekBarPreference mSeekBarPreference;
    private Context mContext;

    @VisibleForTesting int mPercentage;

    public BatterySaverScheduleSeekBarController(Context context) {
        mContext = context;
        mSeekBarPreference = new SeekBarPreference(context);
        mSeekBarPreference.setLayoutResource(R.layout.preference_widget_seekbar_settings);
        mSeekBarPreference.setIconSpaceReserved(false);
        mSeekBarPreference.setOnPreferenceChangeListener(this);
        mSeekBarPreference.setOnSeekBarChangeListener(this);
        mSeekBarPreference.setContinuousUpdates(true);
        mSeekBarPreference.setMax(MAX_SEEKBAR_VALUE);
        mSeekBarPreference.setMin(MIN_SEEKBAR_VALUE);
        mSeekBarPreference.setKey(KEY_BATTERY_SAVER_SEEK_BAR);
        mSeekBarPreference.setHapticFeedbackMode(SeekBarPreference.HAPTIC_FEEDBACK_MODE_ON_TICKS);
        updateSeekBar();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mPercentage = ((Integer) newValue) * LEVEL_UNIT_SCALE;
        final CharSequence stateDescription = formatStateDescription(mPercentage);
        preference.setTitle(stateDescription);
        mSeekBarPreference.overrideSeekBarStateDescription(stateDescription);
        return true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mPercentage > 0) {
            Settings.Global.putInt(
                    mContext.getContentResolver(),
                    Global.LOW_POWER_MODE_TRIGGER_LEVEL,
                    mPercentage);
        }
    }

    public void updateSeekBar() {
        final ContentResolver resolver = mContext.getContentResolver();
        final String mode = BatterySaverUtils.getBatterySaverScheduleKey(mContext);
        if (KEY_PERCENTAGE.equals(mode)) {
            final int threshold =
                    Settings.Global.getInt(resolver, Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0);
            final int currentSeekbarValue = Math.max(threshold / 5, MIN_SEEKBAR_VALUE);
            mSeekBarPreference.setVisible(true);
            mSeekBarPreference.setProgress(currentSeekbarValue);
            final CharSequence stateDescription = formatStateDescription(currentSeekbarValue * 5);
            mSeekBarPreference.setTitle(stateDescription);
            mSeekBarPreference.overrideSeekBarStateDescription(stateDescription);
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

    private CharSequence formatStateDescription(int percentage) {
        return mContext.getString(
                R.string.battery_saver_seekbar_title, Utils.formatPercentage(percentage));
    }
}
