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

import android.content.Intent;
import android.os.Bundle;
import android.util.FeatureFlagUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toolbar;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.accounts.AvatarViewMixin;
import com.android.settings.core.FeatureFlags;
import com.android.settings.core.SettingsBaseActivity;
import com.android.settings.homepage.contextualcards.ContextualCardsFragment;
import com.android.settings.overlay.FeatureFactory;

public class SettingsHomepageActivity extends SettingsBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!FeatureFlagUtils.isEnabled(this, FeatureFlags.DYNAMIC_HOMEPAGE)) {
            final Intent settings = new Intent(this, SettingsActivity.class);
            startActivity(settings);
            finish();
            return;
        }

        updateWindowProperties();
        setContentView(R.layout.settings_homepage_container);

        final Toolbar toolbar = findViewById(R.id.search_action_bar);
        FeatureFactory.getFactory(this).getSearchFeatureProvider()
                .initSearchToolbar(this /* activity */, toolbar);

        final ImageView avatarView = findViewById(R.id.account_avatar);
        final AvatarViewMixin avatarViewMixin = new AvatarViewMixin(this, avatarView);
        getLifecycle().addObserver(avatarViewMixin);

        showFragment(new ContextualCardsFragment(), R.id.contextual_cards_content);
        showFragment(new TopLevelSettings(), R.id.main_content);
    }

    private void showFragment(Fragment fragment, int id) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        final Fragment showFragment = fragmentManager.findFragmentById(id);

        if (showFragment == null) {
            fragmentTransaction.add(id, fragment);
        } else {
            fragmentTransaction.show(showFragment);
        }
        fragmentTransaction.commit();
    }

    private void updateWindowProperties() {
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                decorView.getSystemUiVisibility() |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        getWindow().setStatusBarColor(getResources().getColor(R.color.homepage_status_bar_color));
    }
}