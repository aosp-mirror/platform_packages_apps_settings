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
import android.provider.Settings;

import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ReduceBrightColorsPersistencePreferenceControllerTest {
    private static final String PREF_KEY = "rbc_persist";
    private static final String RBC_PERSIST =
            Settings.Secure.REDUCE_BRIGHT_COLORS_PERSIST_ACROSS_REBOOTS;
    private static final int ON = 1;
    private static final int OFF = 0;
    private static final int UNKNOWN = -1;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final SwitchPreference mPreference = new SwitchPreference(mContext);
    private final ReduceBrightColorsPersistencePreferenceController mController =
            new ReduceBrightColorsPersistencePreferenceController(mContext, PREF_KEY);

    @Test
    public void isChecked_enabledRbc_shouldReturnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(), RBC_PERSIST, ON);

        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void isChecked_disabledRbc_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(), RBC_PERSIST, OFF);

        mController.updateState(mPreference);

        assertThat(mController.isChecked()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void setChecked_setTrue_shouldEnableRbc() {
        mController.setChecked(true);

        assertThat(
                Settings.Secure.getInt(mContext.getContentResolver(), RBC_PERSIST, UNKNOWN))
                .isEqualTo(ON);
    }

    @Test
    public void setChecked_setFalse_shouldDisableRbc() {
        mController.setChecked(false);

        assertThat(
                Settings.Secure.getInt(mContext.getContentResolver(), RBC_PERSIST, UNKNOWN))
                .isEqualTo(OFF);
    }
}
