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
package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.UserInfo;
import android.provider.SearchIndexableResource;
import android.view.Menu;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.drawer.CategoryKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class NetworkDashboardFragmentTest {

    @Mock
    private Context mContext;
    @Mock
    private UserInfo mUserInfo;

    private NetworkDashboardFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFragment = new NetworkDashboardFragment();
    }

    @Test
    public void testCategory_isNetwork() {
        assertThat(mFragment.getCategoryKey()).isEqualTo(CategoryKey.CATEGORY_NETWORK);
    }

    @Test
    public void testSearchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
                NetworkDashboardFragment.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(
                        ShadowApplication.getInstance().getApplicationContext(),
                        true /* enabled */);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes.get(0).xmlResId).isEqualTo(mFragment.getPreferenceScreenResId());
    }

    @Test
    public void testPrepareActionBar_networkResetShouldBeCreated() {
        final NetworkResetActionMenuController resetController =
                mock(NetworkResetActionMenuController.class);
        ReflectionHelpers.setField(mFragment, "mNetworkResetController", resetController);

        mFragment.onCreateOptionsMenu(null, null);

        verify(resetController).buildMenuItem(nullable(Menu.class));
    }

    @Test
    public void testSummaryProvider_hasMobileAndHotspot_shouldReturnMobileSummary() {
        final MobileNetworkPreferenceController mobileNetworkPreferenceController =
                mock(MobileNetworkPreferenceController.class);
        final TetherPreferenceController tetherPreferenceController =
                mock(TetherPreferenceController.class);

        final SummaryLoader summaryLoader = mock(SummaryLoader.class);
        final SummaryLoader.SummaryProvider provider =
                new NetworkDashboardFragment.SummaryProvider(mContext, summaryLoader,
                        mobileNetworkPreferenceController, tetherPreferenceController);

        provider.setListening(false);

        verifyZeroInteractions(summaryLoader);

        when(mobileNetworkPreferenceController.isAvailable()).thenReturn(true);
        when(tetherPreferenceController.isAvailable()).thenReturn(true);

        provider.setListening(true);

        verify(mContext).getString(R.string.wifi_settings_title);
        verify(mContext).getString(R.string.network_dashboard_summary_data_usage);
        verify(mContext).getString(R.string.network_dashboard_summary_hotspot);
        verify(mContext).getString(R.string.network_dashboard_summary_mobile);
        verify(mContext, times(3)).getString(R.string.join_many_items_middle, null, null);
    }

    @Test
    public void testSummaryProvider_noMobileOrHotspot_shouldReturnSimpleSummary() {
        final MobileNetworkPreferenceController mobileNetworkPreferenceController =
                mock(MobileNetworkPreferenceController.class);
        final TetherPreferenceController tetherPreferenceController =
                mock(TetherPreferenceController.class);

        final SummaryLoader summaryLoader = mock(SummaryLoader.class);
        final SummaryLoader.SummaryProvider provider =
                new NetworkDashboardFragment.SummaryProvider(mContext, summaryLoader,
                        mobileNetworkPreferenceController, tetherPreferenceController);

        provider.setListening(false);

        verifyZeroInteractions(summaryLoader);

        when(mobileNetworkPreferenceController.isAvailable()).thenReturn(false);
        when(tetherPreferenceController.isAvailable()).thenReturn(false);

        provider.setListening(true);

        verify(mContext).getString(R.string.wifi_settings_title);
        verify(mContext).getString(R.string.network_dashboard_summary_data_usage);
        verify(mContext).getString(R.string.join_many_items_middle, null, null);
    }

}