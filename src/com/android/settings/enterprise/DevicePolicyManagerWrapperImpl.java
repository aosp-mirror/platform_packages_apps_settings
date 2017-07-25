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
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.UserHandle;
import android.support.annotation.Nullable;

import java.util.List;

public class DevicePolicyManagerWrapperImpl implements DevicePolicyManagerWrapper {
    private final DevicePolicyManager mDpm;

    public DevicePolicyManagerWrapperImpl(DevicePolicyManager dpm) {
        mDpm = dpm;
    }

    @Override
    public @Nullable List<ComponentName> getActiveAdminsAsUser(int userId) {
        return mDpm.getActiveAdminsAsUser(userId);
    }

    @Override
    public int getMaximumFailedPasswordsForWipe(@Nullable ComponentName admin, int userHandle) {
        return mDpm.getMaximumFailedPasswordsForWipe(admin, userHandle);
    }

    @Override
    public ComponentName getDeviceOwnerComponentOnCallingUser() {
        return mDpm.getDeviceOwnerComponentOnCallingUser();
    }

    @Override
    public ComponentName getDeviceOwnerComponentOnAnyUser() {
        return mDpm.getDeviceOwnerComponentOnAnyUser();
    }

    @Override
    public @Nullable ComponentName getProfileOwnerAsUser(final int userId) {
        return mDpm.getProfileOwnerAsUser(userId);
    }

    @Override
    public CharSequence getDeviceOwnerOrganizationName() {
        return mDpm.getDeviceOwnerOrganizationName();
    }

    @Override
    public int getPermissionGrantState(@Nullable ComponentName admin, String packageName,
            String permission) {
        return mDpm.getPermissionGrantState(admin, packageName, permission);
    }

    @Override
    public boolean isSecurityLoggingEnabled(@Nullable ComponentName admin) {
        return mDpm.isSecurityLoggingEnabled(admin);
    }

    @Override
    public boolean isNetworkLoggingEnabled(@Nullable ComponentName admin) {
        return mDpm.isNetworkLoggingEnabled(admin);
    }

    @Override
    public long getLastSecurityLogRetrievalTime() {
        return mDpm.getLastSecurityLogRetrievalTime();
    }

    @Override
    public long getLastBugReportRequestTime() {
        return mDpm.getLastBugReportRequestTime();
    }

    @Override
    public long getLastNetworkLogRetrievalTime() {
        return mDpm.getLastNetworkLogRetrievalTime();
    }

    @Override
    public boolean isCurrentInputMethodSetByOwner() {
        return mDpm.isCurrentInputMethodSetByOwner();
    }

    @Override
    public List<String> getOwnerInstalledCaCerts(@NonNull UserHandle user) {
        return mDpm.getOwnerInstalledCaCerts(user);
    }

    @Override
    public boolean isDeviceOwnerAppOnAnyUser(String packageName) {
        return mDpm.isDeviceOwnerAppOnAnyUser(packageName);
    }

    @Override
    public boolean packageHasActiveAdmins(String packageName) {
        return mDpm.packageHasActiveAdmins(packageName);
    }

    @Override
    public boolean isUninstallInQueue(String packageName) {
        return mDpm.isUninstallInQueue(packageName);
    }

    @Override
    public Intent createAdminSupportIntent(@NonNull String restriction) {
        return mDpm.createAdminSupportIntent(restriction);
    }
}
