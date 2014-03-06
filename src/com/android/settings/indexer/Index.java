/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.indexer;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import com.android.settings.R;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.android.settings.indexer.IndexDatabaseHelper.Tables;
import static com.android.settings.indexer.IndexDatabaseHelper.IndexColumns;

public class Index {

    private static final String LOG_TAG = "Indexer";

    // Those indices should match the indices of SELECT_COLUMNS !
    public static final int COLUMN_INDEX_TITLE = 1;
    public static final int COLUMN_INDEX_SUMMARY = 2;
    public static final int COLUMN_INDEX_FRAGMENT_NAME = 4;
    public static final int COLUMN_INDEX_FRAGMENT_TITLE = 5;
    public static final int COLUMN_INDEX_ICON = 7;

    // If you change the order of columns here, you SHOULD change the COLUMN_INDEX_XXX values
    private static final String[] SELECT_COLUMNS = new String[] {
            IndexColumns.DATA_RANK,
            IndexColumns.DATA_TITLE,
            IndexColumns.DATA_SUMMARY,
            IndexColumns.DATA_KEYWORDS,
            IndexColumns.FRAGMENT_NAME,
            IndexColumns.FRAGMENT_TITLE,
            IndexColumns.INTENT,
            IndexColumns.ICON
    };

    private static final String[] MATCH_COLUMNS = {
            IndexColumns.DATA_TITLE,
            IndexColumns.DATA_SUMMARY,
            IndexColumns.DATA_KEYWORDS
    };

    private static final String EMPTY = "";
    private static final String NON_BREAKING_HYPHEN = "\u2011";
    private static final String HYPHEN = "-";

    private static Index sInstance;

    private final AtomicBoolean mIsAvailable = new AtomicBoolean(false);
    private final List<IndexableData> mDataToIndex = new ArrayList<IndexableData>();

    private final Context mContext;

