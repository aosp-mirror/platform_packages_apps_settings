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
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class CameraLaserSensorPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String KEY_CAMERA_LASER_SENSOR_SWITCH = "camera_laser_sensor_switch";
    @VisibleForTesting
    static final String BUILD_TYPE = "ro.build.type";
    @VisibleForTesting
    static final String PROPERTY_CAMERA_LASER_SENSOR = "persist.camera.stats.disablehaf";
    @VisibleForTesting
    static final int ENABLED = 0;
    @VisibleForTesting
    static final int DISABLED = 2;
    @VisibleForTesting
    static final String USERDEBUG_BUILD = "userdebug";
    @VisibleForTesting
    static final String ENG_BUILD = "eng";
    @VisibleForTesting
    static final String USER_BUILD = "user";

    public CameraLaserSensorPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(R.bool.config_show_camera_laser_sensor);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_CAMERA_LASER_SENSOR_SWITCH;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        String value = Integer.toString(isEnabled ? ENABLED : DISABLED);
        SystemProperties.set(PROPERTY_CAMERA_LASER_SENSOR, value);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean enabled = isLaserSensorEnabled();
        ((SwitchPreference) mPreference).setChecked(enabled);
    }

    // There should be no impact on the current
    // laser sensor settings in case the developer
    // settings switch is turned on or off!

    private boolean isLaserSensorEnabled() {
        final String prop = SystemProperties.get(PROPERTY_CAMERA_LASER_SENSOR,
                Integer.toString(ENABLED));
        return TextUtils.equals(Integer.toString(ENABLED), prop);
    }

}
