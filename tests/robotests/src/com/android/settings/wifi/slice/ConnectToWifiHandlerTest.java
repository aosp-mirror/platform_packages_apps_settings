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

package com.android.settings.wifi.slice;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.NetworkSelectionStatus;
import android.net.wifi.WifiManager;

import com.android.settings.testutils.shadow.ShadowWifiManager;
import com.android.settingslib.wifi.AccessPoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowWifiManager.class)
public class ConnectToWifiHandlerTest {

    private static final String AP_SSID = "\"ap\"";
    private Context mContext;
    private ConnectToWifiHandler mHandler;
    private WifiConfiguration mWifiConfig;
    @Mock
    private AccessPoint mAccessPoint;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mHandler = new ConnectToWifiHandler();
        mWifiConfig = spy(new WifiConfiguration());
        mWifiConfig.SSID = AP_SSID;
        doReturn(mWifiConfig).when(mAccessPoint).getConfig();
    }

    @Test
    public void connect_shouldConnectToUnsavedOpenNetwork() {
        when(mAccessPoint.isSaved()).thenReturn(false);
        when(mAccessPoint.getSecurity()).thenReturn(AccessPoint.SECURITY_NONE);

        mHandler.connect(mContext, mAccessPoint);

        assertThat(ShadowWifiManager.get().savedWifiConfig.SSID).isEqualTo(AP_SSID);
    }

    @Test
    public void connect_shouldStartOsuProvisioning() {
        when(mAccessPoint.isSaved()).thenReturn(false);
        when(mAccessPoint.isOsuProvider()).thenReturn(true);

        mHandler.connect(mContext, mAccessPoint);

        verify(mAccessPoint).startOsuProvisioning(any(WifiManager.ActionListener.class));
    }


    @Test
    public void connect_shouldConnectWithPasspointProvider() {
        when(mAccessPoint.isSaved()).thenReturn(false);
        when(mAccessPoint.isPasspoint()).thenReturn(true);

        mHandler.connect(mContext, mAccessPoint);

        assertThat(ShadowWifiManager.get().savedWifiConfig.SSID).isEqualTo(AP_SSID);
    }

    @Test
    public void connect_shouldConnectToSavedSecuredNetwork() {
        when(mAccessPoint.isSaved()).thenReturn(true);
        when(mAccessPoint.getSecurity()).thenReturn(AccessPoint.SECURITY_PSK);
        final NetworkSelectionStatus status = mock(NetworkSelectionStatus.class);
        when(status.hasEverConnected()).thenReturn(true);
        when(mWifiConfig.getNetworkSelectionStatus()).thenReturn(status);

        mHandler.connect(mContext, mAccessPoint);

        assertThat(ShadowWifiManager.get().savedWifiConfig.SSID).isEqualTo(AP_SSID);
    }

    @Test
    public void connect_shouldNotConnectToUnsavedSecuredNetwork() {
        when(mAccessPoint.isSaved()).thenReturn(false);
        when(mAccessPoint.getSecurity()).thenReturn(AccessPoint.SECURITY_PSK);

        mHandler.connect(mContext, mAccessPoint);

        assertThat(ShadowWifiManager.get().savedWifiConfig).isNull();
    }
}
