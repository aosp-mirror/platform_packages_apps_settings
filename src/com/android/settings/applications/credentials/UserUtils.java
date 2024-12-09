/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.applications.credentials;

import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.Utils;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;

import java.util.List;

/** Handles user related utils. */
public class UserUtils {
    public static final String EXTRA_IS_WORK_PROFILE = "isWorkProfile";
    public static final String EXTRA_IS_PRIVATE_SPACE = "isPrivateSpace";

    /**
     * Returns the managed profile of the current user or {@code null} if none is found or a profile
     * exists but it is disabled.
     */
    @Nullable
    public static UserHandle getManagedProfile(UserManager userManager) {
        final List<UserHandle> userProfiles = userManager.getUserProfiles();
        for (UserHandle profile : userProfiles) {
            final UserInfo userInfo = userManager.getUserInfo(profile.getIdentifier());
            if (userInfo.isManagedProfile()) {
                return profile;
            }
        }
        return null;
    }

    public static int getUser(boolean isWorkProfile, boolean isPrivateSpace, Context context) {
        int profileType = ProfileSelectFragment.ProfileType.PERSONAL;
        if (isWorkProfile) {
            profileType = ProfileSelectFragment.ProfileType.WORK;
        } else if (isPrivateSpace) {
            profileType = ProfileSelectFragment.ProfileType.PRIVATE;
        }
        // Get the user information of the tab we are trying to populate as the tab being populated
        // might be different from the current userId
        UserHandle userHandle = Utils.getProfileOfType(UserManager.get(context), profileType);
        if (userHandle != null) {
            return userHandle.getIdentifier();
        }
        return UserHandle.myUserId();
    }
}
