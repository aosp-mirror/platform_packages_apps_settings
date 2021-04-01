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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.UserHandle;

import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.core.TogglePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class OneHandedAppTapsExitPreferenceControllerTest {

    private static final String KEY = "gesture_app_taps_to_exit";

    private Context mContext;
    private SwitchPreference mSwitchPreference;
    private OneHandedAppTapsExitPreferenceController mController;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new OneHandedAppTapsExitPreferenceController(mContext, KEY);
        mSwitchPreference = new SwitchPreference(mContext);
        mSwitchPreference.setKey(KEY);
        OneHandedSettingsUtils.setUserId(UserHandle.myUserId());
    }

    @Test
    public void setChecked_setBoolean_checkIsTrueOrFalse() {
        mController.setChecked(false);
        assertThat(mController.isChecked()).isFalse();

        mController.setChecked(true);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void getAvailabilityStatus_enabledOneHanded_shouldAvailable() {
        OneHandedSettingsUtils.setOneHandedModeEnabled(mContext, true);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(TogglePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_disabledOneHanded_shouldUnavailable() {
        OneHandedSettingsUtils.setOneHandedModeEnabled(mContext, false);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(TogglePreferenceController.DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void updateState_enableOneHanded_switchShouldEnabled() {
        OneHandedSettingsUtils.setOneHandedModeEnabled(mContext, true);

        mController.updateState(mSwitchPreference);

        assertThat(mSwitchPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_disableOneHanded_switchShouldDisabled() {
        OneHandedSettingsUtils.setOneHandedModeEnabled(mContext, false);

        mController.updateState(mSwitchPreference);

        assertThat(mSwitchPreference.isEnabled()).isFalse();
    }
}
