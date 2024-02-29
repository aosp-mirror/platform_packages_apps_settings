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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.util.Pair;

import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class})
public class SavedBluetoothDeviceUpdaterTest {

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
    private BluetoothAdapter mBluetoothAdapter;
    @Mock
    private CachedBluetoothDeviceManager mDeviceManager;
    @Mock
    private LocalBluetoothManager mBluetoothManager;
    @Mock
    private Drawable mDrawable;
    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private SavedBluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private BluetoothDevicePreference mPreference;
    private List<CachedBluetoothDevice> mCachedDevices = new ArrayList<>();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Pair<Drawable, String> pairs = new Pair<>(mDrawable, "fake_device");
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mContext).when(mDashboardFragment).getContext();
        when(mCachedBluetoothDevice.getDevice()).thenReturn(mBluetoothDevice);
        when(mCachedBluetoothDevice.getAddress()).thenReturn(MAC_ADDRESS);
        when(mBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDED);
        when(mCachedBluetoothDevice.getDrawableWithDescription()).thenReturn(pairs);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        mBluetoothDeviceUpdater = spy(new SavedBluetoothDeviceUpdater(mContext,
                mDevicePreferenceCallback, false, /* metricsCategory= */ 0));
        mBluetoothDeviceUpdater.setPrefContext(mContext);
        mBluetoothDeviceUpdater.mBluetoothAdapter = mBluetoothAdapter;
        mBluetoothDeviceUpdater.mLocalManager = mBluetoothManager;
        mPreference = new BluetoothDevicePreference(mContext, mCachedBluetoothDevice,
                false, BluetoothDevicePreference.SortType.TYPE_DEFAULT);
        doNothing().when(mBluetoothDeviceUpdater).addPreference(any());
        doNothing().when(mBluetoothDeviceUpdater).removePreference(any());
        mCachedDevices.add(mCachedBluetoothDevice);
        when(mBluetoothManager.getCachedDeviceManager()).thenReturn(mDeviceManager);
        when(mDeviceManager.getCachedDevicesCopy()).thenReturn(mCachedDevices);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_HIDE_EXCLUSIVELY_MANAGED_BLUETOOTH_DEVICE)
    public void update_filterMatch_addPreference() {
        doReturn(BluetoothDevice.BOND_BONDED).when(mBluetoothDevice).getBondState();
        doReturn(false).when(mBluetoothDevice).isConnected();

        mBluetoothDeviceUpdater.update(mCachedBluetoothDevice);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice,
                BluetoothDevicePreference.SortType.TYPE_NO_SORT);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_HIDE_EXCLUSIVELY_MANAGED_BLUETOOTH_DEVICE)
    public void update_filterNotMatch_removePreference() {
        doReturn(BluetoothDevice.BOND_NONE).when(mBluetoothDevice).getBondState();
        doReturn(true).when(mBluetoothDevice).isConnected();

        mBluetoothDeviceUpdater.update(mCachedBluetoothDevice);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_deviceConnected_removePreference() {
        when(mBluetoothDevice.isConnected()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_deviceDisconnected_addPreference() {
        when(mBluetoothDevice.isConnected()).thenReturn(false);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_DISCONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice,
                BluetoothDevicePreference.SortType.TYPE_NO_SORT);
    }

    @Test
    public void
            onProfileConnectionStateChanged_leDeviceDisconnected_inDeviceList_invokesAddPreference()
    {
        when(mBluetoothDevice.isConnected()).thenReturn(false);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_DISCONNECTED, BluetoothProfile.LE_AUDIO);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice,
                BluetoothDevicePreference.SortType.TYPE_NO_SORT);
    }

    @Test
    public void
    onProfileConnectionStateChanged_deviceDisconnected_notInDeviceList_invokesRemovePreference() {
        when(mBluetoothDevice.isConnected()).thenReturn(false);
        mCachedDevices.clear();

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_DISCONNECTED, BluetoothProfile.LE_AUDIO);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onClick_Preference_setConnect() {
        mBluetoothDeviceUpdater.onPreferenceClick(mPreference);

        verify(mCachedBluetoothDevice).connect();
    }

    @Test
    public void onClick_Preference_connected_setActive() {
        when(mCachedBluetoothDevice.isConnected()).thenReturn(true);

        mBluetoothDeviceUpdater.onPreferenceClick(mPreference);

        verify(mCachedBluetoothDevice).setActive();
    }

    @Test
    public void forceUpdate_findCachedBluetoothDeviceIsMatched_addPreference() {
        final List<BluetoothDevice> bluetoothDevices = new ArrayList<>();
        bluetoothDevices.add(mBluetoothDevice);

        when(mBluetoothAdapter.isEnabled()).thenReturn(true);
        when(mBluetoothAdapter.getMostRecentlyConnectedDevices()).thenReturn(bluetoothDevices);
        when(mBluetoothManager.getCachedDeviceManager()).thenReturn(mDeviceManager);
        when(mDeviceManager.findDevice(mBluetoothDevice)).thenReturn(mCachedBluetoothDevice);
        when(mBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDED);
        when(mBluetoothDevice.isConnected()).thenReturn(false);

        mBluetoothDeviceUpdater.forceUpdate();

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice,
                BluetoothDevicePreference.SortType.TYPE_NO_SORT);
    }

    @Test
    public void forceUpdate_findCachedBluetoothDeviceNotMatched_removePreference() {
        final List<BluetoothDevice> bluetoothDevices = new ArrayList<>();
        bluetoothDevices.add(mBluetoothDevice);

        when(mBluetoothAdapter.isEnabled()).thenReturn(true);
        when(mBluetoothAdapter.getMostRecentlyConnectedDevices()).thenReturn(bluetoothDevices);
        when(mBluetoothManager.getCachedDeviceManager()).thenReturn(mDeviceManager);
        when(mDeviceManager.findDevice(mBluetoothDevice)).thenReturn(mCachedBluetoothDevice);
        when(mBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDED);
        when(mBluetoothDevice.isConnected()).thenReturn(true);

        mBluetoothDeviceUpdater.forceUpdate();

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void forceUpdate_notFindCachedBluetoothDevice_doNothing() {
        final List<BluetoothDevice> bluetoothDevices = new ArrayList<>();
        bluetoothDevices.add(mBluetoothDevice);

        when(mBluetoothAdapter.isEnabled()).thenReturn(true);
        when(mBluetoothAdapter.getMostRecentlyConnectedDevices()).thenReturn(bluetoothDevices);
        when(mBluetoothManager.getCachedDeviceManager()).thenReturn(mDeviceManager);
        when(mDeviceManager.findDevice(mBluetoothDevice)).thenReturn(null);

        mBluetoothDeviceUpdater.forceUpdate();

        verify(mBluetoothDeviceUpdater, never()).removePreference(mCachedBluetoothDevice);
        verify(mBluetoothDeviceUpdater, never()).addPreference(mCachedBluetoothDevice,
                BluetoothDevicePreference.SortType.TYPE_NO_SORT);
    }

    @Test
    public void forceUpdate_bluetoothAdapterNotEnable_removeAllDevicesFromPreference() {
        final Collection<CachedBluetoothDevice> cachedDevices = new ArrayList<>();
        cachedDevices.add(mCachedBluetoothDevice);

        when(mBluetoothManager.getCachedDeviceManager()).thenReturn(mDeviceManager);
        when(mDeviceManager.getCachedDevicesCopy()).thenReturn(cachedDevices);
        when(mBluetoothAdapter.isEnabled()).thenReturn(false);

        mBluetoothDeviceUpdater.forceUpdate();

        verify(mBluetoothDeviceUpdater).removeAllDevicesFromPreference();
    }

    @Test
    public void forceUpdate_deviceNotContain_removePreference() {
        final List<BluetoothDevice> bluetoothDevices = new ArrayList<>();
        bluetoothDevices.add(mBluetoothDevice);
        final BluetoothDevice device2 = mock(BluetoothDevice.class);
        final CachedBluetoothDevice cachedDevice2 = mock(CachedBluetoothDevice.class);

        mBluetoothDeviceUpdater.mPreferenceMap.put(device2, mPreference);

        when(cachedDevice2.getDevice()).thenReturn(device2);
        when(cachedDevice2.getAddress()).thenReturn("04:52:C7:0B:D8:3S");
        when(mDeviceManager.findDevice(device2)).thenReturn(cachedDevice2);
        when(mBluetoothAdapter.isEnabled()).thenReturn(true);
        when(mBluetoothAdapter.getMostRecentlyConnectedDevices()).thenReturn(bluetoothDevices);
        when(mBluetoothManager.getCachedDeviceManager()).thenReturn(mDeviceManager);
        when(mDeviceManager.findDevice(mBluetoothDevice)).thenReturn(mCachedBluetoothDevice);
        when(mBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDED);
        when(mBluetoothDevice.isConnected()).thenReturn(false);

        mBluetoothDeviceUpdater.forceUpdate();

        verify(mBluetoothDeviceUpdater).removePreference(cachedDevice2);
        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice,
                BluetoothDevicePreference.SortType.TYPE_NO_SORT);
    }

    @Test
    public void forceUpdate_deviceIsSubDevice_doesNothing() {
        final List<BluetoothDevice> bluetoothDevices = new ArrayList<>();
        bluetoothDevices.add(mBluetoothDevice);

        when(mBluetoothAdapter.isEnabled()).thenReturn(true);
        when(mBluetoothAdapter.getMostRecentlyConnectedDevices()).thenReturn(bluetoothDevices);
        when(mBluetoothManager.getCachedDeviceManager()).thenReturn(mDeviceManager);
        when(mDeviceManager.findDevice(mBluetoothDevice)).thenReturn(mCachedBluetoothDevice);
        when(mDeviceManager.isSubDevice(mBluetoothDevice)).thenReturn(true);

        mBluetoothDeviceUpdater.forceUpdate();

        verify(mBluetoothDeviceUpdater, never()).removePreference(mCachedBluetoothDevice);
        verify(mBluetoothDeviceUpdater, never()).addPreference(mCachedBluetoothDevice,
                BluetoothDevicePreference.SortType.TYPE_NO_SORT);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_HIDE_EXCLUSIVELY_MANAGED_BLUETOOTH_DEVICE)
    public void update_notExclusivelyManagedDevice_addDevice() {
        final Collection<CachedBluetoothDevice> cachedDevices = new ArrayList<>();
        cachedDevices.add(mCachedBluetoothDevice);

        when(mBluetoothAdapter.isEnabled()).thenReturn(true);
        when(mBluetoothManager.getCachedDeviceManager()).thenReturn(mDeviceManager);
        when(mDeviceManager.getCachedDevicesCopy()).thenReturn(cachedDevices);
        when(mBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDED);
        when(mBluetoothDevice.isConnected()).thenReturn(false);
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_EXCLUSIVE_MANAGER)).thenReturn(
                null);

        mBluetoothDeviceUpdater.update(mCachedBluetoothDevice);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice,
                BluetoothDevicePreference.SortType.TYPE_NO_SORT);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_HIDE_EXCLUSIVELY_MANAGED_BLUETOOTH_DEVICE)
    public void update_notAllowedExclusivelyManagedDevice_addDevice() {
        final Collection<CachedBluetoothDevice> cachedDevices = new ArrayList<>();
        cachedDevices.add(mCachedBluetoothDevice);

        when(mBluetoothAdapter.isEnabled()).thenReturn(true);
        when(mBluetoothManager.getCachedDeviceManager()).thenReturn(mDeviceManager);
        when(mDeviceManager.getCachedDevicesCopy()).thenReturn(cachedDevices);
        when(mBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDED);
        when(mBluetoothDevice.isConnected()).thenReturn(false);
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_EXCLUSIVE_MANAGER)).thenReturn(
                FAKE_EXCLUSIVE_MANAGER_NAME.getBytes());

        mBluetoothDeviceUpdater.update(mCachedBluetoothDevice);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice,
                BluetoothDevicePreference.SortType.TYPE_NO_SORT);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_HIDE_EXCLUSIVELY_MANAGED_BLUETOOTH_DEVICE)
    public void update_existingExclusivelyManagedDeviceWithPackageInstalled_removePreference()
            throws Exception {
        final Collection<CachedBluetoothDevice> cachedDevices = new ArrayList<>();
        final String exclusiveManagerName =
                BluetoothUtils.getExclusiveManagers().stream().findAny().orElse(
                        FAKE_EXCLUSIVE_MANAGER_NAME);

        when(mBluetoothAdapter.isEnabled()).thenReturn(true);
        when(mBluetoothManager.getCachedDeviceManager()).thenReturn(mDeviceManager);
        when(mDeviceManager.getCachedDevicesCopy()).thenReturn(cachedDevices);
        when(mBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDED);
        when(mBluetoothDevice.isConnected()).thenReturn(false);
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_EXCLUSIVE_MANAGER)).thenReturn(
                exclusiveManagerName.getBytes());

        doReturn(new PackageInfo()).when(mPackageManager).getPackageInfo(exclusiveManagerName, 0);
        mBluetoothDeviceUpdater.mPreferenceMap.put(mBluetoothDevice, mPreference);

        mBluetoothDeviceUpdater.update(mCachedBluetoothDevice);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
        verify(mBluetoothDeviceUpdater, never()).addPreference(mCachedBluetoothDevice,
                BluetoothDevicePreference.SortType.TYPE_NO_SORT);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_HIDE_EXCLUSIVELY_MANAGED_BLUETOOTH_DEVICE)
    public void update_newExclusivelyManagedDeviceWithPackageInstalled_doNotAddPreference()
            throws Exception {
        final Collection<CachedBluetoothDevice> cachedDevices = new ArrayList<>();
        final String exclusiveManagerName =
                BluetoothUtils.getExclusiveManagers().stream().findAny().orElse(
                        FAKE_EXCLUSIVE_MANAGER_NAME);
        cachedDevices.add(mCachedBluetoothDevice);

        when(mBluetoothAdapter.isEnabled()).thenReturn(true);
        when(mBluetoothManager.getCachedDeviceManager()).thenReturn(mDeviceManager);
        when(mDeviceManager.getCachedDevicesCopy()).thenReturn(cachedDevices);
        when(mBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDED);
        when(mBluetoothDevice.isConnected()).thenReturn(false);
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_EXCLUSIVE_MANAGER)).thenReturn(
                exclusiveManagerName.getBytes());

        doReturn(new PackageInfo()).when(mPackageManager).getPackageInfo(exclusiveManagerName, 0);

        mBluetoothDeviceUpdater.update(mCachedBluetoothDevice);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
        verify(mBluetoothDeviceUpdater, never()).addPreference(mCachedBluetoothDevice,
                BluetoothDevicePreference.SortType.TYPE_NO_SORT);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_HIDE_EXCLUSIVELY_MANAGED_BLUETOOTH_DEVICE)
    public void update_exclusivelyManagedDeviceWithoutPackageInstalled_addDevice()
            throws Exception {
        final Collection<CachedBluetoothDevice> cachedDevices = new ArrayList<>();
        final String exclusiveManagerName =
                BluetoothUtils.getExclusiveManagers().stream().findAny().orElse(
                        FAKE_EXCLUSIVE_MANAGER_NAME);
        cachedDevices.add(mCachedBluetoothDevice);

        when(mBluetoothAdapter.isEnabled()).thenReturn(true);
        when(mBluetoothManager.getCachedDeviceManager()).thenReturn(mDeviceManager);
        when(mDeviceManager.getCachedDevicesCopy()).thenReturn(cachedDevices);
        when(mBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDED);
        when(mBluetoothDevice.isConnected()).thenReturn(false);
        when(mBluetoothDevice.getMetadata(BluetoothDevice.METADATA_EXCLUSIVE_MANAGER)).thenReturn(
                exclusiveManagerName.getBytes());

        doThrow(new PackageManager.NameNotFoundException()).when(mPackageManager).getPackageInfo(
                exclusiveManagerName, 0);

        mBluetoothDeviceUpdater.update(mCachedBluetoothDevice);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice,
                BluetoothDevicePreference.SortType.TYPE_NO_SORT);
    }
}
