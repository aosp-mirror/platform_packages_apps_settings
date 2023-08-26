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

import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.PermissionChecker;
import android.content.pm.ApplicationInfo;
import android.content.pm.CrossProfileApps;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowApplicationPackageManager;
import com.android.settings.testutils.shadow.ShadowCrossProfileApps;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.testutils.shadow.ShadowPermissionChecker;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowProcess;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowUserManager.class,
        ShadowApplicationPackageManager.class,
        ShadowCrossProfileApps.class,
})
public class InteractAcrossProfilesSettingsTest {

    private static final int PERSONAL_PROFILE_ID = 0;
    private static final int WORK_PROFILE_ID = 10;
    private static final int WORK_UID = UserHandle.PER_USER_RANGE * WORK_PROFILE_ID;

    private static final String PERSONAL_CROSS_PROFILE_PACKAGE = "personalCrossProfilePackage";
    private static final String PERSONAL_NON_CROSS_PROFILE_PACKAGE =
            "personalNonCrossProfilePackage";
    private static final String WORK_CROSS_PROFILE_PACKAGE = "workCrossProfilePackage";
    private static final String WORK_NON_CROSS_PROFILE_PACKAGE =
            "workNonCrossProfilePackage";
    private static final List<String> PERSONAL_PROFILE_INSTALLED_PACKAGES =
            ImmutableList.of(PERSONAL_CROSS_PROFILE_PACKAGE, PERSONAL_NON_CROSS_PROFILE_PACKAGE);
    private static final List<String> WORK_PROFILE_INSTALLED_PACKAGES =
            ImmutableList.of(WORK_CROSS_PROFILE_PACKAGE, WORK_NON_CROSS_PROFILE_PACKAGE);
    public static final String INTERACT_ACROSS_PROFILES_PERMISSION =
            "android.permission.INTERACT_ACROSS_PROFILES";

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final PackageManager mPackageManager = mContext.getPackageManager();
    private final UserManager mUserManager = mContext.getSystemService(UserManager.class);
    private final CrossProfileApps mCrossProfileApps =
            mContext.getSystemService(CrossProfileApps.class);

    private ShadowApplicationPackageManager mShadowPackageManager;
    private ShadowUserManager mShadowUserManager;
    private ShadowCrossProfileApps mShadowCrossProfileApps;

    @Before
    public void setUp() {
        mShadowUserManager = (ShadowUserManager) shadowOf(
                mContext.getSystemService(UserManager.class)
        );
        mShadowCrossProfileApps = (ShadowCrossProfileApps) shadowOf(
                mContext.getSystemService(CrossProfileApps.class)
        );
        mShadowPackageManager = (ShadowApplicationPackageManager) shadowOf(
                mContext.getPackageManager()
        );
    }

    @Test
    public void collectConfigurableApps_fromPersonal_returnsCombinedPackages() {
        mShadowUserManager.addUser(
                PERSONAL_PROFILE_ID, "personal-profile"/* name */, 0/* flags */);
        mShadowUserManager.addProfile(
                PERSONAL_PROFILE_ID, WORK_PROFILE_ID,
                "work-profile"/* profileName */, UserInfo.FLAG_MANAGED_PROFILE);
        mShadowPackageManager.setInstalledPackagesForUserId(
                PERSONAL_PROFILE_ID, PERSONAL_PROFILE_INSTALLED_PACKAGES);
        mShadowPackageManager.setInstalledPackagesForUserId(
                WORK_PROFILE_ID, WORK_PROFILE_INSTALLED_PACKAGES);
        installCrossProfilePackage(PERSONAL_CROSS_PROFILE_PACKAGE);
        installCrossProfilePackage(WORK_CROSS_PROFILE_PACKAGE);

        List<Pair<ApplicationInfo, UserHandle>> apps =
                InteractAcrossProfilesSettings.collectConfigurableApps(
                        mPackageManager, mUserManager, mCrossProfileApps);

        assertThat(apps.size()).isEqualTo(2);
        assertTrue(apps.stream().anyMatch(
                app -> app.first.packageName.equals(PERSONAL_CROSS_PROFILE_PACKAGE)));
        assertTrue(apps.stream().anyMatch(
                app -> app.first.packageName.equals(WORK_CROSS_PROFILE_PACKAGE)));
    }

