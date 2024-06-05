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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.res.Resources;
import android.os.UserManager;
import android.telephony.SubscriptionManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowDashboardFragment;
import com.android.settings.testutils.shadow.ShadowDataUsageUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;

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
import org.robolectric.shadows.ShadowSubscriptionManager;

@Config(shadows = {
        ShadowUtils.class,
        ShadowDataUsageUtils.class,
        ShadowDashboardFragment.class,
        ShadowUserManager.class,
})
@RunWith(RobolectricTestRunner.class)
public class DataUsageSummaryTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private UserManager mUserManager;

    private DataUsageSummary mDataUsageSummary;

    /**
     * This set up is contrived to get a passing test so that the build doesn't block without tests.
     * These tests should be updated as code gets refactored to improve testability.
     */

    @Before
    public void setUp() {
        doReturn(mUserManager).when(mContext).getSystemService(UserManager.class);
        doReturn(false).when(mUserManager).isGuestUser();

        ShadowUserManager.getShadow().setIsAdminUser(true);

        Resources mResources = spy(mContext.getResources());
        doReturn(mResources).when(mContext).getResources();
        doReturn(true).when(mResources).getBoolean(R.bool.config_show_sim_info);

        mDataUsageSummary = spy(new DataUsageSummary());
        doReturn(mContext).when(mDataUsageSummary).getContext();
        doNothing().when(mDataUsageSummary).enableProxySubscriptionManager(any());
        doReturn(true).when(mDataUsageSummary).removePreference(anyString());
        doNothing().when(mDataUsageSummary).addWifiSection();
    }

    @Test
    public void formatUsage_shouldLookLikeFormatFileSize() {
        final long usage = 2147483648L; // 2GB
        final String formattedUsage =
                DataUsageSummary.formatUsage(mContext, "^1", usage).toString();
        final CharSequence formattedInIECUnit = DataUsageUtils.formatDataUsage(mContext, usage);
        assertThat(formattedUsage).isEqualTo(formattedInIECUnit);
    }

    @Test
    @Config(shadows = ShadowSubscriptionManager.class)
    public void configuration_withSim_shouldShowMobileAndWifi() {
        ShadowDataUsageUtils.IS_MOBILE_DATA_SUPPORTED = true;
        ShadowDataUsageUtils.IS_WIFI_SUPPORTED = true;
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(1);

        final DataUsageSummary dataUsageSummary = spy(new DataUsageSummary());
        doNothing().when(dataUsageSummary).enableProxySubscriptionManager(any());
        doReturn(true).when(dataUsageSummary).hasActiveSubscription();
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

        final DataUsageSummary dataUsageSummary = spy(new DataUsageSummary());
        doNothing().when(dataUsageSummary).enableProxySubscriptionManager(any());
        doReturn(false).when(dataUsageSummary).hasActiveSubscription();
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

        final DataUsageSummary dataUsageSummary = spy(new DataUsageSummary());
        doNothing().when(dataUsageSummary).enableProxySubscriptionManager(any());
        doReturn(false).when(dataUsageSummary).hasActiveSubscription();
        doReturn(mContext).when(dataUsageSummary).getContext();

        doReturn(true).when(dataUsageSummary).removePreference(anyString());
        doNothing().when(dataUsageSummary).addWifiSection();
        doNothing().when(dataUsageSummary).addMobileSection(1);

        dataUsageSummary.onCreate(null);

        verify(dataUsageSummary).addWifiSection();
        verify(dataUsageSummary, never()).addMobileSection(anyInt());
    }

    @Test
    @Config(shadows = ShadowSubscriptionManager.class)
    public void configuration_invalidDataSusbscription_shouldShowWifiSectionOnly() {
        ShadowDataUsageUtils.IS_MOBILE_DATA_SUPPORTED = true;
        ShadowDataUsageUtils.IS_WIFI_SUPPORTED = true;
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        final DataUsageSummary dataUsageSummary = spy(new DataUsageSummary());
        doNothing().when(dataUsageSummary).enableProxySubscriptionManager(any());
        doReturn(false).when(dataUsageSummary).hasActiveSubscription();
        doReturn(mContext).when(dataUsageSummary).getContext();

        doReturn(true).when(dataUsageSummary).removePreference(anyString());
        doNothing().when(dataUsageSummary).addWifiSection();
        doNothing().when(dataUsageSummary).addMobileSection(1);

        dataUsageSummary.onCreate(null);

        verify(dataUsageSummary).addWifiSection();
        verify(dataUsageSummary, never()).addMobileSection(anyInt());
    }

    @Test
    public void onCreate_isNotGuestUser_shouldNotFinish() {
        doReturn(false).when(mUserManager).isGuestUser();

        mDataUsageSummary.onCreate(null);

        verify(mDataUsageSummary, never()).finish();
    }

    @Test
    public void onCreate_isGuestUser_shouldFinish() {
        doReturn(true).when(mUserManager).isGuestUser();

        mDataUsageSummary.onCreate(null);

        verify(mDataUsageSummary).finish();
    }
}
