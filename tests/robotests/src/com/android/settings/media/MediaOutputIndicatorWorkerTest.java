/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.media;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.net.Uri;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.HearingAidProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBluetoothDevice;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothUtils.class,
        ShadowBluetoothDevice.class})
public class MediaOutputIndicatorWorkerTest {

    private static final String TEST_A2DP_DEVICE_NAME = "Test_A2DP_BT_Device_NAME";
    private static final String TEST_HAP_DEVICE_NAME = "Test_HAP_BT_Device_NAME";
    private static final String TEST_A2DP_DEVICE_ADDRESS = "00:A1:A1:A1:A1:A1";
    private static final String TEST_HAP_DEVICE_ADDRESS = "00:B2:B2:B2:B2:B2";
    private static final Uri URI = Uri.parse("content://com.android.settings.slices/test");

    @Mock
    private A2dpProfile mA2dpProfile;
    @Mock
    private HearingAidProfile mHearingAidProfile;
    @Mock
    private LocalBluetoothManager mLocalManager;
    @Mock
    private BluetoothEventManager mBluetoothEventManager;
    @Mock
    private LocalBluetoothProfileManager mLocalBluetoothProfileManager;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mA2dpDevice;
    private BluetoothDevice mHapDevice;
    private BluetoothManager mBluetoothManager;
    private Context mContext;
    private List<BluetoothDevice> mDevicesList;
    private LocalBluetoothManager mLocalBluetoothManager;
    private MediaOutputIndicatorWorker mMediaDeviceUpdateWorker;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalManager;
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        when(mLocalBluetoothManager.getEventManager()).thenReturn(mBluetoothEventManager);
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mLocalBluetoothProfileManager);
        when(mLocalBluetoothProfileManager.getA2dpProfile()).thenReturn(mA2dpProfile);
        when(mLocalBluetoothProfileManager.getHearingAidProfile()).thenReturn(mHearingAidProfile);
        mBluetoothManager = new BluetoothManager(mContext);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Setup A2dp device
        mA2dpDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_A2DP_DEVICE_ADDRESS));
        when(mA2dpDevice.getName()).thenReturn(TEST_A2DP_DEVICE_NAME);
        when(mA2dpDevice.isConnected()).thenReturn(true);
        // Setup HearingAid device
        mHapDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_HAP_DEVICE_ADDRESS));
        when(mHapDevice.getName()).thenReturn(TEST_HAP_DEVICE_NAME);
        when(mHapDevice.isConnected()).thenReturn(true);

        mMediaDeviceUpdateWorker = new MediaOutputIndicatorWorker(mContext, URI);
        mDevicesList = new ArrayList<>();
    }

    @Test
    public void isVisible_noConnectableDevice_returnFalse() {
        mDevicesList.clear();
        when(mA2dpProfile.getConnectableDevices()).thenReturn(mDevicesList);

        assertThat(mMediaDeviceUpdateWorker.isVisible()).isFalse();
    }

    @Test
    public void isVisible_withConnectableA2dpDevice_returnTrue() {
        mDevicesList.clear();
        mDevicesList.add(mA2dpDevice);
        when(mHearingAidProfile.getConnectableDevices()).thenReturn(mDevicesList);

        assertThat(mMediaDeviceUpdateWorker.isVisible()).isTrue();
    }

    @Test
    public void isVisible_withConnectableHADevice_returnTrue() {
        mDevicesList.clear();
        mDevicesList.add(mHapDevice);
        when(mA2dpProfile.getConnectableDevices()).thenReturn(mDevicesList);

        assertThat(mMediaDeviceUpdateWorker.isVisible()).isTrue();
    }

    @Test
    public void findActiveDeviceName_A2dpDeviceActive_verifyName() {
        when(mA2dpProfile.getActiveDevice()).thenReturn(mA2dpDevice);

        assertThat(mMediaDeviceUpdateWorker.findActiveDeviceName())
                .isEqualTo(mA2dpDevice.getAliasName());
    }

    @Test
    public void findActiveDeviceName_HADeviceActive_verifyName() {
        mDevicesList.add(mHapDevice);
        when(mHearingAidProfile.getActiveDevices()).thenReturn(mDevicesList);

        assertThat(mMediaDeviceUpdateWorker.findActiveDeviceName())
                .isEqualTo(mHapDevice.getAliasName());
    }

    @Test
    public void findActiveDeviceName_noActiveDevice_verifyDefaultName() {
        when(mA2dpProfile.getActiveDevice()).thenReturn(null);
        mDevicesList.clear();
        when(mHearingAidProfile.getActiveDevices()).thenReturn(mDevicesList);

        assertThat(mMediaDeviceUpdateWorker.findActiveDeviceName())
                .isEqualTo(mContext.getText(R.string.media_output_default_summary));
    }
}
