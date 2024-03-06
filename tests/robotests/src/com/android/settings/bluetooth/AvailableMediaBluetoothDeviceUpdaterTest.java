/*
 * Copyright 2018 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.settings.bluetooth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.util.Pair;

import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.flags.Flags;
import com.android.settings.testutils.shadow.ShadowAudioManager;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.Collection;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowAudioManager.class,
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class
        })
public class AvailableMediaBluetoothDeviceUpdaterTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private static final String MAC_ADDRESS = "04:52:C7:0B:D8:3C";

    @Mock private DashboardFragment mDashboardFragment;
    @Mock private DevicePreferenceCallback mDevicePreferenceCallback;
    @Mock private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock private BluetoothDevice mBluetoothDevice;
    @Mock private Drawable mDrawable;
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private LocalBluetoothProfileManager mLocalBtProfileManager;
    @Mock private CachedBluetoothDeviceManager mCachedDeviceManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private BluetoothLeBroadcastReceiveState mBroadcastReceiveState;

    private Context mContext;
    private AvailableMediaBluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private Collection<CachedBluetoothDevice> mCachedDevices;
    private AudioManager mAudioManager;
    private BluetoothDevicePreference mPreference;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mAudioManager = mContext.getSystemService(AudioManager.class);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mLocalBtManager = Utils.getLocalBtManager(mContext);
        when(mLocalBtManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mCachedDevices = new ArrayList<>();
        mCachedDevices.add(mCachedBluetoothDevice);
        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(mCachedDevices);
        Pair<Drawable, String> pairs = new Pair<>(mDrawable, "fake_device");

        doReturn(mContext).when(mDashboardFragment).getContext();
        when(mCachedBluetoothDevice.getDevice()).thenReturn(mBluetoothDevice);
        when(mCachedBluetoothDevice.getAddress()).thenReturn(MAC_ADDRESS);
        when(mCachedBluetoothDevice.getDrawableWithDescription()).thenReturn(pairs);
        when(mCachedBluetoothDevice.getMemberDevice()).thenReturn(ImmutableSet.of());

        mBluetoothDeviceUpdater =
                spy(
                        new AvailableMediaBluetoothDeviceUpdater(
                                mContext, mDevicePreferenceCallback, /* metricsCategory= */ 0));
        mBluetoothDeviceUpdater.setPrefContext(mContext);
        mPreference =
                new BluetoothDevicePreference(
                        mContext,
                        mCachedBluetoothDevice,
                        false,
                        BluetoothDevicePreference.SortType.TYPE_DEFAULT);
        doNothing().when(mBluetoothDeviceUpdater).addPreference(any());
        doNothing().when(mBluetoothDeviceUpdater).removePreference(any());
    }

    @Test
    public void onAudioModeChanged_hfpDeviceConnected_inCall_addPreference() {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class)))
                .thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHfpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onAudioModeChanged();

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onAudioModeChanged_hfpDeviceConnected_notInCall_removePreference() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class)))
                .thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHfpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onAudioModeChanged();

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onAudioModeChanged_a2dpDeviceConnected_inCall_removePreference() {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class)))
                .thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedA2dpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onAudioModeChanged();

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onAudioModeChanged_a2dpDeviceConnected_notInCall_addPreference() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class)))
                .thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedA2dpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onAudioModeChanged();

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_a2dpDeviceConnected_notInCall_addPreference() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class)))
                .thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedA2dpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_a2dpDeviceConnected_inCall_removePreference() {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class)))
                .thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedA2dpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_hfpDeviceConnected_notInCall_removePreference() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class)))
                .thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHfpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_hfpDeviceConnected_inCall_addPreference() {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class)))
                .thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHfpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_ashaHearingAidConnected_notInCall_addPreference() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class)))
                .thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.HEARING_AID);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_ashaHearingAidConnected_inCall_addPreference() {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class)))
                .thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.HEARING_AID);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onProfileConnectionStateChanged_leAudioDeviceConnected_notInCall_addsPreference() {
        setUpBroadcast(/* isSupported= */ false, /* isBroadcasting= */ false);
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class)))
                .thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.LE_AUDIO);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onProfileConnectionStateChanged_leAudioDeviceConnected_inCall_addsPreference() {
        setUpBroadcast(/* isSupported= */ false, /* isBroadcasting= */ false);
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class)))
                .thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.LE_AUDIO);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void
            onProfileConnectionStateChanged_leaDeviceConnected_notInCall_notInBroadcast_addsPref() {
        setUpBroadcast(/* isSupported= */ true, /* isBroadcasting= */ false);
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class)))
                .thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.LE_AUDIO);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void
            onProfileConnectionStateChanged_leaDeviceConnected_inCall_notInBroadcast_addsPref() {
        setUpBroadcast(/* isSupported= */ true, /* isBroadcasting= */ false);
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class)))
                .thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.LE_AUDIO);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void
            onProfileConnectionStateChanged_leaDeviceConnected_notInCall_inBroadcast_removesPref() {
        setUpBroadcast(/* isSupported= */ true, /* isBroadcasting= */ true);
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class)))
                .thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.LE_AUDIO);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void
            onProfileConnectionStateChanged_leaDeviceConnected_inCall_inBroadcast_removesPref() {
        setUpBroadcast(/* isSupported= */ true, /* isBroadcasting= */ true);
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class)))
                .thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.LE_AUDIO);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void
            onProfileConnectionStateChanged_deviceIsNotInList_notInCall_invokesRemovePreference() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class)))
                .thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(true);
        mCachedDevices.clear();

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.LE_AUDIO);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_deviceIsNotInList_inCall_invokesRemovePreference() {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class)))
                .thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(true);
        mCachedDevices.clear();

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.LE_AUDIO);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_deviceDisconnected_removePreference() {
        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice, BluetoothProfile.STATE_DISCONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onClick_Preference_setActive() {
        setUpBroadcast(/* isSupported= */ false, /* isBroadcasting= */ false);
        mBluetoothDeviceUpdater.onPreferenceClick(mPreference);

        verify(mCachedBluetoothDevice).setActive();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onClick_Preference_isNotBroadcasting_setActive() {
        setUpBroadcast(/* isSupported= */ true, /* isBroadcasting= */ false);
        mBluetoothDeviceUpdater.onPreferenceClick(mPreference);

        verify(mCachedBluetoothDevice).setActive();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onClick_Preference_isBroadcasting_stopBroadcastingAndSetActive() {
        setUpBroadcast(/* isSupported= */ true, /* isBroadcasting= */ true);
        doNothing().when(mBroadcast).stopBroadcast(anyInt());
        mBluetoothDeviceUpdater.onPreferenceClick(mPreference);

        verify(mBroadcast).stopBroadcast(anyInt());
        verify(mCachedBluetoothDevice).setActive();
    }

    private void setUpBroadcast(boolean isSupported, boolean isBroadcasting) {
        if (isSupported) {
            mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                    BluetoothStatusCodes.FEATURE_SUPPORTED);
            mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                    BluetoothStatusCodes.FEATURE_SUPPORTED);
            when(mLocalBtManager.getProfileManager()).thenReturn(mLocalBtProfileManager);
            when(mLocalBtProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
            when(mBroadcast.isEnabled(null)).thenReturn(isBroadcasting);
            when(mLocalBtProfileManager.getLeAudioBroadcastAssistantProfile())
                    .thenReturn(mAssistant);
            if (isBroadcasting) {
                when(mAssistant.getAllSources(any()))
                        .thenReturn(ImmutableList.of(mBroadcastReceiveState));
            } else {
                when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of());
            }
        } else {
            mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                    BluetoothStatusCodes.FEATURE_NOT_SUPPORTED);
            mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                    BluetoothStatusCodes.FEATURE_NOT_SUPPORTED);
        }
    }
}
