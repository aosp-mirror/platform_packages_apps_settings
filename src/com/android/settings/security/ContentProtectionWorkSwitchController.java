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
package com.android.settings.security;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;

/** Preference controller for content protection work profile switch bar. */
public class ContentProtectionWorkSwitchController extends TogglePreferenceController {

    public ContentProtectionWorkSwitchController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return getManagedProfile() != null ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    // The switch is always set to unchecked until Android V by design
    @Override
    public boolean isChecked() {
        return false;
    }

    // The switch is disabled until Android V by design
    @Override
    public boolean setChecked(boolean isChecked) {
        return false;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        RestrictedSwitchPreference switchPreference = screen.findPreference(getPreferenceKey());
        UserHandle managedProfile = getManagedProfile();
        if (managedProfile != null) {
            switchPreference.setDisabledByAdmin(getEnforcedAdmin(managedProfile));
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_security;
    }

    @VisibleForTesting
    @Nullable
    protected UserHandle getManagedProfile() {
        return Utils.getManagedProfile(mContext.getSystemService(UserManager.class));
    }

    @VisibleForTesting
    @Nullable
    protected RestrictedLockUtils.EnforcedAdmin getEnforcedAdmin(
            @NonNull UserHandle managedProfile) {
        return RestrictedLockUtils.getProfileOrDeviceOwner(mContext, managedProfile);
    }
}
