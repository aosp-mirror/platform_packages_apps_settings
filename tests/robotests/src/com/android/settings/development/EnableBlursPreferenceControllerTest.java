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

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.SystemProperties;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class EnableBlursPreferenceControllerTest {

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private Context mContext;
    private EnableBlursPreferenceController mController;

    @Before
    public void setup() {
        mContext = RuntimeEnvironment.application;
        mController = new EnableBlursPreferenceController(mContext, true);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(
                mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChanged_settingEnabled_enableBlurs() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        final boolean mode = SystemProperties
                .getBoolean(EnableBlursPreferenceController.DISABLE_BLURS_SYSPROP,
                        false /* default */);
        assertThat(mode).isFalse();
    }

    @Test
    public void onPreferenceChanged_settingDisabled_disableBlurs() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        final boolean mode = SystemProperties
                .getBoolean(EnableBlursPreferenceController.DISABLE_BLURS_SYSPROP,
                        false /* default */);

        assertThat(mode).isTrue();
    }

    @Test
    public void updateState_settingEnabled_preferenceShouldNotBeChecked() {
        SystemProperties.set(EnableBlursPreferenceController.DISABLE_BLURS_SYSPROP, "1");
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void updateState_settingDisabled_preferenceShouldBeChecked() {
        SystemProperties.set(EnableBlursPreferenceController.DISABLE_BLURS_SYSPROP, "0");
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void onDeveloperOptionsDisabled_shouldResetPreference() {
        mController.onDeveloperOptionsDisabled();
        // Can predict true or false, depends on device config.
        verify(mPreference).setChecked(anyBoolean());
    }

    @Test
    public void isAvailable_whenSupported() {
        assertThat(mController.isAvailable()).isTrue();

        mController = new EnableBlursPreferenceController(mContext, false);
        assertThat(mController.isAvailable()).isFalse();
    }
}
