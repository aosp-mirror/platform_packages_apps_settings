/*
 * Copyright (C) 2013 SlimRoms Project
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

package com.android.settings.mahdi;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.Utils;

import com.android.internal.util.mahdi.DeviceUtils;
import com.android.internal.widget.LockPatternUtils;

public class LockscreenWidgets extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {

    private static final String TAG = "LockscreenWidgets";

    private static final String KEY_ENABLE_WIDGETS =
            "lockscreen_enable_widgets";
    private static final String KEY_LOCKSCREEN_CAMERA_WIDGET =
            "lockscreen_camera_widget";
    private static final String KEY_LOCKSCREEN_MAXIMIZE_WIDGETS =
            "lockscreen_maximize_widgets";
    private static final String KEY_LOCKSCREEN_DISABLE_HINTS =
            "lockscreen_disable_hints";
    private static final String PREF_LOCKSCREEN_USE_CAROUSEL =
            "lockscreen_use_widget_container_carousel";

    private CheckBoxPreference mEnableWidgets;
    private CheckBoxPreference mCameraWidget;
    private CheckBoxPreference mMaximizeWidgets;
    private CheckBoxPreference mLockscreenHints;
    private CheckBoxPreference mLockscreenUseCarousel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lockscreen_interface_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        mEnableWidgets = (CheckBoxPreference) findPreference(KEY_ENABLE_WIDGETS);
        final boolean enabled = new LockPatternUtils(getActivity()).getWidgetsEnabled();
        if (!enabled) {
            mEnableWidgets.setSummary(R.string.disabled);
        } else {
            mEnableWidgets.setSummary(R.string.enabled);
        }
        mEnableWidgets.setChecked(enabled);
        mEnableWidgets.setOnPreferenceChangeListener(this);

        Resources keyguardResources = null;
        PackageManager pm = getPackageManager();
        try {
            keyguardResources = pm.getResourcesForApplication("com.android.keyguard");
        } catch (Exception e) {
            e.printStackTrace();
        }

        final boolean cameraDefault = keyguardResources != null
                ? keyguardResources.getBoolean(keyguardResources.getIdentifier(
                "com.android.keyguard:bool/kg_enable_camera_default_widget", null, null)) : false;

        DevicePolicyManager dpm = (DevicePolicyManager)getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        mCameraWidget = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_CAMERA_WIDGET);
        if (dpm.getCameraDisabled(null)
                || (dpm.getKeyguardDisabledFeatures(null)
                    & DevicePolicyManager.KEYGUARD_DISABLE_SECURE_CAMERA) != 0) {
            prefSet.removePreference(mCameraWidget);
        } else {
            mCameraWidget.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_CAMERA_WIDGET, cameraDefault ? 1 : 0) == 1);
            mCameraWidget.setOnPreferenceChangeListener(this);
        }

        mMaximizeWidgets = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_MAXIMIZE_WIDGETS);
        if (!DeviceUtils.isPhone(getActivity())) {
            if (mMaximizeWidgets != null) {
                prefSet.removePreference(mMaximizeWidgets);
            }
        } else {
            mMaximizeWidgets.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_MAXIMIZE_WIDGETS, 0) == 1);
        }
        mMaximizeWidgets.setOnPreferenceChangeListener(this);

        mLockscreenHints = (CheckBoxPreference)findPreference(KEY_LOCKSCREEN_DISABLE_HINTS);
        mLockscreenHints.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_DISABLE_HINTS, 1) == 1);
        mLockscreenHints.setOnPreferenceChangeListener(this);

        mLockscreenUseCarousel = (CheckBoxPreference)findPreference(PREF_LOCKSCREEN_USE_CAROUSEL);
        mLockscreenUseCarousel.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_USE_WIDGET_CONTAINER_CAROUSEL, 0) == 1);
        mLockscreenUseCarousel.setOnPreferenceChangeListener(this);

    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mEnableWidgets) {
            new LockPatternUtils(getActivity()).setWidgetsEnabled((Boolean) newValue);
            if ((Boolean) newValue) {
                mEnableWidgets.setSummary(R.string.enabled);
            } else {
                mEnableWidgets.setSummary(R.string.disabled);
            }
            return true;
        } else if (preference == mCameraWidget) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_CAMERA_WIDGET,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mMaximizeWidgets) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_MAXIMIZE_WIDGETS,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mLockscreenHints) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_DISABLE_HINTS,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mLockscreenUseCarousel) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_USE_WIDGET_CONTAINER_CAROUSEL,
                    (Boolean) newValue ? 1 : 0);
            return true;
        }
        return false;
    }

}

