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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowConnectivityManager;
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
}
