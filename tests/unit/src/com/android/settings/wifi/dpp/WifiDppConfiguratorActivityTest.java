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

import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.test.uiautomator.UiDevice;

import androidx.fragment.app.FragmentManager;
import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.google.android.setupdesign.GlifLayout;

import org.junit.Before;
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

    private UiDevice mDevice;

    @Before
    public void setUp() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    @Test
    public void launchActivity_qrCodeScanner_shouldNotAutoFinish() {
        Intent intent = new Intent(WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_SCANNER);
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SECURITY, "WEP");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SSID, "GoogleGuest");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_PRE_SHARED_KEY, "password");

        mActivityRule.launchActivity(intent);
        FragmentManager fragmentManager = mActivityRule.getActivity().getSupportFragmentManager();
        WifiDppQrCodeScannerFragment fragment =
                (WifiDppQrCodeScannerFragment) fragmentManager.findFragmentByTag(
                        WifiDppUtils.TAG_FRAGMENT_QR_CODE_SCANNER);

        assertThat(fragment.getView() instanceof GlifLayout).isTrue();
        assertThat(mActivityRule.getActivity().isFinishing()).isFalse();
    }

    @Test
    public void launchActivity_qrCodeGenerator_shouldNotAutoFinish() {
        Intent intent = new Intent(
                WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_GENERATOR);
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SECURITY, "WEP");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SSID, "GoogleGuest");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_PRE_SHARED_KEY, "password");

        mActivityRule.launchActivity(intent);
        FragmentManager fragmentManager = mActivityRule.getActivity().getSupportFragmentManager();
        WifiDppQrCodeGeneratorFragment fragment =
                (WifiDppQrCodeGeneratorFragment) fragmentManager.findFragmentByTag(
                        WifiDppUtils.TAG_FRAGMENT_QR_CODE_GENERATOR);

        assertThat(fragment.getView() instanceof GlifLayout).isTrue();
        assertThat(mActivityRule.getActivity().isFinishing()).isFalse();
    }

    @Test
    public void launchActivity_chooseSavedWifiNetwork_shouldNotAutoFinish() {
        final Intent intent = new Intent(Settings.ACTION_PROCESS_WIFI_EASY_CONNECT_URI);
        intent.setData(Uri.parse(VALID_WIFI_DPP_QR_CODE));

        mActivityRule.launchActivity(intent);

        assertThat(mActivityRule.getActivity().isFinishing()).isFalse();
    }

    @Test
    public void testActivity_shouldImplementsWifiNetworkConfigRetriever() {
        WifiDppConfiguratorActivity activity = mActivityRule.getActivity();

        assertThat(activity instanceof WifiNetworkConfig.Retriever).isTrue();
    }

    @Test
    public void testActivity_shouldImplementsOnScanWifiDppSuccessCallback() {
        WifiDppConfiguratorActivity activity = mActivityRule.getActivity();

        assertThat(activity instanceof WifiDppQrCodeScannerFragment
                .OnScanWifiDppSuccessListener).isTrue();
    }

    @Test
    public void testActivity_shouldImplementsOnClickChooseDifferentNetworkCallback() {
        WifiDppConfiguratorActivity activity = mActivityRule.getActivity();

        assertThat(activity instanceof WifiDppAddDeviceFragment
                .OnClickChooseDifferentNetworkListener).isTrue();
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

        try {
            mDevice.setOrientationLeft();
            mDevice.setOrientationNatural();
            mDevice.setOrientationRight();
            mDevice.setOrientationNatural();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        WifiQrCode restoredWifiDppQrCode = mActivityRule.getActivity().getWifiDppQrCode();
        assertThat(restoredWifiDppQrCode).isNotNull();
        assertThat(restoredWifiDppQrCode.getQrCode()).isEqualTo(VALID_WIFI_DPP_QR_CODE);
    }

    @Test
    public void rotateScreen_shouldGetCorrectWifiNetworkConfig() {
        final WifiNetworkConfig wifiNetworkConfig = new WifiNetworkConfig("WPA", "WifiSsid",
                "password", /* hiddenSsid */ false, /* networkId */ 0, /* isHotspot */ true);
        final Intent intent = new Intent(Settings.ACTION_PROCESS_WIFI_EASY_CONNECT_URI);
        intent.setData(Uri.parse(VALID_WIFI_DPP_QR_CODE));

        // setWifiNetworkConfig and check if getWifiNetworkConfig correctly after rotation
        mActivityRule.launchActivity(intent);
        mActivityRule.getActivity().setWifiNetworkConfig(wifiNetworkConfig);

        try {
            mDevice.setOrientationLeft();
            mDevice.setOrientationNatural();
            mDevice.setOrientationRight();
            mDevice.setOrientationNatural();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        WifiNetworkConfig restoredWifiNetworkConfig =
                mActivityRule.getActivity().getWifiNetworkConfig();

        assertThat(restoredWifiNetworkConfig).isNotNull();
        assertThat(restoredWifiNetworkConfig.getSecurity()).isEqualTo("WPA");
        assertThat(restoredWifiNetworkConfig.getSsid()).isEqualTo("WifiSsid");
        assertThat(restoredWifiNetworkConfig.getPreSharedKey()).isEqualTo("password");
        assertThat(restoredWifiNetworkConfig.getHiddenSsid()).isFalse();
        assertThat(restoredWifiNetworkConfig.getNetworkId()).isEqualTo(0);
        assertThat(restoredWifiNetworkConfig.isHotspot()).isTrue();
    }
}
