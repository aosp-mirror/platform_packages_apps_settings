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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.google.common.truth.Truth.assertThat;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.NetworkSelectionStatus;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import androidx.fragment.app.Fragment;
import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.Settings.WifiSettingsActivity;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.TestAccessPointBuilder;
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

import java.nio.charset.StandardCharsets;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class WifiSettingsUiTest {
    private static final String TEST_SSID = "\"Test Ssid\"";
    private static final String TEST_UNQUOTED_SSID = "Test Ssid";
    private static final String TEST_BSSID = "0a:08:5c:67:89:00";
    private static final int TEST_RSSI = 123;
    private static final int TEST_NETWORK_ID = 1;

    // Keys used to lookup resources by name (see the resourceId/resourceString helper methods).
    private static final String ID = "id";
    private static final String STRING = "string";
    private static final String WIFI_CONFIGURE_SETTINGS_PREFERENCE_TITLE =
            "wifi_configure_settings_preference_title";
    private static final String WIFI_SAVED_ACCESS_POINTS_LABEL = "wifi_saved_access_points_label";
    private static final String WIFI_EMPTY_LIST_WIFI_OFF = "wifi_empty_list_wifi_off";
    private static final String WIFI_DISPLAY_STATUS_CONNECTED = "wifi_display_status_connected";
    private static final String WIFI_PASSWORD = "wifi_password";
    private static final String WIFI_SHOW_PASSWORD = "wifi_show_password";
    private static final String PASSWORD_LAYOUT = "password_layout";
    private static final String PASSWORD = "password";

    @Mock
    private WifiTracker mWifiTracker;
    @Mock
    private WifiManager mWifiManager;
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

    /**
     * Helper to get around the problem that directly accessing settings resource id's from
     * com.android.settings.R via R.(type).(name) (eg R.id.password or
     * R.string.wifi_configure_settings_preference_title) may not work due to mismatched resource
     * ids. See b/37714546 and b/63546650.
     */
    private int resourceId(String type, String name) {
        return mContext.getResources().getIdentifier(name, type, mContext.getPackageName());
    }

    /** Similar to {@link #resourceId}, but for accessing R.string.<name> values. */
    private String resourceString(String name) {
        return mContext.getResources().getString(resourceId(STRING, name));
    }

    private void setupConnectedAccessPoint() {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = TEST_SSID;
        config.BSSID = TEST_BSSID;
        config.networkId = TEST_NETWORK_ID;
        WifiInfo wifiInfo = new WifiInfo.Builder()
                .setSsid(TEST_UNQUOTED_SSID.getBytes(StandardCharsets.UTF_8))
                .setBssid(TEST_BSSID)
                .setRssi(TEST_RSSI)
                .setNetworkId(TEST_NETWORK_ID)
                .build();
        NetworkInfo networkInfo = new NetworkInfo(ConnectivityManager.TYPE_WIFI, 0, null, null);
        networkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, null);
        AccessPoint accessPoint = new AccessPoint(mContext, config);
        accessPoint.update(config, wifiInfo, networkInfo);

        assertThat(accessPoint.getSsidStr()).isEqualTo(TEST_UNQUOTED_SSID);
        assertThat(accessPoint.getBssid()).isEqualTo(TEST_BSSID);
        assertThat(accessPoint.getNetworkInfo()).isNotNull();
        assertThat(accessPoint.isActive()).isTrue();
        assertThat(accessPoint.getSettingsSummary()).isEqualTo(
                resourceString(WIFI_DISPLAY_STATUS_CONNECTED));

        when(mWifiTracker.getAccessPoints()).thenReturn(
                Lists.asList(accessPoint, new AccessPoint[] {}));
    }

    /** Launch the activity via an Intent with a String extra. */
    private void launchActivity(String extraName, String extraValue) {
        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        if (extraName != null && extraValue != null) {
            intent.putExtra(extraName, extraValue);
        }
        mActivityRule.launchActivity(intent);

        verify(mWifiTracker).getManager();

        List<Fragment> fragments =
                mActivityRule.getActivity().getSupportFragmentManager().getFragments();
        assertThat(fragments.size()).isEqualTo(1);
        mWifiListener = (WifiSettings) fragments.get(0);
        assertThat(mWifiListener).isNotNull();
    }

    /** Helper to launch the activity with no extra. */
    private void launchActivity() {
        launchActivity(null, null);
    }

    private void setWifiState(int wifiState) {
        when(mWifiManager.getWifiState()).thenReturn(wifiState);
        when(mWifiManager.isWifiEnabled()).thenReturn(wifiState == WifiManager.WIFI_STATE_ENABLED);
    }

    private void callOnWifiStateChanged(int state) {
        mActivityRule.getActivity().getMainThreadHandler()
                .post(() -> mWifiListener.onWifiStateChanged(state));
    }

    @Test
    public void launchActivityShouldSucceed() {
        launchActivity();
    }

    @Test
    public void shouldShowWifiPreferences() {
        launchActivity();

        onView(withText(resourceId(STRING, WIFI_CONFIGURE_SETTINGS_PREFERENCE_TITLE))).check(
                matches(isDisplayed()));
    }

    @Test
    public void noSavedNetworks_wifiDisabled_shouldNotShowSavedNetworksButton() {
        setWifiState(WifiManager.WIFI_STATE_DISABLED);
        when(mWifiTracker.getNumSavedNetworks()).thenReturn(0);

        launchActivity();

        onView(withText(resourceId(STRING, WIFI_SAVED_ACCESS_POINTS_LABEL))).check(
                doesNotExist());
    }

    @Test
    public void savedNetworksExist_shouldShowSavedNetworksButton() {
        setWifiState(WifiManager.WIFI_STATE_ENABLED);
        when(mWifiTracker.getNumSavedNetworks()).thenReturn(1);

        launchActivity();

        onView(allOf(withText(resourceId(STRING, WIFI_SAVED_ACCESS_POINTS_LABEL)),
                withEffectiveVisibility(VISIBLE))).check(matches(isDisplayed()));
    }

    @Test
    public void onDisableWifi_seeOffMessage() {
        setWifiState(WifiManager.WIFI_STATE_DISABLED);

        launchActivity();
        callOnWifiStateChanged(WifiManager.WIFI_STATE_DISABLED);

        onView(withText(startsWith(resourceString(WIFI_EMPTY_LIST_WIFI_OFF)))).check(
                matches(isDisplayed()));
    }

    @Test
    public void onEnableWifi_shouldNotSeeOffMessage() {
        setWifiState(WifiManager.WIFI_STATE_ENABLED);

        launchActivity();
        callOnWifiStateChanged(WifiManager.WIFI_STATE_ENABLED);

        onView(withText(startsWith(resourceString(WIFI_EMPTY_LIST_WIFI_OFF)))).check(
                doesNotExist());
    }

    @Test
    public void onConnected_shouldSeeConnectedMessage() {
        setWifiState(WifiManager.WIFI_STATE_ENABLED);
        setupConnectedAccessPoint();
        when(mWifiTracker.isConnected()).thenReturn(true);

        launchActivity();

        onView(withText(resourceString(WIFI_DISPLAY_STATUS_CONNECTED))).check(
                matches(isDisplayed()));
    }

    @Test
    public void changingSecurityStateOnApShouldNotCauseMultipleListItems() {
        setWifiState(WifiManager.WIFI_STATE_ENABLED);
        TestAccessPointBuilder builder = new TestAccessPointBuilder(mContext)
                .setSsid(TEST_SSID)
                .setSecurity(AccessPoint.SECURITY_NONE)
                .setRssi(TEST_RSSI);
        AccessPoint open = builder.build();

        builder.setSecurity(AccessPoint.SECURITY_EAP);
        AccessPoint eap = builder.build();

        builder.setSecurity(AccessPoint.SECURITY_WEP);
        AccessPoint wep = builder.build();

        // Return a different security state each time getAccessPoints is invoked
        when(mWifiTracker.getAccessPoints())
                .thenReturn(Lists.newArrayList(open))
                .thenReturn(Lists.newArrayList(eap))
                .thenReturn(Lists.newArrayList(wep));

        launchActivity();

        onView(withText(TEST_SSID)).check(matches(isDisplayed()));

        ThreadUtils.postOnMainThread(() -> mWifiListener.onAccessPointsChanged());
        onView(withText(TEST_SSID)).check(matches(isDisplayed()));

        ThreadUtils.postOnMainThread(() -> mWifiListener.onAccessPointsChanged());
        onView(withText(TEST_SSID)).check(matches(isDisplayed()));
    }

    @Test
    public void wrongPasswordSavedNetwork() {
        setWifiState(WifiManager.WIFI_STATE_ENABLED);

        // Set up an AccessPoint that is disabled due to incorrect password.
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = TEST_SSID;
        config.BSSID = TEST_BSSID;
        config.networkId = TEST_NETWORK_ID;
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

        NetworkSelectionStatus selectionStatus = new NetworkSelectionStatus.Builder()
                .setNetworkSelectionDisableReason(
                        NetworkSelectionStatus.DISABLED_BY_WRONG_PASSWORD)
                .setNetworkSelectionStatus(
                        NetworkSelectionStatus.NETWORK_SELECTION_TEMPORARY_DISABLED)
                .build();
        config.setNetworkSelectionStatus(selectionStatus);

        WifiInfo wifiInfo = new WifiInfo.Builder()
                .setSsid(TEST_UNQUOTED_SSID.getBytes(StandardCharsets.UTF_8))
                .setBssid(TEST_BSSID)
                .setRssi(TEST_RSSI)
                .setNetworkId(TEST_NETWORK_ID)
                .build();
        AccessPoint accessPoint = new AccessPoint(mContext, config);
        accessPoint.update(config, wifiInfo, null);

        // Make sure we've set up our access point correctly.
        assertThat(accessPoint.getSsidStr()).isEqualTo(TEST_UNQUOTED_SSID);
        assertThat(accessPoint.getBssid()).isEqualTo(TEST_BSSID);
        assertThat(accessPoint.isActive()).isFalse();
        assertThat(accessPoint.getConfig()).isNotNull();
        NetworkSelectionStatus networkStatus = accessPoint.getConfig().getNetworkSelectionStatus();
        assertThat(networkStatus).isNotNull();
        assertThat(networkStatus.getNetworkSelectionStatus())
                .isEqualTo(NetworkSelectionStatus.NETWORK_SELECTION_TEMPORARY_DISABLED);
        assertThat(networkStatus.getNetworkSelectionDisableReason()).isEqualTo(
                NetworkSelectionStatus.DISABLED_BY_WRONG_PASSWORD);

        when(mWifiTracker.getAccessPoints()).thenReturn(Lists.newArrayList(accessPoint));
        launchActivity(WifiSettings.EXTRA_START_CONNECT_SSID, accessPoint.getSsidStr());

        // Make sure that the password dialog is visible.
        onView(withText(resourceId(STRING, WIFI_PASSWORD))).check(matches(isDisplayed()));
        onView(withText(resourceId(STRING, WIFI_SHOW_PASSWORD))).check(matches(isDisplayed()));
        onView(withId(resourceId(ID, PASSWORD_LAYOUT))).check(matches(isDisplayed()));
        onView(withId(resourceId(ID, PASSWORD))).check(matches(isDisplayed()));
    }
}
