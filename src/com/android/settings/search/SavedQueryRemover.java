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

import static com.android.settings.search.IndexDatabaseHelper.Tables.TABLE_SAVED_QUERIES;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.android.settings.utils.AsyncLoader;

public class SavedQueryRemover extends AsyncLoader<Void> {

    private static final String LOG_TAG = "SavedQueryRemover";

    public SavedQueryRemover(Context context) {
        super(context);
    }

    @Override
    public Void loadInBackground() {
        final SQLiteDatabase database = getWritableDatabase();
        try {
            // First, delete all saved queries that are the same
            database.delete(TABLE_SAVED_QUERIES,
                    null /* where */,
                    null /* whereArgs */);
        } catch (Exception e) {
            Log.d(LOG_TAG, "Cannot update saved Search queries", e);
        }
        return null;
    }

    @Override
    protected void onDiscardResult(Void result) {

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
