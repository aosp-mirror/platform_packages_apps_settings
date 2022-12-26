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

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.UserManager;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.LinearLayout;

import com.android.settings.SettingsActivity;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
public final class BatteryChartPreferenceControllerTest {
    @Mock
    private Intent mIntent;
    @Mock
    private UserManager mUserManager;
    @Mock
    private SettingsActivity mSettingsActivity;
    @Mock
    private BatteryChartView mDailyChartView;
    @Mock
    private BatteryChartView mHourlyChartView;
    @Mock
    private ViewPropertyAnimator mViewPropertyAnimator;
    @Mock
    private LinearLayout.LayoutParams mLayoutParams;

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private BatteryChartPreferenceController mBatteryChartPreferenceController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Locale.setDefault(new Locale("en_US"));
        org.robolectric.shadows.ShadowSettings.set24HourTimeFormat(false);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mContext).when(mContext).getApplicationContext();
        doReturn(mUserManager)
                .when(mContext)
                .getSystemService(UserManager.class);
        doReturn(true).when(mUserManager).isUserUnlocked(anyInt());
        final Resources resources = spy(mContext.getResources());
        resources.getConfiguration().setLocales(new LocaleList(new Locale("en_US")));
        doReturn(resources).when(mContext).getResources();
        doReturn(Set.of("com.android.gms.persistent"))
                .when(mFeatureFactory.powerUsageFeatureProvider)
                .getHideApplicationSet(mContext);
        doReturn(mLayoutParams).when(mDailyChartView).getLayoutParams();
        doReturn(mIntent).when(mContext).registerReceiver(any(), any());
        doReturn(100).when(mIntent).getIntExtra(eq(BatteryManager.EXTRA_SCALE), anyInt());
        doReturn(66).when(mIntent).getIntExtra(eq(BatteryManager.EXTRA_LEVEL), anyInt());
        setupHourlyChartViewAnimationMock();
        mBatteryChartPreferenceController = createController();
        mBatteryChartPreferenceController.mPrefContext = mContext;
        mBatteryChartPreferenceController.mDailyChartView = mDailyChartView;
        mBatteryChartPreferenceController.mHourlyChartView = mHourlyChartView;
        // Adds fake testing data.
        BatteryDiffEntry.sResourceCache.put(
                "fakeBatteryDiffEntryKey",
                new BatteryEntry.NameAndIcon("fakeName", /*icon=*/ null, /*iconId=*/ 1));
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

        mBatteryChartPreferenceController.setBatteryHistoryMap(createBatteryHistoryMap(6));

        verify(mDailyChartView, atLeastOnce()).setVisibility(View.GONE);
        // Ignore fast refresh ui from the data processor callback.
        verify(mHourlyChartView, atLeast(0)).setViewModel(null);
        verify(mHourlyChartView, atLeastOnce()).setViewModel(new BatteryChartViewModel(
                List.of(100, 97, 95, 66),
                List.of(1619251200000L /* 8 AM */,
                        1619258400000L /* 10 AM */,
                        1619265600000L /* 12 PM */,
                        1619272800000L /* 2 PM */),
                BatteryChartViewModel.AxisLabelPosition.BETWEEN_TRAPEZOIDS,
                mBatteryChartPreferenceController.mHourlyChartLabelTextGenerator));
    }

    @Test
    public void setBatteryChartViewModel_60Hours() {
        reset(mDailyChartView);
        reset(mHourlyChartView);
        setupHourlyChartViewAnimationMock();

        BatteryChartViewModel expectedDailyViewModel = new BatteryChartViewModel(
                List.of(100, 83, 59, 66),
                // "Sat", "Sun", "Mon", "Mon"
                List.of(1619251200000L /* Sat */,
                        1619308800000L /* Sun */,
                        1619395200000L /* Mon */,
                        1619467200000L /* Mon */),
                BatteryChartViewModel.AxisLabelPosition.CENTER_OF_TRAPEZOIDS,
                mBatteryChartPreferenceController.mDailyChartLabelTextGenerator);

        mBatteryChartPreferenceController.setBatteryHistoryMap(createBatteryHistoryMap(60));

        verify(mDailyChartView, atLeastOnce()).setVisibility(View.VISIBLE);
        verify(mViewPropertyAnimator, atLeastOnce()).alpha(0f);
        verify(mDailyChartView).setViewModel(expectedDailyViewModel);

        reset(mDailyChartView);
        reset(mHourlyChartView);
        setupHourlyChartViewAnimationMock();
        doReturn(mLayoutParams).when(mDailyChartView).getLayoutParams();
        mBatteryChartPreferenceController.mDailyChartIndex = 0;
        mBatteryChartPreferenceController.refreshUi();
        verify(mDailyChartView).setVisibility(View.VISIBLE);
        verify(mHourlyChartView).setVisibility(View.VISIBLE);
        verify(mViewPropertyAnimator, atLeastOnce()).alpha(1f);

        expectedDailyViewModel.setSelectedIndex(0);
        verify(mDailyChartView).setViewModel(expectedDailyViewModel);
        verify(mHourlyChartView).setViewModel(new BatteryChartViewModel(
                List.of(100, 97, 95, 93, 91, 89, 87, 85, 83),
                List.of(1619251200000L /* 8 AM */,
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
        BatteryChartViewModel expectedHourlyViewModel = new BatteryChartViewModel(
                List.of(83, 81, 79, 77, 75, 73, 71, 69, 67, 65, 63, 61, 59),
                List.of(1619308800000L /* 12 AM */,
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
        mBatteryChartPreferenceController.mHourlyChartIndex =
                BatteryChartViewModel.SELECTED_INDEX_ALL;
        mBatteryChartPreferenceController.refreshUi();
        verify(mDailyChartView).setVisibility(View.VISIBLE);
        verify(mViewPropertyAnimator, atLeastOnce()).alpha(1f);
        expectedDailyViewModel.setSelectedIndex(2);
        verify(mDailyChartView).setViewModel(expectedDailyViewModel);
        verify(mHourlyChartView).setViewModel(new BatteryChartViewModel(
                List.of(59, 57, 55, 53, 51, 49, 47, 45, 43, 41, 66),
                List.of(1619395200000L /* 12 AM */,
                        1619402400000L /* 2 AM */,
                        1619409600000L /* 4 AM */,
                        1619416800000L /* 6 AM */,
                        1619424000000L /* 8 AM */,
                        1619431200000L /* 10 AM */,
                        1619438400000L /* 12 PM */,
                        1619445600000L /* 2 PM */,
                        1619452800000L /* 4 PM */,
                        1619460000000L /* 6 PM */,
                        1619467200000L /* 8 PM */),
                BatteryChartViewModel.AxisLabelPosition.BETWEEN_TRAPEZOIDS,
                mBatteryChartPreferenceController.mHourlyChartLabelTextGenerator));

    }

    @Test
    public void refreshUi_normalCase_returnTrue() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(createBatteryHistoryMap(6));
        assertThat(mBatteryChartPreferenceController.refreshUi()).isTrue();
    }

    @Test
    public void refreshUi_batteryIndexedMapIsNull_returnTrue() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(null);
        assertThat(mBatteryChartPreferenceController.refreshUi()).isTrue();
    }

    @Test
    public void refreshUi_dailyChartViewIsNull_ignoreRefresh() {
        mBatteryChartPreferenceController.mDailyChartView = null;
        assertThat(mBatteryChartPreferenceController.refreshUi()).isFalse();
    }

    @Test
    public void refreshUi_hourlyChartViewIsNull_ignoreRefresh() {
        mBatteryChartPreferenceController.mHourlyChartView = null;
        assertThat(mBatteryChartPreferenceController.refreshUi()).isFalse();
    }

    @Test
    public void selectedSlotText_selectAllDaysAllHours_returnNull() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(createBatteryHistoryMap(60));
        mBatteryChartPreferenceController.mDailyChartIndex =
                BatteryChartViewModel.SELECTED_INDEX_ALL;
        mBatteryChartPreferenceController.mHourlyChartIndex =
                BatteryChartViewModel.SELECTED_INDEX_ALL;

        assertThat(mBatteryChartPreferenceController.getSlotInformation()).isEqualTo(null);
    }

    @Test
    public void selectedSlotText_onlyOneDayDataSelectAllHours_returnNull() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(createBatteryHistoryMap(6));
        mBatteryChartPreferenceController.mDailyChartIndex = 0;
        mBatteryChartPreferenceController.mHourlyChartIndex =
                BatteryChartViewModel.SELECTED_INDEX_ALL;

        assertThat(mBatteryChartPreferenceController.getSlotInformation()).isEqualTo(null);
    }

    @Test
    public void selectedSlotText_selectADayAllHours_onlyDayText() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(createBatteryHistoryMap(60));
        mBatteryChartPreferenceController.mDailyChartIndex = 1;
        mBatteryChartPreferenceController.mHourlyChartIndex =
                BatteryChartViewModel.SELECTED_INDEX_ALL;

        assertThat(mBatteryChartPreferenceController.getSlotInformation()).isEqualTo("Sunday");
    }

    @Test
    public void selectedSlotText_onlyOneDayDataSelectAnHour_onlyHourText() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(createBatteryHistoryMap(6));
        mBatteryChartPreferenceController.mDailyChartIndex = 0;
        mBatteryChartPreferenceController.mHourlyChartIndex = 1;

        assertThat(mBatteryChartPreferenceController.getSlotInformation()).isEqualTo(
                "10 AM - 12 PM");
    }

    @Test
    public void selectedSlotText_SelectADayAnHour_dayAndHourText() {
        mBatteryChartPreferenceController.setBatteryHistoryMap(createBatteryHistoryMap(60));
        mBatteryChartPreferenceController.mDailyChartIndex = 1;
        mBatteryChartPreferenceController.mHourlyChartIndex = 8;

        assertThat(mBatteryChartPreferenceController.getSlotInformation()).isEqualTo(
                "Sunday 4 PM - 6 PM");
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
        mBatteryChartPreferenceController.setBatteryHistoryMap(createBatteryHistoryMap(25));

        assertThat(mBatteryChartPreferenceController.mDailyChartIndex)
                .isEqualTo(expectedDailyIndex);
        assertThat(mBatteryChartPreferenceController.mHourlyChartIndex)
                .isEqualTo(expectedHourlyIndex);
    }

    @Test
    public void getTotalHours_getExpectedResult() {
        Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap = createBatteryHistoryMap(60);
        BatteryLevelData batteryLevelData =
                DataProcessManager.getBatteryLevelData(mContext, null, batteryHistoryMap, null);

        final int totalHour = BatteryChartPreferenceController.getTotalHours(batteryLevelData);

        // Only calculate the even hours.
        assertThat(totalHour).isEqualTo(60);
    }

    private static Long generateTimestamp(int index) {
        // "2021-04-23 07:00:00 UTC" + index hours
        return 1619247600000L + index * DateUtils.HOUR_IN_MILLIS;
    }

    private static Map<Long, Map<String, BatteryHistEntry>> createBatteryHistoryMap(
            int numOfHours) {
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap = new HashMap<>();
        for (int index = 0; index < numOfHours; index++) {
            final ContentValues values = new ContentValues();
            final DeviceBatteryState deviceBatteryState =
                    DeviceBatteryState
                            .newBuilder()
                            .setBatteryLevel(100 - index)
                            .build();
            final BatteryInformation batteryInformation =
                    BatteryInformation
                            .newBuilder()
                            .setDeviceBatteryState(deviceBatteryState)
                            .setConsumePower(100 - index)
                            .build();
            values.put(BatteryHistEntry.KEY_BATTERY_INFORMATION,
                    ConvertUtils.convertBatteryInformationToString(batteryInformation));
            values.put(BatteryHistEntry.KEY_PACKAGE_NAME, "package" + index);
            final BatteryHistEntry entry = new BatteryHistEntry(values);
            final Map<String, BatteryHistEntry> entryMap = new HashMap<>();
            entryMap.put("fake_entry_key" + index, entry);
            batteryHistoryMap.put(generateTimestamp(index), entryMap);
        }
        DataProcessor.sFakeCurrentTimeMillis =
                generateTimestamp(numOfHours - 1) + DateUtils.MINUTE_IN_MILLIS;
        return batteryHistoryMap;
    }

    private BatteryChartPreferenceController createController() {
        final BatteryChartPreferenceController controller =
                new BatteryChartPreferenceController(
                        mContext, /*lifecycle=*/ null, mSettingsActivity);
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
