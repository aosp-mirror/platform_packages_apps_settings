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

import android.text.TextUtils;
import com.android.settings.dashboard.SiteMapManager;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.IndexDatabaseHelper;
import com.android.settings.utils.AsyncLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.android.settings.search.IndexDatabaseHelper.IndexColumns;
import static com.android.settings.search.IndexDatabaseHelper.Tables.TABLE_PREFS_INDEX;

/**
 * AsyncTask to retrieve Settings, First party app and any intent based results.
 */
public class DatabaseResultLoader extends AsyncLoader<List<SearchResult>> {
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
     * If the query matches the title, the best rank it can be is 1
     * If the query only matches the summary, the best rank it can be is 4
     * If the query only matches keywords or entries, the best rank it can be is 7
     */
    private static final int[] BASE_RANKS = {1, 4, 7};

    private final String mQueryText;
    private final SQLiteDatabase mDatabase;
    private final CursorToSearchResultConverter mConverter;
    private final SiteMapManager mSiteMapManager;

    public DatabaseResultLoader(Context context, String queryText) {
        super(context);
        mSiteMapManager = FeatureFactory.getFactory(context)
                .getSearchFeatureProvider().getSiteMapManager();
        mDatabase = IndexDatabaseHelper.getInstance(context).getReadableDatabase();
        mQueryText = cleanQuery(queryText);
        mConverter = new CursorToSearchResultConverter(context, mQueryText);
    }

    @Override
    protected void onDiscardResult(List<SearchResult> result) {
        // TODO Search
    }

    @Override
    public List<SearchResult> loadInBackground() {
        if (mQueryText == null || mQueryText.isEmpty()) {
            return null;
        }

        final List<SearchResult> primaryResults;
        final List<SearchResult> secondaryResults;
        final List<SearchResult> tertiaryResults;

        primaryResults = query(MATCH_COLUMNS_PRIMARY, BASE_RANKS[0]);
        secondaryResults = query(MATCH_COLUMNS_SECONDARY, BASE_RANKS[1]);
        tertiaryResults = query(MATCH_COLUMNS_TERTIARY, BASE_RANKS[2]);

        final List<SearchResult> results = new ArrayList<>(primaryResults.size()
                + secondaryResults.size()
                + tertiaryResults.size());

        results.addAll(primaryResults);
        results.addAll(secondaryResults);
        results.addAll(tertiaryResults);
        return results;
    }

    private List<SearchResult> query(String[] matchColumns, int baseRank) {
        final String whereClause = buildWhereClause(matchColumns);
        final String[] selection = new String[matchColumns.length];
        final String query = "%" + mQueryText + "%";
        Arrays.fill(selection, query);

        final Cursor resultCursor = mDatabase.query(TABLE_PREFS_INDEX, SELECT_COLUMNS, whereClause,
                selection, null, null, null);
        return mConverter.convertCursor(mSiteMapManager, resultCursor, baseRank);
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

    private static String buildWhereClause(String[] matchColumns) {
        StringBuilder sb = new StringBuilder(" (");
        final int count = matchColumns.length;
        for (int n = 0; n < count; n++) {
            sb.append(matchColumns[n]);
            sb.append(" like ?");
            if (n < count - 1) {
                sb.append(" OR ");
            }
        }
        sb.append(") AND enabled = 1");
        return sb.toString();
    }
}
