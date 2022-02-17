/*
 * Copyright (C) 2021 The Android Open Source Project
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
 *
 *
 */
package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.os.LocaleList;
import android.view.accessibility.AccessibilityManager;

import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
public final class BatteryChartViewTest {

    private Context mContext;
    private BatteryChartView mBatteryChartView;
    private FakeFeatureFactory mFeatureFactory;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;

    @Mock private AccessibilityServiceInfo mockAccessibilityServiceInfo;
    @Mock private AccessibilityManager mockAccessibilityManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mPowerUsageFeatureProvider = mFeatureFactory.powerUsageFeatureProvider;
        mContext = spy(RuntimeEnvironment.application);
        mContext.getResources().getConfiguration().setLocales(
            new LocaleList(new Locale("en_US")));
        mBatteryChartView = new BatteryChartView(mContext);
        doReturn(mockAccessibilityManager).when(mContext)
            .getSystemService(AccessibilityManager.class);
        doReturn("TalkBackService").when(mockAccessibilityServiceInfo).getId();
        doReturn(Arrays.asList(mockAccessibilityServiceInfo))
            .when(mockAccessibilityManager)
            .getEnabledAccessibilityServiceList(anyInt());
    }

    @Test
    public void testIsAccessibilityEnabled_disable_returnFalse() {
        doReturn(false).when(mockAccessibilityManager).isEnabled();
        assertThat(BatteryChartView.isAccessibilityEnabled(mContext)).isFalse();
    }

    @Test
    public void testIsAccessibilityEnabled_emptyInfo_returnFalse() {
        doReturn(true).when(mockAccessibilityManager).isEnabled();
        doReturn(new ArrayList<AccessibilityServiceInfo>())
            .when(mockAccessibilityManager)
            .getEnabledAccessibilityServiceList(anyInt());

        assertThat(BatteryChartView.isAccessibilityEnabled(mContext)).isFalse();
    }

    @Test
    public void testIsAccessibilityEnabled_validServiceId_returnTrue() {
        doReturn(true).when(mockAccessibilityManager).isEnabled();
        assertThat(BatteryChartView.isAccessibilityEnabled(mContext)).isTrue();
    }

    @Test
    public void testSetSelectedIndex_invokesCallback() {
        final int selectedIndex[] = new int[1];
        final int expectedIndex = 2;
        mBatteryChartView.mSelectedIndex = 1;
        mBatteryChartView.setOnSelectListener(
            trapezoidIndex -> {
                selectedIndex[0] = trapezoidIndex;
            });

        mBatteryChartView.setSelectedIndex(expectedIndex);

        assertThat(mBatteryChartView.mSelectedIndex)
            .isEqualTo(expectedIndex);
        assertThat(selectedIndex[0]).isEqualTo(expectedIndex);
    }

    @Test
    public void testSetSelectedIndex_sameIndex_notInvokesCallback() {
        final int selectedIndex[] = new int[1];
        final int expectedIndex = 1;
        mBatteryChartView.mSelectedIndex = expectedIndex;
        mBatteryChartView.setOnSelectListener(
            trapezoidIndex -> {
                selectedIndex[0] = trapezoidIndex;
            });

        mBatteryChartView.setSelectedIndex(expectedIndex);

        assertThat(selectedIndex[0]).isNotEqualTo(expectedIndex);
    }

    @Test
    public void testClickable_isChartGraphSlotsEnabledIsFalse_notClickable() {
        mBatteryChartView.setClickableForce(true);
        when(mPowerUsageFeatureProvider.isChartGraphSlotsEnabled(mContext))
            .thenReturn(false);

        mBatteryChartView.onAttachedToWindow();
        assertThat(mBatteryChartView.isClickable()).isFalse();
        assertThat(mBatteryChartView.mTrapezoidCurvePaint).isNotNull();
    }

    @Test
    public void testClickable_accessibilityIsDisabled_clickable() {
        mBatteryChartView.setClickableForce(true);
        when(mPowerUsageFeatureProvider.isChartGraphSlotsEnabled(mContext))
            .thenReturn(true);
        doReturn(false).when(mockAccessibilityManager).isEnabled();

        mBatteryChartView.onAttachedToWindow();
        assertThat(mBatteryChartView.isClickable()).isTrue();
        assertThat(mBatteryChartView.mTrapezoidCurvePaint).isNull();
    }

    @Test
    public void testClickable_accessibilityIsEnabledWithoutValidId_clickable() {
        mBatteryChartView.setClickableForce(true);
        when(mPowerUsageFeatureProvider.isChartGraphSlotsEnabled(mContext))
            .thenReturn(true);
        doReturn(true).when(mockAccessibilityManager).isEnabled();
        doReturn(new ArrayList<AccessibilityServiceInfo>())
            .when(mockAccessibilityManager)
            .getEnabledAccessibilityServiceList(anyInt());

        mBatteryChartView.onAttachedToWindow();
        assertThat(mBatteryChartView.isClickable()).isTrue();
        assertThat(mBatteryChartView.mTrapezoidCurvePaint).isNull();
    }

    @Test
    public void testClickable_accessibilityIsEnabledWithValidId_notClickable() {
        mBatteryChartView.setClickableForce(true);
        when(mPowerUsageFeatureProvider.isChartGraphSlotsEnabled(mContext))
            .thenReturn(true);
        doReturn(true).when(mockAccessibilityManager).isEnabled();

        mBatteryChartView.onAttachedToWindow();
        assertThat(mBatteryChartView.isClickable()).isFalse();
        assertThat(mBatteryChartView.mTrapezoidCurvePaint).isNotNull();
    }

    @Test
    public void testClickable_restoreFromNonClickableState() {
        final int[] levels = new int[13];
        for (int index = 0; index < levels.length; index++) {
            levels[index] = index + 1;
        }
        mBatteryChartView.setTrapezoidCount(12);
        mBatteryChartView.setLevels(levels);
        mBatteryChartView.setClickableForce(true);
        when(mPowerUsageFeatureProvider.isChartGraphSlotsEnabled(mContext))
            .thenReturn(true);
        doReturn(true).when(mockAccessibilityManager).isEnabled();
        mBatteryChartView.onAttachedToWindow();
        // Ensures the testing environment is correct.
        assertThat(mBatteryChartView.isClickable()).isFalse();
        // Turns off accessibility service.
        doReturn(false).when(mockAccessibilityManager).isEnabled();

        mBatteryChartView.onAttachedToWindow();

        assertThat(mBatteryChartView.isClickable()).isTrue();
    }

    @Test
    public void testOnAttachedToWindow_addAccessibilityStateChangeListener() {
        mBatteryChartView.onAttachedToWindow();
        verify(mockAccessibilityManager)
            .addAccessibilityStateChangeListener(mBatteryChartView);
    }

    @Test
    public void testOnDetachedFromWindow_removeAccessibilityStateChangeListener() {
        mBatteryChartView.onAttachedToWindow();
        mBatteryChartView.mHandler.postDelayed(
            mBatteryChartView.mUpdateClickableStateRun, 1000);

        mBatteryChartView.onDetachedFromWindow();

        verify(mockAccessibilityManager)
            .removeAccessibilityStateChangeListener(mBatteryChartView);
        assertThat(mBatteryChartView.mHandler.hasCallbacks(
                mBatteryChartView.mUpdateClickableStateRun))
            .isFalse();
    }

    @Test
    public void testOnAccessibilityStateChanged_postUpdateStateRunnable() {
        mBatteryChartView.mHandler = spy(mBatteryChartView.mHandler);
        mBatteryChartView.onAccessibilityStateChanged(/*enabled=*/ true);

        verify(mBatteryChartView.mHandler)
            .removeCallbacks(mBatteryChartView.mUpdateClickableStateRun);
        verify(mBatteryChartView.mHandler)
            .postDelayed(mBatteryChartView.mUpdateClickableStateRun, 500L);
    }
}
