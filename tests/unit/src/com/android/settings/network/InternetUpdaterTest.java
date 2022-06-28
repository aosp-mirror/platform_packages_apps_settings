/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.network;

import static android.net.NetworkCapabilities.TRANSPORT_CELLULAR;
import static android.net.NetworkCapabilities.TRANSPORT_ETHERNET;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;

import static com.android.settings.network.InternetUpdater.INTERNET_CELLULAR;
import static com.android.settings.network.InternetUpdater.INTERNET_ETHERNET;
import static com.android.settings.network.InternetUpdater.INTERNET_NETWORKS_AVAILABLE;
import static com.android.settings.network.InternetUpdater.INTERNET_OFF;
import static com.android.settings.network.InternetUpdater.INTERNET_WIFI;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.AirplaneModeEnabler;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class InternetUpdaterTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private AirplaneModeEnabler mAirplaneModeEnabler;

    private Context mContext;
    private InternetUpdater mInternetUpdater;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        doReturn(mConnectivityManager).when(mContext).getSystemService(
                Context.CONNECTIVITY_SERVICE);
        doReturn(mWifiManager).when(mContext).getSystemService(Context.WIFI_SERVICE);
        final Lifecycle lifecycle = mock(Lifecycle.class);

        mInternetUpdater = new InternetUpdater(mContext, lifecycle, null);
        mInternetUpdater.mAirplaneModeEnabler = mAirplaneModeEnabler;
    }

    @Test
    public void onResume_shouldRegisterCallback() {
        mInternetUpdater.onResume();

        verify(mAirplaneModeEnabler).start();
        verify(mContext).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class),
                any(int.class));
        verify(mConnectivityManager).registerDefaultNetworkCallback(
                any(ConnectivityManager.NetworkCallback.class));
        // Unregister callbacks
        mInternetUpdater.onPause();
    }

    @Test
    public void onPause_shouldUnregisterCallback() {
        // Register callbacks first for testing
        mInternetUpdater.onResume();

        mInternetUpdater.onPause();

        verify(mAirplaneModeEnabler).stop();
        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
        verify(mConnectivityManager).unregisterNetworkCallback(
                any(ConnectivityManager.NetworkCallback.class));
    }

    @Test
    public void onDestroy_shouldCloseCallback() {
        mInternetUpdater.onDestroy();

        verify(mAirplaneModeEnabler).close();
    }

    @Test
    public void updateInternetAvailable_wifiConnectedAndNoValidated_internetUnavailable() {
        final NetworkCapabilities networkCapabilities = new NetworkCapabilities.Builder()
                .addTransportType(TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        mInternetUpdater.updateInternetAvailable(networkCapabilities);

        assertThat(mInternetUpdater.mInternetAvailable).isEqualTo(false);
    }

    @Test
    public void updateInternetAvailable_wifiConnectedAndValidated_internetAvailable() {
        final NetworkCapabilities networkCapabilities = new NetworkCapabilities.Builder()
                .addTransportType(TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build();

        mInternetUpdater.updateInternetAvailable(networkCapabilities);

        assertThat(mInternetUpdater.mInternetAvailable).isEqualTo(true);
    }

    @Test
    public void updateInternetType_apmOnWifiOff_getInternetApm() {
        doReturn(true).when(mAirplaneModeEnabler).isAirplaneModeOn();
        doReturn(WifiManager.WIFI_STATE_DISABLED).when(mWifiManager).getWifiState();
        mInternetUpdater.mInternetAvailable = false;

        mInternetUpdater.updateInternetType();

        assertThat(mInternetUpdater.getInternetType()).isEqualTo(INTERNET_OFF);
    }

    @Test
    public void updateInternetType_apmOnWifiOnNotConnected_getInternetNetworksAvailable() {
        doReturn(true).when(mAirplaneModeEnabler).isAirplaneModeOn();
        doReturn(WifiManager.WIFI_STATE_ENABLED).when(mWifiManager).getWifiState();
        mInternetUpdater.mInternetAvailable = false;

        mInternetUpdater.updateInternetType();

        assertThat(mInternetUpdater.getInternetType()).isEqualTo(INTERNET_NETWORKS_AVAILABLE);
    }

    @Test
    public void updateInternetType_apmOnWifiConnected_getInternetWifi() {
        doReturn(true).when(mAirplaneModeEnabler).isAirplaneModeOn();
        doReturn(true).when(mWifiManager).isWifiEnabled();
        mInternetUpdater.mInternetAvailable = true;
        mInternetUpdater.mTransport = TRANSPORT_WIFI;

        mInternetUpdater.updateInternetType();

        assertThat(mInternetUpdater.getInternetType()).isEqualTo(INTERNET_WIFI);
    }

    @Test
    public void updateInternetType_apmOnCellularConnected_getInternetCellular() {
        doReturn(true).when(mAirplaneModeEnabler).isAirplaneModeOn();
        mInternetUpdater.mInternetAvailable = true;
        mInternetUpdater.mTransport = TRANSPORT_CELLULAR;

        mInternetUpdater.updateInternetType();

        assertThat(mInternetUpdater.getInternetType()).isEqualTo(INTERNET_CELLULAR);
    }

    @Test
    public void updateInternetType_apmOnEthernetConnected_getInternetEthernet() {
        doReturn(true).when(mAirplaneModeEnabler).isAirplaneModeOn();
        mInternetUpdater.mInternetAvailable = true;
        mInternetUpdater.mTransport = TRANSPORT_ETHERNET;

        mInternetUpdater.updateInternetType();

        assertThat(mInternetUpdater.getInternetType()).isEqualTo(INTERNET_ETHERNET);
    }

    @Test
    public void updateInternetType_carrierWifiConnected_getInternetCellular() {
        final WifiInfo wifiInfo = mock(WifiInfo.class);
        doReturn(wifiInfo).when(mWifiManager).getConnectionInfo();
        doReturn(true).when(wifiInfo).isCarrierMerged();
        mInternetUpdater.mInternetAvailable = true;
        mInternetUpdater.mTransport = TRANSPORT_WIFI;

        mInternetUpdater.updateInternetType();

        assertThat(mInternetUpdater.getInternetType()).isEqualTo(INTERNET_CELLULAR);
    }
}
