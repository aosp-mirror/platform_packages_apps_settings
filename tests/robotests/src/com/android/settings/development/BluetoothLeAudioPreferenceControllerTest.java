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

import static com.android.settings.development.BluetoothLeAudioPreferenceController
        .LE_AUDIO_DYNAMIC_ENABLED_PROPERTY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
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
public class BluetoothLeAudioPreferenceControllerTest {

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
    }

    @Test
    public void onRebootDialogConfirmedAsLeAudioDisabled_shouldSwitchStatus() {
        when(mBluetoothAdapter.isLeAudioSupported())
                .thenReturn(BluetoothStatusCodes.FEATURE_NOT_SUPPORTED);
        mController.mChanged = true;

        mController.onRebootDialogConfirmed();
        final boolean status = SystemProperties
                .getBoolean(LE_AUDIO_DYNAMIC_ENABLED_PROPERTY, false);
        assertThat(status).isTrue();
    }

    @Test
    public void onRebootDialogConfirmedAsLeAudioEnabled_shouldSwitchStatus() {
        when(mBluetoothAdapter.isLeAudioSupported())
                .thenReturn(BluetoothStatusCodes.FEATURE_SUPPORTED);
        mController.mChanged = true;

        mController.onRebootDialogConfirmed();
        final boolean status = SystemProperties
                .getBoolean(LE_AUDIO_DYNAMIC_ENABLED_PROPERTY, false);
        assertThat(status).isFalse();
    }

    @Test
    public void onRebootDialogCanceled_shouldNotSwitchStatus() {
        when(mBluetoothAdapter.isLeAudioSupported())
                .thenReturn(BluetoothStatusCodes.FEATURE_NOT_SUPPORTED);
        mController.mChanged = true;

        mController.onRebootDialogCanceled();
        final boolean status = SystemProperties
                .getBoolean(LE_AUDIO_DYNAMIC_ENABLED_PROPERTY, false);
        assertThat(status).isFalse();
    }
}
