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
 * limitations under the License.
 */

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
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

import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowRestrictedLockUtilsInternal.class)
public class WifiMasterSwitchPreferenceControllerTest {

    @Mock
    private WifiManager mWifiManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private MasterSwitchPreference mPreference;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private NetworkScoreManager mNetworkScoreManager;

    private Context mContext;
    private WifiMasterSwitchPreferenceController mController;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mMetricsFeatureProvider = FakeFeatureFactory.setupForTest().getMetricsFeatureProvider();
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());
        when(mContext.getSystemService(ConnectivityManager.class)).thenReturn(mConnectivityManager);
        when(mContext.getSystemService(NetworkScoreManager.class)).thenReturn(mNetworkScoreManager);
        mController = new WifiMasterSwitchPreferenceController(mContext, mMetricsFeatureProvider);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        when(mWifiManager.getWifiState()).thenReturn(WifiManager.WIFI_STATE_DISABLED);
    }

    @Test
    public void testWifiMasterSwitch_byDefault_shouldBeShown() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testWifiMasterSwitch_ifDisabled_shouldNotBeShown() {
        assertThat(mController.isAvailable()).isFalse();
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
    public void onStart_shouldRegisterPreferenceChangeListener() {
        mController.displayPreference(mScreen);
        mController.onStart();

        verify(mPreference).setOnPreferenceChangeListener(any(OnPreferenceChangeListener.class));
    }

    @Test
    public void onStop_shouldRegisterPreferenceChangeListener() {
        mController.displayPreference(mScreen);
        mController.onStart();

        mController.onStop();

        verify(mPreference).setOnPreferenceChangeListener(null);
    }

    @Test
    public void onSummaryChanged_shouldUpdatePreferenceSummary() {
        mController.displayPreference(mScreen);

        mController.onSummaryChanged("test summary");

        verify(mPreference).setSummary("test summary");
    }
}
