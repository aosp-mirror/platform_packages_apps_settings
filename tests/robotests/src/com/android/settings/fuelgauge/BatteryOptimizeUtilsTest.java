/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import static com.android.settings.fuelgauge.BatteryOptimizeUtils.MODE_OPTIMIZED;
import static com.android.settings.fuelgauge.BatteryOptimizeUtils.MODE_RESTRICTED;
import static com.android.settings.fuelgauge.BatteryOptimizeUtils.MODE_UNRESTRICTED;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.os.UserManager;
import android.util.ArraySet;

import com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action;
import com.android.settingslib.fuelgauge.PowerAllowlistBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class BatteryOptimizeUtilsTest {

    private static final int UID = 12345;
    private static final String PACKAGE_NAME = "com.android.app";

    @Mock private BatteryUtils mMockBatteryUtils;
    @Mock private AppOpsManager mMockAppOpsManager;
    @Mock private PowerAllowlistBackend mMockBackend;
    @Mock private IPackageManager mMockIPackageManager;
    @Mock private UserManager mMockUserManager;

    private Context mContext;
    private BatteryOptimizeUtils mBatteryOptimizeUtils;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mBatteryOptimizeUtils = spy(new BatteryOptimizeUtils(mContext, UID, PACKAGE_NAME));
        mBatteryOptimizeUtils.mAppOpsManager = mMockAppOpsManager;
        mBatteryOptimizeUtils.mBatteryUtils = mMockBatteryUtils;
        mBatteryOptimizeUtils.mPowerAllowListBackend = mMockBackend;
        // Sets the default mode as MODE_RESTRICTED.
        mBatteryOptimizeUtils.mMode = AppOpsManager.MODE_IGNORED;
        mBatteryOptimizeUtils.mAllowListed = false;
        doReturn(mMockUserManager).when(mContext).getSystemService(UserManager.class);
    }

    @Test
    public void testGetAppOptimizationMode_returnRestricted() {
        when(mMockBackend.isAllowlisted(anyString(), anyInt())).thenReturn(false);
        when(mMockAppOpsManager.checkOpNoThrow(anyInt(), anyInt(), anyString()))
                .thenReturn(AppOpsManager.MODE_IGNORED);

        assertThat(mBatteryOptimizeUtils.getAppOptimizationMode()).isEqualTo(MODE_RESTRICTED);
    }

    @Test
    public void testGetAppOptimizationMode_returnUnrestricted() {
        when(mMockBackend.isAllowlisted(anyString(), anyInt())).thenReturn(true);
        when(mMockAppOpsManager.checkOpNoThrow(anyInt(), anyInt(), anyString()))
                .thenReturn(AppOpsManager.MODE_ALLOWED);

        assertThat(mBatteryOptimizeUtils.getAppOptimizationMode()).isEqualTo(MODE_UNRESTRICTED);
    }

    @Test
    public void testGetAppOptimizationMode_returnOptimized() {
        when(mMockBackend.isAllowlisted(anyString(), anyInt())).thenReturn(false);
        when(mMockAppOpsManager.checkOpNoThrow(anyInt(), anyInt(), anyString()))
                .thenReturn(AppOpsManager.MODE_ALLOWED);

        assertThat(mBatteryOptimizeUtils.getAppOptimizationMode()).isEqualTo(MODE_OPTIMIZED);
    }

    @Test
    public void testIsSystemOrDefaultApp_isSystemOrDefaultApp_returnTrue() {
        when(mMockBackend.isAllowlisted(anyString(), anyInt())).thenReturn(true);
        when(mMockBackend.isDefaultActiveApp(anyString(), anyInt())).thenReturn(true);

        assertThat(mBatteryOptimizeUtils.isSystemOrDefaultApp()).isTrue();
    }

    @Test
    public void testIsSystemOrDefaultApp_notSystemOrDefaultApp_returnFalse() {
        assertThat(mBatteryOptimizeUtils.isSystemOrDefaultApp()).isFalse();
    }

    @Test
    public void isDisabledForOptimizeModeOnly_invalidPackageName_returnTrue() {
        final BatteryOptimizeUtils testBatteryOptimizeUtils =
                new BatteryOptimizeUtils(mContext, UID, null);

        assertThat(testBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()).isTrue();
    }

    @Test
    public void isDisabledForOptimizeModeOnly_validPackageName_returnFalse() {
        assertThat(mBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()).isFalse();
    }

    @Test
    public void testSetAppUsageState_Restricted_verifyAction() throws Exception {
        // Sets the current mode as MODE_UNRESTRICTED.
        when(mMockBackend.isAllowlisted(anyString(), anyInt())).thenReturn(true);
        when(mMockAppOpsManager.checkOpNoThrow(anyInt(), anyInt(), anyString()))
                .thenReturn(AppOpsManager.MODE_ALLOWED);

        mBatteryOptimizeUtils.setAppUsageState(MODE_RESTRICTED, Action.UNKNOWN);
        TimeUnit.SECONDS.sleep(1);

        verifySetAppOptimizationMode(AppOpsManager.MODE_IGNORED, /* allowListed */ false);
    }

    @Test
    public void testSetAppUsageState_Unrestricted_verifyAction() throws Exception {
        // Sets the current mode as MODE_RESTRICTED.
        when(mMockBackend.isAllowlisted(anyString(), anyInt())).thenReturn(false);
        when(mMockAppOpsManager.checkOpNoThrow(anyInt(), anyInt(), anyString()))
                .thenReturn(AppOpsManager.MODE_IGNORED);

        mBatteryOptimizeUtils.setAppUsageState(MODE_UNRESTRICTED, Action.UNKNOWN);
        TimeUnit.SECONDS.sleep(1);

        verifySetAppOptimizationMode(AppOpsManager.MODE_ALLOWED, /* allowListed */ true);
    }

    @Test
    public void testSetAppUsageState_Optimized_verifyAction() throws Exception {
        // Sets the current mode as MODE_UNRESTRICTED.
        when(mMockBackend.isAllowlisted(anyString(), anyInt())).thenReturn(true);
        when(mMockAppOpsManager.checkOpNoThrow(anyInt(), anyInt(), anyString()))
                .thenReturn(AppOpsManager.MODE_ALLOWED);

        mBatteryOptimizeUtils.setAppUsageState(MODE_OPTIMIZED, Action.UNKNOWN);
        TimeUnit.SECONDS.sleep(1);

        verifySetAppOptimizationMode(AppOpsManager.MODE_ALLOWED, /* allowListed */ false);
    }

    @Test
    public void testSetAppUsageState_sameUnrestrictedMode_verifyNoAction() throws Exception {
        // Sets the current mode as MODE_UNRESTRICTED.
        when(mMockBackend.isAllowlisted(anyString(), anyInt())).thenReturn(true);
        when(mMockAppOpsManager.checkOpNoThrow(anyInt(), anyInt(), anyString()))
                .thenReturn(AppOpsManager.MODE_ALLOWED);

        mBatteryOptimizeUtils.setAppUsageState(MODE_UNRESTRICTED, Action.UNKNOWN);
        TimeUnit.SECONDS.sleep(1);

        verify(mMockBatteryUtils, never()).setForceAppStandby(anyInt(), anyString(), anyInt());
        verify(mMockBackend, never()).addApp(anyString());
        verify(mMockBackend, never()).removeApp(anyString());
    }

    @Test
    public void testGetInstalledApplications_returnEmptyArray() {
        assertTrue(
                BatteryOptimizeUtils.getInstalledApplications(mContext, mMockIPackageManager)
                        .isEmpty());
    }

    @Test
    public void testGetInstalledApplications_returnNull() throws Exception {
        final UserInfo userInfo =
                new UserInfo(/* userId= */ 0, /* userName= */ "google", /* flag= */ 0);
        doReturn(Arrays.asList(userInfo)).when(mMockUserManager).getProfiles(anyInt());
        doThrow(new RuntimeException())
                .when(mMockIPackageManager)
                .getInstalledApplications(anyLong(), anyInt());

        assertNull(BatteryOptimizeUtils.getInstalledApplications(mContext, mMockIPackageManager));
    }

    @Test
    public void testGetInstalledApplications_returnInstalledApps() throws Exception {
        final UserInfo userInfo =
                new UserInfo(/* userId= */ 0, /* userName= */ "google", /* flag= */ 0);
        doReturn(Arrays.asList(userInfo)).when(mMockUserManager).getProfiles(anyInt());

        final ApplicationInfo applicationInfo1 = new ApplicationInfo();
        applicationInfo1.enabled = true;
        applicationInfo1.uid = 1;
        final ApplicationInfo applicationInfo2 = new ApplicationInfo();
        applicationInfo2.enabled = false;
        applicationInfo2.uid = 2;
        applicationInfo2.enabledSetting = PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER;
        final ApplicationInfo applicationInfo3 = new ApplicationInfo();
        applicationInfo3.enabled = false;
        applicationInfo3.uid = 3;
        applicationInfo3.enabledSetting = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        final ApplicationInfo applicationInfo4 = new ApplicationInfo();
        applicationInfo4.enabled = true;
        applicationInfo4.uid = 4;
        applicationInfo4.enabledSetting = PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER;
        doReturn(
                        new ParceledListSlice<ApplicationInfo>(
                                Arrays.asList(
                                        applicationInfo1,
                                        applicationInfo2,
                                        applicationInfo3,
                                        applicationInfo4)))
                .when(mMockIPackageManager)
                .getInstalledApplications(anyLong(), anyInt());

        final ArraySet<ApplicationInfo> applications =
                BatteryOptimizeUtils.getInstalledApplications(mContext, mMockIPackageManager);
        assertThat(applications.size()).isEqualTo(3);
        // applicationInfo3 should be filtered.
        assertTrue(applications.contains(applicationInfo1));
        assertTrue(applications.contains(applicationInfo2));
        assertFalse(applications.contains(applicationInfo3));
        assertTrue(applications.contains(applicationInfo4));
    }

    @Test
    public void testResetAppOptimizationMode_Optimized_verifyAction() throws Exception {
        runTestForResetWithMode(
                AppOpsManager.MODE_ALLOWED, /* allowListed */
                false,
                /* isSystemOrDefaultApp */ false);

        verifyNoInteractions(mMockBatteryUtils);

        final InOrder inOrder = inOrder(mMockBackend);
        inOrder.verify(mMockBackend).refreshList();
        inOrder.verify(mMockBackend).isAllowlisted(PACKAGE_NAME, UID);
        verifyNoMoreInteractions(mMockBackend);
    }

    @Test
    public void testResetAppOptimizationMode_SystemOrDefault_verifyAction() throws Exception {
        runTestForResetWithMode(
                AppOpsManager.MODE_ALLOWED, /* allowListed */
                true,
                /* isSystemOrDefaultApp */ true);

        verifyNoInteractions(mMockBatteryUtils);

        final InOrder inOrder = inOrder(mMockBackend);
        inOrder.verify(mMockBackend).refreshList();
        inOrder.verify(mMockBackend).isAllowlisted(PACKAGE_NAME, UID);
        inOrder.verify(mMockBackend).isSysAllowlisted(PACKAGE_NAME);
        verifyNoMoreInteractions(mMockBackend);
    }

    @Test
    public void testResetAppOptimizationMode_Restricted_verifyAction() throws Exception {
        runTestForResetWithMode(
                AppOpsManager.MODE_IGNORED, /* allowListed */
                false,
                /* isSystemOrDefaultApp */ false);

        verifySetAppOptimizationMode(AppOpsManager.MODE_ALLOWED, /* allowListed */ false);
    }

    @Test
    public void testResetAppOptimizationMode_Unrestricted_verifyAction() throws Exception {
        runTestForResetWithMode(
                AppOpsManager.MODE_ALLOWED, /* allowListed */
                true,
                /* isSystemOrDefaultApp */ false);

        verifySetAppOptimizationMode(AppOpsManager.MODE_ALLOWED, /* allowListed */ false);
    }

    private void runTestForResetWithMode(
            int appStandbyMode, boolean allowListed, boolean isSystemOrDefaultApp)
            throws Exception {
        final UserInfo userInfo =
                new UserInfo(/* userId= */ 0, /* userName= */ "google", /* flag= */ 0);
        doReturn(Arrays.asList(userInfo)).when(mMockUserManager).getProfiles(anyInt());
        final ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.uid = UID;
        applicationInfo.packageName = PACKAGE_NAME;
        applicationInfo.enabled = true;
        doReturn(new ParceledListSlice<ApplicationInfo>(Arrays.asList(applicationInfo)))
                .when(mMockIPackageManager)
                .getInstalledApplications(anyLong(), anyInt());

        doReturn(appStandbyMode)
                .when(mMockAppOpsManager)
                .checkOpNoThrow(anyInt(), anyInt(), anyString());
        doReturn(allowListed).when(mMockBackend).isAllowlisted(anyString(), anyInt());
        doReturn(isSystemOrDefaultApp).when(mMockBackend).isSysAllowlisted(anyString());
        doReturn(isSystemOrDefaultApp).when(mMockBackend).isDefaultActiveApp(anyString(), anyInt());

        BatteryOptimizeUtils.resetAppOptimizationMode(
                mContext,
                mMockIPackageManager,
                mMockAppOpsManager,
                mMockBackend,
                mMockBatteryUtils);
        TimeUnit.SECONDS.sleep(1);
    }

    private void verifySetAppOptimizationMode(int appStandbyMode, boolean allowListed) {
        verify(mMockBatteryUtils).setForceAppStandby(UID, PACKAGE_NAME, appStandbyMode);
        if (allowListed) {
            verify(mMockBackend).addApp(PACKAGE_NAME);
        } else {
            verify(mMockBackend).removeApp(PACKAGE_NAME);
        }
    }
}
