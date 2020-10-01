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
import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toolbar;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.search.SearchIndexableResources;

import com.google.android.setupcompat.util.WizardManagerHelper;

/**
 * FeatureProvider for Settings Search
 */
public interface SearchFeatureProvider {

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

    default String getSettingsIntelligencePkgName(Context context) {
        return context.getString(R.string.config_settingsintelligence_package_name);
    }

    /**
     * Initializes the search toolbar.
     */
    default void initSearchToolbar(Activity activity, Toolbar toolbar, int pageId) {
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

        toolbar.setOnClickListener(tb -> {
            final Context context = activity.getApplicationContext();
            final Intent intent = buildSearchIntent(context, pageId);

            if (activity.getPackageManager().queryIntentActivities(intent,
                    PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
                return;
            }

            FeatureFactory.getFactory(context).getSlicesFeatureProvider()
                    .indexSliceDataAsync(context);
            FeatureFactory.getFactory(context).getMetricsFeatureProvider()
                    .action(context, SettingsEnums.ACTION_SEARCH_RESULTS);
            activity.startActivityForResult(intent, REQUEST_CODE);
        });
    }

    Intent buildSearchIntent(Context context, int pageId);
}
