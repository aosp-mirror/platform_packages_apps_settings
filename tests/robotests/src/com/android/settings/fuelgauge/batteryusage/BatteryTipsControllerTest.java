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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.os.LocaleList;

import com.android.settings.R;
import com.android.settings.testutils.BatteryTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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

    @Mock private BatteryTipsCardPreference mBatteryTipsCardPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Locale.setDefault(new Locale("en_US"));
        org.robolectric.shadows.ShadowSettings.set24HourTimeFormat(false);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        mContext = spy(RuntimeEnvironment.application);
        final Resources resources = spy(mContext.getResources());
        resources.getConfiguration().setLocales(new LocaleList(new Locale("en_US")));
        doReturn(resources).when(mContext).getResources();
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mBatteryTipsController = new BatteryTipsController(mContext);
        mBatteryTipsController.mCardPreference = mBatteryTipsCardPreference;
    }

    @Test
    public void handleBatteryTipsCardUpdated_null_hidePreference() {
        mBatteryTipsController.handleBatteryTipsCardUpdated(/* powerAnomalyEvents= */ null, false);

        verify(mBatteryTipsCardPreference).setVisible(false);
    }

    @Test
    public void handleBatteryTipsCardUpdated_adaptiveBrightnessAnomaly_showAnomaly() {
        PowerAnomalyEvent event = BatteryTestUtils.createAdaptiveBrightnessAnomalyEvent();
        when(mFeatureFactory.powerUsageFeatureProvider.isBatteryTipsEnabled()).thenReturn(true);

        mBatteryTipsController.handleBatteryTipsCardUpdated(
                new AnomalyEventWrapper(mContext, event), false);

        // Check pre-defined string
        verify(mBatteryTipsCardPreference)
                .setTitle("Turn on adaptive brightness to extend battery life");
        verify(mBatteryTipsCardPreference).setIconResourceId(R.drawable.ic_battery_tips_lightbulb);
        verify(mBatteryTipsCardPreference).setButtonColorResourceId(R.color.color_accent_selector);
        verify(mBatteryTipsCardPreference).setMainButtonLabel("View Settings");
        verify(mBatteryTipsCardPreference).setDismissButtonLabel("Got it");
        // Check proto info
        verify(mBatteryTipsCardPreference).setVisible(true);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_BATTERY_TIPS_CARD_SHOW, "BrightnessAnomaly");
    }

    @Test
    public void handleBatteryTipsCardUpdated_screenTimeoutAnomaly_showAnomaly() {
        PowerAnomalyEvent event = BatteryTestUtils.createScreenTimeoutAnomalyEvent();
        when(mFeatureFactory.powerUsageFeatureProvider.isBatteryTipsEnabled()).thenReturn(true);

        mBatteryTipsController.handleBatteryTipsCardUpdated(
                new AnomalyEventWrapper(mContext, event), false);

        verify(mBatteryTipsCardPreference).setTitle("Reduce screen timeout to extend battery life");
        verify(mBatteryTipsCardPreference).setIconResourceId(R.drawable.ic_battery_tips_lightbulb);
        verify(mBatteryTipsCardPreference).setButtonColorResourceId(R.color.color_accent_selector);
        verify(mBatteryTipsCardPreference).setMainButtonLabel("View Settings");
        verify(mBatteryTipsCardPreference).setDismissButtonLabel("Got it");
        verify(mBatteryTipsCardPreference).setVisible(true);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_BATTERY_TIPS_CARD_SHOW,
                        "ScreenTimeoutAnomaly");
    }

    @Test
    public void handleBatteryTipsCardUpdated_screenTimeoutAnomalyHasTitle_showAnomaly() {
        PowerAnomalyEvent event = BatteryTestUtils.createScreenTimeoutAnomalyEvent();
        String testTitle = "TestTitle";
        event =
                event.toBuilder()
                        .setWarningBannerInfo(
                                event.getWarningBannerInfo().toBuilder()
                                        .setTitleString(testTitle)
                                        .build())
                        .build();
        when(mFeatureFactory.powerUsageFeatureProvider.isBatteryTipsEnabled()).thenReturn(true);

        mBatteryTipsController.handleBatteryTipsCardUpdated(
                new AnomalyEventWrapper(mContext, event), false);

        verify(mBatteryTipsCardPreference).setTitle(testTitle);
        verify(mBatteryTipsCardPreference).setIconResourceId(R.drawable.ic_battery_tips_lightbulb);
        verify(mBatteryTipsCardPreference).setButtonColorResourceId(R.color.color_accent_selector);
        verify(mBatteryTipsCardPreference).setMainButtonLabel("View Settings");
        verify(mBatteryTipsCardPreference).setDismissButtonLabel("Got it");
        verify(mBatteryTipsCardPreference).setVisible(true);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_BATTERY_TIPS_CARD_SHOW,
                        "ScreenTimeoutAnomaly");
    }

    @Test
    public void handleBatteryTipsCardUpdated_appAnomaly_showAnomaly() {
        PowerAnomalyEvent event = BatteryTestUtils.createAppAnomalyEvent();
        when(mFeatureFactory.powerUsageFeatureProvider.isBatteryTipsEnabled()).thenReturn(true);

        AnomalyEventWrapper eventWrapper = new AnomalyEventWrapper(mContext, event);
        eventWrapper.setRelatedBatteryDiffEntry(new BatteryDiffEntry(mContext, "", "Chrome", 0));
        mBatteryTipsController.handleBatteryTipsCardUpdated(eventWrapper, false);

        verify(mBatteryTipsCardPreference).setTitle("Chrome used more battery than usual");
        verify(mBatteryTipsCardPreference)
                .setIconResourceId(R.drawable.ic_battery_tips_warning_icon);
        verify(mBatteryTipsCardPreference)
                .setButtonColorResourceId(R.color.color_battery_anomaly_app_warning_selector);
        verify(mBatteryTipsCardPreference).setMainButtonLabel("Check");
        verify(mBatteryTipsCardPreference).setDismissButtonLabel("Got it");
        verify(mBatteryTipsCardPreference).setVisible(true);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_BATTERY_TIPS_CARD_SHOW, "AppAnomaly");
    }
}
