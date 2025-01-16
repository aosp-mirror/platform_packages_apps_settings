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
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant.LocalBluetoothLeBroadcastSourceState.PAUSED;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant.LocalBluetoothLeBroadcastSourceState.STREAMING;
import static com.android.settingslib.flags.Flags.FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX;
import static com.android.settingslib.flags.Flags.FLAG_ENABLE_LE_AUDIO_SHARING;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows.ShadowAudioStreamsHelper;
import com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows.ShadowEntityHeaderController;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
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
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowEntityHeaderController.class,
            ShadowThreadUtils.class,
            ShadowAudioStreamsHelper.class,
            ShadowBluetoothAdapter.class,
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
        ShadowBluetoothAdapter shadowBluetoothAdapter = Shadow.extract(
                BluetoothAdapter.getDefaultAdapter());
        shadowBluetoothAdapter.setEnabled(true);
        shadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        shadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);

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
    public void testDisplayPreference_sourceStreaming_setSummary() {
        when(mAudioStreamsHelper.getConnectedBroadcastIdAndState(anyBoolean()))
                .thenReturn(Map.of(BROADCAST_ID, STREAMING));

        mController.displayPreference(mScreen);

        verify(mHeaderController).setLabel(BROADCAST_NAME);
        verify(mHeaderController).setIcon(any(Drawable.class));
        verify(mHeaderController)
                .setSummary(mContext.getString(AUDIO_STREAM_HEADER_LISTENING_NOW_SUMMARY));
        verify(mHeaderController).done(true);
        verify(mScreen).addPreference(any());
    }

    @Test
    public void testDisplayPreference_sourceNotStreaming_setSummary() {
        when(mAudioStreamsHelper.getConnectedBroadcastIdAndState(anyBoolean()))
                .thenReturn(Collections.emptyMap());

        mController.displayPreference(mScreen);

        verify(mHeaderController).setLabel(BROADCAST_NAME);
        verify(mHeaderController).setIcon(any(Drawable.class));
        verify(mHeaderController).setSummary(AUDIO_STREAM_HEADER_NOT_LISTENING_SUMMARY);
        verify(mHeaderController).done(true);
        verify(mScreen).addPreference(any());
    }

    @Test
    public void testDisplayPreference_sourcePaused_setSummary() {
        mSetFlagsRule.enableFlags(FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX);
        when(mAudioStreamsHelper.getConnectedBroadcastIdAndState(anyBoolean()))
                .thenReturn(Map.of(BROADCAST_ID, PAUSED));

        // Create new controller to enable hysteresis mode
        mController = new AudioStreamHeaderController(mContext, KEY);
        mController.init(mFragment, BROADCAST_NAME, BROADCAST_ID);
        mController.displayPreference(mScreen);

        verify(mHeaderController).setLabel(BROADCAST_NAME);
        verify(mHeaderController).setIcon(any(Drawable.class));
        verify(mHeaderController)
                .setSummary(mContext.getString(AUDIO_STREAM_HEADER_PRESENT_NOW_SUMMARY));
        verify(mHeaderController).done(true);
        verify(mScreen).addPreference(any());
    }

    @Test
    public void testDisplayPreference_sourceNotPaused_setSummary() {
        mSetFlagsRule.enableFlags(FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX);
        when(mAudioStreamsHelper.getConnectedBroadcastIdAndState(anyBoolean()))
                .thenReturn(Collections.emptyMap());

        mController.displayPreference(mScreen);

        verify(mHeaderController).setLabel(BROADCAST_NAME);
        verify(mHeaderController).setIcon(any(Drawable.class));
        verify(mHeaderController).setSummary(AUDIO_STREAM_HEADER_NOT_LISTENING_SUMMARY);
        verify(mHeaderController).done(true);
        verify(mScreen).addPreference(any());
    }

    @Test
    public void testCallback_onSourceRemoved_updateButton() {
        when(mAudioStreamsHelper.getConnectedBroadcastIdAndState(anyBoolean()))
                .thenReturn(Collections.emptyMap());

        mController.displayPreference(mScreen);
        mController.mBroadcastAssistantCallback.onSourceRemoved(
                mock(BluetoothDevice.class), /* sourceId= */ 0, /* reason= */ 0);

        // Called twice, once in displayPreference, the other one in callback
        verify(mHeaderController, times(2)).setSummary(AUDIO_STREAM_HEADER_NOT_LISTENING_SUMMARY);
        verify(mHeaderController, times(2)).done(true);
    }

    @Test
    public void testCallback_onSourceLost_updateButton() {
        when(mAudioStreamsHelper.getConnectedBroadcastIdAndState(anyBoolean()))
                .thenReturn(Collections.emptyMap());

        mController.displayPreference(mScreen);
        mController.mBroadcastAssistantCallback.onSourceLost(/* broadcastId= */ 1);

        // Called twice, once in displayPreference, the other one in callback
        verify(mHeaderController, times(2)).setSummary(AUDIO_STREAM_HEADER_NOT_LISTENING_SUMMARY);
        verify(mHeaderController, times(2)).done(true);
    }

    @Test
    public void testCallback_onReceiveStateChanged_updateButton() {
        when(mAudioStreamsHelper.getConnectedBroadcastIdAndState(anyBoolean()))
                .thenReturn(Map.of(BROADCAST_ID, STREAMING));
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
    public void testCallback_onReceiveStateChangedWithSourcePaused_updateButton() {
        mSetFlagsRule.enableFlags(FLAG_ENABLE_LE_AUDIO_SHARING);
        mSetFlagsRule.enableFlags(FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX);
        String address = "11:22:33:44:55:66";

        when(mAudioStreamsHelper.getConnectedBroadcastIdAndState(anyBoolean()))
                .thenReturn(Map.of(BROADCAST_ID, PAUSED));
        when(mBroadcastReceiveState.getBroadcastId()).thenReturn(BROADCAST_ID);
        when(mBroadcastReceiveState.getSourceDevice()).thenReturn(mBluetoothDevice);
        when(mBluetoothDevice.getAddress()).thenReturn(address);

        // Create new controller to enable hysteresis mode
        mController = new AudioStreamHeaderController(mContext, KEY);
        mController.init(mFragment, BROADCAST_NAME, BROADCAST_ID);
        mController.displayPreference(mScreen);
        mController.mBroadcastAssistantCallback.onReceiveStateChanged(
                mock(BluetoothDevice.class), /* sourceId= */ 0, mBroadcastReceiveState);

        // Called twice, once in displayPreference, the other one in callback
        verify(mHeaderController, times(2))
                .setSummary(mContext.getString(AUDIO_STREAM_HEADER_PRESENT_NOW_SUMMARY));
        verify(mHeaderController, times(2)).done(true);
    }
}
