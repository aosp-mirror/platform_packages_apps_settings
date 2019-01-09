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
import android.content.pm.ActivityInfo;

import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WifiDppConfiguratorActivityTest {
    // Valid Wi-Fi DPP QR code & it's parameters
    private static final String VALID_WIFI_DPP_QR_CODE = "DPP:I:SN=4774LH2b4044;M:010203040506;K:"
            + "MDkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDIgADURzxmttZoIRIPWGoQMV00XHWCAQIhXruVWOz0NjlkIA=;;";

    @Rule
    public final ActivityTestRule<WifiDppConfiguratorActivity> mActivityRule =
            new ActivityTestRule<>(WifiDppConfiguratorActivity.class);

    @Test
    public void launchActivity_qrCodeScanner_shouldNotAutoFinish() {
        Intent intent = new Intent(WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_SCANNER);
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SECURITY, "WEP");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SSID, "GoogleGuest");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_PRE_SHARED_KEY, "password");

        mActivityRule.launchActivity(intent);

        assertThat(mActivityRule.getActivity().isFinishing()).isEqualTo(false);
    }

    @Test
    public void launchActivity_qrCodeGenerator_shouldNotAutoFinish() {
        Intent intent = new Intent(
                WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_GENERATOR);
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SECURITY, "WEP");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SSID, "GoogleGuest");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_PRE_SHARED_KEY, "password");

        mActivityRule.launchActivity(intent);

        assertThat(mActivityRule.getActivity().isFinishing()).isEqualTo(false);
    }

    @Test
    public void launchActivity_chooseSavedWifiNetwork_shouldNotAutoFinish() {
        Intent intent = new Intent(
                WifiDppConfiguratorActivity.ACTION_PROCESS_WIFI_DPP_QR_CODE);
        intent.putExtra(WifiDppUtils.EXTRA_QR_CODE, VALID_WIFI_DPP_QR_CODE);

        mActivityRule.launchActivity(intent);

        assertThat(mActivityRule.getActivity().isFinishing()).isEqualTo(false);
    }

    @Test
    public void testActivity_shouldImplementsWifiNetworkConfigRetriever() {
        WifiDppConfiguratorActivity activity = mActivityRule.getActivity();

        assertThat(activity instanceof WifiNetworkConfig.Retriever).isEqualTo(true);
    }

    @Test
    public void testActivity_shouldImplementsQrCodeGeneratorFragmentCallback() {
        WifiDppConfiguratorActivity activity = mActivityRule.getActivity();

        assertThat(activity instanceof WifiDppQrCodeGeneratorFragment
                .OnQrCodeGeneratorFragmentAddButtonClickedListener).isEqualTo(true);
    }

    @Test
    public void testActivity_shouldImplementsOnScanWifiDppSuccessCallback() {
        WifiDppConfiguratorActivity activity = mActivityRule.getActivity();

        assertThat(activity instanceof WifiDppQrCodeScannerFragment
                .OnScanWifiDppSuccessListener).isEqualTo(true);
    }

    @Test
    public void testActivity_shouldImplementsOnScanZxingWifiFormatSuccessCallback() {
        WifiDppConfiguratorActivity activity = mActivityRule.getActivity();

        assertThat(activity instanceof WifiDppQrCodeScannerFragment
                .OnScanZxingWifiFormatSuccessListener).isEqualTo(true);
    }

    @Test
    public void testActivity_shouldImplementsOnClickChooseDifferentNetworkCallback() {
        WifiDppConfiguratorActivity activity = mActivityRule.getActivity();

        assertThat(activity instanceof WifiDppAddDeviceFragment
                .OnClickChooseDifferentNetworkListener).isEqualTo(true);
    }

    @Test
    public void rotateScreen_shouldGetCorrectWifiDppQrCode() {
        WifiQrCode wifiQrCode = new WifiQrCode(VALID_WIFI_DPP_QR_CODE);
        Intent intent = new Intent(WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_SCANNER);
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SECURITY, "WEP");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SSID, "GoogleGuest");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_PRE_SHARED_KEY, "password");

        // setWifiDppQrCode and check if getWifiDppQrCode correctly after rotation
        mActivityRule.launchActivity(intent);
        mActivityRule.getActivity().setWifiDppQrCode(wifiQrCode);
        mActivityRule.getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mActivityRule.getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        WifiQrCode restoredWifiDppQrCode = mActivityRule.getActivity().getWifiDppQrCode();

        assertThat(restoredWifiDppQrCode).isNotNull();
        assertThat(restoredWifiDppQrCode.getQrCode()).isEqualTo(VALID_WIFI_DPP_QR_CODE);
    }

    @Test
    public void rotateScreen_shouldGetCorrectWifiNetworkConfig() {
        WifiNetworkConfig wifiNetworkConfig = new WifiNetworkConfig("WPA", "WifiSsid", "password",
                /* hiddenSsid */ false, /* networkId */ 0);
        Intent intent = new Intent(
                WifiDppConfiguratorActivity.ACTION_PROCESS_WIFI_DPP_QR_CODE);
        intent.putExtra(WifiDppUtils.EXTRA_QR_CODE, VALID_WIFI_DPP_QR_CODE);

        // setWifiNetworkConfig and check if getWifiNetworkConfig correctly after rotation
        mActivityRule.launchActivity(intent);
        mActivityRule.getActivity().setWifiNetworkConfig(wifiNetworkConfig);
        mActivityRule.getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mActivityRule.getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        WifiNetworkConfig restoredWifiNetworkConfig =
                mActivityRule.getActivity().getWifiNetworkConfig();

        assertThat(restoredWifiNetworkConfig).isNotNull();
        assertThat(restoredWifiNetworkConfig.getSecurity()).isEqualTo("WPA");
        assertThat(restoredWifiNetworkConfig.getSsid()).isEqualTo("WifiSsid");
        assertThat(restoredWifiNetworkConfig.getPreSharedKey()).isEqualTo("password");
        assertThat(restoredWifiNetworkConfig.getHiddenSsid()).isFalse();
        assertThat(restoredWifiNetworkConfig.getNetworkId()).isEqualTo(0);
    }
}
