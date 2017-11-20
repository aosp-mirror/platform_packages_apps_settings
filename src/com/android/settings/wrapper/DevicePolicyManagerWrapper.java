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

package com.android.settings.wrapper;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

import java.util.List;

/**
 * This class replicates a subset of the android.app.admin.DevicePolicyManager (DPM). The
 * class exists so that we can use a thin wrapper around the DPM in production code and a mock
 * in tests. We cannot directly mock or shadow the DPM, because some of the methods we rely on are
 * newer than the API version supported by Robolectric.
 */
public class DevicePolicyManagerWrapper {
    private final DevicePolicyManager mDpm;

    public DevicePolicyManagerWrapper(DevicePolicyManager dpm) {
        mDpm = dpm;
    }

    public static @Nullable DevicePolicyManagerWrapper from(Context context) {
        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm == null ? null : new DevicePolicyManagerWrapper(dpm);
    }

    /**
     * Calls {@code DevicePolicyManager.getActiveAdminsAsUser()}.
     *
     * @see DevicePolicyManager#getActiveAdminsAsUser
     */
    public @Nullable List<ComponentName> getActiveAdminsAsUser(int userId) {
        return mDpm.getActiveAdminsAsUser(userId);
    }

    /**
     * Calls {@code DevicePolicyManager.getMaximumFailedPasswordsForWipe()}.
     *
     * @see DevicePolicyManager#getMaximumFailedPasswordsForWipe
     */
    public int getMaximumFailedPasswordsForWipe(@Nullable ComponentName admin, int userHandle) {
        return mDpm.getMaximumFailedPasswordsForWipe(admin, userHandle);
    }

    /**
     * Calls {@code DevicePolicyManager.getDeviceOwnerComponentOnCallingUser()}.
     *
     * @see DevicePolicyManager#getDeviceOwnerComponentOnCallingUser
     */
    public ComponentName getDeviceOwnerComponentOnCallingUser() {
        return mDpm.getDeviceOwnerComponentOnCallingUser();
    }

    /**
     * Calls {@code DevicePolicyManager.getDeviceOwnerComponentOnAnyUser()}.
     *
     * @see DevicePolicyManager#getDeviceOwnerComponentOnAnyUser
     */
    public ComponentName getDeviceOwnerComponentOnAnyUser() {
        return mDpm.getDeviceOwnerComponentOnAnyUser();
    }

    /**
     * Calls {@code DevicePolicyManager.getProfileOwnerAsUser()}.
     *
     * @see DevicePolicyManager#getProfileOwnerAsUser
     */
    public @Nullable ComponentName getProfileOwnerAsUser(final int userId) {
        return mDpm.getProfileOwnerAsUser(userId);
    }

    /**
     * Calls {@code DevicePolicyManager.getDeviceOwnerNameOnAnyUser()}.
     *
     * @see DevicePolicyManager#getDeviceOwnerNameOnAnyUser
     */
    public CharSequence getDeviceOwnerOrganizationName() {
        return mDpm.getDeviceOwnerOrganizationName();
    }

    /**
     * Calls {@code DevicePolicyManager.getPermissionGrantState()}.
     *
     * @see DevicePolicyManager#getPermissionGrantState
     */
    public int getPermissionGrantState(@Nullable ComponentName admin, String packageName,
            String permission) {
        return mDpm.getPermissionGrantState(admin, packageName, permission);
    }

    /**
     * Calls {@code DevicePolicyManager.isSecurityLoggingEnabled()}.
     *
     * @see DevicePolicyManager#isSecurityLoggingEnabled
     */
    public boolean isSecurityLoggingEnabled(@Nullable ComponentName admin) {
        return mDpm.isSecurityLoggingEnabled(admin);
    }

    /**
     * Calls {@code DevicePolicyManager.isNetworkLoggingEnabled()}.
     *
     * @see DevicePolicyManager#isNetworkLoggingEnabled
     */
    public boolean isNetworkLoggingEnabled(@Nullable ComponentName admin) {
        return mDpm.isNetworkLoggingEnabled(admin);
    }

    /**
     * Calls {@code DevicePolicyManager.getLastSecurityLogRetrievalTime()}.
     *
     * @see DevicePolicyManager#getLastSecurityLogRetrievalTime
     */
    public long getLastSecurityLogRetrievalTime() {
        return mDpm.getLastSecurityLogRetrievalTime();
    }

    /**
     * Calls {@code DevicePolicyManager.getLastBugReportRequestTime()}.
     *
     * @see DevicePolicyManager#getLastBugReportRequestTime
     */
    public long getLastBugReportRequestTime() {
        return mDpm.getLastBugReportRequestTime();
    }

    /**
     * Calls {@code DevicePolicyManager.getLastNetworkLogRetrievalTime()}.
     *
     * @see DevicePolicyManager#getLastNetworkLogRetrievalTime
     */
    public long getLastNetworkLogRetrievalTime() {
        return mDpm.getLastNetworkLogRetrievalTime();
    }

    /**
     * Calls {@code DevicePolicyManager.isCurrentInputMethodSetByOwner()}.
     *
     * @see DevicePolicyManager#isCurrentInputMethodSetByOwner
     */
    public boolean isCurrentInputMethodSetByOwner() {
        return mDpm.isCurrentInputMethodSetByOwner();
    }

    /**
     * Calls {@code DevicePolicyManager.getOwnerInstalledCaCerts()}.
     *
     * @see DevicePolicyManager#getOwnerInstalledCaCerts
     */
    public List<String> getOwnerInstalledCaCerts(@NonNull UserHandle user) {
        return mDpm.getOwnerInstalledCaCerts(user);
    }

    /**
     * Calls {@code DevicePolicyManager.isDeviceOwnerAppOnAnyUser()}.
     *
     * @see DevicePolicyManager#isDeviceOwnerAppOnAnyUser
     */
    public boolean isDeviceOwnerAppOnAnyUser(String packageName) {
        return mDpm.isDeviceOwnerAppOnAnyUser(packageName);
    }

    /**
     * Calls {@code DevicePolicyManager.packageHasActiveAdmins()}.
     *
     * @see DevicePolicyManager#packageHasActiveAdmins
     */
    public boolean packageHasActiveAdmins(String packageName) {
        return mDpm.packageHasActiveAdmins(packageName);
    }

    /**
     * Calls {@code DevicePolicyManager.isUninstallInQueue()}.
     *
     * @see DevicePolicyManager#isUninstallInQueue
     */
    public boolean isUninstallInQueue(String packageName) {
        return mDpm.isUninstallInQueue(packageName);
    }

    /**
     * Calls {@code DevicePolicyManager.createAdminSupportIntent()}.
     *
     * @see DevicePolicyManager#createAdminSupportIntent(String)
     */
    public Intent createAdminSupportIntent(@NonNull String restriction) {
        return mDpm.createAdminSupportIntent(restriction);
    }

    /**
     * Calls {@code DevicePolicyManager#getDeviceOwnerUserId()}.
     *
     * @see DevicePolicyManager#getDeviceOwnerUserId()
     */
    public int getDeviceOwnerUserId() {
        return mDpm.getDeviceOwnerUserId();
    }
}
