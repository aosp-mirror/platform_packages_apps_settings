/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkPolicyManager;
import android.net.VpnManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.telephony.TelephonyManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.ResetNetworkRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class ResetNetworkOperationBuilderTest {

    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private VpnManager mVpnManager;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private WifiP2pManager mWifiP2pManager;
    @Mock
    private WifiP2pManager.Channel mChannel;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private NetworkPolicyManager mNetworkPolicyManager;
    @Mock
    private ContentProvider mContentProvider;;


    private Context mContext;
    private ResetNetworkOperationBuilder mBuilder;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        doReturn(ContentResolver.wrap(mContentProvider)).when(mContext).getContentResolver();

        mBuilder = spy(new ResetNetworkOperationBuilder(mContext));
    }

    @Test
    public void resetConnectivityManager_performReset_whenBuildAndRun() {
        doReturn(mConnectivityManager).when(mContext)
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        mBuilder.resetConnectivityManager().build().run();

        verify(mConnectivityManager).factoryReset();
    }

    @Test
    public void resetVpnManager_performReset_whenBuildAndRun() {
        doReturn(mVpnManager).when(mContext)
                .getSystemService(Context.VPN_MANAGEMENT_SERVICE);

        mBuilder.resetVpnManager().build().run();

        verify(mVpnManager).factoryReset();
    }

    @Test
    public void resetWifiManager_performReset_whenBuildAndRun() {
        doReturn(mWifiManager).when(mContext)
                .getSystemService(Context.WIFI_SERVICE);

        mBuilder.resetWifiManager().build().run();

        verify(mWifiManager).factoryReset();
    }

    @Test
    public void resetWifiP2pManager_performReset_whenBuildAndRun() {
        doReturn(mChannel).when(mWifiP2pManager).initialize(mContext, null, null);
        doReturn(mWifiP2pManager).when(mContext)
                .getSystemService(Context.WIFI_P2P_SERVICE);

        mBuilder.resetWifiP2pManager(null).build().run();

        verify(mWifiP2pManager).factoryReset(mChannel, null);
    }

    @Test
    public void resetTelephonyAndNetworkPolicyManager_performReset_whenBuildAndRun() {
        int subId = 3;
        String imsi = "123456789012345";

        doReturn(mTelephonyManager).when(mTelephonyManager)
                .createForSubscriptionId(anyInt());
        doReturn(mTelephonyManager).when(mContext)
                .getSystemService(Context.TELEPHONY_SERVICE);
        doReturn(mNetworkPolicyManager).when(mContext)
                .getSystemService(Context.NETWORK_POLICY_SERVICE);

        doReturn(imsi).when(mTelephonyManager).getSubscriberId();

        mBuilder.resetTelephonyAndNetworkPolicyManager(subId).build().run();

        verify(mTelephonyManager).resetSettings();
        verify(mNetworkPolicyManager).factoryReset(imsi);
    }

    @Test
    public void resetIms_performReset_whenBuildAndRun_withSingleValidSubId() {
        final int subId = 1;
        doReturn(mTelephonyManager).when(mTelephonyManager)
                .createForSubscriptionId(anyInt());
        doReturn(mTelephonyManager).when(mContext)
                .getSystemService(Context.TELEPHONY_SERVICE);

        mBuilder.resetIms(subId).build().run();

        verify(mTelephonyManager).resetIms(anyInt());
    }

    @Test
    public void resetIms_performReset_whenBuildAndRun_withInvalidSubId() {
        final int subId = ResetNetworkRequest.INVALID_SUBSCRIPTION_ID;
        doReturn(mTelephonyManager).when(mTelephonyManager)
                .createForSubscriptionId(anyInt());
        doReturn(mTelephonyManager).when(mContext)
                .getSystemService(Context.TELEPHONY_SERVICE);

        mBuilder.resetIms(subId).build().run();

        verify(mTelephonyManager, never()).resetIms(anyInt());
    }

    @Test
    public void resetIms_performReset_whenBuildAndRun_withAllValidSubId() {
        final int subId = ResetNetworkRequest.ALL_SUBSCRIPTION_ID;
        doReturn(mTelephonyManager).when(mTelephonyManager)
                .createForSubscriptionId(anyInt());
        doReturn(mTelephonyManager).when(mContext)
                .getSystemService(Context.TELEPHONY_SERVICE);
        doReturn(2).when(mTelephonyManager).getActiveModemCount();

        mBuilder.resetIms(subId).build().run();

        verify(mTelephonyManager, times(2)).resetIms(anyInt());
    }

    @Test
    public void restartPhoneProcess_withoutTelephonyContentProvider_shouldNotCrash() {
        doThrow(new IllegalArgumentException()).when(mContentProvider).call(
                anyString(), anyString(), anyString(), any());

        mBuilder.restartPhoneProcess();
    }

    @Test
    public void restartRild_withoutTelephonyContentProvider_shouldNotCrash() {
        doThrow(new IllegalArgumentException()).when(mContentProvider).call(
                anyString(), anyString(), anyString(), any());

        mBuilder.restartRild();
    }

    @Test
    public void restartPhoneProcess_withTelephonyContentProvider_shouldCallRestartPhoneProcess() {
        mBuilder.restartPhoneProcess();

        verify(mContentProvider).call(
                eq(mBuilder.getResetTelephonyContentProviderAuthority()),
                eq(ResetNetworkOperationBuilder.METHOD_RESTART_PHONE_PROCESS),
                isNull(),
                isNull());
    }

    @Test
    public void restartRild_withTelephonyContentProvider_shouldCallRestartRild() {
        mBuilder.restartRild();

        verify(mContentProvider).call(
                eq(mBuilder.getResetTelephonyContentProviderAuthority()),
                eq(ResetNetworkOperationBuilder.METHOD_RESTART_RILD),
                isNull(),
                isNull());
    }
}
