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
 * limitations under the License.
 */

package com.android.settings.applications;

import static com.android.settings.testutils.ApplicationTestUtils.buildInfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Build;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowApplication;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public final class AppWithAdminGrantedPermissionsCounterTest {

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
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private IPackageManager mPackageManagerService;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;

    private int mAppCount = -1;
    private ApplicationInfo mApp1;
    private ApplicationInfo mApp2;
    private ApplicationInfo mApp3;
    private ApplicationInfo mApp4;
    private ApplicationInfo mApp5;
    private ApplicationInfo mApp6;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);

        mApp1 = buildInfo(APP_1_UID, APP_1, 0 /* flags */, Build.VERSION_CODES.M);
        mApp2 = buildInfo(APP_2_UID, APP_2, 0 /* flags */, Build.VERSION_CODES.M);
        mApp3 = buildInfo(APP_3_UID, APP_3, 0 /* flags */, Build.VERSION_CODES.LOLLIPOP);
        mApp4 = buildInfo(APP_4_UID, APP_4, 0 /* flags */, Build.VERSION_CODES.LOLLIPOP);
        mApp5 = buildInfo(APP_5_UID, APP_5, 0 /* flags */, Build.VERSION_CODES.LOLLIPOP);
        mApp6 = buildInfo(APP_6_UID, APP_6, 0 /* flags */, Build.VERSION_CODES.M);
    }

    private void verifyCountInstalledApps(boolean async) throws Exception {
        configureUserManager();
        configurePackageManager();
        configureRunTimePermissions();
        configureInstallTimePermissions();

        // Count the number of all apps installed that were granted on or more permissions by the
        // admin.
        if (async) {
            (new AppWithAdminGrantedPermissionsCounterTestable(PERMISSIONS)).execute();
            // Wait for the background task to finish.
            ShadowApplication.runBackgroundTasks();
        } else {
            (new AppWithAdminGrantedPermissionsCounterTestable(PERMISSIONS)).executeInForeground();
        }
        assertThat(mAppCount).isEqualTo(3);

        // Verify that installed packages were retrieved the current user and the user's managed
        // profile only.
        verify(mPackageManager).getInstalledApplicationsAsUser(anyInt(), eq(MAIN_USER_ID));
        verify(mPackageManager).getInstalledApplicationsAsUser(anyInt(),
                eq(MANAGED_PROFILE_ID));
        verify(mPackageManager, atLeast(0)).getInstallReason(anyObject(), anyObject());
        verifyNoMoreInteractions(mPackageManager);
    }

    @Test
    public void testIncludeInCount() throws Exception {
        configurePackageManager();
        configureRunTimePermissions();
        configureInstallTimePermissions();

        assertThat(AppWithAdminGrantedPermissionsCounter.includeInCount(PERMISSIONS,
                mDevicePolicyManager, mPackageManager, mPackageManagerService, mApp1)).isTrue();

        assertThat(AppWithAdminGrantedPermissionsCounter.includeInCount(PERMISSIONS,
                mDevicePolicyManager, mPackageManager, mPackageManagerService, mApp2)).isFalse();

        assertThat(AppWithAdminGrantedPermissionsCounter.includeInCount(PERMISSIONS,
                mDevicePolicyManager, mPackageManager, mPackageManagerService, mApp3)).isTrue();

        assertThat(AppWithAdminGrantedPermissionsCounter.includeInCount(PERMISSIONS,
                mDevicePolicyManager, mPackageManager, mPackageManagerService, mApp4)).isFalse();

        assertThat(AppWithAdminGrantedPermissionsCounter.includeInCount(PERMISSIONS,
                mDevicePolicyManager, mPackageManager, mPackageManagerService, mApp5)).isFalse();
    }

    @Test
    public void testCountInstalledAppsSync() throws Exception {
        verifyCountInstalledApps(false /* async */);
    }

    @Test
    public void testCountInstalledAppsAync() throws Exception {
        verifyCountInstalledApps(true /* async */);
    }

    private void configureInstallTimePermissions() throws RemoteException {
        when(mPackageManagerService.checkUidPermission(anyObject(), eq(APP_1_UID)))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        when(mPackageManagerService.checkUidPermission(anyObject(), eq(APP_2_UID)))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        when(mPackageManagerService.checkUidPermission(PERMISSION_1, APP_3_UID))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        when(mPackageManagerService.checkUidPermission(PERMISSION_2, APP_3_UID))
                .thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mPackageManagerService.checkUidPermission(PERMISSION_1, APP_4_UID))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        when(mPackageManagerService.checkUidPermission(PERMISSION_2, APP_4_UID))
                .thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mPackageManagerService.checkUidPermission(anyObject(), eq(APP_5_UID)))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        when(mPackageManagerService.checkUidPermission(anyObject(), eq(APP_6_UID)))
                .thenReturn(PackageManager.PERMISSION_DENIED);
    }

    private void configureRunTimePermissions() {
        when(mDevicePolicyManager.getPermissionGrantState(null, APP_1, PERMISSION_1))
                .thenReturn(DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        when(mDevicePolicyManager.getPermissionGrantState(null, APP_1, PERMISSION_2))
                .thenReturn(DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED);
        when(mDevicePolicyManager.getPermissionGrantState(eq(null), eq(APP_2), anyObject()))
                .thenReturn(DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED);
        when(mDevicePolicyManager.getPermissionGrantState(eq(null), eq(APP_3), anyObject()))
                .thenReturn(DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED);
        when(mDevicePolicyManager.getPermissionGrantState(eq(null), eq(APP_4), anyObject()))
                .thenReturn(DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED);
        when(mDevicePolicyManager.getPermissionGrantState(eq(null), eq(APP_5), anyObject()))
                .thenReturn(DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED);
        when(mDevicePolicyManager.getPermissionGrantState(eq(null), eq(APP_6), anyObject()))
                .thenReturn(DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
    }

    private void configurePackageManager() {
        // The first user has five apps installed:
        // * app1 uses run-time permissions. It has been granted one of the permissions by the
        //        admin. It should be counted.
        // * app2 uses run-time permissions. It has not been granted any of the permissions by the
        //        admin. It should not be counted.
        // * app3 uses install-time permissions. It was installed by the admin and requested one of
        //        the permissions. It should be counted.
        // * app4 uses install-time permissions. It was not installed by the admin but did request
        //        one of the permissions. It should not be counted.
        // * app5 uses install-time permissions. It was installed by the admin but did not request
        //        any of the permissions. It should not be counted.
        when(mPackageManager.getInstalledApplicationsAsUser(PackageManager.GET_DISABLED_COMPONENTS
                        | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS
                        | PackageManager.MATCH_ANY_USER,
                MAIN_USER_ID)).thenReturn(Arrays.asList(mApp1, mApp2, mApp3, mApp4, mApp5));
        // The second user has one app installed. This app uses run-time permissions. It has been
        // granted both permissions by the admin. It should be counted.
        when(mPackageManager.getInstalledApplicationsAsUser(PackageManager.GET_DISABLED_COMPONENTS
                        | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS,
                MANAGED_PROFILE_ID)).thenReturn(Collections.singletonList(mApp6));

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
        // app6 was not installed by enterprise policy.
        final UserHandle managedProfileUser = new UserHandle(MANAGED_PROFILE_ID);
        when(mPackageManager.getInstallReason(APP_6, managedProfileUser))
                .thenReturn(PackageManager.INSTALL_REASON_UNKNOWN);
    }

    private void configureUserManager() {
        // There are two users.
        when(mUserManager.getProfiles(UserHandle.myUserId())).thenReturn(Arrays.asList(
                new UserInfo(MAIN_USER_ID, "main", UserInfo.FLAG_ADMIN),
                new UserInfo(MANAGED_PROFILE_ID, "managed profile", 0)));
    }

    private class AppWithAdminGrantedPermissionsCounterTestable
            extends AppWithAdminGrantedPermissionsCounter {
        private AppWithAdminGrantedPermissionsCounterTestable(String[] permissions) {
            super(mContext, permissions, mPackageManager, mPackageManagerService,
                    mDevicePolicyManager);
        }

        @Override
        protected void onCountComplete(int num) {
            mAppCount = num;
        }
    }
}
