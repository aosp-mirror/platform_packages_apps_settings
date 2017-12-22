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

package com.android.settings.fuelgauge.batterytip;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;

import android.content.Context;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.TestConfig;
import com.android.settings.fuelgauge.batterytip.detectors.BatteryTipDetector;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BatteryTipLoaderTest {
    @Mock
    private BatteryStatsHelper mBatteryStatsHelper;
    @Mock
    private BatteryTipDetector mBatteryTipDetector;
    @Mock
    private BatteryTip mBatteryTip;
    private Context mContext;
    private BatteryTipLoader mBatteryTipLoader;
    private List<BatteryTip> mBatteryTips;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        doReturn(mBatteryTip).when(mBatteryTipDetector).detect();
        mBatteryTipLoader = new BatteryTipLoader(mContext, mBatteryStatsHelper);
        mBatteryTips = new ArrayList<>();
    }

    @Test
    public void testAddBatteryTipFromDetector_tipVisible_addAndUpdateCount() {
        doReturn(true).when(mBatteryTip).isVisible();
        mBatteryTipLoader.mVisibleTips = 0;

        mBatteryTipLoader.addBatteryTipFromDetector(mBatteryTips, mBatteryTipDetector);

        assertThat(mBatteryTips.contains(mBatteryTip)).isTrue();
        assertThat(mBatteryTipLoader.mVisibleTips).isEqualTo(1);
    }

    @Test
    public void testAddBatteryTipFromDetector_tipInvisible_doNotAddCount() {
        doReturn(false).when(mBatteryTip).isVisible();
        mBatteryTipLoader.mVisibleTips = 0;

        mBatteryTipLoader.addBatteryTipFromDetector(mBatteryTips, mBatteryTipDetector);

        assertThat(mBatteryTips.contains(mBatteryTip)).isTrue();
        assertThat(mBatteryTipLoader.mVisibleTips).isEqualTo(0);
    }

}