    @Test
    public void collectConfigurableApps_fromWork_returnsCombinedPackages() {
        mShadowUserManager.addUser(
                PERSONAL_PROFILE_ID, "personal-profile"/* name */, 0/* flags */);
        mShadowUserManager.addProfile(
                PERSONAL_PROFILE_ID, WORK_PROFILE_ID,
                "work-profile"/* profileName */, UserInfo.FLAG_MANAGED_PROFILE);
        ShadowProcess.setUid(WORK_UID);
        mShadowPackageManager.setInstalledPackagesForUserId(
                PERSONAL_PROFILE_ID, PERSONAL_PROFILE_INSTALLED_PACKAGES);
        mShadowPackageManager.setInstalledPackagesForUserId(
                WORK_PROFILE_ID, WORK_PROFILE_INSTALLED_PACKAGES);
        installCrossProfilePackage(PERSONAL_CROSS_PROFILE_PACKAGE);
        installCrossProfilePackage(WORK_CROSS_PROFILE_PACKAGE);

        List<Pair<ApplicationInfo, UserHandle>> apps =
                InteractAcrossProfilesSettings.collectConfigurableApps(
                        mPackageManager, mUserManager, mCrossProfileApps);

        assertThat(apps.size()).isEqualTo(2);
        assertTrue(apps.stream().anyMatch(
                app -> app.first.packageName.equals(PERSONAL_CROSS_PROFILE_PACKAGE)));
        assertTrue(apps.stream().anyMatch(
                app -> app.first.packageName.equals(WORK_CROSS_PROFILE_PACKAGE)));
    }

    @Test
    public void collectConfigurableApps_onlyOneProfile_returnsEmpty() {
        mShadowUserManager.addUser(
                PERSONAL_PROFILE_ID, "personal-profile"/* name */, 0/* flags */);
        mShadowPackageManager.setInstalledPackagesForUserId(
                PERSONAL_PROFILE_ID, PERSONAL_PROFILE_INSTALLED_PACKAGES);
        installCrossProfilePackage(PERSONAL_CROSS_PROFILE_PACKAGE);

        List<Pair<ApplicationInfo, UserHandle>> apps =
                InteractAcrossProfilesSettings.collectConfigurableApps(
                        mPackageManager, mUserManager, mCrossProfileApps);

        assertThat(apps).isEmpty();
    }

    @Test
    public void getNumberOfEnabledApps_returnsNumberOfEnabledApps() {
        mShadowUserManager.addUser(
                PERSONAL_PROFILE_ID, "personal-profile"/* name */, 0/* flags */);
        mShadowUserManager.addProfile(
                PERSONAL_PROFILE_ID, WORK_PROFILE_ID,
                "work-profile"/* profileName */, UserInfo.FLAG_MANAGED_PROFILE);
        mShadowPackageManager.setInstalledPackagesForUserId(
                PERSONAL_PROFILE_ID, PERSONAL_PROFILE_INSTALLED_PACKAGES);
        mShadowPackageManager.setInstalledPackagesForUserId(
                WORK_PROFILE_ID, WORK_PROFILE_INSTALLED_PACKAGES);
        installCrossProfilePackage(PERSONAL_CROSS_PROFILE_PACKAGE);
        installCrossProfilePackage(WORK_CROSS_PROFILE_PACKAGE);
        mShadowCrossProfileApps.addCrossProfilePackage(PERSONAL_CROSS_PROFILE_PACKAGE);
        ShadowPermissionChecker.setResult(
                PERSONAL_CROSS_PROFILE_PACKAGE,
                INTERACT_ACROSS_PROFILES_PERMISSION,
                PermissionChecker.PERMISSION_GRANTED);
        ShadowPermissionChecker.setResult(
                PERSONAL_NON_CROSS_PROFILE_PACKAGE,
                INTERACT_ACROSS_PROFILES_PERMISSION,
                PermissionChecker.PERMISSION_SOFT_DENIED);

        int numOfApps = InteractAcrossProfilesSettings.getNumberOfEnabledApps(
                mContext, mPackageManager, mUserManager, mCrossProfileApps);

        assertThat(numOfApps).isEqualTo(1);
    }

    private void installCrossProfilePackage(String packageName) {
        PackageInfo personalPackageInfo = mShadowPackageManager.getInternalMutablePackageInfo(
                packageName);
        personalPackageInfo.requestedPermissions = new String[]{
                INTERACT_ACROSS_PROFILES_PERMISSION};
    }
}
