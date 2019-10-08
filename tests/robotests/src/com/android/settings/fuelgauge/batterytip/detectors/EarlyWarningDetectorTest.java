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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.PowerManager;

import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class EarlyWarningDetectorTest {

    private Context mContext;
    private BatteryTipPolicy mPolicy;
    private EarlyWarningDetector mEarlyWarningDetector;
    @Mock
    private Intent mIntent;
    @Mock
    private PowerManager mPowerManager;
    private FakeFeatureFactory mFakeFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mPolicy = spy(new BatteryTipPolicy(mContext));
        doReturn(mPowerManager).when(mContext).getSystemService(Context.POWER_SERVICE);
        doReturn(mIntent).when(mContext).registerReceiver(any(), any());
        doReturn(0).when(mIntent).getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        doReturn(true).when(mFakeFeatureFactory.powerUsageFeatureProvider)
            .getEarlyWarningSignal(any(), any());

        mEarlyWarningDetector = new EarlyWarningDetector(mPolicy, mContext);
    }

    @Test
    public void testDetect_policyDisabled_tipInvisible() {
        ReflectionHelpers.setField(mPolicy, "batterySaverTipEnabled", false);

        assertThat(mEarlyWarningDetector.detect().isVisible()).isFalse();
    }

    @Test
    public void testDetect_testFeatureOn_tipNew() {
        doReturn(false).when(mFakeFeatureFactory.powerUsageFeatureProvider)
                .getEarlyWarningSignal(any(), any());
        ReflectionHelpers.setField(mPolicy, "testBatterySaverTip", true);

        assertThat(mEarlyWarningDetector.detect().getState())
                .isEqualTo(BatteryTip.StateType.NEW);
    }

    @Test
    public void testDetect_batterySaverOn_tipHandled() {
        doReturn(true).when(mPowerManager).isPowerSaveMode();

        assertThat(mEarlyWarningDetector.detect().getState())
                .isEqualTo(BatteryTip.StateType.HANDLED);
    }

    @Test
    public void testDetect_charging_tipInvisible() {
        doReturn(1).when(mIntent).getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

        assertThat(mEarlyWarningDetector.detect().isVisible()).isFalse();
    }

    @Test
    public void testDetect_noEarlyWarning_tipInvisible() {
        doReturn(false).when(mFakeFeatureFactory.powerUsageFeatureProvider)
            .getEarlyWarningSignal(any(), any());

        assertThat(mEarlyWarningDetector.detect().isVisible()).isFalse();
    }
}
