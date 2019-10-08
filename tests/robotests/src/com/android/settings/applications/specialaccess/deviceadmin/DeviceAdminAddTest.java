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

package com.android.settings.applications.specialaccess.deviceadmin;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AppOpsManager;
import android.app.admin.DeviceAdminInfo;
import android.app.settings.SettingsEnums;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DeviceAdminAddTest {
    private static final int UID = 12345;
    private static final String PACKAGE_NAME = "com.android.test.device.admin";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DeviceAdminInfo mDeviceAdmin;
    private BatteryUtils mBatteryUtils;
    private FakeFeatureFactory mFeatureFactory;
    private DeviceAdminAdd mDeviceAdminAdd;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mBatteryUtils = spy(BatteryUtils.getInstance(RuntimeEnvironment.application));
        doNothing().when(mBatteryUtils).setForceAppStandby(anyInt(), anyString(), anyInt());
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mDeviceAdminAdd = Robolectric.buildActivity(DeviceAdminAdd.class).get();

        doReturn(UID).when(mBatteryUtils).getPackageUid(PACKAGE_NAME);
        when(mDeviceAdmin.getComponent().getPackageName()).thenReturn(PACKAGE_NAME);
        mDeviceAdminAdd.mDeviceAdmin = mDeviceAdmin;
    }

    @Test
    public void logSpecialPermissionChange() {
        mDeviceAdminAdd.logSpecialPermissionChange(true, "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_ADMIN_ALLOW,
                SettingsEnums.PAGE_UNKNOWN,
                "app",
                0);

        mDeviceAdminAdd.logSpecialPermissionChange(false, "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_ADMIN_DENY,
                SettingsEnums.PAGE_UNKNOWN,
                "app",
                0);
    }

    @Test
    public void unrestrictAppIfPossible_appRestricted_unrestrictApp() {
        doReturn(true).when(mBatteryUtils).isForceAppStandbyEnabled(UID, PACKAGE_NAME);

        mDeviceAdminAdd.unrestrictAppIfPossible(mBatteryUtils);

        verify(mBatteryUtils).setForceAppStandby(UID, PACKAGE_NAME, AppOpsManager.MODE_ALLOWED);
    }

    @Test
    public void unrestrictAppIfPossible_appUnrestricted_doNothing() {
        doReturn(false).when(mBatteryUtils).isForceAppStandbyEnabled(UID, PACKAGE_NAME);

        mDeviceAdminAdd.unrestrictAppIfPossible(mBatteryUtils);

        verify(mBatteryUtils, never()).setForceAppStandby(UID, PACKAGE_NAME,
                AppOpsManager.MODE_ALLOWED);
    }
}
