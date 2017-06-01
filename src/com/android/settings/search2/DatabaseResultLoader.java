/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.search2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import com.android.settings.dashboard.SiteMapManager;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.IndexDatabaseHelper;
import com.android.settings.utils.AsyncLoader;

import java.util.ArrayList;
import java.util.List;

import static com.android.settings.search.IndexDatabaseHelper.IndexColumns;
import static com.android.settings.search.IndexDatabaseHelper.Tables.TABLE_PREFS_INDEX;

/**
 * AsyncTask to retrieve Settings, First party app and any intent based results.
 */
public class DatabaseResultLoader extends AsyncLoader<List<? extends SearchResult>> {
    private static final String LOG = "DatabaseResultLoader";

    /* These indices are used to match the columns of the this loader's SELECT statement.
     These are not necessarily the same order nor similar coverage as the schema defined in
     IndexDatabaseHelper */
    static final int COLUMN_INDEX_ID = 0;
    static final int COLUMN_INDEX_TITLE = 1;
    static final int COLUMN_INDEX_SUMMARY_ON = 2;
    static final int COLUMN_INDEX_SUMMARY_OFF = 3;
    static final int COLUMN_INDEX_CLASS_NAME = 4;
    static final int COLUMN_INDEX_SCREEN_TITLE = 5;
    static final int COLUMN_INDEX_ICON = 6;
    static final int COLUMN_INDEX_INTENT_ACTION = 7;
    static final int COLUMN_INDEX_INTENT_ACTION_TARGET_PACKAGE = 8;
    static final int COLUMN_INDEX_INTENT_ACTION_TARGET_CLASS = 9;
    static final int COLUMN_INDEX_KEY = 10;
    static final int COLUMN_INDEX_PAYLOAD_TYPE = 11;
    static final int COLUMN_INDEX_PAYLOAD = 12;

    public static final String[] SELECT_COLUMNS = {
            IndexColumns.DOCID,
            IndexColumns.DATA_TITLE,
            IndexColumns.DATA_SUMMARY_ON,
            IndexColumns.DATA_SUMMARY_OFF,
            IndexColumns.CLASS_NAME,
            IndexColumns.SCREEN_TITLE,
            IndexColumns.ICON,
            IndexColumns.INTENT_ACTION,
            IndexColumns.INTENT_TARGET_PACKAGE,
            IndexColumns.INTENT_TARGET_CLASS,
            IndexColumns.DATA_KEY_REF,
            IndexColumns.PAYLOAD_TYPE,
            IndexColumns.PAYLOAD
    };

    public static final String[] MATCH_COLUMNS_PRIMARY = {
            IndexColumns.DATA_TITLE,
            IndexColumns.DATA_TITLE_NORMALIZED,
    };

    public static final String[] MATCH_COLUMNS_SECONDARY = {
            IndexColumns.DATA_SUMMARY_ON,
            IndexColumns.DATA_SUMMARY_ON_NORMALIZED,
            IndexColumns.DATA_SUMMARY_OFF,
            IndexColumns.DATA_SUMMARY_OFF_NORMALIZED,
    };

    public static final String[] MATCH_COLUMNS_TERTIARY = {
            IndexColumns.DATA_KEYWORDS,
            IndexColumns.DATA_ENTRIES
    };

    /**
     * Base ranks defines the best possible rank based on what the query matches.
     * If the query matches the prefix of the first word in the title, the best rank it can be is 1
     * If the query matches the prefix of the other words in the title, the best rank it can be is 3
     * If the query only matches the summary, the best rank it can be is 7
     * If the query only matches keywords or entries, the best rank it can be is 9
     *
     */
    public static final int[] BASE_RANKS = {1, 3, 7, 9};

    private final String mQueryText;
    private final Context mContext;
    private final CursorToSearchResultConverter mConverter;
    private final SiteMapManager mSiteMapManager;

    public DatabaseResultLoader(Context context, String queryText, SiteMapManager mapManager) {
        super(context);
        mSiteMapManager = mapManager;
        mContext = context;
        mQueryText = cleanQuery(queryText);
        mConverter = new CursorToSearchResultConverter(context, mQueryText);
    }

