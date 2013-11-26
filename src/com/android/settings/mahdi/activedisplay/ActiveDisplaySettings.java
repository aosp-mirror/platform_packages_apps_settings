/*
 * Copyright (C) 2013 The ChameleonOS Project
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

package com.android.settings.mahdi.activedisplay;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.android.settings.mahdi.activedisplay.SeekBarPreference;
import com.android.settings.mahdi.activedisplay.AppMultiSelectListPreference;

import static android.hardware.Sensor.TYPE_LIGHT;
import static android.hardware.Sensor.TYPE_PROXIMITY;

public class ActiveDisplaySettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {
    private static final String TAG = "ActiveDisplaySettings";

    private static final String KEY_ENABLED = "ad_enable";
    private static final String KEY_SHOW_TEXT = "ad_text";
    private static final String KEY_ALL_NOTIFICATIONS = "ad_all_notifications";
    private static final String KEY_HIDE_LOW_PRIORITY = "ad_hide_low_priority";
    private static final String KEY_POCKET_MODE = "ad_pocket_mode";
    private static final String KEY_SUNLIGHT_MODE = "ad_sunlight_mode";
    private static final String KEY_REDISPLAY = "ad_redisplay";
    private static final String KEY_EXCLUDED_APPS = "ad_excluded_apps";
    private static final String KEY_SHOW_DATE = "ad_show_date";
    private static final String KEY_SHOW_AMPM = "ad_show_ampm";
    private static final String KEY_BRIGHTNESS = "ad_brightness";

    private SwitchPreference mEnabledPref;
    private CheckBoxPreference mShowTextPref;
    private CheckBoxPreference mShowDatePref;
    private CheckBoxPreference mShowAmPmPref;
    private CheckBoxPreference mAllNotificationsPref;
    private CheckBoxPreference mHideLowPriorityPref;
    private ListPreference mPocketModePref;
    private CheckBoxPreference mSunlightModePref;
    private ListPreference mRedisplayPref;
    private SeekBarPreference mBrightnessLevel;
    private AppMultiSelectListPreference mExcludedAppsPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.active_display_settings);

        mEnabledPref = (SwitchPreference) findPreference(KEY_ENABLED);
        mEnabledPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.ENABLE_ACTIVE_DISPLAY, 0) == 1));
        mEnabledPref.setOnPreferenceChangeListener(this);

        mShowTextPref = (CheckBoxPreference) findPreference(KEY_SHOW_TEXT);
        mShowTextPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_TEXT, 0) == 1));

        mAllNotificationsPref = (CheckBoxPreference) findPreference(KEY_ALL_NOTIFICATIONS);
        mAllNotificationsPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_ALL_NOTIFICATIONS, 0) == 1));

        mHideLowPriorityPref = (CheckBoxPreference) findPreference(KEY_HIDE_LOW_PRIORITY);
        mHideLowPriorityPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_HIDE_LOW_PRIORITY_NOTIFICATIONS, 0) == 1));

        mPocketModePref = (ListPreference) findPreference(KEY_POCKET_MODE);
        mPocketModePref.setOnPreferenceChangeListener(this);
        int mode = Settings.System.getInt(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_POCKET_MODE, 0);
        mPocketModePref.setValue(String.valueOf(mode));
        updatePocketModeSummary(mode);
        if (!hasProximitySensor()) {
            getPreferenceScreen().removePreference(mPocketModePref);
        }

        mSunlightModePref = (CheckBoxPreference) findPreference(KEY_SUNLIGHT_MODE);
        mSunlightModePref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_SUNLIGHT_MODE, 0) == 1));
        if (!hasLightSensor()) {
            getPreferenceScreen().removePreference(mSunlightModePref);
        }

        PreferenceScreen prefSet = getPreferenceScreen();
        mRedisplayPref = (ListPreference) prefSet.findPreference(KEY_REDISPLAY);
        mRedisplayPref.setOnPreferenceChangeListener(this);
        long timeout = Settings.System.getLong(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_REDISPLAY, 0);
        mRedisplayPref.setValue(String.valueOf(timeout));
        updateRedisplaySummary(timeout);

        mShowDatePref = (CheckBoxPreference) findPreference(KEY_SHOW_DATE);
        mShowDatePref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_SHOW_DATE, 0) == 1));

        mShowAmPmPref = (CheckBoxPreference) findPreference(KEY_SHOW_AMPM);
        mShowAmPmPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_SHOW_AMPM, 0) == 1));

        mBrightnessLevel = (SeekBarPreference) findPreference(KEY_BRIGHTNESS);
        mBrightnessLevel.setValue(Settings.System.getInt(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_BRIGHTNESS, 100));
        mBrightnessLevel.setOnPreferenceChangeListener(this);

        mExcludedAppsPref = (AppMultiSelectListPreference) findPreference(KEY_EXCLUDED_APPS);
        Set<String> excludedApps = getExcludedApps();
        if (excludedApps != null) mExcludedAppsPref.setValues(excludedApps);
        mExcludedAppsPref.setOnPreferenceChangeListener(this);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mRedisplayPref) {
            int timeout = Integer.valueOf((String) newValue);
            updateRedisplaySummary(timeout);
            return true;
        } else if (preference == mPocketModePref) {
            int mode = Integer.valueOf((String) newValue);
            updatePocketModeSummary(mode);
            return true;
        } else if (preference == mEnabledPref) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ENABLE_ACTIVE_DISPLAY,
                    ((Boolean) newValue).booleanValue() ? 1 : 0);
            return true;
        } else if (preference == mBrightnessLevel) {
            int brightness = ((Integer)newValue).intValue();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVE_DISPLAY_BRIGHTNESS, brightness);
            return true;
        } else if (preference == mExcludedAppsPref) {
            storeExcludedApps((Set<String>) newValue);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mShowTextPref) {
            value = mShowTextPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVE_DISPLAY_TEXT,
                    value ? 1 : 0);
        } else if (preference == mAllNotificationsPref) {
            value = mAllNotificationsPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVE_DISPLAY_ALL_NOTIFICATIONS,
                    value ? 1 : 0);
        } else if (preference == mHideLowPriorityPref) {
            value = mHideLowPriorityPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVE_DISPLAY_HIDE_LOW_PRIORITY_NOTIFICATIONS,
                    value ? 1 : 0);
        } else if (preference == mSunlightModePref) {
            value = mSunlightModePref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVE_DISPLAY_SUNLIGHT_MODE,
                    value ? 1 : 0);
        } else if (preference == mShowDatePref) {
            value = mShowDatePref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVE_DISPLAY_SHOW_DATE,
                    value ? 1 : 0);
        } else if (preference == mShowAmPmPref) {
            value = mShowAmPmPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ACTIVE_DISPLAY_SHOW_AMPM,
                    value ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    private void updatePocketModeSummary(int value) {
        mPocketModePref.setSummary(
                mPocketModePref.getEntries()[mPocketModePref.findIndexOfValue("" + value)]);
        Settings.System.putInt(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_POCKET_MODE, value);
    }

    private void updateRedisplaySummary(long value) {
        mRedisplayPref.setSummary(mRedisplayPref.getEntries()[mRedisplayPref.findIndexOfValue("" + value)]);
        Settings.System.putLong(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_REDISPLAY, value);
    }

    private boolean hasProximitySensor() {
        SensorManager sm = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        return sm.getDefaultSensor(TYPE_PROXIMITY) != null;
    }

    private boolean hasLightSensor() {
        SensorManager sm = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        return sm.getDefaultSensor(TYPE_LIGHT) != null;
    }

    private Set<String> getExcludedApps() {
        String excluded = Settings.System.getString(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_EXCLUDED_APPS);
        if (TextUtils.isEmpty(excluded))
            return null;

        return new HashSet<String>(Arrays.asList(excluded.split("\\|")));
    }

    private void storeExcludedApps(Set<String> values) {
        StringBuilder builder = new StringBuilder();
        String delimiter = "";
        for (String value : values) {
            builder.append(delimiter);
            builder.append(value);
            delimiter = "|";
        }
        Settings.System.putString(getContentResolver(),
                Settings.System.ACTIVE_DISPLAY_EXCLUDED_APPS, builder.toString());
    }
}
