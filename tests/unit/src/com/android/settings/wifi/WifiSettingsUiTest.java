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

import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.google.common.truth.Truth.assertThat;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.test.uiautomator.UiDevice;

import androidx.fragment.app.Fragment;
import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.Settings.WifiSettingsActivity;
import com.android.settingslib.utils.ThreadUtils;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiPickerTracker;

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
    private static final String TEST_SSID = "Test Ssid";
    private static final String TEST_KEY = "Test Key";

    // Keys used to lookup resources by name (see the resourceId/resourceString helper methods).
    private static final String STRING = "string";
    private static final String WIFI_CONFIGURE_SETTINGS_PREFERENCE_TITLE =
            "wifi_configure_settings_preference_title";
    private static final String WIFI_SAVED_ACCESS_POINTS_LABEL = "wifi_saved_access_points_label";
    private static final String WIFI_EMPTY_LIST_WIFI_OFF = "wifi_empty_list_wifi_off";
    private static final String WIFI_DISPLAY_STATUS_CONNECTED = "wifi_display_status_connected";

    @Mock
    private WifiPickerTracker mWifiTracker;
    @Mock
    private WifiPickerTracker.WifiPickerTrackerCallback mWifiListener;

    private Context mContext;
    private UiDevice mDevice;

    @Rule
    public ActivityTestRule<WifiSettingsActivity> mActivityRule =
            new ActivityTestRule<>(WifiSettingsActivity.class, true);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = InstrumentationRegistry.getTargetContext();
        mDevice = UiDevice.getInstance(getInstrumentation());
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

    /** Launch the activity via an Intent with a String extra. */
    private void launchActivity(String extraName, String extraValue) {
        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        if (extraName != null && extraValue != null) {
            intent.putExtra(extraName, extraValue);
        }
        mActivityRule.launchActivity(intent);

        List<Fragment> fragments =
                mActivityRule.getActivity().getSupportFragmentManager().getFragments();
        assertThat(fragments.size()).isEqualTo(1);
        ((WifiSettings) fragments.get(0)).mWifiPickerTracker = mWifiTracker;
        mWifiListener = (WifiSettings) fragments.get(0);
        assertThat(mWifiListener).isNotNull();
    }

    /** Helper to launch the activity with no extra. */
    private void launchActivity() {
        launchActivity(null, null);
    }

    private void setWifiState(int wifiState) {
        when(mWifiTracker.getWifiState()).thenReturn(wifiState);
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
        mActivityRule.getActivity().getMainThreadHandler()
                .post(() -> mWifiListener.onNumSavedNetworksChanged());

        onView(allOf(withText(resourceId(STRING, WIFI_SAVED_ACCESS_POINTS_LABEL)),
                withEffectiveVisibility(VISIBLE))).check(matches(isDisplayed()));
    }

    @Test
    public void onWifiStateChanged_wifiDisabled_seeOffMessage() {
        setWifiState(WifiManager.WIFI_STATE_DISABLED);

        launchActivity();
        mActivityRule.getActivity().getMainThreadHandler()
                .post(() -> mWifiListener.onWifiStateChanged());

        onView(withText(startsWith(resourceString(WIFI_EMPTY_LIST_WIFI_OFF)))).check(
                matches(isDisplayed()));
    }

    @Test
    public void onWifiStateChanged_wifiEnabled_shouldNotSeeOffMessage() {
        setWifiState(WifiManager.WIFI_STATE_ENABLED);

        launchActivity();
        mActivityRule.getActivity().getMainThreadHandler()
                .post(() -> mWifiListener.onWifiStateChanged());

        onView(withText(startsWith(resourceString(WIFI_EMPTY_LIST_WIFI_OFF)))).check(
                doesNotExist());
    }

    @Test
    public void onConnected_shouldSeeConnectedMessage() {
        setWifiState(WifiManager.WIFI_STATE_ENABLED);
        final WifiEntry wifiEntry = mock(WifiEntry.class);
        when(wifiEntry.getConnectedState()).thenReturn(WifiEntry.CONNECTED_STATE_CONNECTED);
        when(wifiEntry.getSummary(false /* concise */))
                .thenReturn(resourceString(WIFI_DISPLAY_STATUS_CONNECTED));
        when(wifiEntry.getKey()).thenReturn(TEST_KEY);
        when(mWifiTracker.getConnectedWifiEntry()).thenReturn(wifiEntry);

        launchActivity();
        ThreadUtils.postOnMainThread(() -> mWifiListener.onWifiEntriesChanged());
        mDevice.waitForIdle();

        onView(withText(resourceString(WIFI_DISPLAY_STATUS_CONNECTED))).check(
                matches(isDisplayed()));
    }

    @Test
    public void changingSecurityStateOnAp_ShouldNotCauseMultipleListItems() {
        setWifiState(WifiManager.WIFI_STATE_ENABLED);

        final WifiEntry openWifiEntry = mock(WifiEntry.class);
        when(openWifiEntry.getTitle()).thenReturn(TEST_SSID);
        when(openWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_NONE);

        final WifiEntry eapWifiEntry = mock(WifiEntry.class);
        when(eapWifiEntry.getTitle()).thenReturn(TEST_SSID);
        when(eapWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_EAP);

        final WifiEntry wepWifiEntry = mock(WifiEntry.class);
        when(wepWifiEntry.getTitle()).thenReturn(TEST_SSID);
        when(wepWifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_WEP);

        // Return a different security state each time getWifiEntries is invoked
        when(mWifiTracker.getWifiEntries())
                .thenReturn(Lists.newArrayList(openWifiEntry))
                .thenReturn(Lists.newArrayList(eapWifiEntry))
                .thenReturn(Lists.newArrayList(wepWifiEntry));

        launchActivity();

        ThreadUtils.postOnMainThread(() -> mWifiListener.onWifiEntriesChanged());
        mDevice.waitForIdle();
        onView(withText(TEST_SSID)).check(matches(isDisplayed()));

        ThreadUtils.postOnMainThread(() -> mWifiListener.onWifiEntriesChanged());
        mDevice.waitForIdle();
        onView(withText(TEST_SSID)).check(matches(isDisplayed()));

        ThreadUtils.postOnMainThread(() -> mWifiListener.onWifiEntriesChanged());
        mDevice.waitForIdle();
        onView(withText(TEST_SSID)).check(matches(isDisplayed()));
    }
}
