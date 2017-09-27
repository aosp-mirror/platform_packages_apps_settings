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

package com.android.settings.datausage;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkPolicy;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowDataUsageUtils;
import com.android.settingslib.NetworkPolicyEditor;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DataPlanUsageSummaryTest {
    @Mock
    private ConnectivityManager mManager;

    private Context mContext;
    private DataPlanUsageSummary mDataUsageSummary;
    private NetworkPolicyEditor mPolicyEditor;
    private WifiConfiguration mWifiConfiguration;
    private NetworkPolicy mNetworkPolicy;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowContext = ShadowApplication.getInstance();
        shadowContext.setSystemService(Context.CONNECTIVITY_SERVICE, mManager);
        mContext = shadowContext.getApplicationContext();
        when(mManager.isNetworkSupported(anyInt())).thenReturn(true);
    }

    @Test
    public void testUpdateNetworkRestrictionSummary_shouldSetSummary() {
        mDataUsageSummary = spy(new DataPlanUsageSummary());
        NetworkRestrictionsPreference preference = mock(NetworkRestrictionsPreference.class);
        mPolicyEditor = mock(NetworkPolicyEditor.class);
        WifiManager wifiManager = mock(WifiManager.class);
        ReflectionHelpers.setField(mDataUsageSummary, "mPolicyEditor", mPolicyEditor);
        ReflectionHelpers.setField(mDataUsageSummary, "mWifiManager", wifiManager);
        when(wifiManager.getConfiguredNetworks()).thenReturn(new ArrayList<>());
        doReturn(mContext.getResources()).when(mDataUsageSummary).getResources();

        mDataUsageSummary.updateNetworkRestrictionSummary(preference);

        verify(preference).setSummary(mContext.getResources().getQuantityString(
                R.plurals.network_restrictions_summary, 0, 0));
    }

    @Test
    public void testIsMetered_noSsid_shouldReturnFalse() {
        initTest();

        assertThat(mDataUsageSummary.isMetered(mWifiConfiguration)).isFalse();
    }

    @Test
    public void testIsMetered_noNetworkPolicy_shouldReturnFalse() {
        initTest();
        mWifiConfiguration.SSID = "network1";
        doReturn(null).when(mPolicyEditor).getPolicyMaybeUnquoted(any());

        assertThat(mDataUsageSummary.isMetered(mWifiConfiguration)).isFalse();
    }

    @Test
    public void testIsMetered_policyHasLimit_shouldReturnTrue() {
        initTest();
        mWifiConfiguration.SSID = "network1";
        mNetworkPolicy = mock(NetworkPolicy.class);
        mNetworkPolicy.limitBytes = 100;
        doReturn(mNetworkPolicy).when(mPolicyEditor).getPolicyMaybeUnquoted(any());

        assertThat(mDataUsageSummary.isMetered(mWifiConfiguration)).isTrue();
    }

    @Test
    public void testIsMetered_noPolicyLimit_shouldReturnMeteredValue() {
        initTest();
        mWifiConfiguration.SSID = "network1";
        mNetworkPolicy = mock(NetworkPolicy.class);
        mNetworkPolicy.limitBytes = NetworkPolicy.LIMIT_DISABLED;
        doReturn(mNetworkPolicy).when(mPolicyEditor).getPolicyMaybeUnquoted(any());

        mNetworkPolicy.metered = true;
        assertThat(mDataUsageSummary.isMetered(mWifiConfiguration)).isTrue();

        mNetworkPolicy.metered = false;
        assertThat(mDataUsageSummary.isMetered(mWifiConfiguration)).isFalse();
    }

    @Test
    @Config(shadows = ShadowDataUsageUtils.class)
    public void testNonIndexableKeys_existInXmlLayout() {
        final Context context = RuntimeEnvironment.application;
        ShadowDataUsageUtils.IS_WIFI_SUPPORTED = true;
        ShadowDataUsageUtils.IS_MOBILE_DATA_SUPPORTED = true;
        final List<String> niks = DataPlanUsageSummary.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(context);
        final List<String> keys = new ArrayList<>();

        keys.addAll(XmlTestUtils.getKeysFromPreferenceXml(context, R.xml.data_usage_wifi));
        keys.addAll(XmlTestUtils.getKeysFromPreferenceXml(context, R.xml.data_usage));
        keys.addAll(XmlTestUtils.getKeysFromPreferenceXml(context,
                R.xml.data_plan_usage_cell_data_preference_screen));

        assertThat(keys).containsAllIn(niks);
    }

    @Test
    @Config(shadows = ShadowDataUsageUtils.class)
    public void testNonIndexableKeys_hasMobileData_hasWifi_allNonIndexableKeysAdded() {
        ShadowDataUsageUtils.IS_WIFI_SUPPORTED = false;
        ShadowDataUsageUtils.IS_MOBILE_DATA_SUPPORTED = false;
        List<String> keys = DataPlanUsageSummary.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);

        // Mobile data keys
        assertThat(keys).contains(DataPlanUsageSummary.KEY_MOBILE_USAGE_TITLE);
        assertThat(keys).contains(DataPlanUsageSummary.KEY_MOBILE_DATA_USAGE_TOGGLE);

        // Wifi keys
        assertThat(keys).contains(DataPlanUsageSummary.KEY_WIFI_DATA_USAGE);
        assertThat(keys).contains(DataPlanUsageSummary.KEY_NETWORK_RESTRICTIONS);
        assertThat(keys).contains(DataPlanUsageSummary.KEY_WIFI_USAGE_TITLE);
    }

    @Test
    @Config(shadows = ShadowDataUsageUtils.class)
    public void testNonIndexableKeys_noMobile_noWifi_limitedNonIndexableKeys() {
        ShadowDataUsageUtils.IS_WIFI_SUPPORTED = true;
        ShadowDataUsageUtils.IS_MOBILE_DATA_SUPPORTED = true;
        List<String> keys = DataPlanUsageSummary.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);

        assertThat(keys).containsExactly(DataPlanUsageSummary.KEY_WIFI_USAGE_TITLE);
    }

    private void initTest() {
        mDataUsageSummary = new DataPlanUsageSummary();
        mPolicyEditor = mock(NetworkPolicyEditor.class);
        ReflectionHelpers.setField(mDataUsageSummary, "mPolicyEditor", mPolicyEditor);
        mWifiConfiguration = mock(WifiConfiguration.class);
    }
}
