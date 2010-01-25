/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;

import java.util.List;

/**
 * Settings screen for voice input/output.
 */
public class VoiceInputOutputSettings extends PreferenceActivity {
    
    private static final String KEY_PARENT = "parent";
    private static final String KEY_VOICE_SEARCH_SETTINGS = "voice_search_settings";
    private static final String KEY_KEYBOARD_SETTINGS = "keyboard_settings";
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.voice_input_output_settings);
        
        removePreferenceIfNecessary(KEY_VOICE_SEARCH_SETTINGS);
        removePreferenceIfNecessary(KEY_KEYBOARD_SETTINGS);
    }

    /**
     * Removes a preference if there is no activity to handle its intent.
     */
    private void removePreferenceIfNecessary(String preferenceKey) {
        PreferenceGroup parent = (PreferenceGroup) findPreference(KEY_PARENT);
        
        Preference preference = parent.findPreference(preferenceKey);
        if (preference == null) {
            return;
        }

        Intent intent = preference.getIntent();
        if (intent != null) {
            PackageManager pm = getPackageManager();
            if (!pm.queryIntentActivities(intent, 0).isEmpty()) {
                return;
            }
        }
        
        // Did not find a matching activity, so remove the preference.
        parent.removePreference(preference);
    }
}
