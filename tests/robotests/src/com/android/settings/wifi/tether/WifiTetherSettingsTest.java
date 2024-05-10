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

import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_OPEN;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA3_SAE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import static com.android.settings.wifi.WifiUtils.setCanShowWifiHotspotCached;
import static com.android.settings.wifi.repository.WifiHotspotRepository.BAND_2GHZ_5GHZ_6GHZ;
import static com.android.settings.wifi.tether.WifiTetherSettings.KEY_INSTANT_HOTSPOT;
import static com.android.settings.wifi.tether.WifiTetherSettings.KEY_WIFI_HOTSPOT_SECURITY;
import static com.android.settings.wifi.tether.WifiTetherSettings.KEY_WIFI_HOTSPOT_SPEED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.TetheringManager;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.UserManager;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settings.wifi.factory.WifiFeatureProvider;
import com.android.settings.wifi.repository.WifiHotspotRepository;

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
    private static final String SSID = "ssid";
    private static final String PASSWORD = "password";
    private static final String SUMMARY = "summary";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private Context mContext = ApplicationProvider.getApplicationContext();
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
    @Mock
    private WifiTetherViewModel mWifiTetherViewModel;
    @Mock
    private WifiHotspotRepository mWifiHotspotRepository;
    @Mock
    private Preference mWifiHotspotSecurity;
    @Mock
    private LiveData<Integer> mSecuritySummary;
    @Mock
    private Preference mWifiHotspotSpeed;
    @Mock
    private LiveData<Integer> mSpeedSummary;
    @Mock
    private SettingsMainSwitchBar mMainSwitchBar;
    @Mock
    private WifiTetherSSIDPreferenceController mSSIDPreferenceController;
    @Mock
    private WifiTetherSecurityPreferenceController mSecurityPreferenceController;
    @Mock
    private WifiTetherPasswordPreferenceController mPasswordPreferenceController;
    @Mock
    private WifiTetherAutoOffPreferenceController mWifiTetherAutoOffPreferenceController;
    @Mock
    private WifiTetherMaximizeCompatibilityPreferenceController mMaxCompatibilityPrefController;
    @Mock
    private Preference mInstantHotspot;
    @Mock
    private LiveData<String> mInstantHotspotSummary;

    private WifiTetherSettings mSettings;

    @Before
    public void setUp() {
        setCanShowWifiHotspotCached(true);
        doReturn(mWifiManager).when(mContext).getSystemService(WifiManager.class);
        doReturn(mConnectivityManager)
                .when(mContext).getSystemService(Context.CONNECTIVITY_SERVICE);
        doReturn(mTetheringManager).when(mContext).getSystemService(Context.TETHERING_SERVICE);
        doReturn(WIFI_REGEXS).when(mTetheringManager).getTetherableWifiRegexs();
        doReturn(mUserManager).when(mContext).getSystemService(Context.USER_SERVICE);
        doReturn(true).when(mUserManager).isAdminUser();
        when(mWifiRestriction.isTetherAvailable(mContext)).thenReturn(true);
        when(mWifiRestriction.isHotspotAvailable(mContext)).thenReturn(true);

        WifiFeatureProvider provider = FakeFeatureFactory.setupForTest().getWifiFeatureProvider();
        when(provider.getWifiHotspotRepository()).thenReturn(mWifiHotspotRepository);
        when(provider.getWifiTetherViewModel(mock(ViewModelStoreOwner.class)))
                .thenReturn(mWifiTetherViewModel);
        when(mWifiTetherViewModel.isSpeedFeatureAvailable()).thenReturn(false);
        when(mWifiTetherViewModel.isInstantHotspotFeatureAvailable()).thenReturn(true);
        when(mWifiTetherViewModel.getSecuritySummary()).thenReturn(mSecuritySummary);
        when(mWifiTetherViewModel.getSpeedSummary()).thenReturn(mSpeedSummary);
        when(mWifiTetherViewModel.getInstantHotspotSummary()).thenReturn(mInstantHotspotSummary);

        mSettings = spy(new WifiTetherSettings(mWifiRestriction));
        mSettings.mMainSwitchBar = mMainSwitchBar;
        mSettings.mSSIDPreferenceController = mSSIDPreferenceController;
        when(mSSIDPreferenceController.getSSID()).thenReturn(SSID);
        mSettings.mSecurityPreferenceController = mSecurityPreferenceController;
        when(mSecurityPreferenceController.getSecurityType()).thenReturn(SECURITY_TYPE_WPA3_SAE);
        mSettings.mPasswordPreferenceController = mPasswordPreferenceController;
        when(mPasswordPreferenceController.getPasswordValidated(anyInt())).thenReturn(PASSWORD);
        mSettings.mWifiTetherAutoOffPreferenceController = mWifiTetherAutoOffPreferenceController;
        when(mWifiTetherAutoOffPreferenceController.isEnabled()).thenReturn(true);
        mSettings.mMaxCompatibilityPrefController = mMaxCompatibilityPrefController;
        mSettings.mWifiTetherViewModel = mWifiTetherViewModel;
        when(mSettings.findPreference(KEY_WIFI_HOTSPOT_SECURITY)).thenReturn(mWifiHotspotSecurity);
        when(mSettings.findPreference(KEY_WIFI_HOTSPOT_SPEED)).thenReturn(mWifiHotspotSpeed);
        when(mSettings.findPreference(KEY_INSTANT_HOTSPOT)).thenReturn(mInstantHotspot);
        mSettings.mInstantHotspot = mInstantHotspot;
    }

    @Test
    @Config(shadows = ShadowRestrictedDashboardFragment.class)
    public void onCreate_canNotShowWifiHotspot_shouldFinish() {
        setCanShowWifiHotspotCached(false);
        mSettings = spy(new WifiTetherSettings(mWifiRestriction));

        mSettings.onCreate(null);

        verify(mSettings).finish();
    }

    @Test
    @Config(shadows = ShadowRestrictedDashboardFragment.class)
    public void onCreate_uiIsRestricted_shouldNotGetViewModel() {
        mSettings.mWifiTetherViewModel = null;
        when(mWifiRestriction.isHotspotAvailable(mContext)).thenReturn(false);

        mSettings.onCreate(null);

        assertThat(mSettings.mWifiTetherViewModel).isNull();
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void onStart_uiIsRestricted_removeAllPreferences() {
        spyWifiTetherSettings();
        mSettings.mUnavailable = true;

        mSettings.onStart();

        verify(mPreferenceScreen).removeAll();
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    public void onStart_hotspotNotAvailable_removeAllPreferences() {
        spyWifiTetherSettings();
        when(mWifiRestriction.isHotspotAvailable(mContext)).thenReturn(false);

        mSettings.onStart();

        verify(mPreferenceScreen).removeAll();
        verify(mEmptyTextView).setText(anyInt());
    }

    @Test
    public void onSecuritySummaryChanged_canNotShowWifiHotspot_returnFalse() {
        int stringResId = com.android.settingslib.R.string.wifi_security_sae;
        mSettings.mWifiHotspotSecurity = mock(Preference.class);

        mSettings.onSecuritySummaryChanged(stringResId);

        verify(mSettings.mWifiHotspotSecurity).setSummary(stringResId);
    }

    @Test
    public void onSpeedSummaryChanged_canNotShowWifiHotspot_returnFalse() {
        int stringResId = R.string.wifi_hotspot_speed_summary_6g;
        mSettings.mWifiHotspotSpeed = mock(Preference.class);

        mSettings.onSpeedSummaryChanged(stringResId);

        verify(mSettings.mWifiHotspotSpeed).setSummary(stringResId);
    }

    @Test
    public void createPreferenceControllers_getPreferenceControllersNotEmpty() {
        assertThat(WifiTetherSettings.SEARCH_INDEX_DATA_PROVIDER.getPreferenceControllers(mContext))
                .isNotEmpty();
    }

    @Test
    public void createPreferenceControllers_hasAutoOffPreference() {
        assertThat(mSettings.createPreferenceControllers(mContext)
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
    public void isPageSearchEnabled_allReady_returnTrue() {
        setCanShowWifiHotspotCached(true);

        assertThat(WifiTetherSettings.SEARCH_INDEX_DATA_PROVIDER.isPageSearchEnabled(mContext))
                .isTrue();
    }

    @Test
    public void isPageSearchEnabled_isNotAdminUser_returnFalse() {
        doReturn(false).when(mUserManager).isAdminUser();

        assertThat(WifiTetherSettings.SEARCH_INDEX_DATA_PROVIDER.isPageSearchEnabled(mContext))
                .isFalse();
    }

    @Test
    public void isPageSearchEnabled_canNotShowWifiHotspot_returnFalse() {
        setCanShowWifiHotspotCached(false);

        assertThat(WifiTetherSettings.SEARCH_INDEX_DATA_PROVIDER.isPageSearchEnabled(mContext))
                .isFalse();
    }

    @Test
    public void setupSpeedFeature_speedFeatureIsAvailable_setVisibleToTrue() {
        mSettings.setupSpeedFeature(true);

        verify(mWifiHotspotSecurity).setVisible(true);
        verify(mWifiHotspotSpeed).setVisible(true);
        verify(mSecuritySummary).observe(any(), any());
        verify(mSpeedSummary).observe(any(), any());
    }

    @Test
    public void setupSpeedFeature_speedFeatureIsNotAvailable_setVisibleToFalse() {
        mSettings.setupSpeedFeature(false);

        verify(mWifiHotspotSecurity).setVisible(false);
        verify(mWifiHotspotSpeed).setVisible(false);
        verify(mSecuritySummary, never()).observe(any(), any());
        verify(mSpeedSummary, never()).observe(any(), any());
    }

    @Test
    public void onRestartingChanged_restartingTrue_setLoadingTrue() {
        doNothing().when(mSettings).setLoading(anyBoolean(), anyBoolean());

        mSettings.onRestartingChanged(true);

        verify(mMainSwitchBar).setVisibility(INVISIBLE);
        verify(mSettings).setLoading(true, false);
    }

    @Test
    public void setupInstantHotspot_featureNotAvailable_doNothing() {
        mSettings.setupInstantHotspot(false /* isFeatureAvailable */);

        verify(mSettings, never()).findPreference(KEY_INSTANT_HOTSPOT);
        verify(mWifiTetherViewModel, never()).getInstantHotspotSummary();
    }

    @Test
    public void setupInstantHotspot_featureAvailable_doSetup() {
        when(mWifiTetherViewModel.isInstantHotspotFeatureAvailable()).thenReturn(true);

        mSettings.setupInstantHotspot(true /* isFeatureAvailable */);

        verify(mSettings).findPreference(KEY_INSTANT_HOTSPOT);
        verify(mInstantHotspotSummary).observe(any(), any());
        verify(mInstantHotspot).setOnPreferenceClickListener(any());
    }

    @Test
    public void onInstantHotspotChanged_nullRecord_setVisibleFalse() {
        mSettings.onInstantHotspotChanged(null);

        verify(mInstantHotspot).setVisible(false);
    }

    @Test
    public void onInstantHotspotChanged_summaryNull_setVisibleFalse() {
        mSettings.onInstantHotspotChanged(null);

        verify(mInstantHotspot).setVisible(false);
    }

    @Test
    public void onInstantHotspotChanged_summaryNotNull_setVisibleAndSummary() {
        mSettings.onInstantHotspotChanged(SUMMARY);

        verify(mInstantHotspot).setVisible(true);
        verify(mInstantHotspot).setSummary(SUMMARY);
    }

    @Test
    public void buildNewConfig_speedFeatureIsAvailableAndPasswordChanged_bandShouldNotBeLost() {
        String newPassword = "new" + PASSWORD;
        SoftApConfiguration currentConfig = new SoftApConfiguration.Builder()
                .setPassphrase(PASSWORD, SECURITY_TYPE_WPA3_SAE)
                .setBand(BAND_2GHZ_5GHZ_6GHZ)
                .build();
        when(mWifiTetherViewModel.getSoftApConfiguration()).thenReturn(currentConfig);
        when(mWifiTetherViewModel.isSpeedFeatureAvailable()).thenReturn(true);
        when(mPasswordPreferenceController.getPasswordValidated(anyInt())).thenReturn(newPassword);

        SoftApConfiguration newConfig = mSettings.buildNewConfig();

        assertThat(newConfig.getBand()).isEqualTo(currentConfig.getBand());
    }

    @Test
    public void buildNewConfig_securityTypeChangeToOpen_setSecurityTypeCorrectly() {
        SoftApConfiguration currentConfig = new SoftApConfiguration.Builder()
                .setPassphrase(PASSWORD, SECURITY_TYPE_WPA3_SAE)
                .setBand(BAND_2GHZ_5GHZ_6GHZ)
                .build();
        when(mWifiTetherViewModel.getSoftApConfiguration()).thenReturn(currentConfig);
        when(mWifiTetherViewModel.isSpeedFeatureAvailable()).thenReturn(false);
        doNothing().when(mMaxCompatibilityPrefController)
                .setupMaximizeCompatibility(any(SoftApConfiguration.Builder.class));

        when(mSecurityPreferenceController.getSecurityType()).thenReturn(SECURITY_TYPE_OPEN);
        SoftApConfiguration newConfig = mSettings.buildNewConfig();

        assertThat(newConfig.getSecurityType()).isEqualTo(SECURITY_TYPE_OPEN);
    }

    @Test
    public void onRestartingChanged_restartingFalse_setLoadingFalse() {
        doNothing().when(mSettings).setLoading(anyBoolean(), anyBoolean());

        mSettings.onRestartingChanged(false);

        verify(mMainSwitchBar).setVisibility(VISIBLE);
        verify(mSettings).setLoading(false, false);
    }

    private void spyWifiTetherSettings() {
        mSettings = spy(new WifiTetherSettings(mWifiRestriction));
        final FragmentActivity activity = mock(FragmentActivity.class);
        when(mSettings.getActivity()).thenReturn(activity);
        when(mSettings.getContext()).thenReturn(mContext);
        final Resources.Theme theme = mContext.getTheme();
        when(activity.getTheme()).thenReturn(theme);
        when(activity.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        doNothing().when(mSettings).onCreatePreferences(any(Bundle.class), nullable(String.class));
        final FakeFeatureFactory fakeFeatureFactory = FakeFeatureFactory.setupForTest();
        ReflectionHelpers.setField(mSettings, "mDashboardFeatureProvider",
                fakeFeatureFactory.dashboardFeatureProvider);
        ReflectionHelpers.setField(mSettings, "mEmptyTextView", mEmptyTextView);
        doReturn(mPreferenceScreen).when(mSettings).getPreferenceScreen();

        mSettings.onCreate(Bundle.EMPTY);
    }

    @Implements(RestrictedDashboardFragment.class)
    public static final class ShadowRestrictedDashboardFragment {

        @Implementation
        public void onCreate(Bundle icicle) {
            // do nothing
        }

        @Implementation
        public boolean isUiRestricted() {
            return false;
        }
    }
}
