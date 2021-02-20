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

import static com.android.settings.network.InternetUpdater.INTERNET_CELLULAR;
import static com.android.settings.network.InternetUpdater.INTERNET_ETHERNET;
import static com.android.settings.network.InternetUpdater.INTERNET_NETWORKS_AVAILABLE;
import static com.android.settings.network.InternetUpdater.INTERNET_OFF;
import static com.android.settings.network.InternetUpdater.INTERNET_WIFI;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Looper;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class ConnectedEthernetNetworkControllerTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private Context mContext;
    private ConnectedEthernetNetworkController mController;
    private PreferenceScreen mScreen;
    private Preference mPreference;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(ConnectivityManager.class)).thenReturn(
                mock(ConnectivityManager.class));
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mock(WifiManager.class));

        mController = new ConnectedEthernetNetworkController(mContext, mock(Lifecycle.class));
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreference = new Preference(mContext);
        mPreference.setKey(ConnectedEthernetNetworkController.KEY);
        mScreen.addPreference(mPreference);
    }

    @Test
    public void isAvailable_internetOff_shouldBeFalse() {
        mController.onInternetTypeChanged(INTERNET_OFF);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_internetNetworksAvailable_shouldBeFalse() {
        mController.onInternetTypeChanged(INTERNET_NETWORKS_AVAILABLE);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_internetWifi_shouldBeFalse() {
        mController.onInternetTypeChanged(INTERNET_WIFI);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_internetCellular_shouldBeFalse() {
        mController.onInternetTypeChanged(INTERNET_CELLULAR);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_internetEthernet_shouldBeTrue() {
        mController.onInternetTypeChanged(INTERNET_ETHERNET);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isVisible_internetWifiToEthernet_shouldBeFalseThenTrue() {
        mController.displayPreference(mScreen);
        mController.onInternetTypeChanged(INTERNET_WIFI);

        assertThat(mPreference.isVisible()).isFalse();

        mController.onInternetTypeChanged(INTERNET_ETHERNET);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void isVisible_internetEthernetToCellular_shouldBeTrueThenFalse() {
        mController.displayPreference(mScreen);
        mController.onInternetTypeChanged(INTERNET_ETHERNET);

        assertThat(mPreference.isVisible()).isTrue();

        mController.onInternetTypeChanged(INTERNET_CELLULAR);

        assertThat(mPreference.isVisible()).isFalse();
    }
}
