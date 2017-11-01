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

package com.android.settings.applications;

import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;

public class IPackageManagerWrapperImpl implements IPackageManagerWrapper {

    private final IPackageManager mPms;

    public IPackageManagerWrapperImpl(IPackageManager pms) {
        mPms = pms;
    }

    @Override
    public int checkUidPermission(String permName, int uid) throws RemoteException {
        return mPms.checkUidPermission(permName, uid);
    }

    @Override
    public ResolveInfo findPersistentPreferredActivity(Intent intent, int userId)
            throws RemoteException {
        return mPms.findPersistentPreferredActivity(intent, userId);
    }

    @Override
    public PackageInfo getPackageInfo(String packageName, int flags, int userId)
            throws RemoteException {
        return mPms.getPackageInfo(packageName, flags, userId);
    }

    @Override
    public String[] getAppOpPermissionPackages(String permissionName) throws RemoteException {
        return mPms.getAppOpPermissionPackages(permissionName);
    }

    @Override
    public boolean isPackageAvailable(String packageName, int userId) throws RemoteException {
        return mPms.isPackageAvailable(packageName, userId);
    }

    @Override
    public ParceledListSlice<PackageInfo> getPackagesHoldingPermissions(
        String[] permissions, int flags, int userId) throws RemoteException {
        return mPms.getPackagesHoldingPermissions(permissions, flags, userId);
    }

}
