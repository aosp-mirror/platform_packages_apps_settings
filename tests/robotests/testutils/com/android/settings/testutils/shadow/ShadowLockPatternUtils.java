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

import android.app.admin.DevicePolicyManager;
import android.app.admin.PasswordMetrics;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Implements(LockPatternUtils.class)
public class ShadowLockPatternUtils {

    private static boolean sDeviceEncryptionEnabled;
    private static Map<Integer, Integer> sUserToActivePasswordQualityMap = new HashMap<>();
    private static Map<Integer, Integer> sUserToComplexityMap = new HashMap<>();
    private static Map<Integer, Integer> sUserToProfileComplexityMap = new HashMap<>();
    private static Map<Integer, PasswordMetrics> sUserToMetricsMap = new HashMap<>();
    private static Map<Integer, PasswordMetrics> sUserToProfileMetricsMap = new HashMap<>();
    private static Map<Integer, Boolean> sUserToIsSecureMap = new HashMap<>();
    private static Map<Integer, Boolean> sUserToVisiblePatternEnabledMap = new HashMap<>();
    private static Map<Integer, Boolean> sUserToBiometricAllowedMap = new HashMap<>();
    private static Map<Integer, Boolean> sUserToLockPatternEnabledMap = new HashMap<>();
    private static Map<Integer, Integer> sKeyguardStoredPasswordQualityMap = new HashMap<>();

    private static boolean sIsUserOwnsFrpCredential;

    @Resetter
    public static void reset() {
        sUserToActivePasswordQualityMap.clear();
        sUserToComplexityMap.clear();
        sUserToProfileComplexityMap.clear();
        sUserToMetricsMap.clear();
        sUserToProfileMetricsMap.clear();
        sUserToIsSecureMap.clear();
        sUserToVisiblePatternEnabledMap.clear();
        sUserToBiometricAllowedMap.clear();
        sUserToLockPatternEnabledMap.clear();
        sDeviceEncryptionEnabled = false;
        sIsUserOwnsFrpCredential = false;
        sKeyguardStoredPasswordQualityMap.clear();
    }

    @Implementation
    protected boolean hasSecureLockScreen() {
        return true;
    }

    @Implementation
    protected boolean isSecure(int userId) {
        Boolean isSecure = sUserToIsSecureMap.get(userId);
        if (isSecure == null) {
            return true;
        }
        return isSecure;
    }

    public static void setIsSecure(int userId, boolean isSecure) {
        sUserToIsSecureMap.put(userId, isSecure);
    }

    @Implementation
    protected int getActivePasswordQuality(int userId) {
        final Integer activePasswordQuality = sUserToActivePasswordQualityMap.get(userId);
        if (activePasswordQuality == null) {
            return DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
        }
        return activePasswordQuality;
    }

    @Implementation
    protected int getKeyguardStoredPasswordQuality(int userHandle) {
        return sKeyguardStoredPasswordQualityMap.getOrDefault(userHandle, /* defaultValue= */ 1);
    }

    @Implementation
    protected static boolean isDeviceEncryptionEnabled() {
        return sDeviceEncryptionEnabled;
    }

    @Implementation
    protected List<ComponentName> getEnabledTrustAgents(int userId) {
        return null;
    }

    public static void setDeviceEncryptionEnabled(boolean deviceEncryptionEnabled) {
        sDeviceEncryptionEnabled = deviceEncryptionEnabled;
    }

    @Implementation
    protected byte[] getPasswordHistoryHashFactor(
            LockscreenCredential currentPassword, int userId) {
        return null;
    }

    @Implementation
    protected boolean checkPasswordHistory(byte[] passwordToCheck, byte[] hashFactor, int userId) {
        return false;
    }

    @Implementation
    public @DevicePolicyManager.PasswordComplexity int getRequestedPasswordComplexity(int userId) {
        return getRequestedPasswordComplexity(userId, false);
    }

