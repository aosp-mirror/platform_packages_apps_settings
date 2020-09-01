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
 * limitations under the License
 */
package com.android.settings.wifi;

import static android.net.wifi.WifiManager.WIFI_STATE_ENABLED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.EAPConstants;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.hotspot2.pps.Credential;
import android.net.wifi.hotspot2.pps.HomeSp;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.UserManager;
import android.provider.Settings;
import android.util.FeatureFlagUtils;
import android.view.ContextMenu;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.datausage.DataUsagePreference;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowDataUsageUtils;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.WifiTracker;
import com.android.settingslib.wifi.WifiTrackerFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class WifiSettingsTest {

    private static final int NUM_NETWORKS = 4;

    @Mock
    private WifiTracker mWifiTracker;
    @Mock
    private PowerManager mPowerManager;
    @Mock
    private DataUsagePreference mDataUsagePreference;
    @Mock
    private RecyclerView mRecyclerView;
    @Mock
    private RecyclerView.Adapter mRecyclerViewAdapter;
    @Mock
    private View mHeaderView;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private Intent mActivityIntent;
    @Mock
    private SwitchBar mSwitchBar;
    @Mock
    private WifiInfo mWifiInfo;
    @Mock
    private PackageManager mPackageManager;
    private Context mContext;
    private WifiSettings mWifiSettings;
    private FakeFeatureFactory mFakeFeatureFactory;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mWifiSettings = spy(new WifiSettings());
        doReturn(mContext).when(mWifiSettings).getContext();
        doReturn(mRecyclerViewAdapter).when(mRecyclerView).getAdapter();
        doReturn(mRecyclerView).when(mWifiSettings).getListView();
        doReturn(mPowerManager).when(mContext).getSystemService(PowerManager.class);
        doReturn(mHeaderView).when(mWifiSettings).setPinnedHeaderView(anyInt());
        doReturn(mWifiInfo).when(mWifiManager).getConnectionInfo();
        doReturn(mWifiManager).when(mWifiTracker).getManager();
        mWifiSettings.mAddWifiNetworkPreference = new AddWifiNetworkPreference(mContext);
        mWifiSettings.mSavedNetworksPreference = new Preference(mContext);
        mWifiSettings.mConfigureWifiSettingsPreference = new Preference(mContext);
        mWifiSettings.mWifiTracker = mWifiTracker;
        mWifiSettings.mWifiManager = mWifiManager;
        mWifiSettings.mConnectivityManager = mConnectivityManager;
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = mFakeFeatureFactory.getMetricsFeatureProvider();
        ReflectionHelpers.setField(mWifiSettings, "mMetricsFeatureProvider",
                mMetricsFeatureProvider);
        WifiTrackerFactory.setTestingWifiTracker(mWifiTracker);
    }

    @Test
    public void addNetworkFragmentSendResult_onActivityResult_shouldHandleEvent() {
        final WifiSettings wifiSettings = spy(new WifiSettings());
        final Intent intent = new Intent();
        doNothing().when(wifiSettings).handleAddNetworkRequest(anyInt(), any(Intent.class));

        wifiSettings.onActivityResult(WifiSettings.ADD_NETWORK_REQUEST, Activity.RESULT_OK, intent);

        verify(wifiSettings).handleAddNetworkRequest(anyInt(), any(Intent.class));
    }

    private List<WifiConfiguration> createMockWifiConfigurations(int count) {
        final List<WifiConfiguration> mockConfigs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            mockConfigs.add(new WifiConfiguration());
        }
        return mockConfigs;
    }

    private List<PasspointConfiguration> createMockPasspointConfigurations(int count) {
        final List<PasspointConfiguration> mockConfigs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final HomeSp sp = new HomeSp();
            sp.setFqdn("fqdn");
            final PasspointConfiguration config = new PasspointConfiguration();
            config.setHomeSp(sp);
            Credential.SimCredential simCredential = new Credential.SimCredential();
            Credential credential = new Credential();
            credential.setRealm("test.example.com");
            simCredential.setImsi("12345*");
            simCredential.setEapType(EAPConstants.EAP_SIM);
            credential.setSimCredential(simCredential);
            config.setCredential(credential);
            mockConfigs.add(config);
        }
        return mockConfigs;
    }

    static NetworkCapabilities makeCaptivePortalNetworkCapabilities() {
        final NetworkCapabilities capabilities = new NetworkCapabilities();
        capabilities.clearAll();
        capabilities.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        capabilities.addCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL);
        return capabilities;
    }

    @Test
    public void setAdditionalSettingsSummaries_hasSavedNetwork_preferenceVisible() {
        when(mWifiManager.getConfiguredNetworks())
                .thenReturn(createMockWifiConfigurations(NUM_NETWORKS));

        mWifiSettings.setAdditionalSettingsSummaries();

        assertThat(mWifiSettings.mSavedNetworksPreference.isVisible()).isTrue();
        assertThat(mWifiSettings.mSavedNetworksPreference.getSummary()).isEqualTo(
                mContext.getResources().getQuantityString(
                        R.plurals.wifi_saved_access_points_summary,
                        NUM_NETWORKS, NUM_NETWORKS));
    }

    @Test
    public void setAdditionalSettingsSummaries_hasSavedPasspointNetwork_preferenceVisible() {
        when(mWifiManager.getPasspointConfigurations())
                .thenReturn(createMockPasspointConfigurations(NUM_NETWORKS));

        mWifiSettings.setAdditionalSettingsSummaries();

        assertThat(mWifiSettings.mSavedNetworksPreference.isVisible()).isTrue();
        assertThat(mWifiSettings.mSavedNetworksPreference.getSummary()).isEqualTo(
                mContext.getResources().getQuantityString(
                        R.plurals.wifi_saved_passpoint_access_points_summary,
                        NUM_NETWORKS, NUM_NETWORKS));
    }

    @Test
    public void setAdditionalSettingsSummaries_hasTwoKindsSavedNetwork_preferenceVisible() {
        when(mWifiManager.getConfiguredNetworks())
                .thenReturn(createMockWifiConfigurations(NUM_NETWORKS));
        when(mWifiManager.getPasspointConfigurations())
                .thenReturn(createMockPasspointConfigurations(NUM_NETWORKS));

        mWifiSettings.setAdditionalSettingsSummaries();

        assertThat(mWifiSettings.mSavedNetworksPreference.isVisible()).isTrue();
        assertThat(mWifiSettings.mSavedNetworksPreference.getSummary()).isEqualTo(
                mContext.getResources().getQuantityString(
                        R.plurals.wifi_saved_all_access_points_summary,
                        NUM_NETWORKS*2, NUM_NETWORKS*2));
    }

    @Test
    public void setAdditionalSettingsSummaries_noSavedNetwork_preferenceInvisible() {
        when(mWifiManager.getConfiguredNetworks())
                .thenReturn(createMockWifiConfigurations(0 /* count */));

        mWifiSettings.setAdditionalSettingsSummaries();

        assertThat(mWifiSettings.mSavedNetworksPreference.isVisible()).isFalse();
    }

    @Test
    public void setAdditionalSettingsSummaries_wifiWakeupEnabled_displayOn() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        when(mWifiManager.isAutoWakeupEnabled()).thenReturn(true);
        when(mWifiManager.isScanAlwaysAvailable()).thenReturn(true);
        Settings.Global.putInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0);
        when(mPowerManager.isPowerSaveMode()).thenReturn(false);

        mWifiSettings.setAdditionalSettingsSummaries();

        assertThat(mWifiSettings.mConfigureWifiSettingsPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.wifi_configure_settings_preference_summary_wakeup_on));
    }

    @Test
    public void setAdditionalSettingsSummaries_wifiWakeupDisabled_displayOff() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        when(mWifiManager.isAutoWakeupEnabled()).thenReturn(false);

        mWifiSettings.setAdditionalSettingsSummaries();

        assertThat(mWifiSettings.mConfigureWifiSettingsPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.wifi_configure_settings_preference_summary_wakeup_off));
    }

    @Test
    public void checkAddWifiNetworkPrefernce_preferenceVisible() {
        assertThat(mWifiSettings.mAddWifiNetworkPreference.isVisible()).isTrue();
        assertThat(mWifiSettings.mAddWifiNetworkPreference.getTitle()).isEqualTo(
                mContext.getString(R.string.wifi_add_network));
    }

    private void setUpForOnCreate() {
        final SettingsActivity activity = mock(SettingsActivity.class);
        when(activity.getSwitchBar()).thenReturn(mSwitchBar);
        when(mWifiSettings.getActivity()).thenReturn(activity);
        final Resources.Theme theme = mContext.getTheme();
        when(activity.getTheme()).thenReturn(theme);
        when(activity.getIntent()).thenReturn(mActivityIntent);
        UserManager userManager = mock(UserManager.class);
        when(activity.getSystemService(Context.USER_SERVICE))
                .thenReturn(userManager);
        when(mWifiSettings.findPreference(WifiSettings.PREF_KEY_DATA_USAGE))
                .thenReturn(mDataUsagePreference);
        when(activity.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        when(activity.getSystemService(ConnectivityManager.class)).thenReturn(mConnectivityManager);
        when(activity.getPackageManager()).thenReturn(mPackageManager);
    }

    @Test
    @Config(shadows = {ShadowDataUsageUtils.class, ShadowFragment.class})
    public void checkDataUsagePreference_perferenceInvisibleIfWifiNotSupported() {
        if (FeatureFlagUtils.isEnabled(mContext, FeatureFlagUtils.SETTINGS_WIFITRACKER2)) {
            return;
        }

        setUpForOnCreate();
        ShadowDataUsageUtils.IS_WIFI_SUPPORTED = false;

        mWifiSettings.onCreate(Bundle.EMPTY);

        verify(mDataUsagePreference).setVisible(false);
    }

    @Test
    @Config(shadows = {ShadowDataUsageUtils.class, ShadowFragment.class})
    public void checkDataUsagePreference_perferenceVisibleIfWifiSupported() {
        if (FeatureFlagUtils.isEnabled(mContext, FeatureFlagUtils.SETTINGS_WIFITRACKER2)) {
            return;
        }

        setUpForOnCreate();
        ShadowDataUsageUtils.IS_WIFI_SUPPORTED = true;

        mWifiSettings.onCreate(Bundle.EMPTY);

        verify(mDataUsagePreference).setVisible(true);
        verify(mDataUsagePreference).setTemplate(any(), eq(0) /*subId*/, eq(null) /*service*/);
    }

    @Test
    public void onCreateContextMenu_shouldHaveForgetMenuForConnectedAccessPreference() {
        final FragmentActivity mockActivity = mock(FragmentActivity.class);
        when(mockActivity.getApplicationContext()).thenReturn(mContext);
        when(mWifiSettings.getActivity()).thenReturn(mockActivity);

        final AccessPoint accessPoint = mock(AccessPoint.class);
        when(accessPoint.isConnectable()).thenReturn(false);
        when(accessPoint.isSaved()).thenReturn(true);
        when(accessPoint.isActive()).thenReturn(true);

        final ConnectedAccessPointPreference connectedPreference =
            mWifiSettings.createConnectedAccessPointPreference(accessPoint, mContext);
        final View view = mock(View.class);
        when(view.getTag()).thenReturn(connectedPreference);

        final ContextMenu menu = mock(ContextMenu.class);
        mWifiSettings.onCreateContextMenu(menu, view, null /* info */);

        verify(menu).add(anyInt(), eq(WifiSettings.MENU_ID_FORGET), anyInt(), anyInt());
    }

    @Test
    public void onCreateAdapter_hasStableIdsTrue() {
        final PreferenceScreen preferenceScreen = mock(PreferenceScreen.class);
        when(preferenceScreen.getContext()).thenReturn(mContext);

        RecyclerView.Adapter adapter = mWifiSettings.onCreateAdapter(preferenceScreen);

        assertThat(adapter.hasStableIds()).isTrue();
    }

    @Test
    @Config(shadows = {ShadowDataUsageUtils.class, ShadowFragment.class})
    public void clickOnWifiNetworkWith_shouldStartCaptivePortalApp() {
        if (FeatureFlagUtils.isEnabled(mContext, FeatureFlagUtils.SETTINGS_WIFITRACKER2)) {
            return;
        }

        when(mWifiManager.getConfiguredNetworks()).thenReturn(createMockWifiConfigurations(
                NUM_NETWORKS));
        when(mWifiTracker.isConnected()).thenReturn(true);

        final AccessPoint accessPointActive = mock(AccessPoint.class);
        when(accessPointActive.isActive()).thenReturn(true);
        when(accessPointActive.isSaved()).thenReturn(false);
        when(accessPointActive.getConfig()).thenReturn(mock(WifiConfiguration.class));

        final AccessPoint accessPointInactive = mock(AccessPoint.class);
        when(accessPointInactive.isActive()).thenReturn(false);
        when(accessPointInactive.isSaved()).thenReturn(false);
        when(accessPointInactive.getConfig()).thenReturn(mock(WifiConfiguration.class));

        when(mWifiTracker.getAccessPoints()).thenReturn(Arrays.asList(accessPointActive,
                accessPointInactive));
        when(mWifiManager.getWifiState()).thenReturn(WIFI_STATE_ENABLED);
        when(mWifiManager.isWifiEnabled()).thenReturn(true);

        final Network network = mock(Network.class);
        when(mWifiManager.getCurrentNetwork()).thenReturn(network);

        // Simulate activity creation cycle
        setUpForOnCreate();
        ShadowDataUsageUtils.IS_WIFI_SUPPORTED = true;
        mWifiSettings.onCreate(Bundle.EMPTY);
        mWifiSettings.onActivityCreated(null);
        mWifiSettings.onViewCreated(new View(mContext), new Bundle());
        mWifiSettings.onStart();

        // Click on open network
        final Preference openWifiPref = new LongPressAccessPointPreference(accessPointInactive,
                mContext, null,
                false /* forSavedNetworks */, R.drawable.ic_wifi_signal_0,
                null);
        mWifiSettings.onPreferenceTreeClick(openWifiPref);

        // Ensure connect() was called, and fake success.
        ArgumentCaptor<WifiManager.ActionListener> wifiCallbackCaptor = ArgumentCaptor.forClass(
                WifiManager.ActionListener.class);
        verify(mWifiManager).connect(any(WifiConfiguration.class), wifiCallbackCaptor.capture());
        wifiCallbackCaptor.getValue().onSuccess();

        // Simulate capability change
        mWifiSettings.mCaptivePortalNetworkCallback.onCapabilitiesChanged(network,
                makeCaptivePortalNetworkCapabilities());

        // Ensure CP was called
        verify(mConnectivityManager).startCaptivePortalApp(eq(network));
    }
}
