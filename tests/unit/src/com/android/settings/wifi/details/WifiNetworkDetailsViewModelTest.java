/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.wifi.details;

import static android.net.wifi.sharedconnectivity.app.HotspotNetwork.NETWORK_TYPE_CELLULAR;
import static android.net.wifi.sharedconnectivity.app.HotspotNetwork.NETWORK_TYPE_ETHERNET;
import static android.net.wifi.sharedconnectivity.app.HotspotNetwork.NETWORK_TYPE_WIFI;
import static android.telephony.CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;

import static com.android.wifitrackerlib.WifiEntry.WIFI_LEVEL_MAX;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;

import androidx.lifecycle.MutableLiveData;
import androidx.test.core.app.ApplicationProvider;

import com.android.wifitrackerlib.HotspotNetworkEntry;
import com.android.wifitrackerlib.WifiEntry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class WifiNetworkDetailsViewModelTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Application mApplication = ApplicationProvider.getApplicationContext();
    @Mock
    MutableLiveData<WifiNetworkDetailsViewModel.HotspotNetworkData> mHotspotNetworkData;
    @Mock
    HotspotNetworkEntry mHotspotNetworkEntry;

    WifiNetworkDetailsViewModel mViewModel;
    ArgumentCaptor<WifiNetworkDetailsViewModel.HotspotNetworkData> mHotspotNetworkDataCaptor =
            ArgumentCaptor.forClass(WifiNetworkDetailsViewModel.HotspotNetworkData.class);

    @Before
    public void setUp() {
        mViewModel = new WifiNetworkDetailsViewModel(mApplication);
        mViewModel.mHotspotNetworkData = mHotspotNetworkData;
    }

    @Test
    public void setWifiEntry_notHotspotNetworkEntry_postValueNull() {
        mViewModel.setWifiEntry(mock(WifiEntry.class));

        verify(mHotspotNetworkData).postValue(null);
    }

    @Test
    public void setWifiEntry_hotspotNetworkEntryWifi_postValueCorrect() {
        when(mHotspotNetworkEntry.getNetworkType()).thenReturn(NETWORK_TYPE_WIFI);
        when(mHotspotNetworkEntry.getUpstreamConnectionStrength()).thenReturn(WIFI_LEVEL_MAX);
        when(mHotspotNetworkEntry.getBatteryPercentage()).thenReturn(100);
        when(mHotspotNetworkEntry.isBatteryCharging()).thenReturn(false);


        mViewModel.setWifiEntry(mHotspotNetworkEntry);

        verify(mHotspotNetworkData).postValue(mHotspotNetworkDataCaptor.capture());
        WifiNetworkDetailsViewModel.HotspotNetworkData data = mHotspotNetworkDataCaptor.getValue();
        assertThat(data.getNetworkType()).isEqualTo(NETWORK_TYPE_WIFI);
        assertThat(data.getUpstreamConnectionStrength()).isEqualTo(WIFI_LEVEL_MAX);
        assertThat(data.getBatteryPercentage()).isEqualTo(100);
        assertThat(data.isBatteryCharging()).isEqualTo(false);
    }

    @Test
    public void setWifiEntry_hotspotNetworkEntryMobileData_postValueCorrect() {
        when(mHotspotNetworkEntry.getNetworkType()).thenReturn(NETWORK_TYPE_CELLULAR);
        when(mHotspotNetworkEntry.getUpstreamConnectionStrength())
                .thenReturn(SIGNAL_STRENGTH_NONE_OR_UNKNOWN);
        when(mHotspotNetworkEntry.getBatteryPercentage()).thenReturn(0);
        when(mHotspotNetworkEntry.isBatteryCharging()).thenReturn(true);


        mViewModel.setWifiEntry(mHotspotNetworkEntry);

        verify(mHotspotNetworkData).postValue(mHotspotNetworkDataCaptor.capture());
        WifiNetworkDetailsViewModel.HotspotNetworkData data = mHotspotNetworkDataCaptor.getValue();
        assertThat(data.getNetworkType()).isEqualTo(NETWORK_TYPE_CELLULAR);
        assertThat(data.getUpstreamConnectionStrength()).isEqualTo(SIGNAL_STRENGTH_NONE_OR_UNKNOWN);
        assertThat(data.getBatteryPercentage()).isEqualTo(0);
        assertThat(data.isBatteryCharging()).isEqualTo(true);
    }

    @Test
    public void setWifiEntry_hotspotNetworkEntryEthernet_postValueCorrect() {
        when(mHotspotNetworkEntry.getNetworkType()).thenReturn(NETWORK_TYPE_ETHERNET);
        when(mHotspotNetworkEntry.getBatteryPercentage()).thenReturn(50);
        when(mHotspotNetworkEntry.isBatteryCharging()).thenReturn(true);


        mViewModel.setWifiEntry(mHotspotNetworkEntry);

        verify(mHotspotNetworkData).postValue(mHotspotNetworkDataCaptor.capture());
        WifiNetworkDetailsViewModel.HotspotNetworkData data = mHotspotNetworkDataCaptor.getValue();
        assertThat(data.getNetworkType()).isEqualTo(NETWORK_TYPE_ETHERNET);
        assertThat(data.getBatteryPercentage()).isEqualTo(50);
        assertThat(data.isBatteryCharging()).isEqualTo(true);
    }

    @Test
    public void getSecuritySummary_returnNotNull() {
        assertThat(mViewModel.getHotspotNetworkData()).isNotNull();
    }
}
