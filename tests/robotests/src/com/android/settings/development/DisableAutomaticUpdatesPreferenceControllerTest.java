/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DisableAutomaticUpdatesPreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private SwitchPreference mPreference;

    private Context mContext;
    private DisableAutomaticUpdatesPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new DisableAutomaticUpdatesPreferenceController(mContext);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(
                mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChanged_turnOnAutomaticUpdates() {
        mController.onPreferenceChange(null, true);

        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.OTA_DISABLE_AUTOMATIC_UPDATE, -1);

        assertThat(mode).isEqualTo(
                DisableAutomaticUpdatesPreferenceController.ENABLE_UPDATES_SETTING);
    }

    @Test
    public void onPreferenceChanged_turnOffAutomaticUpdates() {
        mController.onPreferenceChange(null, false);

        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.OTA_DISABLE_AUTOMATIC_UPDATE, -1);

        assertThat(mode).isEqualTo(
                DisableAutomaticUpdatesPreferenceController.DISABLE_UPDATES_SETTING);
    }

    @Test
    public void updateState_preferenceShouldBeChecked() {
        Settings.Global
                .putInt(mContext.getContentResolver(), Settings.Global.OTA_DISABLE_AUTOMATIC_UPDATE,
                        DisableAutomaticUpdatesPreferenceController.ENABLE_UPDATES_SETTING);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_preferenceShouldNotBeChecked() {
        Settings.Global
                .putInt(mContext.getContentResolver(), Settings.Global.OTA_DISABLE_AUTOMATIC_UPDATE,
                        DisableAutomaticUpdatesPreferenceController.DISABLE_UPDATES_SETTING);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsDisabled();

        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
    }
}
