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

package com.android.settings.development.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.development.BluetoothA2dpConfigStore;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBluetoothDevice;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothDevice.class})
public class BluetoothHDAudioPreferenceControllerTest {

    private static final String TEST_DEVICE_ADDRESS = "00:A1:A1:A1:A1:A1";

    @Mock
    private BluetoothA2dp mBluetoothA2dp;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private AbstractBluetoothPreferenceController.Callback mCallback;

    private BluetoothHDAudioPreferenceController mController;
    private SwitchPreference mPreference;
    private BluetoothA2dpConfigStore mBluetoothA2dpConfigStore;
    private BluetoothDevice mActiveDevice;
    private Context mContext;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mBluetoothA2dpConfigStore = spy(new BluetoothA2dpConfigStore());
        mController = new BluetoothHDAudioPreferenceController(mContext, mLifecycle,
                mBluetoothA2dpConfigStore, mCallback);
        mPreference = new SwitchPreference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
        mActiveDevice = ShadowBluetoothDevice.newInstance(TEST_DEVICE_ADDRESS);
    }

    @Test
    public void updateState_noActiveDevice_setDisable() {
        when(mBluetoothA2dp.getActiveDevice()).thenReturn(null);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_codecSupported_setEnable() {
        when(mBluetoothA2dp.getActiveDevice()).thenReturn(mActiveDevice);
        when(mBluetoothA2dp.isOptionalCodecsSupported(mActiveDevice)).thenReturn(
                mBluetoothA2dp.OPTIONAL_CODECS_SUPPORTED);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_codecNotSupported_setDisable() {
        when(mBluetoothA2dp.getActiveDevice()).thenReturn(mActiveDevice);
        when(mBluetoothA2dp.isOptionalCodecsSupported(mActiveDevice)).thenReturn(
                mBluetoothA2dp.OPTIONAL_CODECS_NOT_SUPPORTED);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_codecSupportedAndEnabled_checked() {
        when(mBluetoothA2dp.getActiveDevice()).thenReturn(mActiveDevice);
        when(mBluetoothA2dp.isOptionalCodecsSupported(mActiveDevice)).thenReturn(
                mBluetoothA2dp.OPTIONAL_CODECS_SUPPORTED);
        when(mBluetoothA2dp.isOptionalCodecsEnabled(mActiveDevice)).thenReturn(
                mBluetoothA2dp.OPTIONAL_CODECS_PREF_ENABLED);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_codecSupportedAndDisabled_notChecked() {
        when(mBluetoothA2dp.getActiveDevice()).thenReturn(mActiveDevice);
        when(mBluetoothA2dp.isOptionalCodecsSupported(mActiveDevice)).thenReturn(
                mBluetoothA2dp.OPTIONAL_CODECS_SUPPORTED);
        when(mBluetoothA2dp.isOptionalCodecsEnabled(mActiveDevice)).thenReturn(
                mBluetoothA2dp.OPTIONAL_CODECS_PREF_DISABLED);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void onPreferenceChange_disable_verifyFlow() {
        when(mBluetoothA2dp.getActiveDevice()).thenReturn(mActiveDevice);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        final boolean enabled = false;
        mController.onPreferenceChange(mPreference, enabled);

        verify(mBluetoothA2dp).disableOptionalCodecs(mActiveDevice);
        verify(mBluetoothA2dp).setOptionalCodecsEnabled(mActiveDevice,
                BluetoothA2dp.OPTIONAL_CODECS_PREF_DISABLED);
        verify(mCallback).onBluetoothHDAudioEnabled(enabled);
    }

    @Test
    public void onPreferenceChange_enable_verifyFlow() {
        when(mBluetoothA2dp.getActiveDevice()).thenReturn(mActiveDevice);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        final boolean enabled = true;
        mController.onPreferenceChange(mPreference, enabled);

        verify(mBluetoothA2dp).enableOptionalCodecs(mActiveDevice);
        verify(mBluetoothA2dp).setOptionalCodecsEnabled(mActiveDevice,
                BluetoothA2dp.OPTIONAL_CODECS_PREF_ENABLED);
        verify(mCallback).onBluetoothHDAudioEnabled(enabled);
    }
}
