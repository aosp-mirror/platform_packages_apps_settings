/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.datausage;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkPolicy;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowConnectivityManager;
import com.android.settings.testutils.shadow.ShadowDataUsageSummary;
import com.android.settingslib.NetworkPolicyEditor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DataUsageSummaryTest {
    @Mock private ConnectivityManager mManager;
    private Context mContext;

    /**
     * This set up is contrived to get a passing test so that the build doesn't block without tests.
     * These tests should be updated as code gets refactored to improve testability.
     */

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowContext = ShadowApplication.getInstance();
        shadowContext.setSystemService(Context.CONNECTIVITY_SERVICE, mManager);
        mContext = shadowContext.getApplicationContext();
        when(mManager.isNetworkSupported(anyInt())).thenReturn(true);
    }

    @Test
    public void testMobileDataStatus() {
        boolean hasMobileData = DataUsageSummary.hasMobileData(mContext);
        assertThat(hasMobileData).isTrue();
    }

    @Test
    public void testUpdateNetworkRestrictionSummary_shouldSetSummary() {
        final DataUsageSummary dataUsageSummary = spy(new DataUsageSummary());
        final NetworkRestrictionsPreference preference = mock(NetworkRestrictionsPreference.class);
        final NetworkPolicyEditor policyEditor = mock(NetworkPolicyEditor.class);
        final WifiManager wifiManager = mock(WifiManager.class);
        ReflectionHelpers.setField(dataUsageSummary, "mPolicyEditor", policyEditor);
        ReflectionHelpers.setField(dataUsageSummary, "mWifiManager", wifiManager);
        when(wifiManager.getConfiguredNetworks()).thenReturn(new ArrayList<WifiConfiguration>());
        doReturn(mContext.getResources()).when(dataUsageSummary).getResources();

        dataUsageSummary.updateNetworkRestrictionSummary(preference);

        verify(preference).setSummary(mContext.getResources().getQuantityString(
            R.plurals.network_restrictions_summary, 0, 0));
    }

    @Test
    public void testIsMetered_noSsid_shouldReturnFalse() {
        final DataUsageSummary dataUsageSummary = new DataUsageSummary();
        final NetworkPolicyEditor policyEditor = mock(NetworkPolicyEditor.class);
        ReflectionHelpers.setField(dataUsageSummary, "mPolicyEditor", policyEditor);
        WifiConfiguration config = mock(WifiConfiguration.class);

        assertThat(dataUsageSummary.isMetered(config)).isFalse();
    }

    @Test
    public void testIsMetered_noNetworkPolicy_shouldReturnFalse() {
        final DataUsageSummary dataUsageSummary = new DataUsageSummary();
        final NetworkPolicyEditor policyEditor = mock(NetworkPolicyEditor.class);
        ReflectionHelpers.setField(dataUsageSummary, "mPolicyEditor", policyEditor);
        WifiConfiguration config = mock(WifiConfiguration.class);
        config.SSID = "network1";
        doReturn(null).when(policyEditor).getPolicyMaybeUnquoted(any());

        assertThat(dataUsageSummary.isMetered(config)).isFalse();
    }

    @Test
    public void testIsMetered_policyHasLimit_shouldReturnTrue() {
        final DataUsageSummary dataUsageSummary = new DataUsageSummary();
        final NetworkPolicyEditor policyEditor = mock(NetworkPolicyEditor.class);
        ReflectionHelpers.setField(dataUsageSummary, "mPolicyEditor", policyEditor);
        WifiConfiguration config = mock(WifiConfiguration.class);
        config.SSID = "network1";
        NetworkPolicy policy = mock(NetworkPolicy.class);
        policy.limitBytes = 100;
        doReturn(policy).when(policyEditor).getPolicyMaybeUnquoted(any());

        assertThat(dataUsageSummary.isMetered(config)).isTrue();
    }

    @Test
    public void testIsMetered_noPolicyLimit_shouldReturnMeteredValue() {
        final DataUsageSummary dataUsageSummary = new DataUsageSummary();
        final NetworkPolicyEditor policyEditor = mock(NetworkPolicyEditor.class);
        ReflectionHelpers.setField(dataUsageSummary, "mPolicyEditor", policyEditor);
        WifiConfiguration config = mock(WifiConfiguration.class);
        config.SSID = "network1";
        NetworkPolicy policy = mock(NetworkPolicy.class);
        policy.limitBytes = NetworkPolicy.LIMIT_DISABLED;
        doReturn(policy).when(policyEditor).getPolicyMaybeUnquoted(any());

        policy.metered = true;
        assertThat(dataUsageSummary.isMetered(config)).isTrue();

        policy.metered = false;
        assertThat(dataUsageSummary.isMetered(config)).isFalse();
    }

    @Test
    @Config(shadows = ShadowDataUsageSummary.class)
    public void testNonIndexableKeys_existInXmlLayout() {
        final Context context = RuntimeEnvironment.application;
        ShadowDataUsageSummary.IS_WIFI_SUPPORTED = true;
        ShadowDataUsageSummary.IS_MOBILE_DATA_SUPPORTED = true;
        final List<String> niks = DataUsageSummary.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(context);
        final List<String> keys = new ArrayList<>();

        keys.addAll(XmlTestUtils.getKeysFromPreferenceXml(context, R.xml.data_usage_wifi));
        keys.addAll(XmlTestUtils.getKeysFromPreferenceXml(context, R.xml.data_usage));
        keys.addAll(XmlTestUtils.getKeysFromPreferenceXml(context, R.xml.data_usage_cellular));

        assertThat(keys).containsAllIn(niks);
    }

    @Test
    @Config(shadows = ShadowDataUsageSummary.class)
    public void testNonIndexableKeys_hasMobileData_hasWifi_allNonIndexableKeysAdded() {
        ShadowDataUsageSummary.IS_WIFI_SUPPORTED = false;
        ShadowDataUsageSummary.IS_MOBILE_DATA_SUPPORTED = false;
        List<String> keys = DataUsageSummary.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);

        // Mobile data keys
        assertThat(keys).contains(DataUsageSummary.KEY_MOBILE_CATEGORY);
        assertThat(keys).contains(DataUsageSummary.KEY_MOBILE_DATA_USAGE_TOGGLE);
        assertThat(keys).contains(DataUsageSummary.KEY_MOBILE_DATA_USAGE);
        assertThat(keys).contains(DataUsageSummary.KEY_MOBILE_BILLING_CYCLE);

        // Wifi keys
        assertThat(keys).contains(DataUsageSummary.KEY_WIFI_DATA_USAGE);
        assertThat(keys).contains(DataUsageSummary.KEY_NETWORK_RESTRICTIONS);
        assertThat(keys).contains(DataUsageSummary.KEY_WIFI_USAGE_TITLE);
    }

    @Test
    @Config(shadows = ShadowDataUsageSummary.class)
    public void testNonIndexableKeys_noMobile_noWifi_limitedNonIndexableKeys() {
        ShadowDataUsageSummary.IS_WIFI_SUPPORTED = true;
        ShadowDataUsageSummary.IS_MOBILE_DATA_SUPPORTED = true;
        List<String> keys = DataUsageSummary.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);

        assertThat(keys).containsExactly(DataUsageSummary.KEY_WIFI_USAGE_TITLE);
    }
}
