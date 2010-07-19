/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;

public class Settings extends PreferenceActivity {

    private static final String KEY_PARENT = "parent";
    private static final String KEY_CALL_SETTINGS = "call_settings";
    private static final String KEY_SYNC_SETTINGS = "sync_settings";
    private static final String KEY_SEARCH_SETTINGS = "search_settings";
    private static final String KEY_DOCK_SETTINGS = "dock_settings";
    
    private static final String KEY_OPERATOR_SETTINGS = "operator_settings";
    private static final String KEY_MANUFACTURER_SETTINGS = "manufacturer_settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.settings);

        PreferenceGroup parent = (PreferenceGroup) findPreference(KEY_PARENT);
        Utils.updatePreferenceToSpecificActivityOrRemove(this, parent, KEY_SYNC_SETTINGS, 0);
        Utils.updatePreferenceToSpecificActivityOrRemove(this, parent, KEY_SEARCH_SETTINGS, 0);

        Preference dockSettings = parent.findPreference(KEY_DOCK_SETTINGS);
        if (getResources().getBoolean(R.bool.has_dock_settings) == false && dockSettings != null) {
            parent.removePreference(dockSettings);
        }

        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(this, parent,
                KEY_OPERATOR_SETTINGS);
        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(this, parent,
                KEY_MANUFACTURER_SETTINGS);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        findPreference(KEY_CALL_SETTINGS).setEnabled(!AirplaneModeEnabler.isAirplaneModeOn(this));
    }

}
