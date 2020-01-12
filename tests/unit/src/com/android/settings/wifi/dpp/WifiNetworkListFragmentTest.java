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

package com.android.settings.wifi.dpp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import androidx.fragment.app.Fragment;
import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.WifiTracker;
import com.android.settingslib.wifi.WifiTrackerFactory;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class WifiNetworkListFragmentTest {
    private static final String TEST_SSID = "\"Test Ssid\"";
    private static final String TEST_UNQUOTED_SSID = "Test Ssid";
    private static final String TEST_BSSID = "0a:08:5c:67:89:00";
    private static final int TEST_RSSI = 123;
    private static final int TEST_NETWORK_ID = 1;

    private static final String TEST_DPP_URL = "DPP:C:81/1;I:DPP_TESTER;K:"
        + "MDkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDIgADUysZxnwRFGQR7Fepipjl84TG/dQR07es91iOB3PkPOk=;;";

    // Keys used to lookup resources by name (see the resourceId/resourceString helper methods).
    private static final String ID = "id";
    private static final String STRING = "string";
    private static final String WIFI_DISPLAY_STATUS_CONNECTED = "wifi_display_status_connected";

    @Mock
    private WifiTracker mWifiTracker;
    @Mock
    private WifiManager mWifiManager;

    private WifiNetworkListFragment mWifiNetworkListFragment;
    private Context mContext;

    @Rule
    public ActivityTestRule<WifiDppConfiguratorActivity> mActivityRule = new ActivityTestRule<>(
            WifiDppConfiguratorActivity.class, true);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = InstrumentationRegistry.getTargetContext();
        WifiTrackerFactory.setTestingWifiTracker(mWifiTracker);
        when(mWifiTracker.getManager()).thenReturn(mWifiManager);
    }

    private void callOnWifiStateChanged(int state) {
        mActivityRule.getActivity().getMainThreadHandler()
                .post(() -> mWifiNetworkListFragment.onWifiStateChanged(state));
    }

    /** Launch the activity via an Intent with data Uri */
    private void launchActivity(String uriString) {
        final Intent intent = new Intent(Settings.ACTION_PROCESS_WIFI_EASY_CONNECT_URI);
        intent.setData(Uri.parse(uriString));
        mActivityRule.launchActivity(intent);

        List<Fragment> fragments =
                mActivityRule.getActivity().getSupportFragmentManager().getFragments();
        assertThat(fragments.size()).isEqualTo(1);
        List<Fragment> childFragments = fragments.get(0).getChildFragmentManager().getFragments();
        assertThat(childFragments.size()).isEqualTo(1);
        mWifiNetworkListFragment = (WifiNetworkListFragment) childFragments.get(0);
        assertThat(mWifiNetworkListFragment).isNotNull();
    }

    private int resourceId(String type, String name) {
        return mContext.getResources().getIdentifier(name, type, mContext.getPackageName());
    }

    /** Similar to {@link #resourceId}, but for accessing R.string.<name> values. */
    private String resourceString(String name) {
        return mContext.getResources().getString(resourceId(STRING, name));
    }

    private void setWifiState(int wifiState) {
        when(mWifiManager.getWifiState()).thenReturn(wifiState);
        when(mWifiManager.isWifiEnabled()).thenReturn(wifiState == WifiManager.WIFI_STATE_ENABLED);
    }

    private void setupConnectedAccessPoint() {
        final WifiConfiguration config = new WifiConfiguration();
        config.SSID = TEST_SSID;
        config.BSSID = TEST_BSSID;
        config.networkId = TEST_NETWORK_ID;
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        final WifiInfo wifiInfo = new WifiInfo.Builder()
                .setSsid(TEST_UNQUOTED_SSID.getBytes(StandardCharsets.UTF_8))
                .setBssid(TEST_BSSID)
                .setRssi(TEST_RSSI)
                .setNetworkId(TEST_NETWORK_ID)
                .build();
        final NetworkInfo networkInfo = new NetworkInfo(ConnectivityManager.TYPE_WIFI, 0, null, null);
        networkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, null);
        final AccessPoint accessPoint = new AccessPoint(mContext, config);
        accessPoint.update(config, wifiInfo, networkInfo);

        assertThat(accessPoint.getSsidStr()).isEqualTo(TEST_UNQUOTED_SSID);
        assertThat(accessPoint.getBssid()).isEqualTo(TEST_BSSID);
        assertThat(accessPoint.getNetworkInfo()).isNotNull();
        assertThat(accessPoint.isActive()).isTrue();
        assertThat(accessPoint.getSettingsSummary()).isEqualTo(
                resourceString(WIFI_DISPLAY_STATUS_CONNECTED));

        when(mWifiTracker.getAccessPoints()).thenReturn(
                Lists.asList(accessPoint, new AccessPoint[]{}));
    }

    @Test
    public void onConnected_shouldSeeConnectedMessage() throws Exception {
        setWifiState(WifiManager.WIFI_STATE_ENABLED);
        setupConnectedAccessPoint();
        when(mWifiTracker.isConnected()).thenReturn(true);

        launchActivity(TEST_DPP_URL);
        callOnWifiStateChanged(WifiManager.WIFI_STATE_ENABLED);

        onView(withText(resourceString(WIFI_DISPLAY_STATUS_CONNECTED))).check(
                matches(isDisplayed()));
    }
}
