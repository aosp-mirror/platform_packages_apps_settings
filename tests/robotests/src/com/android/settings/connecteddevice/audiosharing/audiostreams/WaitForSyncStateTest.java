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

import static com.android.settings.connecteddevice.audiosharing.audiostreams.WaitForSyncState.AUDIO_STREAM_WAIT_FOR_SYNC_STATE_SUMMARY;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.WaitForSyncState.WAIT_FOR_SYNC_TIMEOUT_MILLIS;

import static com.google.common.truth.Truth.assertThat;

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
public class WaitForSyncStateTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock private AudioStreamPreference mMockPreference;
    @Mock private AudioStreamsProgressCategoryController mMockController;
    @Mock private AudioStreamsHelper mMockHelper;
    @Mock private BluetoothLeBroadcastMetadata mMockMetadata;
    private WaitForSyncState mInstance;

    @Before
    public void setUp() {
        mInstance = WaitForSyncState.getInstance();
    }

    @Test
    public void testGetInstance() {
        assertThat(mInstance).isNotNull();
        assertThat(mInstance).isInstanceOf(AudioStreamStateHandler.class);
    }

    @Test
    public void testGetSummary() {
        int summary = mInstance.getSummary();
        assertThat(summary).isEqualTo(AUDIO_STREAM_WAIT_FOR_SYNC_STATE_SUMMARY);
    }

    @Test
    public void testGetStateEnum() {
        AudioStreamsProgressCategoryController.AudioStreamState stateEnum =
                mInstance.getStateEnum();
        assertThat(stateEnum)
                .isEqualTo(AudioStreamsProgressCategoryController.AudioStreamState.WAIT_FOR_SYNC);
    }

    @Test
    public void testPerformAction_timeout_stateNotMatching_doNothing() {
        when(mMockPreference.isShown()).thenReturn(true);
        when(mMockPreference.getAudioStreamState())
                .thenReturn(AudioStreamsProgressCategoryController.AudioStreamState.UNKNOWN);

        mInstance.performAction(mMockPreference, mMockController, mMockHelper);
        ShadowLooper.idleMainLooper(WAIT_FOR_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        verify(mMockController, never()).handleSourceLost(anyInt());
    }

    @Test
    public void testPerformAction_timeout_stateMatching_sourceLost() {
        when(mMockPreference.isShown()).thenReturn(true);
        when(mMockPreference.getAudioStreamState())
                .thenReturn(AudioStreamsProgressCategoryController.AudioStreamState.WAIT_FOR_SYNC);
        when(mMockPreference.getAudioStreamBroadcastId()).thenReturn(1);
        when(mMockPreference.getAudioStreamMetadata()).thenReturn(mMockMetadata);
        when(mMockPreference.getSourceOriginForLogging())
                .thenReturn(SourceOriginForLogging.UNKNOWN);

        mInstance.performAction(mMockPreference, mMockController, mMockHelper);
        ShadowLooper.idleMainLooper(WAIT_FOR_SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        verify(mMockController).handleSourceLost(1);
    }
}
