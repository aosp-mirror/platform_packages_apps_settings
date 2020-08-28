/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.accounts;

import static android.os.UserManager.DISALLOW_REMOVE_MANAGED_PROFILE;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.annotation.UserIdInt;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.AccessiblePreferenceCategory;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;

import java.util.ArrayList;

public class AccountRestrictionHelper {

    private final Context mContext;

    public AccountRestrictionHelper(Context context) {
        mContext = context;
    }

    /**
     * Configure the UI of the preference by checking user restriction.
     * @param preference The preference we are configuring.
     * @param userRestriction The user restriction related to the preference.
     * @param userId The user that we retrieve user restriction of.
     */
    public void enforceRestrictionOnPreference(RestrictedPreference preference,
        String userRestriction, @UserIdInt int userId) {
        if (preference == null) {
            return;
        }
        if (hasBaseUserRestriction(userRestriction, userId)) {
            if (userRestriction.equals(DISALLOW_REMOVE_MANAGED_PROFILE)
                    && isOrganizationOwnedDevice()) {
                preference.setDisabledByAdmin(getEnforcedAdmin(userRestriction, userId));
            } else {
                preference.setEnabled(false);
            }
        } else {
            preference.checkRestrictionAndSetDisabled(userRestriction, userId);
        }
    }

    public boolean hasBaseUserRestriction(String userRestriction, @UserIdInt int userId) {
        return RestrictedLockUtilsInternal.hasBaseUserRestriction(mContext, userRestriction,
                userId);
    }

    private boolean isOrganizationOwnedDevice() {
        final DevicePolicyManager dpm = (DevicePolicyManager) mContext.getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        if (dpm == null) {
            return false;
        }
        return dpm.isOrganizationOwnedDeviceWithManagedProfile();
    }

    private EnforcedAdmin getEnforcedAdmin(String userRestriction, int userId) {
        final DevicePolicyManager dpm = (DevicePolicyManager) mContext.getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        if (dpm == null) {
            return null;
        }
        final int managedUsedId = getManagedUserId(userId);
        ComponentName adminComponent = dpm.getProfileOwnerAsUser(managedUsedId);
        if (adminComponent != null) {
            return new EnforcedAdmin(adminComponent, userRestriction,
                    UserHandle.of(managedUsedId));
        }
        return null;
    }

    private int getManagedUserId(int userId) {
        final UserManager um = UserManager.get(mContext);
        for (UserInfo ui : um.getProfiles(userId)) {
            if (ui.id == userId || !ui.isManagedProfile()) {
                continue;
            }
            return ui.id;
        }
        return -1;
    }

    public AccessiblePreferenceCategory createAccessiblePreferenceCategory(Context context) {
        return new AccessiblePreferenceCategory(context);
    }

    /**
     * Checks if the account should be shown based on the required authorities for the account type
     * @param authorities given authority that is passed as activity extra
     * @param auths list of authorities for particular account type
     * @return true if the activity has the required authority to show the account
     */
    public static boolean showAccount(String[] authorities, ArrayList<String> auths) {
        boolean showAccount = true;
        if (authorities != null && auths != null) {
            showAccount = false;
            for (String requestedAuthority : authorities) {
                if (auths.contains(requestedAuthority)) {
                    return true;
                }
            }
        }
        return showAccount;
    }
}
