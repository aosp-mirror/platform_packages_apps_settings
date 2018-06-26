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
import android.app.admin.DevicePolicyManager;
import android.content.Context;

import com.android.internal.util.ArrayUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@Implements(RestrictedLockUtils.class)
public class ShadowRestrictedLockUtils {

    private static boolean sIsRestricted;
    private static boolean sAdminSupportDetailsIntentLaunched;
    private static boolean sHasSystemFeature;
    private static boolean sMaximumTimeToLockIsSet;
    private static String[] sRestrictedPkgs;
    private static DevicePolicyManager sDevicePolicyManager;
    private static String[] sDisabledTypes;
    private static int sKeyguardDisabledFeatures;

    @Resetter
    public static void reset() {
        sIsRestricted = false;
        sRestrictedPkgs = null;
        sAdminSupportDetailsIntentLaunched = false;
        sKeyguardDisabledFeatures = 0;
        sDisabledTypes = new String[0];
        sMaximumTimeToLockIsSet = false;
    }

    @Implementation
    public static EnforcedAdmin checkIfMeteredDataRestricted(Context context,
            String packageName, int userId) {
        if (sIsRestricted) {
            return new EnforcedAdmin();
        }
        if (ArrayUtils.contains(sRestrictedPkgs, packageName)) {
            return new EnforcedAdmin();
        }
        return null;
    }

    @Implementation
    public static void sendShowAdminSupportDetailsIntent(Context context, EnforcedAdmin admin) {
        sAdminSupportDetailsIntentLaunched = true;
    }

    @Implementation
    public static EnforcedAdmin checkIfAccountManagementDisabled(Context context,
            String accountType, int userId) {
        if (accountType == null) {
            return null;
        }
        if (!sHasSystemFeature || sDevicePolicyManager == null) {
            return null;
        }
        boolean isAccountTypeDisabled = false;
        if (ArrayUtils.contains(sDisabledTypes, accountType)) {
            isAccountTypeDisabled = true;
        }
        if (!isAccountTypeDisabled) {
            return null;
        }
        return new EnforcedAdmin();
    }

    @Implementation
    public static EnforcedAdmin checkIfKeyguardFeaturesDisabled(Context context,
            int features, final @UserIdInt int userId) {
        return (sKeyguardDisabledFeatures & features) == 0 ? null : new EnforcedAdmin();
    }

    @Implementation
    public static boolean hasBaseUserRestriction(Context context,
            String userRestriction, int userId) {
        return sIsRestricted;
    }

    @Implementation
    public static EnforcedAdmin checkIfRestrictionEnforced(Context context,
            String userRestriction, int userId) {
        return sIsRestricted ? new EnforcedAdmin() : null;
    }

    @Implementation
    public static EnforcedAdmin checkIfMaximumTimeToLockIsSet(Context context) {
        return sMaximumTimeToLockIsSet ? new EnforcedAdmin() : null;
    }

    public static boolean hasAdminSupportDetailsIntentLaunched() {
        return sAdminSupportDetailsIntentLaunched;
    }

    public static void clearAdminSupportDetailsIntentLaunch() {
        sAdminSupportDetailsIntentLaunched = false;
    }

    public static void setRestricted(boolean restricted) {
        sIsRestricted = restricted;
    }

    public static void setRestrictedPkgs(String... pkgs) {
        sRestrictedPkgs = pkgs;
    }

    public static void setHasSystemFeature(boolean hasSystemFeature) {
        sHasSystemFeature = hasSystemFeature;
    }

    public static void setDevicePolicyManager(DevicePolicyManager dpm) {
        sDevicePolicyManager = dpm;
    }

    public static void setDisabledTypes(String[] disabledTypes) {
        sDisabledTypes = disabledTypes;
    }

    public static void clearDisabledTypes() {
        sDisabledTypes = new String[0];
    }

    public static void setKeyguardDisabledFeatures(int features) {
        sKeyguardDisabledFeatures = features;
    }

    public static void setMaximumTimeToLockIsSet(boolean isSet) {
        sMaximumTimeToLockIsSet = isSet;
    }

}
