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

package com.android.settings.fuelgauge.batterytip.detectors;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;

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

import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class LowBatteryDetectorTest {

    @Mock private BatteryInfo mBatteryInfo;
    private BatteryTipPolicy mPolicy;
    private LowBatteryDetector mLowBatteryDetector;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mPolicy = spy(new BatteryTipPolicy(RuntimeEnvironment.application));
        mContext = RuntimeEnvironment.application;
        ReflectionHelpers.setField(mPolicy, "lowBatteryEnabled", true);
        mBatteryInfo.discharging = true;

        mLowBatteryDetector =
                new LowBatteryDetector(
                        mContext, mPolicy, mBatteryInfo, false /* isPowerSaveMode */);
    }

    @Test
    public void testDetect_disabledByPolicy_tipInvisible() {
        ReflectionHelpers.setField(mPolicy, "lowBatteryEnabled", false);
        mLowBatteryDetector =
                new LowBatteryDetector(mContext, mPolicy, mBatteryInfo, true /* isPowerSaveMode */);

        assertThat(mLowBatteryDetector.detect().isVisible()).isFalse();
    }

    @Test
    public void testDetect_enabledByTest_tipNew() {
        ReflectionHelpers.setField(mPolicy, "testLowBatteryTip", true);

        assertThat(mLowBatteryDetector.detect().getState()).isEqualTo(BatteryTip.StateType.NEW);
    }

    @Test
    public void testDetect_lowBattery_tipNew() {
        mBatteryInfo.batteryLevel = 20;
        mBatteryInfo.remainingTimeUs = TimeUnit.DAYS.toMillis(1);
        assertThat(mLowBatteryDetector.detect().getState()).isEqualTo(BatteryTip.StateType.NEW);
    }

    @Test
    public void testDetect_batterySaverOn_tipInvisible() {
        mLowBatteryDetector =
                new LowBatteryDetector(mContext, mPolicy, mBatteryInfo, true /* isPowerSaveMode */);

        assertThat(mLowBatteryDetector.detect().getState())
                .isEqualTo(BatteryTip.StateType.INVISIBLE);
    }

    @Test
    public void testDetect_charging_tipInvisible() {
        mBatteryInfo.discharging = false;

        assertThat(mLowBatteryDetector.detect().isVisible()).isFalse();
    }

    @Test
    public void testDetect_lowTimeEstimation_tipInvisible() {
        mBatteryInfo.batteryLevel = 50;
        mBatteryInfo.remainingTimeUs = TimeUnit.MINUTES.toMillis(1);
        assertThat(mLowBatteryDetector.detect().isVisible()).isFalse();
    }

    @Test
    public void testDetect_noEarlyWarning_tipInvisible() {
        mBatteryInfo.remainingTimeUs = TimeUnit.DAYS.toMicros(1);
        mBatteryInfo.batteryLevel = 100;

        assertThat(mLowBatteryDetector.detect().isVisible()).isFalse();
    }
}
