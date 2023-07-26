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

package com.android.settings.fuelgauge.batteryusage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class BatteryTipsCardPreferenceTest {

    private Context mContext;
    private BatteryTipsCardPreference mBatteryTipsCardPreference;
    private BatteryTipsController mBatteryTipsController;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mBatteryTipsCardPreference = new BatteryTipsCardPreference(mContext, /*attrs=*/ null);
        mBatteryTipsController = new BatteryTipsController(mContext);
        mBatteryTipsController.mCardPreference = mBatteryTipsCardPreference;
    }

    @Test
    public void constructor_returnExpectedResult() {
        assertThat(mBatteryTipsCardPreference.getLayoutResource()).isEqualTo(
                R.layout.battery_tips_card);
    }
}