    @Override
    protected void onDiscardResult(List<? extends SearchResult> result) {
        // TODO Search
    }

    @Override
    public List<? extends SearchResult> loadInBackground() {
        if (mQueryText == null || mQueryText.isEmpty()) {
            return null;
        }

        final List<SearchResult> primaryFirstWordResults;
        final List<SearchResult> primaryMidWordResults;
        final List<SearchResult> secondaryResults;
        final List<SearchResult> tertiaryResults;

        primaryFirstWordResults = firstWordQuery(MATCH_COLUMNS_PRIMARY, BASE_RANKS[0]);
        primaryMidWordResults = secondaryWordQuery(MATCH_COLUMNS_PRIMARY, BASE_RANKS[1]);
        secondaryResults = anyWordQuery(MATCH_COLUMNS_SECONDARY, BASE_RANKS[2]);
        tertiaryResults = anyWordQuery(MATCH_COLUMNS_TERTIARY, BASE_RANKS[3]);

        final List<SearchResult> results = new ArrayList<>(
                primaryFirstWordResults.size()
                + primaryMidWordResults.size()
                + secondaryResults.size()
                + tertiaryResults.size());

        results.addAll(primaryFirstWordResults);
        results.addAll(primaryMidWordResults);
        results.addAll(secondaryResults);
        results.addAll(tertiaryResults);

        return removeDuplicates(results);
    }

    @Override
    protected boolean onCancelLoad() {
        // TODO
        return super.onCancelLoad();
    }

    /**
     * A generic method to make the query suitable for searching the database.
     *
     * @return the cleaned query string
     */
    private static String cleanQuery(String query) {
        if (TextUtils.isEmpty(query)) {
            return null;
        }
        return query.trim();
    }

    /**
     * Creates and executes the query which matches prefixes of the first word of the given columns.
     *
     * @param matchColumns The columns to match on
     * @param baseRank The highest rank achievable by these results
     * @return A list of the matching results.
     */
    private List<SearchResult> firstWordQuery(String[] matchColumns, int baseRank) {
        final String whereClause = buildSingleWordWhereClause(matchColumns);
        final String query = mQueryText + "%";
        final String[] selection = buildSingleWordSelection(query, matchColumns.length);

        return query(whereClause, selection, baseRank);
    }

    /**
     * Creates and executes the query which matches prefixes of the non-first words of the
     * given columns.
     *
     * @param matchColumns The columns to match on
     * @param baseRank The highest rank achievable by these results
     * @return A list of the matching results.
     */
    private List<SearchResult> secondaryWordQuery(String[] matchColumns, int baseRank) {
        final String whereClause = buildSingleWordWhereClause(matchColumns);
        final String query = "% " + mQueryText + "%";
        final String[] selection = buildSingleWordSelection(query, matchColumns.length);

        return query(whereClause, selection, baseRank);
    }

    /**
     * Creates and executes the query which matches prefixes of the any word of the given columns.
     *
     * @param matchColumns The columns to match on
     * @param baseRank The highest rank achievable by these results
     * @return A list of the matching results.
     */
    private List<SearchResult> anyWordQuery(String[] matchColumns, int baseRank) {
        final String whereClause = buildTwoWordWhereClause(matchColumns);
        final String[] selection = buildAnyWordSelection(matchColumns.length * 2);

        return query(whereClause, selection, baseRank);
    }

    /**
     * Generic method used by all of the query methods above to execute a query.
     *
     * @param whereClause Where clause for the SQL query which uses bindings.
     * @param selection List of the transformed query to match each bind in the whereClause
     * @param baseRank The highest rank achievable by these results.
     * @return A list of the matching results.
     */
    private List<SearchResult> query(String whereClause, String[] selection, int baseRank) {
        final SQLiteDatabase database = IndexDatabaseHelper.getInstance(mContext)
                .getReadableDatabase();
        final Cursor resultCursor = database.query(TABLE_PREFS_INDEX, SELECT_COLUMNS, whereClause,
                selection, null, null, null);
        return mConverter.convertCursor(mSiteMapManager, resultCursor, baseRank);
    }

