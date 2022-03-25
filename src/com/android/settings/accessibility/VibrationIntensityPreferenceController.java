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

package com.android.settings.accessibility;

import android.content.Context;
import android.os.Vibrator;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.SliderPreferenceController;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Abstract preference controller for a vibration intensity setting, that displays multiple
 * intensity levels to the user as a slider.
 */
public abstract class VibrationIntensityPreferenceController extends SliderPreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    protected final VibrationPreferenceConfig mPreferenceConfig;
    private final VibrationPreferenceConfig.SettingObserver mSettingsContentObserver;
    private final int mMaxIntensity;

    protected VibrationIntensityPreferenceController(Context context, String prefkey,
            VibrationPreferenceConfig preferenceConfig) {
        this(context, prefkey, preferenceConfig,
                context.getResources().getInteger(
                        R.integer.config_vibration_supported_intensity_levels));
    }

    protected VibrationIntensityPreferenceController(Context context, String prefkey,
            VibrationPreferenceConfig preferenceConfig, int supportedIntensityLevels) {
        super(context, prefkey);
        mPreferenceConfig = preferenceConfig;
        mSettingsContentObserver = new VibrationPreferenceConfig.SettingObserver(
                preferenceConfig);
        mMaxIntensity = Math.min(Vibrator.VIBRATION_INTENSITY_HIGH, supportedIntensityLevels);
    }

    @Override
    public void onStart() {
        mSettingsContentObserver.register(mContext);
    }

    @Override
    public void onStop() {
        mSettingsContentObserver.unregister(mContext);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final SeekBarPreference preference = screen.findPreference(getPreferenceKey());
        mSettingsContentObserver.onDisplayPreference(this, preference);
        preference.setEnabled(mPreferenceConfig.isPreferenceEnabled());
        preference.setSummaryProvider(unused -> mPreferenceConfig.getSummary());
        preference.setMin(getMin());
        preference.setMax(getMax());
        // Haptics previews played by the Settings app don't bypass user settings to be played.
        // The sliders continuously updates the intensity value so the previews can apply them.
        preference.setContinuousUpdates(true);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference != null) {
            preference.setEnabled(mPreferenceConfig.isPreferenceEnabled());
        }
    }

    @Override
    public int getMin() {
        return Vibrator.VIBRATION_INTENSITY_OFF;
    }

    @Override
    public int getMax() {
        return mMaxIntensity;
    }

    @Override
    public int getSliderPosition() {
        if (!mPreferenceConfig.isPreferenceEnabled()) {
            return getMin();
        }
        final int position = mPreferenceConfig.readIntensity();
        return Math.min(position, getMax());
    }

    @Override
    public boolean setSliderPosition(int position) {
        if (!mPreferenceConfig.isPreferenceEnabled()) {
            // Ignore slider updates when the preference is disabled.
            return false;
        }
        final int intensity = calculateVibrationIntensity(position);
        final boolean success = mPreferenceConfig.updateIntensity(intensity);

        if (success && (position != Vibrator.VIBRATION_INTENSITY_OFF)) {
            mPreferenceConfig.playVibrationPreview();
        }

        return success;
    }

    private int calculateVibrationIntensity(int position) {
        int maxPosition = getMax();
        if (position >= maxPosition) {
            if (maxPosition == 1) {
                // If there is only one intensity available besides OFF, then use the device default
                // intensity to ensure no scaling will ever happen in the platform.
                return mPreferenceConfig.getDefaultIntensity();
            }
            // If the settings granularity is lower than the platform's then map the max position to
            // the highest vibration intensity, skipping intermediate values in the scale.
            return Vibrator.VIBRATION_INTENSITY_HIGH;
        }
        return position;
    }
}
