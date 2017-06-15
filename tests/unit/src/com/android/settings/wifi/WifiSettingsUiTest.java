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

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import static com.google.common.truth.Truth.assertThat;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiSsid;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.android.settings.Settings.WifiSettingsActivity;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.WifiTracker;
import com.android.settingslib.wifi.WifiTracker.WifiListener;
import com.android.settingslib.wifi.WifiTrackerFactory;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class WifiSettingsUiTest {

    // TODO(b/37714546): Investigate why resource ids are not resolving correctly in the test apk,
    // then remove this manual string entry
    /** R.string.wifi_configure_settings_preference_title */
    private static final String WIFI_PREFERENCES = "Wi\u2011Fi preferences";
    /** R.string.wifi_saved_access_points_label */
    private static final String SAVED_NETWORKS = "Saved networks";
    /** R.string.wifi_empty_list_wifi_off */
    private static final String WIFI_OFF_MESSAGE = "To see available networks, turn Wi\u2011Fi on.";
    /** R.string.wifi_display_status_connected */
    private static final String CONNECTED = "Connected";

    private static final String TEST_SSID = "\"Test Ssid\"";
    private static final String TEST_UNQUOTED_SSID = "Test Ssid";
    private static final String TEST_BSSID = "0a:08:5c:67:89:00";
    private static final int TEST_RSSI = 123;
    private static final int TEST_NETWORK_ID = 1;

    @Mock private WifiTracker mWifiTracker;
    @Mock private WifiManager mWifiManager;
    private Context mContext;
    private WifiListener mWifiListener;

    @Rule
    public ActivityTestRule<WifiSettingsActivity> mActivityRule =
            new ActivityTestRule<>(WifiSettingsActivity.class, true);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = InstrumentationRegistry.getTargetContext();
        WifiTrackerFactory.setTestingWifiTracker(mWifiTracker);
        when(mWifiTracker.getManager()).thenReturn(mWifiManager);
    }

    private void setupConnectedAccessPoint() {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = TEST_SSID;
        config.BSSID = TEST_BSSID;
        config.networkId = TEST_NETWORK_ID;
        WifiInfo wifiInfo = new WifiInfo();
        wifiInfo.setSSID(WifiSsid.createFromAsciiEncoded(TEST_UNQUOTED_SSID));
        wifiInfo.setBSSID(TEST_BSSID);
        wifiInfo.setRssi(TEST_RSSI);
        wifiInfo.setNetworkId(TEST_NETWORK_ID);
        NetworkInfo networkInfo = new NetworkInfo(ConnectivityManager.TYPE_WIFI, 0, null, null);
        networkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, null);
        AccessPoint accessPoint = new AccessPoint(mContext, config);
        accessPoint.update(config, wifiInfo, networkInfo);

        assertThat(accessPoint.getSsidStr()).isEqualTo(TEST_UNQUOTED_SSID);
        assertThat(accessPoint.getBssid()).isEqualTo(TEST_BSSID);
        assertThat(accessPoint.getNetworkInfo()).isNotNull();
        assertThat(accessPoint.isActive()).isTrue();
        assertThat(accessPoint.getSettingsSummary()).isEqualTo(CONNECTED);

        when(mWifiTracker.getAccessPoints()).thenReturn(
                Lists.asList(accessPoint, new AccessPoint[]{}));
    }

    private void launchActivity() {
        mActivityRule.launchActivity(new Intent("android.settings.WIFI_SETTINGS"));

        verify(mWifiTracker).getManager();

        List<Fragment> fragments = mActivityRule.getActivity().getFragmentManager().getFragments();
        assertThat(fragments.size()).isEqualTo(1);
        mWifiListener = (WifiSettings) fragments.get(0);
        assertThat(mWifiListener).isNotNull();
    }

    private void setWifiState(int wifiState) {
        when(mWifiManager.getWifiState()).thenReturn(wifiState);
        when(mWifiManager.isWifiEnabled()).thenReturn(wifiState == WifiManager.WIFI_STATE_ENABLED);
    }

    private void callOnWifiStateChanged(int state) {
        mActivityRule.getActivity().getMainThreadHandler()
                .post( () -> mWifiListener.onWifiStateChanged(state) );
    }

    @Test
    public void launchActivityShouldSucceed() {
        launchActivity();
    }

    @Test
    public void shouldShowWifiPreferences() {
        launchActivity();

        onView(withText(WIFI_PREFERENCES)).check(matches(isDisplayed()));
    }

    @Test
    public void noSavedNetworks_shouldNotShowSavedNetworksButton() {
        setWifiState(WifiManager.WIFI_STATE_ENABLED);
        when(mWifiTracker.getNumSavedNetworks()).thenReturn(0);

        launchActivity();

        onView(withText(SAVED_NETWORKS)).check(matches(not(isDisplayed())));
    }

    @Test
    public void savedNetworksExist_shouldShowSavedNetworksButton() {
        setWifiState(WifiManager.WIFI_STATE_ENABLED);
        when(mWifiTracker.getNumSavedNetworks()).thenReturn(1);

        launchActivity();

        onView(allOf(withText(SAVED_NETWORKS),
                withEffectiveVisibility(VISIBLE))).check(matches(isDisplayed()));
    }

    @Test
    public void onDisableWifi_seeOffMessage() {
        setWifiState(WifiManager.WIFI_STATE_DISABLED);

        launchActivity();
        callOnWifiStateChanged(WifiManager.WIFI_STATE_DISABLED);

        onView(withText(startsWith(WIFI_OFF_MESSAGE))).check(matches(isDisplayed()));
    }

    @Test
    public void onEnableWifi_shouldNotSeeOffMessage() {
        setWifiState(WifiManager.WIFI_STATE_ENABLED);

        launchActivity();
        callOnWifiStateChanged(WifiManager.WIFI_STATE_ENABLED);

        onView(withText(startsWith(WIFI_OFF_MESSAGE))).check(doesNotExist());
    }

    @Test
    public void onConnected_shouldSeeConnectedMessage() {
        setWifiState(WifiManager.WIFI_STATE_ENABLED);
        setupConnectedAccessPoint();
        when(mWifiTracker.isConnected()).thenReturn(true);

        launchActivity();

        onView(withText(CONNECTED)).check(matches(isDisplayed()));
    }

    @Test
    public void resumingAp_shouldNotForceUpdateWhenExistingAPsAreListed() {
        setWifiState(WifiManager.WIFI_STATE_ENABLED);
        setupConnectedAccessPoint();
        when(mWifiTracker.isConnected()).thenReturn(true);

        launchActivity();

        onView(withText(CONNECTED)).check(matches(isDisplayed()));
        verify(mWifiTracker).forceUpdate();

        Activity activity = mActivityRule.getActivity();
        activity.finish();
        getInstrumentation().waitForIdleSync();

        getInstrumentation().callActivityOnStart(activity);
        verify(mWifiTracker, atMost(1)).forceUpdate();
    }
}
