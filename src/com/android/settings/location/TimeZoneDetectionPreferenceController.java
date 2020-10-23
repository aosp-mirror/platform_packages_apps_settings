/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.location;

import android.app.time.TimeManager;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneConfiguration;
import android.content.Context;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settingslib.core.AbstractPreferenceController;

/**
 * The controller for the "location time zone detection" switch on the location time zone detection
 * screen.
 */
public class TimeZoneDetectionPreferenceController extends AbstractPreferenceController {

    private static final String KEY_LOCATION_TIME_ZONE_DETECTION_ENABLED =
            "location_time_zone_detection_enabled";

    private final TimeManager mTimeManager;

    public TimeZoneDetectionPreferenceController(Context context) {
        super(context);
        mTimeManager = context.getSystemService(TimeManager.class);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_LOCATION_TIME_ZONE_DETECTION_ENABLED;
    }

    @Override
    public void updateState(Preference preference) {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig =
                mTimeManager.getTimeZoneCapabilitiesAndConfig();
        setPreferenceUiState((SwitchPreference) preference, capabilitiesAndConfig);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), KEY_LOCATION_TIME_ZONE_DETECTION_ENABLED)) {
            SwitchPreference switchPreference = (SwitchPreference) preference;
            final boolean switchState = switchPreference.isChecked();

            // Update the settings to match the UI.
            TimeZoneConfiguration configuration = new TimeZoneConfiguration.Builder()
                    .setGeoDetectionEnabled(switchState)
                    .build();

            // The return value is ignored, but the current state is read back below ensuring it
            // does not matter.
            mTimeManager.updateTimeZoneConfiguration(configuration);

            // Configure the UI preference state from the configuration. This means that even in the
            // unlikely event that the update failed, the UI should reflect current settings.
            setPreferenceUiState(switchPreference, mTimeManager.getTimeZoneCapabilitiesAndConfig());

            return true;
        }
        return false;
    }

    /**
     * Sets the switch's checked state from the supplied {@link TimeZoneCapabilitiesAndConfig}.
     */
    @android.annotation.UiThread
    private void setPreferenceUiState(SwitchPreference preference,
            TimeZoneCapabilitiesAndConfig timeZoneCapabilitiesAndConfig) {
        TimeZoneConfiguration configuration = timeZoneCapabilitiesAndConfig.getConfiguration();
        boolean checked = configuration.isGeoDetectionEnabled();
        preference.setChecked(checked);
    }
}
