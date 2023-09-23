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

import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.Context;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.flags.Flags;

/** Controller for "Vibration & haptics" settings page. */
public class VibrationPreferenceController extends BasePreferenceController {

    private final boolean mHasVibrator;

    public VibrationPreferenceController(Context context, String key) {
        super(context, key);
        mHasVibrator = context.getSystemService(Vibrator.class).hasVibrator();
    }

    @Override
    public int getAvailabilityStatus() {
        return mHasVibrator ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        final boolean isVibrateOn = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.VIBRATE_ON, ON) == ON;
        return mContext.getText(
                isVibrateOn
                        ? R.string.accessibility_vibration_settings_state_on
                        : R.string.accessibility_vibration_settings_state_off);
    }

    @VisibleForTesting
    void launchVibrationSettingsFragment(Class klass) {
        new SubSettingLauncher(mContext)
                .setSourceMetricsCategory(getMetricsCategory())
                .setDestination(klass.getName())
                .launch();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (Flags.separateAccessibilityVibrationSettingsFragments()
                && TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            if (mContext.getResources().getInteger(
                    R.integer.config_vibration_supported_intensity_levels) > 1) {
                launchVibrationSettingsFragment(VibrationIntensitySettingsFragment.class);
            } else {
                launchVibrationSettingsFragment(VibrationSettings.class);
            }
            return true;
        }
        return super.handlePreferenceTreeClick(preference);
    }


}
