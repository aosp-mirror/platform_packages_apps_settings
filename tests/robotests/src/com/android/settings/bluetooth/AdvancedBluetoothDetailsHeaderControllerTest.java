/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.android.settings.fuelgauge.BatteryMeterView;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowEntityHeaderController.class)
public class AdvancedBluetoothDetailsHeaderControllerTest{
    private static final int BATTERY_LEVEL = 30;

    private Context mContext;
    private AdvancedBluetoothDetailsHeaderController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;

        mController = new AdvancedBluetoothDetailsHeaderController(mContext, "pref_Key");
    }

    @Test
    public void createBatteryIcon_hasCorrectInfo() {
        final Drawable drawable = mController.createBtBatteryIcon(mContext, BATTERY_LEVEL);
        assertThat(drawable).isInstanceOf(BatteryMeterView.BatteryMeterDrawable.class);

        final BatteryMeterView.BatteryMeterDrawable iconDrawable =
                (BatteryMeterView.BatteryMeterDrawable) drawable;
        assertThat(iconDrawable.getBatteryLevel()).isEqualTo(BATTERY_LEVEL);
    }

}
