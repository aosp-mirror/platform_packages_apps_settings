/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.wifi.p2p;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiP2pPeerTest {

    private static final String DEVICE_NAME = "fakeName";
    private static final String OTHER_NAME = "otherName";
    private static final String MAC_ADDRESS = "00:11:22:33:44:55";

    private Context mContext;
    private WifiP2pPeer mPreference;

    @Mock
    private WifiP2pDevice mWifiP2pDevice;
    @Mock
    private WifiP2pPeer mOtherWifiP2pPeer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void compareTo_withSameDeviceName_shouldBeZero() {
        setupOneOtherP2pPeer(DEVICE_NAME, null /* address */);
        mWifiP2pDevice.deviceName = DEVICE_NAME;
        mPreference = new WifiP2pPeer(mContext, mWifiP2pDevice);

        assertThat(mPreference.compareTo(mOtherWifiP2pPeer)).isEqualTo(0);
    }

    @Test
    public void compareTo_withDifferentDeviceName_shouldNotZero() {
        setupOneOtherP2pPeer(DEVICE_NAME, null /* address */);
        mWifiP2pDevice.deviceName = OTHER_NAME;
        mPreference = new WifiP2pPeer(mContext, mWifiP2pDevice);

        assertThat(mPreference.compareTo(mOtherWifiP2pPeer)).isNotEqualTo(0);
    }

    @Test
    public void compareTo_withSameDeviceAddress_shouldBeZero() {
        setupOneOtherP2pPeer(null /* name */, MAC_ADDRESS);
        mWifiP2pDevice.deviceAddress = MAC_ADDRESS;
        mPreference = new WifiP2pPeer(mContext, mWifiP2pDevice);

        assertThat(mPreference.compareTo(mOtherWifiP2pPeer)).isEqualTo(0);
    }

    @Test
    public void compareTo_withLowerDeviceStatus_shouldBeOne() {
        setupOneOtherP2pPeer(DEVICE_NAME, null /* address */);
        mWifiP2pDevice.status = WifiP2pDevice.FAILED;
        mPreference = new WifiP2pPeer(mContext, mWifiP2pDevice);

        assertThat(mPreference.compareTo(mOtherWifiP2pPeer)).isEqualTo(1);
    }

    @Test
    public void compareTo_withNotPeerParameter_shouldBeOne() {
        final Preference fakePreference = mock(Preference.class);
        setupOneOtherP2pPeer(DEVICE_NAME, null /* address */);
        mPreference = new WifiP2pPeer(mContext, mWifiP2pDevice);

        assertThat(mPreference.compareTo(fakePreference)).isEqualTo(1);
    }

    @Test
    public void signalLevel_afterNewPreference_shouldBeExpected() {
        mPreference = new WifiP2pPeer(mContext, mWifiP2pDevice);

        final int expectSignalLevel = WifiManager.calculateSignalLevel(mPreference.mRssi,
                WifiP2pPeer.SIGNAL_LEVELS);

        assertThat(mPreference.getLevel()).isEqualTo(expectSignalLevel);
    }

    private void setupOneOtherP2pPeer(String name, String address) {
        final WifiP2pDevice wifiP2pDevice = mock(WifiP2pDevice.class);
        wifiP2pDevice.status = WifiP2pDevice.CONNECTED;
        wifiP2pDevice.deviceAddress = address;
        wifiP2pDevice.deviceName = name;
        mOtherWifiP2pPeer.device = wifiP2pDevice;
    }
}
