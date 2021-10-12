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

import static com.android.settings.fuelgauge.BatteryOptimizeUtils.AppUsageState.OPTIMIZED;
import static com.android.settings.fuelgauge.BatteryOptimizeUtils.AppUsageState.RESTRICTED;
import static com.android.settings.fuelgauge.BatteryOptimizeUtils.AppUsageState.UNRESTRICTED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AppOpsManager;
import android.content.Context;

import com.android.settingslib.fuelgauge.PowerAllowlistBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BatteryOptimizeUtilsTest {

    private static final int UID = 12345;
    private static final String PACKAGE_NAME = "com.android.app";

    @Mock BatteryUtils mockBatteryUtils;
    @Mock AppOpsManager mockAppOpsManager;
    @Mock PowerAllowlistBackend mockBackend;

    private Context mContext;
    private BatteryOptimizeUtils mBatteryOptimizeUtils;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mBatteryOptimizeUtils = spy(new BatteryOptimizeUtils(mContext, UID, PACKAGE_NAME));
        mBatteryOptimizeUtils.mAppOpsManager = mockAppOpsManager;
        mBatteryOptimizeUtils.mBatteryUtils = mockBatteryUtils;
        mBatteryOptimizeUtils.mPowerAllowListBackend = mockBackend;
    }

    @Test
    public void testGetAppUsageState_returnRestricted() {
        when(mockBackend.isAllowlisted(anyString())).thenReturn(false);
        when(mockAppOpsManager.checkOpNoThrow(anyInt(), anyInt(), anyString()))
                .thenReturn(AppOpsManager.MODE_IGNORED);

        assertThat(mBatteryOptimizeUtils.getAppUsageState()).isEqualTo(RESTRICTED);
    }

    @Test
    public void testGetAppUsageState_returnUnrestricted() {
        when(mockBackend.isAllowlisted(anyString())).thenReturn(true);
        when(mockAppOpsManager.checkOpNoThrow(anyInt(), anyInt(), anyString()))
                .thenReturn(AppOpsManager.MODE_ALLOWED);

        assertThat(mBatteryOptimizeUtils.getAppUsageState()).isEqualTo(UNRESTRICTED);
    }

    @Test
    public void testGetAppUsageState_returnOptimized() {
        when(mockBackend.isAllowlisted(anyString())).thenReturn(false);
        when(mockAppOpsManager.checkOpNoThrow(anyInt(), anyInt(), anyString()))
                .thenReturn(AppOpsManager.MODE_ALLOWED);

        assertThat(mBatteryOptimizeUtils.getAppUsageState()).isEqualTo(OPTIMIZED);
    }

    @Test
    public void testIsSystemOrDefaultApp_isSystemOrDefaultApp_returnTrue() {
        when(mockBackend.isAllowlisted(anyString())).thenReturn(true);
        when(mockBackend.isDefaultActiveApp(anyString())).thenReturn(true);

        assertThat(mBatteryOptimizeUtils.isSystemOrDefaultApp()).isTrue();
    }

    @Test
    public void testIsSystemOrDefaultApp_notSystemOrDefaultApp_returnFalse() {
        assertThat(mBatteryOptimizeUtils.isSystemOrDefaultApp()).isFalse();
    }

    @Test
    public void testIsValidPackageName_InvalidPackageName_returnFalse() {
        final BatteryOptimizeUtils testBatteryOptimizeUtils =
                new BatteryOptimizeUtils(mContext, UID, null);

        assertThat(testBatteryOptimizeUtils.isValidPackageName()).isFalse();
    }

    @Test
    public void testIsValidPackageName_validPackageName_returnTrue() {
        assertThat(mBatteryOptimizeUtils.isValidPackageName()).isTrue();
    }

    @Test
    public void testSetAppUsageState_Restricted_verifyAction() {
        mBatteryOptimizeUtils.setAppUsageState(RESTRICTED);

        verify(mockBatteryUtils).setForceAppStandby(UID,
                PACKAGE_NAME, AppOpsManager.MODE_IGNORED);
        verify(mockBackend).removeApp(PACKAGE_NAME);
    }

    @Test
    public void  testSetAppUsageState_Unrestricted_verifyAction() {
        mBatteryOptimizeUtils.setAppUsageState(UNRESTRICTED);

        verify(mockBatteryUtils).setForceAppStandby(UID,
                PACKAGE_NAME, AppOpsManager.MODE_ALLOWED);
        verify(mockBackend).addApp(PACKAGE_NAME);
    }

    @Test
    public void  testSetAppUsageState_Optimized_verifyAction() {
        mBatteryOptimizeUtils.setAppUsageState(OPTIMIZED);

        verify(mockBatteryUtils).setForceAppStandby(UID,
                PACKAGE_NAME, AppOpsManager.MODE_ALLOWED);
        verify(mockBackend).removeApp(PACKAGE_NAME);
    }
}
