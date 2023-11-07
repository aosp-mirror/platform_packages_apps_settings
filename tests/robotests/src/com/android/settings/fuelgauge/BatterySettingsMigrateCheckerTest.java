/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserHandle;

import com.android.settings.TestUtils;
import com.android.settings.fuelgauge.batterysaver.BatterySaverScheduleRadioButtonsController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {BatterySettingsMigrateCheckerTest.ShadowUserHandle.class})
public final class BatterySettingsMigrateCheckerTest {

    private static final Intent BOOT_COMPLETED_INTENT = new Intent(Intent.ACTION_BOOT_COMPLETED);
    private static final int UID = 2003;
    private static final String PACKAGE_NAME = "com.android.test.app";

    private Context mContext;
    private BatterySettingsMigrateChecker mBatterySettingsMigrateChecker;

    @Mock private PackageManager mPackageManager;
    @Mock private BatteryOptimizeUtils mBatteryOptimizeUtils;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        BatteryUtils.getInstance(mContext).reset();
        doReturn(mContext).when(mContext).getApplicationContext();
        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(UID)
                .when(mPackageManager)
                .getPackageUid(PACKAGE_NAME, PackageManager.GET_META_DATA);
        BatterySettingsMigrateChecker.sBatteryOptimizeUtils = mBatteryOptimizeUtils;
        mBatterySettingsMigrateChecker = new BatterySettingsMigrateChecker();
    }

    @After
    public void resetShadows() {
        ShadowUserHandle.reset();
    }

    @Test
    public void onReceive_invalidScheduledLevel_resetScheduledValue() {
        final int invalidScheduledLevel = 5;
        setScheduledLevel(invalidScheduledLevel);

        mBatterySettingsMigrateChecker.onReceive(mContext, BOOT_COMPLETED_INTENT);

        assertThat(getScheduledLevel())
                .isEqualTo(BatterySaverScheduleRadioButtonsController.TRIGGER_LEVEL_MIN);
    }

    @Test
    public void onReceive_validScheduledLevel_notResetScheduledValue() {
        final int validScheduledLevel = 12;
        setScheduledLevel(validScheduledLevel);

        mBatterySettingsMigrateChecker.onReceive(mContext, BOOT_COMPLETED_INTENT);

        assertThat(getScheduledLevel()).isEqualTo(validScheduledLevel);
    }

    @Test
    public void onReceive_validSpecialScheduledLevel_notResetScheduledValue() {
        final int validScheduledLevel = 0;
        setScheduledLevel(validScheduledLevel);

        mBatterySettingsMigrateChecker.onReceive(mContext, BOOT_COMPLETED_INTENT);

        assertThat(getScheduledLevel()).isEqualTo(validScheduledLevel);
    }

    @Test
    public void onReceive_nullIntnt_noAction() {
        final int invalidScheduledLevel = 5;
        setScheduledLevel(invalidScheduledLevel);

        mBatterySettingsMigrateChecker.onReceive(mContext, null);

        assertThat(getScheduledLevel()).isEqualTo(invalidScheduledLevel);
    }

    @Test
    public void onReceive_invalidIntent_noAction() {
        final int invalidScheduledLevel = 5;
        setScheduledLevel(invalidScheduledLevel);

        mBatterySettingsMigrateChecker.onReceive(mContext, new Intent());

        assertThat(getScheduledLevel()).isEqualTo(invalidScheduledLevel);
    }

    @Test
    public void onReceive_nonOwner_noAction() {
        ShadowUserHandle.setUid(1);
        final int invalidScheduledLevel = 5;
        setScheduledLevel(invalidScheduledLevel);

        mBatterySettingsMigrateChecker.onReceive(mContext, BOOT_COMPLETED_INTENT);

        assertThat(getScheduledLevel()).isEqualTo(invalidScheduledLevel);
    }

    @Test
    public void verifyBatteryOptimizeModeApps_inAllowList_resetOptimizationMode() throws Exception {
        doReturn(BatteryOptimizeUtils.MODE_RESTRICTED)
                .when(mBatteryOptimizeUtils)
                .getAppOptimizationMode();

        mBatterySettingsMigrateChecker.verifyBatteryOptimizeModeApps(
                mContext, BatteryOptimizeUtils.MODE_OPTIMIZED, Arrays.asList(PACKAGE_NAME));

        final InOrder inOrder = inOrder(mBatteryOptimizeUtils);
        inOrder.verify(mBatteryOptimizeUtils).getAppOptimizationMode();
        inOrder.verify(mBatteryOptimizeUtils)
                .setAppUsageState(
                        BatteryOptimizeUtils.MODE_OPTIMIZED,
                        BatteryOptimizeHistoricalLogEntry.Action.FORCE_RESET);
    }

    @Test
    public void verifyBatteryOptimizeModeApps_optimizedMode_noAction() throws Exception {
        doReturn(BatteryOptimizeUtils.MODE_OPTIMIZED)
                .when(mBatteryOptimizeUtils)
                .getAppOptimizationMode();

        mBatterySettingsMigrateChecker.verifyBatteryOptimizeModeApps(
                mContext, BatteryOptimizeUtils.MODE_OPTIMIZED, Arrays.asList(PACKAGE_NAME));

        verify(mBatteryOptimizeUtils, never()).setAppUsageState(anyInt(), any());
    }

    @Test
    public void verifyBatteryOptimizeModeApps_notInAllowList_noAction() throws Exception {
        doReturn(BatteryOptimizeUtils.MODE_RESTRICTED)
                .when(mBatteryOptimizeUtils)
                .getAppOptimizationMode();

        mBatterySettingsMigrateChecker.verifyBatteryOptimizeModeApps(
                mContext, BatteryOptimizeUtils.MODE_OPTIMIZED, new ArrayList<String>());

        verifyNoInteractions(mBatteryOptimizeUtils);
    }

    private void setScheduledLevel(int scheduledLevel) {
        TestUtils.setScheduledLevel(mContext, scheduledLevel);
    }

    private int getScheduledLevel() {
        return TestUtils.getScheduledLevel(mContext);
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
