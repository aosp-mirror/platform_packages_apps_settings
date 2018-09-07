/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.homepage;

import static com.android.settings.homepage.SettingsHomepageActivity.PERSONAL_SETTINGS_TAG;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.util.FeatureFlagUtils;

import androidx.fragment.app.Fragment;

import com.android.settings.core.FeatureFlags;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class SettingsHomepageActivityTest {

    private Context mContext;
    private SettingsHomepageActivity mActivity;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.DYNAMIC_HOMEPAGE, true);
    }

    @Test
    public void launchHomepage_shouldOpenPersonalSettings() {
        mActivity = Robolectric.setupActivity(SettingsHomepageActivity.class);
        final Fragment fragment = mActivity.getSupportFragmentManager()
                .findFragmentByTag(PERSONAL_SETTINGS_TAG);

        assertThat(fragment).isInstanceOf(PersonalSettingsFragment.class);
    }
}
