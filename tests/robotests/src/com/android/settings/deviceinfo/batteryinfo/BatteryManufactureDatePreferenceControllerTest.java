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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.BatteryManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBatteryManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBatteryManager.class})
public class BatteryManufactureDatePreferenceControllerTest {

    private BatteryManufactureDatePreferenceController mController;
    private Context mContext;
    private BatteryManager mBatteryManager;
    private ShadowBatteryManager mShadowBatteryManager;
    private FakeFeatureFactory mFactory;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mBatteryManager = mContext.getSystemService(BatteryManager.class);
        mShadowBatteryManager = shadowOf(mBatteryManager);
        mFactory = FakeFeatureFactory.setupForTest();
        mController = new BatteryManufactureDatePreferenceController(mContext,
                "battery_info_manufacture_date");
    }

    @Test
    public void getAvailabilityStatus_dateAvailable_returnAvailable() {
        when(mFactory.batterySettingsFeatureProvider.isManufactureDateAvailable(eq(mContext),
                anyLong())).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_dateUnavailable_returnNotAvailable() {
        when(mFactory.batterySettingsFeatureProvider.isManufactureDateAvailable(eq(mContext),
                anyLong())).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Ignore("b/315267179")
    @Test
    public void getSummary_available_returnExpectedDate() {
        when(mFactory.batterySettingsFeatureProvider.isManufactureDateAvailable(eq(mContext),
                anyLong())).thenReturn(true);
        mShadowBatteryManager.setLongProperty(BatteryManager.BATTERY_PROPERTY_MANUFACTURING_DATE,
                1669680000L);

        final CharSequence result = mController.getSummary();

        assertThat(result.toString()).isEqualTo("November 29, 2022");
    }

    @Test
    public void getSummary_unavailable_returnNull() {
        when(mFactory.batterySettingsFeatureProvider.isManufactureDateAvailable(eq(mContext),
                anyLong())).thenReturn(false);

        assertThat(mController.getSummary()).isNull();
    }
}
