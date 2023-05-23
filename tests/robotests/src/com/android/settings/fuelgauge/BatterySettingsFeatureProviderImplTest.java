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

package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.BatteryManager;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBatteryManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBatteryManager.class})
public class BatterySettingsFeatureProviderImplTest {
    private BatteryManager mBatteryManager;
    private ShadowBatteryManager mShadowBatteryManager;
    private Context mContext;
    private BatterySettingsFeatureProviderImpl mImpl;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mBatteryManager = mContext.getSystemService(BatteryManager.class);
        mShadowBatteryManager = shadowOf(mBatteryManager);
        mImpl = spy(new BatterySettingsFeatureProviderImpl(mContext));
    }

    @Test
    public void isManufactureDateAvailable_returnFalse() {
        assertThat(mImpl.isManufactureDateAvailable()).isFalse();
    }

    @Test
    public void isFirstUseDateAvailable_returnFalse() {
        assertThat(mImpl.isFirstUseDateAvailable()).isFalse();
    }

    @Test
    public void getManufactureDateSummary_available_returnExpectedDate() {
        doReturn(true).when(mImpl).isManufactureDateAvailable();
        mShadowBatteryManager.setLongProperty(BatteryManager.BATTERY_PROPERTY_MANUFACTURING_DATE,
                1669680000L);

        final CharSequence result = mImpl.getManufactureDateSummary();

        assertThat(result.toString()).isEqualTo("November 29, 2022");
    }

    @Test
    public void getManufactureDateSummary_unavailable_returnNull() {
        doReturn(false).when(mImpl).isManufactureDateAvailable();

        assertThat(mImpl.getManufactureDateSummary()).isNull();
    }

    @Test
    public void getFirstUseDateSummary_available_returnExpectedDate() {
        doReturn(true).when(mImpl).isFirstUseDateAvailable();
        mShadowBatteryManager.setLongProperty(BatteryManager.BATTERY_PROPERTY_FIRST_USAGE_DATE,
                1669680000L);

        final CharSequence result = mImpl.getFirstUseDateSummary();

        assertThat(result.toString()).isEqualTo("November 29, 2022");
    }

    @Test
    public void getFirstUseDateSummary_unavailable_returnNull() {
        doReturn(false).when(mImpl).isFirstUseDateAvailable();

        assertThat(mImpl.getFirstUseDateSummary()).isNull();
    }
}
