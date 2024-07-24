/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.os.LocaleList;
import android.view.View;

import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;
import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
public final class BatteryChartViewTest {

    private Context mContext;
    private BatteryChartView mBatteryChartView;
    private FakeFeatureFactory mFeatureFactory;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;

    @Mock private View mMockView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mPowerUsageFeatureProvider = mFeatureFactory.powerUsageFeatureProvider;
        mContext = spy(RuntimeEnvironment.application);
        mContext.getResources().getConfiguration().setLocales(new LocaleList(new Locale("en_US")));
        mBatteryChartView = new BatteryChartView(mContext);
    }

    @Test
    public void onClick_invokesCallback() {
        final int originalSelectedIndex = 2;
        BatteryChartViewModel batteryChartViewModel =
                new BatteryChartViewModel(
                        List.of(90, 80, 70, 60),
                        List.of(0L, 0L, 0L, 0L),
                        BatteryChartViewModel.AxisLabelPosition.BETWEEN_TRAPEZOIDS,
                        null);
        batteryChartViewModel.setSelectedIndex(originalSelectedIndex);
        mBatteryChartView.setViewModel(batteryChartViewModel);
        for (int i = 0; i < mBatteryChartView.mTrapezoidSlots.length; i++) {
            mBatteryChartView.mTrapezoidSlots[i] = new BatteryChartView.TrapezoidSlot();
            mBatteryChartView.mTrapezoidSlots[i].mLeft = i;
            mBatteryChartView.mTrapezoidSlots[i].mRight = i + 0.5f;
        }
        final int[] selectedIndex = new int[1];
        mBatteryChartView.setOnSelectListener(
                trapezoidIndex -> {
                    selectedIndex[0] = trapezoidIndex;
                });

        // Verify onClick() a different index 1.
        mBatteryChartView.mTouchUpEventX = 1;
        selectedIndex[0] = Integer.MIN_VALUE;
        mBatteryChartView.onClick(mMockView);
        assertThat(selectedIndex[0]).isEqualTo(1);

        // Verify onClick() the same index 2.
        mBatteryChartView.mTouchUpEventX = 2;
        selectedIndex[0] = Integer.MIN_VALUE;
        mBatteryChartView.onClick(mMockView);
        assertThat(selectedIndex[0]).isEqualTo(BatteryChartViewModel.SELECTED_INDEX_ALL);
    }
}
