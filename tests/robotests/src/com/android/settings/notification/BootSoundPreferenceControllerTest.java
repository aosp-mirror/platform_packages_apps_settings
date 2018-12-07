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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.SystemProperties;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BootSoundPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SwitchPreference mPreference;

    private BootSoundPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getResources().getBoolean(R.bool.has_boot_sounds))
                .thenReturn(true);
        mController = new BootSoundPreferenceController(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());
    }

    @Test
    public void isAvailable_hasBootSounds_shouldReturnTrue() {
        when(mContext.getResources().getBoolean(
                R.bool.has_boot_sounds)).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_noBootSounds_shouldReturnFale() {
        when(mContext.getResources().getBoolean(
                R.bool.has_boot_sounds)).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void displayPreference_bootSoundEnabled_shouldCheckedPreference() {
        SystemProperties.set(BootSoundPreferenceController.PROPERTY_BOOT_SOUNDS, "true");
        mController.displayPreference(mScreen);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void displayPreference_bootSoundDisabled_shouldUncheckedPreference() {
        SystemProperties.set(BootSoundPreferenceController.PROPERTY_BOOT_SOUNDS, "0");

        mController.displayPreference(mScreen);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void handlePreferenceTreeClick_preferenceChecked_shouldEnableBootSound() {
        when(mPreference.isChecked()).thenReturn(true);

        mController.handlePreferenceTreeClick(mPreference);

        assertThat(SystemProperties.get(
                BootSoundPreferenceController.PROPERTY_BOOT_SOUNDS, null)).isEqualTo("1");
    }

    @Test
    public void handlePreferenceTreeClick_preferenceUnchecked_shouldDisableBootSound() {
        when(mPreference.isChecked()).thenReturn(false);

        mController.handlePreferenceTreeClick(mPreference);

        assertThat(SystemProperties.get(
                BootSoundPreferenceController.PROPERTY_BOOT_SOUNDS, null)).isEqualTo("0");
    }
}
