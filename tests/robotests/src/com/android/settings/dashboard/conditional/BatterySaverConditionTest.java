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

package com.android.settings.dashboard.conditional;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.IntentFilter;
import android.os.PowerManager;

import com.android.settings.R;
import com.android.settings.fuelgauge.BatterySaverReceiver;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPowerManager;

@RunWith(SettingsRobolectricTestRunner.class)
public class BatterySaverConditionTest {
    @Mock
    private ConditionManager mConditionManager;

    private ShadowPowerManager mPowerManager;
    private Context mContext;
    private BatterySaverCondition mCondition;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mPowerManager = Shadows.shadowOf(mContext.getSystemService(PowerManager.class));
        when(mConditionManager.getContext()).thenReturn(mContext);
        mCondition = spy(new BatterySaverCondition(mConditionManager));
    }

    @Test
    public void verifyText() {
        assertThat(mCondition.getTitle()).isEqualTo(
                mContext.getText(R.string.condition_battery_title));
        assertThat(mCondition.getSummary()).isEqualTo(
                mContext.getText(R.string.condition_battery_summary));
        assertThat(mCondition.getActions()[0]).isEqualTo(
                mContext.getText(R.string.condition_turn_off));
    }

    @Test
    public void onResume_shouldRegisterReceiver() {
        mCondition.onResume();

        verify(mContext).registerReceiver(any(BatterySaverReceiver.class), any(IntentFilter.class));
    }

    @Test
    public void onPause_shouldUnregisterReceiver() {
        mCondition.onResume();
        mCondition.onPause();

        verify(mContext).unregisterReceiver(any(BatterySaverReceiver.class));
    }

    @Test
    public void refreshState_PowerSaverOn_shouldActivate() {
        mPowerManager.setIsPowerSaveMode(true);

        mCondition.refreshState();

        assertThat(mCondition.isActive()).isTrue();
    }

    @Test
    public void refreshState_PowerSaverOff_shouldNotActivate() {
        mPowerManager.setIsPowerSaveMode(false);

        mCondition.refreshState();

        assertThat(mCondition.isActive()).isFalse();
    }
}
