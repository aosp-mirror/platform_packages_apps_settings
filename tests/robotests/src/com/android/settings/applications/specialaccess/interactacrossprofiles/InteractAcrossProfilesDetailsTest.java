/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.interactacrossprofiles;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.CrossProfileApps;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.UserInfo;
import android.os.UserManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class InteractAcrossProfilesDetailsTest {

    private static final int PERSONAL_PROFILE_ID = 0;
    private static final int WORK_PROFILE_ID = 10;
    private static final int PACKAGE_UID = 0;
    private static final String CROSS_PROFILE_PACKAGE_NAME = "crossProfilePackage";
    public static final String INTERACT_ACROSS_PROFILES_PERMISSION =
            "android.permission.INTERACT_ACROSS_PROFILES";

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final AppOpsManager mAppOpsManager = mContext.getSystemService(AppOpsManager.class);
    private final PackageManager mPackageManager = mContext.getPackageManager();
    private final UserManager mUserManager = mContext.getSystemService(UserManager.class);
    private final CrossProfileApps mCrossProfileApps = mContext.getSystemService(
            CrossProfileApps.class);

    @Test
    public void getPreferenceSummary_appOpAllowed_returnsAllowed() {
        shadowOf(mUserManager).addUser(
                PERSONAL_PROFILE_ID, "personal-profile"/* name */, 0/* flags */);
        shadowOf(mUserManager).addProfile(
                PERSONAL_PROFILE_ID, WORK_PROFILE_ID,
                "work-profile"/* profileName */, UserInfo.FLAG_MANAGED_PROFILE);
        shadowOf(mPackageManager).setInstalledPackagesForUserId(
                PERSONAL_PROFILE_ID, ImmutableList.of(CROSS_PROFILE_PACKAGE_NAME));
        shadowOf(mPackageManager).setInstalledPackagesForUserId(
                WORK_PROFILE_ID, ImmutableList.of(CROSS_PROFILE_PACKAGE_NAME));
        shadowOf(mCrossProfileApps).addCrossProfilePackage(
                CROSS_PROFILE_PACKAGE_NAME);
        String appOp = AppOpsManager.permissionToOp(INTERACT_ACROSS_PROFILES_PERMISSION);
        shadowOf(mAppOpsManager).setMode(
                appOp, PACKAGE_UID, CROSS_PROFILE_PACKAGE_NAME, AppOpsManager.MODE_ALLOWED);
        shadowOf(mPackageManager).addPermissionInfo(createCrossProfilesPermissionInfo());

        assertThat(InteractAcrossProfilesDetails.getPreferenceSummary(
                mContext, CROSS_PROFILE_PACKAGE_NAME))
                .isEqualTo(mContext.getString(R.string.interact_across_profiles_summary_allowed));
    }

    @Test
    public void getPreferenceSummary_appOpNotAllowed_returnsNotAllowed() {
        shadowOf(mUserManager).addUser(
                PERSONAL_PROFILE_ID, "personal-profile"/* name */, 0/* flags */);
        shadowOf(mUserManager).addProfile(
                PERSONAL_PROFILE_ID, WORK_PROFILE_ID,
                "work-profile"/* profileName */, UserInfo.FLAG_MANAGED_PROFILE);
        shadowOf(mPackageManager).setInstalledPackagesForUserId(
                PERSONAL_PROFILE_ID, ImmutableList.of(CROSS_PROFILE_PACKAGE_NAME));
        shadowOf(mPackageManager).setInstalledPackagesForUserId(
                WORK_PROFILE_ID, ImmutableList.of(CROSS_PROFILE_PACKAGE_NAME));
        shadowOf(mCrossProfileApps).addCrossProfilePackage(
                CROSS_PROFILE_PACKAGE_NAME);
        String appOp = AppOpsManager.permissionToOp(INTERACT_ACROSS_PROFILES_PERMISSION);
        shadowOf(mAppOpsManager).setMode(
                appOp, PACKAGE_UID, CROSS_PROFILE_PACKAGE_NAME, AppOpsManager.MODE_IGNORED);
        shadowOf(mPackageManager).addPermissionInfo(createCrossProfilesPermissionInfo());

        assertThat(InteractAcrossProfilesDetails.getPreferenceSummary(
                mContext, CROSS_PROFILE_PACKAGE_NAME))
                .isEqualTo(mContext.getString(
                        R.string.interact_across_profiles_summary_not_allowed));
    }

    @Test
    public void getPreferenceSummary_noWorkProfile_returnsNotAllowed() {
        shadowOf(mUserManager).addUser(
                PERSONAL_PROFILE_ID, "personal-profile"/* name */, 0/* flags */);
        shadowOf(mPackageManager).setInstalledPackagesForUserId(
                PERSONAL_PROFILE_ID, ImmutableList.of(CROSS_PROFILE_PACKAGE_NAME));

        assertThat(InteractAcrossProfilesDetails.getPreferenceSummary(
                mContext, CROSS_PROFILE_PACKAGE_NAME))
                .isEqualTo(mContext.getString(
                        R.string.interact_across_profiles_summary_not_allowed));
    }

    private PermissionInfo createCrossProfilesPermissionInfo() {
        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.name = INTERACT_ACROSS_PROFILES_PERMISSION;
        permissionInfo.protectionLevel = PermissionInfo.PROTECTION_FLAG_APPOP;
        return permissionInfo;
    }
}
