/*
 * Copyright (C) 2018 The Android Open Source Project
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

import com.android.internal.util.ArrayUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@Implements(RestrictedLockUtils.class)
public class ShadowRestrictedLockUtils {
    private static boolean isRestricted;
    private static String[] restrictedPkgs;
    private static boolean adminSupportDetailsIntentLaunched;
    private static int keyguardDisabledFeatures;

    @Resetter
    public static void reset() {
        isRestricted = false;
        restrictedPkgs = null;
        adminSupportDetailsIntentLaunched = false;
        keyguardDisabledFeatures = 0;
    }

    @Implementation
    public static EnforcedAdmin checkIfMeteredDataRestricted(Context context,
            String packageName, int userId) {
        if (isRestricted) {
            return new EnforcedAdmin();
        }
        if (ArrayUtils.contains(restrictedPkgs, packageName)) {
            return new EnforcedAdmin();
        }
        return null;
    }

    @Implementation
    public static void sendShowAdminSupportDetailsIntent(Context context, EnforcedAdmin admin) {
        adminSupportDetailsIntentLaunched = true;
    }

    @Implementation
    public static EnforcedAdmin checkIfKeyguardFeaturesDisabled(Context context,
            int features, final @UserIdInt int userId) {
        return (keyguardDisabledFeatures & features) == 0 ? null : new EnforcedAdmin();
    }

    public static boolean hasAdminSupportDetailsIntentLaunched() {
        return adminSupportDetailsIntentLaunched;
    }

    public static void clearAdminSupportDetailsIntentLaunch() {
        adminSupportDetailsIntentLaunched = false;
    }

    public static void setRestricted(boolean restricted) {
        isRestricted = restricted;
    }

    public static void setRestrictedPkgs(String... pkgs) {
        restrictedPkgs = pkgs;
    }

    public static void setKeyguardDisabledFeatures(int features) {
        keyguardDisabledFeatures = features;
    }
}
