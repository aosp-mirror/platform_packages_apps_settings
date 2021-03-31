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

package com.android.settings.testutils.shadow;

import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.notification.AudioHelper;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@Implements(AudioHelper.class)
public class ShadowAudioHelper {

    private static boolean sIsSingleVolume = true;
    private static int sManagedProfileId = UserHandle.USER_CURRENT;

    @Resetter
    public static void reset() {
        sIsSingleVolume = true;
        sManagedProfileId = UserHandle.USER_CURRENT;
    }

    public static void setIsSingleVolume(boolean isSingleVolume) {
        sIsSingleVolume = isSingleVolume;
    }

    public static void setManagedProfileId(int managedProfileId) {
        sManagedProfileId = managedProfileId;
    }

    @Implementation
    protected boolean isSingleVolume() {
        return sIsSingleVolume;
    }

    @Implementation
    protected int getManagedProfileId(UserManager um) {
        return sManagedProfileId;
    }
}
