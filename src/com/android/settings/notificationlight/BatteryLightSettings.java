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

package com.android.settings.notificationlight;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class BatteryLightSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "BatteryLightSettings";

    private static final String ENABLED_PREF = "battery_light";
    private static final String PULSE_PREF = "battery_low_pulse";
    private static final String LOW_COLOR_PREF = "low_color";
    private static final String MEDIUM_COLOR_PREF = "medium_color";
    private static final String FULL_COLOR_PREF = "full_color";

    private boolean mLightEnabled;
    private boolean mLightPulse;
    private boolean mMultiColorLed;
    private CheckBoxPreference mPulsePref;
    private ApplicationLightPreference mLowColorPref;
    private ApplicationLightPreference mMediumColorPref;
    private ApplicationLightPreference mFullColorPref;
    private static final int MENU_RESET = Menu.FIRST;
    private Menu mOptionsMenu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.battery_light_settings);

        // Retrieve general settings
        ContentResolver resolver = getContentResolver();
        mLightEnabled = Settings.System.getInt(resolver,
                Settings.System.BATTERY_LIGHT_ENABLED, 1) == 1;
        mLightPulse = Settings.System.getInt(resolver,
                Settings.System.BATTERY_LIGHT_PULSE, 1) == 1;

        // Does the Device support changing battery LED colors?
        mMultiColorLed = getResources().getBoolean(com.android.internal.R.bool.config_multiColorBatteryLed);

        if(mMultiColorLed) {
            // Only used when the colors are changeable
            setHasOptionsMenu(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDefault();
    }

    private void refreshDefault() {
        ContentResolver resolver = getContentResolver();
        int lowColor = Settings.System.getInt(resolver,
                Settings.System.BATTERY_LIGHT_LOW_COLOR,
                getResources().getInteger(com.android.internal.R.integer.config_notificationsBatteryLowARGB));
        int mediumColor = Settings.System.getInt(resolver,
                Settings.System.BATTERY_LIGHT_MEDIUM_COLOR,
                getResources().getInteger(com.android.internal.R.integer.config_notificationsBatteryMediumARGB));
        int fullColor = Settings.System.getInt(resolver,
                Settings.System.BATTERY_LIGHT_FULL_COLOR,
                getResources().getInteger(com.android.internal.R.integer.config_notificationsBatteryFullARGB));

        PreferenceScreen prefSet = getPreferenceScreen();
        PreferenceGroup generalPrefs = (PreferenceGroup) prefSet.findPreference("general_section");
        if (generalPrefs != null) {

            // Enabled preference
            CheckBoxPreference cPref = (CheckBoxPreference) prefSet.findPreference(ENABLED_PREF);
            cPref.setChecked(mLightEnabled);
            cPref.setOnPreferenceChangeListener(this);

            // Custom enabled preference
            mPulsePref = (CheckBoxPreference) prefSet.findPreference(PULSE_PREF);
            mPulsePref.setChecked(mLightPulse);
            mPulsePref.setEnabled(mLightEnabled);
            mPulsePref.setOnPreferenceChangeListener(this);
        }

        PreferenceGroup colorPrefs = (PreferenceGroup) prefSet.findPreference("colors_list");
        if (colorPrefs != null) {
            if (!mMultiColorLed) {
                prefSet.removePreference(colorPrefs);
            } else {
                // Low, Medium and full color preferences
                mLowColorPref = (ApplicationLightPreference) prefSet.findPreference(LOW_COLOR_PREF);
                mLowColorPref.setAllValues(lowColor, 0, 0, false);
                mLowColorPref.setEnabled(mLightEnabled);
                mLowColorPref.setOnPreferenceChangeListener(this);

                mMediumColorPref = (ApplicationLightPreference) prefSet.findPreference(MEDIUM_COLOR_PREF);
                mMediumColorPref.setAllValues(mediumColor, 0, 0, false);
                mMediumColorPref.setEnabled(mLightEnabled);
                mMediumColorPref.setOnPreferenceChangeListener(this);

                mFullColorPref = (ApplicationLightPreference) prefSet.findPreference(FULL_COLOR_PREF);
                mFullColorPref.setAllValues(fullColor, 0, 0, false);
                mFullColorPref.setEnabled(mLightEnabled);
                mFullColorPref.setOnPreferenceChangeListener(this);
            }
        }
    }

    /**
     * Updates the default or application specific notification settings.
     *
     * @param key of the specific setting to update
     * @param color
     */
    protected void updateValues(String key, Integer color) {
        ContentResolver resolver = getContentResolver();

        if (key.equals(LOW_COLOR_PREF)) {
            Settings.System.putInt(resolver, Settings.System.BATTERY_LIGHT_LOW_COLOR, color);
            refreshDefault();
            return;
        } else if (key.equals(MEDIUM_COLOR_PREF)) {
            Settings.System.putInt(resolver, Settings.System.BATTERY_LIGHT_MEDIUM_COLOR, color);
            refreshDefault();
            return;
        } else if (key.equals(FULL_COLOR_PREF)) {
            Settings.System.putInt(resolver, Settings.System.BATTERY_LIGHT_FULL_COLOR, color);
            refreshDefault();
            return;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mOptionsMenu = menu;

        mOptionsMenu.add(0, MENU_RESET, 0, R.string.profile_reset_title)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setAlphabeticShortcut('r')
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetColors();
                return true;
        }
        return false;
    }


    protected void resetColors() {
        ContentResolver resolver = getContentResolver();
        Resources res = getResources();

        // Reset to the framework default colors
        Settings.System.putInt(resolver, Settings.System.BATTERY_LIGHT_LOW_COLOR,
                res.getInteger(com.android.internal.R.integer.config_notificationsBatteryLowARGB));
        Settings.System.putInt(resolver, Settings.System.BATTERY_LIGHT_MEDIUM_COLOR,
                res.getInteger(com.android.internal.R.integer.config_notificationsBatteryMediumARGB));
        Settings.System.putInt(resolver, Settings.System.BATTERY_LIGHT_FULL_COLOR,
                res.getInteger(com.android.internal.R.integer.config_notificationsBatteryFullARGB));
        refreshDefault();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        String key = preference.getKey();

        if (ENABLED_PREF.equals(key)) {
            mLightEnabled = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), Settings.System.BATTERY_LIGHT_ENABLED,
                    mLightEnabled ? 1 : 0);

            mPulsePref.setEnabled(mLightEnabled);
            if(mMultiColorLed) {
                mLowColorPref.setEnabled(mLightEnabled);
                mMediumColorPref.setEnabled(mLightEnabled);
                mFullColorPref.setEnabled(mLightEnabled);
            }
        } else if (PULSE_PREF.equals(key)) {
            mLightPulse = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), Settings.System.BATTERY_LIGHT_PULSE,
                    mLightPulse ? 1 : 0);
        } else {
            ApplicationLightPreference tPref = (ApplicationLightPreference) preference;
            updateValues(key, tPref.getColor());
        }
        return true;
    }
}
