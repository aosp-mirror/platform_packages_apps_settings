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

import android.content.ContentResolver;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;
import android.provider.Settings.System;

public class LanguageSettings extends PreferenceActivity {
    
    private final String[] mSettingsUiKey = {
            "auto_caps",
            "auto_replace",
            "auto_punctuate",
    };
    
    // Note: Order of this array should correspond to the order of the above array
    private final String[] mSettingsSystemId = {
            System.TEXT_AUTO_CAPS,
            System.TEXT_AUTO_REPLACE,
            System.TEXT_AUTO_PUNCTUATE,
    };

    // Note: Order of this array should correspond to the order of the above array
    private final int[] mSettingsDefault = {
            1,
            1,
            1,
    };
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.language_settings);

        if (getAssets().getLocales().length == 1) {
            getPreferenceScreen().
                removePreference(findPreference("language_category"));
        }
    
        ContentResolver resolver = getContentResolver();
        for (int i = 0; i < mSettingsUiKey.length; i++) {
            CheckBoxPreference pref = (CheckBoxPreference) findPreference(mSettingsUiKey[i]);
            pref.setChecked(System.getInt(resolver, mSettingsSystemId[i],
                                          mSettingsDefault[i]) > 0);
        }
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        for (int i = 0; i < mSettingsUiKey.length; i++) {
            if (mSettingsUiKey[i].equals(preference.getKey())) {
                System.putInt(getContentResolver(), mSettingsSystemId[i], 
                        ((CheckBoxPreference)preference).isChecked()? 1 : 0);
                return true;
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

}
