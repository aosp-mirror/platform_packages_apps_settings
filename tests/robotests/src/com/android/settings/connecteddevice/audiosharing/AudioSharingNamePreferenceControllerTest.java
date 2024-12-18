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

package com.android.settings.connecteddevice.audiosharing;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
        })
public class AudioSharingNamePreferenceControllerTest {
    private static final String PREF_KEY = "audio_sharing_stream_name";
    private static final String BROADCAST_NAME = "broadcast_name";
    private static final CharSequence UPDATED_NAME = "updated_name";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Spy Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private VolumeControlProfile mVolumeControl;
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private BluetoothEventManager mEventManager;
    @Mock private LocalBluetoothProfileManager mProfileManager;
    @Mock private PreferenceScreen mScreen;
    private AudioSharingNamePreferenceController mController;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private AudioSharingNamePreference mPreference;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mLocalBtManager = Utils.getLocalBtManager(mContext);
        when(mLocalBtManager.getEventManager()).thenReturn(mEventManager);
        when(mLocalBtManager.getProfileManager()).thenReturn(mProfileManager);
        when(mProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        when(mProfileManager.getLeAudioBroadcastAssistantProfile()).thenReturn(mAssistant);
        when(mProfileManager.getVolumeControlProfile()).thenReturn(mVolumeControl);
        when(mBroadcast.isProfileReady()).thenReturn(true);
        when(mAssistant.isProfileReady()).thenReturn(true);
        when(mVolumeControl.isProfileReady()).thenReturn(true);
        when(mBroadcast.isProfileReady()).thenReturn(true);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mController = new AudioSharingNamePreferenceController(mContext, PREF_KEY);
        mPreference = spy(new AudioSharingNamePreference(mContext));
        when(mScreen.findPreference(PREF_KEY)).thenReturn(mPreference);
    }

