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

import static com.android.settings.connecteddevice.audiosharing.AudioSharingBluetoothDeviceUpdater.PREF_KEY_PREFIX;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.Pair;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.BluetoothDevicePreference;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.flags.Flags;

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
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
            ShadowThreadUtils.class
        })
public class AudioSharingBluetoothDeviceUpdaterTest {
    private static final String MAC_ADDRESS = "04:52:C7:0B:D8:3C";
    private static final String TEST_DEVICE_NAME = "test";
    private static final String TAG = "AudioSharingBluetoothDeviceUpdater";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private DevicePreferenceCallback mDevicePreferenceCallback;
    @Mock private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock private BluetoothDevice mBluetoothDevice;
    @Mock private Drawable mDrawable;
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private CachedBluetoothDeviceManager mCachedDeviceManager;
    @Mock private LocalBluetoothProfileManager mLocalBtProfileManager;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private BluetoothLeBroadcastReceiveState mState;

    private Context mContext;
    private AudioSharingBluetoothDeviceUpdater mDeviceUpdater;
    private Collection<CachedBluetoothDevice> mCachedDevices;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mLocalBtManager = Utils.getLocalBtManager(mContext);
        when(mLocalBtManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        when(mLocalBtManager.getProfileManager()).thenReturn(mLocalBtProfileManager);
        when(mLocalBtProfileManager.getLeAudioBroadcastAssistantProfile()).thenReturn(mAssistant);
        List<Long> bisSyncState = new ArrayList<>();
        bisSyncState.add(1L);
        when(mState.getBisSyncState()).thenReturn(bisSyncState);
        Pair<Drawable, String> pairs = new Pair<>(mDrawable, TEST_DEVICE_NAME);
        doReturn(TEST_DEVICE_NAME).when(mCachedBluetoothDevice).getName();
        doReturn(mBluetoothDevice).when(mCachedBluetoothDevice).getDevice();
        doReturn(MAC_ADDRESS).when(mCachedBluetoothDevice).getAddress();
        doReturn(pairs).when(mCachedBluetoothDevice).getDrawableWithDescription();
        doReturn(ImmutableSet.of()).when(mCachedBluetoothDevice).getMemberDevice();
        doReturn("").when(mCachedBluetoothDevice).getConnectionSummary();
        mCachedDevices = new ArrayList<>();
        mCachedDevices.add(mCachedBluetoothDevice);
        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(mCachedDevices);
        doNothing().when(mDevicePreferenceCallback).onDeviceAdded(any(Preference.class));
        doNothing().when(mDevicePreferenceCallback).onDeviceRemoved(any(Preference.class));
        mDeviceUpdater =
                spy(
                        new AudioSharingBluetoothDeviceUpdater(
                                mContext, mDevicePreferenceCallback, /* metricsCategory= */ 0));
        mDeviceUpdater.setPrefContext(mContext);
    }

    @After
    public void tearDown() {
        ShadowThreadUtils.reset();
        ShadowBluetoothUtils.reset();
    }

    @Test
    public void onProfileConnectionStateChanged_leaDeviceConnected_flagOff_removesPref() {
        setupPreferenceMapWithDevice();

        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);

        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceRemoved(captor.capture());
        assertThat(captor.getValue() instanceof BluetoothDevicePreference).isTrue();
        assertThat(((BluetoothDevicePreference) captor.getValue()).getBluetoothDevice())
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
        assertThat(captor.getValue() instanceof BluetoothDevicePreference).isTrue();
        assertThat(((BluetoothDevicePreference) captor.getValue()).getBluetoothDevice())
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
        assertThat(captor.getValue() instanceof BluetoothDevicePreference).isTrue();
        assertThat(((BluetoothDevicePreference) captor.getValue()).getBluetoothDevice())
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
        assertThat(captor.getValue() instanceof BluetoothDevicePreference).isTrue();
        assertThat(((BluetoothDevicePreference) captor.getValue()).getBluetoothDevice())
                .isEqualTo(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_leaDeviceDisconnecting_removesPref() {
        setupPreferenceMapWithDevice();
        doReturn(false).when(mCachedBluetoothDevice).isConnectedLeAudioDevice();
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);

        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceRemoved(captor.capture());
        assertThat(captor.getValue() instanceof BluetoothDevicePreference).isTrue();
        assertThat(((BluetoothDevicePreference) captor.getValue()).getBluetoothDevice())
                .isEqualTo(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_leaDeviceConnected_hasSource_addsPreference() {
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);
        setupPreferenceMapWithDevice();

        verify(mDevicePreferenceCallback).onDeviceAdded(captor.capture());
        assertThat(captor.getValue() instanceof BluetoothDevicePreference).isTrue();
        assertThat(((BluetoothDevicePreference) captor.getValue()).getBluetoothDevice())
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
    public void onPreferenceClick_logClick() {
        Preference preference = new Preference(mContext);
        mDeviceUpdater.onPreferenceClick(preference);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_AUDIO_SHARING_DEVICE_CLICK);
    }

    private void setupPreferenceMapWithDevice() {
        // Add device to preferenceMap
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        when(mAssistant.getAllSources(mBluetoothDevice)).thenReturn(ImmutableList.of(mState));
        when(mDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        doReturn(true).when(mCachedBluetoothDevice).isConnectedLeAudioDevice();
        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();
    }
}
