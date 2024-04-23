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

package com.android.settings.fuelgauge.batteryusage;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

class UserIdsSeries {
    private final UserManager mUserManager;
    private final int mCurrentUserId;
    private final List<Integer> mVisibleUserIds = new ArrayList<>();

    @Nullable private UserInfo mPrivateUser = null;
    @Nullable private UserInfo mManagedProfileUser = null;

    UserIdsSeries(final Context context, final boolean isNonUIRequest) {
        mUserManager = context.getSystemService(UserManager.class);
        mCurrentUserId = context.getUserId();
        List<UserInfo> aliveUsers =
                mUserManager != null ? mUserManager.getAliveUsers() : new ArrayList<>();

        for (UserInfo userInfo : aliveUsers) {
            if (!mUserManager.isSameProfileGroup(mCurrentUserId, userInfo.id)) {
                continue;
            }
            if (userInfo.isManagedProfile()) {
                // Load data for WorkProfile mode
                mManagedProfileUser = userInfo;
                mVisibleUserIds.add(userInfo.id);
            } else if (userInfo.isPrivateProfile()) {
                mPrivateUser = userInfo;
                // Load data for PrivateProfile if it is from UI caller and PS is unlocked.
                if (!isNonUIRequest && !userInfo.isQuietModeEnabled()) {
                    mVisibleUserIds.add(userInfo.id);
                }
            } else if (!userInfo.isQuietModeEnabled()) {
                // Load data for other profiles if it is not in quiet mode
                mVisibleUserIds.add(userInfo.id);
            }
        }
    }

    int getCurrentUserId() {
        return mCurrentUserId;
    }

    List<Integer> getVisibleUserIds() {
        return mVisibleUserIds;
    }

    boolean isCurrentUserLocked() {
        return isUserLocked(mCurrentUserId);
    }

    boolean isUserLocked(int userId) {
        return mUserManager == null || !mUserManager.isUserUnlocked(userId);
    }

    boolean isFromOtherUsers(long userId) {
        return !mVisibleUserIds.contains((int) userId);
    }

    boolean isMainUserProfileOnly() {
        return mUserManager != null
                && mUserManager.isMainUser()
                && mPrivateUser == null
                && mManagedProfileUser == null;
    }
}
