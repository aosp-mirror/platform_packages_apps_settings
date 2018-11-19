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
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.NetworkPolicyManager;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.SettingsShadowResourcesImpl;
import com.android.settings.testutils.shadow.ShadowDashboardFragment;
import com.android.settings.testutils.shadow.ShadowDataUsageUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@Config(shadows = {
        SettingsShadowResourcesImpl.class,
        SettingsShadowResources.SettingsShadowTheme.class,
        ShadowUtils.class,
        ShadowDataUsageUtils.class,
        ShadowDashboardFragment.class,
        ShadowUserManager.class,
})
@RunWith(SettingsRobolectricTestRunner.class)
public class DataUsageSummaryTest {

    @Mock
    private SummaryLoader mSummaryLoader;
    @Mock
    private NetworkPolicyManager mNetworkPolicyManager;
    @Mock
    private NetworkStatsManager mNetworkStatsManager;
    private Context mContext;
    private FragmentActivity mActivity;
    private SummaryLoader.SummaryProvider mSummaryProvider;

    /**
     * This set up is contrived to get a passing test so that the build doesn't block without tests.
     * These tests should be updated as code gets refactored to improve testability.
     */

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowContext = ShadowApplication.getInstance();
        ShadowUserManager.getShadow().setIsAdminUser(true);
        shadowContext.setSystemService(Context.NETWORK_POLICY_SERVICE, mNetworkPolicyManager);

        mContext = RuntimeEnvironment.application;
        mActivity = spy(Robolectric.buildActivity(FragmentActivity.class).get());
        doReturn(mNetworkStatsManager).when(mActivity).getSystemService(NetworkStatsManager.class);

        mSummaryProvider = DataUsageSummary.SUMMARY_PROVIDER_FACTORY
                .createSummaryProvider(mActivity, mSummaryLoader);
    }

    @After
    public void tearDown() {
        ShadowUserManager.getShadow().reset();
    }

    @Test
    public void formatUsage_shouldLookLikeFormatFileSize() {
        SettingsShadowResources.overrideResource(com.android.internal.R.string.fileSizeSuffix,
                "%1$s %2$s");
        final long usage = 2147483648L; // 2GB
        final String formattedUsage =
                DataUsageSummary.formatUsage(mContext, "^1", usage).toString();
        final CharSequence formattedInIECUnit = DataUsageUtils.formatDataUsage(mContext, usage);
        assertThat(formattedUsage).isEqualTo(formattedInIECUnit);
    }

    @Test
    public void setListening_shouldBlankSummaryWithNoSim() {
        ShadowDataUsageUtils.HAS_SIM = false;
        mSummaryProvider.setListening(true);
        verify(mSummaryLoader).setSummary(mSummaryProvider, null);
    }

    @Test
    public void setListening_shouldSetSummaryWithSim() {
        ShadowDataUsageUtils.HAS_SIM = true;
        mSummaryProvider.setListening(true);
        verify(mSummaryLoader).setSummary(anyObject(), endsWith(" of data used"));
    }

    @Test
    public void configuration_withSim_shouldShowMobileAndWifi() {
        ShadowDataUsageUtils.IS_MOBILE_DATA_SUPPORTED = true;
        ShadowDataUsageUtils.IS_WIFI_SUPPORTED = true;
        ShadowDataUsageUtils.DEFAULT_SUBSCRIPTION_ID = 1;
        ShadowDataUsageUtils.HAS_SIM = true;

        final DataUsageSummary dataUsageSummary = spy(new DataUsageSummary());
        doReturn(mContext).when(dataUsageSummary).getContext();

        doReturn(true).when(dataUsageSummary).removePreference(anyString());
        doNothing().when(dataUsageSummary).addWifiSection();
        doNothing().when(dataUsageSummary).addMobileSection(1);

        dataUsageSummary.onCreate(null);

        verify(dataUsageSummary).addWifiSection();
        verify(dataUsageSummary).addMobileSection(anyInt());
    }

    @Test
    public void configuration_withoutSim_shouldShowWifiSectionOnly() {
        ShadowDataUsageUtils.IS_MOBILE_DATA_SUPPORTED = true;
        ShadowDataUsageUtils.IS_WIFI_SUPPORTED = true;
        ShadowDataUsageUtils.HAS_SIM = false;

        final DataUsageSummary dataUsageSummary = spy(new DataUsageSummary());
        doReturn(mContext).when(dataUsageSummary).getContext();

        doReturn(true).when(dataUsageSummary).removePreference(anyString());
        doNothing().when(dataUsageSummary).addWifiSection();
        doNothing().when(dataUsageSummary).addMobileSection(1);

        dataUsageSummary.onCreate(null);

        verify(dataUsageSummary).addWifiSection();
        verify(dataUsageSummary, never()).addMobileSection(anyInt());
    }

    @Test
    public void configuration_withoutMobile_shouldShowWifiSectionOnly() {
        ShadowDataUsageUtils.IS_MOBILE_DATA_SUPPORTED = false;
        ShadowDataUsageUtils.IS_WIFI_SUPPORTED = true;
        ShadowDataUsageUtils.HAS_SIM = false;

        final DataUsageSummary dataUsageSummary = spy(new DataUsageSummary());
        doReturn(mContext).when(dataUsageSummary).getContext();

        doReturn(true).when(dataUsageSummary).removePreference(anyString());
        doNothing().when(dataUsageSummary).addWifiSection();
        doNothing().when(dataUsageSummary).addMobileSection(1);

        dataUsageSummary.onCreate(null);

        verify(dataUsageSummary).addWifiSection();
        verify(dataUsageSummary, never()).addMobileSection(anyInt());
    }
}
