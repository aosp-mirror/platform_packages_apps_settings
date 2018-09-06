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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.settings.core.FeatureFlags;
import com.android.settings.core.SettingsBaseActivity;
import com.android.settings.dashboard.DashboardSummary;
import com.android.settings.homepage.PersonalSettingsFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.SearchFeatureProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class SettingsHomepageActivity extends SettingsBaseActivity {

    private static final String ALL_SETTINGS_TAG = "all_settings";
    private static final String PERSONAL_SETTINGS_TAG = "personal_settings";

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

        final FloatingActionButton searchButton = findViewById(R.id.search_fab);
        FeatureFactory.getFactory(this).getSearchFeatureProvider()
                .initSearchToolbar(this, searchButton);

        final BottomNavigationView navigation = (BottomNavigationView) findViewById(
                R.id.bottom_nav);
        navigation.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.homepage_personal_settings:
                    switchFragment(PersonalSettingsFragment.class.getName(), PERSONAL_SETTINGS_TAG,
                            ALL_SETTINGS_TAG);
                    return true;

                case R.id.homepage_all_settings:
                    switchFragment(DashboardSummary.class.getName(), ALL_SETTINGS_TAG,
                            PERSONAL_SETTINGS_TAG);
                    return true;
            }
            return false;
        });
    }

    public static boolean isDynamicHomepageEnabled(Context context) {
        return FeatureFlagUtils.isEnabled(context, FeatureFlags.DYNAMIC_HOMEPAGE);
    }

    private void switchFragment(String fragmentName, String showFragmentTag,
            String hideFragmentTag) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        final Fragment hideFragment = fragmentManager.findFragmentByTag(hideFragmentTag);
        if (hideFragment != null) {
            fragmentTransaction.hide(hideFragment);
        }

        Fragment showFragment = fragmentManager.findFragmentByTag(showFragmentTag);
        if (showFragment == null) {
            showFragment = Fragment.instantiate(this, fragmentName, null /* args */);
            fragmentTransaction.add(R.id.main_content, showFragment, showFragmentTag);
        } else {
            fragmentTransaction.show(showFragment);
        }
        fragmentTransaction.commit();
    }
}