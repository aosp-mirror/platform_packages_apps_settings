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
public class ArtVerifierPreferenceControllerTest {

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private Context mContext;
    private ArtVerifierPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new ArtVerifierPreferenceController(mContext);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChanged_settingEnabled_turnOnArtVerifier() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ART_VERIFIER_VERIFY_DEBUGGABLE, -1 /* default */);

        assertThat(mode).isEqualTo(ArtVerifierPreferenceController.SETTING_VALUE_ON);
    }

    @Test
    public void onPreferenceChanged_settingDisabled_turnOffArtVerifier() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ART_VERIFIER_VERIFY_DEBUGGABLE, -1 /* default */);

        assertThat(mode).isEqualTo(ArtVerifierPreferenceController.SETTING_VALUE_OFF);
    }

    @Test
    public void updateState_settingEnabled_preferenceShouldBeChecked() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ART_VERIFIER_VERIFY_DEBUGGABLE,
                ArtVerifierPreferenceController.SETTING_VALUE_ON);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_settingDisabled_preferenceShouldBeChecked() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ART_VERIFIER_VERIFY_DEBUGGABLE,
                ArtVerifierPreferenceController.SETTING_VALUE_OFF);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsSwitchDisabled();
        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ART_VERIFIER_VERIFY_DEBUGGABLE, -1 /* default */);

        assertThat(mode).isEqualTo(ArtVerifierPreferenceController.SETTING_VALUE_ON);
        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(true);
    }
}
