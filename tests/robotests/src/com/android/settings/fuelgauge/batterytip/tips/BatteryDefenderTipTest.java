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
package com.android.settings.fuelgauge.batterytip.tips;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BatteryDefenderTipTest {

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private BatteryDefenderTip mBatteryDefenderTip;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Mock private BatteryTip mBatteryTip;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = mFeatureFactory.metricsFeatureProvider;
        mContext = RuntimeEnvironment.application;
        mBatteryDefenderTip = new BatteryDefenderTip(BatteryTip.StateType.NEW);
    }

    @Test
    public void getTitle_showTitle() {
        assertThat(mBatteryDefenderTip.getTitle(mContext))
                .isEqualTo(mContext.getString(R.string.battery_tip_limited_temporarily_title));
    }

    @Test
    public void getSummary_showSummary() {
        assertThat(mBatteryDefenderTip.getSummary(mContext))
                .isEqualTo(mContext.getString(R.string.battery_tip_limited_temporarily_summary));
    }

    @Test
    public void getIcon_showIcon() {
        assertThat(mBatteryDefenderTip.getIconId())
                .isEqualTo(R.drawable.ic_battery_status_good_24dp);
    }

    @Test
    public void testLog_logMetric() {
        mBatteryDefenderTip.updateState(mBatteryTip);
        mBatteryDefenderTip.log(mContext, mMetricsFeatureProvider);

        verify(mMetricsFeatureProvider).action(mContext,
                SettingsEnums.ACTION_BATTERY_DEFENDER_TIP, mBatteryTip.mState);
    }
}
