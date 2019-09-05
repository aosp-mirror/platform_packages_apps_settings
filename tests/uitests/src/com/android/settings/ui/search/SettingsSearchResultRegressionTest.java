
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

package com.android.settings.ui.search;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.platform.test.annotations.Presubmit;
import android.text.TextUtils;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class SettingsSearchResultRegressionTest {

    private static final String TAG = "SearchRegressionTest";

    private Context mContext;

    public interface IndexColumns {
        String DATA_TITLE = "data_title";
        String DATA_KEY_REF = "data_key_reference";
    }

    private static final String ERROR_RESULTS_MISSING =
            "\nSettings search results missing. \n"
                    + "If the changes are intentional, we want to update the master-list.\n";

    private static final String ERROR_NEW_RESULTS =
            "\nNew settings search results have been found.\nIf the changes are intentional, we "
                    + "want to"
                    + "prevent the new results from regressing.\n";

    private static final String ERROR_RERUN_TEST =
            "Please re-run the test \"generate_search_result_list\" by removing the '@Ignore' "
                    + "annotation above 'generate_search_result_list' test, and run: \n"
                    + "$ runtest --path "
                    + "packages/apps/Settings/tests/uitests/src/com/android/settings/search"
                    + "/SettingsSearchResultRegressionTest.java \n"
                    + "and copy the output into "
                    + "'packages/apps/Settings/tests/uitests/assets/search_result_list'\n";


    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getContext();
    }

    /**
     * Tests that the set of search results does not regress.
     * <p>
     * The data set used here (/tests/unit/assets/search_results_list) needs to be updated
     * every once in a while so that we can check newly added results.
     * </p>
     */
    @Test
    @Presubmit
    public void searchResultsDoNotRegress() {
        final ContentResolver resolver = mContext.getContentResolver();
        final Uri uri = getTestProviderUri();
        if (uri == null) {
            Log.e(TAG, "Something is wrong getting test provider uri, skipping");
            return;
        }
        final Cursor cursor = resolver.query(uri, null, null, null, null);

        if (cursor == null) {
            // Assume Settings Intelligence is wrong.
            return;
        }

        final Set<SearchData> availableSearchResults = getSearchDataFromCursor(cursor);
        final Set<SearchData> registeredSearchResults = getRegisteredResults();

        // Seed with results that we expect
        final Set<SearchData> missingSearchResults = new HashSet<>(registeredSearchResults);
        // Seed with results that are available
        final Set<SearchData> newSearchResults = new HashSet<>(availableSearchResults);

        // Remove all available results, leaving results that have been removed.
        missingSearchResults.removeAll(availableSearchResults);
        // Remove all results we expect, leaving results that have not yet been registered.
        newSearchResults.removeAll(registeredSearchResults);

        assertWithMessage(ERROR_RESULTS_MISSING + ERROR_RERUN_TEST)
                .that(missingSearchResults).isEmpty();
        assertWithMessage(ERROR_NEW_RESULTS + ERROR_RERUN_TEST).that(newSearchResults).isEmpty();
    }

    // TODO (b/113907111) add a test to catch duplicate title search results.

    /**
     * Test to generate a new list of search results. Uncomment the Test annotation and run the
     * test to generate the list.
     */
    @Ignore
    @Test
    public void generate_search_result_list() {
        final ContentResolver resolver = mContext.getContentResolver();
        final Uri uri = getTestProviderUri();
        if (uri == null) {
            Log.e(TAG, "Something is wrong getting test provider uri, skipping");
            return;
        }
        final Cursor cursor = resolver.query(uri, null, null, null, null);
        final List<SearchData> availableSearchResults =
                new ArrayList<>(getSearchDataFromCursor(cursor));

        Collections.sort(availableSearchResults, Comparator.comparing(SearchData::getTitle)
                .thenComparing(SearchData::getKey));

        assertThat(generateListFromSearchData(availableSearchResults)).isNull();
    }

    private Set<SearchData> getSearchDataFromCursor(Cursor cursor) {
        final Set<SearchData> searchData = new HashSet<>();

        final int titleIndex = cursor.getColumnIndex(
                IndexColumns.DATA_TITLE);
        final int keyIndex = cursor.getColumnIndex(
                IndexColumns.DATA_KEY_REF);

        while (cursor.moveToNext()) {
            String title = cursor.getString(titleIndex);
            String key = cursor.getString(keyIndex);

            if (TextUtils.isEmpty(title)) {
                title = "";
            }

            if (TextUtils.isEmpty(key)) {
                key = "";
            }

            searchData.add(new SearchData.Builder()
                    .setTitle(title)
                    .setKey(key)
                    .build());
        }

        return searchData;
    }

    /**
     * Utility method to generate the list of search results that this class uses to validate
     * results.
     */
    private String generateListFromSearchData(List<SearchData> searchData) {
        StringBuilder builder = new StringBuilder();
        for (SearchData searchResult : searchData) {
            builder.append(searchResult.title)
                    .append(
                            SearchData.DELIM)
                    .append(searchResult.key)
                    .append("\n");
        }
        return builder.toString();
    }

    private Uri getTestProviderUri() {
        final Intent providerIntent = new Intent("com.android.settings.intelligence.DUMP_INDEX");

        final List<ResolveInfo> info = mContext.getPackageManager().queryIntentContentProviders(
                providerIntent, 0 /* flags */);
        if (info.size() != 1) {
            Log.e(TAG, "Unexpected number of DUMP_INDEX providers, skipping. Expected 1, Found "
                    + info.size());
            return null;
        }
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(info.get(0).providerInfo.authority)
                .build();
    }

    private Set<SearchData> getRegisteredResults() {
        final String filename = "search_results_list";
        final Set<SearchData> registeredResults = new HashSet<>();

        try {
            final InputStream in = mContext.getAssets().open(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                registeredResults.add(
                        SearchData.from(line));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error initializing registered result list "
                    + filename, e);
        }

        return registeredResults;
    }
}