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
 * limitations under the License.
 */

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowResources.SettingsShadowTheme.class
        })
public class DevelopmentSettingsDashboardFragmentTest {

    private DevelopmentSettingsDashboardFragment mDashboard;

    @Before
    public void setUp() {
        mDashboard = new DevelopmentSettingsDashboardFragment();
    }

    @Test
    public void shouldNotHaveHelpResource() {
        assertThat(mDashboard.getHelpResource()).isEqualTo(0);
    }

    @Test
    public void shouldLogAsFeatureFlagPage() {
        assertThat(mDashboard.getMetricsCategory())
                .isEqualTo(MetricsProto.MetricsEvent.DEVELOPMENT);
    }

    @Test
    public void searchIndex_shouldIndexFromPrefXml() {
        final List<SearchIndexableResource> index =
                DevelopmentSettingsDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getXmlResourcesToIndex(RuntimeEnvironment.application, true);

        assertThat(index.size()).isEqualTo(1);
        assertThat(index.get(0).xmlResId).isEqualTo(R.xml.development_prefs);
    }

    @Test
    public void searchIndex_pageDisabled_shouldAddAllKeysToNonIndexable() {
        final Context appContext = RuntimeEnvironment.application;
        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(appContext, false);

        final List<String> nonIndexableKeys =
                DevelopmentSettingsDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getNonIndexableKeys(appContext);

        assertThat(nonIndexableKeys).contains("development_prefs_screen");
    }

    @Test
    public void searchIndex_pageEnabled_shouldNotAddKeysToNonIndexable() {
        final Context appContext = RuntimeEnvironment.application;
        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(appContext, true);

        final List<String> nonIndexableKeys =
                DevelopmentSettingsDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getNonIndexableKeys(appContext);

        assertThat(nonIndexableKeys).doesNotContain("development_prefs_screen");
    }
}
