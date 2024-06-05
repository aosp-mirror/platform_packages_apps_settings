/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class SmartBatteryDetectorTest {

    private static final int EXPECTED_BATTERY_LEVEL = 30;
    private static final int UNEXPECTED_BATTERY_LEVEL = 31;

    private Context mContext;
    private ContentResolver mContentResolver;
    private SmartBatteryDetector mSmartBatteryDetector;
    private BatteryTipPolicy mPolicy;

    @Mock private BatteryInfo mBatteryInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mContentResolver = mContext.getContentResolver();
        mPolicy = spy(new BatteryTipPolicy(mContext));
        mSmartBatteryDetector =
                new SmartBatteryDetector(
                        mContext,
                        mPolicy,
                        mBatteryInfo,
                        mContentResolver,
                        false /* isPowerSaveMode */);
    }

    @Test
    public void testDetect_testFeatureOn_tipNew() {
        ReflectionHelpers.setField(mPolicy, "testSmartBatteryTip", true);

        assertThat(mSmartBatteryDetector.detect().getState()).isEqualTo(BatteryTip.StateType.NEW);
    }

    @Test
    public void testDetect_smartBatteryOff_tipVisible() {
        Settings.Global.putInt(
                mContentResolver, Settings.Global.ADAPTIVE_BATTERY_MANAGEMENT_ENABLED, 0);
        mBatteryInfo.batteryLevel = EXPECTED_BATTERY_LEVEL;

        assertThat(mSmartBatteryDetector.detect().isVisible()).isTrue();
    }

    @Test
    public void testDetect_batterySaverOn_tipInvisible() {
        Settings.Global.putInt(
                mContentResolver, Settings.Global.ADAPTIVE_BATTERY_MANAGEMENT_ENABLED, 0);
        mBatteryInfo.batteryLevel = EXPECTED_BATTERY_LEVEL;
        mSmartBatteryDetector =
                new SmartBatteryDetector(
                        mContext,
                        mPolicy,
                        mBatteryInfo,
                        mContentResolver,
                        true /* isPowerSaveMode */);

        assertThat(mSmartBatteryDetector.detect().isVisible()).isFalse();
    }

    @Test
    public void testDetect_unexpectedBatteryLevel_tipInvisible() {
        Settings.Global.putInt(
                mContentResolver, Settings.Global.ADAPTIVE_BATTERY_MANAGEMENT_ENABLED, 0);
        mBatteryInfo.batteryLevel = UNEXPECTED_BATTERY_LEVEL;
        mSmartBatteryDetector =
                new SmartBatteryDetector(
                        mContext,
                        mPolicy,
                        mBatteryInfo,
                        mContentResolver,
                        true /* isPowerSaveMode */);

        assertThat(mSmartBatteryDetector.detect().isVisible()).isFalse();
    }

    @Test
    public void testDetect_smartBatteryOn_tipInvisible() {
        Settings.Global.putInt(
                mContentResolver, Settings.Global.ADAPTIVE_BATTERY_MANAGEMENT_ENABLED, 1);
        mBatteryInfo.batteryLevel = EXPECTED_BATTERY_LEVEL;

        assertThat(mSmartBatteryDetector.detect().isVisible()).isFalse();
    }
}
