/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.provider.Settings;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
public class PowerMenuPreferenceControllerTest {
    private Context mContext;
    private Resources mResources;
    private PowerMenuPreferenceController mController;
    private ShadowPackageManager mShadowPackageManager;

    private static final String KEY_GESTURE_POWER_MENU = "gesture_power_menu";
    private static final String CONTROLS_ENABLED = Settings.Secure.CONTROLS_ENABLED;
    private static final String CONTROLS_FEATURE = PackageManager.FEATURE_CONTROLS;
    private static final String CARDS_ENABLED = Settings.Secure.GLOBAL_ACTIONS_PANEL_ENABLED;
    private static final String CARDS_AVAILABLE = Settings.Secure.GLOBAL_ACTIONS_PANEL_AVAILABLE;


    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mResources = mock(Resources.class);
        when(mResources.getBoolean(
            com.android.internal.R.bool.config_longPressOnPowerForAssistantSettingAvailable))
                .thenReturn(true);
        when(mContext.getResources()).thenReturn(mResources);
        mShadowPackageManager = Shadows.shadowOf(mContext.getPackageManager());
        mController = new PowerMenuPreferenceController(mContext, KEY_GESTURE_POWER_MENU);
    }

    @Test
    public void getAvailabilityStatus_allAvailable_available() {
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_AVAILABLE, 1);
        mShadowPackageManager.setSystemFeature(CONTROLS_FEATURE, true);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_longPressOnPowerForAssistantSettingAvailable))
                    .thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_onlyCardsAvailable_available() {
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_AVAILABLE, 1);
        mShadowPackageManager.setSystemFeature(CONTROLS_FEATURE, false);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_longPressOnPowerForAssistantSettingAvailable))
                    .thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_onlyControlsAvailable_available() {
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_AVAILABLE, 0);
        mShadowPackageManager.setSystemFeature(CONTROLS_FEATURE, true);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_longPressOnPowerForAssistantSettingAvailable))
                    .thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_controlsAndCardsAvailable_available() {
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_AVAILABLE, 1);
        mShadowPackageManager.setSystemFeature(CONTROLS_FEATURE, true);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_longPressOnPowerForAssistantSettingAvailable))
                    .thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_controlsAndAssistAvailable_available() {
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_AVAILABLE, 0);
        mShadowPackageManager.setSystemFeature(CONTROLS_FEATURE, true);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_longPressOnPowerForAssistantSettingAvailable))
                    .thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_cardsAndAssistAvailable_available() {
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_AVAILABLE, 1);
        mShadowPackageManager.setSystemFeature(CONTROLS_FEATURE, false);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_longPressOnPowerForAssistantSettingAvailable))
                    .thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_onlyAssistAvailable_available() {
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_AVAILABLE, 0);
        mShadowPackageManager.setSystemFeature(CONTROLS_FEATURE, false);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_longPressOnPowerForAssistantSettingAvailable))
                    .thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_allUnavailable_unavailable() {
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_AVAILABLE, 0);
        mShadowPackageManager.setSystemFeature(CONTROLS_FEATURE, false);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_longPressOnPowerForAssistantSettingAvailable))
                    .thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }
}
