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

import static android.bluetooth.BluetoothStatusCodes.FEATURE_SUPPORTED;

import static com.android.settings.development.BluetoothLeAudioAllowListPreferenceController
        .LE_AUDIO_ALLOW_LIST_ENABLED_PROPERTY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
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
public class BluetoothLeAudioAllowListPreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private DevelopmentSettingsDashboardFragment mFragment;

    @Mock
    private BluetoothAdapter mBluetoothAdapter;

    private Context mContext;
    private SwitchPreference mPreference;
    private BluetoothLeAudioPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreference = new SwitchPreference(mContext);
        mController = spy(new BluetoothLeAudioPreferenceController(mContext, mFragment));
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.mBluetoothAdapter = mBluetoothAdapter;
        mController.displayPreference(mPreferenceScreen);
        when(mBluetoothAdapter.isLeAudioSupported())
            .thenReturn(FEATURE_SUPPORTED);
    }

    @Test
    public void onRebootDialogConfirmedAsLeAudioAllowListDisabled_shouldSwitchStatus() {
        SystemProperties.set(LE_AUDIO_ALLOW_LIST_ENABLED_PROPERTY, Boolean.toString(false));
        mController.mChanged = true;

        mController.onRebootDialogConfirmed();
        final boolean mode = SystemProperties.getBoolean(
                LE_AUDIO_ALLOW_LIST_ENABLED_PROPERTY, false);
        assertThat(mode).isFalse();
    }


    @Test
    public void onRebootDialogConfirmedAsLeAudioAllowListEnabled_shouldSwitchStatus() {
        SystemProperties.set(LE_AUDIO_ALLOW_LIST_ENABLED_PROPERTY, Boolean.toString(true));
        mController.mChanged = true;

        mController.onRebootDialogConfirmed();
        final boolean status = SystemProperties.getBoolean(
                LE_AUDIO_ALLOW_LIST_ENABLED_PROPERTY, false);
        assertThat(status).isTrue();
    }

    @Test
    public void onRebootDialogCanceled_shouldNotSwitchStatus() {
        SystemProperties.set(LE_AUDIO_ALLOW_LIST_ENABLED_PROPERTY, Boolean.toString(false));
        mController.mChanged = true;

        mController.onRebootDialogCanceled();
        final boolean status = SystemProperties.getBoolean(
                LE_AUDIO_ALLOW_LIST_ENABLED_PROPERTY, false);
        assertThat(status).isFalse();
    }
}
