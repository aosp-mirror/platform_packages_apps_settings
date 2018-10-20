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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.FeatureFlagUtils;
import android.widget.Toolbar;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.settings.R;
import com.android.settings.core.FeatureFlags;
import com.android.settings.core.SettingsBaseActivity;
import com.android.settings.overlay.FeatureFactory;

public class SettingsHomepageActivity extends SettingsBaseActivity {

    private static final String SUGGESTION_TAG = "suggestion";
    private static final String MAIN_TAG = "main";

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

        final Toolbar toolbar = findViewById(R.id.search_action_bar);
        FeatureFactory.getFactory(this).getSearchFeatureProvider()
                .initSearchToolbar(this, toolbar);

        showFragment(new PersonalSettingsFragment(), R.id.suggestion_content, SUGGESTION_TAG);
        showFragment(new TopLevelSettings(), R.id.main_content, MAIN_TAG);
    }

    public static boolean isDynamicHomepageEnabled(Context context) {
        return FeatureFlagUtils.isEnabled(context, FeatureFlags.DYNAMIC_HOMEPAGE);
    }

    private void showFragment(Fragment fragment, int id, String tag) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(id, fragment, tag);
        fragmentTransaction.commit();
    }
}