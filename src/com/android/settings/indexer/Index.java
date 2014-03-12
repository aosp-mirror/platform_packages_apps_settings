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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.android.settings.indexer.IndexDatabaseHelper.Tables;
import static com.android.settings.indexer.IndexDatabaseHelper.IndexColumns;

public class Index {

    private static final String LOG_TAG = "Index";

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
            IndexColumns.DATA_TITLE_NORMALIZED,
            IndexColumns.DATA_SUMMARY,
            IndexColumns.DATA_SUMMARY_NORMALIZED,
            IndexColumns.DATA_KEYWORDS
    };

    private static final String EMPTY = "";
    private static final String NON_BREAKING_HYPHEN = "\u2011";
    private static final String HYPHEN = "-";

    private static Index sInstance;

    private final AtomicBoolean mIsAvailable = new AtomicBoolean(false);

    private final UpdateData mUpdateData = new UpdateData();

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

    public void addIndexableData(IndexableRef[] array) {
        synchronized (mUpdateData) {
            final int count = array.length;
            for (int n = 0; n < count; n++) {
                mUpdateData.dataToAdd.add(array[n]);
            }
        }
    }

    public void deleteIndexableData(String[] array) {
        synchronized (mUpdateData) {
            final int count = array.length;
            for (int n = 0; n < count; n++) {
                mUpdateData.dataToDelete.add(array[n]);
            }
        }
    }

    public boolean update() {
        synchronized (mUpdateData) {
            final UpdateIndexTask task = new UpdateIndexTask();
            task.execute(mUpdateData);
            try {
                final boolean result = task.get();
                mUpdateData.clear();
                return result;
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Cannot update index: " + e.getMessage());
                return false;
            } catch (ExecutionException e) {
                Log.e(LOG_TAG, "Cannot update index: " + e.getMessage());
                return false;
            }
        }
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

    private SQLiteDatabase getReadableDatabase() {
        return IndexDatabaseHelper.getInstance(mContext).getReadableDatabase();
    }

    private SQLiteDatabase getWritableDatabase() {
        return IndexDatabaseHelper.getInstance(mContext).getWritableDatabase();
    }

    /**
     * A private class to describe the update data for the Index database
     */
    private class UpdateData {
        public List<IndexableRef> dataToAdd;
        public List<String> dataToDelete;

        public UpdateData() {
            dataToAdd = new ArrayList<IndexableRef>();
            dataToDelete = new ArrayList<String>();
        }

        public void clear() {
            dataToAdd.clear();
            dataToDelete.clear();
        }
    }

    /**
     * A private class for updating the Index database
     */
    private class UpdateIndexTask extends AsyncTask<UpdateData, Integer, Boolean> {

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
        protected Boolean doInBackground(UpdateData... params) {
            boolean result = false;

            final List<IndexableRef> dataToAdd = params[0].dataToAdd;
            final List<String> dataToDelete = params[0].dataToDelete;
            final SQLiteDatabase database = getWritableDatabase();
            final String localeStr = Locale.getDefault().toString();

            try {
                database.beginTransaction();
                if (dataToAdd.size() > 0) {
                    processDataToAdd(database, localeStr, dataToAdd);
                }
                if (dataToDelete.size() > 0) {
                    processDataToDelete(database, localeStr, dataToDelete);
                }
                database.setTransactionSuccessful();
                result = true;
            } finally {
                database.endTransaction();
            }
            return result;
        }

        private boolean processDataToDelete(SQLiteDatabase database, String localeStr,
                                         List<String> dataToDelete) {

            boolean result = false;
            final long current = System.currentTimeMillis();

            final int count = dataToDelete.size();
            for (int n = 0; n < count; n++) {
                final String data = dataToDelete.get(n);
                delete(database, data);
            }

            final long now = System.currentTimeMillis();
            Log.d(LOG_TAG, "Deleting data for locale '" + localeStr + "' took " +
                    (now - current) + " millis");
            return result;
        }

        private boolean processDataToAdd(SQLiteDatabase database, String localeStr,
                                         List<IndexableRef> dataToAdd) {
            if (isLocaleAlreadyIndexed(database, localeStr)) {
                Log.d(LOG_TAG, "Locale '" + localeStr + "' is already indexed");
                return true;
            }

            boolean result = false;
            final long current = System.currentTimeMillis();

            final int count = dataToAdd.size();
            for (int n = 0; n < count; n++) {
                final IndexableRef ref = dataToAdd.get(n);
                indexOneRef(database, localeStr, ref);
            }

            final long now = System.currentTimeMillis();
            Log.d(LOG_TAG, "Indexing locale '" + localeStr + "' took " +
                    (now - current) + " millis");
            return result;
        }

        private void indexOneRef(SQLiteDatabase database, String localeStr, IndexableRef ref) {
            if (ref.xmlResId > 0) {
                indexFromResource(database, localeStr, ref.xmlResId, ref.fragmentName,
                        ref.iconResId, ref.rank);
            } else if (!TextUtils.isEmpty(ref.fragmentName)) {
                indexRawData(database, localeStr, ref);
            }
        }

        private void indexRawData(SQLiteDatabase database, String localeStr, IndexableRef ref) {
            try {
                final Class<?> clazz = Class.forName(ref.fragmentName);
                if (Indexable.class.isAssignableFrom(clazz)) {
                    final Field f = clazz.getField("INDEX_DATA_PROVIDER");
                    final Indexable.IndexDataProvider provider =
                            (Indexable.IndexDataProvider) f.get(null);

                    final List<IndexableData> data = provider.getRawDataToIndex(mContext);

                    final int size = data.size();
                    for (int i = 0; i < size; i++) {
                        IndexableData raw = data.get(i);

                        // Should be the same locale as the one we are processing
                        if (!raw.locale.toString().equalsIgnoreCase(localeStr)) {
                            continue;
                        }

                        inserOneRowWithFilteredData(database, localeStr,
                                raw.title,
                                raw.summary,
                                ref.fragmentName,
                                raw.fragmentTitle,
                                ref.iconResId,
                                ref.rank,
                                raw.keywords);
                    }
                }
            } catch (ClassNotFoundException e) {
                Log.e(LOG_TAG, "Cannot find class: " + ref.fragmentName, e);
            } catch (NoSuchFieldException e) {
                Log.e(LOG_TAG, "Cannot find field 'INDEX_DATA_PROVIDER'", e);
            } catch (IllegalAccessException e) {
                Log.e(LOG_TAG, "Illegal access to field 'INDEX_DATA_PROVIDER'", e);
            }
        }

        private boolean isLocaleAlreadyIndexed(SQLiteDatabase database, String locale) {
            Cursor cursor = null;
            boolean result = false;
            final StringBuilder sb = new StringBuilder(IndexColumns.LOCALE);
            sb.append(" = ");
            DatabaseUtils.appendEscapedSQLString(sb, locale);
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

        private void indexFromResource(SQLiteDatabase database, String localeStr, int xmlResId,
                String fragmentName, int iconResId, int rank) {
            XmlResourceParser parser = null;
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
                final String fragmentTitle = getDataTitle(attrs);

                String title = getDataTitle(attrs);
                String summary = getDataSummary(attrs);
                String keywords = getDataKeywords(attrs);

                // Insert rows for the main PreferenceScreen node. Rewrite the data for removing
                // hyphens.
                inserOneRowWithFilteredData(database, localeStr, title, summary, fragmentName,
                        fragmentTitle, iconResId, rank, keywords);

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
                            fragmentTitle, iconResId, rank, keywords);
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
                int iconResId, int rank, String keywords) {

            String updatedTitle;
            if (title != null) {
                updatedTitle = title.replaceAll(NON_BREAKING_HYPHEN, HYPHEN);
            }
            else {
                updatedTitle = EMPTY;
            }

            String updatedSummary;
            if (summary != null) {
                updatedSummary = summary.replaceAll(NON_BREAKING_HYPHEN, HYPHEN);
            } else {
                updatedSummary = EMPTY;
            }

            String normalizedTitle = updatedTitle.replaceAll(HYPHEN, EMPTY);
            String normalizedSummary = updatedSummary.replaceAll(HYPHEN, EMPTY);

            insertOneRow(database, locale,
                    updatedTitle, normalizedTitle, updatedSummary, normalizedSummary,
                    fragmentName, fragmentTitle, iconResId, rank, keywords);
        }

        private void insertOneRow(SQLiteDatabase database, String locale,
                                  String updatedTitle, String normalizedTitle,
                                  String updatedSummary, String normalizedSummary,
                                  String fragmentName, String fragmentTitle,
                                  int iconResId, int rank, String keywords) {

            if (TextUtils.isEmpty(updatedTitle)) {
                return;
            }
            ContentValues values = new ContentValues();
            values.put(IndexColumns.LOCALE, locale);
            values.put(IndexColumns.DATA_RANK, rank);
            values.put(IndexColumns.DATA_TITLE, updatedTitle);
            values.put(IndexColumns.DATA_TITLE_NORMALIZED, normalizedTitle);
            values.put(IndexColumns.DATA_SUMMARY, updatedSummary);
            values.put(IndexColumns.DATA_SUMMARY_NORMALIZED, normalizedSummary);
            values.put(IndexColumns.DATA_KEYWORDS, keywords);
            values.put(IndexColumns.FRAGMENT_NAME, fragmentName);
            values.put(IndexColumns.FRAGMENT_TITLE, fragmentTitle);
            values.put(IndexColumns.INTENT, "");
            values.put(IndexColumns.ICON, iconResId);

            database.insertOrThrow(Tables.TABLE_PREFS_INDEX, null, values);
        }

        private int delete(SQLiteDatabase database, String title) {
            final String whereClause = IndexColumns.DATA_TITLE + "=?";
            final String[] whereArgs = new String[] { title };

            return database.delete(Tables.TABLE_PREFS_INDEX, whereClause, whereArgs);
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
