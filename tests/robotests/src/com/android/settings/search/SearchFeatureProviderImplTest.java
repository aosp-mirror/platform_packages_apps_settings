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

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toolbar;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
public class SearchFeatureProviderImplTest {

    private SearchFeatureProviderImpl mProvider;
    private FragmentActivity mActivity;
    private ShadowPackageManager mPackageManager;

    @Before
    public void setUp() {
        FakeFeatureFactory.setupForTest();
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        mProvider = new SearchFeatureProviderImpl();
        mPackageManager = Shadows.shadowOf(mActivity.getPackageManager());
        Settings.Global.putInt(mActivity.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1);
    }

    @Test
    @Ignore
    @Config(shadows = ShadowUtils.class)
    public void initSearchToolbar_hasResolvedInfo_shouldStartCorrectIntent() {
        final Intent searchIntent = new Intent(Settings.ACTION_APP_SEARCH_SETTINGS)
                .setPackage(mActivity.getString(R.string.config_settingsintelligence_package_name));
        final ResolveInfo info = new ResolveInfo();
        final ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = "com.android.example";
        info.activityInfo = activityInfo;
        mPackageManager.addResolveInfoForIntent(searchIntent, info);

        // Should not crash.
        mProvider.initSearchToolbar(mActivity, null, SettingsEnums.TESTING);

        final Toolbar toolbar = new Toolbar(mActivity);
        // This ensures navigationView is created.
        toolbar.setNavigationContentDescription("test");
        mProvider.initSearchToolbar(mActivity, toolbar, SettingsEnums.TESTING);

        toolbar.performClick();

        final Intent launchIntent = Shadows.shadowOf(mActivity).getNextStartedActivity();

        assertThat(launchIntent.getAction()).isEqualTo(Settings.ACTION_APP_SEARCH_SETTINGS);
    }

    @Test
    @Config(shadows = ShadowUtils.class)
    public void initSearchToolbar_noResolvedInfo_shouldNotStartActivity() {
        final Toolbar toolbar = new Toolbar(mActivity);
        // This ensures navigationView is created.
        toolbar.setNavigationContentDescription("test");
        mProvider.initSearchToolbar(mActivity, toolbar, SettingsEnums.TESTING);

        toolbar.performClick();

        assertThat(Shadows.shadowOf(mActivity).getNextStartedActivity()).isNull();
    }

    @Test
    public void initSearchToolbar_deviceNotProvisioned_shouldNotCreateSearchBar() {
        final Toolbar toolbar = new Toolbar(mActivity);
        // This ensures navigationView is created.
        toolbar.setNavigationContentDescription("test");

        Settings.Global.putInt(mActivity.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0);

        toolbar.performClick();

        assertThat(Shadows.shadowOf(mActivity).getNextStartedActivity()).isNull();
    }

    @Test
    public void buildSearchIntent_shouldIncludeReferrer() {
        final Intent searchIntent = mProvider.buildSearchIntent(mActivity, SettingsEnums.TESTING);
        final Uri referrer = searchIntent.getParcelableExtra(Intent.EXTRA_REFERRER);

        assertThat(referrer.toString()).isEqualTo(
                "android-app://" + mActivity.getPackageName() + "/" + SettingsEnums.TESTING);
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
        final String packageName = mProvider.getSettingsIntelligencePkgName(mActivity);
        final ComponentName cn = new ComponentName(packageName, "class");
        mProvider.verifyLaunchSearchResultPageCaller(mActivity, cn);
    }
}
