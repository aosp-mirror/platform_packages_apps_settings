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

package com.android.settings.notification;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Vibrator;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import com.android.server.notification.Flags;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PoliteNotifVibrateUnlockedToggleControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String PREFERENCE_KEY = "preference_key";

    private PoliteNotifVibrateUnlockedToggleController mController;
    private Context mContext;
    @Mock
    private Vibrator mVibrator;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mController = new PoliteNotifVibrateUnlockedToggleController(mContext, PREFERENCE_KEY);
        when(mContext.getSystemService(Vibrator.class)).thenReturn(mVibrator);
    }

    @Test
    public void isAvailable_flagEnabled_vibrationSupported_shouldReturnTrue() {
        // TODO: b/291907312 - remove feature flags
        mSetFlagsRule.enableFlags(Flags.FLAG_POLITE_NOTIFICATIONS);
        mSetFlagsRule.enableFlags(Flags.FLAG_VIBRATE_WHILE_UNLOCKED);
        when(mVibrator.hasVibrator()).thenReturn(true);
        assertThat(mController.isAvailable()).isTrue();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void isAvailable_flagEnabled_vibrationNotSupported_shouldReturnFalse() {
        // TODO: b/291907312 - remove feature flags
        mSetFlagsRule.enableFlags(Flags.FLAG_POLITE_NOTIFICATIONS);
        mSetFlagsRule.enableFlags(Flags.FLAG_VIBRATE_WHILE_UNLOCKED);
        when(mVibrator.hasVibrator()).thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void isAvailable_flagDisabled_shouldReturnFalse() {
        // TODO: b/291907312 - remove feature flags
        mSetFlagsRule.disableFlags(Flags.FLAG_POLITE_NOTIFICATIONS);
        mSetFlagsRule.enableFlags(Flags.FLAG_VIBRATE_WHILE_UNLOCKED);
        assertThat(mController.isAvailable()).isFalse();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void isChecked_vibrateEnabled_shouldReturnTrue() {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_VIBRATE_UNLOCKED, ON);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_vibrateDisabled_shouldReturnFalse() {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_VIBRATE_UNLOCKED, OFF);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_setTrue_shouldEnableVibrateSetting() {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_VIBRATE_UNLOCKED, OFF);
        mController.setChecked(true);
        assertThat(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_VIBRATE_UNLOCKED, OFF)).isEqualTo(ON);
    }

    @Test
    public void setChecked_setFalse_shouldDisableVibrateSetting() {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_VIBRATE_UNLOCKED, ON);
        mController.setChecked(false);
        assertThat(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_VIBRATE_UNLOCKED, ON)).isEqualTo(OFF);
    }
}
