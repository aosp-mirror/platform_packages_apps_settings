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
 *
 */
package com.android.settings.search;

import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO;

import android.annotation.NonNull;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toolbar;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.activityembedding.ActivityEmbeddingRulesController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.search.SearchIndexableResources;

import com.google.android.setupcompat.util.WizardManagerHelper;

import java.util.List;

/**
 * FeatureProvider for Settings Search
 */
public interface SearchFeatureProvider {

    String KEY_HOMEPAGE_SEARCH_BAR = "homepage_search_bar";
    int REQUEST_CODE = 501;

    /**
     * Ensures the caller has necessary privilege to launch search result page.
     *
     * @throws IllegalArgumentException when caller is null
     * @throws SecurityException        when caller is not allowed to launch search result page
     */
    void verifyLaunchSearchResultPageCaller(Context context, @NonNull ComponentName caller)
            throws SecurityException, IllegalArgumentException;

    /**
     * @return a {@link SearchIndexableResources} to be used for indexing search results.
     */
    SearchIndexableResources getSearchIndexableResources();

    /**
     * @return a package name of settings intelligence.
     */
    default String getSettingsIntelligencePkgName(Context context) {
        return context.getString(R.string.config_settingsintelligence_package_name);
    }

    /**
     * Send the pre-index intent.
     */
    default void sendPreIndexIntent(Context context){
    }

    /**
     * Initializes the search toolbar.
     */
    default void initSearchToolbar(FragmentActivity activity, Toolbar toolbar, int pageId) {
        if (activity == null || toolbar == null) {
            return;
        }

        if (!WizardManagerHelper.isDeviceProvisioned(activity)
                || !Utils.isPackageEnabled(activity, getSettingsIntelligencePkgName(activity))
                || WizardManagerHelper.isAnySetupWizard(activity.getIntent())) {
            final ViewGroup parent = (ViewGroup) toolbar.getParent();
            if (parent != null) {
                parent.setVisibility(View.GONE);
            }
            return;
        }
        // Please forgive me for what I am about to do.
        //
        // Need to make the navigation icon non-clickable so that the entire card is clickable
        // and goes to the search UI. Also set the background to null so there's no ripple.
        final View navView = toolbar.getNavigationView();
        navView.setClickable(false);
        navView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        navView.setBackground(null);

        final Context context = activity.getApplicationContext();
        final Intent intent = buildSearchIntent(context, pageId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        final List<ResolveInfo> resolveInfos =
                activity.getPackageManager().queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfos.isEmpty()) {
            return;
        }

        final ComponentName searchComponentName = resolveInfos.get(0)
                .getComponentInfo().getComponentName();
        // Set a component name since activity embedding requires a component name for
        // registering a rule.
        intent.setComponent(searchComponentName);
        ActivityEmbeddingRulesController.registerTwoPanePairRuleForSettingsHome(
                context,
                searchComponentName,
                intent.getAction(),
                false /* finishPrimaryWithSecondary */,
                true /* finishSecondaryWithPrimary */,
                false /* clearTop */);

        toolbar.setOnClickListener(tb -> {
            FeatureFactory.getFactory(context).getSlicesFeatureProvider()
                    .indexSliceDataAsync(context);

            FeatureFactory.getFactory(context).getMetricsFeatureProvider()
                    .logSettingsTileClick(KEY_HOMEPAGE_SEARCH_BAR, pageId);

            final Bundle bundle = ActivityOptions.makeSceneTransitionAnimation(activity).toBundle();
            activity.startActivity(intent, bundle);
        });
    }

    Intent buildSearchIntent(Context context, int pageId);
}
