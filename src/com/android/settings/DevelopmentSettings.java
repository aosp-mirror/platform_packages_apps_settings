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
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import android.text.TextUtils;

/*
 * Displays preferences for application developers.
 */
public class DevelopmentSettings extends PreferenceActivity {

    private static final String ENABLE_ADB = "enable_adb";
    private static final String KEEP_SCREEN_ON = "keep_screen_on";

    private CheckBoxPreference mEnableAdb;
    private CheckBoxPreference mKeepScreenOn;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.development_prefs);

        mEnableAdb = (CheckBoxPreference) findPreference(ENABLE_ADB);
        mKeepScreenOn = (CheckBoxPreference) findPreference(KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        mEnableAdb.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.ADB_ENABLED, 0) != 0);
        mKeepScreenOn.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.STAY_ON_WHILE_PLUGGED_IN, 0) != 0);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        // Those monkeys kept committing suicide, so we add this property
        // to disable this functionality
        if (!TextUtils.isEmpty(SystemProperties.get("ro.monkey"))) {
            return false;
        }

        if (preference == mEnableAdb) {
            Settings.System.putInt(getContentResolver(), Settings.System.ADB_ENABLED, 
                    mEnableAdb.isChecked() ? 1 : 0);
        } else if (preference == mKeepScreenOn) {
            Settings.System.putInt(getContentResolver(), Settings.System.STAY_ON_WHILE_PLUGGED_IN, 
                    mKeepScreenOn.isChecked() ? 1 : 0);
        }
        
        return false;
    }
}
