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

package com.android.settings.wrapper;

import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;

/**
 * This class replicates a subset of the android.content.pm.IPackageManager (PMS). The class
 * exists so that we can use a thin wrapper around the PMS in production code and a mock in tests.
 * We cannot directly mock or shadow the PMS, because some of the methods we rely on are newer than
 * the API version supported by Robolectric.
 */
public class IPackageManagerWrapper {

    private final IPackageManager mPms;

    public IPackageManagerWrapper(IPackageManager pms) {
        mPms = pms;
    }

    /**
     * Calls {@code IPackageManager.checkUidPermission()}.
     *
     * @see android.content.pm.IPackageManager#checkUidPermission
     */
    public int checkUidPermission(String permName, int uid) throws RemoteException {
        return mPms.checkUidPermission(permName, uid);
    }

    /**
     * Calls {@code IPackageManager.findPersistentPreferredActivity()}.
     *
     * @see android.content.pm.IPackageManager#findPersistentPreferredActivity
     */
    public ResolveInfo findPersistentPreferredActivity(Intent intent, int userId)
            throws RemoteException {
        return mPms.findPersistentPreferredActivity(intent, userId);
    }

    /**
     * Calls {@code IPackageManager.getPackageInfo()}.
     *
     * @see android.content.pm.IPackageManager#getPackageInfo
     */
    public PackageInfo getPackageInfo(String packageName, int flags, int userId)
            throws RemoteException {
        return mPms.getPackageInfo(packageName, flags, userId);
    }

    /**
     * Calls {@code IPackageManager.getAppOpPermissionPackages()}.
     *
     * @see android.content.pm.IPackageManager#getAppOpPermissionPackages
     */
    public String[] getAppOpPermissionPackages(String permissionName) throws RemoteException {
        return mPms.getAppOpPermissionPackages(permissionName);
    }

    /**
     * Calls {@code IPackageManager.isPackageAvailable()}.
     *
     * @see android.content.pm.IPackageManager#isPackageAvailable
     */
    public boolean isPackageAvailable(String packageName, int userId) throws RemoteException {
        return mPms.isPackageAvailable(packageName, userId);
    }

    /**
     * Calls {@code IPackageManager.getPackagesHoldingPermissions()}.
     *
     * @see android.content.pm.IPackageManager#getPackagesHoldingPermissions
     */
    public ParceledListSlice<PackageInfo> getPackagesHoldingPermissions(
        String[] permissions, int flags, int userId) throws RemoteException {
        return mPms.getPackagesHoldingPermissions(permissions, flags, userId);
    }

}
