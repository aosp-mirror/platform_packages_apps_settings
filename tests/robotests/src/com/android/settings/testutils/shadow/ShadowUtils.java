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

import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.Utils;
import com.android.settings.password.IFingerprintManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(Utils.class)
public class ShadowUtils {

    private static IFingerprintManager sFingerprintManager = null;
    private static boolean sIsUserAMonkey;
    private static boolean sIsDemoUser;
    private static ComponentName sDeviceOwnerComponentName;

    @Implementation
    public static int enforceSameOwner(Context context, int userId) {
        return userId;
    }

    @Implementation
    public static IFingerprintManager getFingerprintManagerWrapperOrNull(Context context) {
        return sFingerprintManager;
    }

    public static void setFingerprintManager(IFingerprintManager fingerprintManager) {
        sFingerprintManager = fingerprintManager;
    }

    public static void reset() {
        sFingerprintManager = null;
        sIsUserAMonkey = false;
        sIsDemoUser = false;
    }

    @Implementation
    public static boolean isWifiOnly(Context context) {
        return true;
    }

    public static void setIsDemoUser(boolean isDemoUser) {
        sIsDemoUser = isDemoUser;
    }

    @Implementation
    public static boolean isDemoUser(Context context) {
        return sIsDemoUser;
    }

    public static void setIsUserAMonkey(boolean isUserAMonkey) {
        sIsUserAMonkey = isUserAMonkey;
    }

    /**
     * Returns true if Monkey is running.
     */
    @Implementation
    public static boolean isMonkeyRunning() {
        return sIsUserAMonkey;
    }

    public static void setDeviceOwnerComponent(ComponentName componentName) {
        sDeviceOwnerComponentName = componentName;
    }

    @Implementation
    public static ComponentName getDeviceOwnerComponent(Context context) {
        return sDeviceOwnerComponentName;
    }

    @Implementation
    public static int getManagedProfileId(UserManager um, int parentUserId) {
        return UserHandle.USER_NULL;
    }
}
