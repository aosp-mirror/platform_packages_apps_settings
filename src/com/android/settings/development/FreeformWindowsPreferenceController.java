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

import static android.content.pm.PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class FreeformWindowsPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin,
        RebootConfirmationDialogHost {

    private static final String ENABLE_FREEFORM_SUPPORT_KEY = "enable_freeform_support";

    @VisibleForTesting
    static final int SETTING_VALUE_OFF = 0;
    @VisibleForTesting
    static final int SETTING_VALUE_ON = 1;

    @Nullable
    private final DevelopmentSettingsDashboardFragment mFragment;

    public FreeformWindowsPreferenceController(
            Context context, @Nullable DevelopmentSettingsDashboardFragment fragment) {
        super(context);
        mFragment = fragment;
    }

    @Override
    public boolean isAvailable() {
        // When devices have the system feature FEATURE_FREEFORM_WINDOW_MANAGEMENT, freeform
        // mode is enabled automatically, and this toggle is not needed.
        return !mContext.getPackageManager().hasSystemFeature(FEATURE_FREEFORM_WINDOW_MANAGEMENT);
    }

    @Override
    public String getPreferenceKey() {
        return ENABLE_FREEFORM_SUPPORT_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_ENABLE_FREEFORM_WINDOWS_SUPPORT,
                isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        if (isEnabled) {
            RebootConfirmationDialogFragment.show(
                    mFragment, R.string.reboot_dialog_enable_freeform_support, this);
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_ENABLE_FREEFORM_WINDOWS_SUPPORT, SETTING_VALUE_OFF);
        ((TwoStatePreference) mPreference).setChecked(mode != SETTING_VALUE_OFF);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_ENABLE_FREEFORM_WINDOWS_SUPPORT, SETTING_VALUE_OFF);
        ((TwoStatePreference) mPreference).setChecked(false);
    }
}
