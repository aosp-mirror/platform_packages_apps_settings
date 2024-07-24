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

import static android.content.Context.POWER_SERVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.BrightnessInfo;
import android.os.PowerManager;
import android.provider.Settings.System;
import android.view.Display;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.SettingsBaseActivity;
import com.android.settingslib.transition.SettingsTransitionHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowContentResolver;

@RunWith(RobolectricTestRunner.class)
public class BrightnessLevelPreferenceControllerTest {

    @Mock
    private PowerManager mPowerManager;
    @Mock
    private Display mDisplay;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Preference mPreference;

    private Context mContext;

    private ContentResolver mContentResolver;

    private BrightnessLevelPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mContentResolver = mContext.getContentResolver();
        when(mPowerManager.getBrightnessConstraint(
                PowerManager.BRIGHTNESS_CONSTRAINT_TYPE_MINIMUM)).thenReturn(0.0f);
        when(mPowerManager.getBrightnessConstraint(
                PowerManager.BRIGHTNESS_CONSTRAINT_TYPE_MAXIMUM)).thenReturn(1.0f);
        ShadowApplication.getInstance().setSystemService(POWER_SERVICE,
                mPowerManager);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        doReturn(mDisplay).when(mContext).getDisplay();
        mController = spy(new BrightnessLevelPreferenceController(mContext, null));
    }

    @Test
    public void isAvailable_shouldAlwaysReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void onStart_shouldRegisterObserver() {
        BrightnessLevelPreferenceController controller =
                new BrightnessLevelPreferenceController(mContext, null);
        ShadowContentResolver shadowContentResolver = Shadow.extract(mContentResolver);

        controller.onStart();

        assertThat(shadowContentResolver.getContentObservers(
                System.getUriFor(System.SCREEN_AUTO_BRIGHTNESS_ADJ))).isNotEmpty();
    }

    @Test
    public void onStop_shouldUnregisterObserver() {
        BrightnessLevelPreferenceController controller =
                new BrightnessLevelPreferenceController(mContext, null);
        ShadowContentResolver shadowContentResolver = Shadow.extract(mContext.getContentResolver());

        controller.displayPreference(mScreen);
        controller.onStart();
        controller.onStop();

        assertThat(shadowContentResolver.getContentObservers(
                System.getUriFor(System.SCREEN_AUTO_BRIGHTNESS_ADJ))).isEmpty();
    }

    @Test
    public void onStart_shouldSetSummary() {
        BrightnessLevelPreferenceController controller =
                new BrightnessLevelPreferenceController(mContext, null);
        controller.displayPreference(mScreen);

        controller.onStop();
        when(mDisplay.getBrightnessInfo()).thenReturn(
                new BrightnessInfo(0.5f, 0.0f, 1.0f, BrightnessInfo.HIGH_BRIGHTNESS_MODE_OFF,
                        0.5f, BrightnessInfo.BRIGHTNESS_MAX_REASON_NONE));
        controller.onStart();

        verify(mPreference).setSummary("87%");
    }

    @Test
    public void updateState_autoBrightness_shouldSetSummaryToAutoBrightness() {
        System.putInt(mContentResolver, System.SCREEN_BRIGHTNESS_MODE,
                System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);

        when(mDisplay.getBrightnessInfo()).thenReturn(
                new BrightnessInfo(0.1f, 0.0f, 1.0f, BrightnessInfo.HIGH_BRIGHTNESS_MODE_OFF,
                    0.5f, BrightnessInfo.BRIGHTNESS_MAX_REASON_NONE));

        mController.updateState(mPreference);

        verify(mPreference).setSummary("54%");
    }

    @Test
    public void updateState_manualBrightness_shouldSetSummaryToScreenBrightness() {
        System.putInt(mContentResolver, System.SCREEN_BRIGHTNESS_MODE,
                System.SCREEN_BRIGHTNESS_MODE_MANUAL);

        when(mDisplay.getBrightnessInfo()).thenReturn(
                new BrightnessInfo(0.5f, 0.0f, 1.0f, BrightnessInfo.HIGH_BRIGHTNESS_MODE_OFF,
                    0.5f, BrightnessInfo.BRIGHTNESS_MAX_REASON_NONE));

        mController.updateState(mPreference);

        verify(mPreference).setSummary("87%");
    }

    @Test
    public void handlePreferenceTreeClick_transitionTypeNone_shouldPassToNextActivity() {
        final Activity activity = Robolectric.setupActivity(Activity.class);
        final BrightnessLevelPreferenceController controller =
                new BrightnessLevelPreferenceController(activity, null);
        final ShadowActivity shadowActivity = shadowOf(activity);
        when(mPreference.getKey()).thenReturn("brightness");

        controller.handlePreferenceTreeClick(mPreference);

        final Intent intent = shadowActivity.getNextStartedActivity();
        assertThat(intent.getIntExtra(SettingsBaseActivity.EXTRA_PAGE_TRANSITION_TYPE, 0))
                .isEqualTo(SettingsTransitionHelper.TransitionType.TRANSITION_NONE);

    }
}
