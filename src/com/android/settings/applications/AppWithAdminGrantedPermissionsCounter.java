/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.applications;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.os.UserHandle;

/**
 * Counts installed apps across all users that have been granted one or more specific permissions by
 * the admin.
 */
public abstract class AppWithAdminGrantedPermissionsCounter extends AppCounter {

    private final String[] mPermissions;
    private final IPackageManager mPackageManagerService;
    private final DevicePolicyManager mDevicePolicyManager;

    public AppWithAdminGrantedPermissionsCounter(Context context, String[] permissions,
            PackageManager packageManager, IPackageManager packageManagerService,
            DevicePolicyManager devicePolicyManager) {
        super(context, packageManager);
        mPermissions = permissions;
        mPackageManagerService = packageManagerService;
        mDevicePolicyManager = devicePolicyManager;
    }

    @Override
    protected boolean includeInCount(ApplicationInfo info) {
        return includeInCount(mPermissions, mDevicePolicyManager, mPm, mPackageManagerService,
                info);
    }

    public static boolean includeInCount(String[] permissions,
            DevicePolicyManager devicePolicyManager, PackageManager packageManager,
            IPackageManager packageManagerService, ApplicationInfo info) {
        if (info.targetSdkVersion >= Build.VERSION_CODES.M) {
            // The app uses run-time permissions. Check whether one or more of the permissions were
            // granted by enterprise policy.
            for (final String permission : permissions) {
                if (devicePolicyManager.getPermissionGrantState(null /* admin */, info.packageName,
                        permission) == DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED) {
                    return true;
                }
            }
            return false;
        }

        // The app uses install-time permissions. Check whether the app requested one or more of the
        // permissions and was installed by enterprise policy, implicitly granting permissions.
        if (packageManager.getInstallReason(info.packageName,
                new UserHandle(UserHandle.getUserId(info.uid)))
                != PackageManager.INSTALL_REASON_POLICY) {
            return false;
        }
        try {
            for (final String permission : permissions) {
                if (packageManagerService.checkUidPermission(permission, info.uid)
                        == PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
            }
        } catch (RemoteException exception) {
        }
        return false;
    }
}
