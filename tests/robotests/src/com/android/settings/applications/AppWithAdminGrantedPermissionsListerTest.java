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
 * limitations under the License
 */

package com.android.settings.applications;

import static com.android.settings.testutils.ApplicationTestUtils.buildInfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowApplication;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public final class AppWithAdminGrantedPermissionsListerTest {

    private final String APP_1 = "app1";
    private final String APP_2 = "app2";
    private final String APP_3 = "app3";
    private final String APP_4 = "app4";
    private final String APP_5 = "app5";
    private final String APP_6 = "app6";

    private final int MAIN_USER_ID = 0;
    private final int MANAGED_PROFILE_ID = 10;

    private final int PER_USER_UID_RANGE = 100000;
    private final int APP_1_UID = MAIN_USER_ID * PER_USER_UID_RANGE + 1;
    private final int APP_2_UID = MAIN_USER_ID * PER_USER_UID_RANGE + 2;
    private final int APP_3_UID = MAIN_USER_ID * PER_USER_UID_RANGE + 3;
    private final int APP_4_UID = MAIN_USER_ID * PER_USER_UID_RANGE + 4;
    private final int APP_5_UID = MAIN_USER_ID * PER_USER_UID_RANGE + 5;
    private final int APP_6_UID = MANAGED_PROFILE_ID * PER_USER_UID_RANGE + 1;

    private final String PERMISSION_1 = "some.permission.1";
    private final String PERMISSION_2 = "some.permission.2";
    private final String[] PERMISSIONS = {PERMISSION_1, PERMISSION_2};

    @Mock
    private UserManager mUserManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private IPackageManager mPackageManagerService;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;

    private List<UserAppInfo> mAppList = Collections.emptyList();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void verifyListInstalledApps() throws Exception {
        // There are two users.
        when(mUserManager.getProfiles(UserHandle.myUserId())).thenReturn(Arrays.asList(
                new UserInfo(MAIN_USER_ID, "main", UserInfo.FLAG_ADMIN),
                new UserInfo(MANAGED_PROFILE_ID, "managed profile", 0)));

        // The first user has five apps installed:
        // * app1 uses run-time permissions. It has been granted one of the permissions by the
        //        admin. It should be listed.
        // * app2 uses run-time permissions. It has not been granted any of the permissions by the
        //        admin. It should not be listed.
        // * app3 uses install-time permissions. It was installed by the admin and requested one of
        //        the permissions. It should be listed.
        // * app4 uses install-time permissions. It was not installed by the admin but did request
        //        one of the permissions. It should not be listed.
        // * app5 uses install-time permissions. It was installed by the admin but did not request
        //        any of the permissions. It should not be listed.
        when(mPackageManager.getInstalledApplicationsAsUser(PackageManager.GET_DISABLED_COMPONENTS
                        | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS
                        | PackageManager.MATCH_ANY_USER,
                MAIN_USER_ID)).thenReturn(Arrays.asList(
                buildInfo(APP_1_UID, APP_1, 0 /* flags */, Build.VERSION_CODES.M),
                buildInfo(APP_2_UID, APP_2, 0 /* flags */, Build.VERSION_CODES.M),
                buildInfo(APP_3_UID, APP_3, 0 /* flags */, Build.VERSION_CODES.LOLLIPOP),
                buildInfo(APP_4_UID, APP_4, 0 /* flags */, Build.VERSION_CODES.LOLLIPOP),
                buildInfo(APP_5_UID, APP_5, 0 /* flags */, Build.VERSION_CODES.LOLLIPOP)));

        // Grant run-time permissions as appropriate.
        when(mDevicePolicyManager.getPermissionGrantState(null, APP_1, PERMISSION_1))
                .thenReturn(DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        when(mDevicePolicyManager.getPermissionGrantState(null, APP_1, PERMISSION_2))
                .thenReturn(DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED);
        when(mDevicePolicyManager.getPermissionGrantState(eq(null), eq(APP_2), any()))
                .thenReturn(DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED);
        when(mDevicePolicyManager.getPermissionGrantState(eq(null), eq(APP_3), any()))
                .thenReturn(DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED);
        when(mDevicePolicyManager.getPermissionGrantState(eq(null), eq(APP_4), any()))
                .thenReturn(DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED);
        when(mDevicePolicyManager.getPermissionGrantState(eq(null), eq(APP_5), any()))
                .thenReturn(DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED);

        // Grant install-time permissions as appropriate.
        when(mPackageManagerService.checkUidPermission(any(), eq(APP_1_UID)))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        when(mPackageManagerService.checkUidPermission(any(), eq(APP_2_UID)))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        when(mPackageManagerService.checkUidPermission(PERMISSION_1, APP_3_UID))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        when(mPackageManagerService.checkUidPermission(PERMISSION_2, APP_3_UID))
                .thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mPackageManagerService.checkUidPermission(PERMISSION_1, APP_4_UID))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        when(mPackageManagerService.checkUidPermission(PERMISSION_2, APP_4_UID))
                .thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mPackageManagerService.checkUidPermission(any(), eq(APP_5_UID)))
                .thenReturn(PackageManager.PERMISSION_DENIED);

        // app3 and app5 were installed by enterprise policy.
        final UserHandle mainUser = new UserHandle(MAIN_USER_ID);
        when(mPackageManager.getInstallReason(APP_1, mainUser))
                .thenReturn(PackageManager.INSTALL_REASON_UNKNOWN);
        when(mPackageManager.getInstallReason(APP_2, mainUser))
                .thenReturn(PackageManager.INSTALL_REASON_UNKNOWN);
        when(mPackageManager.getInstallReason(APP_3, mainUser))
                .thenReturn(PackageManager.INSTALL_REASON_POLICY);
        when(mPackageManager.getInstallReason(APP_4, mainUser))
                .thenReturn(PackageManager.INSTALL_REASON_UNKNOWN);
        when(mPackageManager.getInstallReason(APP_5, mainUser))
                .thenReturn(PackageManager.INSTALL_REASON_POLICY);

        // The second user has one app installed. This app uses run-time permissions. It has been
        // granted both permissions by the admin. It should be listed.
        when(mPackageManager.getInstalledApplicationsAsUser(PackageManager.GET_DISABLED_COMPONENTS
                        | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS,
                MANAGED_PROFILE_ID)).thenReturn(Arrays.asList(
                buildInfo(APP_6_UID, APP_6, 0 /* flags */, Build.VERSION_CODES.M)));

        // Grant run-time permissions as appropriate.
        when(mDevicePolicyManager.getPermissionGrantState(eq(null), eq(APP_6), any()))
                .thenReturn(DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);

        // Grant install-time permissions as appropriate.
        when(mPackageManagerService.checkUidPermission(any(), eq(APP_6_UID)))
                .thenReturn(PackageManager.PERMISSION_DENIED);

        // app6 was not installed by enterprise policy.
        final UserHandle managedProfileUser = new UserHandle(MANAGED_PROFILE_ID);
        when(mPackageManager.getInstallReason(APP_6, managedProfileUser))
                .thenReturn(PackageManager.INSTALL_REASON_UNKNOWN);

        // List all apps installed that were granted one or more permissions by the
        // admin.
        (new AppWithAdminGrantedPermissionsListerTestable(PERMISSIONS)).execute();

        // Wait for the background task to finish.
        ShadowApplication.runBackgroundTasks();
        assertThat(mAppList.size()).isEqualTo(3);
        InstalledAppListerTest.verifyListUniqueness(mAppList);

        assertThat(InstalledAppListerTest.checkAppFound(mAppList, APP_1, MAIN_USER_ID)).isTrue();
        assertThat(InstalledAppListerTest.checkAppFound(mAppList, APP_2, MAIN_USER_ID)).isFalse();
        assertThat(InstalledAppListerTest.checkAppFound(mAppList, APP_3, MAIN_USER_ID)).isTrue();
        assertThat(InstalledAppListerTest.checkAppFound(mAppList, APP_4, MAIN_USER_ID)).isFalse();
        assertThat(InstalledAppListerTest.checkAppFound(mAppList, APP_5, MAIN_USER_ID)).isFalse();
        assertThat(InstalledAppListerTest.checkAppFound(mAppList, APP_6, MANAGED_PROFILE_ID)).
                isTrue();

        // Verify that installed packages were retrieved the current user and the user's managed
        // profile only.
        verify(mPackageManager).getInstalledApplicationsAsUser(anyInt(), eq(MAIN_USER_ID));
        verify(mPackageManager).getInstalledApplicationsAsUser(anyInt(),
                eq(MANAGED_PROFILE_ID));
        verify(mPackageManager, atLeast(0)).getInstallReason(any(), any());
        verifyNoMoreInteractions(mPackageManager);
    }

    private class AppWithAdminGrantedPermissionsListerTestable extends
            AppWithAdminGrantedPermissionsLister {

        private AppWithAdminGrantedPermissionsListerTestable(String[] permissions) {
            super(permissions, mPackageManager, mPackageManagerService,
                    mDevicePolicyManager, mUserManager);
        }

        @Override
        protected void onAppListBuilt(List<UserAppInfo> list) {
            mAppList = list;
        }
    }
}
