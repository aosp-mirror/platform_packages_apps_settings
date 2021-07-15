/*
 * Copyright (C) 2020 The Android Open Source Project
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

import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BatteryDefenderDetectorTest {

    @Mock
    private BatteryInfo mBatteryInfo;
    private BatteryDefenderDetector mBatteryDefenderDetector;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mBatteryInfo.discharging = false;

        mBatteryDefenderDetector = new BatteryDefenderDetector(mBatteryInfo);
    }

    @Test
    public void testDetect_notOverheated_tipInvisible() {
        mBatteryInfo.isOverheated = false;

        assertThat(mBatteryDefenderDetector.detect().isVisible()).isFalse();
    }

    @Test
    public void testDetect_isOverheated_tipNew() {
        mBatteryInfo.isOverheated = true;

        assertThat(mBatteryDefenderDetector.detect().getState())
                .isEqualTo(BatteryTip.StateType.NEW);
    }
}
