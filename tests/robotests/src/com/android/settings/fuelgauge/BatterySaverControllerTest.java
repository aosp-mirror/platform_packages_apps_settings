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
package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class BatterySaverControllerTest {

    @Mock private Preference mBatterySaverPref;
    @Mock private PowerManager mPowerManager;

    private BatterySaverController mBatterySaverController;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mBatterySaverController = spy(new BatterySaverController(mContext));
        ReflectionHelpers.setField(mBatterySaverController, "mPowerManager", mPowerManager);
        ReflectionHelpers.setField(mBatterySaverController, "mBatterySaverPref", mBatterySaverPref);

        Settings.Global.putInt(
                mContext.getContentResolver(), Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0);
    }

    @Test
    public void onPreferenceChange_onStart() {
        mBatterySaverController.onStart();
        verify(mBatterySaverPref).setSummary("Off");
    }

    @Test
    public void onPreferenceChange_onPowerSaveModeChanged() {
        mBatterySaverController.onPowerSaveModeChanged();
        verify(mBatterySaverPref).setSummary("Off");
    }

    @Test
    public void getSummary_batterySaverOn_showSummaryOn() {
        when(mPowerManager.isPowerSaveMode()).thenReturn(true);

        assertThat(mBatterySaverController.getSummary()).isEqualTo("On");
    }

    @Test
    public void getSummary_batterySaverOffButScheduled_showSummaryScheduled() {
        when(mPowerManager.isPowerSaveMode()).thenReturn(false);
        Settings.Global.putInt(
                mContext.getContentResolver(), Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, 15);

        assertThat(mBatterySaverController.getSummary()).isEqualTo("Will turn on at 15%");
    }

    @Test
    public void getSummary_batterySaverOffButScheduledZeroPercent_showSummaryOff() {
        when(mPowerManager.isPowerSaveMode()).thenReturn(false);
        Settings.Global.putInt(
                mContext.getContentResolver(), Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0);

        assertThat(mBatterySaverController.getSummary()).isEqualTo("Off");
    }

    @Test
    public void getSummary_batterySaverOffButScheduledBasedOnRoutine_showSummaryBasedOnRoutine() {
        when(mPowerManager.isPowerSaveMode()).thenReturn(false);
        Settings.Global.putInt(
                mContext.getContentResolver(),
                Settings.Global.AUTOMATIC_POWER_SAVE_MODE,
                PowerManager.POWER_SAVE_MODE_TRIGGER_DYNAMIC);

        assertThat(mBatterySaverController.getSummary())
                .isEqualTo("Will turn on based on your routine");
    }

    @Test
    public void getSummary_batterySaverOff_showSummaryOff() {
        when(mPowerManager.isPowerSaveMode()).thenReturn(false);

        assertThat(mBatterySaverController.getSummary()).isEqualTo("Off");
    }

    @Test
    public void getAvailabilityStatus_returnAvailable() {
        assertThat(mBatterySaverController.getAvailabilityStatus())
                .isEqualTo(BatterySaverController.AVAILABLE);
    }
}
