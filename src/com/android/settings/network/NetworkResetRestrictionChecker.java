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

package com.android.settings.network;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;

import com.android.settingslib.RestrictedLockUtilsInternal;

public class NetworkResetRestrictionChecker {

    private final Context mContext;
    private final UserManager mUserManager;

    public NetworkResetRestrictionChecker(Context context) {
        mContext = context;
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    boolean hasUserBaseRestriction() {
        return RestrictedLockUtilsInternal.hasBaseUserRestriction(mContext,
                UserManager.DISALLOW_NETWORK_RESET, UserHandle.myUserId());
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    boolean isRestrictionEnforcedByAdmin() {
        return RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_NETWORK_RESET, UserHandle.myUserId()) != null;
    }

    boolean hasUserRestriction() {
        return !mUserManager.isAdminUser()
                || hasUserBaseRestriction();
    }

    boolean hasRestriction() {
        return hasUserRestriction()
                || isRestrictionEnforcedByAdmin();
    }
}
