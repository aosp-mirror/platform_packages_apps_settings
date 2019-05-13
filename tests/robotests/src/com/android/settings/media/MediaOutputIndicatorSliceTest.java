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
 *
 */

package com.android.settings.media;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.telephony.TelephonyManager;

import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.A2dpProfile;
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
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowTelephonyManager;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothUtils.class,
        ShadowTelephonyManager.class})
public class MediaOutputIndicatorSliceTest {

    private static final String TEST_A2DP_DEVICE_NAME = "Test_A2DP_BT_Device_NAME";
    private static final String TEST_HAP_DEVICE_NAME = "Test_HAP_BT_Device_NAME";
    private static final String TEST_A2DP_DEVICE_ADDRESS = "00:A1:A1:A1:A1:A1";
    private static final String TEST_HAP_DEVICE_ADDRESS = "00:B2:B2:B2:B2:B2";

    @Mock
    private A2dpProfile mA2dpProfile;
    @Mock
    private HearingAidProfile mHearingAidProfile;
    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;
    @Mock
    private LocalBluetoothProfileManager mLocalBluetoothProfileManager;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mA2dpDevice;
    private BluetoothDevice mHapDevice;
    private BluetoothManager mBluetoothManager;
    private Context mContext;
    private List<BluetoothDevice> mDevicesList;
    private MediaOutputIndicatorSlice mMediaOutputIndicatorSlice;
    private ShadowTelephonyManager mShadowTelephonyManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mShadowTelephonyManager = Shadow.extract(mContext.getSystemService(
                Context.TELEPHONY_SERVICE));
        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);

        // Setup Bluetooth environment
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        mBluetoothManager = new BluetoothManager(mContext);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mLocalBluetoothProfileManager);
        when(mLocalBluetoothProfileManager.getA2dpProfile()).thenReturn(mA2dpProfile);
        when(mLocalBluetoothProfileManager.getHearingAidProfile()).thenReturn(mHearingAidProfile);

        // Setup A2dp device
        mA2dpDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_A2DP_DEVICE_ADDRESS));
        when(mA2dpDevice.getName()).thenReturn(TEST_A2DP_DEVICE_NAME);
        when(mA2dpDevice.isConnected()).thenReturn(true);
        // Setup HearingAid device
        mHapDevice = spy(mBluetoothAdapter.getRemoteDevice(TEST_HAP_DEVICE_ADDRESS));
        when(mHapDevice.getName()).thenReturn(TEST_HAP_DEVICE_NAME);
        when(mHapDevice.isConnected()).thenReturn(true);

        mMediaOutputIndicatorSlice = new MediaOutputIndicatorSlice(mContext);
        mDevicesList = new ArrayList<>();
    }

    @Test
    public void getSlice_noConnectedDevice_returnNull() {
        mDevicesList.clear();
        when(mA2dpProfile.getConnectedDevices()).thenReturn(mDevicesList);

        assertThat(mMediaOutputIndicatorSlice.getSlice()).isNull();
    }

    @Test
    public void getSlice_noActiveDevice_verifyDefaultName() {
        mDevicesList.add(mA2dpDevice);
        when(mA2dpProfile.getConnectedDevices()).thenReturn(mDevicesList);
        when(mA2dpProfile.getActiveDevice()).thenReturn(null);

        // Verify slice title and subtitle
        final Slice mediaSlice = mMediaOutputIndicatorSlice.getSlice();
        final SliceMetadata metadata = SliceMetadata.from(mContext, mediaSlice);
        assertThat(metadata.getTitle()).isEqualTo(mContext.getText(R.string.media_output_title));
        assertThat(metadata.getSubtitle()).isEqualTo(mContext.getText(
                R.string.media_output_default_summary));
    }

    @Test
    public void getSlice_A2dpDeviceActive_verifyName() {
        mDevicesList.add(mA2dpDevice);
        when(mA2dpProfile.getConnectedDevices()).thenReturn(mDevicesList);
        when(mA2dpProfile.getActiveDevice()).thenReturn(mA2dpDevice);

        final Slice mediaSlice = mMediaOutputIndicatorSlice.getSlice();
        final SliceMetadata metadata = SliceMetadata.from(mContext, mediaSlice);
        assertThat(metadata.getTitle()).isEqualTo(mContext.getText(R.string.media_output_title));
        assertThat(metadata.getSubtitle()).isEqualTo(TEST_A2DP_DEVICE_NAME);
    }

    @Test
    public void getSlice_HADeviceActive_verifyName() {
        mDevicesList.add(mHapDevice);
        when(mHearingAidProfile.getConnectedDevices()).thenReturn(mDevicesList);
        when(mHearingAidProfile.getActiveDevices()).thenReturn(mDevicesList);

        // Verify slice title and subtitle
        final Slice mediaSlice = mMediaOutputIndicatorSlice.getSlice();
        final SliceMetadata metadata = SliceMetadata.from(mContext, mediaSlice);
        assertThat(metadata.getTitle()).isEqualTo(mContext.getText(R.string.media_output_title));
        assertThat(metadata.getSubtitle()).isEqualTo(TEST_HAP_DEVICE_NAME);
    }

    @Test
    public void getSlice_callStateIdle_available() {
        mDevicesList.add(mA2dpDevice);
        when(mA2dpProfile.getConnectedDevices()).thenReturn(mDevicesList);
        mShadowTelephonyManager.setCallState(TelephonyManager.CALL_STATE_IDLE);

        assertThat(mMediaOutputIndicatorSlice.getSlice()).isNotNull();
    }

    @Test
    public void getSlice_callStateRinging_returnNull() {
        mDevicesList.add(mA2dpDevice);
        when(mA2dpProfile.getConnectedDevices()).thenReturn(mDevicesList);
        mShadowTelephonyManager.setCallState(TelephonyManager.CALL_STATE_RINGING);

        assertThat(mMediaOutputIndicatorSlice.getSlice()).isNull();
    }

    @Test
    public void getSlice_callStateOffHook_returnNull() {
        mDevicesList.add(mA2dpDevice);
        when(mA2dpProfile.getConnectedDevices()).thenReturn(mDevicesList);
        mShadowTelephonyManager.setCallState(TelephonyManager.CALL_STATE_OFFHOOK);

        assertThat(mMediaOutputIndicatorSlice.getSlice()).isNull();
    }
}
