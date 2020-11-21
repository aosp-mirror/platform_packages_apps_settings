/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkRequest;
import android.net.NetworkScoreManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class InternetPreferenceControllerTest {

    private static final String TEST_SUMMARY = "test summary";

    private Context mContext;
    private InternetPreferenceController mController;
    private PreferenceScreen mScreen;
    private Preference mPreference;

    @Mock
    private ConnectivityManager mConnectivityManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(ConnectivityManager.class)).thenReturn(mConnectivityManager);
        when(mContext.getSystemService(NetworkScoreManager.class))
            .thenReturn(mock(NetworkScoreManager.class));
        final WifiManager wifiManager = mock(WifiManager.class);
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(wifiManager);
        when(wifiManager.getWifiState()).thenReturn(WifiManager.WIFI_STATE_DISABLED);

        mController = new InternetPreferenceController(mContext);
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreference = new Preference(mContext);
        mPreference.setKey(InternetPreferenceController.KEY_INTERNET_SETTINGS);
        mScreen.addPreference(mPreference);
    }

    @Test
    public void isAvailable_shouldBeTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void onResume_shouldRegisterCallback() {
        mController.onResume();

        verify(mContext).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class));
        verify(mConnectivityManager).registerNetworkCallback(
                any(NetworkRequest.class),
                any(ConnectivityManager.NetworkCallback.class),
                any(Handler.class));
    }

    @Test
    public void onPause_shouldUnregisterCallback() {
        mController.onResume();
        mController.onPause();

        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
        verify(mConnectivityManager, times(2)).unregisterNetworkCallback(
                any(ConnectivityManager.NetworkCallback.class));
    }

    @Test
    public void onSummaryChanged_shouldUpdatePreferenceSummary() {
        mController.displayPreference(mScreen);

        mController.onSummaryChanged(TEST_SUMMARY);

        assertThat(mPreference.getSummary()).isEqualTo(TEST_SUMMARY);
    }
}
