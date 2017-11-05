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

package com.android.settings.security;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public abstract class RestrictedEncryptionPreferenceController extends
        AbstractPreferenceController implements PreferenceControllerMixin {

    protected final UserManager mUserManager;

    private final UserHandle mUserHandle;
    private final String mUserRestriction;

    public RestrictedEncryptionPreferenceController(Context context, String userRestriction) {
        super(context);
        mUserHandle = UserHandle.of(UserHandle.myUserId());
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mUserRestriction = userRestriction;
    }

    @Override
    public boolean isAvailable() {
        return !mUserManager.hasBaseUserRestriction(mUserRestriction, mUserHandle);
    }
}
