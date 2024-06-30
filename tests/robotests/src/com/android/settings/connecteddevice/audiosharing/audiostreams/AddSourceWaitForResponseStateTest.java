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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowAlertDialog.class,
        })
public class AddSourceWaitForResponseStateTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private static final int BROADCAST_ID = 1;
    private final Context mContext = spy(ApplicationProvider.getApplicationContext());
    @Mock private AudioStreamPreference mMockPreference;
    @Mock private AudioStreamsProgressCategoryController mMockController;
    @Mock private AudioStreamsHelper mMockHelper;
    @Mock private BluetoothLeBroadcastMetadata mMockMetadata;
    @Mock private AudioStreamsRepository mMockRepository;
    private FakeFeatureFactory mFeatureFactory;
    private AddSourceWaitForResponseState mInstance;

    @Before
    public void setUp() {
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mInstance = new AddSourceWaitForResponseState();
        when(mMockPreference.getContext()).thenReturn(mContext);
        when(mMockPreference.getSourceOriginForLogging())
                .thenReturn(SourceOriginForLogging.QR_CODE_SCAN_SETTINGS);
    }

    @Test
    public void testGetInstance() {
        mInstance = AddSourceWaitForResponseState.getInstance();
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
        when(mMockPreference.getSourceOriginForLogging())
                .thenReturn(SourceOriginForLogging.QR_CODE_SCAN_SETTINGS);
        mInstance.setAudioStreamsRepositoryForTesting(mMockRepository);

        mInstance.performAction(mMockPreference, mMockController, mMockHelper);

        verify(mMockHelper).addSource(mMockMetadata);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        eq(mContext),
                        eq(SettingsEnums.ACTION_AUDIO_STREAM_JOIN),
                        eq(SourceOriginForLogging.QR_CODE_SCAN_SETTINGS.ordinal()));
        verify(mMockRepository).cacheMetadata(mMockMetadata);
        verify(mMockController, never()).handleSourceFailedToConnect(anyInt());
    }

    @Test
    public void testPerformAction_timeout_addSource_sourceFailedToConnect() {
        when(mMockPreference.getAudioStreamMetadata()).thenReturn(mMockMetadata);
        when(mMockPreference.isShown()).thenReturn(true);
        when(mMockPreference.getAudioStreamState()).thenReturn(mInstance.getStateEnum());
        when(mMockPreference.getAudioStreamBroadcastId()).thenReturn(BROADCAST_ID);
        when(mMockPreference.getSourceOriginForLogging())
                .thenReturn(SourceOriginForLogging.QR_CODE_SCAN_SETTINGS);
        when(mMockController.getFragment()).thenReturn(mock(AudioStreamsDashboardFragment.class));
        mInstance.setAudioStreamsRepositoryForTesting(mMockRepository);

        mInstance.performAction(mMockPreference, mMockController, mMockHelper);
        ShadowLooper.idleMainLooper(ADD_SOURCE_WAIT_FOR_RESPONSE_TIMEOUT_MILLIS, TimeUnit.SECONDS);

        verify(mMockHelper).addSource(mMockMetadata);
        verify(mMockController).handleSourceFailedToConnect(BROADCAST_ID);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        eq(mContext),
                        eq(SettingsEnums.ACTION_AUDIO_STREAM_JOIN),
                        eq(SourceOriginForLogging.QR_CODE_SCAN_SETTINGS.ordinal()));
        verify(mMockRepository).cacheMetadata(mMockMetadata);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        eq(mContext),
                        eq(SettingsEnums.ACTION_AUDIO_STREAM_JOIN_FAILED_TIMEOUT),
                        eq(SourceOriginForLogging.QR_CODE_SCAN_SETTINGS.ordinal()));
        verify(mContext).getString(R.string.audio_streams_dialog_stream_is_not_available);
        verify(mContext).getString(R.string.audio_streams_is_not_playing);
        verify(mContext).getString(R.string.audio_streams_dialog_close);
    }
}
