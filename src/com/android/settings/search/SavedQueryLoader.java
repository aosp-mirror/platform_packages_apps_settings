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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.VisibleForTesting;

import com.android.settings.search.IndexDatabaseHelper.SavedQueriesColumns;
import com.android.settings.utils.AsyncLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Loader for recently searched queries.
 */
public class SavedQueryLoader extends AsyncLoader<List<? extends SearchResult>> {

    // Max number of proposed suggestions
    @VisibleForTesting
    static final int MAX_PROPOSED_SUGGESTIONS = 5;

    private final SQLiteDatabase mDatabase;

    public SavedQueryLoader(Context context) {
        super(context);
        mDatabase = IndexDatabaseHelper.getInstance(context).getReadableDatabase();
    }

    @Override
    protected void onDiscardResult(List<? extends SearchResult> result) {

    }

    @Override
    public List<? extends SearchResult> loadInBackground() {
        try (final Cursor cursor = mDatabase.query(
                IndexDatabaseHelper.Tables.TABLE_SAVED_QUERIES /* table */,
                new String[]{SavedQueriesColumns.QUERY} /* columns */,
                null /* selection */,
                null /* selectionArgs */,
                null /* groupBy */,
                null /* having */,
                "rowId DESC" /* orderBy */,
                String.valueOf(MAX_PROPOSED_SUGGESTIONS) /* limit */)) {
            return convertCursorToResult(cursor);
        }
    }

    private List<SearchResult> convertCursorToResult(Cursor cursor) {
        final List<SearchResult> results = new ArrayList<>();
        while (cursor.moveToNext()) {
            final SavedQueryPayload payload = new SavedQueryPayload(
                    cursor.getString(cursor.getColumnIndex(SavedQueriesColumns.QUERY)));
            results.add(new SearchResult.Builder()
                    .setStableId(payload.hashCode())
                    .setTitle(payload.query)
                    .setPayload(payload)
                    .build());
        }
        return results;
    }
}
