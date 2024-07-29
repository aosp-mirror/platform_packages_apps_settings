/*
 * Copyright (C) 2024 The Android Open Source Project
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
public class AdbQrCodeTest {
    @Test
    public void testZxParsing_validCode() {
        WifiNetworkConfig config = new AdbQrCode(
                "WIFI:S:reallyLONGone;T:ADB;P:somepasswo#%^**123rd").getWifiNetworkConfig();
        assertThat(config.getSsid()).isEqualTo("reallyLONGone");
        assertThat(config.getSecurity()).isEqualTo("ADB");
        assertThat(config.getPreSharedKey()).isEqualTo("somepasswo#%^**123rd");

        config = new AdbQrCode("WIFI:S:anotherone;T:ADB;P:3#=3j9asicla").getWifiNetworkConfig();
        assertThat(config.getSsid()).isEqualTo("anotherone");
        assertThat(config.getSecurity()).isEqualTo("ADB");
        assertThat(config.getPreSharedKey()).isEqualTo("3#=3j9asicla");

        config = new AdbQrCode("WIFI:S:xx;T:ADB;P:a").getWifiNetworkConfig();
        assertThat(config.getSsid()).isEqualTo("xx");
        assertThat(config.getSecurity()).isEqualTo("ADB");
        assertThat(config.getPreSharedKey()).isEqualTo("a");
    }

    @Test
    public void testZxParsing_invalidCodeButShouldWork() {
        WifiNetworkConfig config = new AdbQrCode(
                "WIFI:S:reallyLONGone;T:ADB; P:somepassword").getWifiNetworkConfig();
        assertThat(config.getSsid()).isEqualTo("reallyLONGone");
        assertThat(config.getSecurity()).isEqualTo("ADB");
        assertThat(config.getPreSharedKey()).isEqualTo("somepassword");

        config = new AdbQrCode("WIFI: S:anotherone;T:ADB;P:abcdefghihklmn").getWifiNetworkConfig();
        assertThat(config.getSsid()).isEqualTo("anotherone");
        assertThat(config.getSecurity()).isEqualTo("ADB");
        assertThat(config.getPreSharedKey()).isEqualTo("abcdefghihklmn");

        config = new AdbQrCode("WIFI: S:xx; T:ADB;   P:a").getWifiNetworkConfig();
        assertThat(config.getSsid()).isEqualTo("xx");
        assertThat(config.getSecurity()).isEqualTo("ADB");
        assertThat(config.getPreSharedKey()).isEqualTo("a");
    }
}

