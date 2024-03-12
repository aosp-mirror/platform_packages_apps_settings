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

package com.android.settings.fuelgauge.batterytip.detectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.DockDefenderTip;
import com.android.settings.testutils.BatteryTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class DockDefenderDetectorTest {

    private BatteryInfo mBatteryInfo;
    private DockDefenderDetector mDockDefenderDetector;
    private Context mContext;
    private FakeFeatureFactory mFakeFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mBatteryInfo = new BatteryInfo();
        mBatteryInfo.pluggedStatus = BatteryManager.BATTERY_PLUGGED_DOCK;
        mDockDefenderDetector = new DockDefenderDetector(mBatteryInfo, mContext);
        Intent intent =
                BatteryTestUtils.getCustomBatteryIntent(
                        BatteryManager.BATTERY_PLUGGED_DOCK,
                        50 /* level */,
                        100 /* scale */,
                        BatteryManager.BATTERY_STATUS_CHARGING);
        doReturn(intent)
                .when(mContext)
                .registerReceiver(eq(null), refEq(new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));

        Settings.Global.putInt(
                mContext.getContentResolver(),
                BatteryUtils.SETTINGS_GLOBAL_DOCK_DEFENDER_BYPASS,
                0);
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
    }

    @Test
    public void testDetect_dockDefenderTemporarilyBypassed() {
        Settings.Global.putInt(
                mContext.getContentResolver(),
                BatteryUtils.SETTINGS_GLOBAL_DOCK_DEFENDER_BYPASS,
                1);

        BatteryTip batteryTip = mDockDefenderDetector.detect();

        assertTrue(batteryTip instanceof DockDefenderTip);
        assertEquals(
                ((DockDefenderTip) batteryTip).getMode(),
                BatteryUtils.DockDefenderMode.TEMPORARILY_BYPASSED);
    }

    @Test
    public void testDetect_dockDefenderActive() {
        mBatteryInfo.isBatteryDefender = true;
        doReturn(true).when(mFakeFeatureFactory.powerUsageFeatureProvider).isExtraDefend();

        BatteryTip batteryTip = mDockDefenderDetector.detect();

        assertTrue(batteryTip instanceof DockDefenderTip);
        assertEquals(
                ((DockDefenderTip) batteryTip).getMode(), BatteryUtils.DockDefenderMode.ACTIVE);
    }

    @Test
    public void testDetect_dockDefenderFutureBypass() {
        mBatteryInfo.isBatteryDefender = false;
        doReturn(false).when(mFakeFeatureFactory.powerUsageFeatureProvider).isExtraDefend();

        BatteryTip batteryTip = mDockDefenderDetector.detect();

        assertTrue(batteryTip instanceof DockDefenderTip);
        assertEquals(
                ((DockDefenderTip) batteryTip).getMode(),
                BatteryUtils.DockDefenderMode.FUTURE_BYPASS);
    }

    @Test
    public void testDetect_overheatedTrue_dockDefenderDisabled() {
        mBatteryInfo.isBatteryDefender = true;
        doReturn(false).when(mFakeFeatureFactory.powerUsageFeatureProvider).isExtraDefend();

        BatteryTip batteryTip = mDockDefenderDetector.detect();

        assertTrue(batteryTip instanceof DockDefenderTip);
        assertEquals(
                ((DockDefenderTip) batteryTip).getMode(), BatteryUtils.DockDefenderMode.DISABLED);
    }

    @Test
    public void testDetect_pluggedInAC_dockDefenderDisabled() {
        mBatteryInfo.pluggedStatus = BatteryManager.BATTERY_PLUGGED_AC;

        BatteryTip batteryTip = mDockDefenderDetector.detect();

        assertTrue(batteryTip instanceof DockDefenderTip);
        assertEquals(
                ((DockDefenderTip) batteryTip).getMode(), BatteryUtils.DockDefenderMode.DISABLED);
    }

    @Test
    public void testDetect_overheatedTrueAndDockDefenderNotTriggered_dockDefenderDisabled() {
        doReturn(false).when(mFakeFeatureFactory.powerUsageFeatureProvider).isExtraDefend();
        mBatteryInfo.isBatteryDefender = true;

        BatteryTip batteryTip = mDockDefenderDetector.detect();

        assertTrue(batteryTip instanceof DockDefenderTip);
        assertEquals(
                ((DockDefenderTip) batteryTip).getMode(), BatteryUtils.DockDefenderMode.DISABLED);
    }
}
