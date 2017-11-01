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

package com.android.settings.notification;

import android.annotation.UserIdInt;
import android.content.Context;
import android.media.AudioSystem;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.settings.Utils;

/**
 * Helper class to wrap API for testing
 */
public class AudioHelper {

    private Context mContext;

    public AudioHelper(Context context) {
        mContext = context;
    }

    public boolean isSingleVolume() {
        return AudioSystem.isSingleVolume(mContext);
    }

    public int getManagedProfileId(UserManager um) {
        return Utils.getManagedProfileId(um, UserHandle.myUserId());
    }

    public boolean isUserUnlocked(UserManager um, @UserIdInt int userId) {
        return um.isUserUnlocked(userId);
    }

    public Context createPackageContextAsUser(@UserIdInt int profileId) {
        return Utils.createPackageContextAsUser(mContext, profileId);
    }
}
