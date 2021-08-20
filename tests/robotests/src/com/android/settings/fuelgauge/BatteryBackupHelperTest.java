/*
 * Copyright (C) 2021 The Android Open Source Project
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
 *
 */

package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AppOpsManager;
import android.app.backup.BackupDataOutput;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.os.IDeviceIdleController;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {BatteryBackupHelperTest.ShadowUserHandle.class})
public final class BatteryBackupHelperTest {

    private Context mContext;
    private BatteryBackupHelper mBatteryBackupHelper;

    @Mock
    private BackupDataOutput mBackupDataOutput;
    @Mock
    private IDeviceIdleController mDeviceController;
    @Mock
    private IPackageManager mIPackageManager;
    @Mock
    private AppOpsManager mAppOpsManager;
    @Mock
    private UserManager mUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mContext).when(mContext).getApplicationContext();
        doReturn(mAppOpsManager).when(mContext).getSystemService(AppOpsManager.class);
        doReturn(mUserManager).when(mContext).getSystemService(UserManager.class);
        mBatteryBackupHelper = new BatteryBackupHelper(mContext);
        mBatteryBackupHelper.mIDeviceIdleController = mDeviceController;
        mBatteryBackupHelper.mIPackageManager = mIPackageManager;
    }

    @After
    public void resetShadows() {
        ShadowUserHandle.reset();
    }

    @Test
    public void performBackup_nullPowerList_notBackupPowerList() throws Exception {
        doReturn(null).when(mDeviceController).getFullPowerWhitelist();
        mBatteryBackupHelper.performBackup(null, mBackupDataOutput, null);

        verify(mBackupDataOutput, never()).writeEntityHeader(anyString(), anyInt());
    }

    @Test
    public void performBackup_emptyPowerList_notBackupPowerList() throws Exception {
        doReturn(new String[0]).when(mDeviceController).getFullPowerWhitelist();
        mBatteryBackupHelper.performBackup(null, mBackupDataOutput, null);

        verify(mBackupDataOutput, never()).writeEntityHeader(anyString(), anyInt());
    }

    @Test
    public void performBackup_remoteException_notBackupPowerList() throws Exception {
        doThrow(new RemoteException()).when(mDeviceController).getFullPowerWhitelist();
        mBatteryBackupHelper.performBackup(null, mBackupDataOutput, null);

        verify(mBackupDataOutput, never()).writeEntityHeader(anyString(), anyInt());
    }

    @Test
    public void performBackup_oneFullPowerListElement_backupFullPowerListData()
            throws Exception {
        final String[] fullPowerList = {"com.android.package"};
        doReturn(fullPowerList).when(mDeviceController).getFullPowerWhitelist();

        mBatteryBackupHelper.performBackup(null, mBackupDataOutput, null);

        final byte[] expectedBytes = fullPowerList[0].getBytes();
        verify(mBackupDataOutput).writeEntityHeader(
                BatteryBackupHelper.KEY_FULL_POWER_LIST, expectedBytes.length);
        verify(mBackupDataOutput).writeEntityData(expectedBytes, expectedBytes.length);
    }

    @Test
    public void performBackup_backupFullPowerListData() throws Exception {
        final String[] fullPowerList = {"com.android.package1", "com.android.package2"};
        doReturn(fullPowerList).when(mDeviceController).getFullPowerWhitelist();

        mBatteryBackupHelper.performBackup(null, mBackupDataOutput, null);

        final String expectedResult = fullPowerList[0]
                + BatteryBackupHelper.DELIMITER + fullPowerList[1];
        final byte[] expectedBytes = expectedResult.getBytes();
        verify(mBackupDataOutput).writeEntityHeader(
                BatteryBackupHelper.KEY_FULL_POWER_LIST, expectedBytes.length);
        verify(mBackupDataOutput).writeEntityData(expectedBytes, expectedBytes.length);
    }

    @Test
    public void performBackup_nonOwner_ignoreAllBackupAction() throws Exception {
        ShadowUserHandle.setUid(1);
        final String[] fullPowerList = {"com.android.package"};
        doReturn(fullPowerList).when(mDeviceController).getFullPowerWhitelist();

        mBatteryBackupHelper.performBackup(null, mBackupDataOutput, null);

        verify(mBackupDataOutput, never()).writeEntityHeader(anyString(), anyInt());
    }

    @Test
    public void backupOptimizationMode_nullInstalledApps_ignoreBackupOptimization()
            throws Exception {
        final UserInfo userInfo =
                new UserInfo(/*userId=*/ 0, /*userName=*/ "google", /*flag=*/ 0);
        doReturn(Arrays.asList(userInfo)).when(mUserManager).getProfiles(anyInt());
        doThrow(new RuntimeException())
                .when(mIPackageManager)
                .getInstalledApplications(anyInt(), anyInt());

        mBatteryBackupHelper.backupOptimizationMode(mBackupDataOutput, null);

        verify(mBackupDataOutput, never()).writeEntityHeader(anyString(), anyInt());
    }

    @Test
    public void backupOptimizationMode_backupOptimizationMode() throws Exception {
        final String packageName1 = "com.android.testing.1";
        final String packageName2 = "com.android.testing.2";
        final String packageName3 = "com.android.testing.3";
        final List<String> allowlistedApps = Arrays.asList(packageName1);
        createTestingData(packageName1, packageName2, packageName3);

        mBatteryBackupHelper.backupOptimizationMode(mBackupDataOutput, allowlistedApps);

        final String expectedResult =
                packageName1 + "|UNRESTRICTED," + packageName2 + "|RESTRICTED,";
        final byte[] expectedBytes = expectedResult.getBytes();
        verify(mBackupDataOutput).writeEntityHeader(
                BatteryBackupHelper.KEY_OPTIMIZATION_LIST, expectedBytes.length);
        verify(mBackupDataOutput).writeEntityData(expectedBytes, expectedBytes.length);
    }

    private void createTestingData(
            String packageName1, String packageName2, String packageName3) throws Exception {
        // Sets the getInstalledApplications() method for testing.
        final UserInfo userInfo =
                new UserInfo(/*userId=*/ 0, /*userName=*/ "google", /*flag=*/ 0);
        doReturn(Arrays.asList(userInfo)).when(mUserManager).getProfiles(anyInt());
        final ApplicationInfo applicationInfo1 = new ApplicationInfo();
        applicationInfo1.enabled = true;
        applicationInfo1.uid = 1;
        applicationInfo1.packageName = packageName1;
        final ApplicationInfo applicationInfo2 = new ApplicationInfo();
        applicationInfo2.enabled = false;
        applicationInfo2.uid = 2;
        applicationInfo2.packageName = packageName2;
        applicationInfo2.enabledSetting = PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER;
        final ApplicationInfo applicationInfo3 = new ApplicationInfo();
        applicationInfo3.enabled = false;
        applicationInfo3.uid = 3;
        applicationInfo3.packageName = packageName3;
        applicationInfo3.enabledSetting = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        doReturn(new ParceledListSlice<ApplicationInfo>(
                Arrays.asList(applicationInfo1, applicationInfo2, applicationInfo3)))
            .when(mIPackageManager)
            .getInstalledApplications(anyInt(), anyInt());
        // Sets the AppOpsManager for checkOpNoThrow() method.
        doReturn(AppOpsManager.MODE_ALLOWED)
                .when(mAppOpsManager)
                .checkOpNoThrow(
                        AppOpsManager.OP_RUN_ANY_IN_BACKGROUND,
                        applicationInfo1.uid,
                        applicationInfo1.packageName);
        doReturn(AppOpsManager.MODE_IGNORED)
                .when(mAppOpsManager)
                .checkOpNoThrow(
                        AppOpsManager.OP_RUN_ANY_IN_BACKGROUND,
                        applicationInfo2.uid,
                        applicationInfo2.packageName);
    }

    @Implements(UserHandle.class)
    public static class ShadowUserHandle {
        // Sets the default as thte OWNER role.
        private static int sUid = 0;

        public static void setUid(int uid) {
            sUid = uid;
        }

        @Implementation
        public static int myUserId() {
            return sUid;
        }

        @Resetter
        public static void reset() {
            sUid = 0;
        }
    }
}
