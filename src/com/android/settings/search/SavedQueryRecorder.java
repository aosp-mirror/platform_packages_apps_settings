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

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.android.settings.search.IndexDatabaseHelper;
import com.android.settings.utils.AsyncLoader;

import static com.android.settings.search.IndexDatabaseHelper.Tables.TABLE_SAVED_QUERIES;

/**
 * A background task to update saved queries.
 */
public class SavedQueryRecorder extends AsyncLoader<Void> {

    private static final String LOG_TAG = "SavedQueryRecorder";

    // Max number of saved search queries (who will be used for proposing suggestions)
    private static long MAX_SAVED_SEARCH_QUERY = 64;

    private final String mQuery;

    public SavedQueryRecorder(Context context, String query) {
        super(context);
        mQuery = query;
    }

    @Override
    protected void onDiscardResult(Void result) {

    }

    @Override
    public Void loadInBackground() {
        final long now = System.currentTimeMillis();

        final ContentValues values = new ContentValues();
        values.put(IndexDatabaseHelper.SavedQueriesColumns.QUERY, mQuery);
        values.put(IndexDatabaseHelper.SavedQueriesColumns.TIME_STAMP, now);

        final SQLiteDatabase database = getWritableDatabase();
        if (database == null) {
            return null;
        }

        long lastInsertedRowId;
        try {
            // First, delete all saved queries that are the same
            database.delete(TABLE_SAVED_QUERIES,
                    IndexDatabaseHelper.SavedQueriesColumns.QUERY + " = ?",
                    new String[]{mQuery});

            // Second, insert the saved query
            lastInsertedRowId = database.insertOrThrow(TABLE_SAVED_QUERIES, null, values);

            // Last, remove "old" saved queries
            final long delta = lastInsertedRowId - MAX_SAVED_SEARCH_QUERY;
            if (delta > 0) {
                int count = database.delete(TABLE_SAVED_QUERIES,
                        "rowId <= ?",
                        new String[]{Long.toString(delta)});
                Log.d(LOG_TAG, "Deleted '" + count + "' saved Search query(ies)");
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, "Cannot update saved Search queries", e);
        }
        return null;
    }

    private SQLiteDatabase getWritableDatabase() {
        try {
            return IndexDatabaseHelper.getInstance(getContext()).getWritableDatabase();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Cannot open writable database", e);
            return null;
        }
    }
}
