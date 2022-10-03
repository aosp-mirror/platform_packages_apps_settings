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
package com.android.settings.fuelgauge.batteryusage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;

import android.content.Context;

import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public final class BatteryHistoryLoaderTest {

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private BatteryHistoryLoader mBatteryHistoryLoader;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mBatteryHistoryLoader = new BatteryHistoryLoader(mContext);
    }

    @Test
    public void testLoadIBackground_returnsMapFromPowerFeatureProvider() {
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap = new HashMap<>();
        doReturn(batteryHistoryMap).when(mFeatureFactory.powerUsageFeatureProvider)
                .getBatteryHistorySinceLastFullCharge(mContext);

        assertThat(mBatteryHistoryLoader.loadInBackground())
                .isSameInstanceAs(batteryHistoryMap);
    }
}
