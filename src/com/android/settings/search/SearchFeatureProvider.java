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

import android.annotation.NonNull;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.Toolbar;

import com.android.settings.overlay.FeatureFactory;

/**
 * FeatureProvider for Settings Search
 */
public interface SearchFeatureProvider {

    Intent SEARCH_UI_INTENT = new Intent("com.android.settings.action.SETTINGS_SEARCH");

    /**
     * Ensures the caller has necessary privilege to launch search result page.
     *
     * @throws IllegalArgumentException when caller is null
     * @throws SecurityException        when caller is not allowed to launch search result page
     */
    void verifyLaunchSearchResultPageCaller(Context context, @NonNull ComponentName caller)
            throws SecurityException, IllegalArgumentException;

    /**
     * Synchronously updates the Settings database.
     */
    void updateIndex(Context context);

    DatabaseIndexingManager getIndexingManager(Context context);

    /**
     * @return a {@link SearchIndexableResources} to be used for indexing search results.
     */
    SearchIndexableResources getSearchIndexableResources();

    default String getSettingsIntelligencePkgName() {
        return "com.android.settings.intelligence";
    }

    /**
     * Initializes the search toolbar.
     */
    default void initSearchToolbar(Activity activity, Toolbar toolbar) {
        if (activity == null || toolbar == null) {
            return;
        }
        toolbar.setOnClickListener(tb -> {
            final Intent intent = SEARCH_UI_INTENT;
            intent.setPackage(getSettingsIntelligencePkgName());

            FeatureFactory.getFactory(
                    activity.getApplicationContext()).getSlicesFeatureProvider()
                    .indexSliceDataAsync(activity.getApplicationContext());
            activity.startActivityForResult(intent, 0 /* requestCode */);
        });
    }
}
