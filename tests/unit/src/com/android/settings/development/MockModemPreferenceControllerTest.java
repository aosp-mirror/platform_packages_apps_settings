/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.development;

import static com.android.settings.development.MockModemPreferenceController
        .ALLOW_MOCK_MODEM_PROPERTY;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.Looper;
import android.os.SystemProperties;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MockModemPreferenceControllerTest {

    private Context mContext;
    private MockModemPreferenceController mController;
    private SwitchPreference mPreference;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new MockModemPreferenceController(mContext);
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        final PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);
        mPreference = new SwitchPreference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        screen.addPreference(mPreference);
        mController.displayPreference(screen);
    }

    @Test
    public void onPreferenceChanged_settingDisabled_shouldNotAllowedMockModem() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        final boolean mode = SystemProperties.getBoolean(
                ALLOW_MOCK_MODEM_PROPERTY, false /* default */);

        assertThat(mode).isFalse();
    }

    @Test
    public void onPreferenceChanged_settingEnabled_shouldAllowMockModem() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        final boolean mode = SystemProperties.getBoolean(
                ALLOW_MOCK_MODEM_PROPERTY, false /* default */);

        assertThat(mode).isTrue();
    }

    @Test
    public void updateState_settingEnabled_preferenceShouldBeChecked() {
        SystemProperties.set(ALLOW_MOCK_MODEM_PROPERTY,
                Boolean.toString(true));

        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_settingDisabled_preferenceShouldNotBeChecked() {
        SystemProperties.set(ALLOW_MOCK_MODEM_PROPERTY,
                Boolean.toString(false));

        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void onDeveloperOptionsDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsSwitchDisabled();
        final boolean mode = SystemProperties.getBoolean(
                ALLOW_MOCK_MODEM_PROPERTY,
                false /* default */);

        mController.updateState(mPreference);

        assertThat(mode).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
    }
}
