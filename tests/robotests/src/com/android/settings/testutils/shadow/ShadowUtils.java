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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArraySet;

import com.android.settings.Utils;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.HashMap;
import java.util.Map;

@Implements(Utils.class)
public class ShadowUtils {

    private static FingerprintManager sFingerprintManager = null;
    private static boolean sIsUserAMonkey;
    private static boolean sIsDemoUser;
    private static ComponentName sDeviceOwnerComponentName;
    private static Map<String, String> sAppNameMap;
    private static boolean sIsSystemAlertWindowEnabled;
    private static boolean sIsVoiceCapable;
    private static ArraySet<String> sResultLinks = new ArraySet<>();
    private static boolean sIsBatteryPresent;

    @Implementation
    protected static int enforceSameOwner(Context context, int userId) {
        return userId;
    }

    @Implementation
    protected static FingerprintManager getFingerprintManagerOrNull(Context context) {
        return sFingerprintManager;
    }

    public static void setFingerprintManager(FingerprintManager fingerprintManager) {
        sFingerprintManager = fingerprintManager;
    }

    public static void reset() {
        sFingerprintManager = null;
        sIsUserAMonkey = false;
        sIsDemoUser = false;
        sIsVoiceCapable = false;
        sResultLinks = new ArraySet<>();
        sIsBatteryPresent = true;
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
    protected static boolean isMonkeyRunning() {
        return sIsUserAMonkey;
    }

    public static void setDeviceOwnerComponent(ComponentName componentName) {
        sDeviceOwnerComponentName = componentName;
    }

    @Implementation
    protected static ComponentName getDeviceOwnerComponent(Context context) {
        return sDeviceOwnerComponentName;
    }

    @Implementation
    protected static int getManagedProfileId(UserManager um, int parentUserId) {
        return UserHandle.USER_NULL;
    }

    @Implementation
    protected static CharSequence getApplicationLabel(Context context, String packageName) {
        if (sAppNameMap != null) {
            return sAppNameMap.get(packageName);
        }
        return null;
    }

    @Implementation
    protected static boolean isPackageEnabled(Context context, String packageName) {
        return true;
    }

    public static void setApplicationLabel(String packageName, String appLabel) {
        if (sAppNameMap == null) {
            sAppNameMap = new HashMap<>();
        }
        sAppNameMap.put(packageName, appLabel);
    }

    @Implementation
    protected static boolean isSystemAlertWindowEnabled(Context context) {
        return sIsSystemAlertWindowEnabled;
    }

    public static void setIsSystemAlertWindowEnabled(boolean enabled) {
        sIsSystemAlertWindowEnabled = enabled;
    }

    @Implementation
    protected static boolean isVoiceCapable(Context context) {
        return sIsVoiceCapable;
    }

    public static void setIsVoiceCapable(boolean isVoiceCapable) {
        sIsVoiceCapable = isVoiceCapable;
    }

    @Implementation
    protected static ArraySet<String> getHandledDomains(PackageManager pm, String packageName) {
        return sResultLinks;
    }

    @Implementation
    protected static Drawable getBadgedIcon(Context context, ApplicationInfo appInfo) {
        return new ColorDrawable(0);
    }

    public static void setHandledDomains(ArraySet<String> links) {
        sResultLinks = links;
    }

    @Implementation
    protected static boolean isBatteryPresent(Context context) {
        return sIsBatteryPresent;
    }

    public static void setIsBatteryPresent(boolean isBatteryPresent) {
        sIsBatteryPresent = isBatteryPresent;
    }
}
