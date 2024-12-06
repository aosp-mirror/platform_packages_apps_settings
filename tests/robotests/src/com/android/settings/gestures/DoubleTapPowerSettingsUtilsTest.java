/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static com.android.settings.gestures.DoubleTapPowerSettingsUtils.OFF;
import static com.android.settings.gestures.DoubleTapPowerSettingsUtils.ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DoubleTapPowerSettingsUtilsTest {

    private static final int DOUBLE_TAP_POWER_BUTTON_CAMERA_LAUNCH_VALUE = 0;
    private static final int DOUBLE_TAP_POWER_BUTTON_WALLET_LAUNCH_VALUE = 1;

    private Context mContext;
    private Resources mResources;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = mock(Resources.class);
        when(mContext.getResources()).thenReturn(mResources);
    }

    @Test
    public void isDoubleTapPowerButtonGestureAvailable_setAvailable_returnsTrue() {
        when(mResources.getBoolean(R.bool.config_doubleTapPowerGestureEnabled)).thenReturn(true);

        assertThat(DoubleTapPowerSettingsUtils.isDoubleTapPowerButtonGestureAvailable(mContext))
                .isTrue();
    }

    @Test
    public void isDoubleTapPowerButtonGestureAvailable_setUnavailable_returnsFalse() {
        when(mResources.getBoolean(R.bool.config_doubleTapPowerGestureEnabled)).thenReturn(false);

        assertThat(DoubleTapPowerSettingsUtils.isDoubleTapPowerButtonGestureAvailable(mContext))
                .isFalse();
    }

    @Test
    public void isDoubleTapPowerButtonGestureEnabled_setEnabled_returnsTrue() {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED,
                ON);

        assertThat(DoubleTapPowerSettingsUtils.isDoubleTapPowerButtonGestureEnabled(mContext))
                .isTrue();
    }

    @Test
    public void isDoubleTapPowerButtonGestureEnabled_setDisabled_returnsFalse() {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED,
                OFF);

        assertThat(DoubleTapPowerSettingsUtils.isDoubleTapPowerButtonGestureEnabled(mContext))
                .isFalse();
    }

    @Test
    public void isDoubleTapPowerButtonGestureEnabled_valueNotSet_returnsTrue() {
        assertThat(DoubleTapPowerSettingsUtils.isDoubleTapPowerButtonGestureEnabled(mContext))
                .isTrue();
    }

    @Test
    public void setDoubleTapPowerButtonGestureEnabled_setEnabled_returnsEnabled() {
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonGestureEnabled(mContext, true);

        assertThat(
                        Settings.Secure.getInt(
                                mContext.getContentResolver(),
                                Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED,
                                OFF))
                .isEqualTo(ON);
    }

    @Test
    public void setDoubleTapPowerButtonGestureEnabled_setDisabled_returnsDisabled() {
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonGestureEnabled(mContext, false);

        assertThat(
                        Settings.Secure.getInt(
                                mContext.getContentResolver(),
                                Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED,
                                ON))
                .isEqualTo(OFF);
    }

    @Test
    public void isDoubleTapPowerButtonGestureForCameraLaunchEnabled_valueSetToCamera_returnsTrue() {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE,
                DOUBLE_TAP_POWER_BUTTON_CAMERA_LAUNCH_VALUE);

        assertThat(
                        DoubleTapPowerSettingsUtils
                                .isDoubleTapPowerButtonGestureForCameraLaunchEnabled(mContext))
                .isTrue();
    }

    @Test
    public void
            isDoubleTapPowerButtonGestureForCameraLaunchEnabled_valueNotSetToCamera_returnsFalse() {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE,
                DOUBLE_TAP_POWER_BUTTON_WALLET_LAUNCH_VALUE);

        assertThat(
                        DoubleTapPowerSettingsUtils
                                .isDoubleTapPowerButtonGestureForCameraLaunchEnabled(mContext))
                .isFalse();
    }

    @Test
    public void
            isDoubleTapPowerButtonGestureForCameraLaunchEnabled_defaultSetToCamera_returnsTrue() {
        when(mResources.getInteger(R.integer.config_defaultDoubleTapPowerGestureAction))
                .thenReturn(DOUBLE_TAP_POWER_BUTTON_CAMERA_LAUNCH_VALUE);

        assertThat(
                        DoubleTapPowerSettingsUtils
                                .isDoubleTapPowerButtonGestureForCameraLaunchEnabled(mContext))
                .isTrue();
    }

    @Test
    public void
            isDoubleTapPowerButtonGestureForCameraLaunchEnabled_defaultNotCamera_returnsFalse() {
        when(mResources.getInteger(R.integer.config_defaultDoubleTapPowerGestureAction))
                .thenReturn(DOUBLE_TAP_POWER_BUTTON_WALLET_LAUNCH_VALUE);

        assertThat(
                        DoubleTapPowerSettingsUtils
                                .isDoubleTapPowerButtonGestureForCameraLaunchEnabled(mContext))
                .isFalse();
    }

    @Test
    public void setDoubleTapPowerButtonForCameraLaunch_setGestureBehaviorToCameraLaunch() {
        boolean result =
                DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonForCameraLaunch(mContext);

        assertThat(result).isTrue();
        assertThat(
                        Settings.Secure.getInt(
                                mContext.getContentResolver(),
                                Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE,
                                DOUBLE_TAP_POWER_BUTTON_WALLET_LAUNCH_VALUE))
                .isEqualTo(DOUBLE_TAP_POWER_BUTTON_CAMERA_LAUNCH_VALUE);
    }

    @Test
    public void setDoubleTapPowerButtonForWalletLaunch_setGestureBehaviorToWalletLaunch() {
        boolean result =
                DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonForWalletLaunch(mContext);

        assertThat(result).isTrue();
        assertThat(
                        Settings.Secure.getInt(
                                mContext.getContentResolver(),
                                Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE,
                                DOUBLE_TAP_POWER_BUTTON_CAMERA_LAUNCH_VALUE))
                .isEqualTo(DOUBLE_TAP_POWER_BUTTON_WALLET_LAUNCH_VALUE);
    }
}
