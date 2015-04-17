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

import android.Manifest;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.AppOpsManager.PackageOps;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;

import com.android.settings.applications.ApplicationsState.AppEntry;
import com.android.settings.applications.ApplicationsState.AppFilter;

import java.util.List;

/*
 * Connects app usage info to the ApplicationsState.
 * Also provides app filters that can use the info.
 */
public class AppStateUsageBridge extends AppStateBaseBridge {

    private static final String TAG = "AppStateUsageBridge";

    private static final String[] PM_USAGE_STATS_PERMISSION = {
            Manifest.permission.PACKAGE_USAGE_STATS
    };

    private static final int[] APP_OPS_OP_CODES = {
            AppOpsManager.OP_GET_USAGE_STATS
    };

    private final IPackageManager mIPackageManager;
    private final UserManager mUserManager;
    private final List<UserHandle> mProfiles;
    private final AppOpsManager mAppOpsManager;
    private final Context mContext;

    public AppStateUsageBridge(Context context, ApplicationsState appState, Callback callback) {
        super(appState, callback);
        mContext = context;
        mIPackageManager = AppGlobals.getPackageManager();
        mUserManager = UserManager.get(context);
        mProfiles = mUserManager.getUserProfiles();
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
    }

    private boolean isThisUserAProfileOfCurrentUser(final int userId) {
        final int profilesMax = mProfiles.size();
        for (int i = 0; i < profilesMax; i++) {
            if (mProfiles.get(i).getIdentifier() == userId) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void updateExtraInfo(AppEntry app, String pkg, int uid) {
        app.extraInfo = getUsageInfo(pkg, uid);
    }

    public UsageState getUsageInfo(String pkg, int uid) {
        UsageState usageState = new UsageState(pkg, new UserHandle(UserHandle.getUserId(uid)));
        try {
            usageState.packageInfo = mIPackageManager.getPackageInfo(pkg,
                    PackageManager.GET_PERMISSIONS, usageState.userHandle.getIdentifier());
            // Check permission state.
            String[] requestedPermissions = usageState.packageInfo.requestedPermissions;
            int[] permissionFlags = usageState.packageInfo.requestedPermissionsFlags;
            if (requestedPermissions != null) {
                for (int i = 0; i < requestedPermissions.length; i++) {
                    if (Manifest.permission.PACKAGE_USAGE_STATS.equals(requestedPermissions[i])
                            && (permissionFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED)
                            != 0) {
                        usageState.permissionGranted = true;
                        break;
                    }
                }
            }
            // Check app op state.
            List<PackageOps> ops = mAppOpsManager.getOpsForPackage(uid, pkg, APP_OPS_OP_CODES);
            if (ops != null && ops.size() > 0 && ops.get(0).getOps().size() > 0) {
                usageState.appOpMode = ops.get(0).getOps().get(0).getMode();
            }
        } catch (RemoteException e) {
            Log.w(TAG, "PackageManager is dead. Can't get package info " + pkg, e);
        }
        return usageState;
    }

    @Override
    protected void loadAllExtraInfo() {
        SparseArray<ArrayMap<String, UsageState>> entries = getEntries();

        // Load state info.
        loadPermissionsStates(entries);
        loadAppOpsStates(entries);

        // Map states to application info.
        List<AppEntry> apps = mAppSession.getAllApps();
        final int N = apps.size();
        for (int i = 0; i < N; i++) {
            AppEntry app = apps.get(i);
            int userId = UserHandle.getUserId(app.info.uid);
            ArrayMap<String, UsageState> userMap = entries.get(userId);
            app.extraInfo = userMap != null ? userMap.get(app.info.packageName) : null;
        }
    }

    private SparseArray<ArrayMap<String, UsageState>> getEntries() {
        try {
            final String[] packages = mIPackageManager.getAppOpPermissionPackages(
                    Manifest.permission.PACKAGE_USAGE_STATS);

            if (packages == null) {
                // No packages are requesting permission to use the UsageStats API.
                return null;
            }

            SparseArray<ArrayMap<String, UsageState>> entries = new SparseArray<>();
            for (final UserHandle profile : mProfiles) {
                final ArrayMap<String, UsageState> entriesForProfile = new ArrayMap<>();
                final int profileId = profile.getIdentifier();
                entries.put(profileId, entriesForProfile);
                for (final String packageName : packages) {
                    final boolean isAvailable = mIPackageManager.isPackageAvailable(packageName,
                            profileId);
                    if (!shouldIgnorePackage(packageName) && isAvailable) {
                        final UsageState newEntry = new UsageState(packageName, profile);
                        entriesForProfile.put(packageName, newEntry);
                    }
                }
            }

            return entries;
        } catch (RemoteException e) {
            Log.w(TAG, "PackageManager is dead. Can't get list of packages requesting "
                    + Manifest.permission.PACKAGE_USAGE_STATS, e);
            return null;
        }
    }

    private void loadPermissionsStates(SparseArray<ArrayMap<String, UsageState>> entries) {
         // Load the packages that have been granted the PACKAGE_USAGE_STATS permission.
        try {
            for (final UserHandle profile : mProfiles) {
                final int profileId = profile.getIdentifier();
                final ArrayMap<String, UsageState> entriesForProfile = entries.get(profileId);
                if (entriesForProfile == null) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                final List<PackageInfo> packageInfos = mIPackageManager
                        .getPackagesHoldingPermissions(PM_USAGE_STATS_PERMISSION, 0, profileId)
                        .getList();
                final int packageInfoCount = packageInfos != null ? packageInfos.size() : 0;
                for (int i = 0; i < packageInfoCount; i++) {
                    final PackageInfo packageInfo = packageInfos.get(i);
                    final UsageState pe = entriesForProfile.get(packageInfo.packageName);
                    if (pe != null) {
                        pe.packageInfo = packageInfo;
                        pe.permissionGranted = true;
                    }
                }
            }
        } catch (RemoteException e) {
            Log.w(TAG, "PackageManager is dead. Can't get list of packages granted "
                    + Manifest.permission.PACKAGE_USAGE_STATS, e);
            return;
        }
    }

    private void loadAppOpsStates(SparseArray<ArrayMap<String, UsageState>> entries) {
        // Find out which packages have been granted permission from AppOps.
        final List<AppOpsManager.PackageOps> packageOps = mAppOpsManager.getPackagesForOps(
                APP_OPS_OP_CODES);
        final int packageOpsCount = packageOps != null ? packageOps.size() : 0;
        for (int i = 0; i < packageOpsCount; i++) {
            final AppOpsManager.PackageOps packageOp = packageOps.get(i);
            final int userId = UserHandle.getUserId(packageOp.getUid());
            if (!isThisUserAProfileOfCurrentUser(userId)) {
                // This AppOp does not belong to any of this user's profiles.
                continue;
            }

            final ArrayMap<String, UsageState> entriesForProfile = entries.get(userId);
            if (entriesForProfile == null) {
                continue;
            }
            final UsageState pe = entriesForProfile.get(packageOp.getPackageName());
            if (pe == null) {
                Log.w(TAG, "AppOp permission exists for package " + packageOp.getPackageName()
                        + " of user " + userId +
                        " but package doesn't exist or did not request UsageStats access");
                continue;
            }

            if (packageOp.getOps().size() < 1) {
                Log.w(TAG, "No AppOps permission exists for package "
                        + packageOp.getPackageName());
                continue;
            }

            pe.appOpMode = packageOp.getOps().get(0).getMode();
        }
    }

    private boolean shouldIgnorePackage(String packageName) {
        return packageName.equals("android") || packageName.equals(mContext.getPackageName());
    }

    public static class UsageState {
        public final String packageName;
        public final UserHandle userHandle;
        public PackageInfo packageInfo;
        public boolean permissionGranted;
        public int appOpMode;

        public UsageState(String packageName, UserHandle userHandle) {
            this.packageName = packageName;
            this.appOpMode = AppOpsManager.MODE_DEFAULT;
            this.userHandle = userHandle;
        }

        public boolean hasAccess() {
            if (appOpMode == AppOpsManager.MODE_DEFAULT) {
                return permissionGranted;
            }
            return appOpMode == AppOpsManager.MODE_ALLOWED;
        }
    }

    public static final AppFilter FILTER_APP_USAGE = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry info) {
            return info.extraInfo != null;
        }
    };
}
