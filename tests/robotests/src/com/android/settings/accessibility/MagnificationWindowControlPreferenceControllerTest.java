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
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@RunWith(RobolectricTestRunner.class)
public class MagnificationWindowControlPreferenceControllerTest {
    private static final String PREF_KEY = "screen_magnification_window_control_switch";
    // TODO(b/146019459): Use magnification_window_control_enabled.
    private static final String KEY_CONTROL = Settings.System.MASTER_MONO;
    private Context mContext;
    private SwitchPreference mPreference;
    private MagnificationWindowControlPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreference = new SwitchPreference(mContext);
        mController = new MagnificationWindowControlPreferenceController(mContext, PREF_KEY);
    }

    @Test
    public void isChecked_enabledWindowControl_shouldReturnTrue() {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                KEY_CONTROL, State.ON, UserHandle.USER_CURRENT);

        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void isChecked_disabledWindowControl_shouldReturnFalse() {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                KEY_CONTROL, State.OFF, UserHandle.USER_CURRENT);

        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void setChecked_setTrue_shouldEnableWindowControl() {
        mController.setChecked(true);

        assertThat(Settings.System.getIntForUser(mContext.getContentResolver(),
                KEY_CONTROL, State.UNKNOWN, UserHandle.USER_CURRENT)).isEqualTo(State.ON);
    }

    @Test
    public void setChecked_setFalse_shouldDisableWindowControl() {
        mController.setChecked(false);

        assertThat(Settings.System.getIntForUser(mContext.getContentResolver(),
                KEY_CONTROL, State.UNKNOWN, UserHandle.USER_CURRENT)).isEqualTo(State.OFF);
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface State {
        int UNKNOWN = -1;
        int OFF = 0;
        int ON = 1;
    }
}