    @Test
    public void getAvailabilityStatus_flagOn_available() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_flagOff_unsupported() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void onStart_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStart(mLifecycleOwner);
        verify(mBroadcast, never())
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcast.Callback.class));
    }

    @Test
    public void onStart_flagOn_registerCallbacks() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStart(mLifecycleOwner);
        verify(mBroadcast)
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcast.Callback.class));
    }

    @Test
    public void onStart_flagOn_serviceNotReady_registerCallbacks() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mBroadcast.isProfileReady()).thenReturn(false);
        mController.onStart(mLifecycleOwner);
        verify(mProfileManager)
                .addServiceListener(any(LocalBluetoothProfileManager.ServiceListener.class));
    }

    @Test
    public void onServiceConnected_removeCallbacks() {
        mController.onServiceConnected();
        verify(mProfileManager)
                .removeServiceListener(any(LocalBluetoothProfileManager.ServiceListener.class));
    }

    @Test
    public void onStop_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStart(mLifecycleOwner);
        mController.onStop(mLifecycleOwner);
        verify(mBroadcast, never())
                .unregisterServiceCallBack(any(BluetoothLeBroadcast.Callback.class));
    }

    @Test
    public void onStop_flagOn_unregisterCallbacks() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStart(mLifecycleOwner);
        mController.onStop(mLifecycleOwner);
        verify(mBroadcast).unregisterServiceCallBack(any(BluetoothLeBroadcast.Callback.class));
    }

    @Test
    public void displayPreference_updateName_showIcon() {
        when(mBroadcast.getBroadcastName()).thenReturn(BROADCAST_NAME);
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        mController.displayPreference(mScreen);
        ShadowLooper.idleMainLooper();

        assertThat(mPreference.getText()).isEqualTo(BROADCAST_NAME);
        assertThat(mPreference.getSummary()).isEqualTo(BROADCAST_NAME);
        verify(mPreference).setValidator(any());
        verify(mPreference).setShowQrCodeIcon(true);
    }

    @Test
    public void displayPreference_updateName_hideIcon() {
        when(mBroadcast.getBroadcastName()).thenReturn(BROADCAST_NAME);
        when(mBroadcast.isEnabled(any())).thenReturn(false);
        mController.displayPreference(mScreen);
        ShadowLooper.idleMainLooper();

        assertThat(mPreference.getText()).isEqualTo(BROADCAST_NAME);
        assertThat(mPreference.getSummary()).isEqualTo(BROADCAST_NAME);
        verify(mPreference).setValidator(any());
        verify(mPreference).setShowQrCodeIcon(false);
    }

    @Test
    public void onPreferenceChange_noChange_doNothing() {
        when(mPreference.getSummary()).thenReturn(BROADCAST_NAME);
        mController.displayPreference(mScreen);
        boolean changed = mController.onPreferenceChange(mPreference, BROADCAST_NAME);
        ShadowLooper.idleMainLooper();

        verify(mBroadcast, never()).setBroadcastName(anyString());
        verify(mBroadcast, never()).setProgramInfo(anyString());
        verify(mBroadcast, never()).updateBroadcast();
        verify(mFeatureFactory.metricsFeatureProvider, never()).action(any(), anyInt(), anyInt());

        assertThat(changed).isFalse();
    }

    @Test
    public void onPreferenceChange_changed_updateName_broadcasting() {
        when(mPreference.getSummary()).thenReturn(BROADCAST_NAME);
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        mController.displayPreference(mScreen);
        boolean changed = mController.onPreferenceChange(mPreference, UPDATED_NAME);
        ShadowLooper.idleMainLooper();

        verify(mBroadcast).setBroadcastName(UPDATED_NAME.toString());
        verify(mBroadcast).setProgramInfo(UPDATED_NAME.toString());
        verify(mBroadcast).updateBroadcast();
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_AUDIO_STREAM_NAME_UPDATED, 1);
        assertThat(changed).isTrue();
    }

    @Test
    public void onPreferenceChange_changed_updateName_notBroadcasting() {
        when(mPreference.getSummary()).thenReturn(BROADCAST_NAME);
        when(mBroadcast.isEnabled(any())).thenReturn(false);
        mController.displayPreference(mScreen);
        boolean changed = mController.onPreferenceChange(mPreference, UPDATED_NAME);
        ShadowLooper.idleMainLooper();

        verify(mBroadcast).setBroadcastName(UPDATED_NAME.toString());
        verify(mBroadcast).setProgramInfo(UPDATED_NAME.toString());
        verify(mBroadcast, never()).updateBroadcast();
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_AUDIO_STREAM_NAME_UPDATED, 0);
        assertThat(changed).isTrue();
    }

    @Test
    public void unrelatedCallbacks_doNotUpdateIcon() {
        mController.displayPreference(mScreen);
        mController.mBroadcastCallback.onBroadcastStartFailed(/* reason= */ 0);
        mController.mBroadcastCallback.onBroadcastStarted(/* reason= */ 0, /* broadcastId= */ 0);
        mController.mBroadcastCallback.onBroadcastStopFailed(/* reason= */ 0);
        mController.mBroadcastCallback.onBroadcastUpdateFailed(
                /* reason= */ 0, /* broadcastId= */ 0);
        mController.mBroadcastCallback.onBroadcastUpdated(/* reason= */ 0, /* broadcastId= */ 0);
        mController.mBroadcastCallback.onPlaybackStarted(/* reason= */ 0, /* broadcastId= */ 0);
        mController.mBroadcastCallback.onPlaybackStopped(/* reason= */ 0, /* broadcastId= */ 0);

        ShadowLooper.idleMainLooper();
        // Should be called once in displayPreference, but not called after callbacks
        verify(mPreference).setShowQrCodeIcon(anyBoolean());
    }

    @Test
    public void broadcastOnCallback_updateIcon() {
        mController.displayPreference(mScreen);
        mController.mBroadcastCallback.onBroadcastMetadataChanged(
                /* broadcastId= */ 0, mock(BluetoothLeBroadcastMetadata.class));

        ShadowLooper.idleMainLooper();

        // Should be called twice, in displayPreference and also after callback
        verify(mPreference, times(2)).setShowQrCodeIcon(anyBoolean());
    }

    @Test
    public void broadcastStopCallback_updateIcon() {
        mController.displayPreference(mScreen);
        mController.mBroadcastCallback.onBroadcastStopped(/* reason= */ 0, /* broadcastId= */ 0);

        ShadowLooper.idleMainLooper();

        // Should be called twice, in displayPreference and also after callback
        verify(mPreference, times(2)).setShowQrCodeIcon(anyBoolean());
    }

    @Test
    public void idTextValid_emptyString() {
        boolean valid = mController.isTextValid("");

        assertThat(valid).isFalse();
    }

    @Test
    public void idTextValid_validName() {
        boolean valid = mController.isTextValid("valid name");

        assertThat(valid).isTrue();
    }
}
