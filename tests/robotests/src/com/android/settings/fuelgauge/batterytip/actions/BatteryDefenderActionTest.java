/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.fuelgauge.batterytip.actions;


import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
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
public final class BatteryDefenderActionTest {

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private BatteryDefenderAction mBatteryDefenderAction;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Mock private SettingsActivity mSettingsActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = mFeatureFactory.metricsFeatureProvider;
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mContext).when(mSettingsActivity).getApplicationContext();
        mBatteryDefenderAction = new BatteryDefenderAction(mSettingsActivity);
    }

    @Test
    public void testHandlePositiveAction_logMetric() {
        final int metricKey = 10;
        mBatteryDefenderAction.handlePositiveAction(metricKey);

        verify(mMetricsFeatureProvider).action(mContext,
                SettingsEnums.ACTION_TIP_BATTERY_DEFENDER, metricKey);
    }
}
