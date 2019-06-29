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

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class MasterMonoPreferenceControllerTest {

    private static final int ON = 1;
    private static final int OFF = 0;
    private static final int UNKNOWN = -1;

    private Context mContext;
    private SwitchPreference mPreference;
    private MasterMonoPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreference = new SwitchPreference(mContext);
        mController = new MasterMonoPreferenceController(mContext, "master_mono");
    }

    @Test
    public void getAvailabilityStatus_byDefault_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void isChecked_enabledMonoAudio_shouldReturnTrue() {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.MASTER_MONO, ON, UserHandle.USER_CURRENT);

        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void isChecked_disabledMonoAudio_shouldReturnFalse() {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.MASTER_MONO, OFF, UserHandle.USER_CURRENT);

        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void setChecked_setTrue_shouldEnableMonoAudio() {
        mController.setChecked(true);

        assertThat(Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.MASTER_MONO, UNKNOWN, UserHandle.USER_CURRENT)).isEqualTo(ON);
    }

    @Test
    public void setChecked_setFalse_shouldDisableMonoAudio() {
        mController.setChecked(false);

        assertThat(Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.MASTER_MONO, UNKNOWN, UserHandle.USER_CURRENT)).isEqualTo(OFF);
    }
}
