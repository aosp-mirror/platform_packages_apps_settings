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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Activity for modifying a setting using the Voice Interaction API. This activity
 * MUST only modify the setting if the intent was sent using
 * {@link android.service.voice.VoiceInteractionSession#startVoiceActivity startVoiceActivity}.
 */
abstract public class VoiceSettingsActivity extends Activity {

    private static final String TAG = "VoiceSettingsActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isVoiceInteraction()) {
            // Only permit if this is a voice interaction.
            onVoiceSettingInteraction(getIntent());
        } else {
            Log.v(TAG, "Cannot modify settings without voice interaction");
        }
        finish();
    }

    /**
     * Modify the setting as a voice interaction. The activity will finish
     * after this method is called.
     */
    abstract protected void onVoiceSettingInteraction(Intent intent);
}
