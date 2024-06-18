/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.util.Pair;

import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.testutils.shadow.ShadowAudioManager;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowCachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.flags.Flags;

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
@Config(shadows = {ShadowAudioManager.class, ShadowBluetoothAdapter.class,
        ShadowCachedBluetoothDeviceManager.class})
public class ConnectedBluetoothDeviceUpdaterTest {

    private static final String MAC_ADDRESS = "04:52:C7:0B:D8:3C";
    private static final String FAKE_EXCLUSIVE_MANAGER_NAME = "com.fake.name";

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Mock
    private DashboardFragment mDashboardFragment;
    @Mock
    private DevicePreferenceCallback mDevicePreferenceCallback;
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private BluetoothDevice mBluetoothDevice;
    @Mock
    private Drawable mDrawable;
    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private ConnectedBluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private Collection<CachedBluetoothDevice> mCachedDevices;
    private AudioManager mAudioManager;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private ShadowCachedBluetoothDeviceManager mShadowCachedBluetoothDeviceManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Pair<Drawable, String> pairs = new Pair<>(mDrawable, "fake_device");
        mContext = spy(RuntimeEnvironment.application);
        mAudioManager = mContext.getSystemService(AudioManager.class);
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowCachedBluetoothDeviceManager = Shadow.extract(
                Utils.getLocalBtManager(mContext).getCachedDeviceManager());
        doReturn(mContext).when(mDashboardFragment).getContext();
        mCachedDevices = new ArrayList<>();
        mCachedDevices.add(mCachedBluetoothDevice);

        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mCachedBluetoothDevice.getDevice()).thenReturn(mBluetoothDevice);
        when(mCachedBluetoothDevice.getAddress()).thenReturn(MAC_ADDRESS);
        when(mCachedBluetoothDevice.getDrawableWithDescription()).thenReturn(pairs);
        mShadowCachedBluetoothDeviceManager.setCachedDevicesCopy(mCachedDevices);
        mBluetoothDeviceUpdater = spy(new ConnectedBluetoothDeviceUpdater(mContext,
                mDevicePreferenceCallback, /* metricsCategory= */ 0));
        mBluetoothDeviceUpdater.setPrefContext(mContext);
        doNothing().when(mBluetoothDeviceUpdater).addPreference(any());
        doNothing().when(mBluetoothDeviceUpdater).removePreference(any());
    }

    @Test
    public void onAudioModeChanged_hfpDeviceConnected_notInCall_addPreference() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHfpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onAudioModeChanged();

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onAudioModeChanged_hfpDeviceConnected_inCall_removePreference() {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHfpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onAudioModeChanged();

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onAudioModeChanged_a2dpDeviceConnected_notInCall_removePreference() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedA2dpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onAudioModeChanged();

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onAudioModeChanged_a2dpDeviceConnected_inCall_addPreference() {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedA2dpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onAudioModeChanged();

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_a2dpDeviceConnected_inCall_addPreference() {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedA2dpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_deviceIsNotInList_inCall_invokesRemovePreference() {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedA2dpDevice()).thenReturn(true);
        mCachedDevices.clear();

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_a2dpDeviceConnected_notInCall_removePreference() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedA2dpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_hfpDeviceConnected_inCall_removePreference() {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHfpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_hfpDeviceConnected_notInCall_addPreference() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHfpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_ashaHearingAidConnected_inCall_removePreference()
    {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.HEARING_AID);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_ashaHearingAidConnected_notInCall_removePreference()
    {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedAshaHearingAidDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.HEARING_AID);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_leAudioDeviceConnected_inCall_removesPreference() {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater
                .isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_leAudioDeviceConnected_notInCall_removesPreference()
    {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater
                .isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }
    @Test
    public void onProfileConnectionStateChanged_deviceIsNotInList_inCall_invokesRemovesPreference()
    {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater
                .isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(true);
        mCachedDevices.clear();

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_deviceIsNotInList_notInCall_invokesRemovesPreference
            () {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater
                .isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(true);
        mCachedDevices.clear();

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_deviceDisconnected_removePreference() {
        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_DISCONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void addPreference_addPreference_shouldHideSecondTarget() {
        BluetoothDevicePreference btPreference =
                new BluetoothDevicePreference(mContext, mCachedBluetoothDevice,
                        true, BluetoothDevicePreference.SortType.TYPE_DEFAULT);
        mBluetoothDeviceUpdater.mPreferenceMap.put(mBluetoothDevice, btPreference);

        mBluetoothDeviceUpdater.addPreference(mCachedBluetoothDevice);

        assertThat(btPreference.shouldHideSecondTarget()).isTrue();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_HIDE_EXCLUSIVELY_MANAGED_BLUETOOTH_DEVICE)
    public void update_notExclusiveManagedDevice_addDevice() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater
                .isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHfpDevice()).thenReturn(true);
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_EXCLUSIVE_MANAGER)).thenReturn(
                null);

        mBluetoothDeviceUpdater.update(mCachedBluetoothDevice);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_HIDE_EXCLUSIVELY_MANAGED_BLUETOOTH_DEVICE)
    public void update_notAllowedExclusiveManagedDevice_addDevice() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater
                .isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHfpDevice()).thenReturn(true);
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_EXCLUSIVE_MANAGER)).thenReturn(
                FAKE_EXCLUSIVE_MANAGER_NAME.getBytes());

        mBluetoothDeviceUpdater.update(mCachedBluetoothDevice);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_HIDE_EXCLUSIVELY_MANAGED_BLUETOOTH_DEVICE)
    public void update_existingExclusivelyManagedDeviceWithPackageInstalled_removePreference()
            throws Exception {
        final String exclusiveManagerName =
                BluetoothUtils.getExclusiveManagers().stream().findAny().orElse(
                        FAKE_EXCLUSIVE_MANAGER_NAME);
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater
                .isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHfpDevice()).thenReturn(true);
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_EXCLUSIVE_MANAGER)).thenReturn(
                exclusiveManagerName.getBytes());
        doReturn(new PackageInfo()).when(mPackageManager).getPackageInfo(exclusiveManagerName, 0);

        mBluetoothDeviceUpdater.update(mCachedBluetoothDevice);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
        verify(mBluetoothDeviceUpdater, never()).addPreference(mCachedBluetoothDevice);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_HIDE_EXCLUSIVELY_MANAGED_BLUETOOTH_DEVICE)
    public void update_newExclusivelyManagedDeviceWithPackageInstalled_doNotAddPreference()
            throws Exception {
        final String exclusiveManagerName =
                BluetoothUtils.getExclusiveManagers().stream().findAny().orElse(
                        FAKE_EXCLUSIVE_MANAGER_NAME);
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater
                .isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHfpDevice()).thenReturn(true);
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_EXCLUSIVE_MANAGER)).thenReturn(
                exclusiveManagerName.getBytes());
        doReturn(new PackageInfo()).when(mPackageManager).getPackageInfo(exclusiveManagerName, 0);

        mBluetoothDeviceUpdater.update(mCachedBluetoothDevice);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
        verify(mBluetoothDeviceUpdater, never()).addPreference(mCachedBluetoothDevice);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_HIDE_EXCLUSIVELY_MANAGED_BLUETOOTH_DEVICE)
    public void update_exclusivelyManagedDeviceWithoutPackageInstalled_addDevice()
            throws Exception {
        final String exclusiveManagerName =
                BluetoothUtils.getExclusiveManagers().stream().findAny().orElse(
                        FAKE_EXCLUSIVE_MANAGER_NAME);
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater
                .isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHfpDevice()).thenReturn(true);
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_EXCLUSIVE_MANAGER)).thenReturn(
                exclusiveManagerName.getBytes());
        doThrow(new PackageManager.NameNotFoundException()).when(mPackageManager).getPackageInfo(
                exclusiveManagerName, 0);

        mBluetoothDeviceUpdater.update(mCachedBluetoothDevice);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }
}
