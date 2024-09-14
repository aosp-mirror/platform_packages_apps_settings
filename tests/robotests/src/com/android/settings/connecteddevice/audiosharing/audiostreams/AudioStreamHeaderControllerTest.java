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

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamHeaderController.AUDIO_STREAM_HEADER_LISTENING_NOW_SUMMARY;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamHeaderController.AUDIO_STREAM_HEADER_NOT_LISTENING_SUMMARY;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamHeaderController.AUDIO_STREAM_HEADER_PRESENT_NOW_SUMMARY;
import static com.android.settingslib.flags.Flags.FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows.ShadowAudioStreamsHelper;
import com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows.ShadowEntityHeaderController;
import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
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
            ShadowEntityHeaderController.class,
            ShadowThreadUtils.class,
            ShadowAudioStreamsHelper.class,
        })
public class AudioStreamHeaderControllerTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String KEY = "audio_stream_header";
    private static final int BROADCAST_ID = 1;
    private static final String BROADCAST_NAME = "broadcast name";
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private AudioStreamsHelper mAudioStreamsHelper;
    @Mock private PreferenceScreen mScreen;
    @Mock private BluetoothLeBroadcastReceiveState mBroadcastReceiveState;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private AudioStreamDetailsFragment mFragment;
    @Mock private LayoutPreference mPreference;
    @Mock private EntityHeaderController mHeaderController;
    @Mock private BluetoothDevice mBluetoothDevice;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private AudioStreamHeaderController mController;

    @Before
    public void setUp() {
        mSetFlagsRule.disableFlags(FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX);

        ShadowEntityHeaderController.setUseMock(mHeaderController);
        ShadowAudioStreamsHelper.setUseMock(mAudioStreamsHelper);
        when(mAudioStreamsHelper.getLeBroadcastAssistant()).thenReturn(mAssistant);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mController = new AudioStreamHeaderController(mContext, KEY);
        mController.init(mFragment, BROADCAST_NAME, BROADCAST_ID);
        when(mScreen.findPreference(KEY)).thenReturn(mPreference);
        when(mScreen.getContext()).thenReturn(mContext);
        when(mPreference.getContext()).thenReturn(mContext);
    }

    @After
    public void tearDown() {
        ShadowEntityHeaderController.reset();
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
        mController = new AudioStreamHeaderController(mContext, KEY);
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
        mController = new AudioStreamHeaderController(mContext, KEY);
        mController.onStop(mLifecycleOwner);
        verify(mAssistant, never())
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
    }

    @Test
    public void testDisplayPreference_sourceConnected_setSummary() {
        when(mAudioStreamsHelper.getAllConnectedSources())
                .thenReturn(List.of(mBroadcastReceiveState));
        when(mBroadcastReceiveState.getBroadcastId()).thenReturn(BROADCAST_ID);

        mController.displayPreference(mScreen);

        verify(mHeaderController).setLabel(BROADCAST_NAME);
        verify(mHeaderController).setIcon(any(Drawable.class));
        verify(mHeaderController)
                .setSummary(mContext.getString(AUDIO_STREAM_HEADER_LISTENING_NOW_SUMMARY));
        verify(mHeaderController).done(true);
        verify(mScreen).addPreference(any());
    }

    @Test
    public void testDisplayPreference_sourceNotConnected_setSummary() {
        when(mAudioStreamsHelper.getAllConnectedSources()).thenReturn(Collections.emptyList());

        mController.displayPreference(mScreen);

        verify(mHeaderController).setLabel(BROADCAST_NAME);
        verify(mHeaderController).setIcon(any(Drawable.class));
        verify(mHeaderController).setSummary(AUDIO_STREAM_HEADER_NOT_LISTENING_SUMMARY);
        verify(mHeaderController).done(true);
        verify(mScreen).addPreference(any());
    }

    @Test
    public void testDisplayPreference_sourcePresent_setSummary() {
        mSetFlagsRule.enableFlags(FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX);
        String address = "11:22:33:44:55:66";

        when(mBroadcastReceiveState.getBroadcastId()).thenReturn(BROADCAST_ID);
        when(mBroadcastReceiveState.getSourceDevice()).thenReturn(mBluetoothDevice);
        when(mBluetoothDevice.getAddress()).thenReturn(address);
        List<Long> bisSyncState = new ArrayList<>();
        when(mBroadcastReceiveState.getBisSyncState()).thenReturn(bisSyncState);
        when(mAudioStreamsHelper.getAllPresentSources())
                .thenReturn(List.of(mBroadcastReceiveState));

        mController.displayPreference(mScreen);

        verify(mHeaderController).setLabel(BROADCAST_NAME);
        verify(mHeaderController).setIcon(any(Drawable.class));
        verify(mHeaderController)
                .setSummary(mContext.getString(AUDIO_STREAM_HEADER_PRESENT_NOW_SUMMARY));
        verify(mHeaderController).done(true);
        verify(mScreen).addPreference(any());
    }

    @Test
    public void testDisplayPreference_sourceNotPresent_setSummary() {
        mSetFlagsRule.enableFlags(FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX);

        when(mAudioStreamsHelper.getAllPresentSources()).thenReturn(Collections.emptyList());

        mController.displayPreference(mScreen);

        verify(mHeaderController).setLabel(BROADCAST_NAME);
        verify(mHeaderController).setIcon(any(Drawable.class));
        verify(mHeaderController).setSummary(AUDIO_STREAM_HEADER_NOT_LISTENING_SUMMARY);
        verify(mHeaderController).done(true);
        verify(mScreen).addPreference(any());
    }

    @Test
    public void testCallback_onSourceRemoved_updateButton() {
        when(mAudioStreamsHelper.getAllConnectedSources()).thenReturn(Collections.emptyList());

        mController.displayPreference(mScreen);
        mController.mBroadcastAssistantCallback.onSourceRemoved(
                mock(BluetoothDevice.class), /* sourceId= */ 0, /* reason= */ 0);

        // Called twice, once in displayPreference, the other one in callback
        verify(mHeaderController, times(2)).setSummary(AUDIO_STREAM_HEADER_NOT_LISTENING_SUMMARY);
        verify(mHeaderController, times(2)).done(true);
    }

    @Test
    public void testCallback_onSourceLost_updateButton() {
        when(mAudioStreamsHelper.getAllConnectedSources()).thenReturn(Collections.emptyList());

        mController.displayPreference(mScreen);
        mController.mBroadcastAssistantCallback.onSourceLost(/* broadcastId= */ 1);

        // Called twice, once in displayPreference, the other one in callback
        verify(mHeaderController, times(2)).setSummary(AUDIO_STREAM_HEADER_NOT_LISTENING_SUMMARY);
        verify(mHeaderController, times(2)).done(true);
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

        // Called twice, once in displayPreference, the other one in callback
        verify(mHeaderController, times(2))
                .setSummary(mContext.getString(AUDIO_STREAM_HEADER_LISTENING_NOW_SUMMARY));
        verify(mHeaderController, times(2)).done(true);
    }

    @Test
    public void testCallback_onReceiveStateChangedWithSourcePresent_updateButton() {
        mSetFlagsRule.enableFlags(FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX);
        String address = "11:22:33:44:55:66";

        when(mAudioStreamsHelper.getAllPresentSources())
                .thenReturn(List.of(mBroadcastReceiveState));
        when(mBroadcastReceiveState.getBroadcastId()).thenReturn(BROADCAST_ID);
        when(mBroadcastReceiveState.getSourceDevice()).thenReturn(mBluetoothDevice);
        when(mBluetoothDevice.getAddress()).thenReturn(address);

        mController.displayPreference(mScreen);
        mController.mBroadcastAssistantCallback.onReceiveStateChanged(
                mock(BluetoothDevice.class), /* sourceId= */ 0, mBroadcastReceiveState);

        // Called twice, once in displayPreference, the other one in callback
        verify(mHeaderController, times(2))
                .setSummary(mContext.getString(AUDIO_STREAM_HEADER_PRESENT_NOW_SUMMARY));
        verify(mHeaderController, times(2)).done(true);
    }
}
