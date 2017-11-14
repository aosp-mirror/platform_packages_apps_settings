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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.FeatureFlagUtils;
import android.util.Pair;
import android.view.View;
import android.widget.Toolbar;

import com.android.settings.core.FeatureFlags;
import com.android.settings.dashboard.SiteMapManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

/**
 * FeatureProvider for Settings Search
 */
public interface SearchFeatureProvider {

    /**
     * Ensures the caller has necessary privilege to launch search result page.
     *
     * @throws IllegalArgumentException when caller is null
     * @throws SecurityException        when caller is not allowed to launch search result page
     */
    void verifyLaunchSearchResultPageCaller(Context context, @NonNull ComponentName caller)
            throws SecurityException, IllegalArgumentException;

    /**
     * Returns a new loader to get settings search results.
     */
    SearchResultLoader getSearchResultLoader(Context context, String query);

    /**
     * Returns a new loader to search in index database.
     */
    DatabaseResultLoader getStaticSearchResultTask(Context context, String query);

    /**
     * Returns a new loader to search installed apps.
     */
    InstalledAppResultLoader getInstalledAppSearchTask(Context context, String query);

    /**
     * Returns a new loader to search accessibility services.
     */
    AccessibilityServiceResultLoader getAccessibilityServiceResultTask(Context context,
            String query);

    /**
     * Returns a new loader to search input devices.
     */
    InputDeviceResultLoader getInputDeviceResultTask(Context context, String query);

    /**
     * Returns a new loader to get all recently saved queries search terms.
     */
    SavedQueryLoader getSavedQueryLoader(Context context);

    /**
     * Returns the manager for indexing Settings data.
     */
    DatabaseIndexingManager getIndexingManager(Context context);

    /**
     * Returns the manager for looking up breadcrumbs.
     */
    SiteMapManager getSiteMapManager();

    /**
     * Updates the Settings indexes and calls {@link IndexingCallback#onIndexingFinished()} on
     * {@param callback} when indexing is complete.
     */
    void updateIndexAsync(Context context, IndexingCallback callback);

    /**
     * Synchronously updates the Settings database.
     */
    void updateIndex(Context context);

    /**
     * @returns true when indexing is complete.
     */
    boolean isIndexingComplete(Context context);

    /**
     * @return a {@link ExecutorService} to be shared between search tasks.
     */
    ExecutorService getExecutorService();

    /**
     * Initializes the feedback button in case it was dismissed.
     */
    default void initFeedbackButton() {
    }

    /**
     * Show a button users can click to submit feedback on the quality of the search results.
     */
    default void showFeedbackButton(SearchFragment fragment, View view) {
    }

    /**
     * Hide the feedback button shown by
     * {@link #showFeedbackButton(SearchFragment fragment, View view) showFeedbackButton}
     */
    default void hideFeedbackButton() {
    }

    /**
     * Notify that a search result is clicked.
     *
     * @param context      application context
     * @param query        input user query
     * @param searchResult clicked result
     */
    default void searchResultClicked(Context context, String query, SearchResult searchResult) {
    }

    /**
     * @return true to enable search ranking.
     */
    default boolean isSmartSearchRankingEnabled(Context context) {
        return false;
    }

    /**
     * @return smart ranking timeout in milliseconds.
     */
    default long smartSearchRankingTimeoutMs(Context context) {
        return 300L;
    }

    /**
     * Prepare for search ranking predictions to avoid latency on the first prediction call.
     */
    default void searchRankingWarmup(Context context) {
    }

    /**
     * Return a FutureTask to get a list of scores for search results.
     */
    default FutureTask<List<Pair<String, Float>>> getRankerTask(Context context, String query) {
        return null;
    }

    /**
     * Initializes the search toolbar.
     */
    default void initSearchToolbar(Context context, Toolbar toolbar) {
        if (context == null || toolbar == null) {
            return;
        }
        toolbar.setOnClickListener(tb -> {
            final Intent intent;
            if (FeatureFlagUtils.isEnabled(FeatureFlags.SEARCH_V2)) {
                intent = new Intent("com.android.settings.action.SETTINGS_SEARCH");
            } else {
                intent = new Intent(context, SearchActivity.class);
            }
            context.startActivity(intent);
        });
    }
}
