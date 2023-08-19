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

import android.content.Context;
import android.content.res.Resources;
import android.os.LocaleList;

import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.testutils.BatteryTestUtils;

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
    private BatteryTipsController mBatteryTipsController;

    @Mock
    private BatteryTipsCardPreference mBatteryTipsCardPreference;

    @Mock
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;

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
        mBatteryTipsController = new BatteryTipsController(mContext);
        mBatteryTipsController.mCardPreference = mBatteryTipsCardPreference;
        mBatteryTipsController.mPowerUsageFeatureProvider = mPowerUsageFeatureProvider;
    }

    @Test
    public void handleBatteryTipsCardUpdated_null_hidePreference() {
        mBatteryTipsController.handleBatteryTipsCardUpdated(/* powerAnomalyEvents= */ null);

        verify(mBatteryTipsCardPreference).setVisible(false);
    }

    @Test
    public void handleBatteryTipsCardUpdated_adaptiveBrightnessAnomaly_showAnomaly() {
        PowerAnomalyEvent event = BatteryTestUtils.createAdaptiveBrightnessAnomalyEvent();
        when(mPowerUsageFeatureProvider.isBatteryTipsEnabled()).thenReturn(true);

        mBatteryTipsController.handleBatteryTipsCardUpdated(event);

        // Check pre-defined string
        verify(mBatteryTipsCardPreference).setTitle(
                "Turn on adaptive brightness to extend battery life");
        verify(mBatteryTipsCardPreference).setMainButtonLabel("View Settings");
        verify(mBatteryTipsCardPreference).setDismissButtonLabel("Got it");
        // Check proto info
        verify(mBatteryTipsCardPreference).setMainButtonLauncherInfo(
                "com.android.settings.display.AutoBrightnessSettings",
                1381);
        verify(mBatteryTipsCardPreference).setVisible(true);
    }

    @Test
    public void handleBatteryTipsCardUpdated_screenTimeoutAnomaly_showAnomaly() {
        PowerAnomalyEvent event = BatteryTestUtils.createScreenTimeoutAnomalyEvent();
        when(mPowerUsageFeatureProvider.isBatteryTipsEnabled()).thenReturn(true);

        mBatteryTipsController.handleBatteryTipsCardUpdated(event);

        verify(mBatteryTipsCardPreference).setTitle("Reduce screen timeout to extend battery life");
        verify(mBatteryTipsCardPreference).setMainButtonLabel("View Settings");
        verify(mBatteryTipsCardPreference).setDismissButtonLabel("Got it");
        verify(mBatteryTipsCardPreference).setMainButtonLauncherInfo(
                "com.android.settings.display.ScreenTimeoutSettings",
                1852);
        verify(mBatteryTipsCardPreference).setVisible(true);
    }
    @Test
    public void handleBatteryTipsCardUpdated_screenTimeoutAnomalyHasTitle_showAnomaly() {
        PowerAnomalyEvent event = BatteryTestUtils.createScreenTimeoutAnomalyEvent();
        String testTitle = "TestTitle";
        event = event.toBuilder()
                .setWarningBannerInfo(
                        event.getWarningBannerInfo().toBuilder()
                                .setTitleString(testTitle)
                                .build())
                .build();
        when(mPowerUsageFeatureProvider.isBatteryTipsEnabled()).thenReturn(true);

        mBatteryTipsController.handleBatteryTipsCardUpdated(event);

        verify(mBatteryTipsCardPreference).setTitle(testTitle);
        verify(mBatteryTipsCardPreference).setMainButtonLabel("View Settings");
        verify(mBatteryTipsCardPreference).setDismissButtonLabel("Got it");
        verify(mBatteryTipsCardPreference).setMainButtonLauncherInfo(
                "com.android.settings.display.ScreenTimeoutSettings",
                1852);
        verify(mBatteryTipsCardPreference).setVisible(true);
    }
}