    @Implementation
    @DevicePolicyManager.PasswordComplexity
    public int getRequestedPasswordComplexity(int userId, boolean deviceWideOnly) {
        int complexity = sUserToComplexityMap.getOrDefault(userId,
                DevicePolicyManager.PASSWORD_COMPLEXITY_NONE);
        if (!deviceWideOnly) {
            complexity = Math.max(complexity, sUserToProfileComplexityMap.getOrDefault(userId,
                    DevicePolicyManager.PASSWORD_COMPLEXITY_NONE));
        }
        return complexity;
    }

    @Implementation
    public static boolean userOwnsFrpCredential(Context context, UserInfo info) {
        return sIsUserOwnsFrpCredential;
    }

    public static void setUserOwnsFrpCredential(boolean isUserOwnsFrpCredential) {
        sIsUserOwnsFrpCredential = isUserOwnsFrpCredential;
    }

    @Implementation
    public boolean isVisiblePatternEnabled(int userId) {
        return sUserToVisiblePatternEnabledMap.getOrDefault(userId, false);
    }

    public static void setIsVisiblePatternEnabled(int userId, boolean isVisiblePatternEnabled) {
        sUserToVisiblePatternEnabledMap.put(userId, isVisiblePatternEnabled);
    }

    @Implementation
    public boolean isBiometricAllowedForUser(int userId) {
        return sUserToBiometricAllowedMap.getOrDefault(userId, false);
    }

    public static void setIsBiometricAllowedForUser(int userId, boolean isBiometricAllowed) {
        sUserToBiometricAllowedMap.put(userId, isBiometricAllowed);
    }

    @Implementation
    public boolean isLockPatternEnabled(int userId) {
        return sUserToLockPatternEnabledMap.getOrDefault(userId, false);
    }

    public static void setIsLockPatternEnabled(int userId, boolean isLockPatternEnabled) {
        sUserToLockPatternEnabledMap.put(userId, isLockPatternEnabled);
    }

    @Implementation
    public boolean setLockCredential(
            @NonNull LockscreenCredential newCredential,
            @NonNull LockscreenCredential savedCredential, int userHandle) {
        setIsSecure(userHandle, true);
        return true;
    }

    @Implementation
    public boolean checkCredential(
            @NonNull LockscreenCredential credential, int userId,
            @Nullable LockPatternUtils.CheckCredentialProgressCallback progressCallback)
            throws LockPatternUtils.RequestThrottledException {
        return true;
    }

    public static void setRequiredPasswordComplexity(int userHandle, int complexity) {
        sUserToComplexityMap.put(userHandle, complexity);
    }

    public static void setRequiredPasswordComplexity(int complexity) {
        sUserToComplexityMap.put(UserHandle.myUserId(), complexity);
    }

    public static void setRequiredProfilePasswordComplexity(int complexity) {
        sUserToProfileComplexityMap.put(UserHandle.myUserId(), complexity);
    }

    @Implementation
    public PasswordMetrics getRequestedPasswordMetrics(int userId, boolean deviceWideOnly) {
        PasswordMetrics metrics = sUserToMetricsMap.getOrDefault(userId,
                new PasswordMetrics(LockPatternUtils.CREDENTIAL_TYPE_NONE));
        if (!deviceWideOnly) {
            metrics.maxWith(sUserToProfileMetricsMap.getOrDefault(userId,
                    new PasswordMetrics(LockPatternUtils.CREDENTIAL_TYPE_NONE)));
        }
        return metrics;
    }

    public static void setRequestedPasswordMetrics(PasswordMetrics metrics) {
        sUserToMetricsMap.put(UserHandle.myUserId(), metrics);
    }

    public static void setRequestedProfilePasswordMetrics(PasswordMetrics metrics) {
        sUserToProfileMetricsMap.put(UserHandle.myUserId(), metrics);
    }

    public static void setActivePasswordQuality(int quality) {
        sUserToActivePasswordQualityMap.put(UserHandle.myUserId(), quality);
    }

    @Implementation
    public boolean isLockScreenDisabled(int userId) {
        return false;
    }

    @Implementation
    public boolean isSeparateProfileChallengeEnabled(int userHandle) {
        return false;
    }

    public static void setKeyguardStoredPasswordQuality(int quality) {
        sKeyguardStoredPasswordQualityMap.put(UserHandle.myUserId(), quality);
    }
}