    /**
     * Builds the SQLite WHERE clause that matches all matchColumns for a single query.
     *
     * @param matchColumns List of columns that will be used for matching.
     * @return The constructed WHERE clause.
     */
    private static String buildSingleWordWhereClause(String[] matchColumns) {
        StringBuilder sb = new StringBuilder(" (");
        final int count = matchColumns.length;
        for (int n = 0; n < count; n++) {
            sb.append(matchColumns[n]);
            sb.append(" like ? ");
            if (n < count - 1) {
                sb.append(" OR ");
            }
        }
        sb.append(") AND enabled = 1");
        return sb.toString();
    }

    /**
     * Builds the SQLite WHERE clause that matches all matchColumns to two different queries.
     *
     * @param matchColumns List of columns that will be used for matching.
     * @return The constructed WHERE clause.
     */
    private static String buildTwoWordWhereClause(String[] matchColumns) {
        StringBuilder sb = new StringBuilder(" (");
        final int count = matchColumns.length;
        for (int n = 0; n < count; n++) {
            sb.append(matchColumns[n]);
            sb.append(" like ? OR ");
            sb.append(matchColumns[n]);
            sb.append(" like ?");
            if (n < count - 1) {
                sb.append(" OR ");
            }
        }
        sb.append(") AND enabled = 1");
        return sb.toString();
    }

    /**
     * Fills out the selection array to match the query as the prefix of a single word.
     *
     * @param size is the number of columns to be matched.
     */
    private String[] buildSingleWordSelection(String query, int size) {
        String[] selection = new String[size];

        for(int i = 0; i < size; i ++) {
            selection[i] = query;
        }
        return selection;
    }

    /**
     * Fills out the selection array to match the query as the prefix of a word.
     *
     * @param size is twice the number of columns to be matched. The first match is for the prefix
     *             of the first word in the column. The second match is for any subsequent word
     *             prefix match.
     */
    private String[] buildAnyWordSelection(int size) {
        String[] selection = new String[size];
        final String query = mQueryText + "%";
        final String subStringQuery = "% " + mQueryText + "%";

        for(int i = 0; i < (size - 1); i += 2) {
            selection[i] = query;
            selection[i + 1] = subStringQuery;
        }
        return selection;
    }

    /**
     * Goes through the list of search results and verifies that none of the results are duplicates.
     * A duplicate is quantified by a result with the same Title and the same non-empty Summary.
     *
     * The method walks through the results starting with the highest priority result. It removes
     * the duplicates by doing the first rule that applies below:
     * - If a result is inline, remove the intent result.
     * - Remove the lower rank item.
     * @param results A list of results with potential duplicates
     * @return The list of results with duplicates removed.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    List<SearchResult> removeDuplicates(List<SearchResult> results) {
        SearchResult primaryResult, secondaryResult;

        // We accept the O(n^2) solution because the number of results is small.
        for (int i = results.size() - 1; i >= 0; i--) {
            secondaryResult = results.get(i);

            for (int j = i - 1; j >= 0; j--) {
                primaryResult = results.get(j);
                if (areDuplicateResults(primaryResult, secondaryResult)) {
                    if (primaryResult.viewType != ResultPayload.PayloadType.INTENT) {
                        // Case where both payloads are inline
                        results.remove(i);
                        break;
                    } else if (secondaryResult.viewType != ResultPayload.PayloadType.INTENT) {
                        // Case where only second result is inline.
                        results.remove(j);
                        i--; // shift the top index to reflect the lower element being removed
                    } else {
                        // Case where both payloads are intent.
                        results.remove(i);
                        break;
                    }
                }
            }
        }
        return results;
    }

    /**
     * @return True when the two {@link SearchResult SearchResults} have the same title, and the same
     * non-empty summary.
     */
    private boolean areDuplicateResults(SearchResult primary, SearchResult secondary) {
        return TextUtils.equals(primary.title, secondary.title)
                && TextUtils.equals(primary.summary, secondary.summary)
                && !TextUtils.isEmpty(primary.summary);
    }
}