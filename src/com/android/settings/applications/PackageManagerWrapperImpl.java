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

package com.android.settings.applications;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.os.storage.VolumeInfo;

import java.util.List;

public class PackageManagerWrapperImpl implements PackageManagerWrapper {

    private final PackageManager mPm;

    public PackageManagerWrapperImpl(PackageManager pm) {
        mPm = pm;
    }

    @Override
    public PackageManager getPackageManager() {
        return mPm;
    }

    @Override
    public List<ApplicationInfo> getInstalledApplicationsAsUser(int flags, int userId) {
        return mPm.getInstalledApplicationsAsUser(flags, userId);
    }

    @Override
    public List<PackageInfo> getInstalledPackagesAsUser(int flags, int userId) {
        return mPm.getInstalledPackagesAsUser(flags, userId);
    }

    @Override
    public boolean hasSystemFeature(String name) {
        return mPm.hasSystemFeature(name);
    }

    @Override
    public List<ResolveInfo> queryIntentActivitiesAsUser(Intent intent, int flags, int userId) {
        return mPm.queryIntentActivitiesAsUser(intent, flags, userId);
    }

    @Override
    public int getInstallReason(String packageName, UserHandle user) {
        return mPm.getInstallReason(packageName, user);
    }

    @Override
    public ApplicationInfo getApplicationInfoAsUser(String packageName, int i, int userId)
            throws PackageManager.NameNotFoundException {
        return mPm.getApplicationInfoAsUser(packageName, i, userId);
    }

    @Override
    public boolean setDefaultBrowserPackageNameAsUser(String packageName, int userId) {
        return mPm.setDefaultBrowserPackageNameAsUser(packageName, userId);
    }

    @Override
    public String getDefaultBrowserPackageNameAsUser(int userId) {
        return mPm.getDefaultBrowserPackageNameAsUser(userId);
    }

    @Override
    public ComponentName getHomeActivities(List<ResolveInfo> homeActivities) {
        return mPm.getHomeActivities(homeActivities);
    }

    @Override
    public List<ResolveInfo> queryIntentServicesAsUser(Intent intent, int i, int user) {
        return mPm.queryIntentServicesAsUser(intent, i, user);
    }

    @Override
    public void replacePreferredActivity(IntentFilter homeFilter, int matchCategoryEmpty,
            ComponentName[] componentNames, ComponentName component) {
        mPm.replacePreferredActivity(homeFilter, matchCategoryEmpty, componentNames, component);
    }

    @Override
    public VolumeInfo getPrimaryStorageCurrentVolume() {
        return mPm.getPrimaryStorageCurrentVolume();
    }

    @Override
    public void deletePackageAsUser(String packageName, IPackageDeleteObserver observer, int flags,
            int userId) {
        mPm.deletePackageAsUser(packageName, observer, flags, userId);
    }

    @Override
    public int getPackageUidAsUser(String pkg, int userId)
            throws PackageManager.NameNotFoundException {
        return mPm.getPackageUidAsUser(pkg, userId);
    }
}
