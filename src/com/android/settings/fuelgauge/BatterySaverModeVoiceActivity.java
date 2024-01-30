/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import static android.provider.Settings.EXTRA_BATTERY_SAVER_MODE_ENABLED;

import static com.android.settingslib.fuelgauge.BatterySaverLogging.SAVER_ENABLED_VOICE;

import android.content.Intent;
import android.util.Log;

import com.android.settings.utils.VoiceSettingsActivity;
import com.android.settingslib.fuelgauge.BatterySaverUtils;

/**
 * Activity for modifying the {@link android.os.PowerManager} power save mode setting using the
 * Voice Interaction API.
 */
public class BatterySaverModeVoiceActivity extends VoiceSettingsActivity {
    private static final String TAG = "BatterySaverModeVoiceActivity";

    @Override
    protected boolean onVoiceSettingInteraction(Intent intent) {
        if (intent.hasExtra(EXTRA_BATTERY_SAVER_MODE_ENABLED)) {
            if (BatterySaverUtils.setPowerSaveMode(
                    this,
                    intent.getBooleanExtra(EXTRA_BATTERY_SAVER_MODE_ENABLED, false),
                    /* needFirstTimeWarning= */ true,
                    SAVER_ENABLED_VOICE)) {
                notifySuccess(null);
            } else {
                Log.v(TAG, "Unable to set power mode");
                notifyFailure(null);
            }
        } else {
            Log.v(TAG, "Missing battery saver mode extra");
        }
        return true;
    }
}
