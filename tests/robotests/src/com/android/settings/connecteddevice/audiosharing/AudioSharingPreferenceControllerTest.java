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

import static android.bluetooth.BluetoothAdapter.STATE_OFF;
import static android.bluetooth.BluetoothAdapter.STATE_ON;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.os.Looper;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.flags.Flags;

import org.junit.After;
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

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
            ShadowThreadUtils.class
        })
public class AudioSharingPreferenceControllerTest {
    private static final String PREF_KEY = "audio_sharing_settings";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Spy Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private PreferenceScreen mScreen;
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private BluetoothEventManager mBtEventManager;
    @Mock private LocalBluetoothProfileManager mLocalBtProfileManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private BluetoothLeBroadcastMetadata mMetadata;
    private AudioSharingPreferenceController mController;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    @Spy private Preference mPreference;

    @Before
    public void setUp() {
        ShadowBluetoothAdapter shadowBluetoothAdapter =
                Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        shadowBluetoothAdapter.setEnabled(true);
        shadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        shadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        LocalBluetoothManager localBluetoothManager = Utils.getLocalBtManager(mContext);
        when(localBluetoothManager.getEventManager()).thenReturn(mBtEventManager);
        when(localBluetoothManager.getProfileManager()).thenReturn(mLocalBtProfileManager);
        when(mLocalBtProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        mController = new AudioSharingPreferenceController(mContext, PREF_KEY);
        mPreference = spy(new Preference(mContext));
        when(mScreen.findPreference(PREF_KEY)).thenReturn(mPreference);
    }

    @After
    public void tearDown() {
        ShadowBluetoothUtils.reset();
        ShadowThreadUtils.reset();
    }

    @Test
    public void onStart_flagOn_registerCallback() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStart(mLifecycleOwner);
        verify(mBtEventManager).registerCallback(mController);
        verify(mBroadcast).registerServiceCallBack(any(), any(BluetoothLeBroadcast.Callback.class));
    }

    @Test
    public void onStart_flagOff_skipRegisterCallback() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStart(mLifecycleOwner);
        verify(mBtEventManager, never()).registerCallback(mController);
        verify(mBroadcast, never())
                .registerServiceCallBack(any(), any(BluetoothLeBroadcast.Callback.class));
    }

    @Test
    public void onStop_flagOn_unregisterCallback() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStop(mLifecycleOwner);
        verify(mBtEventManager).unregisterCallback(mController);
        verify(mBroadcast).unregisterServiceCallBack(any(BluetoothLeBroadcast.Callback.class));
    }

    @Test
    public void onStop_flagOff_skipUnregisterCallback() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mController.onStop(mLifecycleOwner);
        verify(mBtEventManager, never()).unregisterCallback(mController);
        verify(mBroadcast, never())
                .unregisterServiceCallBack(any(BluetoothLeBroadcast.Callback.class));
    }

    @Test
    public void getAvailabilityStatus_flagOn() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_flagOff() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getSummary_broadcastOn() {
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        assertThat(mController.getSummary().toString())
                .isEqualTo(mContext.getString(R.string.audio_sharing_summary_on));
    }

    @Test
    public void getSummary_broadcastOff() {
        when(mBroadcast.isEnabled(any())).thenReturn(false);
        assertThat(mController.getSummary().toString())
                .isEqualTo(mContext.getString(R.string.audio_sharing_summary_off));
    }

    @Test
    public void onBluetoothStateChanged_refreshSummary() {
        mController.displayPreference(mScreen);
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        mController.onBluetoothStateChanged(STATE_ON);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.getSummary().toString())
                .isEqualTo(mContext.getString(R.string.audio_sharing_summary_on));

        when(mBroadcast.isEnabled(any())).thenReturn(false);
        mController.onBluetoothStateChanged(STATE_OFF);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.getSummary().toString())
                .isEqualTo(mContext.getString(R.string.audio_sharing_summary_off));
    }

    @Test
    public void testBluetoothLeBroadcastCallbacks_refreshSummary() {
        mController.displayPreference(mScreen);

        when(mBroadcast.isEnabled(any())).thenReturn(true);
        mController.mBroadcastCallback.onBroadcastStarted(/* reason= */ 1, /* broadcastId= */ 1);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.getSummary().toString())
                .isEqualTo(mContext.getString(R.string.audio_sharing_summary_on));

        when(mBroadcast.isEnabled(any())).thenReturn(false);
        mController.mBroadcastCallback.onBroadcastStopped(/* reason= */ 1, /* broadcastId= */ 1);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.getSummary().toString())
                .isEqualTo(mContext.getString(R.string.audio_sharing_summary_off));
    }

    @Test
    public void testBluetoothLeBroadcastCallbacks_doNothing() {
        mController.displayPreference(mScreen);

        mController.mBroadcastCallback.onBroadcastMetadataChanged(/* reason= */ 1, mMetadata);
        verify(mPreference, never()).setSummary(any());
        mController.mBroadcastCallback.onBroadcastUpdated(/* reason= */ 1, /* broadcastId= */ 1);
        verify(mPreference, never()).setSummary(any());
        mController.mBroadcastCallback.onPlaybackStarted(/* reason= */ 1, /* broadcastId= */ 1);
        verify(mPreference, never()).setSummary(any());
        mController.mBroadcastCallback.onPlaybackStopped(/* reason= */ 1, /* broadcastId= */ 1);
        verify(mPreference, never()).setSummary(any());
        mController.mBroadcastCallback.onBroadcastStartFailed(/* reason= */ 1);
        verify(mPreference, never()).setSummary(any());
        mController.mBroadcastCallback.onBroadcastStopFailed(/* reason= */ 1);
        verify(mPreference, never()).setSummary(any());
        mController.mBroadcastCallback.onBroadcastUpdateFailed(
                /* reason= */ 1, /* broadcastId= */ 1);
        verify(mPreference, never()).setSummary(any());
    }
}
