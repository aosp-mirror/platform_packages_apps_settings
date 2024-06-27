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
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.os.Looper;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows.ShadowAudioStreamsHelper;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.flags.Flags;

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
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
            ShadowAudioStreamsHelper.class,
        })
public class AudioStreamsCategoryControllerTest {
    private static final String KEY = "audio_streams_settings_category";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private LocalBluetoothProfileManager mBtProfileManager;
    @Mock private BluetoothEventManager mBluetoothEventManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private VolumeControlProfile mVolumeControl;
    @Mock private PreferenceScreen mScreen;
    @Mock private AudioStreamsHelper mAudioStreamsHelper;
    @Mock private CachedBluetoothDevice mCachedBluetoothDevice;

    private AudioStreamsCategoryController mController;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private LocalBluetoothManager mLocalBluetoothManager;
    private Preference mPreference;

    @Before
    public void setUp() {
        ShadowAudioStreamsHelper.setUseMock(mAudioStreamsHelper);
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        when(mLocalBluetoothManager.getEventManager()).thenReturn(mBluetoothEventManager);
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mBtProfileManager);
        when(mBtProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        when(mBtProfileManager.getLeAudioBroadcastAssistantProfile()).thenReturn(mAssistant);
        when(mBtProfileManager.getVolumeControlProfile()).thenReturn(mVolumeControl);
        when(mBroadcast.isProfileReady()).thenReturn(true);
        when(mAssistant.isProfileReady()).thenReturn(true);
        when(mVolumeControl.isProfileReady()).thenReturn(true);
        mController = spy(new AudioStreamsCategoryController(mContext, KEY));
        mPreference = new Preference(mContext);
        when(mScreen.findPreference(KEY)).thenReturn(mPreference);
        mController.displayPreference(mScreen);
        mPreference.setVisible(false);
    }

    @After
    public void tearDown() {
        ShadowAudioStreamsHelper.reset();
        ShadowBluetoothUtils.reset();
    }

    @Test
    public void getAvailabilityStatus_flagOn() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_QR_CODE_PRIVATE_BROADCAST_SHARING);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_flagOff() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_QR_CODE_PRIVATE_BROADCAST_SHARING);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void onStart_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_QR_CODE_PRIVATE_BROADCAST_SHARING);
        mController.onStart(mLifecycleOwner);
        verify(mBluetoothEventManager, never()).registerCallback(any());
    }

    @Test
    public void onStart_flagOn_registerCallback() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_QR_CODE_PRIVATE_BROADCAST_SHARING);
        mController.onStart(mLifecycleOwner);
        verify(mBluetoothEventManager).registerCallback(any());
    }

    @Test
    public void onStop_flagOff_doNothing() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_QR_CODE_PRIVATE_BROADCAST_SHARING);
        mController.onStop(mLifecycleOwner);
        verify(mBluetoothEventManager, never()).unregisterCallback(any());
    }

    @Test
    public void onStop_flagOn_unregisterCallback() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_QR_CODE_PRIVATE_BROADCAST_SHARING);
        mController.onStop(mLifecycleOwner);
        verify(mBluetoothEventManager).unregisterCallback(any());
    }

    @Test
    public void updateVisibility_flagOff_invisible() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_QR_CODE_PRIVATE_BROADCAST_SHARING);
        mController.updateVisibility();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateVisibility_noConnectedLe_invisible() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_QR_CODE_PRIVATE_BROADCAST_SHARING);
        mController.updateVisibility();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateVisibility_isNotProfileReady_invisible() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_QR_CODE_PRIVATE_BROADCAST_SHARING);
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(
                mCachedBluetoothDevice);
        when(mVolumeControl.isProfileReady()).thenReturn(false);
        mController.updateVisibility();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateVisibility_isBroadcasting_invisible() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_QR_CODE_PRIVATE_BROADCAST_SHARING);
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(
                mCachedBluetoothDevice);
        when(mBroadcast.isEnabled(any())).thenReturn(true);
        mController.updateVisibility();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateVisibility_isBluetoothOff_invisible() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_QR_CODE_PRIVATE_BROADCAST_SHARING);
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(
                mCachedBluetoothDevice);
        mShadowBluetoothAdapter.setEnabled(false);
        mController.updateVisibility();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateVisibility_visible() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_QR_CODE_PRIVATE_BROADCAST_SHARING);
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(
                mCachedBluetoothDevice);
        mController.displayPreference(mScreen);
        mController.updateVisibility();
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void onProfileConnectionStateChanged_updateVisibility() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_QR_CODE_PRIVATE_BROADCAST_SHARING);
        ArgumentCaptor<BluetoothCallback> argumentCaptor =
                ArgumentCaptor.forClass(BluetoothCallback.class);
        mController.onStart(mLifecycleOwner);
        verify(mBluetoothEventManager).registerCallback(argumentCaptor.capture());

        BluetoothCallback callback = argumentCaptor.getValue();
        callback.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT,
                BluetoothAdapter.STATE_DISCONNECTED);

        verify(mController).updateVisibility();
    }
}
