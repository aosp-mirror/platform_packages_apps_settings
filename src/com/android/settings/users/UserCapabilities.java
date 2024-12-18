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

package com.android.settings.users;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

public class UserCapabilities {
    boolean mEnabled = true;
    boolean mCanAddUser = true;
    boolean mCanAddRestrictedProfile;
    boolean mIsMain;
    boolean mIsAdmin;
    boolean mIsGuest;
    boolean mIsEphemeral;
    boolean mUserSwitcherEnabled;
    boolean mCanAddGuest;
    boolean mDisallowAddUser;
    boolean mDisallowAddUserSetByAdmin;
    boolean mDisallowSwitchUser;
    RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;

    private UserCapabilities() {
    }

    public static UserCapabilities create(Context context) {
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        UserCapabilities caps = new UserCapabilities();

        if (!UserManager.supportsMultipleUsers() || Utils.isMonkeyRunning()) {
            caps.mEnabled = false;
            return caps;
        }

        final UserInfo myUserInfo = userManager.getUserInfo(UserHandle.myUserId());
        caps.mIsGuest = myUserInfo.isGuest();
        caps.mIsAdmin = myUserInfo.isAdmin();
        caps.mIsMain = myUserInfo.isMain();
        caps.mIsEphemeral = myUserInfo.isEphemeral();
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(
                Context.DEVICE_POLICY_SERVICE);

        boolean offerRestricted =
                context.getResources().getBoolean(R.bool.config_offer_restricted_profiles);
        caps.mCanAddRestrictedProfile =
                offerRestricted && !dpm.isDeviceManaged() && userManager.isUserTypeEnabled(
                        UserManager.USER_TYPE_FULL_RESTRICTED);

        caps.updateAddUserCapabilities(context);
        return caps;
    }

    public void updateAddUserCapabilities(Context context) {
        final UserManager userManager =
                (UserManager) context.getSystemService(Context.USER_SERVICE);
        final UserInfo myUserInfo = userManager.getUserInfo(UserHandle.myUserId());
        mIsAdmin = myUserInfo.isAdmin();

        mEnforcedAdmin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(context,
                UserManager.DISALLOW_ADD_USER, UserHandle.myUserId());
        final boolean hasBaseUserRestriction = RestrictedLockUtilsInternal.hasBaseUserRestriction(
                context, UserManager.DISALLOW_ADD_USER, UserHandle.myUserId());
        mDisallowAddUserSetByAdmin = mEnforcedAdmin != null && !hasBaseUserRestriction;
        mDisallowAddUser = (mEnforcedAdmin != null || hasBaseUserRestriction);
        mUserSwitcherEnabled = userManager.isUserSwitcherEnabled();
        mCanAddUser = true;
        if (!mIsAdmin
                || UserManager.getMaxSupportedUsers() < 2
                || !UserManager.supportsMultipleUsers()
                || mDisallowAddUser
                || (!userManager.isUserTypeEnabled(UserManager.USER_TYPE_FULL_SECONDARY)
                    && !mCanAddRestrictedProfile)) {
            mCanAddUser = false;
        }

        final boolean canAddUsersWhenLocked = mIsAdmin || Settings.Global.getInt(
                context.getContentResolver(), Settings.Global.ADD_USERS_WHEN_LOCKED, 0) == 1;
        mCanAddGuest = !mIsGuest && !mDisallowAddUser && canAddUsersWhenLocked
                && userManager.isUserTypeEnabled(UserManager.USER_TYPE_FULL_GUEST);

        mDisallowSwitchUser = userManager.hasUserRestriction(UserManager.DISALLOW_USER_SWITCH);
    }

    public boolean isAdmin() {
        return mIsAdmin;
    }

    public boolean disallowAddUser() {
        return mDisallowAddUser;
    }

    public boolean disallowAddUserSetByAdmin() {
        return mDisallowAddUserSetByAdmin;
    }

    public RestrictedLockUtils.EnforcedAdmin getEnforcedAdmin() {
        return mEnforcedAdmin;
    }


    @Override
    public String toString() {
        return "UserCapabilities{" +
                "mEnabled=" + mEnabled +
                ", mCanAddUser=" + mCanAddUser +
                ", mCanAddRestrictedProfile=" + mCanAddRestrictedProfile +
                ", mIsAdmin=" + mIsAdmin +
                ", mIsGuest=" + mIsGuest +
                ", mCanAddGuest=" + mCanAddGuest +
                ", mDisallowAddUser=" + mDisallowAddUser +
                ", mEnforcedAdmin=" + mEnforcedAdmin +
                ", mDisallowSwitchUser=" + mDisallowSwitchUser +
                ", mDisallowAddUserSetByAdmin=" + mDisallowAddUserSetByAdmin +
                ", mUserSwitcherEnabled=" + mUserSwitcherEnabled +
                '}';
    }
}
