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

package com.android.settings.testutils.shadow;

import android.annotation.UserIdInt;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;
import android.util.SparseArray;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.internal.ShadowExtractor;

import java.util.Collections;
import java.util.List;

/**
 * This class provides the API 24 implementation of UserManager.get(Context).
 */
@Implements(UserManager.class)
public class ShadowUserManager extends org.robolectric.shadows.ShadowUserManager {

    private SparseArray<UserInfo> mUserInfos = new SparseArray<>();

    public void setUserInfo(int userHandle, UserInfo userInfo) {
        mUserInfos.put(userHandle, userInfo);
    }

    @Implementation
    public UserInfo getUserInfo(int userHandle) {
        return mUserInfos.get(userHandle);
    }

    @Implementation
    public List<UserInfo> getProfiles(@UserIdInt int userHandle) {
        return Collections.emptyList();
    }

    @Implementation
    public int getCredentialOwnerProfile(@UserIdInt int userHandle) {
        return userHandle;
    }

    @Implementation
    public static UserManager get(Context context) {
        return (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    public static ShadowUserManager getShadow() {
        return (ShadowUserManager) ShadowExtractor.extract(
                RuntimeEnvironment.application.getSystemService(UserManager.class));
    }
}
