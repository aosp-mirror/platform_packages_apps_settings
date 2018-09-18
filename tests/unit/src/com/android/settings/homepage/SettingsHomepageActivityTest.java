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

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import static com.android.settings.homepage.SettingsHomepageActivity.PERSONAL_SETTINGS_TAG;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.FeatureFlagUtils;

import androidx.fragment.app.Fragment;

import com.android.settings.core.FeatureFlags;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SettingsHomepageActivityTest {

    private Context mContext;
    private SettingsHomepageActivity mActivity;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.DYNAMIC_HOMEPAGE, true);
    }

    @After
    public void tearDown() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.DYNAMIC_HOMEPAGE, false);
    }

    @Test
    public void launchHomepage_shouldOpenPersonalSettings() {
        final Intent intent = new Intent().setClass(mContext, SettingsHomepageActivity.class)
                .addFlags(FLAG_ACTIVITY_NEW_TASK);

        mActivity = (SettingsHomepageActivity) InstrumentationRegistry.getInstrumentation()
                .startActivitySync(intent);

        final Fragment fragment = mActivity.getSupportFragmentManager()
                .findFragmentByTag(PERSONAL_SETTINGS_TAG);

        assertThat(fragment).isInstanceOf(PersonalSettingsFragment.class);
    }

}
