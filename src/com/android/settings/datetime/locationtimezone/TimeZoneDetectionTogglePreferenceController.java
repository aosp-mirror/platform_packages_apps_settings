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

package com.android.settings.datetime.locationtimezone;

import android.app.time.TimeManager;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneConfiguration;
import android.content.Context;

import com.android.settings.core.TogglePreferenceController;

/**
 * The controller for the "location time zone detection" switch on the location time zone detection
 * screen.
 */
public class TimeZoneDetectionTogglePreferenceController extends TogglePreferenceController {

    private final TimeManager mTimeManager;

    public TimeZoneDetectionTogglePreferenceController(Context context, String key) {
        super(context, key);
        mTimeManager = context.getSystemService(TimeManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig =
                mTimeManager.getTimeZoneCapabilitiesAndConfig();
        TimeZoneConfiguration configuration = capabilitiesAndConfig.getConfiguration();
        return configuration.isGeoDetectionEnabled();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        TimeZoneConfiguration configuration = new TimeZoneConfiguration.Builder()
                .setGeoDetectionEnabled(isChecked)
                .build();
        return mTimeManager.updateTimeZoneConfiguration(configuration);
    }
}
