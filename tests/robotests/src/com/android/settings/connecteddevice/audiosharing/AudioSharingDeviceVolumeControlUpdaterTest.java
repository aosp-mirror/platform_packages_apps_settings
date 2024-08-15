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

import static com.android.settings.connecteddevice.audiosharing.AudioSharingDeviceVolumeControlUpdater.PREF_KEY_PREFIX;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Looper;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.BluetoothDevicePreference;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

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
import java.util.Collection;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothUtils.class})
public class AudioSharingDeviceVolumeControlUpdaterTest {
    private static final String TEST_DEVICE_NAME = "test";
    private static final String TAG = "AudioSharingVolUpdater";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private DevicePreferenceCallback mDevicePreferenceCallback;
    @Mock private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock private BluetoothDevice mBluetoothDevice;
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private CachedBluetoothDeviceManager mCachedDeviceManager;
    @Mock private LocalBluetoothProfileManager mLocalBtProfileManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private BluetoothLeBroadcastReceiveState mState;

    private Context mContext;
    private AudioSharingDeviceVolumeControlUpdater mDeviceUpdater;
    private Collection<CachedBluetoothDevice> mCachedDevices;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mLocalBtManager = Utils.getLocalBtManager(mContext);
        when(mLocalBtManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        when(mLocalBtManager.getProfileManager()).thenReturn(mLocalBtProfileManager);
        when(mLocalBtProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        when(mLocalBtProfileManager.getLeAudioBroadcastAssistantProfile()).thenReturn(mAssistant);
        List<Long> bisSyncState = new ArrayList<>();
        bisSyncState.add(1L);
        when(mState.getBisSyncState()).thenReturn(bisSyncState);
        doReturn(TEST_DEVICE_NAME).when(mCachedBluetoothDevice).getName();
        doReturn(mBluetoothDevice).when(mCachedBluetoothDevice).getDevice();
        doReturn(ImmutableSet.of()).when(mCachedBluetoothDevice).getMemberDevice();
        mCachedDevices = new ArrayList<>();
        mCachedDevices.add(mCachedBluetoothDevice);
        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(mCachedDevices);
        doNothing().when(mDevicePreferenceCallback).onDeviceAdded(any(Preference.class));
        doNothing().when(mDevicePreferenceCallback).onDeviceRemoved(any(Preference.class));
        mDeviceUpdater =
                spy(
                        new AudioSharingDeviceVolumeControlUpdater(
                                mContext, mDevicePreferenceCallback, /* metricsCategory= */ 0));
        mDeviceUpdater.setPrefContext(mContext);
    }

    @After
    public void tearDown() {
        ShadowBluetoothUtils.reset();
    }

    @Test
    public void onProfileConnectionStateChanged_leaDeviceConnected_noSharing_removesPref() {
        setupPreferenceMapWithDevice();

        when(mBroadcast.isEnabled(null)).thenReturn(false);
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);

        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceRemoved(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumePreference).isTrue();
        assertThat(((AudioSharingDeviceVolumePreference) captor.getValue()).getCachedDevice())
                .isEqualTo(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_leaDeviceConnected_noSource_removesPref() {
        setupPreferenceMapWithDevice();

        when(mAssistant.getAllSources(mBluetoothDevice)).thenReturn(ImmutableList.of());
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);

        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceRemoved(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumePreference).isTrue();
        assertThat(((AudioSharingDeviceVolumePreference) captor.getValue()).getCachedDevice())
                .isEqualTo(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_deviceIsNotInList_removesPref() {
        setupPreferenceMapWithDevice();

        mCachedDevices.clear();
        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(mCachedDevices);
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);

        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceRemoved(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumePreference).isTrue();
        assertThat(((AudioSharingDeviceVolumePreference) captor.getValue()).getCachedDevice())
                .isEqualTo(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_leaDeviceDisconnected_removesPref() {
        setupPreferenceMapWithDevice();

        when(mDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(false);
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);

        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothProfile.STATE_DISCONNECTED,
                BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceRemoved(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumePreference).isTrue();
        assertThat(((AudioSharingDeviceVolumePreference) captor.getValue()).getCachedDevice())
                .isEqualTo(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_leaDeviceDisconnecting_removesPref() {
        setupPreferenceMapWithDevice();

        when(mCachedBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(false);
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);

        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceRemoved(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumePreference).isTrue();
        assertThat(((AudioSharingDeviceVolumePreference) captor.getValue()).getCachedDevice())
                .isEqualTo(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_leaDeviceConnected_hasSource_addsPreference() {
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);
        setupPreferenceMapWithDevice();

        verify(mDevicePreferenceCallback).onDeviceAdded(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumePreference).isTrue();
        assertThat(((AudioSharingDeviceVolumePreference) captor.getValue()).getCachedDevice())
                .isEqualTo(mCachedBluetoothDevice);
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        assertThat(mDeviceUpdater.getLogTag()).isEqualTo(TAG);
    }

    @Test
    public void getPreferenceKey_returnsCorrectKey() {
        assertThat(mDeviceUpdater.getPreferenceKeyPrefix()).isEqualTo(PREF_KEY_PREFIX);
    }

    @Test
    public void addPreferenceWithSortType_doNothing() {
        mDeviceUpdater.addPreference(
                mCachedBluetoothDevice, BluetoothDevicePreference.SortType.TYPE_DEFAULT);
        // Verify AudioSharingDeviceVolumeControlUpdater overrides BluetoothDeviceUpdater and won't
        // trigger add preference.
        verifyNoInteractions(mDevicePreferenceCallback);
    }

    @Test
    public void launchDeviceDetails_doNothing() {
        Preference preference = mock(Preference.class);
        mDeviceUpdater.launchDeviceDetails(preference);
        // Verify AudioSharingDeviceVolumeControlUpdater overrides BluetoothDeviceUpdater and won't
        // launch device details
        verifyNoInteractions(preference);
    }

    @Test
    public void refreshPreference_doNothing() {
        setupPreferenceMapWithDevice();
        verify(mDevicePreferenceCallback).onDeviceAdded(any(Preference.class));
        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(ImmutableList.of());
        mDeviceUpdater.refreshPreference();
        // Verify AudioSharingDeviceVolumeControlUpdater overrides BluetoothDeviceUpdater and won't
        // refresh preference map
        verify(mDevicePreferenceCallback, never()).onDeviceRemoved(any(Preference.class));
    }

    private void setupPreferenceMapWithDevice() {
        // Add device to preferenceMap
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        when(mAssistant.getAllSources(mBluetoothDevice)).thenReturn(ImmutableList.of(mState));
        when(mDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(true);
        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();
    }
}
