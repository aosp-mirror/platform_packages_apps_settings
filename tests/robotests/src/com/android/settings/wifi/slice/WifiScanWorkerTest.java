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

import static com.android.settings.slices.CustomSliceRegistry.WIFI_SLICE_URI;
import static com.android.settings.wifi.WifiDialogActivity.KEY_ACCESS_POINT_STATE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.UserHandle;

import com.android.settings.slices.ShadowSliceBackgroundWorker;
import com.android.settings.testutils.shadow.ShadowWifiManager;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.WifiTracker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowNetworkInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowSliceBackgroundWorker.class, ShadowWifiManager.class,
        WifiScanWorkerTest.ShadowWifiTracker.class})
public class WifiScanWorkerTest {

    private Context mContext;
    private ContentResolver mResolver;
    private WifiManager mWifiManager;
    private ConnectivityManager mConnectivityManager;
    private WifiScanWorker mWifiScanWorker;
    private ConnectToWifiHandler mConnectToWifiHandler;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mResolver = mock(ContentResolver.class);
        doReturn(mResolver).when(mContext).getContentResolver();
        mWifiManager = mContext.getSystemService(WifiManager.class);
        mWifiManager.setWifiEnabled(true);

        mConnectivityManager = mContext.getSystemService(ConnectivityManager.class);
        mWifiScanWorker = new WifiScanWorker(mContext, WIFI_SLICE_URI);
        mConnectToWifiHandler = new ConnectToWifiHandler();
    }

    @After
    public void tearDown() {
        mWifiScanWorker.clearClickedWifi();
    }

    @Test
    public void onWifiStateChanged_shouldNotifyChange() {
        mWifiScanWorker.onWifiStateChanged(WifiManager.WIFI_STATE_DISABLED);

        verify(mResolver).notifyChange(WIFI_SLICE_URI, null);
    }

    @Test
    public void AccessPointList_sameState_shouldBeTheSame() {
        final AccessPoint ap1 = createAccessPoint(DetailedState.CONNECTED);
        final AccessPoint ap2 = createAccessPoint(DetailedState.CONNECTED);

        assertThat(mWifiScanWorker.areListsTheSame(Arrays.asList(ap1), Arrays.asList(ap2)))
                .isTrue();
    }

    @Test
    public void AccessPointList_differentState_shouldBeDifferent() {
        final AccessPoint ap1 = createAccessPoint(DetailedState.CONNECTING);
        final AccessPoint ap2 = createAccessPoint(DetailedState.CONNECTED);

        assertThat(mWifiScanWorker.areListsTheSame(Arrays.asList(ap1), Arrays.asList(ap2)))
                .isFalse();
    }

    @Test
    public void AccessPointList_differentListLength_shouldBeDifferent() {
        final AccessPoint ap1 = createAccessPoint(DetailedState.CONNECTED);
        final AccessPoint ap2 = createAccessPoint(DetailedState.CONNECTED);
        final List<AccessPoint> list = new ArrayList<>();
        list.add(ap1);
        list.add(ap2);

        assertThat(mWifiScanWorker.areListsTheSame(list, Arrays.asList(ap1))).isFalse();
    }

    @Test
    public void NetworkCallback_onCapabilitiesChanged_shouldNotifyChange() {
        final Network network = mConnectivityManager.getActiveNetwork();
        mWifiScanWorker.registerNetworkCallback(network);

        mWifiScanWorker.mNetworkCallback.onCapabilitiesChanged(network,
                WifiSliceTest.makeCaptivePortalNetworkCapabilities());

        verify(mResolver).notifyChange(WIFI_SLICE_URI, null);
    }

    @Test
    public void NetworkCallback_onCapabilitiesChanged_isClickedWifi_shouldSendBroadcast() {
        final Intent intent = getIntentWithAccessPoint("ap1");
        setConnectionInfoSSID("ap1");
        final Network network = mConnectivityManager.getActiveNetwork();
        mWifiScanWorker.registerNetworkCallback(network);

        mConnectToWifiHandler.onReceive(mContext, intent);
        mWifiScanWorker.mNetworkCallback.onCapabilitiesChanged(network,
                WifiSliceTest.makeCaptivePortalNetworkCapabilities());

        verify(mContext).sendBroadcastAsUser(any(Intent.class), eq(UserHandle.CURRENT));
    }

    @Test
    public void NetworkCallback_onCapabilitiesChanged_isNotClickedWifi_shouldNotSendBroadcast() {
        final Intent intent = getIntentWithAccessPoint("ap1");
        setConnectionInfoSSID("ap2");
        final Network network = mConnectivityManager.getActiveNetwork();
        mWifiScanWorker.registerNetworkCallback(network);

        mConnectToWifiHandler.onReceive(mContext, intent);
        mWifiScanWorker.mNetworkCallback.onCapabilitiesChanged(network,
                WifiSliceTest.makeCaptivePortalNetworkCapabilities());

        verify(mContext, never()).sendBroadcastAsUser(any(Intent.class), eq(UserHandle.CURRENT));
    }

    @Test
    public void NetworkCallback_onCapabilitiesChanged_neverClickWifi_shouldNotSendBroadcast() {
        setConnectionInfoSSID("ap1");
        final Network network = mConnectivityManager.getActiveNetwork();
        mWifiScanWorker.registerNetworkCallback(network);

        mWifiScanWorker.mNetworkCallback.onCapabilitiesChanged(network,
                WifiSliceTest.makeCaptivePortalNetworkCapabilities());

        verify(mContext, never()).sendBroadcastAsUser(any(Intent.class), eq(UserHandle.CURRENT));
    }

    @Test
    public void NetworkCallback_onCapabilitiesChanged_sliceIsUnpinned_shouldNotSendBroadcast() {
        final Intent intent = getIntentWithAccessPoint("ap1");
        setConnectionInfoSSID("ap1");
        final Network network = mConnectivityManager.getActiveNetwork();
        mWifiScanWorker.registerNetworkCallback(network);
        final NetworkCallback callback = mWifiScanWorker.mNetworkCallback;

        mWifiScanWorker.onSlicePinned();
        mConnectToWifiHandler.onReceive(mContext, intent);
        mWifiScanWorker.onSliceUnpinned();
        callback.onCapabilitiesChanged(network,
                WifiSliceTest.makeCaptivePortalNetworkCapabilities());

        verify(mContext, never()).sendBroadcastAsUser(any(Intent.class), eq(UserHandle.CURRENT));
    }

    static Intent getIntentWithAccessPoint(String ssid) {
        final Bundle savedState = new Bundle();
        savedState.putString("key_ssid", ssid);
        return new Intent().putExtra(KEY_ACCESS_POINT_STATE, savedState);
    }

    static void setConnectionInfoSSID(String ssid) {
        final WifiInfo wifiInfo = mock(WifiInfo.class);
        when(wifiInfo.getSSID()).thenReturn(ssid);
        ShadowWifiManager.get().setConnectionInfo(wifiInfo);
    }

    private AccessPoint createAccessPoint(String ssid, DetailedState detailedState) {
        final NetworkInfo info = ShadowNetworkInfo.newInstance(detailedState, 1 /* type */,
                0 /*subType */, true /* isAvailable */, true /* isConnected */);
        final Bundle savedState = new Bundle();
        savedState.putString("key_ssid", ssid);
        savedState.putParcelable("key_networkinfo", info);
        return new AccessPoint(mContext, savedState);
    }

    private AccessPoint createAccessPoint(DetailedState detailedState) {
        return createAccessPoint("ap", detailedState);
    }

    @Implements(WifiTracker.class)
    public static class ShadowWifiTracker {
        @Implementation
        public void onStart() {
            // do nothing
        }
    }
}
