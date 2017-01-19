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

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArraySet;
import android.view.View;

import com.android.settings.enterprise.DevicePolicyManagerWrapper;

import java.util.List;
import java.util.Set;

public class ApplicationFeatureProviderImpl implements ApplicationFeatureProvider {

    private final Context mContext;
    private final PackageManagerWrapper mPm;
    private final IPackageManagerWrapper mPms;
    private final DevicePolicyManagerWrapper mDpm;
    private final UserManager mUm;

    public ApplicationFeatureProviderImpl(Context context, PackageManagerWrapper pm,
            IPackageManagerWrapper pms, DevicePolicyManagerWrapper dpm) {
        mContext = context.getApplicationContext();
        mPm = pm;
        mPms = pms;
        mDpm = dpm;
        mUm = UserManager.get(mContext);
    }

    @Override
    public AppHeaderController newAppHeaderController(Fragment fragment, View appHeader) {
        return new AppHeaderController(mContext, fragment, appHeader);
    }

    @Override
    public void calculateNumberOfInstalledApps(int installReason, NumberOfAppsCallback callback) {
        new AllUserInstalledAppCounter(mContext, installReason, mPm, callback).execute();
    }

    @Override
    public void calculateNumberOfAppsWithAdminGrantedPermissions(String[] permissions,
            NumberOfAppsCallback callback) {
        new AllUserAppWithAdminGrantedPermissionsCounter(mContext, permissions, mPm, mPms, mDpm,
                callback).execute();
    }

    @Override
    public Set<PersistentPreferredActivityInfo> findPersistentPreferredActivities(
            Intent[] intents) {
        final Set<PersistentPreferredActivityInfo> activities = new ArraySet<>();
        final List<UserHandle> users = mUm.getUserProfiles();
        for (final Intent intent : intents) {
            for (final UserHandle user : users) {
                final int userId = user.getIdentifier();
                try {
                    final ResolveInfo resolveInfo = mPms.findPersistentPreferredActivity(intent,
                            userId);
                    if (resolveInfo != null) {
                        ComponentInfo componentInfo = null;
                        if (resolveInfo.activityInfo != null) {
                            componentInfo = resolveInfo.activityInfo;
                        } else if (resolveInfo.serviceInfo != null) {
                            componentInfo = resolveInfo.serviceInfo;
                        } else if (resolveInfo.providerInfo != null) {
                            componentInfo = resolveInfo.providerInfo;
                        }
                        if (componentInfo != null) {
                            activities.add(new PersistentPreferredActivityInfo(
                                    componentInfo.packageName, userId));
                        }
                    }
                } catch (RemoteException exception) {
                }
            }

        }
        return activities;
    }

    private static class AllUserInstalledAppCounter extends InstalledAppCounter {
        private NumberOfAppsCallback mCallback;

        AllUserInstalledAppCounter(Context context, int installReason,
                PackageManagerWrapper packageManager, NumberOfAppsCallback callback) {
            super(context, installReason, packageManager);
            mCallback = callback;
        }

        @Override
        protected void onCountComplete(int num) {
            mCallback.onNumberOfAppsResult(num);
        }

        @Override
        protected List<UserInfo> getUsersToCount() {
            return mUm.getUsers(true /* excludeDying */);
        }
    }

    private static class AllUserAppWithAdminGrantedPermissionsCounter extends
            AppWithAdminGrantedPermissionsCounter {
        private NumberOfAppsCallback mCallback;

        AllUserAppWithAdminGrantedPermissionsCounter(Context context, String[] permissions,
                PackageManagerWrapper packageManager, IPackageManagerWrapper packageManagerService,
                DevicePolicyManagerWrapper devicePolicyManager, NumberOfAppsCallback callback) {
            super(context, permissions, packageManager, packageManagerService, devicePolicyManager);
            mCallback = callback;
        }

        @Override
        protected void onCountComplete(int num) {
            mCallback.onNumberOfAppsResult(num);
        }

        @Override
        protected List<UserInfo> getUsersToCount() {
            return mUm.getUsers(true /* excludeDying */);
        }
    }
}
