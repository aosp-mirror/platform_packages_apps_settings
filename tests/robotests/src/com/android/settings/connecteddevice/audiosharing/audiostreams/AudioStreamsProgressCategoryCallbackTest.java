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

import static com.android.settingslib.flags.Flags.FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX;
import static com.android.settingslib.flags.Flags.FLAG_ENABLE_LE_AUDIO_SHARING;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
        })
public class AudioStreamsProgressCategoryCallbackTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private AudioStreamsProgressCategoryController mController;
    @Mock private BluetoothDevice mDevice;
    @Mock private BluetoothLeBroadcastReceiveState mState;
    @Mock private BluetoothLeBroadcastMetadata mMetadata;
    @Mock private BluetoothDevice mSourceDevice;
    private AudioStreamsProgressCategoryCallback mCallback;

    @Before
    public void setUp() {
        mSetFlagsRule.disableFlags(FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX);
        ShadowBluetoothAdapter shadowBluetoothAdapter =
                Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        shadowBluetoothAdapter.setEnabled(true);
        shadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        shadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mCallback = new AudioStreamsProgressCategoryCallback(mContext, mController);
    }

    @Test
    public void testOnReceiveStateChanged_connected() {
        List<Long> bisSyncState = new ArrayList<>();
        bisSyncState.add(1L);
        when(mState.getBisSyncState()).thenReturn(bisSyncState);
        mCallback.onReceiveStateChanged(mDevice, /* sourceId= */ 0, mState);

        verify(mController).handleSourceConnected(any(), any());
    }

    @Test
    public void testOnReceiveStateChanged_sourcePresent() {
        mSetFlagsRule.enableFlags(FLAG_ENABLE_LE_AUDIO_SHARING);
        mSetFlagsRule.enableFlags(FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX);
        String address = "11:22:33:44:55:66";

        List<Long> bisSyncState = new ArrayList<>();
        when(mState.getBisSyncState()).thenReturn(bisSyncState);
        when(mState.getSourceDevice()).thenReturn(mSourceDevice);
        when(mSourceDevice.getAddress()).thenReturn(address);
        mCallback.onReceiveStateChanged(mDevice, /* sourceId= */ 0, mState);

        verify(mController).handleSourcePresent(any(), any());
    }

    @Test
    public void testOnReceiveStateChanged_badCode() {
        when(mState.getPaSyncState())
                .thenReturn(BluetoothLeBroadcastReceiveState.PA_SYNC_STATE_SYNCHRONIZED);
        when(mState.getBigEncryptionState())
                .thenReturn(BluetoothLeBroadcastReceiveState.BIG_ENCRYPTION_STATE_BAD_CODE);
        mCallback.onReceiveStateChanged(mDevice, /* sourceId= */ 0, mState);

        verify(mController).handleSourceConnectBadCode(any());
    }

    @Test
    public void testOnSearchStartFailed() {
        mCallback.onSearchStartFailed(/* reason= */ 0);

        verify(mController).showToast(anyString());
        verify(mController).setScanning(anyBoolean());
    }

    @Test
    public void testOnSearchStarted() {
        mCallback.onSearchStarted(/* reason= */ 0);

        verify(mController).setScanning(anyBoolean());
    }

    @Test
    public void testOnSearchStopFailed() {
        mCallback.onSearchStopFailed(/* reason= */ 0);

        verify(mController).showToast(anyString());
    }

    @Test
    public void testOnSearchStopped() {
        mCallback.onSearchStopped(/* reason= */ 0);

        verify(mController).setScanning(anyBoolean());
    }

    @Test
    public void testOnSourceAddFailed() {
        when(mMetadata.getBroadcastId()).thenReturn(1);
        mCallback.onSourceAddFailed(mDevice, mMetadata, /* reason= */ 0);

        verify(mController).handleSourceFailedToConnect(1);
    }

    @Test
    public void testOnSourceFound() {
        mCallback.onSourceFound(mMetadata);

        verify(mController).handleSourceFound(mMetadata);
    }

    @Test
    public void testOnSourceLost() {
        mCallback.onSourceLost(/* broadcastId= */ 1);

        verify(mController).handleSourceLost(1);
    }

    @Test
    public void testOnSourceRemoveFailed() {
        mCallback.onSourceRemoveFailed(mDevice, /* sourceId= */ 0, /* reason= */ 0);

        verify(mController).showToast(anyString());
    }

    @Test
    public void testOnSourceRemoved() {
        mCallback.onSourceRemoved(mDevice, /* sourceId= */ 0, /* reason= */ 0);

        verify(mController).handleSourceRemoved();
    }
}
