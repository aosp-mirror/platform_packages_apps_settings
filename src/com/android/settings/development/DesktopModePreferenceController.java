/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static android.provider.Settings.Global.DEVELOPMENT_OVERRIDE_DESKTOP_MODE_FEATURES;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.window.flags.Flags;

/**
 * Preference controller to control Desktop mode features
 */
public class DesktopModePreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin,
        RebootConfirmationDialogHost {

    private static final String OVERRIDE_DESKTOP_MODE_FEATURES_KEY =
            "override_desktop_mode_features";

    private static final String TAG = "DesktopModePreferenceController";

    @VisibleForTesting
    static final int SETTING_VALUE_OFF = 0;
    @VisibleForTesting
    static final int SETTING_VALUE_ON = 1;
    @VisibleForTesting
    static final int SETTING_VALUE_UNSET = -1;

    @Nullable
    private final DevelopmentSettingsDashboardFragment mFragment;

    public DesktopModePreferenceController(
            Context context, @Nullable DevelopmentSettingsDashboardFragment fragment) {
        super(context);
        mFragment = fragment;
    }

    @Override
    public boolean isAvailable() {
        return isDeviceEligibleForDesktopMode() && Flags.showDesktopWindowingDevOption();
    }

    @Override
    public String getPreferenceKey() {
        return OVERRIDE_DESKTOP_MODE_FEATURES_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(),
                DEVELOPMENT_OVERRIDE_DESKTOP_MODE_FEATURES,
                isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        if (mFragment != null) {
            RebootConfirmationDialogFragment.show(
                    mFragment, R.string.reboot_dialog_override_desktop_mode, this);
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        // Use overridden state, if not present, then use default state
        final boolean shouldDevOptionBeEnabledByDefault = Flags.enableDesktopWindowingMode();
        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                DEVELOPMENT_OVERRIDE_DESKTOP_MODE_FEATURES,
                shouldDevOptionBeEnabledByDefault ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        final boolean shouldDevOptionBeEnabled = switch (mode) {
            case SETTING_VALUE_OFF -> false;
            case SETTING_VALUE_ON -> true;
            case SETTING_VALUE_UNSET -> shouldDevOptionBeEnabledByDefault;
            default -> {
                Log.w(TAG, "Invalid override for desktop mode: " + mode);
                yield shouldDevOptionBeEnabledByDefault;
            }
        };
        ((TwoStatePreference) mPreference).setChecked(shouldDevOptionBeEnabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(mContext.getContentResolver(),
                DEVELOPMENT_OVERRIDE_DESKTOP_MODE_FEATURES, SETTING_VALUE_UNSET);
    }

    private boolean isDeviceEligibleForDesktopMode() {
        boolean enforceDeviceRestrictions = SystemProperties.getBoolean(
                "persist.wm.debug.desktop_mode_enforce_device_restrictions", true);
        boolean isDesktopModeSupported = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_isDesktopModeSupported);
        return !enforceDeviceRestrictions || isDesktopModeSupported;
    }
}
