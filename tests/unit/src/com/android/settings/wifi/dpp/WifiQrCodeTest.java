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

package com.android.settings.wifi.dpp;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WifiQrCodeTest {
    @Test
    public void testZxParsing_validCode() {
        WifiNetworkConfig config = new WifiQrCode("WIFI:S:testAbC;T:nopass").getWifiNetworkConfig();
        assertThat(config.getSsid()).isEqualTo("testAbC");
        assertThat(config.getSecurity()).isEqualTo("nopass");

        config = new WifiQrCode(
                "WIFI:S:reallyLONGone;T:WEP;P:somepasswo#%^**123rd").getWifiNetworkConfig();
        assertThat(config.getSsid()).isEqualTo("reallyLONGone");
        assertThat(config.getSecurity()).isEqualTo("WEP");
        assertThat(config.getPreSharedKey()).isEqualTo("somepasswo#%^**123rd");

        config = new WifiQrCode("WIFI:S:anotherone;T:WPA;P:3#=3j9asicla").getWifiNetworkConfig();
        assertThat(config.getSsid()).isEqualTo("anotherone");
        assertThat(config.getSecurity()).isEqualTo("WPA");
        assertThat(config.getPreSharedKey()).isEqualTo("3#=3j9asicla");

        config = new WifiQrCode("WIFI:S:xx;T:SAE;P:a").getWifiNetworkConfig();
        assertThat(config.getSsid()).isEqualTo("xx");
        assertThat(config.getSecurity()).isEqualTo("SAE");
        assertThat(config.getPreSharedKey()).isEqualTo("a");
    }

    @Test
    public void testZxParsing_invalidCodeButShouldWork() {
        WifiNetworkConfig config = new WifiQrCode(
                "WIFI:S:testAbC; T:nopass").getWifiNetworkConfig();
        assertThat(config.getSsid()).isEqualTo("testAbC");
        assertThat(config.getSecurity()).isEqualTo("nopass");

        config = new WifiQrCode(
                "WIFI:S:reallyLONGone;T:WEP; P:somepassword").getWifiNetworkConfig();
        assertThat(config.getSsid()).isEqualTo("reallyLONGone");
        assertThat(config.getSecurity()).isEqualTo("WEP");
        assertThat(config.getPreSharedKey()).isEqualTo("somepassword");

        config = new WifiQrCode("WIFI: S:anotherone;T:WPA;P:abcdefghihklmn").getWifiNetworkConfig();
        assertThat(config.getSsid()).isEqualTo("anotherone");
        assertThat(config.getSecurity()).isEqualTo("WPA");
        assertThat(config.getPreSharedKey()).isEqualTo("abcdefghihklmn");

        config = new WifiQrCode("WIFI: S:xx; T:SAE;   P:a").getWifiNetworkConfig();
        assertThat(config.getSsid()).isEqualTo("xx");
        assertThat(config.getSecurity()).isEqualTo("SAE");
        assertThat(config.getPreSharedKey()).isEqualTo("a");
    }
}
