/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.telephony.SubscriptionManager;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowSubscriptionManager;

@RunWith(RobolectricTestRunner.class)
public class SubscriptionUtilRoboTest {
    private static final int SUBID_1 = 1;
    private static final int SUBID_2 = 2;
    private static final int RAC_CARRIER_ID = 1;
    private static final int CARRIER_ID = 2;

    private Context mContext;
    private NetworkCapabilities mNetworkCapabilities;
    private ShadowSubscriptionManager mShadowSubscriptionManager;

    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock private Resources mResources;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mShadowSubscriptionManager = shadowOf(mContext.getSystemService(SubscriptionManager.class));
        when(mContext.getResources()).thenReturn(mResources);
        when(mContext.getSystemService(ConnectivityManager.class)).thenReturn(mConnectivityManager);
        when(mResources.getIntArray(anyInt())).thenReturn(new int[] {RAC_CARRIER_ID});
    }

    @Test
    public void isConnectedToMobileDataWithDifferentSubId_hasDataOnSubId2_returnTrue() {
        addNetworkTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        mShadowSubscriptionManager.setActiveDataSubscriptionId(SUBID_2);

        assertTrue(SubscriptionUtil.isConnectedToMobileDataWithDifferentSubId(mContext, SUBID_1));
    }

    @Test
    public void isConnectedToMobileDataWithDifferentSubId_hasDataOnSubId1_returnFalse() {
        addNetworkTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        mShadowSubscriptionManager.setActiveDataSubscriptionId(SUBID_1);

        assertFalse(SubscriptionUtil.isConnectedToMobileDataWithDifferentSubId(mContext, SUBID_1));
    }

    @Test
    public void carrierIsNotRAC_showRacDialogForEsim_returnFalse() {
        assertFalse(
                SubscriptionUtil.shouldShowRacDialogWhenErasingEsim(mContext, SUBID_1, CARRIER_ID));
    }

    @Test
    public void carrierIsNotRAC_noWifi_noDataConnection_showRacDialogForEsimreturnFalse() {
        addNetworkTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH);

        assertFalse(
                SubscriptionUtil.shouldShowRacDialogWhenErasingEsim(mContext, SUBID_1, CARRIER_ID));
    }

    @Test
    public void carrierIsRAC_isConnectedToDataOnSubId2_showRacDialogForEsim_returnFalse() {
        addNetworkTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        mShadowSubscriptionManager.setActiveDataSubscriptionId(SUBID_2);

        assertFalse(
                SubscriptionUtil.shouldShowRacDialogWhenErasingEsim(
                        mContext, SUBID_1, RAC_CARRIER_ID));
    }

    @Test
    public void carrierIsRAC_hasWifi_showRacDialogForEsim_returnFalse() {
        addNetworkTransportType(NetworkCapabilities.TRANSPORT_WIFI);

        assertFalse(
                SubscriptionUtil.shouldShowRacDialogWhenErasingEsim(
                        mContext, SUBID_1, RAC_CARRIER_ID));
    }

    @Test
    public void carrierIsRAC_isConnectedToDataOnSubId1_noWifi_showRacDialogForEsim_returnTrue() {
        addNetworkTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        mShadowSubscriptionManager.setActiveDataSubscriptionId(SUBID_1);

        assertTrue(
                SubscriptionUtil.shouldShowRacDialogWhenErasingEsim(
                        mContext, SUBID_1, RAC_CARRIER_ID));
    }

    @Test
    public void carrierIsRAC_noData_noWifi_showRacDialogForEsim_returnTrue() {
        addNetworkTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH);

        assertTrue(
                SubscriptionUtil.shouldShowRacDialogWhenErasingEsim(
                        mContext, SUBID_1, RAC_CARRIER_ID));
    }

    private void addNetworkTransportType(int networkType) {
        mNetworkCapabilities =
                new NetworkCapabilities.Builder().addTransportType(networkType).build();
        when(mConnectivityManager.getNetworkCapabilities(any())).thenReturn(mNetworkCapabilities);
    }
}
