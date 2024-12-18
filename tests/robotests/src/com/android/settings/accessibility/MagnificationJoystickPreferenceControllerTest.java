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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MagnificationJoystickPreferenceControllerTest {

    private static final String KEY_JOYSTICK =
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_JOYSTICK_ENABLED;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final SwitchPreference mSwitchPreference = spy(new SwitchPreference(mContext));
    private final MagnificationJoystickPreferenceController mController =
            new MagnificationJoystickPreferenceController(mContext,
                    MagnificationJoystickPreferenceController.PREF_KEY);

    @Before
    public void setUp() {
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        final PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);
        mSwitchPreference.setKey(MagnificationJoystickPreferenceController.PREF_KEY);
        screen.addPreference(mSwitchPreference);
        mController.displayPreference(screen);

        mController.updateState(mSwitchPreference);
        reset(mSwitchPreference);
    }

    @Test
    public void performClick_switchDefaultStateForJoystick_shouldReturnTrue() {
        mSwitchPreference.performClick();

        verify(mSwitchPreference).setChecked(true);
        assertThat(mController.isChecked()).isTrue();
        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_disableJoystick_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(), KEY_JOYSTICK, OFF);

        mController.updateState(mSwitchPreference);

        verify(mSwitchPreference).setChecked(false);
        assertThat(mController.isChecked()).isFalse();
        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void getAvailableStatus_notInSetupWizard_returnAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailableStatus_inSetupWizard_returnConditionallyUnavailable() {
        mController.setInSetupWizard(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }
}
