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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.BatteryManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;


@RunWith(RobolectricTestRunner.class)
public class BatteryInfoFeatureProviderImplTest {
    @Mock
    private BatteryManager mBatteryManager;

    private Context mContext;
    private BatteryInfoFeatureProviderImpl mImpl;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(org.robolectric.RuntimeEnvironment.application);
        doReturn(mBatteryManager).when(mContext).getSystemService(BatteryManager.class);
        mImpl = spy(new BatteryInfoFeatureProviderImpl(mContext));
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
        when(mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_MANUFACTURING_DATE))
                .thenReturn(1669680000L);

        final CharSequence result = mImpl.getManufactureDateSummary();

        assertThat(result).isEqualTo("November 29, 2022");
    }

    @Test
    public void getManufactureDateSummary_unavailable_returnNull() {
        doReturn(false).when(mImpl).isManufactureDateAvailable();

        assertThat(mImpl.getManufactureDateSummary()).isNull();
    }

    @Test
    public void getFirstUseDateSummary_available_returnExpectedDate() {
        doReturn(true).when(mImpl).isFirstUseDateAvailable();
        when(mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_FIRST_USAGE_DATE))
                .thenReturn(1669680000L);

        final CharSequence result = mImpl.getFirstUseDateSummary();

        assertThat(result).isEqualTo("November 29, 2022");
    }

    @Test
    public void getFirstUseDateSummary_unavailable_returnNull() {
        doReturn(false).when(mImpl).isFirstUseDateAvailable();

        assertThat(mImpl.getFirstUseDateSummary()).isNull();
    }
}
