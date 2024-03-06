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
import android.content.Intent;
import android.os.GraphicsEnvironment;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.development.DevelopmentSettingsDashboardFragment;
import com.android.settings.development.RebootConfirmationDialogFragment;
import com.android.settings.development.RebootConfirmationDialogHost;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/** Controller to handle the events when user toggles this developer option switch: Enable ANGLE */
public class GraphicsDriverEnableAngleAsSystemDriverController
        extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener,
                PreferenceControllerMixin,
                RebootConfirmationDialogHost {

    private static final String TAG = "GraphicsEnableAngleCtrl";

    private static final String ENABLE_ANELE_AS_SYSTEM_DRIVER_KEY = "enable_angle_as_system_driver";

    private final DevelopmentSettingsDashboardFragment mFragment;

    private final GraphicsDriverSystemPropertiesWrapper mSystemProperties;

    private boolean mShouldToggleSwitchBackOnRebootDialogDismiss;

    @VisibleForTesting
    static final String PROPERTY_PERSISTENT_GRAPHICS_EGL = "persist.graphics.egl";

    @VisibleForTesting
    static final String PROPERTY_DEBUG_ANGLE_DEVELOPER_OPTION =
            "debug.graphics.angle.developeroption.enable";

    @VisibleForTesting static final String ANGLE_DRIVER_SUFFIX = "angle";

    @VisibleForTesting
    static class Injector {
        public GraphicsDriverSystemPropertiesWrapper createSystemPropertiesWrapper() {
            return new GraphicsDriverSystemPropertiesWrapper() {
                @Override
                public String get(String key, String def) {
                    return SystemProperties.get(key, def);
                }

                @Override
                public void set(String key, String val) {
                    SystemProperties.set(key, val);
                }

                @Override
                public boolean getBoolean(String key, boolean def) {
                    return SystemProperties.getBoolean(key, def);
                }
            };
        }
    }

    public GraphicsDriverEnableAngleAsSystemDriverController(
            Context context, DevelopmentSettingsDashboardFragment fragment) {
        this(context, fragment, new Injector());
    }

    // Return true if the ANGLE developer option entry point is enabled.
    // This can be enabled by calling:
    //     `adb shell setprop debug.graphics.angle.developeroption.enable true`
    private boolean isAngleDeveloperOptionEnabled() {
        return mSystemProperties.getBoolean(PROPERTY_DEBUG_ANGLE_DEVELOPER_OPTION, false);
    }

    @VisibleForTesting
    GraphicsDriverEnableAngleAsSystemDriverController(
            Context context, DevelopmentSettingsDashboardFragment fragment, Injector injector) {
        super(context);
        mFragment = fragment;
        mSystemProperties = injector.createSystemPropertiesWrapper();
        // By default, when the reboot dialog is dismissed we want to toggle the switch back.
        // Exception is when user chooses to reboot now, the switch should keep its current value
        // and persist its' state over reboot.
        mShouldToggleSwitchBackOnRebootDialogDismiss = true;
        final String persistGraphicsEglValue =
                mSystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL, "");
        Log.v(TAG, "Value of " + PROPERTY_PERSISTENT_GRAPHICS_EGL + " is: "
                + persistGraphicsEglValue);
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
        showRebootDialog();
        return true;
    }

    @VisibleForTesting
    void showRebootDialog() {
        RebootConfirmationDialogFragment.show(
                mFragment,
                R.string.reboot_dialog_enable_angle_as_system_driver,
                R.string.cancel,
                this);
    }

    /** Return the default value of "persist.graphics.egl" */
    public boolean isDefaultValue() {
        final String currentGlesDriver =
                mSystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL, "");
        // default value of "persist.graphics.egl" is ""
        return TextUtils.isEmpty(currentGlesDriver);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        // set switch on if "persist.graphics.egl" is "angle".
        final String currentGlesDriver =
                mSystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL, "");
        final boolean isAngle = TextUtils.equals(ANGLE_DRIVER_SUFFIX, currentGlesDriver);
        ((TwoStatePreference) mPreference).setChecked(isAngle);

        // Disable the developer option toggle UI if ANGLE is disabled, this means next time the
        // debug property needs to be set to true again to enable ANGLE. If ANGLE is enabled, don't
        // disable the developer option toggle UI so that it can be turned off easily.
        if (!isAngleDeveloperOptionEnabled() && !((TwoStatePreference) mPreference).isChecked()) {
            mPreference.setEnabled(false);
        }
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        // 1) disable the switch
        super.onDeveloperOptionsSwitchDisabled();
        // 2) set the persist.graphics.egl empty string
        GraphicsEnvironment.getInstance().toggleAngleAsSystemDriver(false);
        // 3) reset the switch
        ((TwoStatePreference) mPreference).setChecked(false);
    }

    void toggleSwitchBack() {
        final String currentGlesDriver =
                mSystemProperties.get(PROPERTY_PERSISTENT_GRAPHICS_EGL, "");
        if (TextUtils.equals(ANGLE_DRIVER_SUFFIX, currentGlesDriver)) {
            // if persist.graphics.egl = "angle", set the property value back to ""
            GraphicsEnvironment.getInstance().toggleAngleAsSystemDriver(false);
            // toggle switch off
            ((TwoStatePreference) mPreference).setChecked(false);
            return;
        }

        if (TextUtils.isEmpty(currentGlesDriver)) {
            // if persist.graphicx.egl = "", set the persist.graphics.egl back to "angle"
            GraphicsEnvironment.getInstance().toggleAngleAsSystemDriver(true);
            // toggle switch on
            ((TwoStatePreference) mPreference).setChecked(true);
            return;
        }

        // if persist.graphics.egl holds values other than the above two, log error message
        Log.e(TAG, "Invalid persist.graphics.egl property value");
    }

    @VisibleForTesting
    void rebootDevice(Context context) {
        final Intent intent = new Intent(Intent.ACTION_REBOOT);
        context.startActivity(intent);
    }

    @Override
    public void onRebootConfirmed(Context context) {
        // User chooses to reboot now, do not toggle switch back
        mShouldToggleSwitchBackOnRebootDialogDismiss = false;

        // Reboot the device
        rebootDevice(context);
    }

    @Override
    public void onRebootCancelled() {
        // User chooses to cancel reboot, toggle switch back
        mShouldToggleSwitchBackOnRebootDialogDismiss = true;
    }

    @Override
    public void onRebootDialogDismissed() {
        // If reboot dialog is dismissed either from
        // 1) User clicks cancel
        // 2) User taps phone screen area outside of reboot dialog
        // do not reboot the device, and toggles switch back.
        if (mShouldToggleSwitchBackOnRebootDialogDismiss) {
            toggleSwitchBack();
        }

        // Reset the flag so that the default option is to toggle switch back
        // on reboot dialog dismissed.
        mShouldToggleSwitchBackOnRebootDialogDismiss = true;
    }
}
