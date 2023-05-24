/*
 * Copyright (C) 2023 The Android Open Source Project
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


package com.android.settings.deviceinfo.batteryinfo;


import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BatteryCycleCountPreferenceControllerTest {
    private BatteryCycleCountPreferenceController mController;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mController = new BatteryCycleCountPreferenceController(mContext,
                "battery_info_cycle_count");
    }

    @Test
    public void getAvailabilityStatus_returnAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getSummary_returnExpectedResult() {
        final Intent batteryIntent = new Intent();
        batteryIntent.putExtra(BatteryManager.EXTRA_CYCLE_COUNT, 10);
        doReturn(batteryIntent).when(mContext).registerReceiver(any(), any());

        assertThat(mController.getSummary()).isEqualTo("10");
    }

    @Test
    public void getSummary_noValue_returnUnavailable() {
        final Intent batteryIntent = new Intent();
        doReturn(batteryIntent).when(mContext).registerReceiver(any(), any());

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getText(R.string.battery_cycle_count_not_available));
    }
}
