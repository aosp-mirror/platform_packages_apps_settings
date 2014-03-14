/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.purity;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SeekBarPreference;
import android.provider.Settings;

import com.android.internal.util.cm.DeviceUtils;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.R;


public class NavbarSettings extends SettingsPreferenceFragment implements 
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "NavBar";
    private static final String PREF_STYLE_DIMEN = "navbar_style_dimen_settings";
    private static final String KEY_NAVIGATION_BAR_LEFT = "navigation_bar_left";
    private static final String NAVIGATION_BUTTON_GLOW_TIME = "navigation_button_glow_time";

    PreferenceScreen mStyleDimenPreference;
    private CheckBoxPreference mNavigationBarLeftPref;
    private SeekBarPreference mNavigationButtonGlowTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.navbar_settings);

        PreferenceScreen prefs = getPreferenceScreen();

        mNavigationButtonGlowTime = (SeekBarPreference) findPreference(NAVIGATION_BUTTON_GLOW_TIME);
        mNavigationButtonGlowTime.setProgress(Settings.System.getInt(getContentResolver(),
                  Settings.System.NAVIGATION_BUTTON_GLOW_TIME, 500));
        mNavigationButtonGlowTime.setOnPreferenceChangeListener(this);

        mStyleDimenPreference = (PreferenceScreen) findPreference(PREF_STYLE_DIMEN);
        mNavigationBarLeftPref = (CheckBoxPreference) findPreference(KEY_NAVIGATION_BAR_LEFT);
        if (!Utils.isPhone(getActivity())) {
            getPreferenceScreen().removePreference(mNavigationBarLeftPref);
            mNavigationBarLeftPref = null;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mNavigationButtonGlowTime) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NAVIGATION_BUTTON_GLOW_TIME, (Integer)value);
            return true;
        }

         return false;
    }

    private void updateNavbarPreferences(boolean show) {
        mStyleDimenPreference.setEnabled(show);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
