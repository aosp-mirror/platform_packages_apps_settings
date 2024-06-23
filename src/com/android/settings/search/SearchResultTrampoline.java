/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.search;

import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS;
import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT_TAB;
import static com.android.settings.activityembedding.EmbeddedDeepLinkUtils.getTrampolineIntent;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;
import android.util.Log;

import com.android.settings.SettingsActivity;
import com.android.settings.SettingsApplication;
import com.android.settings.SubSettings;
import com.android.settings.activityembedding.ActivityEmbeddingRulesController;
import com.android.settings.activityembedding.ActivityEmbeddingUtils;
import com.android.settings.core.FeatureFlags;
import com.android.settings.homepage.DeepLinkHomepageActivityInternal;
import com.android.settings.homepage.SettingsHomepageActivity;
import com.android.settings.overlay.FeatureFactory;

import java.net.URISyntaxException;

/**
 * A trampoline activity that launches setting result page.
 */
public class SearchResultTrampoline extends Activity {

    private static final String TAG = "SearchResultTrampoline";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String callerPackage = getLaunchedFromPackage();
        // First make sure caller has privilege to launch a search result page.
        FeatureFactory.getFeatureFactory()
                .getSearchFeatureProvider()
                .verifyLaunchSearchResultPageCaller(this, callerPackage);
        // Didn't crash, proceed and launch the result as a subsetting.
        Intent intent = getIntent();
        final String highlightMenuKey = intent.getStringExtra(
                Settings.EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_HIGHLIGHT_MENU_KEY);

        final String fragment = intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT);
        if (!TextUtils.isEmpty(fragment)) {
            // Hack to take EXTRA_FRAGMENT_ARG_KEY from intent and set into
            // EXTRA_SHOW_FRAGMENT_ARGUMENTS. This is necessary because intent could be from
            // external caller and args may not persisted.
            final String settingKey = intent.getStringExtra(
                    SettingsActivity.EXTRA_FRAGMENT_ARG_KEY);
            final int tab = intent.getIntExtra(EXTRA_SHOW_FRAGMENT_TAB, 0);
            final Bundle args = new Bundle();
            args.putString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, settingKey);
            args.putInt(EXTRA_SHOW_FRAGMENT_TAB, tab);
            intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);

            // Reroute request to SubSetting.
            intent.setClass(this /* context */, SubSettings.class);
        } else {
            // Direct link case
            final String intentUriString = intent.getStringExtra(
                    Settings.EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_INTENT_URI);
            if (TextUtils.isEmpty(intentUriString)) {
                Log.e(TAG, "No EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_INTENT_URI for deep link");
                finish();
                return;
            }

            final Uri data = intent.getParcelableExtra(
                    SettingsHomepageActivity.EXTRA_SETTINGS_LARGE_SCREEN_DEEP_LINK_INTENT_DATA,
                    Uri.class);
            try {
                intent = Intent.parseUri(intentUriString, Intent.URI_INTENT_SCHEME);
                intent.setData(data);
            } catch (URISyntaxException e) {
                Log.e(TAG, "Failed to parse deep link intent: " + e);
                finish();
                return;
            }
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);

        if (!ActivityEmbeddingUtils.isEmbeddingActivityEnabled(this)
                || ActivityEmbeddingUtils.isAlreadyEmbedded(this)) {
            startActivity(intent);
        } else if (isSettingsIntelligence(callerPackage)) {
            if (FeatureFlagUtils.isEnabled(this, FeatureFlags.SETTINGS_SEARCH_ALWAYS_EXPAND)) {
                startActivity(getTrampolineIntent(intent, highlightMenuKey)
                        .setClass(this, DeepLinkHomepageActivityInternal.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));
            } else {
                // Register SplitPairRule for SubSettings, set clearTop false to prevent unexpected
                // back navigation behavior.
                ActivityEmbeddingRulesController.registerSubSettingsPairRule(this,
                        false /* clearTop */);

                intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                // Pass menu key to homepage
                final SettingsHomepageActivity homeActivity =
                        ((SettingsApplication) getApplicationContext()).getHomeActivity();
                if (homeActivity != null) {
                    homeActivity.getMainFragment().setHighlightMenuKey(highlightMenuKey,
                            /* scrollNeeded= */ true);
                }
            }
        } else {
            // Two-pane case
            startActivity(getTrampolineIntent(intent, highlightMenuKey)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }

        // Done.
        finish();
    }

    private boolean isSettingsIntelligence(String callerPackage) {
        return TextUtils.equals(
                callerPackage,
                FeatureFactory.getFeatureFactory().getSearchFeatureProvider()
                        .getSettingsIntelligencePkgName(this));
    }
}
