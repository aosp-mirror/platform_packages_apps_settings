/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.widget.Toast;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class CameraLaserSensorPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_CAMERA_LASER_SENSOR_SWITCH = "camera_laser_sensor_switch";
    @VisibleForTesting
    static final String BUILD_TYPE = "ro.build.type";
    @VisibleForTesting
    static final String PROPERTY_CAMERA_LASER_SENSOR = "persist.camera.stats.disablehaf";
    @VisibleForTesting
    static final int ENABLED = 0;
    @VisibleForTesting
    static final int DISABLED = 2;

    private SwitchPreference mPreference;

    public CameraLaserSensorPreferenceController(Context context) {
        super(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (SwitchPreference) screen.findPreference(KEY_CAMERA_LASER_SENSOR_SWITCH);
        updatePreference();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_CAMERA_LASER_SENSOR_SWITCH;
    }

    @Override
    public boolean isAvailable() {
        String buildType = SystemProperties.get(BUILD_TYPE);
        return mContext.getResources().getBoolean(R.bool.config_show_camera_laser_sensor) &&
               (buildType.equals("userdebug") || buildType.equals("eng"));
    }

    @Override
    public void updateState(Preference preference) {
        updatePreference();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_CAMERA_LASER_SENSOR_SWITCH.equals(preference.getKey())) {
            final SwitchPreference switchPreference = (SwitchPreference)preference;
            String value = Integer.toString(switchPreference.isChecked() ? ENABLED : DISABLED);
            SystemProperties.set(PROPERTY_CAMERA_LASER_SENSOR, value);
            return true;
        }
        return false;
    }

    public void enablePreference(boolean enabled) {
        if (isAvailable()) {
            mPreference.setEnabled(enabled);
        }
    }

    public boolean updatePreference() {
        if (!isAvailable()) {
            return false;
        }
        final boolean enabled = isLaserSensorEnabled();
        mPreference.setChecked(enabled);
        return enabled;
    }

    private boolean isLaserSensorEnabled() {
        String prop = SystemProperties.get(PROPERTY_CAMERA_LASER_SENSOR, Integer.toString(ENABLED));
        return prop.equals(Integer.toString(ENABLED));
    }
}
