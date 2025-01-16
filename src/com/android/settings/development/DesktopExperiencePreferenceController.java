/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static android.provider.Settings.Global.DEVELOPMENT_OVERRIDE_DESKTOP_EXPERIENCE_FEATURES;
import static android.window.DesktopModeFlags.ToggleOverride.OVERRIDE_OFF;
import static android.window.DesktopModeFlags.ToggleOverride.OVERRIDE_ON;
import static android.window.DesktopModeFlags.ToggleOverride.OVERRIDE_UNSET;
import static android.window.DesktopModeFlags.ToggleOverride.fromSetting;

import android.content.Context;
import android.provider.Settings;
import android.window.DesktopModeFlags;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.wm.shell.shared.desktopmode.DesktopModeStatus;

public class DesktopExperiencePreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin, RebootConfirmationDialogHost {
    private static final String OVERRIDE_DESKTOP_EXPERIENCE_FEATURES_KEY =
            "override_desktop_experience_features";

    @Nullable
    private final DevelopmentSettingsDashboardFragment mFragment;

    public DesktopExperiencePreferenceController(
            Context context, @Nullable DevelopmentSettingsDashboardFragment fragment) {
        super(context);
        mFragment = fragment;
    }

    @Override
    public boolean isAvailable() {
        return DesktopModeStatus.canShowDesktopExperienceDevOption(mContext);
    }

    @Override
    public String getPreferenceKey() {
        return OVERRIDE_DESKTOP_EXPERIENCE_FEATURES_KEY;
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(),
                DEVELOPMENT_OVERRIDE_DESKTOP_EXPERIENCE_FEATURES,
                isEnabled ? OVERRIDE_ON.getSetting() : OVERRIDE_OFF.getSetting());
        if (mFragment != null) {
            RebootConfirmationDialogFragment.show(
                    mFragment, R.string.reboot_dialog_override_desktop_mode, this);
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        // Use overridden state, if not present, then use default state
        final int overrideInt = Settings.Global.getInt(mContext.getContentResolver(),
                DEVELOPMENT_OVERRIDE_DESKTOP_EXPERIENCE_FEATURES, OVERRIDE_UNSET.getSetting());
        final DesktopModeFlags.ToggleOverride toggleOverride = fromSetting(overrideInt,
                OVERRIDE_UNSET);
        final boolean shouldDevOptionBeEnabled = switch (toggleOverride) {
            case OVERRIDE_OFF, OVERRIDE_UNSET -> false;
            case OVERRIDE_ON -> true;
        };
        ((TwoStatePreference) mPreference).setChecked(shouldDevOptionBeEnabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(mContext.getContentResolver(),
                DEVELOPMENT_OVERRIDE_DESKTOP_EXPERIENCE_FEATURES, OVERRIDE_UNSET.getSetting());
    }

    @Override
    public CharSequence getSummary() {
        if (DesktopModeStatus.isDeviceEligibleForDesktopMode(mContext)
                && !DesktopModeFlags.ENABLE_DESKTOP_WINDOWING_MODE.isTrue()) {
            return mContext.getString(
                    R.string.enable_desktop_experience_features_summary_with_desktop);
        }
        return mContext.getString(
                R.string.enable_desktop_experience_features_summary_without_desktop);
    }
}
