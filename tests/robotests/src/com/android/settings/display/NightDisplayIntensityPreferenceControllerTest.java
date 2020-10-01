/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.hardware.display.ColorDisplayManager;
import android.provider.Settings.Secure;

import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class NightDisplayIntensityPreferenceControllerTest {

    private Context mContext;
    private NightDisplayIntensityPreferenceController mPreferenceController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreferenceController = new NightDisplayIntensityPreferenceController(mContext,
                "night_display_temperature");
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void isAvailable_configuredAvailable_isActivated_available() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_nightDisplayAvailable, true);
        Secure.putInt(mContext.getContentResolver(), Secure.NIGHT_DISPLAY_ACTIVATED, 1);
        assertThat(mPreferenceController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_configuredAvailable_isNotActivated_available() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_nightDisplayAvailable, true);
        Secure.putInt(mContext.getContentResolver(), Secure.NIGHT_DISPLAY_ACTIVATED, 0);
        assertThat(mPreferenceController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_configuredUnavailable_unavailable() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_nightDisplayAvailable, false);
        assertThat(mPreferenceController.isAvailable()).isFalse();
    }

    @Test
    public void onPreferenceChange_changesTemperature() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.integer.config_nightDisplayColorTemperatureMin, 2950);
        SettingsShadowResources.overrideResource(
                com.android.internal.R.integer.config_nightDisplayColorTemperatureMax, 3050);
        // A slider-adjusted "20" here would be 1/5 from the left / least-intense, i.e. 3030.
        mPreferenceController.onPreferenceChange(null, 20);

        assertThat(
                mContext.getSystemService(
                        ColorDisplayManager.class).getNightDisplayColorTemperature())
                .isEqualTo(3030);
    }

    @Test
    public void rangeOfSlider_staysWithinValidRange() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.integer.config_nightDisplayColorTemperatureMin, 2950);
        SettingsShadowResources.overrideResource(
                com.android.internal.R.integer.config_nightDisplayColorTemperatureMax, 3050);

        assertThat(mPreferenceController.getMax() - mPreferenceController.getMin())
                .isGreaterThan(0);
    }

    @Test
    public void getMin_alwaysReturnsZero() {
        assertThat(mPreferenceController.getMin()).isEqualTo(0);
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        assertThat(mPreferenceController.isSliceable()).isTrue();
    }

    @Test
    public void isSliceableIncorrectKey_returnsFalse() {
        final NightDisplayIntensityPreferenceController controller =
                new NightDisplayIntensityPreferenceController(mContext, "bad_key");
        assertThat(controller.isSliceable()).isFalse();
    }

    @Test
    public void isPublicSlice_returnTrue() {
        assertThat(mPreferenceController.isPublicSlice()).isTrue();
    }
}
