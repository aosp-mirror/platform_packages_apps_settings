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

import android.annotation.NonNull;
import android.annotation.UserIdInt;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;

import com.android.internal.util.ArrayUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@Implements(RestrictedLockUtilsInternal.class)
public class ShadowRestrictedLockUtilsInternal {

    private static boolean sIsRestricted;
    private static boolean sHasSystemFeature;
    private static boolean sMaximumTimeToLockIsSet;
    private static boolean sMteOverridden;
    private static String[] sRestrictedPkgs;
    private static DevicePolicyManager sDevicePolicyManager;
    private static String[] sDisabledTypes;
    private static int sKeyguardDisabledFeatures;
    private static String[] sEcmRestrictedPkgs;
    private static boolean sAccessibilityServiceRestricted;

    @Resetter
    public static void reset() {
        sIsRestricted = false;
        sRestrictedPkgs = null;
        sKeyguardDisabledFeatures = 0;
        sDisabledTypes = new String[0];
        sMaximumTimeToLockIsSet = false;
        sMteOverridden = false;
        sEcmRestrictedPkgs = new String[0];
        sAccessibilityServiceRestricted = false;
    }

    @Implementation
    protected static EnforcedAdmin checkIfMeteredDataUsageUserControlDisabled(Context context,
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
    protected static EnforcedAdmin checkIfAccountManagementDisabled(Context context,
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
    protected static EnforcedAdmin checkIfKeyguardFeaturesDisabled(Context context,
            int features, final @UserIdInt int userId) {
        return (sKeyguardDisabledFeatures & features) == 0 ? null : new EnforcedAdmin();
    }

    @Implementation
    protected static boolean hasBaseUserRestriction(Context context,
            String userRestriction, int userId) {
        return sIsRestricted;
    }

    @Implementation
    protected static EnforcedAdmin checkIfRestrictionEnforced(Context context,
            String userRestriction, int userId) {
        return sIsRestricted ? new EnforcedAdmin() : null;
    }

    @Implementation
    protected static EnforcedAdmin checkIfMaximumTimeToLockIsSet(Context context) {
        return sMaximumTimeToLockIsSet ? new EnforcedAdmin() : null;
    }

    @Implementation
    public static EnforcedAdmin checkIfMteIsDisabled(Context context) {
        return sMteOverridden ? new EnforcedAdmin() : null;
    }

    public static EnforcedAdmin checkIfAccessibilityServiceDisallowed(Context context,
            String packageName, int userId) {
        return sAccessibilityServiceRestricted ? new EnforcedAdmin() : null;
    }

    @Implementation
    public static Intent checkIfRequiresEnhancedConfirmation(@NonNull Context context,
            @NonNull String settingIdentifier, @NonNull String packageName) {
        if (ArrayUtils.contains(sEcmRestrictedPkgs, packageName)) {
            return new Intent();
        }

        return null;
    }

    @Implementation
    public static boolean isEnhancedConfirmationRestricted(@NonNull Context context,
            @NonNull String settingIdentifier, @NonNull String packageName) {
        return false;
    }

    public static void setRestricted(boolean restricted) {
        sIsRestricted = restricted;
    }

    public static void setEcmRestrictedPkgs(String... pkgs) {
        sEcmRestrictedPkgs = pkgs;
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

    public static void setMteIsDisabled(boolean isSet) {
        sMteOverridden = isSet;
    }
}
