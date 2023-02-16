/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.wifi.tether;

import static com.android.settings.wifi.WifiUtils.setCanShowWifiHotspotCached;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.TetheringManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.UserManager;
import android.util.FeatureFlagUtils;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.FeatureFlags;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowFragment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class WifiTetherSettingsTest {

    private static final int XML_RES = R.xml.wifi_tether_settings;
    private static final String[] WIFI_REGEXS = {"wifi_regexs"};

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private TetheringManager mTetheringManager;
    @Mock
    private WifiTetherSettings.WifiRestriction mWifiRestriction;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private TextView mEmptyTextView;

    private WifiTetherSettings mWifiTetherSettings;

    @Before
    public void setUp() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.TETHER_ALL_IN_ONE, false);
        setCanShowWifiHotspotCached(true);
        doReturn(mWifiManager).when(mContext).getSystemService(WifiManager.class);
        doReturn(mConnectivityManager)
                .when(mContext).getSystemService(Context.CONNECTIVITY_SERVICE);
        doReturn(mTetheringManager).when(mContext).getSystemService(Context.TETHERING_SERVICE);
        doReturn(WIFI_REGEXS).when(mTetheringManager).getTetherableWifiRegexs();
        doReturn(mUserManager).when(mContext).getSystemService(Context.USER_SERVICE);
        when(mWifiRestriction.isTetherAvailable(mContext)).thenReturn(true);
        when(mWifiRestriction.isHotspotAvailable(mContext)).thenReturn(true);

        mWifiTetherSettings = new WifiTetherSettings(mWifiRestriction);
    }

    @Test
    @Config(shadows = ShadowRestrictedDashboardFragment.class)
    public void onCreate_canNotShowWifiHotspot_shouldFinish() {
        setCanShowWifiHotspotCached(false);
        mWifiTetherSettings = spy(new WifiTetherSettings(mWifiRestriction));

        mWifiTetherSettings.onCreate(null);

        verify(mWifiTetherSettings).finish();
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void onStart_uiIsRestricted_removeAllPreferences() {
        spyWifiTetherSettings();

        mWifiTetherSettings.onStart();

        verify(mPreferenceScreen).removeAll();
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void onStart_hotspotNotAvailable_removeAllPreferences() {
        spyWifiTetherSettings();
        when(mWifiRestriction.isHotspotAvailable(mContext)).thenReturn(false);

        mWifiTetherSettings.onStart();

        verify(mPreferenceScreen).removeAll();
        verify(mEmptyTextView).setText(anyInt());
    }

    @Test
    public void createPreferenceControllers_getPreferenceControllersNotEmpty() {
        assertThat(WifiTetherSettings.SEARCH_INDEX_DATA_PROVIDER.getPreferenceControllers(mContext))
                .isNotEmpty();
    }

    @Test
    public void createPreferenceControllers_hasAutoOffPreference() {
        assertThat(mWifiTetherSettings.createPreferenceControllers(mContext)
                .stream()
                .filter(controller -> controller instanceof WifiTetherAutoOffPreferenceController)
                .count())
                .isEqualTo(1);
    }

    @Test
    public void getNonIndexableKeys_tetherAvailable_keysNotReturned() {
        when(mWifiRestriction.isTetherAvailable(mContext)).thenReturn(true);
        when(mWifiRestriction.isHotspotAvailable(mContext)).thenReturn(true);
        WifiTetherSettings.SearchIndexProvider searchIndexProvider =
                new WifiTetherSettings.SearchIndexProvider(XML_RES, mWifiRestriction);

        final List<String> keys = searchIndexProvider.getNonIndexableKeys(mContext);

        assertThat(keys).doesNotContain(WifiTetherSettings.KEY_WIFI_TETHER_NETWORK_NAME);
        assertThat(keys).doesNotContain(WifiTetherSettings.KEY_WIFI_TETHER_SECURITY);
        assertThat(keys).doesNotContain(WifiTetherSettings.KEY_WIFI_TETHER_NETWORK_PASSWORD);
        assertThat(keys).doesNotContain(WifiTetherSettings.KEY_WIFI_TETHER_AUTO_OFF);
        assertThat(keys).doesNotContain(WifiTetherSettings.KEY_WIFI_TETHER_MAXIMIZE_COMPATIBILITY);
    }

    @Test
    public void getNonIndexableKeys_tetherNotAvailable_keysReturned() {
        when(mWifiRestriction.isTetherAvailable(mContext)).thenReturn(false);
        when(mWifiRestriction.isHotspotAvailable(mContext)).thenReturn(true);
        WifiTetherSettings.SearchIndexProvider searchIndexProvider =
                new WifiTetherSettings.SearchIndexProvider(XML_RES, mWifiRestriction);

        final List<String> keys = searchIndexProvider.getNonIndexableKeys(mContext);

        assertThat(keys).contains(WifiTetherSettings.KEY_WIFI_TETHER_NETWORK_NAME);
        assertThat(keys).contains(WifiTetherSettings.KEY_WIFI_TETHER_SECURITY);
        assertThat(keys).contains(WifiTetherSettings.KEY_WIFI_TETHER_NETWORK_PASSWORD);
        assertThat(keys).contains(WifiTetherSettings.KEY_WIFI_TETHER_AUTO_OFF);
        assertThat(keys).contains(WifiTetherSettings.KEY_WIFI_TETHER_MAXIMIZE_COMPATIBILITY);
    }

    @Test
    public void getNonIndexableKeys_hotspotNotAvailable_keysReturned() {
        when(mWifiRestriction.isTetherAvailable(mContext)).thenReturn(true);
        when(mWifiRestriction.isHotspotAvailable(mContext)).thenReturn(false);
        WifiTetherSettings.SearchIndexProvider searchIndexProvider =
                new WifiTetherSettings.SearchIndexProvider(XML_RES, mWifiRestriction);

        final List<String> keys = searchIndexProvider.getNonIndexableKeys(mContext);

        assertThat(keys).contains(WifiTetherSettings.KEY_WIFI_TETHER_NETWORK_NAME);
        assertThat(keys).contains(WifiTetherSettings.KEY_WIFI_TETHER_SECURITY);
        assertThat(keys).contains(WifiTetherSettings.KEY_WIFI_TETHER_NETWORK_PASSWORD);
        assertThat(keys).contains(WifiTetherSettings.KEY_WIFI_TETHER_AUTO_OFF);
        assertThat(keys).contains(WifiTetherSettings.KEY_WIFI_TETHER_MAXIMIZE_COMPATIBILITY);
    }

    @Test
    public void getNonIndexableKeys_tetherAndHotspotNotAvailable_keysReturned() {
        when(mWifiRestriction.isTetherAvailable(mContext)).thenReturn(false);
        when(mWifiRestriction.isHotspotAvailable(mContext)).thenReturn(false);
        WifiTetherSettings.SearchIndexProvider searchIndexProvider =
                new WifiTetherSettings.SearchIndexProvider(XML_RES, mWifiRestriction);

        final List<String> keys = searchIndexProvider.getNonIndexableKeys(mContext);

        assertThat(keys).contains(WifiTetherSettings.KEY_WIFI_TETHER_NETWORK_NAME);
        assertThat(keys).contains(WifiTetherSettings.KEY_WIFI_TETHER_SECURITY);
        assertThat(keys).contains(WifiTetherSettings.KEY_WIFI_TETHER_NETWORK_PASSWORD);
        assertThat(keys).contains(WifiTetherSettings.KEY_WIFI_TETHER_AUTO_OFF);
        assertThat(keys).contains(WifiTetherSettings.KEY_WIFI_TETHER_MAXIMIZE_COMPATIBILITY);
    }

    @Test
    public void isPageSearchEnabled_canShowWifiHotspot_returnTrue() {
        setCanShowWifiHotspotCached(true);

        assertThat(WifiTetherSettings.SEARCH_INDEX_DATA_PROVIDER.isPageSearchEnabled(mContext))
                .isTrue();
    }

    @Test
    public void isPageSearchEnabled_canNotShowWifiHotspot_returnFalse() {
        setCanShowWifiHotspotCached(false);

        assertThat(WifiTetherSettings.SEARCH_INDEX_DATA_PROVIDER.isPageSearchEnabled(mContext))
                .isFalse();
    }

    private void spyWifiTetherSettings() {
        mWifiTetherSettings = spy(new WifiTetherSettings(mWifiRestriction));
        final FragmentActivity activity = mock(FragmentActivity.class);
        when(mWifiTetherSettings.getActivity()).thenReturn(activity);
        when(mWifiTetherSettings.getContext()).thenReturn(mContext);
        final Resources.Theme theme = mContext.getTheme();
        when(activity.getTheme()).thenReturn(theme);
        when(activity.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        doNothing().when(mWifiTetherSettings)
                .onCreatePreferences(any(Bundle.class), nullable(String.class));
        final FakeFeatureFactory fakeFeatureFactory = FakeFeatureFactory.setupForTest();
        ReflectionHelpers.setField(mWifiTetherSettings, "mDashboardFeatureProvider",
                fakeFeatureFactory.dashboardFeatureProvider);
        ReflectionHelpers.setField(mWifiTetherSettings, "mEmptyTextView", mEmptyTextView);
        doReturn(mPreferenceScreen).when(mWifiTetherSettings).getPreferenceScreen();

        mWifiTetherSettings.onCreate(Bundle.EMPTY);
    }

    @Implements(RestrictedDashboardFragment.class)
    public static final class ShadowRestrictedDashboardFragment {

        @Implementation
        public void onCreate(Bundle icicle) {
            // do nothing
        }
    }
}
