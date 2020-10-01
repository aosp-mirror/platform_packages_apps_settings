/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.datausage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;
import android.util.DataUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public final class DataUsageUtilsTest {

    @Mock
    private ConnectivityManager mManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private NetworkStatsManager mNetworkStatsManager;

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final ShadowApplication shadowContext = ShadowApplication.getInstance();
        mContext = RuntimeEnvironment.application;
        shadowContext.setSystemService(Context.CONNECTIVITY_SERVICE, mManager);
        shadowContext.setSystemService(Context.TELEPHONY_SERVICE, mTelephonyManager);
        shadowContext.setSystemService(Context.NETWORK_STATS_SERVICE, mNetworkStatsManager);
    }

    @Test
    public void mobileDataStatus_whenNetworkIsSupported() {
        when(mManager.isNetworkSupported(anyInt())).thenReturn(true);
        final boolean hasMobileData = DataUsageUtils.hasMobileData(mContext);
        assertThat(hasMobileData).isTrue();
    }

    @Test
    public void mobileDataStatus_whenNetworkIsNotSupported() {
        when(mManager.isNetworkSupported(anyInt())).thenReturn(false);
        final boolean hasMobileData = DataUsageUtils.hasMobileData(mContext);
        assertThat(hasMobileData).isFalse();
    }

    @Test
    public void formatDataUsage_useIECUnit() {
        final CharSequence formattedDataUsage = DataUsageUtils.formatDataUsage(
                mContext, DataUnit.GIBIBYTES.toBytes(1));

        assertThat(formattedDataUsage).isEqualTo("1.00 GB");
    }

    @Test
    public void hasEthernet_shouldQueryEthernetSummaryForUser() throws Exception {
        when(mManager.isNetworkSupported(anyInt())).thenReturn(true);
        final String subscriber = "TestSub";
        when(mTelephonyManager.getSubscriberId()).thenReturn(subscriber);

        DataUsageUtils.hasEthernet(mContext);

        verify(mNetworkStatsManager).querySummaryForUser(eq(ConnectivityManager.TYPE_ETHERNET),
                eq(subscriber), anyLong() /* startTime */, anyLong() /* endTime */);
    }
}
