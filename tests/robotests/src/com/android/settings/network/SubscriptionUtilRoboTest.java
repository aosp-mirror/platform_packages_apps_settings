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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
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

    private Context mContext;
    private NetworkCapabilities mNetworkCapabilities;
    private ShadowSubscriptionManager mShadowSubscriptionManager;

    @Mock
    private ConnectivityManager mConnectivityManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mShadowSubscriptionManager = shadowOf(mContext.getSystemService(SubscriptionManager.class));
        when(mContext.getSystemService(ConnectivityManager.class)).thenReturn(mConnectivityManager);
    }

    @Test
    public void isConnectedToWifiOrDifferentSubId_hasDataOnSubId2_returnTrue() {
        addNetworkTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        mShadowSubscriptionManager.setActiveDataSubscriptionId(SUBID_2);

        assertTrue(SubscriptionUtil.isConnectedToWifiOrDifferentSubId(mContext, SUBID_1));
    }

    @Test
    public void isConnectedToWifiOrDifferentSubId_hasDataOnSubId1_returnFalse() {
        addNetworkTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        mShadowSubscriptionManager.setActiveDataSubscriptionId(SUBID_1);

        assertFalse(SubscriptionUtil.isConnectedToWifiOrDifferentSubId(mContext, SUBID_1));
    }

    private void addNetworkTransportType(int networkType) {
        mNetworkCapabilities =
                new NetworkCapabilities.Builder().addTransportType(networkType).build();
        when(mConnectivityManager.getNetworkCapabilities(any())).thenReturn(mNetworkCapabilities);
    }
}
