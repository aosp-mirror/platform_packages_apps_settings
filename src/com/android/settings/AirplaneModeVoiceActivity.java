/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings;

import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

/**
 * Activity for modifying the {@link Settings.Global#AIRPLANE_MODE_ON AIRPLANE_MODE_ON}
 * setting using the Voice Interaction API.
 */
public class AirplaneModeVoiceActivity extends VoiceSettingsActivity {
    private static final String TAG = "AirplaneModeVoiceActivity";

    protected void onVoiceSettingInteraction(Intent intent) {
        if (intent.hasExtra(Settings.EXTRA_AIRPLANE_MODE_ENABLED)) {
            boolean enabled =
                    intent.getBooleanExtra(Settings.EXTRA_AIRPLANE_MODE_ENABLED, false);
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, enabled ? 1 : 0);
        } else {
            Log.v(TAG, "Missing airplane mode extra");
        }
    }
}
