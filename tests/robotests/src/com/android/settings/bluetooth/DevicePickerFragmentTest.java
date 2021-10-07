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
 * limitations under the License.
 */

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DevicePickerFragmentTest {

    @Mock
    private BluetoothProgressCategory mAvailableDevicesCategory;

    private DevicePickerFragment mFragment;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFragment = new DevicePickerFragment();
        mContext = spy(RuntimeEnvironment.application);
        mFragment.mContext = mContext;
        mFragment.mAvailableDevicesCategory = mAvailableDevicesCategory;
    }

    @Test
    public void testScanningStateChanged_started_setProgressStarted() {
        mFragment.mScanEnabled = true;

        mFragment.onScanningStateChanged(true);

        verify(mAvailableDevicesCategory).setProgress(true);
    }

    @Test
    public void callingPackageIsEqualToLaunchPackage_sendBroadcastToLaunchPackage() {
        final CachedBluetoothDevice cachedDevice = mock(CachedBluetoothDevice.class);
        final BluetoothDevice bluetoothDevice = mock(BluetoothDevice.class);
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        when(cachedDevice.getDevice()).thenReturn(bluetoothDevice);
        mFragment.mSelectedDevice = bluetoothDevice;
        mFragment.mLaunchPackage = "com.android.settings";
        mFragment.mLaunchClass = "com.android.settings.bluetooth.BluetoothPermissionActivity";
        mFragment.mCallingAppPackageName = "com.android.settings";

        mFragment.onDeviceBondStateChanged(cachedDevice, BluetoothDevice.BOND_BONDED);

        verify(mContext).sendBroadcast(intentCaptor.capture(),
                eq("android.permission.BLUETOOTH_ADMIN"));
        assertThat(intentCaptor.getValue().getComponent().getPackageName())
                .isEqualTo(mFragment.mLaunchPackage);
    }

    @Test
    public void callingPackageIsNotEqualToLaunchPackage_broadcastNotSend() {
        final CachedBluetoothDevice cachedDevice = mock(CachedBluetoothDevice.class);
        final BluetoothDevice bluetoothDevice = mock(BluetoothDevice.class);
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        when(cachedDevice.getDevice()).thenReturn(bluetoothDevice);
        mFragment.mSelectedDevice = bluetoothDevice;
        mFragment.mLaunchPackage = "com.fake.settings";
        mFragment.mLaunchClass = "com.android.settings.bluetooth.BluetoothPermissionActivity";
        mFragment.mCallingAppPackageName = "com.android.settings";

        mFragment.onDeviceBondStateChanged(cachedDevice, BluetoothDevice.BOND_BONDED);

        verify(mContext, never()).sendBroadcast(intentCaptor.capture());
    }
}
