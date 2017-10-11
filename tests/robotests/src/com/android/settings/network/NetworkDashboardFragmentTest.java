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

import android.content.Context;
import android.provider.SearchIndexableResource;
import android.view.Menu;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settingslib.drawer.CategoryKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class NetworkDashboardFragmentTest {

    private NetworkDashboardFragment mFragment;

    @Before
    public void setUp() {
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

        verify(resetController).buildMenuItem(any(Menu.class));
    }

    @Test
    public void testNonIndexableKeys_existInXmlLayout() {
        final Context context = RuntimeEnvironment.application;
        final List<String> niks = NetworkDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(context);
        final int xmlId = (new NetworkDashboardFragment()).getPreferenceScreenResId();

        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(context, xmlId);

        assertThat(keys).containsAllIn(niks);
    }
}
