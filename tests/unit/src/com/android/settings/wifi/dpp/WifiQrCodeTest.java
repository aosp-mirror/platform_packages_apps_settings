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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class WifiQrCodeTest {
    // Valid Wi-Fi DPP QR code & it's parameters
    private static final String VALID_WIFI_DPP_QR_CODE = "DPP:I:SN=4774LH2b4044;M:010203040506;K:"
            + "MDkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDIgADURzxmttZoIRIPWGoQMV00XHWCAQIhXruVWOz0NjlkIA=;;";

    private static final String PUBLIC_KEY_OF_VALID_WIFI_DPP_QR_CODE =
            "MDkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDIgADURzxmttZoIRIPWGoQMV00XHWCAQIhXruVWOz0NjlkIA=";

    private static final String INFORMATION_OF_VALID_WIFI_DPP_QR_CODE =
            "SN=4774LH2b4044";

    // Valid ZXing reader library's Wi-Fi Network config format & it's parameters
    private static final String VALID_ZXING_WIFI_QR_CODE_WPA =
            "WIFI:T:WPA;S:mynetwork;P:mypass;H:true;;";

     // Valid ZXing reader library's Wi-Fi Network config format - security type SAE
    private static final String VALID_ZXING_WIFI_QR_CODE_SAE =
            "WIFI:T:SAE;S:mynetwork;P:mypass;H:true;;";

    // Valid ZXing reader library's Wi-Fi Network config format - security type nopass and no password
    private static final String VALID_ZXING_WIFI_QR_CODE_NOPASS_AND_NO_PASSWORD =
            "WIFI:T:nopass;S:mynetwork;;";

    // Valid ZXing reader library's Wi-Fi Network config format - no security and no password
    private static final String VALID_ZXING_WIFI_QR_CODE_NO_SECURITY_AND_NO_PASSWORD =
            "WIFI:T:;S:mynetwork;P:;H:false;;";

    private static final String SECURITY_OF_VALID_ZXING_WIFI_QR_CODE_WPA = "WPA";
    private static final String SECURITY_OF_VALID_ZXING_WIFI_QR_CODE_SAE = "SAE";
    private static final String SECURITY_OF_VALID_ZXING_WIFI_QR_CODE_NOPASS = "nopass";
    private static final String SSID_OF_VALID_ZXING_WIFI_QR_CODE = "mynetwork";
    private static final String PASSWORD_OF_VALID_ZXING_WIFI_QR_CODE = "mypass";

    // Valid ZXing reader library's Wi-Fi Network config format - escaped characters
    private static final String VALID_ZXING_WIFI_QR_CODE_SPECIAL_CHARACTERS =
            "WIFI:T:WPA;S:mynetwork;P:m\\;y\\:p\\\\a\\,ss;H:true;;";

    private static final String PASSWORD_OF_VALID_ZXING_WIFI_QR_CODE_SPECIAL_CHARACTERS =
            "m;y:p\\a,ss";

    // Invalid scheme QR code
    private static final String INVALID_SCHEME_QR_CODE = "BT:T:WPA;S:mynetwork;P:mypass;H:true;;";

    // Invalid Wi-Fi DPP QR code - no public key - case 1
    private static final String INVALID_WIFI_DPP_QR_CODE_NO_PUBLIC_KEY_1 =
            "DPP:I:SN=4774LH2b4044;M:010203040506;K:;;";

    // Invalid Wi-Fi DPP QR code - no public key - case 2
    private static final String INVALID_WIFI_DPP_QR_CODE_NO_PUBLIC_KEY_2 =
            "DPP:I:SN=4774LH2b4044;M:010203040506;;";

    // Invalid ZXing reader library's Wi-Fi Network config format - no password
    private static final String INVALID_ZXING_WIFI_QR_CODE_NO_PASSWORD =
            "WIFI:T:WPA;S:mynetwork;P:;;";

    // Invalid ZXing reader library's Wi-Fi Network config format - no SSID
    private static final String INVALID_ZXING_WIFI_QR_CODE_NO_SSID =
            "WIFI:T:WPA;P:mypass;;";

    @Test
    public void parseValidWifiDppQrCode() {
        WifiQrCode wifiQrCode = new WifiQrCode(VALID_WIFI_DPP_QR_CODE);

        assertEquals(WifiQrCode.SCHEME_DPP, wifiQrCode.getScheme());
        assertEquals(PUBLIC_KEY_OF_VALID_WIFI_DPP_QR_CODE, wifiQrCode.getPublicKey());
        assertEquals(INFORMATION_OF_VALID_WIFI_DPP_QR_CODE, wifiQrCode.getInformation());
    }

    @Test
    public void parseValidZxingWifiQrCode() {
        WifiQrCode wifiQrCode = new WifiQrCode(VALID_ZXING_WIFI_QR_CODE_WPA);
        WifiNetworkConfig config = wifiQrCode.getWifiNetworkConfig();

        assertEquals(WifiQrCode.SCHEME_ZXING_WIFI_NETWORK_CONFIG, wifiQrCode.getScheme());
        assertNotNull(config);
        assertEquals(SECURITY_OF_VALID_ZXING_WIFI_QR_CODE_WPA, config.getSecurity());
        assertEquals(SSID_OF_VALID_ZXING_WIFI_QR_CODE, config.getSsid());
        assertEquals(PASSWORD_OF_VALID_ZXING_WIFI_QR_CODE, config.getPreSharedKey());
        assertEquals(true, config.getHiddenSsid());
    }

    @Test
    public void parseValidZxingWifiQrCodeSae() {
        WifiQrCode wifiQrCode = new WifiQrCode(VALID_ZXING_WIFI_QR_CODE_SAE);
        WifiNetworkConfig config = wifiQrCode.getWifiNetworkConfig();

        assertEquals(WifiQrCode.SCHEME_ZXING_WIFI_NETWORK_CONFIG, wifiQrCode.getScheme());
        assertNotNull(config);
        assertEquals(SECURITY_OF_VALID_ZXING_WIFI_QR_CODE_SAE, config.getSecurity());
        assertEquals(SSID_OF_VALID_ZXING_WIFI_QR_CODE, config.getSsid());
        assertEquals(PASSWORD_OF_VALID_ZXING_WIFI_QR_CODE, config.getPreSharedKey());
        assertEquals(true, config.getHiddenSsid());
    }

    @Test
    public void parseValidZxingWifiQrCode_noPass_and_no_password() {
        WifiQrCode wifiQrCode = new WifiQrCode(VALID_ZXING_WIFI_QR_CODE_NOPASS_AND_NO_PASSWORD);
        WifiNetworkConfig config = wifiQrCode.getWifiNetworkConfig();

        assertEquals(WifiQrCode.SCHEME_ZXING_WIFI_NETWORK_CONFIG, wifiQrCode.getScheme());
        assertNotNull(config);
        assertEquals(SECURITY_OF_VALID_ZXING_WIFI_QR_CODE_NOPASS, config.getSecurity());
        assertEquals(SSID_OF_VALID_ZXING_WIFI_QR_CODE, config.getSsid());
        assertNull(config.getPreSharedKey());
        assertEquals(false, config.getHiddenSsid());
    }

    @Test
    public void parseValidZxingWifiQrCode_no_security_and_no_password() {
        WifiQrCode wifiQrCode = new WifiQrCode(VALID_ZXING_WIFI_QR_CODE_NO_SECURITY_AND_NO_PASSWORD);
        WifiNetworkConfig config = wifiQrCode.getWifiNetworkConfig();

        assertEquals(WifiQrCode.SCHEME_ZXING_WIFI_NETWORK_CONFIG, wifiQrCode.getScheme());
        assertNotNull(config);
        assertEquals("", config.getSecurity());
        assertEquals(SSID_OF_VALID_ZXING_WIFI_QR_CODE, config.getSsid());
        assertEquals("", config.getPreSharedKey());
        assertEquals(false, config.getHiddenSsid());
    }

    @Test
    public void parseValidZxingWifiQrCode_specialCharacters() {
        WifiQrCode wifiQrCode = new WifiQrCode(VALID_ZXING_WIFI_QR_CODE_SPECIAL_CHARACTERS);
        WifiNetworkConfig config = wifiQrCode.getWifiNetworkConfig();

        assertEquals(WifiQrCode.SCHEME_ZXING_WIFI_NETWORK_CONFIG, wifiQrCode.getScheme());
        assertNotNull(config);
        assertEquals(SECURITY_OF_VALID_ZXING_WIFI_QR_CODE_WPA, config.getSecurity());
        assertEquals(SSID_OF_VALID_ZXING_WIFI_QR_CODE, config.getSsid());
        assertEquals(PASSWORD_OF_VALID_ZXING_WIFI_QR_CODE_SPECIAL_CHARACTERS,
                config.getPreSharedKey());
        assertEquals(true, config.getHiddenSsid());
    }

    @Test
    public void testRemoveBackSlash() {
        WifiQrCode wifiQrCode = new WifiQrCode(VALID_WIFI_DPP_QR_CODE);

        assertEquals("\\", wifiQrCode.removeBackSlash("\\\\"));
        assertEquals("ab", wifiQrCode.removeBackSlash("a\\b"));
        assertEquals("a", wifiQrCode.removeBackSlash("\\a"));
        assertEquals("\\b", wifiQrCode.removeBackSlash("\\\\b"));
        assertEquals("c\\", wifiQrCode.removeBackSlash("c\\\\"));
    }

    @Test
    public void parseEmptyQrCode_shouldThrowIllegalArgumentException() {
        try {
            new WifiQrCode(null);
            fail("Null QR code");
        } catch (IllegalArgumentException e) {
            // Do nothing
        }

        try {
            new WifiQrCode("");
            fail("Empty string QR code");
        } catch (IllegalArgumentException e) {
            // Do nothing
        }

        try {
            new WifiQrCode("DPP:;");
            fail("Empty content WIFI DPP QR code");
        } catch (IllegalArgumentException e) {
            // Do nothing
        }

        try {
            new WifiQrCode("WIFI:;");
            fail("Empty content ZXing WIFI QR code");
        } catch (IllegalArgumentException e) {
            // Do nothing
        }
    }

    @Test
    public void parseInvalidSchemeQrCode_shouldThrowIllegalArgumentException() {
        try {
            new WifiQrCode(INVALID_SCHEME_QR_CODE);
            fail("Invalid scheme");
        } catch (IllegalArgumentException e) {
            // Do nothing
        }
    }

    @Test
    public void parseInvalidWifiDppQrCode_noPublicKey_shouldThrowIllegalArgumentException() {
        try {
            new WifiQrCode(INVALID_WIFI_DPP_QR_CODE_NO_PUBLIC_KEY_1);
            fail("No public key case 1");
        } catch (IllegalArgumentException e) {
            // Do nothing
        }

        try {
            new WifiQrCode(INVALID_WIFI_DPP_QR_CODE_NO_PUBLIC_KEY_2);
            fail("No public key case 2");
        } catch (IllegalArgumentException e) {
            // Do nothing
        }
    }

    @Test
    public void parseInvalidZxingWifiQrCode_noPassword_shouldThrowIllegalArgumentException() {
        try {
            new WifiQrCode(INVALID_ZXING_WIFI_QR_CODE_NO_PASSWORD);
            fail("No password");
        } catch (IllegalArgumentException e) {
            // Do nothing
        }
    }

    @Test
    public void parseInvalidZxingWifiQrCode_noSsid_shouldThrowIllegalArgumentException() {
        try {
            new WifiQrCode(INVALID_ZXING_WIFI_QR_CODE_NO_SSID);
            fail("No SSID");
        } catch (IllegalArgumentException e) {
            // Do nothing
        }
    }
}
