/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

/** Tests for {@link SavedHearingDeviceUpdater}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothUtils.class})
public class SavedHearingDeviceUpdaterTest {
    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Mock
    private DevicePreferenceCallback mDevicePreferenceCallback;
    @Mock
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private BluetoothDevice mBluetoothDevice;
    private SavedHearingDeviceUpdater mUpdater;

    @Before
    public void setUp() {
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        when(mCachedBluetoothDevice.getDevice()).thenReturn(mBluetoothDevice);
        mUpdater = new SavedHearingDeviceUpdater(mContext,
                mDevicePreferenceCallback, /* metricsCategory= */ 0);
    }

    @Test
    public void isFilterMatch_savedHearingDevice_returnTrue() {
        CachedBluetoothDevice savedHearingDevice = mCachedBluetoothDevice;
        when(savedHearingDevice.isHearingAidDevice()).thenReturn(true);
        doReturn(BluetoothDevice.BOND_BONDED).when(mBluetoothDevice).getBondState();
        doReturn(false).when(mBluetoothDevice).isConnected();
        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(
                new ArrayList<>(List.of(savedHearingDevice)));

        assertThat(mUpdater.isFilterMatched(savedHearingDevice)).isEqualTo(true);
    }

    @Test
    public void isFilterMatch_savedNonHearingDevice_returnFalse() {
        CachedBluetoothDevice savedNonHearingDevice = mCachedBluetoothDevice;
        when(savedNonHearingDevice.isHearingAidDevice()).thenReturn(false);
        doReturn(BluetoothDevice.BOND_BONDED).when(mBluetoothDevice).getBondState();
        doReturn(false).when(mBluetoothDevice).isConnected();
        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(
                new ArrayList<>(List.of(savedNonHearingDevice)));

        assertThat(mUpdater.isFilterMatched(savedNonHearingDevice)).isEqualTo(false);
    }

    @Test
    public void isFilterMatch_savedBondingHearingDevice_returnFalse() {
        CachedBluetoothDevice savedBondingHearingDevice = mCachedBluetoothDevice;
        when(savedBondingHearingDevice.isHearingAidDevice()).thenReturn(true);
        doReturn(BluetoothDevice.BOND_BONDING).when(mBluetoothDevice).getBondState();
        doReturn(false).when(mBluetoothDevice).isConnected();
        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(
                new ArrayList<>(List.of(savedBondingHearingDevice)));

        assertThat(mUpdater.isFilterMatched(savedBondingHearingDevice)).isEqualTo(false);
    }

    @Test
    public void isFilterMatch_connectedHearingDevice_returnFalse() {
        CachedBluetoothDevice connectdHearingDevice = mCachedBluetoothDevice;
        when(connectdHearingDevice.isHearingAidDevice()).thenReturn(true);
        doReturn(BluetoothDevice.BOND_BONDED).when(mBluetoothDevice).getBondState();
        doReturn(true).when(mBluetoothDevice).isConnected();
        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(
                new ArrayList<>(List.of(connectdHearingDevice)));

        assertThat(mUpdater.isFilterMatched(connectdHearingDevice)).isEqualTo(false);
    }

    @Test
    public void isFilterMatch_hearingDeviceNotInCachedDevicesList_returnFalse() {
        CachedBluetoothDevice notInCachedDevicesListDevice = mCachedBluetoothDevice;
        when(notInCachedDevicesListDevice.isHearingAidDevice()).thenReturn(true);
        doReturn(BluetoothDevice.BOND_BONDED).when(mBluetoothDevice).getBondState();
        doReturn(false).when(mBluetoothDevice).isConnected();
        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(new ArrayList<>());

        assertThat(mUpdater.isFilterMatched(notInCachedDevicesListDevice)).isEqualTo(false);
    }
}
