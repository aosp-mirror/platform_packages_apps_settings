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
package com.android.settings.core;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;

import com.android.settings.TestConfig;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION_O)
public class TogglePreferenceControllerTest {

    @Mock
    TogglePreferenceController mTogglePreferenceController;

    Context mContext;
    SwitchPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreference = new SwitchPreference(mContext);
    }

    @Test
    public void testSetsPreferenceValue_setsChecked() {
        when(mTogglePreferenceController.isChecked()).thenReturn(true);
        mPreference.setChecked(false);

        mTogglePreferenceController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void testSetsPreferenceValue_setsNotChecked() {
        when(mTogglePreferenceController.isChecked()).thenReturn(false);
        mPreference.setChecked(true);

        mTogglePreferenceController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void testUpdatesPreferenceOnChange_turnsOn() {
        boolean newValue = true;

        mTogglePreferenceController.onPreferenceChange(mPreference, newValue);

        verify(mTogglePreferenceController).setChecked(newValue);
    }

    @Test
    public void testUpdatesPreferenceOnChange_turnsOff() {
        boolean newValue = false;

        mTogglePreferenceController.onPreferenceChange(mPreference, newValue);

        verify(mTogglePreferenceController).setChecked(newValue);
    }
}
