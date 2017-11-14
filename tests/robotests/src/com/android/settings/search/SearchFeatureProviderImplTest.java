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
 *
 */

package com.android.settings.search;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.util.FeatureFlagUtils;
import android.widget.Toolbar;

import com.android.settings.TestConfig;
import com.android.settings.core.FeatureFlags;
import com.android.settings.dashboard.SiteMapManager;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowSystemProperties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION_O, shadows = {
        SettingsShadowSystemProperties.class
})
public class SearchFeatureProviderImplTest {

    private SearchFeatureProviderImpl mProvider;
    private Activity mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = Robolectric.buildActivity(Activity.class).create().visible().get();
        mProvider = spy(new SearchFeatureProviderImpl());
    }

    @After
    public void tearDown() {
        SettingsShadowSystemProperties.clear();
    }

    @Test
    public void getSiteMapManager_shouldCacheInstance() {
        final SiteMapManager manager1 = mProvider.getSiteMapManager();
        final SiteMapManager manager2 = mProvider.getSiteMapManager();

        assertThat(manager1).isSameAs(manager2);
    }

    @Test
    public void getDatabaseSearchLoader_shouldCleanupQuery() {
        final String query = "  space ";

        mProvider.getStaticSearchResultTask(mActivity, query);

        verify(mProvider).cleanQuery(eq(query));
    }

    @Test
    public void getInstalledAppSearchLoader_shouldCleanupQuery() {
        final String query = "  space ";

        mProvider.getInstalledAppSearchTask(mActivity, query);

        verify(mProvider).cleanQuery(eq(query));
    }

    @Test
    public void initSearchToolbar_searchV2_shouldInitWithOnClickListener() {
        mProvider.initSearchToolbar(mActivity, null);
        // Should not crash.

        SettingsShadowSystemProperties.set(
                FeatureFlagUtils.FFLAG_PREFIX + FeatureFlags.SEARCH_V2,
                "true");
        final Toolbar toolbar = new Toolbar(mActivity);
        mProvider.initSearchToolbar(mActivity, toolbar);

        toolbar.performClick();

        final Intent launchIntent = shadowOf(mActivity).getNextStartedActivity();

        assertThat(launchIntent.getAction())
                .isEqualTo("com.android.settings.action.SETTINGS_SEARCH");
    }

    @Test
    public void initSearchToolbar_searchV1_shouldInitWithOnClickListener() {
        mProvider.initSearchToolbar(mActivity, null);
        // Should not crash.

        SettingsShadowSystemProperties.set(
                FeatureFlagUtils.FFLAG_PREFIX + FeatureFlags.SEARCH_V2,
                "false");
        final Toolbar toolbar = new Toolbar(mActivity);
        mProvider.initSearchToolbar(mActivity, toolbar);

        toolbar.performClick();

        final Intent launchIntent = shadowOf(mActivity).getNextStartedActivity();

        assertThat(launchIntent.getComponent().getClassName())
                .isEqualTo(SearchActivity.class.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyLaunchSearchResultPageCaller_nullCaller_shouldCrash() {
        mProvider.verifyLaunchSearchResultPageCaller(mActivity, null /* caller */);
    }

    @Test(expected = SecurityException.class)
    public void verifyLaunchSearchResultPageCaller_badCaller_shouldCrash() {
        final ComponentName cn = new ComponentName("pkg", "class");
        mProvider.verifyLaunchSearchResultPageCaller(mActivity, cn);
    }

    @Test
    public void verifyLaunchSearchResultPageCaller_settingsCaller_shouldNotCrash() {
        final ComponentName cn = new ComponentName(mActivity.getPackageName(), "class");
        mProvider.verifyLaunchSearchResultPageCaller(mActivity, cn);
    }

    @Test
    public void verifyLaunchSearchResultPageCaller_settingsIntelligenceCaller_shouldNotCrash() {
        final ComponentName cn =
                new ComponentName(mProvider.getSettingsIntelligencePkgName(), "class");
        mProvider.verifyLaunchSearchResultPageCaller(mActivity, cn);
    }

    @Test
    public void cleanQuery_trimsWhitespace() {
        final String query = "  space ";
        final String cleanQuery = "space";

        assertThat(mProvider.cleanQuery(query)).isEqualTo(cleanQuery);
    }
}
