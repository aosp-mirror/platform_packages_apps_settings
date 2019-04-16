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

import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.AudioManager;

import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import com.android.settings.testutils.shadow.ShadowAudioManager;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HeadsetProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collection;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowAudioManager.class})
public class AvailableMediaBluetoothDeviceUpdaterTest {
    @Mock
    private DashboardFragment mDashboardFragment;
    @Mock
    private DevicePreferenceCallback mDevicePreferenceCallback;
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private BluetoothDevice mBluetoothDevice;
    @Mock
    private LocalBluetoothManager mLocalManager;
    @Mock
    private LocalBluetoothProfileManager mLocalBluetoothProfileManager;
    @Mock
    private HeadsetProfile mHeadsetProfile;
    @Mock
    private CachedBluetoothDeviceManager mCachedDeviceManager;

    private Context mContext;
    private AvailableMediaBluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private Collection<CachedBluetoothDevice> cachedDevices;
    private ShadowAudioManager mShadowAudioManager;
    private BluetoothDevicePreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mShadowAudioManager = ShadowAudioManager.getShadow();
        mContext = RuntimeEnvironment.application;
        doReturn(mContext).when(mDashboardFragment).getContext();
        cachedDevices =
                new ArrayList<CachedBluetoothDevice>(new ArrayList<CachedBluetoothDevice>());

        when(mCachedBluetoothDevice.getDevice()).thenReturn(mBluetoothDevice);
        when(mLocalManager.getProfileManager()).thenReturn(mLocalBluetoothProfileManager);
        when(mLocalBluetoothProfileManager.getHeadsetProfile()).thenReturn(mHeadsetProfile);
        when(mLocalManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(cachedDevices);

        mBluetoothDeviceUpdater = spy(new AvailableMediaBluetoothDeviceUpdater(mDashboardFragment,
                mDevicePreferenceCallback, mLocalManager));
        mBluetoothDeviceUpdater.setPrefContext(mContext);
        mPreference = new BluetoothDevicePreference(mContext, mCachedBluetoothDevice, false);
        doNothing().when(mBluetoothDeviceUpdater).addPreference(any());
        doNothing().when(mBluetoothDeviceUpdater).removePreference(any());
    }

    @Test
    public void onAudioModeChanged_hfpDeviceConnected_inCall_addPreference() {
        mShadowAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isHfpDevice()).thenReturn(true);
        cachedDevices.add(mCachedBluetoothDevice);

        mBluetoothDeviceUpdater.onAudioModeChanged();

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onAudioModeChanged_hfpDeviceConnected_notInCall_removePreference() {
        mShadowAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isHfpDevice()).thenReturn(true);
        cachedDevices.add(mCachedBluetoothDevice);

        mBluetoothDeviceUpdater.onAudioModeChanged();

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onAudioModeChanged_a2dpDeviceConnected_inCall_removePreference() {
        mShadowAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isA2dpDevice()).thenReturn(true);
        cachedDevices.add(mCachedBluetoothDevice);

        mBluetoothDeviceUpdater.onAudioModeChanged();

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onAudioModeChanged_a2dpDeviceConnected_notInCall_addPreference() {
        mShadowAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isA2dpDevice()).thenReturn(true);
        cachedDevices.add(mCachedBluetoothDevice);

        mBluetoothDeviceUpdater.onAudioModeChanged();

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_a2dpDeviceConnected_notInCall_addPreference() {
        mShadowAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isA2dpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_a2dpDeviceConnected_inCall_removePreference() {
        mShadowAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isA2dpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_hfpDeviceConnected_notInCall_removePreference() {
        mShadowAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isHfpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_hfpDeviceConnected_inCall_addPreference() {
        mShadowAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isHfpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_hearingAidDeviceConnected_notInCall_addPreference()
    {
        mShadowAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHearingAidDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.HEARING_AID);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_hearingAidDeviceConnected_inCall_addPreference() {
        mShadowAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHearingAidDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.HEARING_AID);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_deviceDisconnected_removePreference() {
        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_DISCONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onClick_Preference_setActive() {
        mBluetoothDeviceUpdater.onPreferenceClick(mPreference);

        verify(mCachedBluetoothDevice).setActive();
    }
}

