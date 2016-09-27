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

import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * Connects app ops info to the ApplicationsState. Makes use of AppOpsManager to
 * determine further permission level.
 */
public abstract class AppStateAppOpsBridge extends AppStateBaseBridge {

    private static final String TAG = "AppStateAppOpsBridge";

    private final IPackageManager mIPackageManager;
    private final UserManager mUserManager;
    private final List<UserHandle> mProfiles;
    private final AppOpsManager mAppOpsManager;
    private final Context mContext;
    private final int[] mAppOpsOpCodes;
    private final String[] mPermissions;

    public AppStateAppOpsBridge(Context context, ApplicationsState appState, Callback callback,
            int appOpsOpCode, String[] permissions) {
        super(appState, callback);
        mContext = context;
        mIPackageManager = AppGlobals.getPackageManager();
        mUserManager = UserManager.get(context);
        mProfiles = mUserManager.getUserProfiles();
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        mAppOpsOpCodes = new int[] {appOpsOpCode};
        mPermissions = permissions;
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

    protected abstract void updateExtraInfo(AppEntry app, String pkg, int uid);

    private boolean doesAnyPermissionMatch(String permissionToMatch, String[] permissions) {
        for (String permission : permissions) {
            if (permissionToMatch.equals(permission)) {
                return true;
            }
        }
        return false;
    }

    public PermissionState getPermissionInfo(String pkg, int uid) {
        PermissionState permissionState = new PermissionState(pkg, new UserHandle(UserHandle
                .getUserId(uid)));
        try {
            permissionState.packageInfo = mIPackageManager.getPackageInfo(pkg,
                    PackageManager.GET_PERMISSIONS | PackageManager.MATCH_UNINSTALLED_PACKAGES,
                    permissionState.userHandle.getIdentifier());
            // Check static permission state (whatever that is declared in package manifest)
            String[] requestedPermissions = permissionState.packageInfo.requestedPermissions;
            int[] permissionFlags = permissionState.packageInfo.requestedPermissionsFlags;
            if (requestedPermissions != null) {
                for (int i = 0; i < requestedPermissions.length; i++) {
                    if (doesAnyPermissionMatch(requestedPermissions[i], mPermissions)) {
                        permissionState.permissionDeclared = true;
                        if ((permissionFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                            permissionState.staticPermissionGranted = true;
                            break;
                        }
                    }
                }
            }
            // Check app op state.
            List<PackageOps> ops = mAppOpsManager.getOpsForPackage(uid, pkg, mAppOpsOpCodes);
            if (ops != null && ops.size() > 0 && ops.get(0).getOps().size() > 0) {
                permissionState.appOpMode = ops.get(0).getOps().get(0).getMode();
            }
        } catch (RemoteException e) {
            Log.w(TAG, "PackageManager is dead. Can't get package info " + pkg, e);
        }
        return permissionState;
    }

    @Override
    protected void loadAllExtraInfo() {
        SparseArray<ArrayMap<String, PermissionState>> entries = getEntries();

        // Load state info.
        loadPermissionsStates(entries);
        loadAppOpsStates(entries);

        // Map states to application info.
        List<AppEntry> apps = mAppSession.getAllApps();
        final int N = apps.size();
        for (int i = 0; i < N; i++) {
            AppEntry app = apps.get(i);
            int userId = UserHandle.getUserId(app.info.uid);
            ArrayMap<String, PermissionState> userMap = entries.get(userId);
            app.extraInfo = userMap != null ? userMap.get(app.info.packageName) : null;
        }
    }

    /*
     * Gets a sparse array that describes every user on the device and all the associated packages
     * of each user, together with the packages available for that user.
     */
    private SparseArray<ArrayMap<String, PermissionState>> getEntries() {
        try {
            Set<String> packagesSet = new HashSet<>();
            for (String permission : mPermissions) {
                String[] pkgs = mIPackageManager.getAppOpPermissionPackages(permission);
                if (pkgs != null) {
                    packagesSet.addAll(Arrays.asList(pkgs));
                }
            }

            if (packagesSet.isEmpty()) {
                // No packages are requesting permission as specified by mPermissions.
                return null;
            }

            // Create a sparse array that maps profileIds to an ArrayMap that maps package names to
            // an associated PermissionState object
            SparseArray<ArrayMap<String, PermissionState>> entries = new SparseArray<>();
            for (final UserHandle profile : mProfiles) {
                final ArrayMap<String, PermissionState> entriesForProfile = new ArrayMap<>();
                final int profileId = profile.getIdentifier();
                entries.put(profileId, entriesForProfile);
                for (final String packageName : packagesSet) {
                    final boolean isAvailable = mIPackageManager.isPackageAvailable(packageName,
                            profileId);
                    if (!shouldIgnorePackage(packageName) && isAvailable) {
                        final PermissionState newEntry = new PermissionState(packageName, profile);
                        entriesForProfile.put(packageName, newEntry);
                    }
                }
            }

            return entries;
        } catch (RemoteException e) {
            Log.w(TAG, "PackageManager is dead. Can't get list of packages requesting "
                    + mPermissions[0], e);
            return null;
        }
    }

    /*
     * This method will set the packageInfo and staticPermissionGranted field of the associated
     * PermissionState, which describes a particular package.
     */
    private void loadPermissionsStates(SparseArray<ArrayMap<String, PermissionState>> entries) {
        // Load the packages that have been granted the permission specified in mPermission.
        if (entries == null) {
            return;
        }

        try {
            for (final UserHandle profile : mProfiles) {
                final int profileId = profile.getIdentifier();
                final ArrayMap<String, PermissionState> entriesForProfile = entries.get(profileId);
                if (entriesForProfile == null) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                final List<PackageInfo> packageInfos = mIPackageManager
                        .getPackagesHoldingPermissions(mPermissions, 0, profileId).getList();
                final int packageInfoCount = packageInfos != null ? packageInfos.size() : 0;
                for (int i = 0; i < packageInfoCount; i++) {
                    final PackageInfo packageInfo = packageInfos.get(i);
                    final PermissionState pe = entriesForProfile.get(packageInfo.packageName);
                    if (pe != null) {
                        pe.packageInfo = packageInfo;
                        pe.staticPermissionGranted = true;
                    }
                }
            }
        } catch (RemoteException e) {
            Log.w(TAG, "PackageManager is dead. Can't get list of packages granted "
                    + mPermissions, e);
            return;
        }
    }

    /*
     * This method will set the appOpMode field of the associated PermissionState, which describes
     * a particular package.
     */
    private void loadAppOpsStates(SparseArray<ArrayMap<String, PermissionState>> entries) {
        // Find out which packages have been granted permission from AppOps.
        final List<AppOpsManager.PackageOps> packageOps = mAppOpsManager.getPackagesForOps(
                mAppOpsOpCodes);
        final int packageOpsCount = packageOps != null ? packageOps.size() : 0;
        for (int i = 0; i < packageOpsCount; i++) {
            final AppOpsManager.PackageOps packageOp = packageOps.get(i);
            final int userId = UserHandle.getUserId(packageOp.getUid());
            if (!isThisUserAProfileOfCurrentUser(userId)) {
                // This AppOp does not belong to any of this user's profiles.
                continue;
            }

            final ArrayMap<String, PermissionState> entriesForProfile = entries.get(userId);
            if (entriesForProfile == null) {
                continue;
            }
            final PermissionState pe = entriesForProfile.get(packageOp.getPackageName());
            if (pe == null) {
                Log.w(TAG, "AppOp permission exists for package " + packageOp.getPackageName()
                        + " of user " + userId + " but package doesn't exist or did not request "
                        + mPermissions + " access");
                continue;
            }

            if (packageOp.getOps().size() < 1) {
                Log.w(TAG, "No AppOps permission exists for package " + packageOp.getPackageName());
                continue;
            }
            pe.appOpMode = packageOp.getOps().get(0).getMode();
        }
    }

    /*
     * Check for packages that should be ignored for further processing
     */
    private boolean shouldIgnorePackage(String packageName) {
        return packageName.equals("android") || packageName.equals(mContext.getPackageName());
    }

    public int getNumPackagesDeclaredPermission() {
        SparseArray<ArrayMap<String, PermissionState>> entries = getEntries();
        if (entries == null) {
            return 0;
        }
        final ArrayMap<String, PermissionState> entriesForProfile = entries.get(mUserManager
                .getUserHandle());
        if (entriesForProfile == null) {
            return 0;
        }
        return entriesForProfile.size();
    }

    public int getNumPackagesAllowedByAppOps() {
        SparseArray<ArrayMap<String, PermissionState>> entries = getEntries();
        if (entries == null) {
            return 0;
        }
        loadPermissionsStates(entries);
        loadAppOpsStates(entries);
        final ArrayMap<String, PermissionState> entriesForProfile = entries.get(mUserManager
                .getUserHandle());
        if (entriesForProfile == null) {
            return 0;
        }
        Collection<PermissionState> permStates = entriesForProfile.values();
        int result = 0;
        for (PermissionState permState : permStates) {
            if (permState.isPermissible()) {
                result++;
            }
        }
        return result;
    }

    public static class PermissionState {
        public final String packageName;
        public final UserHandle userHandle;
        public PackageInfo packageInfo;
        public boolean staticPermissionGranted;
        public boolean permissionDeclared;
        public int appOpMode;

        public PermissionState(String packageName, UserHandle userHandle) {
            this.packageName = packageName;
            this.appOpMode = AppOpsManager.MODE_DEFAULT;
            this.userHandle = userHandle;
        }

        public boolean isPermissible() {
            // defining the default behavior as permissible as long as the package requested this
            // permission (this means pre-M gets approval during install time; M apps gets approval
            // during runtime.
            if (appOpMode == AppOpsManager.MODE_DEFAULT) {
                return staticPermissionGranted;
            }
            return appOpMode == AppOpsManager.MODE_ALLOWED;
        }
    }
}
