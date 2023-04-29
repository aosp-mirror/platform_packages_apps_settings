/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.development.graphicsdriver;

import android.content.Context;
import android.os.GraphicsEnvironment;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.development.DevelopmentSettingsDashboardFragment;
import com.android.settings.development.RebootConfirmationDialogFragment;
import com.android.settings.development.RebootConfirmationDialogHost;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * Controller to handle the events when user toggles this developer option switch: Enable ANGLE
 */
public class GraphicsDriverEnableAngleAsSystemDriverController
        extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener,
                PreferenceControllerMixin,
                RebootConfirmationDialogHost {

    private static final String TAG = "GraphicsEnableAngleCtrl";

    private static final String ENABLE_ANELE_AS_SYSTEM_DRIVER_KEY = "enable_angle_as_system_driver";

    private final DevelopmentSettingsDashboardFragment mFragment;

    @VisibleForTesting
    static final String PROPERTY_RO_GFX_ANGLE_SUPPORTED = "ro.gfx.angle.supported";

    @VisibleForTesting
    static final String PROPERTY_PERSISTENT_GRAPHICS_EGL = "persist.graphics.egl";

    @VisibleForTesting
    static final String ANGLE_DRIVER_SUFFIX = "angle";


    public GraphicsDriverEnableAngleAsSystemDriverController(
            Context context, DevelopmentSettingsDashboardFragment fragment) {
        super(context);
        mFragment = fragment;
    }

    @Override
    public String getPreferenceKey() {
        return ENABLE_ANELE_AS_SYSTEM_DRIVER_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean enableAngleAsSystemDriver = (Boolean) newValue;
        // set "persist.graphics.egl" to "angle" if enableAngleAsSystemDriver is true
        // set "persist.graphics.egl" to "" if enableAngleAsSystemDriver is false
        GraphicsEnvironment.getInstance().toggleAngleAsSystemDriver(enableAngleAsSystemDriver);
        // pop up a window asking user to reboot to make the new "persist.graphics.egl" take effect
        RebootConfirmationDialogFragment.show(
                mFragment, R.string.reboot_dialog_enable_angle_as_system_driver,
                R.string.cancel, this);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        // set switch on if "persist.graphics.egl" is "angle" and angle is built in /vendor
        // set switch off otherwise.
        final String currentGlesDriver = SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL);
        final boolean isAngle = TextUtils.equals(ANGLE_DRIVER_SUFFIX, currentGlesDriver);
        final boolean isAngleSupported =
                TextUtils.equals(SystemProperties.get(PROPERTY_RO_GFX_ANGLE_SUPPORTED), "true");
        ((SwitchPreference) mPreference).setChecked(isAngle && isAngleSupported);
        ((SwitchPreference) mPreference).setEnabled(isAngleSupported);
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        // only enable the switch if ro.gfx.angle.supported is true
        // we use ro.gfx.angle.supported to indicate if ANGLE libs are installed under /vendor
        final boolean isAngleSupported =
                TextUtils.equals(SystemProperties.get(PROPERTY_RO_GFX_ANGLE_SUPPORTED), "true");
        ((SwitchPreference) mPreference).setEnabled(isAngleSupported);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        // 1) set the persist.graphics.egl empty string
        GraphicsEnvironment.getInstance().toggleAngleAsSystemDriver(false);
        // 2) reset the switch
        ((SwitchPreference) mPreference).setChecked(false);
        // 3) disable switch
        ((SwitchPreference) mPreference).setEnabled(false);
    }

    @Override
    public void onRebootCancelled() {
        // if user presses button "Cancel", do not reboot the device, and toggles switch back
        final String currentGlesDriver = SystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL);
        if (TextUtils.equals(ANGLE_DRIVER_SUFFIX, currentGlesDriver)) {
            // if persist.graphics.egl = "angle", set the property value back to ""
            GraphicsEnvironment.getInstance().toggleAngleAsSystemDriver(false);
            // toggle switch off
            ((SwitchPreference) mPreference).setChecked(false);
            return;
        }

        if (TextUtils.isEmpty(currentGlesDriver)) {
            // if persist.graphicx.egl = "", set the persist.graphics.egl back to "angle"
            GraphicsEnvironment.getInstance().toggleAngleAsSystemDriver(true);
            // toggle switch on
            ((SwitchPreference) mPreference).setChecked(true);
            return;
        }

        // if persist.graphics.egl holds values other than the above two, log error message
        Log.e(TAG, "Invalid persist.graphics.egl property value");
    }
}
