/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.flags.Flags;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
        })
public class AudioStreamConfirmDialogActivityTest {
    @Rule public final MockitoRule mocks = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock private LocalBluetoothManager mLocalBluetoothManager;
    @Mock private LocalBluetoothProfileManager mLocalBluetoothProfileManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private VolumeControlProfile mVolumeControl;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private AudioStreamConfirmDialogActivity mActivity;

    @Before
    public void setUp() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mLocalBluetoothProfileManager);
        when(mLocalBluetoothProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        when(mLocalBluetoothProfileManager.getLeAudioBroadcastAssistantProfile())
                .thenReturn(mAssistant);
        when(mLocalBluetoothProfileManager.getVolumeControlProfile()).thenReturn(mVolumeControl);
        when(mBroadcast.isProfileReady()).thenReturn(true);
        when(mAssistant.isProfileReady()).thenReturn(true);
        when(mVolumeControl.isProfileReady()).thenReturn(true);
    }

    @After
    public void tearDown() {
        ShadowBluetoothUtils.reset();
    }

    @Test
    public void isValidFragment_returnsTrue() {
        mActivity = Robolectric.setupActivity(AudioStreamConfirmDialogActivity.class);
        assertThat(mActivity.isValidFragment(AudioStreamConfirmDialog.class.getName())).isTrue();
    }

    @Test
    public void isValidFragment_returnsFalse() {
        mActivity = Robolectric.setupActivity(AudioStreamConfirmDialogActivity.class);
        assertThat(mActivity.isValidFragment("")).isFalse();
    }

    @Test
    public void isToolbarEnabled_returnsFalse() {
        mActivity = Robolectric.setupActivity(AudioStreamConfirmDialogActivity.class);
        assertThat(mActivity.isToolbarEnabled()).isFalse();
    }

    @Test
    public void setupActivity_serviceNotReady_registerCallback() {
        when(mBroadcast.isProfileReady()).thenReturn(false);
        mActivity = Robolectric.setupActivity(AudioStreamConfirmDialogActivity.class);

        verify(mLocalBluetoothProfileManager).addServiceListener(any());
    }

    @Test
    public void setupActivity_serviceNotReady_registerCallback_onServiceCallback() {
        when(mBroadcast.isProfileReady()).thenReturn(false);
        mActivity = Robolectric.setupActivity(AudioStreamConfirmDialogActivity.class);

        verify(mLocalBluetoothProfileManager).addServiceListener(any());

        when(mBroadcast.isProfileReady()).thenReturn(true);
        mActivity.onServiceConnected();
        verify(mLocalBluetoothProfileManager).removeServiceListener(any());

        mActivity.onServiceDisconnected();
        // Do nothing.
    }

    @Test
    public void setupActivity_serviceReady_doNothing() {
        mActivity = Robolectric.setupActivity(AudioStreamConfirmDialogActivity.class);

        verify(mLocalBluetoothProfileManager, never()).addServiceListener(any());
    }

    @Test
    public void onStop_unregisterCallback() {
        mActivity = Robolectric.setupActivity(AudioStreamConfirmDialogActivity.class);
        mActivity.onStop();

        verify(mLocalBluetoothProfileManager).removeServiceListener(any());
    }
}
