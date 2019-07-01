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

package com.android.settings.homepage.contextualcards.conditional;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.IntentFilter;
import android.os.PowerManager;

import com.android.settings.fuelgauge.BatterySaverReceiver;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPowerManager;

@RunWith(RobolectricTestRunner.class)
public class BatterySaverConditionControllerTest {
    @Mock
    private ConditionManager mConditionManager;

    private ShadowPowerManager mPowerManager;
    private Context mContext;
    private BatterySaverConditionController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mPowerManager = Shadows.shadowOf(mContext.getSystemService(PowerManager.class));
        mController = new BatterySaverConditionController(mContext, mConditionManager);
    }

    @Test
    public void startMonitor_shouldRegisterReceiver() {
        mController.startMonitoringStateChange();

        verify(mContext).registerReceiver(any(BatterySaverReceiver.class), any(IntentFilter.class));
    }

    @Test
    public void stopMonitor_shouldUnregisterReceiver() {
        mController.startMonitoringStateChange();
        mController.stopMonitoringStateChange();

        verify(mContext).unregisterReceiver(any(BatterySaverReceiver.class));
    }

    @Test
    public void isDisplayable_PowerSaverOn_true() {
        mPowerManager.setIsPowerSaveMode(true);

        assertThat(mController.isDisplayable()).isTrue();
    }

    @Test
    public void isDisplayable_PowerSaverOff_false() {
        mPowerManager.setIsPowerSaveMode(false);

        assertThat(mController.isDisplayable()).isFalse();
    }
}
