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

package com.android.settings.wifi.p2p;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.net.wifi.p2p.WifiP2pDevice;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class P2pThisDevicePreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    private Preference mPreference;
    private P2pThisDevicePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mPreference = new Preference(RuntimeEnvironment.application);
        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mPreference);

        mController = new P2pThisDevicePreferenceController(RuntimeEnvironment.application);
    }

    @Test
    public void isAlwaysAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateDeviceName_emptyName_shouldUseIpAddress() {
        WifiP2pDevice device = new WifiP2pDevice();
        device.deviceAddress = "address";
        mController.displayPreference(mPreferenceScreen);
        mController.updateDeviceName(device);

        assertThat(mPreference.getTitle()).isEqualTo(device.deviceAddress);
    }

    @Test
    public void updateDeviceName_hasName_shouldUseName() {
        WifiP2pDevice device = new WifiP2pDevice();
        device.deviceAddress = "address";
        device.deviceName = "name";
        mController.displayPreference(mPreferenceScreen);
        mController.updateDeviceName(device);

        assertThat(mPreference.getTitle()).isEqualTo(device.deviceName);
    }
}
