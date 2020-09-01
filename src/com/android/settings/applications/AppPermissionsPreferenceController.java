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
import android.icu.text.ListFormatter;
import android.util.ArraySet;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.applications.PermissionsSummaryHelper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AppPermissionsPreferenceController extends BasePreferenceController {

    private static final String TAG = "AppPermissionPrefCtrl";
    private static final int NUM_PACKAGE_TO_CHECK = 4;

    @VisibleForTesting
    static int NUM_PERMISSIONS_TO_SHOW = 3;

    private final PackageManager mPackageManager;
    private final Set<CharSequence> mPermissionGroups;

    private final PermissionsSummaryHelper.PermissionsResultCallback mPermissionsCallback =
            new PermissionsSummaryHelper.PermissionsResultCallback() {
                @Override
                public void onPermissionSummaryResult(int standardGrantedPermissionCount,
                        int requestedPermissionCount, int additionalGrantedPermissionCount,
                        List<CharSequence> grantedGroupLabels) {
                    updateSummary(grantedGroupLabels);
                }
            };

    @VisibleForTesting
    int mNumPackageChecked;

    private Preference mPreference;

    public AppPermissionsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mPackageManager = context.getPackageManager();
        mPermissionGroups = new ArraySet<>();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        mPreference = preference;
        mNumPackageChecked = 0;
        queryPermissionSummary();
    }

    @VisibleForTesting
    void queryPermissionSummary() {
        final List<PackageInfo> installedPackages =
                mPackageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        // Here we only get the first four apps and check their permissions.
        final List<PackageInfo> packagesWithPermission = installedPackages.stream()
                .filter(pInfo -> pInfo.permissions != null)
                .limit(NUM_PACKAGE_TO_CHECK)
                .collect(Collectors.toList());

        for (PackageInfo installedPackage : packagesWithPermission) {
            PermissionsSummaryHelper.getPermissionSummary(mContext,
                    installedPackage.packageName, mPermissionsCallback);
        }
    }

    @VisibleForTesting
    void updateSummary(List<CharSequence> grantedGroupLabels) {
        mPermissionGroups.addAll(grantedGroupLabels);
        mNumPackageChecked++;

        if (mNumPackageChecked < NUM_PACKAGE_TO_CHECK) {
            return;
        }

        final List<CharSequence> permissionsToShow = mPermissionGroups.stream()
                .limit(NUM_PERMISSIONS_TO_SHOW)
                .collect(Collectors.toList());
        final boolean isMoreShowed = mPermissionGroups.size() > NUM_PERMISSIONS_TO_SHOW;
        CharSequence summary;

        if (!permissionsToShow.isEmpty()) {
            if (isMoreShowed) {
                summary = mContext.getString(R.string.app_permissions_summary_more,
                        ListFormatter.getInstance().format(permissionsToShow).toLowerCase());
            } else {
                summary = mContext.getString(R.string.app_permissions_summary,
                        ListFormatter.getInstance().format(permissionsToShow).toLowerCase());
            }
        } else {
            summary = mContext.getString(
                    R.string.runtime_permissions_summary_no_permissions_granted);
        }
        mPreference.setSummary(summary);
    }
}