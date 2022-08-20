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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.os.LocaleList;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
public final class BatteryChartViewTest {

    private Context mContext;
    private BatteryChartView mBatteryChartView;
    private FakeFeatureFactory mFeatureFactory;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;

    @Mock
    private AccessibilityServiceInfo mMockAccessibilityServiceInfo;
    @Mock
    private AccessibilityManager mMockAccessibilityManager;
    @Mock
    private View mMockView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mPowerUsageFeatureProvider = mFeatureFactory.powerUsageFeatureProvider;
        mContext = spy(RuntimeEnvironment.application);
        mContext.getResources().getConfiguration().setLocales(
                new LocaleList(new Locale("en_US")));
        mBatteryChartView = new BatteryChartView(mContext);
        doReturn(mMockAccessibilityManager).when(mContext)
                .getSystemService(AccessibilityManager.class);
        doReturn("TalkBackService").when(mMockAccessibilityServiceInfo).getId();
        doReturn(Arrays.asList(mMockAccessibilityServiceInfo))
                .when(mMockAccessibilityManager)
                .getEnabledAccessibilityServiceList(anyInt());
    }

    @Test
    public void isAccessibilityEnabled_disable_returnFalse() {
        doReturn(false).when(mMockAccessibilityManager).isEnabled();
        assertThat(BatteryChartView.isAccessibilityEnabled(mContext)).isFalse();
    }

    @Test
    public void isAccessibilityEnabled_emptyInfo_returnFalse() {
        doReturn(true).when(mMockAccessibilityManager).isEnabled();
        doReturn(new ArrayList<AccessibilityServiceInfo>())
                .when(mMockAccessibilityManager)
                .getEnabledAccessibilityServiceList(anyInt());

        assertThat(BatteryChartView.isAccessibilityEnabled(mContext)).isFalse();
    }

    @Test
    public void isAccessibilityEnabled_validServiceId_returnTrue() {
        doReturn(true).when(mMockAccessibilityManager).isEnabled();
        assertThat(BatteryChartView.isAccessibilityEnabled(mContext)).isTrue();
    }

    @Test
    public void onClick_invokesCallback() {
        final int originalSelectedIndex = 2;
        BatteryChartViewModel batteryChartViewModel = new BatteryChartViewModel(
                List.of(90, 80, 70, 60), List.of("", "", "", ""),
                BatteryChartViewModel.AxisLabelPosition.BETWEEN_TRAPEZOIDS);
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

    @Test
    public void clickable_isChartGraphSlotsEnabledIsFalse_notClickable() {
        mBatteryChartView.setClickableForce(true);
        when(mPowerUsageFeatureProvider.isChartGraphSlotsEnabled(mContext))
                .thenReturn(false);

        mBatteryChartView.onAttachedToWindow();

        assertThat(mBatteryChartView.isClickable()).isFalse();
        assertThat(mBatteryChartView.mTrapezoidCurvePaint).isNotNull();
    }

    @Test
    public void clickable_accessibilityIsDisabled_clickable() {
        mBatteryChartView.setClickableForce(true);
        when(mPowerUsageFeatureProvider.isChartGraphSlotsEnabled(mContext))
                .thenReturn(true);
        doReturn(false).when(mMockAccessibilityManager).isEnabled();

        mBatteryChartView.onAttachedToWindow();

        assertThat(mBatteryChartView.isClickable()).isTrue();
        assertThat(mBatteryChartView.mTrapezoidCurvePaint).isNull();
    }

    @Test
    public void clickable_accessibilityIsEnabledWithoutValidId_clickable() {
        mBatteryChartView.setClickableForce(true);
        when(mPowerUsageFeatureProvider.isChartGraphSlotsEnabled(mContext))
                .thenReturn(true);
        doReturn(true).when(mMockAccessibilityManager).isEnabled();
        doReturn(new ArrayList<AccessibilityServiceInfo>())
                .when(mMockAccessibilityManager)
                .getEnabledAccessibilityServiceList(anyInt());

        mBatteryChartView.onAttachedToWindow();

        assertThat(mBatteryChartView.isClickable()).isTrue();
        assertThat(mBatteryChartView.mTrapezoidCurvePaint).isNull();
    }

    @Test
    public void clickable_accessibilityIsEnabledWithValidId_notClickable() {
        mBatteryChartView.setClickableForce(true);
        when(mPowerUsageFeatureProvider.isChartGraphSlotsEnabled(mContext))
                .thenReturn(true);
        doReturn(true).when(mMockAccessibilityManager).isEnabled();

        mBatteryChartView.onAttachedToWindow();

        assertThat(mBatteryChartView.isClickable()).isFalse();
        assertThat(mBatteryChartView.mTrapezoidCurvePaint).isNotNull();
    }

    @Test
    public void clickable_restoreFromNonClickableState() {
        final List<Integer> levels = new ArrayList<Integer>();
        final List<String> texts = new ArrayList<String>();
        for (int index = 0; index < 13; index++) {
            levels.add(index + 1);
            texts.add("");
        }
        mBatteryChartView.setViewModel(new BatteryChartViewModel(levels, texts,
                BatteryChartViewModel.AxisLabelPosition.BETWEEN_TRAPEZOIDS));
        mBatteryChartView.setClickableForce(true);
        when(mPowerUsageFeatureProvider.isChartGraphSlotsEnabled(mContext))
                .thenReturn(true);
        doReturn(true).when(mMockAccessibilityManager).isEnabled();
        mBatteryChartView.onAttachedToWindow();
        // Ensures the testing environment is correct.
        assertThat(mBatteryChartView.isClickable()).isFalse();
        // Turns off accessibility service.
        doReturn(false).when(mMockAccessibilityManager).isEnabled();

        mBatteryChartView.onAttachedToWindow();

        assertThat(mBatteryChartView.isClickable()).isTrue();
    }

    @Test
    public void onAttachedToWindow_addAccessibilityStateChangeListener() {
        mBatteryChartView.onAttachedToWindow();
        verify(mMockAccessibilityManager)
                .addAccessibilityStateChangeListener(mBatteryChartView);
    }

    @Test
    public void onDetachedFromWindow_removeAccessibilityStateChangeListener() {
        mBatteryChartView.onAttachedToWindow();
        mBatteryChartView.mHandler.postDelayed(
                mBatteryChartView.mUpdateClickableStateRun, 1000);

        mBatteryChartView.onDetachedFromWindow();

        verify(mMockAccessibilityManager)
                .removeAccessibilityStateChangeListener(mBatteryChartView);
        assertThat(mBatteryChartView.mHandler.hasCallbacks(
                mBatteryChartView.mUpdateClickableStateRun))
                .isFalse();
    }

    @Test
    public void onAccessibilityStateChanged_postUpdateStateRunnable() {
        mBatteryChartView.mHandler = spy(mBatteryChartView.mHandler);
        mBatteryChartView.onAccessibilityStateChanged(/*enabled=*/ true);

        verify(mBatteryChartView.mHandler)
                .removeCallbacks(mBatteryChartView.mUpdateClickableStateRun);
        verify(mBatteryChartView.mHandler)
                .postDelayed(mBatteryChartView.mUpdateClickableStateRun, 500L);
    }
}