    /**
     * A basic singleton
     */
    public static Index getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new Index(context);
        }
        return sInstance;
    }

    public Index(Context context) {
        mContext = context;
    }

    public boolean isAvailable() {
        return mIsAvailable.get();
    }

    public Cursor search(String query) {
        final String sql = buildSQL(query);
        Log.d(LOG_TAG, "Query: " + sql);
        return getReadableDatabase().rawQuery(sql, null);
    }

    private String buildSQL(String query) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildSQLForColumn(query, MATCH_COLUMNS));
        sb.append(" ORDER BY ");
        sb.append(IndexColumns.DATA_RANK);
        return sb.toString();
    }

    private String buildSQLForColumn(String query, String[] columnNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        for (int n = 0; n < SELECT_COLUMNS.length; n++) {
            sb.append(SELECT_COLUMNS[n]);
            if (n < SELECT_COLUMNS.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(" FROM ");
        sb.append(Tables.TABLE_PREFS_INDEX);
        sb.append(" WHERE ");
        sb.append(buildWhereStringForColumns(query, columnNames));

        return sb.toString();
    }

    private String buildWhereStringForColumns(String query, String[] columnNames) {
        final StringBuilder sb = new StringBuilder(Tables.TABLE_PREFS_INDEX);
        sb.append(" MATCH ");
        DatabaseUtils.appendEscapedSQLString(sb, buildMatchStringForColumns(query, columnNames));
        sb.append(" AND ");
        sb.append(IndexColumns.LOCALE);
        sb.append(" = ");
        DatabaseUtils.appendEscapedSQLString(sb, Locale.getDefault().toString());
        return sb.toString();
    }

    private String buildMatchStringForColumns(String query, String[] columnNames) {
        final String value = query + "*";
        StringBuilder sb = new StringBuilder();
        final int count = columnNames.length;
        for (int n = 0; n < count; n++) {
            sb.append(columnNames[n]);
            sb.append(":");
            sb.append(value);
            if (n < count - 1) {
                sb.append(" OR ");
            }
        }
        return sb.toString();
    }

    public void addIndexableData(IndexableData data) {
        mDataToIndex.add(data);
    }

    public void addIndexableData(IndexableData[] array) {
        final int count = array.length;
        for (int n = 0; n < count; n++) {
            addIndexableData(array[n]);
        }
    }

    public boolean update() {
        final IndexTask task = new IndexTask();
        task.execute();
        try {
            return task.get();
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Cannot update index: " + e.getMessage());
            return false;
        } catch (ExecutionException e) {
            Log.e(LOG_TAG, "Cannot update index: " + e.getMessage());
            return false;
        }
    }

    private SQLiteDatabase getReadableDatabase() {
        return IndexDatabaseHelper.getInstance(mContext).getReadableDatabase();
    }

    private SQLiteDatabase getWritableDatabase() {
        return IndexDatabaseHelper.getInstance(mContext).getWritableDatabase();
    }

    /**
     * A private class for updating the Index database
     */
    private class IndexTask extends AsyncTask<Void, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mIsAvailable.set(false);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            mIsAvailable.set(true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            final SQLiteDatabase database = getWritableDatabase();
            boolean result = false;
            final Locale locale = Locale.getDefault();
            final String localeStr = locale.toString();
            if (isLocaleAlreadyIndexed(database, locale)) {
                Log.d(LOG_TAG, "Locale '" + localeStr + "' is already indexed");
                return true;
            }
            final long current = System.currentTimeMillis();
            try {
                database.beginTransaction();
                final int count = mDataToIndex.size();
                for (int n = 0; n < count; n++) {
                    final IndexableData data = mDataToIndex.get(n);
                    indexFromResource(database, locale, data.xmlResId, data.fragmentName,
                            data.iconResId, data.rank);
                }
                database.setTransactionSuccessful();
                result = true;
            } finally {
                database.endTransaction();
            }
            final long now = System.currentTimeMillis();
            Log.d(LOG_TAG, "Indexing locale '" + localeStr + "' took " +
                    (now - current) + " millis");
            return result;
        }

        private boolean isLocaleAlreadyIndexed(SQLiteDatabase database, Locale locale) {
            Cursor cursor = null;
            boolean result = false;
            final StringBuilder sb = new StringBuilder(IndexColumns.LOCALE);
            sb.append(" = ");
            DatabaseUtils.appendEscapedSQLString(sb, locale.toString());
            try {
                // We care only for 1 row
                cursor = database.query(Tables.TABLE_PREFS_INDEX, null,
                        sb.toString(), null, null, null, null, "1");
                final int count = cursor.getCount();
                result = (count >= 1);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return result;
        }

        private void indexFromResource(SQLiteDatabase database, Locale locale, int xmlResId,
                String fragmentName, int iconResId, int rank) {
            XmlResourceParser parser = null;
            final String localeStr = locale.toString();
            try {
                parser = mContext.getResources().getXml(xmlResId);

                int type;
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                        && type != XmlPullParser.START_TAG) {
                    // Parse next until start tag is found
                }

                String nodeName = parser.getName();
                if (!"PreferenceScreen".equals(nodeName)) {
                    throw new RuntimeException(
                            "XML document must start with <PreferenceScreen> tag; found"
                                    + nodeName + " at " + parser.getPositionDescription());
                }

                final int outerDepth = parser.getDepth();
                final AttributeSet attrs = Xml.asAttributeSet(parser);
                final String fragmentTitle = getData(attrs,
                        com.android.internal.R.styleable.Preference, com.android.internal.R.styleable.Preference_title);

                String title = getDataTitle(attrs);
                String summary = getDataSummary(attrs);
                String keywords = getDataKeywords(attrs);

                // Insert rows for the main PreferenceScreen node. Rewrite the data for removing
                // hyphens.
                inserOneRowWithFilteredData(database, localeStr, title, summary, fragmentName,
                        fragmentTitle, iconResId, rank, keywords, NON_BREAKING_HYPHEN, EMPTY);
                inserOneRowWithFilteredData(database, localeStr, title, summary, fragmentName,
                        fragmentTitle, iconResId, rank, keywords, NON_BREAKING_HYPHEN, HYPHEN);

                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                        && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                    if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                        continue;
                    }

                    title = getDataTitle(attrs);
                    summary = getDataSummary(attrs);
                    keywords = getDataKeywords(attrs);

                    // Insert rows for the child nodes of PreferenceScreen
                    inserOneRowWithFilteredData(database, localeStr, title, summary, fragmentName,
                            fragmentTitle, iconResId, rank, keywords, NON_BREAKING_HYPHEN, EMPTY);
                    inserOneRowWithFilteredData(database, localeStr, title, summary, fragmentName,
                            fragmentTitle, iconResId, rank, keywords, NON_BREAKING_HYPHEN, HYPHEN);
                }

            } catch (XmlPullParserException e) {
                throw new RuntimeException("Error parsing PreferenceScreen", e);
            } catch (IOException e) {
                throw new RuntimeException("Error parsing PreferenceScreen", e);
            } finally {
                if (parser != null) parser.close();
            }
        }

        private void inserOneRowWithFilteredData(SQLiteDatabase database, String locale,
                String title, String summary, String fragmentName, String fragmentTitle,
                int iconResId, int rank, String keywords, String seq, String replacement) {

            String updatedTitle;
            String updateSummary;
            if (title != null && title.contains(seq)) {
                updatedTitle = title.replaceAll(seq, replacement);
            } else {
                updatedTitle = title;
            }
            if (summary != null && summary.contains(seq)) {
                updateSummary = summary.replaceAll(seq, replacement);
            } else {
                updateSummary = summary;
            }
            insertOneRow(database, locale,
                    updatedTitle, updateSummary,
                    fragmentName, fragmentTitle, iconResId, rank, keywords);
        }

        private void insertOneRow(SQLiteDatabase database, String locale, String title,
                                  String summary, String fragmentName, String fragmentTitle,
                                  int iconResId, int rank, String keywords) {

            if (TextUtils.isEmpty(title)) {
                return;
            }
            ContentValues values = new ContentValues();
            values.put(IndexColumns.LOCALE, locale);
            values.put(IndexColumns.DATA_RANK, rank);
            values.put(IndexColumns.DATA_TITLE, title);
            values.put(IndexColumns.DATA_SUMMARY, summary);
            values.put(IndexColumns.DATA_KEYWORDS, keywords);
            values.put(IndexColumns.FRAGMENT_NAME, fragmentName);
            values.put(IndexColumns.FRAGMENT_TITLE, fragmentTitle);
            values.put(IndexColumns.INTENT, "");
            values.put(IndexColumns.ICON, iconResId);

            database.insertOrThrow(Tables.TABLE_PREFS_INDEX, null, values);
        }

        private String getDataTitle(AttributeSet attrs) {
            return getData(attrs,
                    com.android.internal.R.styleable.Preference,
                    com.android.internal.R.styleable.Preference_title);
        }

        private String getDataSummary(AttributeSet attrs) {
            return getData(attrs,
                    com.android.internal.R.styleable.Preference,
                    com.android.internal.R.styleable.Preference_summary);
        }

        private String getDataKeywords(AttributeSet attrs) {
            return getData(attrs,
                    R.styleable.Preference,
                    R.styleable.Preference_keywords);
        }

        private String getData(AttributeSet set, int[] attrs, int resId) {
            final TypedArray sa = mContext.obtainStyledAttributes(set, attrs);
            final TypedValue tv = sa.peekValue(resId);

            CharSequence data = null;
            if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                if (tv.resourceId != 0) {
                    data = mContext.getText(tv.resourceId);
                } else {
                    data = tv.string;
                }
            }
            return (data != null) ? data.toString() : null;
        }
    }
}
