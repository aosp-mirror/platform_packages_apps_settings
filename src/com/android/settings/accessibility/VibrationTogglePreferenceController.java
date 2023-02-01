/*
 * Copyright (C) 2022 The Android Open Source Project
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
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/** Abstract preference controller for a vibration intensity setting, that has only ON/OFF states */
public abstract class VibrationTogglePreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    protected final VibrationPreferenceConfig mPreferenceConfig;
    private final VibrationPreferenceConfig.SettingObserver mSettingsContentObserver;

    protected VibrationTogglePreferenceController(Context context, String preferenceKey,
            VibrationPreferenceConfig preferenceConfig) {
        super(context, preferenceKey);
        mPreferenceConfig = preferenceConfig;
        mSettingsContentObserver = new VibrationPreferenceConfig.SettingObserver(
                preferenceConfig);
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
        final Preference preference = screen.findPreference(getPreferenceKey());
        mSettingsContentObserver.onDisplayPreference(this, preference);
        preference.setEnabled(mPreferenceConfig.isPreferenceEnabled());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference != null) {
            preference.setEnabled(mPreferenceConfig.isPreferenceEnabled());
            preference.setSummary(mPreferenceConfig.getSummary());
        }
    }

    @Override
    public boolean isChecked() {
        return mPreferenceConfig.isPreferenceEnabled()
                && (mPreferenceConfig.readIntensity() != Vibrator.VIBRATION_INTENSITY_OFF);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (!mPreferenceConfig.isPreferenceEnabled()) {
            // Ignore toggle updates when the preference is disabled.
            return false;
        }
        final int newIntensity = isChecked
                ? mPreferenceConfig.getDefaultIntensity()
                : Vibrator.VIBRATION_INTENSITY_OFF;
        final boolean success = mPreferenceConfig.updateIntensity(newIntensity);

        if (success && isChecked) {
            mPreferenceConfig.playVibrationPreview();
        }

        return success;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }
}
