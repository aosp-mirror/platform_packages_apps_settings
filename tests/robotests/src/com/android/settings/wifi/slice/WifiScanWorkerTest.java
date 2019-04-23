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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiSsid;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.slice.SliceProvider;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.testutils.shadow.ShadowWifiManager;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.WifiTracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowWifiManager.class,
        WifiScanWorkerTest.ShadowWifiTracker.class,
})
public class WifiScanWorkerTest {

    private static final String AP_NAME = "ap";

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

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
        mWifiManager.setWifiEnabled(true);

        mConnectivityManager = mContext.getSystemService(ConnectivityManager.class);
        mWifiScanWorker = new WifiScanWorker(mContext, WIFI_SLICE_URI);
        mConnectToWifiHandler = Robolectric.setupActivity(ConnectToWifiHandler.class);
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

    private AccessPoint createAccessPoint(String name, State state) {
        final NetworkInfo info = mock(NetworkInfo.class);
        doReturn(state).when(info).getState();

        final Bundle savedState = new Bundle();
        savedState.putString("key_ssid", name);
        savedState.putParcelable("key_networkinfo", info);
        return new AccessPoint(mContext, savedState);
    }

    @Test
    public void AccessPointList_sameState_shouldBeTheSame() {
        final AccessPoint ap1 = createAccessPoint(AP_NAME, State.CONNECTED);
        final AccessPoint ap2 = createAccessPoint(AP_NAME, State.CONNECTED);

        assertThat(mWifiScanWorker.areListsTheSame(Arrays.asList(ap1), Arrays.asList(ap2)))
                .isTrue();
    }

    @Test
    public void AccessPointList_differentState_shouldBeDifferent() {
        final AccessPoint ap1 = createAccessPoint(AP_NAME, State.CONNECTING);
        final AccessPoint ap2 = createAccessPoint(AP_NAME, State.CONNECTED);

        assertThat(mWifiScanWorker.areListsTheSame(Arrays.asList(ap1), Arrays.asList(ap2)))
                .isFalse();
    }

    @Test
    public void AccessPointList_differentLength_shouldBeDifferent() {
        final AccessPoint ap1 = createAccessPoint(AP_NAME, State.CONNECTED);
        final AccessPoint ap2 = createAccessPoint(AP_NAME, State.CONNECTED);
        final List<AccessPoint> list = new ArrayList<>();
        list.add(ap1);
        list.add(ap2);

        assertThat(mWifiScanWorker.areListsTheSame(list, Arrays.asList(ap1))).isFalse();
    }

    @Test
    public void NetworkCallback_onCapabilitiesChanged_shouldNotifyChange() {
        final Network network = mConnectivityManager.getActiveNetwork();
        mWifiScanWorker.registerCaptivePortalNetworkCallback(network);

        mWifiScanWorker.mCaptivePortalNetworkCallback.onCapabilitiesChanged(network,
                WifiSliceTest.makeCaptivePortalNetworkCapabilities());

        verify(mResolver).notifyChange(WIFI_SLICE_URI, null);
    }

    private AccessPoint createAccessPoint(String ssid) {
        final AccessPoint accessPoint = mock(AccessPoint.class);
        doReturn(ssid).when(accessPoint).getSsidStr();
        return accessPoint;
    }

    private void setConnectionInfoSSID(String ssid) {
        final WifiInfo wifiInfo = new WifiInfo();
        wifiInfo.setSSID(WifiSsid.createFromAsciiEncoded(ssid));
        ShadowWifiManager.get().setConnectionInfo(wifiInfo);
    }

    @Test
    public void NetworkCallback_onCapabilitiesChanged_isClickedWifi_shouldStartActivity() {
        final AccessPoint accessPoint = createAccessPoint("ap1");
        setConnectionInfoSSID("ap1");
        final Network network = mConnectivityManager.getActiveNetwork();
        mWifiScanWorker.registerCaptivePortalNetworkCallback(network);

        mConnectToWifiHandler.connect(accessPoint);
        mWifiScanWorker.mCaptivePortalNetworkCallback.onCapabilitiesChanged(network,
                WifiSliceTest.makeCaptivePortalNetworkCapabilities());

        verify(mContext).startActivityAsUser(any(Intent.class), eq(UserHandle.CURRENT));
    }

    @Test
    public void NetworkCallback_onCapabilitiesChanged_isNotClickedWifi_shouldNotStartActivity() {
        final AccessPoint accessPoint = createAccessPoint("ap1");
        setConnectionInfoSSID("ap2");
        final Network network = mConnectivityManager.getActiveNetwork();
        mWifiScanWorker.registerCaptivePortalNetworkCallback(network);

        mConnectToWifiHandler.connect(accessPoint);
        mWifiScanWorker.mCaptivePortalNetworkCallback.onCapabilitiesChanged(network,
                WifiSliceTest.makeCaptivePortalNetworkCapabilities());

        verify(mContext, never()).startActivityAsUser(any(Intent.class), eq(UserHandle.CURRENT));
    }

    @Test
    public void NetworkCallback_onCapabilitiesChanged_neverClickWifi_shouldNotStartActivity() {
        setConnectionInfoSSID("ap1");
        final Network network = mConnectivityManager.getActiveNetwork();
        mWifiScanWorker.registerCaptivePortalNetworkCallback(network);

        mWifiScanWorker.mCaptivePortalNetworkCallback.onCapabilitiesChanged(network,
                WifiSliceTest.makeCaptivePortalNetworkCapabilities());

        verify(mContext, never()).startActivityAsUser(any(Intent.class), eq(UserHandle.CURRENT));
    }

    @Test
    public void NetworkCallback_onCapabilitiesChanged_sliceIsUnpinned_shouldNotStartActivity() {
        final AccessPoint accessPoint = createAccessPoint("ap1");
        setConnectionInfoSSID("ap1");
        final Network network = mConnectivityManager.getActiveNetwork();
        mWifiScanWorker.registerCaptivePortalNetworkCallback(network);
        final WifiScanWorker.CaptivePortalNetworkCallback callback =
                mWifiScanWorker.mCaptivePortalNetworkCallback;

        mWifiScanWorker.onSlicePinned();
        mConnectToWifiHandler.connect(accessPoint);
        mWifiScanWorker.onSliceUnpinned();
        callback.onCapabilitiesChanged(network,
                WifiSliceTest.makeCaptivePortalNetworkCapabilities());

        verify(mContext, never()).startActivityAsUser(any(Intent.class), eq(UserHandle.CURRENT));
    }

    @Implements(WifiTracker.class)
    public static class ShadowWifiTracker {
        @Implementation
        public void onStart() {
            // do nothing
        }
    }
}
