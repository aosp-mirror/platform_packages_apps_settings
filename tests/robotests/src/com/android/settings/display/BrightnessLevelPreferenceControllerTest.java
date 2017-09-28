/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings.System;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowContentResolver;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BrightnessLevelPreferenceControllerTest {
    @Mock
    private Context mContext;
    @Mock
    private ContentResolver mContentResolver;
    @Mock
    private PowerManagerWrapper mPowerManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Preference mPreference;

    private BrightnessLevelPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        when(mPowerManager.getMinimumScreenBrightnessSetting()).thenReturn(0);
        when(mPowerManager.getMaximumScreenBrightnessSetting()).thenReturn(100);
        when(mPowerManager.getMinimumScreenBrightnessForVrSetting()).thenReturn(0);
        when(mPowerManager.getMaximumScreenBrightnessForVrSetting()).thenReturn(100);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        mController = spy(new BrightnessLevelPreferenceController(mContext, null, mPowerManager));
        doReturn(false).when(mController).isInVrMode();

    }

    @Test
    public void isAvailable_shouldAlwaysReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void onStart_shouldRegisterObserver() {
        Context context = RuntimeEnvironment.application;
        BrightnessLevelPreferenceController controller =
            new BrightnessLevelPreferenceController(context, null, mPowerManager);
        ShadowContentResolver shadowContentResolver =
            (ShadowContentResolver) ShadowExtractor.extract(context.getContentResolver());

        controller.onStart();

        assertThat(shadowContentResolver.getContentObservers(
            System.getUriFor(System.SCREEN_BRIGHTNESS_MODE))).isNotEmpty();
        assertThat(shadowContentResolver.getContentObservers(
            System.getUriFor(System.SCREEN_BRIGHTNESS))).isNotEmpty();
        assertThat(shadowContentResolver.getContentObservers(
            System.getUriFor(System.SCREEN_BRIGHTNESS_FOR_VR))).isNotEmpty();
        assertThat(shadowContentResolver.getContentObservers(
            System.getUriFor(System.SCREEN_AUTO_BRIGHTNESS_ADJ))).isNotEmpty();
    }

    @Test
    public void onStop_shouldUnregisterObserver() {
        Context context = RuntimeEnvironment.application;
        BrightnessLevelPreferenceController controller =
            new BrightnessLevelPreferenceController(context, null, mPowerManager);
        ShadowContentResolver shadowContentResolver =
            (ShadowContentResolver) ShadowExtractor.extract(context.getContentResolver());

        controller.displayPreference(mScreen);
        controller.onStart();
        controller.onStop();

        assertThat(shadowContentResolver.getContentObservers(
            System.getUriFor(System.SCREEN_BRIGHTNESS_MODE))).isEmpty();
        assertThat(shadowContentResolver.getContentObservers(
            System.getUriFor(System.SCREEN_BRIGHTNESS))).isEmpty();
        assertThat(shadowContentResolver.getContentObservers(
            System.getUriFor(System.SCREEN_BRIGHTNESS_FOR_VR))).isEmpty();
        assertThat(shadowContentResolver.getContentObservers(
            System.getUriFor(System.SCREEN_AUTO_BRIGHTNESS_ADJ))).isEmpty();
    }

    @Test
    public void updateState_inVrMode_shouldSetSummaryToVrBrightness() {
        doReturn(true).when(mController).isInVrMode();
        System.putInt(mContentResolver, System.SCREEN_BRIGHTNESS_FOR_VR, 85);

        mController.updateState(mPreference);

        verify(mPreference).setSummary("85%");
    }

    @Test
    public void updateState_autoBrightness_shouldSetSummaryToAutoBrightness() {
        doReturn(false).when(mController).isInVrMode();
        System.putInt(mContentResolver, System.SCREEN_BRIGHTNESS_MODE,
            System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);

        System.putFloat(mContentResolver, System.SCREEN_AUTO_BRIGHTNESS_ADJ, 0.0f);

        mController.updateState(mPreference);

        verify(mPreference).setSummary("50%");
    }

    @Test
    public void updateState_manualBrightness_shouldSetSummaryToScreenBrightness() {
        doReturn(false).when(mController).isInVrMode();
        System.putInt(mContentResolver, System.SCREEN_BRIGHTNESS_MODE,
            System.SCREEN_BRIGHTNESS_MODE_MANUAL);

        System.putInt(mContentResolver, System.SCREEN_BRIGHTNESS, 45);

        mController.updateState(mPreference);

        verify(mPreference).setSummary("45%");
    }

    @Test
    public void updateState_brightnessOutOfRange_shouldSetSummaryInRange() {
        // VR mode
        doReturn(true).when(mController).isInVrMode();

        System.putInt(mContentResolver, System.SCREEN_BRIGHTNESS_FOR_VR, 105);
        mController.updateState(mPreference);
        verify(mPreference).setSummary("100%");

        System.putInt(mContentResolver, System.SCREEN_BRIGHTNESS_FOR_VR, -20);
        mController.updateState(mPreference);
        verify(mPreference).setSummary("0%");

        // Auto mode
        doReturn(false).when(mController).isInVrMode();
        System.putInt(mContentResolver, System.SCREEN_BRIGHTNESS_MODE,
                System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);

        reset(mPreference);
        System.putFloat(mContentResolver, System.SCREEN_AUTO_BRIGHTNESS_ADJ, 1.5f);
        mController.updateState(mPreference);
        verify(mPreference).setSummary("100%");

        System.putFloat(mContentResolver, System.SCREEN_AUTO_BRIGHTNESS_ADJ, -1.5f);
        mController.updateState(mPreference);
        verify(mPreference).setSummary("0%");

        // Manual mode
        System.putInt(mContentResolver, System.SCREEN_BRIGHTNESS_MODE,
                System.SCREEN_BRIGHTNESS_MODE_MANUAL);

        reset(mPreference);
        System.putInt(mContentResolver, System.SCREEN_BRIGHTNESS, 115);
        mController.updateState(mPreference);
        verify(mPreference).setSummary("100%");

        System.putInt(mContentResolver, System.SCREEN_BRIGHTNESS, -10);
        mController.updateState(mPreference);
        verify(mPreference).setSummary("0%");
    }
}
