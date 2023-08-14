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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.display.AutoBrightnessSettings;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.testutils.BatteryTestUtils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class BatteryTipsCardPreferenceTest {

    private Context mContext;
    private BatteryTipsCardPreference mBatteryTipsCardPreference;
    private BatteryTipsController mBatteryTipsController;
    @Mock
    private View mFakeView;
    @Mock
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mBatteryTipsCardPreference = new BatteryTipsCardPreference(mContext, /*attrs=*/ null);
        mBatteryTipsController = new BatteryTipsController(mContext);
        mBatteryTipsController.mCardPreference = mBatteryTipsCardPreference;
        mBatteryTipsController.mPowerUsageFeatureProvider = mPowerUsageFeatureProvider;
    }

    @Test
    public void constructor_returnExpectedResult() {
        assertThat(mBatteryTipsCardPreference.getLayoutResource()).isEqualTo(
                R.layout.battery_tips_card);
    }
    @Test
    public void onClick_actionBtn_getAdaptiveBrightnessLauncher() {
        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        PowerAnomalyEvent adaptiveBrightnessAnomaly =
                BatteryTestUtils.createAdaptiveBrightnessAnomalyEvent();
        when(mPowerUsageFeatureProvider.isBatteryTipsEnabled()).thenReturn(true);
        when(mFakeView.getId()).thenReturn(R.id.main_button);
        doNothing().when(mContext).startActivity(captor.capture());

        mBatteryTipsController.handleBatteryTipsCardUpdated(adaptiveBrightnessAnomaly);
        mBatteryTipsCardPreference.onClick(mFakeView);

        verify(mContext).startActivity(any(Intent.class));
        final Intent intent = captor.getValue();
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(AutoBrightnessSettings.class.getName());
        assertThat(intent.getIntExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY, -1))
                .isEqualTo(SettingsEnums.SETTINGS_AUTO_BRIGHTNESS);
    }
}
