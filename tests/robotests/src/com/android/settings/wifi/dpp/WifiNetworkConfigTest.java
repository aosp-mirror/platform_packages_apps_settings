/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class WifiNetworkConfigTest {
    private WifiNetworkConfig mWifiConfig;

    @Before
    public void setUp() {
        Intent intent = new Intent();
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SSID, "Pixel:_ABCD;");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SECURITY, "WPA");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_PRE_SHARED_KEY, "\\012345678,");
        mWifiConfig = WifiNetworkConfig.getValidConfigOrNull(intent);
    }

    @Test
    public void testInitConfig_IntentReceived_QRCodeValue() {
        String qrcode = mWifiConfig.getQrCode();
        assertThat(qrcode).isEqualTo("WIFI:S:Pixel\\:_ABCD\\;;T:WPA;P:\\\\012345678\\,;H:false;;");
    }
}