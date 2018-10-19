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
import static org.mockito.Mockito.spy;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.widget.Toolbar;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;

@RunWith(SettingsRobolectricTestRunner.class)
public class SearchFeatureProviderImplTest {

    private SearchFeatureProviderImpl mProvider;
    private Activity mActivity;

    @Before
    public void setUp() {
        FakeFeatureFactory.setupForTest();
        mActivity = Robolectric.buildActivity(Activity.class).create().visible().get();
        mProvider = spy(new SearchFeatureProviderImpl());
    }

    @Test
    public void initSearchToolbar_shouldInitWithOnClickListener() {
        mProvider.initSearchToolbar(mActivity, null);
        // Should not crash.

        final Toolbar toolbar = new Toolbar(mActivity);
        mProvider.initSearchToolbar(mActivity, toolbar);

        toolbar.performClick();

        final Intent launchIntent = Shadows.shadowOf(mActivity).getNextStartedActivity();

        assertThat(launchIntent.getAction())
                .isEqualTo("com.android.settings.action.SETTINGS_SEARCH");
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
        final String packageName = mProvider.getSettingsIntelligencePkgName();
        final ComponentName cn = new ComponentName(packageName, "class");
        mProvider.verifyLaunchSearchResultPageCaller(mActivity, cn);
    }

    @Test
    public void cleanQuery_trimsWhitespace() {
        final String query = "  space ";
        final String cleanQuery = "space";

        assertThat(mProvider.cleanQuery(query)).isEqualTo(cleanQuery);
    }
}
