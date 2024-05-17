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

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AddSourceWaitForResponseState.ADD_SOURCE_WAIT_FOR_RESPONSE_TIMEOUT_MILLIS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothLeBroadcastMetadata;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class AddSourceWaitForResponseStateTest {
    private static final int BROADCAST_ID = 1;
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock private AudioStreamPreference mMockPreference;
    @Mock private AudioStreamsProgressCategoryController mMockController;
    @Mock private AudioStreamsHelper mMockHelper;
    @Mock private BluetoothLeBroadcastMetadata mMockMetadata;
    private AddSourceWaitForResponseState mInstance;

    @Before
    public void setUp() {
        mInstance = AddSourceWaitForResponseState.getInstance();
    }

    @Test
    public void testGetInstance() {
        assertThat(mInstance).isNotNull();
        assertThat(mInstance).isInstanceOf(AudioStreamStateHandler.class);
    }

    @Test
    public void testGetSummary() {
        int summary = mInstance.getSummary();
        assertThat(summary)
                .isEqualTo(
                        AddSourceWaitForResponseState
                                .AUDIO_STREAM_ADD_SOURCE_WAIT_FOR_RESPONSE_STATE_SUMMARY);
    }

    @Test
    public void testGetStateEnum() {
        AudioStreamsProgressCategoryController.AudioStreamState stateEnum =
                mInstance.getStateEnum();
        assertThat(stateEnum)
                .isEqualTo(
                        AudioStreamsProgressCategoryController.AudioStreamState
                                .ADD_SOURCE_WAIT_FOR_RESPONSE);
    }

    @Test
    public void testPerformAction_metadataIsNull_doNothing() {
        when(mMockPreference.getAudioStreamMetadata()).thenReturn(null);

        mInstance.performAction(mMockPreference, mMockController, mMockHelper);

        verify(mMockHelper, never()).addSource(any());
    }

    @Test
    public void testPerformAction_metadataIsNotNull_addSource() {
        when(mMockPreference.getAudioStreamMetadata()).thenReturn(mMockMetadata);

        mInstance.performAction(mMockPreference, mMockController, mMockHelper);

        verify(mMockHelper).addSource(mMockMetadata);
        verify(mMockController, never()).handleSourceFailedToConnect(anyInt());
    }

    @Test
    public void testPerformAction_timeout_addSource_sourceFailedToConnect() {
        when(mMockPreference.getAudioStreamMetadata()).thenReturn(mMockMetadata);
        when(mMockPreference.isShown()).thenReturn(true);
        when(mMockPreference.getAudioStreamState()).thenReturn(mInstance.getStateEnum());
        when(mMockPreference.getAudioStreamBroadcastId()).thenReturn(BROADCAST_ID);

        mInstance.performAction(mMockPreference, mMockController, mMockHelper);
        ShadowLooper.idleMainLooper(ADD_SOURCE_WAIT_FOR_RESPONSE_TIMEOUT_MILLIS, TimeUnit.SECONDS);

        verify(mMockHelper).addSource(mMockMetadata);
        verify(mMockController).handleSourceFailedToConnect(BROADCAST_ID);
    }
}
