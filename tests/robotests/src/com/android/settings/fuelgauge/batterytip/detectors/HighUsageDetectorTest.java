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

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.BatteryStats;
import android.text.format.DateUtils;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.TestConfig;
import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.HighUsageDataParser;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class HighUsageDetectorTest {
    private Context mContext;
    @Mock
    private BatteryStatsHelper mBatteryStatsHelper;
    @Mock
    private BatteryUtils mBatteryUtils;
    @Mock
    private BatterySipper mBatterySipper;
    @Mock
    private HighUsageDataParser mDataParser;

    private BatteryTipPolicy mPolicy;
    private HighUsageDetector mHighUsageDetector;
    private List<BatterySipper> mUsageList;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mPolicy = spy(new BatteryTipPolicy(mContext));
        mHighUsageDetector = spy(new HighUsageDetector(mContext, mPolicy, mBatteryStatsHelper));
        mHighUsageDetector.mBatteryUtils = mBatteryUtils;
        mHighUsageDetector.mDataParser = mDataParser;
        doNothing().when(mHighUsageDetector).parseBatteryData();

        mUsageList = new ArrayList<>();
        mUsageList.add(mBatterySipper);
    }

    @Test
    public void testDetect_disabledByPolicy_tipInvisible() {
        ReflectionHelpers.setField(mPolicy, "highUsageEnabled", false);

        assertThat(mHighUsageDetector.detect().isVisible()).isFalse();
    }

    @Test
    public void testDetect_containsHighUsageApp_tipVisible() {
        doReturn(true).when(mDataParser).isDeviceHeavilyUsed();
        doReturn(mUsageList).when(mBatteryStatsHelper).getUsageList();
        doReturn(DateUtils.HOUR_IN_MILLIS).when(mBatteryUtils).getProcessTimeMs(
                BatteryUtils.StatusType.FOREGROUND, mBatterySipper.uidObj,
                BatteryStats.STATS_SINCE_CHARGED);

        assertThat(mHighUsageDetector.detect().isVisible()).isTrue();
    }
}
