/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.wifi.dpp;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiPickerTracker;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class WifiDppQrCodeScannerFragmentTest {

    static final String WIFI_SSID = "wifi-ssid";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    WifiPickerTracker mWifiPickerTracker;
    @Mock
    WifiEntry mWifiEntry;

    WifiDppQrCodeScannerFragment mFragment;

    @Before
    public void setUp() {
        when(mWifiEntry.getSsid()).thenReturn(WIFI_SSID);
        when(mWifiPickerTracker.getWifiEntries()).thenReturn(Arrays.asList(mWifiEntry));

        mFragment = spy(new WifiDppQrCodeScannerFragment());
        mFragment.mWifiPickerTracker = mWifiPickerTracker;
    }

    @Test
    public void canConnectWifi_noAvailableWifiMatch_returnTrue() {
        when(mWifiEntry.getSsid()).thenReturn("diff-wifi-ssid");
        when(mWifiEntry.canConnect()).thenReturn(false);

        assertThat(mFragment.canConnectWifi(WIFI_SSID)).isTrue();
    }

    @Test
    public void canConnectWifi_wifiCanConnect_returnTrue() {
        when(mWifiEntry.canConnect()).thenReturn(true);

        assertThat(mFragment.canConnectWifi(WIFI_SSID)).isTrue();
    }

    @Test
    public void canConnectWifi_wifiCanNotConnect_returnFalseAndShowError() {
        when(mWifiEntry.canConnect()).thenReturn(false);
        doNothing().when(mFragment).showErrorMessageAndRestartCamera(anyInt());

        assertThat(mFragment.canConnectWifi(WIFI_SSID)).isFalse();
        verify(mFragment).showErrorMessageAndRestartCamera(anyInt());
    }
}
