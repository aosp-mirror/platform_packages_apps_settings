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

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.LowBatteryTip;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class BatterySettingsFeatureProviderImplTest {
    private BatterySettingsFeatureProviderImpl mImpl;
    private Context mContext;

    @Before
    public void setUp() {
        mImpl = new BatterySettingsFeatureProviderImpl();
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void isManufactureDateAvailable_returnFalse() {
        assertThat(mImpl.isManufactureDateAvailable(mContext, 1000L)).isFalse();
    }

    @Test
    public void isFirstUseDateAvailable_returnFalse() {
        assertThat(mImpl.isFirstUseDateAvailable(mContext, 1000L)).isFalse();
    }

    @Test
    public void isBatteryInfoEnabled_returnFalse() {
        assertThat(mImpl.isBatteryInfoEnabled(mContext)).isFalse();
    }

    @Test
    public void addBatteryTipDetector_containsLowBatteryTip() {
        var tips = new ArrayList<BatteryTip>();

        mImpl.addBatteryTipDetector(
                mContext, tips, new BatteryInfo(), new BatteryTipPolicy(mContext));

        var expectedResult = tips.stream().anyMatch(tip -> tip instanceof LowBatteryTip);
        assertThat(expectedResult).isTrue();
    }
}
