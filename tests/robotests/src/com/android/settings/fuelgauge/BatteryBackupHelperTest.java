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

import static com.android.settings.fuelgauge.BatteryBackupHelper.DELIMITER;
import static com.android.settings.fuelgauge.BatteryBackupHelper.DELIMITER_MODE;
import static com.android.settings.fuelgauge.BatteryOptimizeUtils.MODE_RESTRICTED;
import static com.android.settings.fuelgauge.BatteryOptimizeUtils.MODE_UNRESTRICTED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import android.app.AppOpsManager;
import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.os.Build;
import android.os.IDeviceIdleController;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArraySet;

import com.android.settings.TestUtils;
import com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.fuelgauge.PowerAllowlistBackend;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {BatteryBackupHelperTest.ShadowUserHandle.class})
public final class BatteryBackupHelperTest {
    private static final String PACKAGE_NAME1 = "com.android.testing.1";
    private static final String PACKAGE_NAME2 = "com.android.testing.2";
    private static final String PACKAGE_NAME3 = "com.android.testing.3";
    private static final int UID1 = 1;

    private Context mContext;
    private PrintWriter mPrintWriter;
    private StringWriter mStringWriter;
    private BatteryBackupHelper mBatteryBackupHelper;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;

    @Mock private PackageManager mPackageManager;
    @Mock private BackupDataOutput mBackupDataOutput;
    @Mock private BackupDataInputStream mBackupDataInputStream;
    @Mock private IDeviceIdleController mDeviceController;
    @Mock private IPackageManager mIPackageManager;
    @Mock private AppOpsManager mAppOpsManager;
    @Mock private UserManager mUserManager;
    @Mock private PowerAllowlistBackend mPowerAllowlistBackend;
    @Mock private BatteryOptimizeUtils mBatteryOptimizeUtils;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mPowerUsageFeatureProvider = FakeFeatureFactory.setupForTest().powerUsageFeatureProvider;
        mContext = spy(RuntimeEnvironment.application);
        mStringWriter = new StringWriter();
        mPrintWriter = new PrintWriter(mStringWriter);
        BatteryUtils.getInstance(mContext).reset();
        doReturn(mContext).when(mContext).getApplicationContext();
        doReturn(mAppOpsManager).when(mContext).getSystemService(AppOpsManager.class);
        doReturn(mUserManager).when(mContext).getSystemService(UserManager.class);
        doReturn(mPackageManager).when(mContext).getPackageManager();
        mBatteryBackupHelper = new BatteryBackupHelper(mContext);
        mBatteryBackupHelper.mIDeviceIdleController = mDeviceController;
        mBatteryBackupHelper.mIPackageManager = mIPackageManager;
        mBatteryBackupHelper.mPowerAllowlistBackend = mPowerAllowlistBackend;
        mBatteryBackupHelper.mBatteryOptimizeUtils = mBatteryOptimizeUtils;
        mockUid(1001 /*fake uid*/, PACKAGE_NAME1);
        mockUid(1002 /*fake uid*/, PACKAGE_NAME2);
        mockUid(BatteryUtils.UID_NULL, PACKAGE_NAME3);
    }

    @After
    public void resetShadows() {
        ShadowUserHandle.reset();
        BatteryBackupHelper.getSharedPreferences(mContext).edit().clear().apply();
    }

    @Test
    public void performBackup_emptyPowerList_backupPowerList() throws Exception {
        doReturn(new String[0]).when(mDeviceController).getFullPowerWhitelist();
        mBatteryBackupHelper.performBackup(null, mBackupDataOutput, null);

        verify(mBackupDataOutput, atLeastOnce()).writeEntityHeader(anyString(), anyInt());
    }

    @Test
    public void performBackup_remoteException_notBackupPowerList() throws Exception {
        doThrow(new RemoteException()).when(mDeviceController).getFullPowerWhitelist();
        mBatteryBackupHelper.performBackup(null, mBackupDataOutput, null);

        verify(mBackupDataOutput, never()).writeEntityHeader(anyString(), anyInt());
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
                new UserInfo(/* userId= */ 0, /* userName= */ "google", /* flag= */ 0);
        doReturn(Arrays.asList(userInfo)).when(mUserManager).getProfiles(anyInt());
        doThrow(new RuntimeException())
                .when(mIPackageManager)
                .getInstalledApplications(anyLong(), anyInt());

        mBatteryBackupHelper.backupOptimizationMode(mBackupDataOutput, null);

        verify(mBackupDataOutput, never()).writeEntityHeader(anyString(), anyInt());
    }

    @Test
    public void backupOptimizationMode_backupOptimizationMode() throws Exception {
        final List<String> allowlistedApps = Arrays.asList(PACKAGE_NAME1);
        createTestingData(PACKAGE_NAME1, UID1, PACKAGE_NAME2, PACKAGE_NAME3);

        mBatteryBackupHelper.backupOptimizationMode(mBackupDataOutput, allowlistedApps);

        // 2 for UNRESTRICTED mode and 1 for RESTRICTED mode.
        final String expectedResult = PACKAGE_NAME1 + ":2," + PACKAGE_NAME2 + ":1,";
        verifyBackupData(expectedResult);
        verifyDumpHistoryData("com.android.testing.1\taction:BACKUP\tevent:mode: 2");
        verifyDumpHistoryData("com.android.testing.2\taction:BACKUP\tevent:mode: 1");
    }

    @Test
    public void backupOptimizationMode_backupOptimizationModeAndIgnoreSystemApp() throws Exception {
        final List<String> allowlistedApps = Arrays.asList(PACKAGE_NAME1);
        createTestingData(PACKAGE_NAME1, UID1, PACKAGE_NAME2, PACKAGE_NAME3);
        // Sets "com.android.testing.1" as system app.
        doReturn(true).when(mPowerAllowlistBackend).isSysAllowlisted(PACKAGE_NAME1);
        doReturn(false).when(mPowerAllowlistBackend).isDefaultActiveApp(anyString(), anyInt());

        mBatteryBackupHelper.backupOptimizationMode(mBackupDataOutput, allowlistedApps);

        // "com.android.testing.2" for RESTRICTED mode.
        final String expectedResult = PACKAGE_NAME2 + ":1,";
        verifyBackupData(expectedResult);
        verifyDumpHistoryData("com.android.testing.2\taction:BACKUP\tevent:mode: 1");
    }

    @Test
    public void backupOptimizationMode_backupOptimizationModeAndIgnoreDefaultApp()
            throws Exception {
        final List<String> allowlistedApps = Arrays.asList(PACKAGE_NAME1);
        createTestingData(PACKAGE_NAME1, UID1, PACKAGE_NAME2, PACKAGE_NAME3);
        // Sets "com.android.testing.1" as device default app.
        doReturn(true).when(mPowerAllowlistBackend).isDefaultActiveApp(PACKAGE_NAME1, UID1);
        doReturn(false).when(mPowerAllowlistBackend).isSysAllowlisted(anyString());

        mBatteryBackupHelper.backupOptimizationMode(mBackupDataOutput, allowlistedApps);

        // "com.android.testing.2" for RESTRICTED mode.
        final String expectedResult = PACKAGE_NAME2 + ":1,";
        verifyBackupData(expectedResult);
        verifyDumpHistoryData("com.android.testing.2\taction:BACKUP\tevent:mode: 1");
    }

    @Test
    public void restoreEntity_nonOwner_notReadBackupData() throws Exception {
        ShadowUserHandle.setUid(1);
        mockBackupData(30 /*dataSize*/, BatteryBackupHelper.KEY_OPTIMIZATION_LIST);

        mBatteryBackupHelper.restoreEntity(mBackupDataInputStream);

        verifyNoInteractions(mBackupDataInputStream);
    }

    @Test
    public void restoreEntity_zeroDataSize_notReadBackupData() throws Exception {
        final int zeroDataSize = 0;
        mockBackupData(zeroDataSize, BatteryBackupHelper.KEY_OPTIMIZATION_LIST);

        mBatteryBackupHelper.restoreEntity(mBackupDataInputStream);

        verify(mBackupDataInputStream, never()).read(any(), anyInt(), anyInt());
    }

    @Test
    public void restoreEntity_incorrectDataKey_notReadBackupData() throws Exception {
        final String incorrectDataKey = "incorrect_data_key";
        mockBackupData(30 /*dataSize*/, incorrectDataKey);

        mBatteryBackupHelper.restoreEntity(mBackupDataInputStream);

        verify(mBackupDataInputStream, never()).read(any(), anyInt(), anyInt());
    }

    @Test
    public void restoreEntity_readExpectedDataFromBackupData() throws Exception {
        final int dataSize = 30;
        mockBackupData(dataSize, BatteryBackupHelper.KEY_OPTIMIZATION_LIST);

        mBatteryBackupHelper.restoreEntity(mBackupDataInputStream);

        final ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(mBackupDataInputStream).read(captor.capture(), eq(0), eq(dataSize));
        assertThat(captor.getValue().length).isEqualTo(dataSize);
    }

    @Test
    public void restoreEntity_verifyConfiguration() {
        final int invalidScheduledLevel = 5;
        TestUtils.setScheduledLevel(mContext, invalidScheduledLevel);

        mBatteryBackupHelper.restoreEntity(mBackupDataInputStream);

        assertThat(TestUtils.getScheduledLevel(mContext)).isNotEqualTo(invalidScheduledLevel);
    }

    @Test
    public void restoreEntity_verifyConfigurationOneTimeOnly() {
        final int invalidScheduledLevel = 5;
        TestUtils.setScheduledLevel(mContext, invalidScheduledLevel);
        mBatteryBackupHelper.restoreEntity(mBackupDataInputStream);
        TestUtils.setScheduledLevel(mContext, invalidScheduledLevel);

        // Invoke the restoreEntity() method 2nd time.
        mBatteryBackupHelper.restoreEntity(mBackupDataInputStream);

        assertThat(TestUtils.getScheduledLevel(mContext)).isEqualTo(invalidScheduledLevel);
    }

    @Test
    public void restoreOptimizationMode_nullBytesData_skipRestore() throws Exception {
        mBatteryBackupHelper.restoreOptimizationMode(new byte[0]);
        verifyNoInteractions(mBatteryOptimizeUtils);

        mBatteryBackupHelper.restoreOptimizationMode("invalid data format".getBytes());
        verifyNoInteractions(mBatteryOptimizeUtils);

        mBatteryBackupHelper.restoreOptimizationMode(DELIMITER.getBytes());
        verifyNoInteractions(mBatteryOptimizeUtils);
    }

    @Test
    public void restoreOptimizationMode_invalidModeFormat_skipRestore() throws Exception {
        final String invalidNumberFormat = "google";
        final String package1Mode = PACKAGE_NAME1
                + DELIMITER_MODE
                + MODE_RESTRICTED
                + DELIMITER;
        final String package2Mode = PACKAGE_NAME2
                + DELIMITER_MODE
                + invalidNumberFormat;
        final String packageModes = package1Mode + package2Mode;

        mBatteryBackupHelper.restoreOptimizationMode(packageModes.getBytes());
        TimeUnit.SECONDS.sleep(1);

        final InOrder inOrder = inOrder(mBatteryOptimizeUtils);
        inOrder.verify(mBatteryOptimizeUtils).setAppUsageState(MODE_RESTRICTED, Action.RESTORE);
        inOrder.verify(mBatteryOptimizeUtils, never())
                .setAppUsageState(anyInt(), eq(Action.RESTORE));
    }

    @Test
    public void restoreOptimizationMode_restoreExpectedModes() throws Exception {
        final String package1Mode = PACKAGE_NAME1
                + DELIMITER_MODE
                + MODE_RESTRICTED
                + DELIMITER;
        final String package2Mode = PACKAGE_NAME2
                + DELIMITER_MODE
                + MODE_UNRESTRICTED
                + DELIMITER;
        final String package3Mode = PACKAGE_NAME3
                + DELIMITER_MODE
                + MODE_RESTRICTED
                + DELIMITER;
        final String packageModes = package1Mode + package2Mode + package3Mode;

        mBatteryBackupHelper.restoreOptimizationMode(packageModes.getBytes());
        TimeUnit.SECONDS.sleep(1);

        final InOrder inOrder = inOrder(mBatteryOptimizeUtils);
        inOrder.verify(mBatteryOptimizeUtils).setAppUsageState(MODE_RESTRICTED, Action.RESTORE);
        inOrder.verify(mBatteryOptimizeUtils).setAppUsageState(MODE_UNRESTRICTED, Action.RESTORE);
        inOrder.verify(mBatteryOptimizeUtils, never())
                .setAppUsageState(MODE_RESTRICTED, Action.RESTORE);
    }

    @Test
    public void performBackup_backupDeviceBuildInformation() throws Exception {
        final String[] fullPowerList = {"com.android.package"};
        doReturn(fullPowerList).when(mDeviceController).getFullPowerWhitelist();
        doReturn(null).when(mPowerUsageFeatureProvider).getBuildMetadata1(mContext);
        final String deviceMetadata = "device.metadata.test_device";
        doReturn(deviceMetadata).when(mPowerUsageFeatureProvider).getBuildMetadata2(mContext);

        mBatteryBackupHelper.performBackup(null, mBackupDataOutput, null);

        final InOrder inOrder = inOrder(mBackupDataOutput);
        verifyBackupData(inOrder, BatteryBackupHelper.KEY_BUILD_BRAND, Build.BRAND);
        verifyBackupData(inOrder, BatteryBackupHelper.KEY_BUILD_PRODUCT, Build.PRODUCT);
        verifyBackupData(inOrder, BatteryBackupHelper.KEY_BUILD_MANUFACTURER, Build.MANUFACTURER);
        verifyBackupData(inOrder, BatteryBackupHelper.KEY_BUILD_FINGERPRINT, Build.FINGERPRINT);
        inOrder.verify(mBackupDataOutput, never())
                .writeEntityHeader(eq(BatteryBackupHelper.KEY_BUILD_METADATA_1), anyInt());
        verifyBackupData(inOrder, BatteryBackupHelper.KEY_BUILD_METADATA_2, deviceMetadata);
    }

    private void mockUid(int uid, String packageName) throws Exception {
        doReturn(uid)
                .when(mPackageManager)
                .getPackageUid(packageName, PackageManager.GET_META_DATA);
    }

    private void mockBackupData(int dataSize, String dataKey) {
        doReturn(dataSize).when(mBackupDataInputStream).size();
        doReturn(dataKey).when(mBackupDataInputStream).getKey();
    }

    private void verifyDumpHistoryData(String expectedResult) {
        BatteryBackupHelper.dumpHistoricalData(mContext, mPrintWriter);
        assertThat(mStringWriter.toString().contains(expectedResult)).isTrue();
    }

    private void verifyBackupData(String expectedResult) throws Exception {
        final byte[] expectedBytes = expectedResult.getBytes();
        final ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        final Set<String> expectedResultSet =
                Set.of(expectedResult.split(BatteryBackupHelper.DELIMITER));

        verify(mBackupDataOutput)
                .writeEntityHeader(BatteryBackupHelper.KEY_OPTIMIZATION_LIST, expectedBytes.length);
        verify(mBackupDataOutput).writeEntityData(captor.capture(), eq(expectedBytes.length));
        final String actualResult = new String(captor.getValue());
        final Set<String> actualResultSet =
                Set.of(actualResult.split(BatteryBackupHelper.DELIMITER));
        assertThat(actualResultSet).isEqualTo(expectedResultSet);
    }

    private void createTestingData(
            String packageName1, int uid1, String packageName2, String packageName3)
            throws Exception {
        // Sets the getInstalledApplications() method for testing.
        final UserInfo userInfo =
                new UserInfo(/* userId= */ 0, /* userName= */ "google", /* flag= */ 0);
        doReturn(Arrays.asList(userInfo)).when(mUserManager).getProfiles(anyInt());
        final ApplicationInfo applicationInfo1 = new ApplicationInfo();
        applicationInfo1.enabled = true;
        applicationInfo1.uid = uid1;
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
        doReturn(
                        new ParceledListSlice<ApplicationInfo>(
                                Arrays.asList(
                                        applicationInfo1, applicationInfo2, applicationInfo3)))
                .when(mIPackageManager)
                .getInstalledApplications(anyLong(), anyInt());
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
        mBatteryBackupHelper.mTestApplicationInfoList =
                new ArraySet<>(Arrays.asList(applicationInfo1, applicationInfo2, applicationInfo3));
    }

    private void verifyBackupData(InOrder inOrder, String dataKey, String dataContent)
            throws Exception {
        final byte[] expectedBytes = dataContent.getBytes();
        inOrder.verify(mBackupDataOutput).writeEntityHeader(dataKey, expectedBytes.length);
        inOrder.verify(mBackupDataOutput).writeEntityData(expectedBytes, expectedBytes.length);
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
