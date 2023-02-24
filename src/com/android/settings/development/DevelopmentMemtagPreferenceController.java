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

package com.android.settings.development;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.content.Context;
import android.os.SystemProperties;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.security.MemtagHelper;
import com.android.settings.security.MemtagRebootDialog;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

public class DevelopmentMemtagPreferenceController extends TogglePreferenceController {
    private Preference mPreference;
    private DashboardFragment mFragment;

    public DevelopmentMemtagPreferenceController(Context context, String key) {
        super(context, key);
    }

    public void setFragment(DashboardFragment fragment) {
        mFragment = fragment;
    }

    @Override
    public int getAvailabilityStatus() {
        return DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(mContext)
                        && SystemProperties.getBoolean("ro.arm64.memtag.bootctl_supported", false)
                ? BasePreferenceController.AVAILABLE
                : BasePreferenceController.UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return MemtagHelper.isChecked();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        MemtagHelper.setChecked(isChecked);
        if (mPreference != null) {
            refreshSummary(mPreference);
        }
        if (isChecked != MemtagHelper.isOn()) {
            int msg =
                    isChecked
                            ? R.string.development_memtag_reboot_message_on
                            : R.string.development_memtag_reboot_message_off;
            MemtagRebootDialog.show(mContext, mFragment, msg);
        }
        mFragment.forceUpdatePreferences();
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        refreshSummary(mPreference);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        EnforcedAdmin admin = RestrictedLockUtilsInternal.checkIfMteIsDisabled(mContext);
        if (admin != null) {
            // Make sure this is disabled even if the user directly goes to this
            // page via the android.settings.ADVANCED_MEMORY_PROTECTION_SETTINGS intent.
            ((RestrictedSwitchPreference) preference).setDisabledByAdmin(admin);
        }
        refreshSummary(preference);
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getResources().getString(MemtagHelper.getSummary());
    }
}
