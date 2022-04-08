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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class MagnificationEnablePreferenceControllerTest {
    private static final String PREF_KEY = "screen_magnification_enable";
    private static final String KEY_ENABLE = Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE;
    private static final int UNKNOWN = -1;
    private Context mContext;
    private SwitchPreference mPreference;
    private MagnificationEnablePreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreference = new SwitchPreference(mContext);
        mController = new MagnificationEnablePreferenceController(mContext, PREF_KEY);
    }

    @Test
    public void isChecked_enabledFullscreenMagnificationMode_shouldReturnTrue() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                KEY_ENABLE, Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_FULLSCREEN,
                UserHandle.USER_CURRENT);

        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void isChecked_enabledWindowMagnificationMode_shouldReturnFalse() {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                KEY_ENABLE, Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_WINDOW,
                UserHandle.USER_CURRENT);

        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
    }


    @Test
    public void setChecked_setTrue_shouldEnableFullscreenMagnificationMode() {
        mController.setChecked(true);

        assertThat(Settings.Secure.getIntForUser(mContext.getContentResolver(),
                KEY_ENABLE, UNKNOWN,
                UserHandle.USER_CURRENT)).isEqualTo(
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_FULLSCREEN);
    }

    @Test
    public void setChecked_setFalse_shouldEnableWindowMagnificationMode() {
        mController.setChecked(false);

        assertThat(Settings.Secure.getIntForUser(mContext.getContentResolver(),
                KEY_ENABLE, UNKNOWN,
                UserHandle.USER_CURRENT)).isEqualTo(
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_WINDOW);
    }
}
