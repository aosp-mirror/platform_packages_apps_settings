/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static com.android.settings.gestures.DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_DISABLED_MODE;
import static com.android.settings.gestures.DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_MULTI_TARGET_MODE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DoubleTapPowerForWalletPreferenceControllerTest {

    private static final String KEY = "gesture_double_power_tap_launch_wallet";
    private Context mContext;
    private Resources mResources;
    private DoubleTapPowerForWalletPreferenceController mController;
    private SelectorWithWidgetPreference mPreference;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = mock(Resources.class);
        when(mContext.getResources()).thenReturn(mResources);
        mController = new DoubleTapPowerForWalletPreferenceController(mContext, KEY);
        mPreference = new SelectorWithWidgetPreference(mContext);
    }

    @Test
    public void updateState_launchWalletEnabled_preferenceChecked() {
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonForWalletLaunch(mContext);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_launchWalletDisabled_preferenceNotChecked() {
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonForCameraLaunch(mContext);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void getAvailabilityStatus_setDoubleTapPowerGestureNotAvailable_preferenceUnsupported() {
        when(mResources.getInteger(R.integer.config_doubleTapPowerGestureMode)).thenReturn(
                DOUBLE_TAP_POWER_DISABLED_MODE);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_setDoubleTapPowerButtonDisabled_preferenceDisabled() {
        when(mResources.getInteger(R.integer.config_doubleTapPowerGestureMode)).thenReturn(
                DOUBLE_TAP_POWER_MULTI_TARGET_MODE);
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonGestureEnabled(mContext, false);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getAvailabilityStatus_setDoubleTapPowerWalletLaunchEnabled_preferenceEnabled() {
        when(mResources.getInteger(R.integer.config_doubleTapPowerGestureMode)).thenReturn(
                DOUBLE_TAP_POWER_MULTI_TARGET_MODE);
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonGestureEnabled(mContext, true);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }
}
