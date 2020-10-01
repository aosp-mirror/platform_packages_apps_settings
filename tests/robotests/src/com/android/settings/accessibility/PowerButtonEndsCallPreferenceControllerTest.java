/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.SwitchPreference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.shadow.ShadowKeyCharacterMap;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUtils.class, ShadowKeyCharacterMap.class})
public class PowerButtonEndsCallPreferenceControllerTest {

    private static final int UNKNOWN = -1;

    private Context mContext;
    private SwitchPreference mPreference;
    private PowerButtonEndsCallPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreference = new SwitchPreference(mContext);
        mController = new PowerButtonEndsCallPreferenceController(mContext, "power_button");
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
        ShadowKeyCharacterMap.reset();
    }

    @Test
    public void getAvailabilityStatus_hasPowerKeyAndVoiceCapable_shouldReturnAvailable() {
        ShadowKeyCharacterMap.setDevicehasKey(true);
        ShadowUtils.setIsVoiceCapable(true);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noVoiceCapable_shouldReturnUnsupportedOnDevice() {
        ShadowKeyCharacterMap.setDevicehasKey(true);
        ShadowUtils.setIsVoiceCapable(false);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_noPowerKey_shouldReturnUnsupportedOnDevice() {
        ShadowKeyCharacterMap.setDevicehasKey(false);
        ShadowUtils.setIsVoiceCapable(true);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void isChecked_enabledHangUp_shouldReturnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP);

        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void isChecked_disabledHangUp_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF);

        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void setChecked_enabled_shouldEnableHangUp() {
        mController.setChecked(true);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR, UNKNOWN))
                .isEqualTo(Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP);
    }

    @Test
    public void setChecked_disabled_shouldDisableHangUp() {
        mController.setChecked(false);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR, UNKNOWN))
                .isEqualTo(Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF);
    }
}
