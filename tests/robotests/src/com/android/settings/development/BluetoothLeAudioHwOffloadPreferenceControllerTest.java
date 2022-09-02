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

import static com.android.settings.development.BluetoothLeAudioHwOffloadPreferenceController
        .LE_AUDIO_OFFLOAD_DISABLED_PROPERTY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.SystemProperties;

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
public class BluetoothLeAudioHwOffloadPreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private DevelopmentSettingsDashboardFragment mFragment;

    private Context mContext;
    private SwitchPreference mPreference;
    private BluetoothLeAudioHwOffloadPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreference = new SwitchPreference(mContext);
        mController = spy(new BluetoothLeAudioHwOffloadPreferenceController(mContext, mFragment));
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onLeAudioHwDialogConfirmedAsLeAudioOffloadDisabled_shouldChangeProperty() {
        SystemProperties.set(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(false));
        mController.mChanged = true;

        mController.onRebootDialogConfirmed();
        final boolean mode = SystemProperties.getBoolean(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, false);
        assertThat(mode).isTrue();
    }

    @Test
    public void onLeAudioHwDialogConfirmedAsLeAudioOffloadEnabled_shouldChangeProperty() {
        SystemProperties.set(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(true));
        mController.mChanged = true;

        mController.onRebootDialogConfirmed();
        final boolean mode2 = SystemProperties.getBoolean(
                LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, true);
        assertThat(mode2).isFalse();
    }

    @Test
    public void onLeAudioHwDialogCanceled_shouldNotChangeProperty() {
        SystemProperties.set(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(false));
        mController.mChanged = true;

        mController.onRebootDialogCanceled();
        final boolean mode = SystemProperties.getBoolean(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, false);
        assertThat(mode).isFalse();
    }
}
