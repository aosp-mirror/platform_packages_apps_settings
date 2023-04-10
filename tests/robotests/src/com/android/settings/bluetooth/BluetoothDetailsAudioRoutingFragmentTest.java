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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
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

/** Tests for {@link BluetoothDetailsAudioRoutingFragment}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothUtils.class})
public class BluetoothDetailsAudioRoutingFragmentTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final String TEST_ADDRESS = "55:66:77:88:99:AA";

    private final Context mContext = ApplicationProvider.getApplicationContext();

    private BluetoothDetailsAudioRoutingFragment mFragment;
    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;
    @Mock
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    @Mock
    private LocalBluetoothAdapter mLocalBluetoothAdapter;
    @Mock
    private BluetoothDevice mBluetoothDevice;
    @Mock
    private CachedBluetoothDevice mCachedDevice;

    @Before
    public void setUp() {
        setupEnvironment();

        when(mLocalBluetoothAdapter.getRemoteDevice(TEST_ADDRESS)).thenReturn(mBluetoothDevice);
        when(mCachedDevice.getAddress()).thenReturn(TEST_ADDRESS);
        when(mCachedDeviceManager.findDevice(mBluetoothDevice)).thenReturn(mCachedDevice);

        mFragment = new BluetoothDetailsAudioRoutingFragment();
    }

    @Test
    public void onAttach_setArgumentsWithAddress_expectedCachedDeviceWithAddress() {
        final Bundle args = new Bundle();
        args.putString(BluetoothDeviceDetailsFragment.KEY_DEVICE_ADDRESS, TEST_ADDRESS);
        mFragment.setArguments(args);

        mFragment.onAttach(mContext);

        assertThat(mFragment.mCachedDevice.getAddress()).isEqualTo(TEST_ADDRESS);
    }

    private void setupEnvironment() {
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        when(mLocalBluetoothManager.getBluetoothAdapter()).thenReturn(mLocalBluetoothAdapter);
    }
}
