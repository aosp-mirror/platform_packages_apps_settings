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

package com.android.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.FeatureFlagUtils;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.android.settings.core.FeatureFlags;
import com.android.settings.core.SettingsBaseActivity;
import com.android.settings.homepage.HomepageFragment;

public class SettingsHomepageActivity extends SettingsBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isDynamicHomepageEnabled(this)) {
            final Intent settings = new Intent();
            settings.setAction("android.settings.SETTINGS");
            startActivity(settings);
            finish();
            return;
        }
        setContentView(R.layout.settings_homepage_container);
        if (savedInstanceState == null) {
            switchToFragment(this, R.id.main_content, HomepageFragment.class.getName());
        }
    }

    public static boolean isDynamicHomepageEnabled(Context context) {
        return FeatureFlagUtils.isEnabled(context, FeatureFlags.DYNAMIC_HOMEPAGE);
    }

    /**
     * Switch to a specific Fragment
     */
    public static void switchToFragment(FragmentActivity activity, int id, String fragmentName) {
        final Fragment f = Fragment.instantiate(activity, fragmentName, null /* args */);

        FragmentManager manager = activity.getSupportFragmentManager();
        manager.beginTransaction().replace(id, f).commitAllowingStateLoss();
        manager.executePendingTransactions();
    }
}