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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.android.settings.DisplaySettings;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.testutils.BatteryTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Map;
import java.util.Optional;

@RunWith(RobolectricTestRunner.class)
public final class BatteryTipsCardPreferenceTest {

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private BatteryTipsCardPreference mBatteryTipsCardPreference;
    private PowerUsageAdvanced mPowerUsageAdvanced;
    private BatteryTipsController mBatteryTipsController;
    private BatteryChartPreferenceController mBatteryChartPreferenceController;

    @Mock private View mFakeView;
    @Mock private BatteryUsageBreakdownController mBatteryUsageBreakdownController;
    @Mock private BatteryDiffEntry mFakeEntry;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mBatteryTipsCardPreference = new BatteryTipsCardPreference(mContext, /* attrs= */ null);
        mBatteryTipsController = new BatteryTipsController(mContext);
        mBatteryChartPreferenceController =
                spy(new BatteryChartPreferenceController(mContext, null, null));
        mBatteryChartPreferenceController.mPrefContext = mContext;
        mBatteryTipsController.mCardPreference = mBatteryTipsCardPreference;

        mPowerUsageAdvanced = spy(new PowerUsageAdvanced());
        doReturn(mContext).when(mPowerUsageAdvanced).getContext();
        mPowerUsageAdvanced.mBatteryTipsController = mBatteryTipsController;
        mPowerUsageAdvanced.mBatteryChartPreferenceController = mBatteryChartPreferenceController;
        mPowerUsageAdvanced.mBatteryUsageBreakdownController = mBatteryUsageBreakdownController;
        mPowerUsageAdvanced.mBatteryLevelData =
                Optional.of(
                        new BatteryLevelData(
                                Map.of(
                                        1694354400000L, 1, // 2023-09-10 22:00:00
                                        1694361600000L, 2, // 2023-09-11 00:00:00
                                        1694368800000L, 3))); // 2023-09-11 02:00:00
        doReturn("TestEntriesKey").when(mFakeEntry).getKey();
    }

    @Test
    public void constructor_returnExpectedResult() {
        assertThat(mBatteryTipsCardPreference.getLayoutResource())
                .isEqualTo(R.layout.battery_tips_card);
    }

    @Test
    public void onClick_mainBtnOfSettingsAnomaly_getAdaptiveBrightnessLauncher() {
        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        PowerAnomalyEvent adaptiveBrightnessAnomaly =
                BatteryTestUtils.createAdaptiveBrightnessAnomalyEvent();
        when(mFeatureFactory.powerUsageFeatureProvider.isBatteryTipsEnabled()).thenReturn(true);
        when(mFakeView.getId()).thenReturn(R.id.main_button);
        doNothing().when(mContext).startActivity(captor.capture());

        mPowerUsageAdvanced.onDisplayAnomalyEventUpdated(
                adaptiveBrightnessAnomaly, adaptiveBrightnessAnomaly);
        mBatteryTipsCardPreference.onClick(mFakeView);

        assertThat(mBatteryTipsCardPreference.isVisible()).isFalse();
        verify(mContext).startActivity(any(Intent.class));
        final Intent intent = captor.getValue();
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(DisplaySettings.class.getName());
        assertThat(intent.getIntExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY, -1))
                .isEqualTo(SettingsEnums.DISPLAY);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                        SettingsEnums.ACTION_BATTERY_TIPS_CARD_SHOW,
                        SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                        "BrightnessAnomaly",
                        PowerAnomalyKey.KEY_BRIGHTNESS.getNumber());
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                        SettingsEnums.ACTION_BATTERY_TIPS_CARD_ACCEPT,
                        SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                        "BrightnessAnomaly",
                        PowerAnomalyKey.KEY_BRIGHTNESS.getNumber());
    }

    @Test
    public void onClick_dismissBtnOfSettingsAnomaly_cardDismissAndLogged() {
        final PowerAnomalyEvent screenTimeoutAnomaly =
                BatteryTestUtils.createScreenTimeoutAnomalyEvent();
        DatabaseUtils.removeDismissedPowerAnomalyKeys(mContext);
        when(mFeatureFactory.powerUsageFeatureProvider.isBatteryTipsEnabled()).thenReturn(true);
        when(mFakeView.getId()).thenReturn(R.id.dismiss_button);

        mPowerUsageAdvanced.onDisplayAnomalyEventUpdated(
                screenTimeoutAnomaly, screenTimeoutAnomaly);
        mBatteryTipsCardPreference.onClick(mFakeView);

        assertThat(mBatteryTipsCardPreference.isVisible()).isFalse();
        assertThat(DatabaseUtils.getDismissedPowerAnomalyKeys(mContext)).hasSize(1);
        assertThat(DatabaseUtils.getDismissedPowerAnomalyKeys(mContext))
                .contains(PowerAnomalyKey.KEY_SCREEN_TIMEOUT.name());
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                        SettingsEnums.ACTION_BATTERY_TIPS_CARD_SHOW,
                        SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                        "ScreenTimeoutAnomaly",
                        PowerAnomalyKey.KEY_SCREEN_TIMEOUT.getNumber());
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                        SettingsEnums.ACTION_BATTERY_TIPS_CARD_DISMISS,
                        SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                        "ScreenTimeoutAnomaly",
                        PowerAnomalyKey.KEY_SCREEN_TIMEOUT.getNumber());
    }

    @Ignore("b/313582999")
    @Test
    public void onClick_mainBtnOfAppsAnomaly_selectHighlightSlot() {
        final PowerAnomalyEvent appsAnomaly = BatteryTestUtils.createAppAnomalyEvent();
        when(mFeatureFactory.powerUsageFeatureProvider.isBatteryTipsEnabled()).thenReturn(true);
        when(mFakeView.getId()).thenReturn(R.id.main_button);
        doNothing().when(mBatteryChartPreferenceController).selectHighlightSlotIndex();
        when(mPowerUsageAdvanced.findRelatedBatteryDiffEntry(any())).thenReturn(mFakeEntry);

        mPowerUsageAdvanced.onDisplayAnomalyEventUpdated(appsAnomaly, appsAnomaly);
        mBatteryTipsCardPreference.onClick(mFakeView);

        assertThat(mBatteryTipsCardPreference.isVisible()).isFalse();
        verify(mContext, never()).startActivity(any(Intent.class));
        verify(mBatteryChartPreferenceController).onHighlightSlotIndexUpdate(eq(1), eq(0));
        verify(mBatteryChartPreferenceController).selectHighlightSlotIndex();
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                        SettingsEnums.ACTION_BATTERY_TIPS_CARD_SHOW,
                        SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                        "AppAnomaly",
                        PowerAnomalyKey.KEY_APP_TOTAL_HIGHER_THAN_USUAL.getNumber());
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                        SettingsEnums.ACTION_BATTERY_TIPS_CARD_ACCEPT,
                        SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                        "AppAnomaly",
                        PowerAnomalyKey.KEY_APP_TOTAL_HIGHER_THAN_USUAL.getNumber());
    }

    @Ignore("b/313582999")
    @Test
    public void onClick_dismissBtnOfAppsAnomaly_keepHighlightSlotIndex() {
        final PowerAnomalyEvent appsAnomaly = BatteryTestUtils.createAppAnomalyEvent();
        when(mFeatureFactory.powerUsageFeatureProvider.isBatteryTipsEnabled()).thenReturn(true);
        when(mFakeView.getId()).thenReturn(R.id.dismiss_button);
        when(mPowerUsageAdvanced.findRelatedBatteryDiffEntry(any())).thenReturn(mFakeEntry);

        mPowerUsageAdvanced.onDisplayAnomalyEventUpdated(appsAnomaly, appsAnomaly);
        mBatteryTipsCardPreference.onClick(mFakeView);

        assertThat(mBatteryTipsCardPreference.isVisible()).isFalse();
        verify(mContext, never()).startActivity(any(Intent.class));
        verify(mBatteryChartPreferenceController).onHighlightSlotIndexUpdate(eq(1), eq(0));
        verify(mBatteryChartPreferenceController, never()).selectHighlightSlotIndex();
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                        SettingsEnums.ACTION_BATTERY_TIPS_CARD_SHOW,
                        SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                        "AppAnomaly",
                        PowerAnomalyKey.KEY_APP_TOTAL_HIGHER_THAN_USUAL.getNumber());
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                        SettingsEnums.ACTION_BATTERY_TIPS_CARD_DISMISS,
                        SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                        "AppAnomaly",
                        PowerAnomalyKey.KEY_APP_TOTAL_HIGHER_THAN_USUAL.getNumber());
    }
}
