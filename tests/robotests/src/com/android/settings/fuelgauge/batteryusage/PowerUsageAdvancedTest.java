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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.util.Pair;

import com.android.settings.testutils.BatteryTestUtils;
import com.android.settings.testutils.shadow.ShadowDashboardFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Predicate;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowDashboardFragment.class)
public final class PowerUsageAdvancedTest {

    private Context mContext;
    private PowerUsageAdvanced mPowerUsageAdvanced;

    private Predicate<PowerAnomalyEvent> mCardFilterPredicate;
    private Predicate<PowerAnomalyEvent> mSlotFilterPredicate;

    @Mock private BatteryTipsController mBatteryTipsController;
    @Mock private BatteryChartPreferenceController mBatteryChartPreferenceController;
    @Mock private ScreenOnTimeController mScreenOnTimeController;
    @Mock private BatteryUsageBreakdownController mBatteryUsageBreakdownController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
        mContext = spy(RuntimeEnvironment.application);

        mPowerUsageAdvanced = spy(new PowerUsageAdvanced());
        mPowerUsageAdvanced.mBatteryTipsController = mBatteryTipsController;
        mPowerUsageAdvanced.mBatteryChartPreferenceController = mBatteryChartPreferenceController;
        mPowerUsageAdvanced.mScreenOnTimeController = mScreenOnTimeController;
        mPowerUsageAdvanced.mBatteryUsageBreakdownController = mBatteryUsageBreakdownController;
        mPowerUsageAdvanced.mBatteryLevelData =
                Optional.of(
                        new BatteryLevelData(
                                Map.of(
                                        1694354400000L, 1, // 2023-09-10 22:00:00
                                        1694361600000L, 2, // 2023-09-11 00:00:00
                                        1694368800000L, 3))); // 2023-09-11 02:00:00
        doReturn(mContext).when(mPowerUsageAdvanced).getContext();
        mSlotFilterPredicate = PowerAnomalyEvent::hasWarningItemInfo;
    }

    @Test
    public void getFilterAnomalyEvent_withEmptyOrNullList_getNull() {
        prepareCardFilterPredicate(null);
        assertThat(PowerUsageAdvanced.getAnomalyEvent(null, mCardFilterPredicate)).isNull();
        assertThat(PowerUsageAdvanced.getAnomalyEvent(null, mSlotFilterPredicate)).isNull();
        assertThat(
                        PowerUsageAdvanced.getAnomalyEvent(
                                BatteryTestUtils.createEmptyPowerAnomalyEventList(),
                                mCardFilterPredicate))
                .isNull();
        assertThat(
                        PowerUsageAdvanced.getAnomalyEvent(
                                BatteryTestUtils.createEmptyPowerAnomalyEventList(),
                                mSlotFilterPredicate))
                .isNull();
    }

    @Test
    public void getFilterAnomalyEvent_withoutDismissed_getHighestScoreEvent() {
        final PowerAnomalyEventList powerAnomalyEventList =
                BatteryTestUtils.createNonEmptyPowerAnomalyEventList();

        final PowerAnomalyEvent slotEvent =
                PowerUsageAdvanced.getAnomalyEvent(powerAnomalyEventList, mSlotFilterPredicate);
        prepareCardFilterPredicate(slotEvent);
        final PowerAnomalyEvent cardEvent =
                PowerUsageAdvanced.getAnomalyEvent(powerAnomalyEventList, mCardFilterPredicate);

        assertThat(cardEvent).isEqualTo(BatteryTestUtils.createAdaptiveBrightnessAnomalyEvent());
        assertThat(slotEvent).isNull();
    }

    @Test
    public void getFilterAnomalyEvent_withBrightnessDismissed_getScreenTimeout() {
        final PowerAnomalyEventList powerAnomalyEventList =
                BatteryTestUtils.createNonEmptyPowerAnomalyEventList();
        DatabaseUtils.removeDismissedPowerAnomalyKeys(mContext);
        DatabaseUtils.setDismissedPowerAnomalyKeys(mContext, PowerAnomalyKey.KEY_BRIGHTNESS.name());

        final PowerAnomalyEvent slotEvent =
                PowerUsageAdvanced.getAnomalyEvent(powerAnomalyEventList, mSlotFilterPredicate);
        prepareCardFilterPredicate(slotEvent);
        final PowerAnomalyEvent cardEvent =
                PowerUsageAdvanced.getAnomalyEvent(powerAnomalyEventList, mCardFilterPredicate);

        assertThat(cardEvent).isEqualTo(BatteryTestUtils.createScreenTimeoutAnomalyEvent());
        assertThat(slotEvent).isNull();
    }

    @Test
    public void getFilterAnomalyEvent_withAllDismissed_getNull() {
        final PowerAnomalyEventList powerAnomalyEventList =
                BatteryTestUtils.createNonEmptyPowerAnomalyEventList();
        DatabaseUtils.removeDismissedPowerAnomalyKeys(mContext);
        for (PowerAnomalyKey key : PowerAnomalyKey.values()) {
            DatabaseUtils.setDismissedPowerAnomalyKeys(mContext, key.name());
        }

        final PowerAnomalyEvent slotEvent =
                PowerUsageAdvanced.getAnomalyEvent(powerAnomalyEventList, mSlotFilterPredicate);
        prepareCardFilterPredicate(slotEvent);
        final PowerAnomalyEvent cardEvent =
                PowerUsageAdvanced.getAnomalyEvent(powerAnomalyEventList, mCardFilterPredicate);

        assertThat(cardEvent).isNull();
        assertThat(slotEvent).isNull();
    }

    @Test
    public void onDisplayAnomalyEventUpdated_withSettingsAnomalyEvent_skipHighlightSlotEffect() {
        final PowerAnomalyEvent event = BatteryTestUtils.createAdaptiveBrightnessAnomalyEvent();

        mPowerUsageAdvanced.onDisplayAnomalyEventUpdated(event, event);

        assertThat(mPowerUsageAdvanced.mHighlightEventWrapper.get().getEventId())
                .isEqualTo(event.getEventId());
        verify(mPowerUsageAdvanced.mBatteryTipsController).setOnAnomalyConfirmListener(isNull());
        verify(mPowerUsageAdvanced.mBatteryTipsController).setOnAnomalyRejectListener(isNull());
        verify(mPowerUsageAdvanced.mBatteryChartPreferenceController)
                .onHighlightSlotIndexUpdate(
                        eq(BatteryChartViewModel.SELECTED_INDEX_INVALID),
                        eq(BatteryChartViewModel.SELECTED_INDEX_INVALID));
    }

    @Test
    public void onDisplayAnomalyEventUpdated_onlyAppAnomalyEvent_setHighlightSlotEffect() {
        final PowerAnomalyEvent event = BatteryTestUtils.createAppAnomalyEvent();

        mPowerUsageAdvanced.onDisplayAnomalyEventUpdated(event, event);

        assertThat(mPowerUsageAdvanced.mHighlightEventWrapper.get().getEventId())
                .isEqualTo(event.getEventId());
        verify(mBatteryTipsController).setOnAnomalyConfirmListener(isNull());
        verify(mBatteryTipsController).setOnAnomalyRejectListener(isNull());
        assertThat(
                        mPowerUsageAdvanced
                                .mBatteryLevelData
                                .get()
                                .getIndexByTimestamps(
                                        event.getWarningItemInfo().getStartTimestamp(),
                                        event.getWarningItemInfo().getEndTimestamp()))
                .isEqualTo(Pair.create(1, 0));
        verify(mBatteryChartPreferenceController).onHighlightSlotIndexUpdate(eq(1), eq(0));
        verify(mBatteryTipsController).setOnAnomalyConfirmListener(notNull());
    }

    @Test
    public void onDisplayAnomalyEventUpdated_withSettingsCardAndAppsSlotEvent_showExpected() {
        final PowerAnomalyEvent settingsEvent =
                BatteryTestUtils.createAdaptiveBrightnessAnomalyEvent();
        final PowerAnomalyEvent appsEvent = BatteryTestUtils.createAppAnomalyEvent();

        mPowerUsageAdvanced.onDisplayAnomalyEventUpdated(settingsEvent, appsEvent);

        assertThat(mPowerUsageAdvanced.mHighlightEventWrapper.get().getEventId())
                .isEqualTo(appsEvent.getEventId());
        verify(mBatteryChartPreferenceController).onHighlightSlotIndexUpdate(eq(1), eq(0));
        verify(mBatteryTipsController).setOnAnomalyConfirmListener(isNull());
        verify(mBatteryTipsController).setOnAnomalyRejectListener(isNull());
    }

    private void prepareCardFilterPredicate(PowerAnomalyEvent slotEvent) {
        final Set<String> dismissedPowerAnomalyKeys =
                DatabaseUtils.getDismissedPowerAnomalyKeys(mContext);
        mCardFilterPredicate =
                event ->
                        !dismissedPowerAnomalyKeys.contains(event.getDismissRecordKey())
                                && (event.equals(slotEvent) || !event.hasWarningItemInfo());
    }
}
