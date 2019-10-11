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
import android.graphics.ColorFilter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BatteryMeterViewTest {

    private static final int BATTERY_LEVEL = 100;
    private static final int BATTERY_CRITICAL_LEVEL = 15;
    private static final int BATTERY_LOW_LEVEL = 3;

    @Mock
    private ColorFilter mErrorColorFilter;
    @Mock
    private ColorFilter mAccentColorFilter;
    @Mock
    private ColorFilter mForegroundColorFilter;
    private Context mContext;
    private BatteryMeterView mBatteryMeterView;
    private BatteryMeterView.BatteryMeterDrawable mDrawable;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mBatteryMeterView = new BatteryMeterView(mContext);
        mDrawable = spy(new BatteryMeterView.BatteryMeterDrawable(mContext, 0));

        mBatteryMeterView.mDrawable = mDrawable;
        mBatteryMeterView.mAccentColorFilter = mAccentColorFilter;
        mBatteryMeterView.mErrorColorFilter = mErrorColorFilter;
        mBatteryMeterView.mForegroundColorFilter = mForegroundColorFilter;

        when(mDrawable.getCriticalLevel()).thenReturn(BATTERY_CRITICAL_LEVEL);
    }

    @Test
    public void testSetBatteryInfo_setCorrectly() {
        mBatteryMeterView.setBatteryLevel(BATTERY_LEVEL);

        assertThat(mDrawable.getBatteryLevel()).isEqualTo(BATTERY_LEVEL);
    }

    @Test
    public void testSetBatteryInfo_levelLow_setErrorColor() {
        mBatteryMeterView.setBatteryLevel(BATTERY_LOW_LEVEL);

        verify(mDrawable).setColorFilter(mErrorColorFilter);
    }

    @Test
    public void testSetBatteryInfo_levelNormal_setNormalColor() {
        mBatteryMeterView.setBatteryLevel(BATTERY_LEVEL);

        verify(mDrawable).setColorFilter(mAccentColorFilter);
    }

    @Test
    public void testSetBatteryInfo_powerSave_setCorrectly() {
        mBatteryMeterView.setPowerSave(true);

        assertThat(mBatteryMeterView.getPowerSave()).isEqualTo(true);
        verify(mDrawable).setColorFilter(mForegroundColorFilter);
    }
}
