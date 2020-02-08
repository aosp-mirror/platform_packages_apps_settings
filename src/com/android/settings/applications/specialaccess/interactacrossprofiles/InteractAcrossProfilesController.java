/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.interactacrossprofiles;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.core.BasePreferenceController;

import java.util.List;

/**
 * Controller to decide when to show the "Connected work and personal apps" option in the
 * Special access screen.
 */
public class InteractAcrossProfilesController extends BasePreferenceController {

    private final Context mContext;
    private final UserManager mUserManager;

    public InteractAcrossProfilesController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mContext = context;
        mUserManager = mContext.getSystemService(UserManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        final List<UserInfo> profiles = mUserManager.getProfiles(UserHandle.myUserId());
        for (final UserInfo userInfo : profiles) {
            if (userInfo.isManagedProfile()) {
                return AVAILABLE;
            }
        }
        return DISABLED_FOR_USER;
    }
}
