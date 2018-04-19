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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.drawer.CategoryKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class NetworkDashboardFragmentTest {

    private Context mContext;

    private NetworkDashboardFragment mFragment;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mFragment = new NetworkDashboardFragment();
    }

    @Test
    public void getCategoryKey_isNetwork() {
        assertThat(mFragment.getCategoryKey()).isEqualTo(CategoryKey.CATEGORY_NETWORK);
    }

    @Test
    public void getXmlResourcesToIndex_shouldIncludeFragmentXml() {
        final List<SearchIndexableResource> indexRes =
                NetworkDashboardFragment.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(
                        mContext,
                        true /* enabled */);

        assertThat(indexRes).hasSize(1);
        assertThat(indexRes.get(0).xmlResId).isEqualTo(mFragment.getPreferenceScreenResId());
    }

    @Test
    public void summaryProviderSetListening_hasMobileAndHotspot_shouldReturnMobileSummary() {
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

        verify(summaryLoader).setSummary(provider, "Wi\u2011Fi, mobile, data usage, and hotspot");
    }

    @Test
    public void summaryProviderSetListening_noMobileOrHotspot_shouldReturnSimpleSummary() {
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

        verify(summaryLoader).setSummary(provider, "Wi\u2011Fi and data usage");
    }
}
