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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.search.Index;
import com.android.settings.search.IndexDatabaseHelper;
import com.android.settings.utils.AsyncLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * AsyncTask to retrieve Settings, First party app and any intent based results.
 */
public class DatabaseResultLoader extends AsyncLoader<List<SearchResult>> {
    private static final String LOG = "DatabaseResultLoader";
    private final String mQueryText;

    protected final SQLiteDatabase mDatabase;

    private final CursorToSearchResultConverter mConverter;

    /* These indices are used to match the columns of the this loader's SELECT statement.
     These are not necessarily the same order or coverage as the schema defined in
     IndexDatabaseHelper */
    public static final int COLUMN_INDEX_RANK = 0;
    public static final int COLUMN_INDEX_TITLE = 1;
    public static final int COLUMN_INDEX_SUMMARY_ON = 2;
    public static final int COLUMN_INDEX_SUMMARY_OFF = 3;
    public static final int COLUMN_INDEX_ENTRIES = 4;
    public static final int COLUMN_INDEX_KEYWORDS = 5;
    public static final int COLUMN_INDEX_CLASS_NAME = 6;
    public static final int COLUMN_INDEX_SCREEN_TITLE = 7;
    public static final int COLUMN_INDEX_ICON = 8;
    public static final int COLUMN_INDEX_INTENT_ACTION = 9;
    public static final int COLUMN_INDEX_INTENT_ACTION_TARGET_PACKAGE = 10;
    public static final int COLUMN_INDEX_INTENT_ACTION_TARGET_CLASS = 11;
    public static final int COLUMN_INDEX_ENABLED = 12;
    public static final int COLUMN_INDEX_KEY = 13;
    public static final int COLUMN_INDEX_PAYLOAD_TYPE = 14;
    public static final int COLUMN_INDEX_PAYLOAD = 15;

    public DatabaseResultLoader(Context context, String queryText) {
        super(context);
        mDatabase = IndexDatabaseHelper.getInstance(context).getReadableDatabase();
        mQueryText = queryText;
        mConverter = new CursorToSearchResultConverter(context);
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
        Cursor result = mDatabase.rawQuery(query, null);

        return mConverter.convertCursor(result);
    }

    @Override
    protected boolean onCancelLoad() {
        // TODO
        return super.onCancelLoad();
    }

    protected String getSQLQuery() {
        return String.format("SELECT data_rank, data_title, data_summary_on, " +
                        "data_summary_off, data_entries, data_keywords, class_name, screen_title,"
                        + " icon, " +
                        "intent_action, intent_target_package, intent_target_class, enabled, " +
                        "data_key_reference, payload_type, payload FROM prefs_index WHERE prefs_index MATCH "
                        + "'data_title:%s* " +
                        "OR data_title_normalized:%s* OR data_keywords:%s*' AND locale = 'en_US'",
                mQueryText, mQueryText, mQueryText);
    }



}
