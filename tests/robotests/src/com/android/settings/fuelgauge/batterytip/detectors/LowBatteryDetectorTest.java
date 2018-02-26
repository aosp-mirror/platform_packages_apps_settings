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

import android.text.format.DateUtils;

import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
public class LowBatteryDetectorTest {

    @Mock
    private BatteryInfo mBatteryInfo;
    private BatteryTipPolicy mPolicy;
    private LowBatteryDetector mLowBatteryDetector;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mPolicy = spy(new BatteryTipPolicy(RuntimeEnvironment.application));
        ReflectionHelpers.setField(mPolicy, "lowBatteryEnabled", true);

        mLowBatteryDetector = new LowBatteryDetector(mPolicy, mBatteryInfo);
    }

    @Test
    public void testDetect_disabledByPolicy_tipInvisible() {
        ReflectionHelpers.setField(mPolicy, "lowBatteryEnabled", false);

        assertThat(mLowBatteryDetector.detect().isVisible()).isFalse();
    }

    @Test
    public void testDetect_shortBatteryLife_tipVisible() {
        mBatteryInfo.discharging = true;
        mBatteryInfo.remainingTimeUs = DateUtils.MINUTE_IN_MILLIS;

        assertThat(mLowBatteryDetector.detect().isVisible()).isTrue();
    }

    @Test
    public void testDetect_longBatteryLife_tipInvisible() {
        mBatteryInfo.discharging = true;
        mBatteryInfo.remainingTimeUs = DateUtils.DAY_IN_MILLIS;

        assertThat(mLowBatteryDetector.detect().isVisible()).isFalse();
    }
}
