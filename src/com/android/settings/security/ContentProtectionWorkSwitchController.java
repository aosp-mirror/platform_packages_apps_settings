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

import static android.view.contentprotection.flags.Flags.manageDevicePolicyEnabled;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;

/** Preference controller for content protection work profile switch bar. */
public class ContentProtectionWorkSwitchController extends TogglePreferenceController {

    @Nullable private UserHandle mManagedProfile;

    @DevicePolicyManager.ContentProtectionPolicy
    private int mContentProtectionPolicy = DevicePolicyManager.CONTENT_PROTECTION_DISABLED;

    public ContentProtectionWorkSwitchController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);

        if (manageDevicePolicyEnabled()) {
            mManagedProfile = getManagedProfile();
            if (mManagedProfile != null) {
                mContentProtectionPolicy = getContentProtectionPolicy(mManagedProfile);
            }
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (!manageDevicePolicyEnabled()) {
            return getManagedProfile() != null ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
        }
        if (mManagedProfile == null) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        if (mContentProtectionPolicy
                == DevicePolicyManager.CONTENT_PROTECTION_NOT_CONTROLLED_BY_POLICY) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        if (!manageDevicePolicyEnabled()) {
            return false;
        }
        return mContentProtectionPolicy == DevicePolicyManager.CONTENT_PROTECTION_ENABLED;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        // Controlled by the admin API
        return false;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        UserHandle managedProfile =
                manageDevicePolicyEnabled() ? mManagedProfile : getManagedProfile();
        if (managedProfile != null) {
            RestrictedSwitchPreference switchPreference = screen.findPreference(getPreferenceKey());
            if (switchPreference != null) {
                switchPreference.setDisabledByAdmin(getEnforcedAdmin(managedProfile));
            }
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_security;
    }

    @VisibleForTesting
    @Nullable
    protected UserHandle getManagedProfile() {
        return ContentProtectionPreferenceUtils.getManagedProfile(mContext);
    }

    @VisibleForTesting
    @Nullable
    protected RestrictedLockUtils.EnforcedAdmin getEnforcedAdmin(@NonNull UserHandle userHandle) {
        return RestrictedLockUtils.getProfileOrDeviceOwner(mContext, userHandle);
    }

    @VisibleForTesting
    @DevicePolicyManager.ContentProtectionPolicy
    protected int getContentProtectionPolicy(@Nullable UserHandle userHandle) {
        return ContentProtectionPreferenceUtils.getContentProtectionPolicy(mContext, userHandle);
    }
}
