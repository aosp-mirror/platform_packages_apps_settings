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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;
import android.view.View;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows.ShadowAudioStreamsHelper;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.ActionButtonsPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowThreadUtils.class,
            ShadowAudioStreamsHelper.class,
        })
public class AudioStreamButtonControllerTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private static final String KEY = "audio_stream_button";
    private static final int BROADCAST_ID = 1;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private AudioStreamsHelper mAudioStreamsHelper;
    @Mock private PreferenceScreen mScreen;
    @Mock private BluetoothLeBroadcastReceiveState mBroadcastReceiveState;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private AudioStreamsRepository mRepository;
    @Mock private ActionButtonsPreference mPreference;
    @Mock private BluetoothDevice mSourceDevice;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private FakeFeatureFactory mFeatureFactory;
    private AudioStreamButtonController mController;

    @Before
    public void setUp() {
        mSetFlagsRule.disableFlags(FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX);
        ShadowAudioStreamsHelper.setUseMock(mAudioStreamsHelper);
        when(mAudioStreamsHelper.getLeBroadcastAssistant()).thenReturn(mAssistant);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mController = new AudioStreamButtonController(mContext, KEY);
        mController.init(BROADCAST_ID);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        when(mScreen.findPreference(KEY)).thenReturn(mPreference);
        when(mPreference.getContext()).thenReturn(mContext);
        when(mPreference.setButton1Text(anyInt())).thenReturn(mPreference);
        when(mPreference.setButton1Icon(anyInt())).thenReturn(mPreference);
        when(mPreference.setButton1Enabled(anyBoolean())).thenReturn(mPreference);
        when(mPreference.setButton1OnClickListener(any(View.OnClickListener.class)))
                .thenReturn(mPreference);
    }

    @After
    public void tearDown() {
        ShadowAudioStreamsHelper.reset();
    }

    @Test
    public void onStart_registerCallbacks() {
        mController.onStart(mLifecycleOwner);
        verify(mAssistant)
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcastAssistant.Callback.class));
    }

    @Test
    public void onStart_profileNull_doNothing() {
        when(mAudioStreamsHelper.getLeBroadcastAssistant()).thenReturn(null);
        mController = new AudioStreamButtonController(mContext, KEY);
        mController.onStart(mLifecycleOwner);
        verify(mAssistant, never())
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcastAssistant.Callback.class));
    }

    @Test
    public void onStop_unregisterCallbacks() {
        mController.onStop(mLifecycleOwner);
        verify(mAssistant)
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
    }

    @Test
    public void onStop_profileNull_doNothing() {
        when(mAudioStreamsHelper.getLeBroadcastAssistant()).thenReturn(null);
        mController = new AudioStreamButtonController(mContext, KEY);
        mController.onStop(mLifecycleOwner);
        verify(mAssistant, never())
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
    }

    @Test
    public void testDisplayPreference_sourceConnected_setDisconnectButton() {
        when(mAudioStreamsHelper.getAllConnectedSources())
                .thenReturn(List.of(mBroadcastReceiveState));
        when(mBroadcastReceiveState.getBroadcastId()).thenReturn(BROADCAST_ID);

        mController.displayPreference(mScreen);

        verify(mPreference).setButton1Enabled(true);
        verify(mPreference).setButton1Text(R.string.audio_streams_disconnect);
        verify(mPreference).setButton1Icon(com.android.settings.R.drawable.ic_settings_close);

        ArgumentCaptor<View.OnClickListener> listenerCaptor =
                ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(mPreference).setButton1OnClickListener(listenerCaptor.capture());
        var listener = listenerCaptor.getValue();

        assertThat(listener).isNotNull();
        listener.onClick(mock(View.class));
        verify(mAudioStreamsHelper).removeSource(BROADCAST_ID);
        verify(mPreference).setButton1Enabled(false);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(any(), eq(SettingsEnums.ACTION_AUDIO_STREAM_LEAVE_BUTTON_CLICK));
    }

    @Test
    public void testDisplayPreference_sourceNotConnected_setConnectButton() {
        when(mAudioStreamsHelper.getAllConnectedSources()).thenReturn(Collections.emptyList());
        mController.setAudioStreamsRepositoryForTesting(mRepository);
        var metadataToRejoin = mock(BluetoothLeBroadcastMetadata.class);
        when(mRepository.getSavedMetadata(any(), anyInt())).thenReturn(metadataToRejoin);

        mController.displayPreference(mScreen);

        verify(mPreference).setButton1Enabled(true);
        verify(mPreference).setButton1Text(R.string.audio_streams_connect);
        verify(mPreference).setButton1Icon(com.android.settings.R.drawable.ic_add_24dp);

        ArgumentCaptor<View.OnClickListener> listenerCaptor =
                ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(mPreference).setButton1OnClickListener(listenerCaptor.capture());
        var listener = listenerCaptor.getValue();

        assertThat(listener).isNotNull();
        listener.onClick(mock(View.class));
        verify(mAudioStreamsHelper).addSource(metadataToRejoin);
        verify(mPreference).setButton1Enabled(false);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(any(), eq(SettingsEnums.ACTION_AUDIO_STREAM_JOIN), anyInt());
    }

    @Test
    public void testCallback_onSourceRemoved_updateButton() {
        when(mAudioStreamsHelper.getAllConnectedSources()).thenReturn(Collections.emptyList());

        mController.displayPreference(mScreen);
        mController.mBroadcastAssistantCallback.onSourceRemoved(
                mock(BluetoothDevice.class), /* sourceId= */ 0, /* reason= */ 0);

        // Called twice, once in displayPreference, the other one in callback
        verify(mPreference, times(2)).setButton1Enabled(true);
        verify(mPreference, times(2)).setButton1Text(R.string.audio_streams_connect);
        verify(mPreference, times(2)).setButton1Icon(com.android.settings.R.drawable.ic_add_24dp);
    }

    @Test
    public void testCallback_onSourceRemovedFailed_updateButton() {
        when(mAudioStreamsHelper.getAllConnectedSources())
                .thenReturn(List.of(mBroadcastReceiveState));
        when(mBroadcastReceiveState.getBroadcastId()).thenReturn(BROADCAST_ID);

        mController.displayPreference(mScreen);
        mController.mBroadcastAssistantCallback.onSourceRemoveFailed(
                mock(BluetoothDevice.class), /* sourceId= */ 0, /* reason= */ 0);

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(any(), eq(SettingsEnums.ACTION_AUDIO_STREAM_LEAVE_FAILED));

        // Called twice, once in displayPreference, the other one in callback
        verify(mPreference, times(2)).setButton1Enabled(true);
        verify(mPreference, times(2)).setButton1Text(R.string.audio_streams_disconnect);
        verify(mPreference, times(2))
                .setButton1Icon(com.android.settings.R.drawable.ic_settings_close);
    }

    @Test
    public void testCallback_onReceiveStateChanged_updateButton() {
        when(mAudioStreamsHelper.getAllConnectedSources())
                .thenReturn(List.of(mBroadcastReceiveState));
        when(mBroadcastReceiveState.getBroadcastId()).thenReturn(BROADCAST_ID);
        BluetoothLeBroadcastReceiveState state = mock(BluetoothLeBroadcastReceiveState.class);
        List<Long> bisSyncState = new ArrayList<>();
        bisSyncState.add(1L);
        when(state.getBisSyncState()).thenReturn(bisSyncState);

        mController.displayPreference(mScreen);
        mController.mBroadcastAssistantCallback.onReceiveStateChanged(
                mock(BluetoothDevice.class), /* sourceId= */ 0, state);

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(any(), eq(SettingsEnums.ACTION_AUDIO_STREAM_JOIN_SUCCEED), anyInt());

        // Called twice, once in displayPreference, the other one in callback
        verify(mPreference, times(2)).setButton1Enabled(true);
        verify(mPreference, times(2)).setButton1Text(R.string.audio_streams_disconnect);
        verify(mPreference, times(2))
                .setButton1Icon(com.android.settings.R.drawable.ic_settings_close);
    }

    @Test
    public void testCallback_onReceiveStateChangedWithSourcePresent_updateButton() {
        mSetFlagsRule.enableFlags(FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX);
        String address = "11:22:33:44:55:66";

        BluetoothLeBroadcastReceiveState state = mock(BluetoothLeBroadcastReceiveState.class);
        when(state.getBroadcastId()).thenReturn(BROADCAST_ID);
        when(state.getSourceDevice()).thenReturn(mSourceDevice);
        when(mSourceDevice.getAddress()).thenReturn(address);
        List<Long> bisSyncState = new ArrayList<>();
        when(state.getBisSyncState()).thenReturn(bisSyncState);
        when(mAudioStreamsHelper.getAllPresentSources()).thenReturn(List.of(state));

        mController.displayPreference(mScreen);
        mController.mBroadcastAssistantCallback.onReceiveStateChanged(
                mock(BluetoothDevice.class), /* sourceId= */ 0, state);

        verify(mFeatureFactory.metricsFeatureProvider, never())
                .action(any(), eq(SettingsEnums.ACTION_AUDIO_STREAM_JOIN_SUCCEED), anyInt());

        // Called twice, once in displayPreference, the other one in callback
        verify(mPreference, times(2)).setButton1Enabled(true);
        verify(mPreference, times(2)).setButton1Text(R.string.audio_streams_disconnect);
        verify(mPreference, times(2))
                .setButton1Icon(com.android.settings.R.drawable.ic_settings_close);
    }

    @Test
    public void testCallback_onSourceAddFailed_updateButton() {
        when(mAudioStreamsHelper.getAllConnectedSources()).thenReturn(Collections.emptyList());

        mController.displayPreference(mScreen);
        mController.mBroadcastAssistantCallback.onSourceAddFailed(
                mock(BluetoothDevice.class),
                mock(BluetoothLeBroadcastMetadata.class),
                /* reason= */ 0);

        verify(mFeatureFactory.metricsFeatureProvider)
                .action(any(), eq(SettingsEnums.ACTION_AUDIO_STREAM_JOIN_FAILED_OTHER), anyInt());

        // Called twice, once in displayPreference, the other one in callback
        verify(mPreference, times(2)).setButton1Enabled(true);
        verify(mPreference, times(2)).setButton1Text(R.string.audio_streams_connect);
        verify(mPreference, times(2)).setButton1Icon(com.android.settings.R.drawable.ic_add_24dp);
    }

    @Test
    public void testCallback_onSourceLost_updateButton() {
        when(mAudioStreamsHelper.getAllConnectedSources()).thenReturn(Collections.emptyList());

        mController.displayPreference(mScreen);
        mController.mBroadcastAssistantCallback.onSourceLost(/* broadcastId= */ 0);

        // Called twice, once in displayPreference, the other one in callback
        verify(mPreference, times(2)).setButton1Enabled(true);
        verify(mPreference, times(2)).setButton1Text(R.string.audio_streams_connect);
        verify(mPreference, times(2)).setButton1Icon(com.android.settings.R.drawable.ic_add_24dp);
    }
}
