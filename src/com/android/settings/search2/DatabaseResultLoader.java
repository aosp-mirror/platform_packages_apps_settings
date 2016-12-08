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
        Cursor result = mDatabase.rawQuery(query, null);

        return parseCursorForSearch(result);
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
                        "data_key_reference FROM prefs_index WHERE prefs_index MATCH "
                        + "'data_title:%s* " +
                        "OR data_title_normalized:%s* OR data_keywords:%s*' AND locale = 'en_US'",
                mQueryText, mQueryText, mQueryText);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public ArrayList<SearchResult> parseCursorForSearch(Cursor cursorResults) {
        if (cursorResults == null) {
            return null;
        }
        final Map<String, Context> contextMap = new HashMap<>();
        final ArrayList<SearchResult> results = new ArrayList<>();

        while (cursorResults.moveToNext()) {
            SearchResult result = buildSingleSearchResultFromCursor(contextMap, cursorResults);
            if (result != null) {
                results.add(result);
            }
        }
        Collections.sort(results);
        return results;
    }

    private SearchResult buildSingleSearchResultFromCursor(Map<String, Context> contextMap,
            Cursor cursor) {
        final String pkgName = cursor.getString(Index.COLUMN_INDEX_INTENT_ACTION_TARGET_PACKAGE);
        final String action = cursor.getString(Index.COLUMN_INDEX_INTENT_ACTION);
        final String title = cursor.getString(Index.COLUMN_INDEX_TITLE);
        final String summaryOn = cursor.getString(Index.COLUMN_INDEX_SUMMARY_ON);
        final String className = cursor.getString(Index.COLUMN_INDEX_CLASS_NAME);
        final int rank = cursor.getInt(Index.COLUMN_INDEX_RANK);
        final String key = cursor.getString(Index.COLUMN_INDEX_KEY);
        final String iconResStr = cursor.getString(Index.COLUMN_INDEX_ICON);

        final ResultPayload payload;
        if (TextUtils.isEmpty(action)) {
            final String screenTitle = cursor.getString(Index.COLUMN_INDEX_SCREEN_TITLE);
            // Action is null, we will launch it as a sub-setting
            final Bundle args = new Bundle();
            args.putString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, key);
            final Intent intent = Utils.onBuildStartFragmentIntent(mContext,
                    className, args, null, 0, screenTitle, false);
            payload = new IntentPayload(intent);
        } else {
            final Intent intent = new Intent(action);
            final String targetClass = cursor.getString(
                    Index.COLUMN_INDEX_INTENT_ACTION_TARGET_CLASS);
            if (!TextUtils.isEmpty(pkgName) && !TextUtils.isEmpty(targetClass)) {
                final ComponentName component = new ComponentName(pkgName, targetClass);
                intent.setComponent(component);
            }
            intent.putExtra(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, key);
            payload = new IntentPayload(intent);
        }
        SearchResult.Builder builder = new SearchResult.Builder();
        builder.addTitle(title)
                .addSummary(summaryOn)
                .addRank(rank)
                .addIcon(getIconForPackage(contextMap, pkgName, className, iconResStr))
                .addPayload(payload);
        return builder.build();
    }

    private Drawable getIconForPackage(Map<String, Context> contextMap, String pkgName,
            String className, String iconResStr) {
        final int iconId = TextUtils.isEmpty(iconResStr)
                ? 0 : Integer.parseInt(iconResStr);
        Drawable icon;
        Context packageContext;
        if (iconId == 0) {
            icon = null;
        } else {
            if (TextUtils.isEmpty(className) && !TextUtils.isEmpty(pkgName)) {
                packageContext = contextMap.get(pkgName);
                if (packageContext == null) {
                    try {
                        packageContext = mContext.createPackageContext(pkgName, 0);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e(LOG, "Cannot create Context for package: " + pkgName);
                        return null;
                    }
                    contextMap.put(pkgName, packageContext);
                }
            } else {
                packageContext = mContext;
            }
            try {
                icon = packageContext.getDrawable(iconId);
            } catch (Resources.NotFoundException nfe) {
                icon = null;
            }
        }
        return icon;
    }

}
