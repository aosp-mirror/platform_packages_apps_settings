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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothLeAudioContentMetadata;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsProgressCategoryController.AudioStreamState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class AudioStreamPreferenceTest {
    private static final int BROADCAST_ID = 1;
    private static final String BROADCAST_NAME = "broadcast_name";
    private static final String PROGRAM_NAME = "program_name";
    private static final int BROADCAST_RSSI = 1;
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private AudioStreamPreference mPreference;
    @Mock private BluetoothLeBroadcastMetadata mBluetoothLeBroadcastMetadata;
    @Mock private BluetoothLeBroadcastReceiveState mBluetoothLeBroadcastReceiveState;
    @Mock private BluetoothLeAudioContentMetadata mBluetoothLeAudioContentMetadata;

    @Before
    public void setUp() {
        mPreference = new AudioStreamPreference(mContext, null);
        when(mBluetoothLeBroadcastMetadata.getBroadcastId()).thenReturn(BROADCAST_ID);
        when(mBluetoothLeBroadcastMetadata.getBroadcastName()).thenReturn(BROADCAST_NAME);
        when(mBluetoothLeBroadcastMetadata.getRssi()).thenReturn(BROADCAST_RSSI);
        when(mBluetoothLeBroadcastReceiveState.getBroadcastId()).thenReturn(BROADCAST_ID);
        when(mBluetoothLeBroadcastReceiveState.getSubgroupMetadata())
                .thenReturn(Collections.singletonList(mBluetoothLeAudioContentMetadata));
        when(mBluetoothLeAudioContentMetadata.getProgramInfo()).thenReturn(PROGRAM_NAME);
    }

    @Test
    public void createNewPreference_shouldSetIcon() {
        assertThat(mPreference.getIcon()).isNotNull();
    }

    @Test
    public void onBind_shouldHideDivider() {
        PreferenceViewHolder holder =
                PreferenceViewHolder.createInstanceForTests(
                        LayoutInflater.from(mContext)
                                .inflate(mPreference.getLayoutResource(), null));
        View divider =
                holder.findViewById(
                        com.android.settingslib.widget.preference.twotarget.R.id
                                .two_target_divider);
        assertThat(divider).isNotNull();

        mPreference.onBindViewHolder(holder);

        assertThat(divider.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void setAudioStreamMetadata_shouldUpdateMetadata() {
        AudioStreamPreference p =
                AudioStreamPreference.fromMetadata(
                        mContext, mBluetoothLeBroadcastMetadata, SourceOriginForLogging.UNKNOWN);
        BluetoothLeBroadcastMetadata metadata = mock(BluetoothLeBroadcastMetadata.class);
        p.setAudioStreamMetadata(metadata);

        assertThat(p.getAudioStreamMetadata()).isEqualTo(metadata);
    }

    @Test
    public void setAudioStreamState_shouldUpdateState() {
        AudioStreamPreference p =
                AudioStreamPreference.fromMetadata(
                        mContext, mBluetoothLeBroadcastMetadata, SourceOriginForLogging.UNKNOWN);
        AudioStreamState state = AudioStreamState.SOURCE_ADDED;
        p.setAudioStreamState(state);

        assertThat(p.getAudioStreamState()).isEqualTo(state);
    }

    @Test
    public void fromMetadata_shouldReturnBroadcastInfo() {
        AudioStreamPreference p =
                AudioStreamPreference.fromMetadata(
                        mContext, mBluetoothLeBroadcastMetadata, SourceOriginForLogging.UNKNOWN);
        assertThat(p.getAudioStreamBroadcastId()).isEqualTo(BROADCAST_ID);
        assertThat(p.getAudioStreamBroadcastName()).isEqualTo(BROADCAST_NAME);
        assertThat(p.getAudioStreamRssi()).isEqualTo(BROADCAST_RSSI);
    }

    @Test
    public void fromReceiveState_shouldReturnBroadcastInfo() {
        AudioStreamPreference p =
                AudioStreamPreference.fromReceiveState(mContext, mBluetoothLeBroadcastReceiveState);
        assertThat(p.getAudioStreamBroadcastId()).isEqualTo(BROADCAST_ID);
        assertThat(p.getAudioStreamBroadcastName()).isEqualTo(PROGRAM_NAME);
        assertThat(p.getAudioStreamRssi()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void shouldHideSecondTarget_connected() {
        mPreference.setIsConnected(true);
        assertThat(mPreference.shouldHideSecondTarget()).isTrue();
    }

    @Test
    public void shouldHideSecondTarget_notEncrypted() {
        when(mBluetoothLeBroadcastMetadata.isEncrypted()).thenReturn(false);
        AudioStreamPreference p =
                AudioStreamPreference.fromMetadata(
                        mContext, mBluetoothLeBroadcastMetadata, SourceOriginForLogging.UNKNOWN);
        assertThat(p.shouldHideSecondTarget()).isTrue();
    }

    @Test
    public void shouldShowSecondTarget_encrypted() {
        when(mBluetoothLeBroadcastMetadata.isEncrypted()).thenReturn(true);
        AudioStreamPreference p =
                AudioStreamPreference.fromMetadata(
                        mContext, mBluetoothLeBroadcastMetadata, SourceOriginForLogging.UNKNOWN);
        assertThat(p.shouldHideSecondTarget()).isFalse();
    }

    @Test
    public void secondTargetResId_shouldReturnLockLayoutId() {
        assertThat(mPreference.getSecondTargetResId()).isEqualTo(R.layout.preference_widget_lock);
    }
}
