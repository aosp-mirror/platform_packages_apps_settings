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

package com.android.settings.enterprise;

import android.annotation.NonNull;
import android.content.ComponentName;
import android.content.Intent;
import android.os.UserHandle;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * This interface replicates a subset of the android.app.admin.DevicePolicyManager (DPM). The
 * interface exists so that we can use a thin wrapper around the DPM in production code and a mock
 * in tests. We cannot directly mock or shadow the DPM, because some of the methods we rely on are
 * newer than the API version supported by Robolectric.
 */
public interface DevicePolicyManagerWrapper {
    /**
     * Calls {@code DevicePolicyManager.getActiveAdminsAsUser()}.
     *
     * @see android.app.admin.DevicePolicyManager#getActiveAdminsAsUser
     */
    public @Nullable List<ComponentName> getActiveAdminsAsUser(int userId);

    /**
     * Calls {@code DevicePolicyManager.getMaximumFailedPasswordsForWipe()}.
     *
     * @see android.app.admin.DevicePolicyManager#getMaximumFailedPasswordsForWipe
     */
    int getMaximumFailedPasswordsForWipe(@Nullable ComponentName admin, int userHandle);

    /**
     * Calls {@code DevicePolicyManager.getDeviceOwnerComponentOnCallingUser()}.
     *
     * @see android.app.admin.DevicePolicyManager#getDeviceOwnerComponentOnCallingUser
     */
    ComponentName getDeviceOwnerComponentOnCallingUser();

    /**
     * Calls {@code DevicePolicyManager.getDeviceOwnerComponentOnAnyUser()}.
     *
     * @see android.app.admin.DevicePolicyManager#getDeviceOwnerComponentOnAnyUser
     */
    ComponentName getDeviceOwnerComponentOnAnyUser();

    /**
     * Calls {@code DevicePolicyManager.getProfileOwnerAsUser()}.
     *
     * @see android.app.admin.DevicePolicyManager#getProfileOwnerAsUser
     */
    @Nullable ComponentName getProfileOwnerAsUser(final int userId);

    /**
     * Calls {@code DevicePolicyManager.getDeviceOwnerNameOnAnyUser()}.
     *
     * @see android.app.admin.DevicePolicyManager#getDeviceOwnerNameOnAnyUser
     */
    CharSequence getDeviceOwnerOrganizationName();

    /**
     * Calls {@code DevicePolicyManager.getPermissionGrantState()}.
     *
     * @see android.app.admin.DevicePolicyManager#getPermissionGrantState
     */
    int getPermissionGrantState(@Nullable ComponentName admin, String packageName,
            String permission);

    /**
     * Calls {@code DevicePolicyManager.isSecurityLoggingEnabled()}.
     *
     * @see android.app.admin.DevicePolicyManager#isSecurityLoggingEnabled
     */
    boolean isSecurityLoggingEnabled(@Nullable ComponentName admin);

    /**
     * Calls {@code DevicePolicyManager.isNetworkLoggingEnabled()}.
     *
     * @see android.app.admin.DevicePolicyManager#isNetworkLoggingEnabled
     */
    boolean isNetworkLoggingEnabled(@Nullable ComponentName admin);

    /**
     * Calls {@code DevicePolicyManager.getLastSecurityLogRetrievalTime()}.
     *
     * @see android.app.admin.DevicePolicyManager#getLastSecurityLogRetrievalTime
     */
    long getLastSecurityLogRetrievalTime();

    /**
     * Calls {@code DevicePolicyManager.getLastBugReportRequestTime()}.
     *
     * @see android.app.admin.DevicePolicyManager#getLastBugReportRequestTime
     */
    long getLastBugReportRequestTime();

    /**
     * Calls {@code DevicePolicyManager.getLastNetworkLogRetrievalTime()}.
     *
     * @see android.app.admin.DevicePolicyManager#getLastNetworkLogRetrievalTime
     */
    long getLastNetworkLogRetrievalTime();

    /**
     * Calls {@code DevicePolicyManager.isCurrentInputMethodSetByOwner()}.
     *
     * @see android.app.admin.DevicePolicyManager#isCurrentInputMethodSetByOwner
     */
    boolean isCurrentInputMethodSetByOwner();


    /**
     * Calls {@code DevicePolicyManager.getOwnerInstalledCaCerts()}.
     *
     * @see android.app.admin.DevicePolicyManager#getOwnerInstalledCaCerts
     */
    List<String> getOwnerInstalledCaCerts(@NonNull UserHandle user);

    /**
     * Calls {@code DevicePolicyManager.isDeviceOwnerAppOnAnyUser()}.
     *
     * @see android.app.admin.DevicePolicyManager#isDeviceOwnerAppOnAnyUser
     */
    boolean isDeviceOwnerAppOnAnyUser(String packageName);

    /**
     * Calls {@code DevicePolicyManager.packageHasActiveAdmins()}.
     *
     * @see android.app.admin.DevicePolicyManager#packageHasActiveAdmins
     */
    boolean packageHasActiveAdmins(String packageName);

    /**
     * Calls {@code DevicePolicyManager.isUninstallInQueue()}.
     *
     * @see android.app.admin.DevicePolicyManager#isUninstallInQueue
     */
    boolean isUninstallInQueue(String packageName);

    /**
     * Calls {@code DevicePolicyManager.createAdminSupportIntent()}.
     *
     * @see android.app.admin.DevicePolicyManager#createAdminSupportIntent
     */
    Intent createAdminSupportIntent(String restriction);
}
