/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionInfo;
import android.os.Build;
import android.util.ArrayMap;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Based off from
 * packages/apps/PackageInstaller/src/com/android/packageinstaller/permission/AppPermissions.java
 * Except we only care about the number rather than the details.
 */
public final class AppPermissions {
    private static final String TAG = "AppPermissions";

    private final ArrayMap<String, PermissionGroup> mGroups = new ArrayMap<>();
    private final Context mContext;
    private final PackageInfo mPackageInfo;

    public AppPermissions(Context context, String packageName) {
        mContext = context;
        mPackageInfo = getPackageInfo(packageName);
        refresh();
    }

    private PackageInfo getPackageInfo(String packageName) {
        try {
            return mContext.getPackageManager().getPackageInfo(packageName,
                    PackageManager.GET_PERMISSIONS);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Unable to find " + packageName, e);
            return null;
        }
    }

    public void refresh() {
        if (mPackageInfo != null) {
            loadPermissionGroups();
        }
    }

    public int getPermissionCount() {
        return mGroups.size();
    }

    public int getGrantedPermissionsCount() {
        int ct = 0;
        for (int i = 0; i < mGroups.size(); i++) {
            if (mGroups.valueAt(i).areRuntimePermissionsGranted()) {
                ct++;
            }
        }
        return ct;
    }

    private void loadPermissionGroups() {
        mGroups.clear();
        if (mPackageInfo.requestedPermissions == null) {
            return;
        }

        final boolean appSupportsRuntimePermissions = appSupportsRuntime(
                mPackageInfo.applicationInfo);

        for (int i = 0; i < mPackageInfo.requestedPermissions.length; i++) {
            String requestedPerm = mPackageInfo.requestedPermissions[i];

            final PermissionInfo permInfo;
            try {
                permInfo = mContext.getPackageManager().getPermissionInfo(requestedPerm, 0);
            } catch (NameNotFoundException e) {
                Log.w(TAG, "Unknown permission: " + requestedPerm);
                continue;
            }

            String permName = permInfo.name;
            String groupName = permInfo.group != null ? permInfo.group : permName;

            PermissionGroup group = mGroups.get(groupName);
            if (group == null) {
                group = new PermissionGroup();
                mGroups.put(groupName, group);
            }

            final boolean runtime = appSupportsRuntimePermissions
                    && permInfo.protectionLevel == PermissionInfo.PROTECTION_DANGEROUS;
            final boolean granted = (mPackageInfo.requestedPermissionsFlags[i]
                    & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0;

            Permission permission = new Permission(runtime, granted);
            group.addPermission(permission, permName);
        }
        // Only care about runtime perms for now.
        for (int i = mGroups.size() - 1; i >= 0; i--) {
            if (!mGroups.valueAt(i).mHasRuntimePermissions) {
                mGroups.removeAt(i);
            }
        }
    }

    public static boolean appSupportsRuntime(ApplicationInfo info) {
        return info.targetSdkVersion > Build.VERSION_CODES.LOLLIPOP_MR1;
    }

    private static final class PermissionGroup {
        private final ArrayMap<String, Permission> mPermissions = new ArrayMap<>();
        private boolean mHasRuntimePermissions;

        public boolean hasRuntimePermissions() {
            return mHasRuntimePermissions;
        }

        public boolean areRuntimePermissionsGranted() {
            final int permissionCount = mPermissions.size();
            for (int i = 0; i < permissionCount; i++) {
                Permission permission = mPermissions.valueAt(i);
                if (permission.runtime && !permission.granted) {
                    return false;
                }
            }
            return true;
        }

        public List<Permission> getPermissions() {
            return new ArrayList<>(mPermissions.values());
        }

        void addPermission(Permission permission, String permName) {
            mPermissions.put(permName, permission);
            if (permission.runtime) {
                mHasRuntimePermissions = true;
            }
        }
    }

    private static final class Permission {
        private final boolean runtime;
        private boolean granted;

        public Permission(boolean runtime, boolean granted) {
            this.runtime = runtime;
            this.granted = granted;
        }
    }
}
