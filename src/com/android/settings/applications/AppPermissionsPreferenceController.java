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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.List;
import java.util.Set;

public class AppPermissionsPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String TAG = "AppPermissionPrefCtrl";
    private static final String KEY_APP_PERMISSION_GROUPS = "manage_perms";
    private static final String[] PERMISSION_GROUPS = new String[] {
        "android.permission-group.LOCATION",
        "android.permission-group.MICROPHONE",
        "android.permission-group.CAMERA",
        "android.permission-group.SMS",
        "android.permission-group.CONTACTS",
        "android.permission-group.PHONE"};

    private static final int NUM_PERMISSION_TO_USE = 3;

    private final PackageManager mPackageManager;

    public AppPermissionsPreferenceController(Context context) {
        super(context);
        mPackageManager = context.getPackageManager();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_APP_PERMISSION_GROUPS;
    }

    @Override
    public void updateState(Preference preference) {
        final String summary = getSummary();
        if (summary != null) {
            preference.setSummary(summary);
        }
    }

    /*
       Summary text looks like: Apps using Permission1, Permission2, Permission3
       The 3 permissions are the first three from the list which any app has granted:
       Location, Microphone, Camera, Sms, Contacts, and Phone
     */
    private String getSummary() {
        final Set<String> permissions = getAllPermissionsInGroups();
        Set<String> grantedPermissionGroups = getGrantedPermissionGroups(permissions);
        CharSequence summary = null;
        int count = 0;
        for (String group : PERMISSION_GROUPS) {
            if (!grantedPermissionGroups.contains(group)) {
                continue;
            }
            summary = concatSummaryText(summary, group);
            if (++count >= NUM_PERMISSION_TO_USE) {
                break;
            }
        }
        return count > 0 ? mContext.getString(R.string.app_permissions_summary, summary) : null;
    }

    private Set<String> getGrantedPermissionGroups(Set<String> permissions) {
        ArraySet<String> grantedPermissionGroups = new ArraySet<>();
        List<PackageInfo> installedPackages =
            mPackageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        for (PackageInfo installedPackage : installedPackages) {
            if (installedPackage.permissions == null) {
                continue;
            }
            for (PermissionInfo permissionInfo : installedPackage.permissions) {
                if (permissions.contains(permissionInfo.name)
                        && !grantedPermissionGroups.contains(permissionInfo.group)) {
                    grantedPermissionGroups.add(permissionInfo.group);
                }
            }
        }
        return grantedPermissionGroups;
    }

    private CharSequence concatSummaryText(CharSequence currentSummary, String permission) {
        final CharSequence label = getPermissionGroupLabel(permission);
        if (TextUtils.isEmpty(currentSummary)) {
            return label;
        }
        return mContext.getString(R.string.join_many_items_middle, currentSummary, label);
    }

    private CharSequence getPermissionGroupLabel(String group) {
        try {
            final PermissionGroupInfo groupInfo = mPackageManager.getPermissionGroupInfo(group, 0);
            return groupInfo.loadLabel(mPackageManager);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Error getting permissions label.", e);
        }
        return group;
    }

    private Set<String> getAllPermissionsInGroups() {
        ArraySet<String> result = new ArraySet<>();
        for (String group : PERMISSION_GROUPS) {
            try {
                final List<PermissionInfo> permissions =
                    mPackageManager.queryPermissionsByGroup(group, 0);
                for (PermissionInfo permissionInfo : permissions) {
                    result.add(permissionInfo.name);
                }
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Error getting permissions in group "+group, e);
            }
        }
        return result;
    }
}
