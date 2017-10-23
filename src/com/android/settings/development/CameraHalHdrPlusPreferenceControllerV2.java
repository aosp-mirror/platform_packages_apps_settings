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
import android.text.TextUtils;
import android.widget.Toast;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class CameraHalHdrPlusPreferenceControllerV2 extends
        DeveloperOptionsPreferenceController implements
        Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String KEY_CAMERA_HAL_HDRPLUS_SWITCH = "camera_hal_hdrplus_switch";
    @VisibleForTesting
    static final String BUILD_TYPE = "ro.build.type";
    @VisibleForTesting
    static final String PROPERTY_CAMERA_HAL_HDRPLUS = "persist.camera.hdrplus.enable";
    @VisibleForTesting
    static final String ENABLED = "1";
    @VisibleForTesting
    static final String DISABLED = "0";

    private SwitchPreference mPreference;

    public CameraHalHdrPlusPreferenceControllerV2(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        final String buildType = SystemProperties.get(BUILD_TYPE);

        return mContext.getResources().getBoolean(R.bool.config_show_camera_hal_hdrplus);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_CAMERA_HAL_HDRPLUS_SWITCH;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = (SwitchPreference) screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        SystemProperties.set(PROPERTY_CAMERA_HAL_HDRPLUS, isEnabled ? ENABLED : DISABLED);
        Toast.makeText(mContext, R.string.camera_hal_hdrplus_toast, Toast.LENGTH_LONG).show();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean enabled = isHalHdrplusEnabled();
        mPreference.setChecked(enabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        mPreference.setEnabled(true);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        SystemProperties.set(PROPERTY_CAMERA_HAL_HDRPLUS, DISABLED);
        mPreference.setChecked(false);
        mPreference.setEnabled(false);
    }

    private boolean isHalHdrplusEnabled() {
        return SystemProperties.getBoolean(PROPERTY_CAMERA_HAL_HDRPLUS, true /* default */);
    }
}
