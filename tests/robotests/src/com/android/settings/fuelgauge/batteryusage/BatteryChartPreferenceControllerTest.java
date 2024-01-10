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

import static com.android.settings.fuelgauge.batteryusage.BatteryChartViewModel.SELECTED_INDEX_ALL;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.UserManager;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.SettingsActivity;
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
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
public final class BatteryChartPreferenceControllerTest {
    @Mock private Intent mIntent;
    @Mock private UserManager mUserManager;
    @Mock private SettingsActivity mSettingsActivity;
    @Mock private TextView mChartSummaryTextView;
    @Mock private BatteryChartView mDailyChartView;
    @Mock private BatteryChartView mHourlyChartView;
    @Mock private ViewPropertyAnimator mViewPropertyAnimator;
    @Mock private LinearLayout.LayoutParams mLayoutParams;

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private BatteryChartPreferenceController mBatteryChartPreferenceController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Locale.setDefault(new Locale("en_US"));
        org.robolectric.shadows.ShadowSettings.set24HourTimeFormat(false);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        DataProcessor.sTestSystemAppsPackageNames = Set.of();
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mContext).when(mContext).getApplicationContext();
        doReturn(mUserManager).when(mContext).getSystemService(UserManager.class);
        doReturn(true).when(mUserManager).isUserUnlocked(anyInt());
        doReturn(new int[] {0}).when(mUserManager).getProfileIdsWithDisabled(anyInt());
        final Resources resources = spy(mContext.getResources());
        resources.getConfiguration().setLocales(new LocaleList(new Locale("en_US")));
        doReturn(resources).when(mContext).getResources();
        doReturn(Set.of("com.android.gms.persistent"))
                .when(mFeatureFactory.powerUsageFeatureProvider)
                .getHideApplicationSet();
        doReturn(mLayoutParams).when(mDailyChartView).getLayoutParams();
        doReturn(mIntent).when(mContext).registerReceiver(any(), any());
        doReturn(100).when(mIntent).getIntExtra(eq(BatteryManager.EXTRA_SCALE), anyInt());
        doReturn(66).when(mIntent).getIntExtra(eq(BatteryManager.EXTRA_LEVEL), anyInt());
        setupHourlyChartViewAnimationMock();
        mBatteryChartPreferenceController = createController();
        mBatteryChartPreferenceController.mPrefContext = mContext;
        mBatteryChartPreferenceController.mChartSummaryTextView = mChartSummaryTextView;
        mBatteryChartPreferenceController.mDailyChartView = mDailyChartView;
        mBatteryChartPreferenceController.mHourlyChartView = mHourlyChartView;
        BatteryDiffEntry.clearCache();
        // Adds fake testing data.
        BatteryDiffEntry.sResourceCache.put(
                "fakeBatteryDiffEntryKey",
                new BatteryEntry.NameAndIcon("fakeName", /* icon= */ null, /* iconId= */ 1));
    }

    @Test
    public void onDestroy_activityIsChanging_clearBatteryEntryCache() {
        doReturn(true).when(mSettingsActivity).isChangingConfigurations();
        // Ensures the testing environment is correct.
        assertThat(BatteryDiffEntry.sResourceCache).hasSize(1);

        mBatteryChartPreferenceController.onDestroy();
        assertThat(BatteryDiffEntry.sResourceCache).isEmpty();
    }

    @Test
    public void onDestroy_activityIsNotChanging_notClearBatteryEntryCache() {
        doReturn(false).when(mSettingsActivity).isChangingConfigurations();
        // Ensures the testing environment is correct.
        assertThat(BatteryDiffEntry.sResourceCache).hasSize(1);

        mBatteryChartPreferenceController.onDestroy();
        assertThat(BatteryDiffEntry.sResourceCache).isNotEmpty();
    }

    @Test
    public void setBatteryChartViewModel_6Hours() {
        reset(mDailyChartView);
        reset(mHourlyChartView);
        setupHourlyChartViewAnimationMock();

        mBatteryChartPreferenceController.onBatteryLevelDataUpdate(createBatteryLevelData(6));

        verify(mDailyChartView, atLeastOnce()).setVisibility(View.GONE);
        // Ignore fast refresh ui from the data processor callback.
        verify(mHourlyChartView, atLeast(0)).setViewModel(null);
        verify(mHourlyChartView, atLeastOnce())
                .setViewModel(
                        new BatteryChartViewModel(
                                List.of(100, 99, 97, 95, 66),
                                List.of(
                                        1619247660000L /* 7:01 AM */,
                                        1619251200000L /* 8 AM */,
                                        1619258400000L /* 10 AM */,
                                        1619265600000L /* 12 PM */,
                                        1619265720000L /* now (12:02 PM) */),
                                BatteryChartViewModel.AxisLabelPosition.BETWEEN_TRAPEZOIDS,
                                mBatteryChartPreferenceController.mHourlyChartLabelTextGenerator));
    }

    @Test
    public void setBatteryChartViewModel_60Hours() {
        reset(mDailyChartView);
        reset(mHourlyChartView);
        setupHourlyChartViewAnimationMock();

        BatteryChartViewModel expectedDailyViewModel =
                new BatteryChartViewModel(
                        List.of(100, 83, 59, 66),
                        // "Sat", "Sun", "Mon", "Mon"
                        List.of(
                                1619247660000L /* Sat */,
                                1619308800000L /* Sun */,
                                1619395200000L /* Mon */,
                                1619460120000L /* Mon */),
                        BatteryChartViewModel.AxisLabelPosition.CENTER_OF_TRAPEZOIDS,
                        mBatteryChartPreferenceController.mDailyChartLabelTextGenerator);

        mBatteryChartPreferenceController.onBatteryLevelDataUpdate(createBatteryLevelData(60));

        verify(mDailyChartView, atLeastOnce()).setVisibility(View.VISIBLE);
        verify(mViewPropertyAnimator, atLeastOnce()).alpha(0f);
        verify(mDailyChartView, atLeastOnce()).setViewModel(expectedDailyViewModel);

        reset(mDailyChartView);
        reset(mHourlyChartView);
        setupHourlyChartViewAnimationMock();
        doReturn(mLayoutParams).when(mDailyChartView).getLayoutParams();
        doReturn(View.GONE).when(mHourlyChartView).getVisibility();
        mBatteryChartPreferenceController.mDailyChartIndex = 0;
        mBatteryChartPreferenceController.refreshUi();
        verify(mDailyChartView).setVisibility(View.VISIBLE);
        verify(mHourlyChartView).setVisibility(View.VISIBLE);
        verify(mViewPropertyAnimator, atLeastOnce()).alpha(1f);

        expectedDailyViewModel.setSelectedIndex(0);
        verify(mDailyChartView).setViewModel(expectedDailyViewModel);
        verify(mHourlyChartView)
                .setViewModel(
                        new BatteryChartViewModel(
                                List.of(100, 99, 97, 95, 93, 91, 89, 87, 85, 83),
                                List.of(
                                        1619247660000L /* 7:01 AM */,
                                        1619251200000L /* 8 AM */,
                                        1619258400000L /* 10 AM */,
                                        1619265600000L /* 12 PM */,
                                        1619272800000L /* 2 PM */,
                                        1619280000000L /* 4 PM */,
                                        1619287200000L /* 6 PM */,
                                        1619294400000L /* 8 PM */,
                                        1619301600000L /* 10 PM */,
                                        1619308800000L /* 12 AM */),
                                BatteryChartViewModel.AxisLabelPosition.BETWEEN_TRAPEZOIDS,
                                mBatteryChartPreferenceController.mHourlyChartLabelTextGenerator));

        reset(mDailyChartView);
        reset(mHourlyChartView);
        setupHourlyChartViewAnimationMock();
        doReturn(mLayoutParams).when(mDailyChartView).getLayoutParams();
        mBatteryChartPreferenceController.mDailyChartIndex = 1;
        mBatteryChartPreferenceController.mHourlyChartIndex = 6;
        mBatteryChartPreferenceController.refreshUi();
        verify(mDailyChartView).setVisibility(View.VISIBLE);
        verify(mViewPropertyAnimator, atLeastOnce()).alpha(1f);
        expectedDailyViewModel.setSelectedIndex(1);
        verify(mDailyChartView).setViewModel(expectedDailyViewModel);
        BatteryChartViewModel expectedHourlyViewModel =
                new BatteryChartViewModel(
                        List.of(83, 81, 79, 77, 75, 73, 71, 69, 67, 65, 63, 61, 59),
                        List.of(
                                1619308800000L /* 12 AM */,
                                1619316000000L /* 2 AM */,
                                1619323200000L /* 4 AM */,
                                1619330400000L /* 6 AM */,
                                1619337600000L /* 8 AM */,
                                1619344800000L /* 10 AM */,
                                1619352000000L /* 12 PM */,
                                1619359200000L /* 2 PM */,
                                1619366400000L /* 4 PM */,
                                1619373600000L /* 6 PM */,
                                1619380800000L /* 8 PM */,
                                1619388000000L /* 10 PM */,
                                1619395200000L /* 12 AM */),
                        BatteryChartViewModel.AxisLabelPosition.BETWEEN_TRAPEZOIDS,
                        mBatteryChartPreferenceController.mHourlyChartLabelTextGenerator);
        expectedHourlyViewModel.setSelectedIndex(6);
        verify(mHourlyChartView).setViewModel(expectedHourlyViewModel);

        reset(mDailyChartView);
        reset(mHourlyChartView);
        setupHourlyChartViewAnimationMock();
        doReturn(mLayoutParams).when(mDailyChartView).getLayoutParams();
        mBatteryChartPreferenceController.mDailyChartIndex = 2;
        mBatteryChartPreferenceController.mHourlyChartIndex = SELECTED_INDEX_ALL;
        mBatteryChartPreferenceController.refreshUi();
        verify(mDailyChartView).setVisibility(View.VISIBLE);
        verify(mViewPropertyAnimator, atLeastOnce()).alpha(1f);
        expectedDailyViewModel.setSelectedIndex(2);
        verify(mDailyChartView).setViewModel(expectedDailyViewModel);
        verify(mHourlyChartView)
                .setViewModel(
                        new BatteryChartViewModel(
                                List.of(59, 57, 55, 53, 51, 49, 47, 45, 43, 41, 66),
                                List.of(
                                        1619395200000L /* 12 AM */,
                                        1619402400000L /* 2 AM */,
                                        1619409600000L /* 4 AM */,
                                        1619416800000L /* 6 AM */,
                                        1619424000000L /* 8 AM */,
                                        1619431200000L /* 10 AM */,
                                        1619438400000L /* 12 PM */,
                                        1619445600000L /* 2 PM */,
                                        1619452800000L /* 4 PM */,
                                        1619460000000L /* 6 PM */,
                                        1619460120000L /* now (6:02 PM) */),
                                BatteryChartViewModel.AxisLabelPosition.BETWEEN_TRAPEZOIDS,
                                mBatteryChartPreferenceController.mHourlyChartLabelTextGenerator));
    }

    @Test
    public void onBatteryLevelDataUpdate_oneDay_showHourlyChartOnly() {
        doReturn(View.GONE).when(mHourlyChartView).getVisibility();

        mBatteryChartPreferenceController.onBatteryLevelDataUpdate(createBatteryLevelData(6));

        verify(mChartSummaryTextView).setVisibility(View.VISIBLE);
        verify(mDailyChartView).setVisibility(View.GONE);
        verify(mHourlyChartView).setVisibility(View.VISIBLE);
    }

    @Test
    public void onBatteryLevelDataUpdate_selectAllForMultipleDays_showDailyChartOnly() {
        doReturn(View.GONE).when(mHourlyChartView).getVisibility();

        mBatteryChartPreferenceController.mDailyChartIndex = SELECTED_INDEX_ALL;
        mBatteryChartPreferenceController.onBatteryLevelDataUpdate(createBatteryLevelData(60));

        verify(mChartSummaryTextView).setVisibility(View.VISIBLE);
        verify(mDailyChartView).setVisibility(View.VISIBLE);
        verify(mHourlyChartView, never()).setVisibility(View.VISIBLE);
    }

    @Test
    public void onBatteryLevelDataUpdate_selectOneDayForMultipleDays_showBothCharts() {
        doReturn(View.GONE).when(mHourlyChartView).getVisibility();

        mBatteryChartPreferenceController.mDailyChartIndex = 0;
        mBatteryChartPreferenceController.onBatteryLevelDataUpdate(createBatteryLevelData(60));

        verify(mChartSummaryTextView).setVisibility(View.VISIBLE);
        verify(mDailyChartView).setVisibility(View.VISIBLE);
        verify(mHourlyChartView).setVisibility(View.VISIBLE);
    }

    @Test
    public void onBatteryLevelDataUpdate_batteryLevelDataIsNull_showNoChart() {
        doReturn(View.GONE).when(mHourlyChartView).getVisibility();

        mBatteryChartPreferenceController.onBatteryLevelDataUpdate(null);

        verify(mChartSummaryTextView).setVisibility(View.GONE);
        verify(mDailyChartView).setVisibility(View.GONE);
        verify(mHourlyChartView).setVisibility(View.GONE);
    }

    @Test
    public void showEmptyChart_normalCase_showEmptyChart() {
        doReturn(View.GONE).when(mHourlyChartView).getVisibility();

        mBatteryChartPreferenceController.showEmptyChart();

        verify(mChartSummaryTextView).setVisibility(View.VISIBLE);
        verify(mDailyChartView).setVisibility(View.GONE);
        verify(mHourlyChartView).setVisibility(View.VISIBLE);
    }

    @Test
    public void showEmptyChart_dailyChartViewIsNull_ignoreShowEmptyChart() {
        mBatteryChartPreferenceController.mDailyChartView = null;
        doReturn(View.GONE).when(mHourlyChartView).getVisibility();

        mBatteryChartPreferenceController.showEmptyChart();

        verify(mChartSummaryTextView, never()).setVisibility(View.VISIBLE);
        verify(mDailyChartView, never()).setVisibility(View.GONE);
        verify(mHourlyChartView, never()).setVisibility(View.VISIBLE);
    }

    @Test
    public void showEmptyChart_hourlyChartViewIsNull_ignoreShowEmptyChart() {
        mBatteryChartPreferenceController.mHourlyChartView = null;

        mBatteryChartPreferenceController.showEmptyChart();

        verify(mChartSummaryTextView, never()).setVisibility(View.VISIBLE);
        verify(mDailyChartView, never()).setVisibility(View.GONE);
        verify(mHourlyChartView, never()).setVisibility(View.VISIBLE);
    }

    @Test
    public void refreshUi_dailyChartViewIsNull_ignoreRefresh() {
        mBatteryChartPreferenceController.mDailyChartView = null;

        mBatteryChartPreferenceController.refreshUi();

        verify(mChartSummaryTextView, never()).setVisibility(anyInt());
    }

    @Test
    public void refreshUi_hourlyChartViewIsNull_ignoreRefresh() {
        mBatteryChartPreferenceController.mHourlyChartView = null;

        mBatteryChartPreferenceController.refreshUi();

        verify(mChartSummaryTextView, never()).setVisibility(anyInt());
    }

    @Test
    public void selectedSlotText_selectAllDaysAllHours_returnNull() {
        mBatteryChartPreferenceController.onBatteryLevelDataUpdate(createBatteryLevelData(60));
        mBatteryChartPreferenceController.mDailyChartIndex = SELECTED_INDEX_ALL;
        mBatteryChartPreferenceController.mHourlyChartIndex = SELECTED_INDEX_ALL;

        assertThat(mBatteryChartPreferenceController.getSlotInformation()).isEqualTo(null);
        assertThat(mBatteryChartPreferenceController.getBatteryLevelPercentageInfo())
                .isEqualTo("Battery level percentage from 100% to 66%");
    }

    @Test
    public void selectedSlotText_onlyOneDayDataSelectAllHours_returnNull() {
        mBatteryChartPreferenceController.onBatteryLevelDataUpdate(createBatteryLevelData(6));
        mBatteryChartPreferenceController.mDailyChartIndex = 0;
        mBatteryChartPreferenceController.mHourlyChartIndex = SELECTED_INDEX_ALL;

        assertThat(mBatteryChartPreferenceController.getSlotInformation()).isEqualTo(null);
        assertThat(mBatteryChartPreferenceController.getBatteryLevelPercentageInfo())
                .isEqualTo("Battery level percentage from 100% to 66%");
    }

    @Test
    public void selectedSlotText_selectADayAllHours_onlyDayText() {
        mBatteryChartPreferenceController.onBatteryLevelDataUpdate(createBatteryLevelData(60));
        mBatteryChartPreferenceController.mDailyChartIndex = 1;
        mBatteryChartPreferenceController.mHourlyChartIndex = SELECTED_INDEX_ALL;

        assertThat(mBatteryChartPreferenceController.getSlotInformation()).isEqualTo("Sunday");
        assertThat(mBatteryChartPreferenceController.getBatteryLevelPercentageInfo())
                .isEqualTo("Battery level percentage from 83% to 59%");
    }

    @Test
    public void selectedSlotText_onlyOneDayDataSelectAnHour_onlyHourText() {
        mBatteryChartPreferenceController.onBatteryLevelDataUpdate(createBatteryLevelData(6));
        mBatteryChartPreferenceController.mDailyChartIndex = 0;
        mBatteryChartPreferenceController.mHourlyChartIndex = 2;

        assertThat(mBatteryChartPreferenceController.getSlotInformation())
                .isEqualTo("10 AM - 12 PM");
        assertThat(mBatteryChartPreferenceController.getBatteryLevelPercentageInfo())
                .isEqualTo("Battery level percentage from 97% to 95%");
    }

    @Test
    public void selectedSlotText_SelectADayAnHour_dayAndHourText() {
        mBatteryChartPreferenceController.onBatteryLevelDataUpdate(createBatteryLevelData(60));
        mBatteryChartPreferenceController.mDailyChartIndex = 1;
        mBatteryChartPreferenceController.mHourlyChartIndex = 8;

        assertThat(mBatteryChartPreferenceController.getSlotInformation())
                .isEqualTo("Sunday 4 PM - 6 PM");
        assertThat(mBatteryChartPreferenceController.getBatteryLevelPercentageInfo())
                .isEqualTo("Battery level percentage from 67% to 65%");
    }

    @Test
    public void selectedSlotText_selectFirstSlot_withMinuteText() {
        mBatteryChartPreferenceController.onBatteryLevelDataUpdate(createBatteryLevelData(6));
        mBatteryChartPreferenceController.mDailyChartIndex = 0;
        mBatteryChartPreferenceController.mHourlyChartIndex = 0;

        assertThat(mBatteryChartPreferenceController.getSlotInformation())
                .isEqualTo("7:01 AM - 8 AM");
        assertThat(mBatteryChartPreferenceController.getBatteryLevelPercentageInfo())
                .isEqualTo("Battery level percentage from 100% to 99%");
    }

    @Test
    public void selectedSlotText_selectLastSlot_withNowText() {
        mBatteryChartPreferenceController.onBatteryLevelDataUpdate(createBatteryLevelData(6));
        mBatteryChartPreferenceController.mDailyChartIndex = 0;
        mBatteryChartPreferenceController.mHourlyChartIndex = 3;

        assertThat(mBatteryChartPreferenceController.getSlotInformation()).isEqualTo("12 PM - now");
        assertThat(mBatteryChartPreferenceController.getBatteryLevelPercentageInfo())
                .isEqualTo("Battery level percentage from 95% to 66%");
    }

    @Test
    public void selectedSlotText_selectOnlySlot_withMinuteAndNowText() {
        mBatteryChartPreferenceController.onBatteryLevelDataUpdate(createBatteryLevelData(1));
        mBatteryChartPreferenceController.mDailyChartIndex = 0;
        mBatteryChartPreferenceController.mHourlyChartIndex = 0;

        assertThat(mBatteryChartPreferenceController.getSlotInformation())
                .isEqualTo("7:01 AM - now");
        assertThat(mBatteryChartPreferenceController.getBatteryLevelPercentageInfo())
                .isEqualTo("Battery level percentage from 100% to 66%");
    }

    @Test
    public void onSaveInstanceState_restoreSelectedIndexAndExpandState() {
        final int expectedDailyIndex = 1;
        final int expectedHourlyIndex = 2;
        final Bundle bundle = new Bundle();
        mBatteryChartPreferenceController.mDailyChartIndex = expectedDailyIndex;
        mBatteryChartPreferenceController.mHourlyChartIndex = expectedHourlyIndex;
        mBatteryChartPreferenceController.onSaveInstanceState(bundle);
        // Replaces the original controller with other values.
        mBatteryChartPreferenceController.mDailyChartIndex = -1;
        mBatteryChartPreferenceController.mHourlyChartIndex = -1;

        mBatteryChartPreferenceController.onCreate(bundle);
        mBatteryChartPreferenceController.onBatteryLevelDataUpdate(createBatteryLevelData(25));

        assertThat(mBatteryChartPreferenceController.mDailyChartIndex)
                .isEqualTo(expectedDailyIndex);
        assertThat(mBatteryChartPreferenceController.mHourlyChartIndex)
                .isEqualTo(expectedHourlyIndex);
    }

    @Test
    public void getTotalHours_getExpectedResult() {
        BatteryLevelData batteryLevelData = createBatteryLevelData(60);

        final int totalHour = BatteryChartPreferenceController.getTotalHours(batteryLevelData);

        // Only calculate the even hours.
        assertThat(totalHour).isEqualTo(59);
    }

    private static Long generateTimestamp(int index) {
        // "2021-04-23 07:00:00 UTC" + index hours
        return 1619247600000L + index * DateUtils.HOUR_IN_MILLIS;
    }

    private static BatteryLevelData createBatteryLevelData(int numOfHours) {
        Map<Long, Integer> batteryLevelMap = new ArrayMap<>();
        for (int index = 0; index < numOfHours; index += 2) {
            final Integer level = 100 - index;
            Long timestamp = generateTimestamp(index);
            if (index == 0) {
                timestamp += DateUtils.MINUTE_IN_MILLIS;
                index--;
            }
            batteryLevelMap.put(timestamp, level);
        }
        long current = generateTimestamp(numOfHours - 1) + DateUtils.MINUTE_IN_MILLIS * 2;
        batteryLevelMap.put(current, 66);
        DataProcessor.sTestCurrentTimeMillis = current;
        return new BatteryLevelData(batteryLevelMap);
    }

    private BatteryChartPreferenceController createController() {
        final BatteryChartPreferenceController controller =
                new BatteryChartPreferenceController(
                        mContext, /* lifecycle= */ null, mSettingsActivity);
        controller.mPrefContext = mContext;
        return controller;
    }

    private void setupHourlyChartViewAnimationMock() {
        doReturn(mViewPropertyAnimator).when(mHourlyChartView).animate();
        doReturn(mViewPropertyAnimator).when(mViewPropertyAnimator).alpha(anyFloat());
        doReturn(mViewPropertyAnimator).when(mViewPropertyAnimator).setDuration(anyLong());
        doReturn(mViewPropertyAnimator).when(mViewPropertyAnimator).setListener(any());
    }
}
