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
 * limitations under the License.
 */

package com.android.settings.development;

import android.app.Activity;
import android.content.Context;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.drawer.CategoryKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowResources.SettingsShadowTheme.class
        })
public class DevelopmentSettingsTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    @Mock
    private PreferenceManager mPreferenceManager;

    private FakeFeatureFactory mFeatureFactory;
    private DevelopmentSettings mSettings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mSettings = spy(new DevelopmentSettings());
    }

    @Test
    public void addDashboardCategoryPreference_shouldAddToScreen() {
        final List<Preference> preferences = new ArrayList<>();
        preferences.add(new Preference(ShadowApplication.getInstance().getApplicationContext()));
        preferences.add(new Preference(ShadowApplication.getInstance().getApplicationContext()));
        doReturn(mScreen).when(mSettings).getPreferenceScreen();
        doReturn(mPreferenceManager).when(mSettings).getPreferenceManager();
        doReturn(mActivity).when(mSettings).getActivity();
        when(mPreferenceManager.getContext()).thenReturn(mContext);
        when(mFeatureFactory.dashboardFeatureProvider.getPreferencesForCategory(
                mActivity, mContext, mSettings.getMetricsCategory(),
                CategoryKey.CATEGORY_SYSTEM_DEVELOPMENT))
                .thenReturn(preferences);

        mSettings.onAttach(mContext);
        mSettings.addDashboardCategoryPreferences();

        verify(mScreen, times(2)).addPreference(any(Preference.class));
    }

    @Test
    public void searchIndex_shouldIndexFromPrefXml() {
        final List<SearchIndexableResource> index =
                DevelopmentSettings.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(
                        RuntimeEnvironment.application, true);

        assertThat(index.size()).isEqualTo(1);
        assertThat(index.get(0).xmlResId).isEqualTo(R.xml.development_prefs);
    }

    @Test
    public void searchIndex_pageDisabled_shouldAddAllKeysToNonIndexable() {
        final Context appContext = RuntimeEnvironment.application;
        new DevelopmentSettingsEnabler(appContext, null /* lifecycle */)
                .disableDevelopmentSettings();

        final List<String> nonIndexableKeys =
                DevelopmentSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(appContext);

        assertThat(nonIndexableKeys).contains("development_prefs_screen");
    }

    @Test
    public void searchIndex_pageEnabled_shouldNotAddKeysToNonIndexable() {
        final Context appContext = RuntimeEnvironment.application;
        new DevelopmentSettingsEnabler(appContext, null /* lifecycle */)
                .enableDevelopmentSettings();

        final List<String> nonIndexableKeys =
                DevelopmentSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(appContext);

        assertThat(nonIndexableKeys).doesNotContain("development_prefs_screen");
    }
}
