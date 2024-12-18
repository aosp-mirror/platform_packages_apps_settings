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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.os.LocaleList;

import com.android.settings.R;
import com.android.settings.testutils.BatteryTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.widget.TipCardPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Locale;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
public final class BatteryTipsControllerTest {

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private BatteryTipsController mBatteryTipsController;
    private TipCardPreference mCardPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Locale.setDefault(new Locale("en_US"));
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        mContext = spy(RuntimeEnvironment.application);
        final Resources resources = spy(mContext.getResources());
        resources.getConfiguration().setLocales(new LocaleList(new Locale("en_US")));
        doReturn(resources).when(mContext).getResources();
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mBatteryTipsController = spy(new BatteryTipsController(mContext));
        mCardPreference = new TipCardPreference(mContext);
        mBatteryTipsController.mCardPreference = mCardPreference;
    }

    @Test
    public void handleBatteryTipsCardUpdated_null_hidePreference() {
        mBatteryTipsController.handleBatteryTipsCardUpdated(/* powerAnomalyEvents= */ null, false);

        assertThat(mCardPreference.isVisible()).isFalse();
    }

    @Test
    public void handleBatteryTipsCardUpdated_adaptiveBrightnessAnomaly_showAnomaly() {
        AnomalyEventWrapper anomalyEventWrapper =
                spy(
                        new AnomalyEventWrapper(
                                mContext,
                                BatteryTestUtils.createAdaptiveBrightnessAnomalyEvent(true)));
        when(mFeatureFactory.powerUsageFeatureProvider.isBatteryTipsEnabled()).thenReturn(true);

        mBatteryTipsController.handleBatteryTipsCardUpdated(anomalyEventWrapper, false);

        assertThat(mCardPreference.getTitle())
                .isEqualTo("Turn on adaptive brightness to extend battery life");
        assertThat(mCardPreference.getPrimaryButtonText()).isEqualTo("Got it");
        assertThat(mCardPreference.getSecondaryButtonText()).isEqualTo("View Settings");
        assertThat(mCardPreference.getIconResId()).isEqualTo(R.drawable.ic_battery_tips_lightbulb);
        assertThat(mCardPreference.getTintColorResId()).isEqualTo(R.color.color_accent_selector);
        assertThat(mCardPreference.getPrimaryButtonVisibility()).isTrue();
        assertThat(mCardPreference.getSecondaryButtonVisibility()).isTrue();
        assertCardButtonActionAndMetrics(anomalyEventWrapper);
    }

    @Test
    public void handleBatteryTipsCardUpdated_screenTimeoutAnomaly_showAnomaly() {
        AnomalyEventWrapper anomalyEventWrapper =
                spy(
                        new AnomalyEventWrapper(
                                mContext, BatteryTestUtils.createScreenTimeoutAnomalyEvent(true)));
        when(mFeatureFactory.powerUsageFeatureProvider.isBatteryTipsEnabled()).thenReturn(true);

        mBatteryTipsController.handleBatteryTipsCardUpdated(anomalyEventWrapper, false);

        assertThat(mCardPreference.getTitle())
                .isEqualTo("Reduce screen timeout to extend battery life");
        assertThat(mCardPreference.getPrimaryButtonText()).isEqualTo("Got it");
        assertThat(mCardPreference.getSecondaryButtonText()).isEqualTo("View Settings");
        assertThat(mCardPreference.getIconResId()).isEqualTo(R.drawable.ic_battery_tips_lightbulb);
        assertThat(mCardPreference.getTintColorResId()).isEqualTo(R.color.color_accent_selector);
        assertThat(mCardPreference.getPrimaryButtonVisibility()).isTrue();
        assertThat(mCardPreference.getSecondaryButtonVisibility()).isTrue();
        assertCardButtonActionAndMetrics(anomalyEventWrapper);
    }

    @Test
    public void handleBatteryTipsCardUpdated_screenTimeoutAnomalyHasTitle_showAnomaly() {
        PowerAnomalyEvent anomalyEvent = BatteryTestUtils.createScreenTimeoutAnomalyEvent(true);
        String testTitle = "TestTitle";
        anomalyEvent =
                anomalyEvent.toBuilder()
                        .setWarningBannerInfo(
                                anomalyEvent.getWarningBannerInfo().toBuilder()
                                        .setTitleString(testTitle)
                                        .build())
                        .build();
        AnomalyEventWrapper anomalyEventWrapper =
                spy(new AnomalyEventWrapper(mContext, anomalyEvent));
        when(mFeatureFactory.powerUsageFeatureProvider.isBatteryTipsEnabled()).thenReturn(true);

        mBatteryTipsController.handleBatteryTipsCardUpdated(anomalyEventWrapper, false);

        assertThat(mCardPreference.getTitle()).isEqualTo(testTitle);
        assertThat(mCardPreference.getPrimaryButtonText()).isEqualTo("Got it");
        assertThat(mCardPreference.getSecondaryButtonText()).isEqualTo("View Settings");
        assertThat(mCardPreference.getIconResId()).isEqualTo(R.drawable.ic_battery_tips_lightbulb);
        assertThat(mCardPreference.getTintColorResId()).isEqualTo(R.color.color_accent_selector);
        assertThat(mCardPreference.getPrimaryButtonVisibility()).isTrue();
        assertThat(mCardPreference.getSecondaryButtonVisibility()).isTrue();
        assertCardButtonActionAndMetrics(anomalyEventWrapper);
    }

    @Test
    public void handleBatteryTipsCardUpdated_appAnomaly_showAnomaly() {
        AnomalyEventWrapper anomalyEventWrapper =
                spy(new AnomalyEventWrapper(mContext, BatteryTestUtils.createAppAnomalyEvent()));
        when(mFeatureFactory.powerUsageFeatureProvider.isBatteryTipsEnabled()).thenReturn(true);

        anomalyEventWrapper.setRelatedBatteryDiffEntry(
                new BatteryDiffEntry(mContext, "", "Chrome", 0));
        mBatteryTipsController.setOnAnomalyConfirmListener(
                () -> mBatteryTipsController.acceptTipsCard());
        mBatteryTipsController.handleBatteryTipsCardUpdated(anomalyEventWrapper, true);

        assertThat(mCardPreference.getTitle()).isEqualTo("Chrome used more battery than usual");
        assertThat(mCardPreference.getPrimaryButtonText()).isEqualTo("Got it");
        assertThat(mCardPreference.getSecondaryButtonText()).isEqualTo("Check");
        assertThat(mCardPreference.getIconResId())
                .isEqualTo(R.drawable.ic_battery_tips_warning_icon);
        assertThat(mCardPreference.getTintColorResId())
                .isEqualTo(R.color.color_battery_anomaly_app_warning_selector);
        assertThat(mCardPreference.getPrimaryButtonVisibility()).isTrue();
        assertThat(mCardPreference.getSecondaryButtonVisibility()).isTrue();
        assertThat(mCardPreference.isVisible()).isTrue();
        assertCardButtonActionAndMetrics(anomalyEventWrapper);
    }

    private void assertCardButtonActionAndMetrics(final AnomalyEventWrapper anomalyEventWrapper) {
        when(anomalyEventWrapper.updateSystemSettingsIfAvailable()).thenReturn(true);

        final int powerAnomalyKeyNumber = anomalyEventWrapper.getAnomalyKeyNumber();
        assertCardMetrics(SettingsEnums.ACTION_BATTERY_TIPS_CARD_SHOW, powerAnomalyKeyNumber);
        assertThat(mCardPreference.isVisible()).isTrue();

        // Check accept button action
        mCardPreference.setVisible(true);
        mCardPreference.getSecondaryButtonAction().invoke();
        assertCardMetrics(SettingsEnums.ACTION_BATTERY_TIPS_CARD_ACCEPT, powerAnomalyKeyNumber);
        assertThat(mCardPreference.isVisible()).isFalse();
        final boolean isAppAnomalyCard = powerAnomalyKeyNumber > 1;
        verify(anomalyEventWrapper, isAppAnomalyCard ? never() : times(1))
                .updateSystemSettingsIfAvailable();

        // Check reject button action
        mCardPreference.setVisible(true);
        mCardPreference.getPrimaryButtonAction().invoke();
        assertCardMetrics(SettingsEnums.ACTION_BATTERY_TIPS_CARD_DISMISS, powerAnomalyKeyNumber);
        assertThat(mCardPreference.isVisible()).isFalse();
    }

    private void assertCardMetrics(final int action, final int powerAnomalyKeyNumber) {
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                        action,
                        SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                        BatteryTipsController.ANOMALY_KEY,
                        powerAnomalyKeyNumber);
    }
}
