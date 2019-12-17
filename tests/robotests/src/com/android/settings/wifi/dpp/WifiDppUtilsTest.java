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
 */

package com.android.settings.wifi.dpp;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiManager;

import com.android.settingslib.wifi.AccessPoint;

import java.util.BitSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class WifiDppUtilsTest {

    @Mock
    private WifiManager mWifiManager;

    @Mock
    private AccessPoint mAccessPoint;

    @Mock
    private WifiConfiguration mWifiConfiguration;

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.WIFI_SERVICE, mWifiManager);
    }

    @Test
    public void getConfiguratorQrCodeScannerIntentOrNull_hiddenSsidNetwork_hasHiddenSsidExtra() {
        when(mWifiManager.isEasyConnectSupported()).thenReturn(true);
        when(mAccessPoint.isPasspoint()).thenReturn(false);
        when(mAccessPoint.getSecurity()).thenReturn(AccessPoint.SECURITY_PSK);
        when(mAccessPoint.getConfig()).thenReturn(mWifiConfiguration);
        mWifiConfiguration.SSID = "GuestNetwork";
        mWifiConfiguration.allowedKeyManagement = new BitSet();
        mWifiConfiguration.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
        mWifiConfiguration.hiddenSSID = true;

        Intent intent = WifiDppUtils
                .getConfiguratorQrCodeScannerIntentOrNull(mContext, mWifiManager, mAccessPoint);

        assertThat(intent.getBooleanExtra(WifiDppUtils.EXTRA_WIFI_HIDDEN_SSID, false))
                .isEqualTo(true);
    }
}
