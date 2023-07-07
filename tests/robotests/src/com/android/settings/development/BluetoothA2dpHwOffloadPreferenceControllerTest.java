/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static com.android.settings.development.BluetoothA2dpHwOffloadPreferenceController
        .A2DP_OFFLOAD_DISABLED_PROPERTY;
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
public class BluetoothA2dpHwOffloadPreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private DevelopmentSettingsDashboardFragment mFragment;

    private Context mContext;
    private SwitchPreference mPreference;
    private BluetoothA2dpHwOffloadPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreference = new SwitchPreference(mContext);
        mController = spy(new BluetoothA2dpHwOffloadPreferenceController(mContext, mFragment));
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onA2dpHwDialogConfirmedAsA2dpOffloadDisabled_shouldChangeProperty() {
        SystemProperties.set(A2DP_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(true));
        mController.mChanged = true;

        mController.onRebootDialogConfirmed();
        final boolean mode = SystemProperties.getBoolean(A2DP_OFFLOAD_DISABLED_PROPERTY, false);
        assertThat(mode).isFalse();
    }

    @Test
    public void onA2dpHwDialogConfirmedAsA2dpOffloadEnabled_shouldChangeProperty() {
        SystemProperties.set(A2DP_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(false));
        SystemProperties.set(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(false));

        mController.mChanged = true;

        mController.onRebootDialogConfirmed();
        final boolean a2dpMode = SystemProperties.getBoolean(A2DP_OFFLOAD_DISABLED_PROPERTY, true);
        final boolean leAudioMode = SystemProperties
                .getBoolean(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, true);
        assertThat(a2dpMode).isTrue();
        assertThat(leAudioMode).isTrue();
    }

    @Test
    public void onA2dpHwDialogCanceled_shouldNotChangeProperty() {
        SystemProperties.set(A2DP_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(false));
        mController.mChanged = true;

        mController.onRebootDialogCanceled();
        final boolean mode = SystemProperties.getBoolean(A2DP_OFFLOAD_DISABLED_PROPERTY, false);
        assertThat(mode).isFalse();
    }
}
