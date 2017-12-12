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

import android.content.Context;
import android.view.View;

import com.android.settings.dashboard.SiteMapManager;
import com.android.settings.search.ranking.SearchResultsRankerCallback;

/**
 * FeatureProvider for Settings Search
 */
public interface SearchFeatureProvider {

    /**
     * @return true to use the new version of search
     */
    boolean isEnabled(Context context);

    /**
     * Returns a new loader to search in index database.
     */
    DatabaseResultLoader getDatabaseSearchLoader(Context context, String query);

    /**
     * Returns a new loader to search installed apps.
     */
    InstalledAppResultLoader getInstalledAppSearchLoader(Context context, String query);

    /**
     * Returns a new loader to search accessibility services.
     */
    AccessibilityServiceResultLoader getAccessibilityServiceResultLoader(Context context,
            String query);

    /**
     * Returns a new loader to search input devices.
     */
    InputDeviceResultLoader getInputDeviceResultLoader(Context context, String query);

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
     * Query search results based on the input query.
     *
     * @param context                     application context
     * @param query                       input user query
     * @param searchResultsRankerCallback {@link SearchResultsRankerCallback}
     */
    default void querySearchResults(Context context, String query,
            SearchResultsRankerCallback searchResultsRankerCallback) {
    }

    /**
     * Cancel pending search query
     */
    default void cancelPendingSearchQuery(Context context) {
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

}
