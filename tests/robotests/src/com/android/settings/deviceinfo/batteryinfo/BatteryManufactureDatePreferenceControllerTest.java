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
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BatteryManufactureDatePreferenceControllerTest {

    private BatteryManufactureDatePreferenceController mController;
    private Context mContext;
    private FakeFeatureFactory mFactory;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mFactory = FakeFeatureFactory.setupForTest();
        mController = new BatteryManufactureDatePreferenceController(mContext,
                "battery_info_manufacture_date");
    }

    @Test
    public void getAvailabilityStatus_dateAvailable_returnAvailable() {
        when(mFactory.batterySettingsFeatureProvider.isManufactureDateAvailable()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_dateUnavailable_returnNotAvailable() {
        when(mFactory.batterySettingsFeatureProvider.isManufactureDateAvailable())
                .thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }
}
