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
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.support.annotation.VisibleForTesting;
import com.android.settings.search.Index;
import com.android.settings.search.IndexDatabaseHelper;
import com.android.settings.utils.AsyncLoader;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_ICON_RESID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_RAW_SUMMARY_ON;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RANK;


/**
 * AsyncTask to retrieve Settings, First party app and any intent based results.
 */
public class DatabaseResultLoader extends AsyncLoader<List<SearchResult>> {
    private final String mQueryText;
    private final Context mContext;
    protected final SQLiteDatabase mDatabase;

    public DatabaseResultLoader(Context context, String queryText) {
        super(context);
        mDatabase = IndexDatabaseHelper.getInstance(context).getReadableDatabase();
        mQueryText = queryText;
        mContext = context;
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

        String query = getSQLQuery();
        Cursor result  = mDatabase.rawQuery(query, null);

        return parseCursorForSearch(result);
    }

    @Override
    protected boolean onCancelLoad() {
        // TODO
        return super.onCancelLoad();
    }

    protected String getSQLQuery() {
        return String.format("SELECT data_rank, data_title, data_summary_on, " +
                "data_summary_off, data_entries, data_keywords, class_name, screen_title, icon, " +
                "intent_action, intent_target_package, intent_target_class, enabled, " +
                "data_key_reference FROM prefs_index WHERE prefs_index MATCH 'data_title:%s* " +
                "OR data_title_normalized:%s* OR data_keywords:%s*' AND locale = 'en_US'",
                mQueryText, mQueryText, mQueryText);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public ArrayList<SearchResult> parseCursorForSearch(Cursor cursorResults) {
        if (cursorResults == null) {
            return null;
        }
        final ArrayList<SearchResult> results = new ArrayList<>();

        while (cursorResults.moveToNext()) {
            final String title = cursorResults.getString(Index.COLUMN_INDEX_TITLE);
            final String summaryOn = cursorResults.getString(COLUMN_INDEX_RAW_SUMMARY_ON);
            final ArrayList<String> breadcrumbs = new ArrayList<>();
            final int rank = cursorResults.getInt(COLUMN_INDEX_XML_RES_RANK);

            final String intentString = cursorResults.getString(Index.COLUMN_INDEX_INTENT_ACTION);
            final IntentPayload intentPayload = new IntentPayload(new Intent(intentString));
            final int iconID = cursorResults.getInt(COLUMN_INDEX_RAW_ICON_RESID);
            Drawable icon;
            try {
                icon = mContext.getDrawable(iconID);
            } catch (Resources.NotFoundException nfe) {
                icon = mContext.getDrawable(R.drawable.ic_search_history);
            }


            SearchResult.Builder builder = new SearchResult.Builder();
            builder.addTitle(title)
                    .addSummary(summaryOn)
                    .addBreadcrumbs(breadcrumbs)
                    .addRank(rank)
                    .addIcon(icon)
                    .addPayload(intentPayload);
            results.add(builder.build());
        }
        Collections.sort(results);
        return results;
    }

}
