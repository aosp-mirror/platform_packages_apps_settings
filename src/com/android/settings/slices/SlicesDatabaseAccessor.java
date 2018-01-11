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
 * limitations under the License
 */

package com.android.settings.slices;

import static com.android.settings.slices.SlicesDatabaseHelper.Tables.TABLE_SLICES_INDEX;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import android.content.Context;
import android.os.Binder;

import com.android.settings.overlay.FeatureFactory;
import com.android.settings.slices.SlicesDatabaseHelper.IndexColumns;

import androidx.app.slice.Slice;

/**
 * Class used to map a {@link Uri} from {@link SettingsSliceProvider} to a Slice.
 */
public class SlicesDatabaseAccessor {

    public static final String[] SELECT_COLUMNS = {
            IndexColumns.KEY,
            IndexColumns.TITLE,
            IndexColumns.SUMMARY,
            IndexColumns.SCREENTITLE,
            IndexColumns.ICON_RESOURCE,
            IndexColumns.FRAGMENT,
            IndexColumns.CONTROLLER,
    };

    Context mContext;

    public SlicesDatabaseAccessor(Context context) {
        mContext = context;
    }

    /**
     * Query the slices database and return a {@link SliceData} object corresponding to the row
     * matching the key provided by the {@param uri}. Additionally adds the {@param uri} to the
     * {@link SliceData} object so the {@link Slice} can bind to the {@link Uri}.
     * Used when building a {@link Slice}.
     */
    public SliceData getSliceDataFromUri(Uri uri) {
        String key = uri.getLastPathSegment();
        Cursor cursor = getIndexedSliceData(key);
        return buildSliceData(cursor, uri);
    }

    /**
     * Query the slices database and return a {@link SliceData} object corresponding to the row
     * matching the {@param key}.
     * Used when handling the action of the {@link Slice}.
     */
    public SliceData getSliceDataFromKey(String key) {
        Cursor cursor = getIndexedSliceData(key);
        return buildSliceData(cursor, null /* uri */);
    }

    private Cursor getIndexedSliceData(String path) {
        verifyIndexing();

        final String whereClause = buildWhereClause();
        final SlicesDatabaseHelper helper = SlicesDatabaseHelper.getInstance(mContext);
        final SQLiteDatabase database = helper.getReadableDatabase();
        final String[] selection = new String[]{path};

        Cursor resultCursor = database.query(TABLE_SLICES_INDEX, SELECT_COLUMNS, whereClause,
                selection, null /* groupBy */, null /* having */, null /* orderBy */);

        int numResults = resultCursor.getCount();

        if (numResults == 0) {
            throw new IllegalStateException("Invalid Slices key from path: " + path);
        }

        if (numResults > 1) {
            throw new IllegalStateException(
                    "Should not match more than 1 slice with path: " + path);
        }

        resultCursor.moveToFirst();
        return resultCursor;
    }

    private String buildWhereClause() {
        return new StringBuilder(IndexColumns.KEY)
                .append(" = ?")
                .toString();
    }

    private SliceData buildSliceData(Cursor cursor, Uri uri) {
        final String key = cursor.getString(cursor.getColumnIndex(IndexColumns.KEY));
        final String title = cursor.getString(cursor.getColumnIndex(IndexColumns.TITLE));
        final String summary = cursor.getString(cursor.getColumnIndex(IndexColumns.SUMMARY));
        final String screenTitle = cursor.getString(
                cursor.getColumnIndex(IndexColumns.SCREENTITLE));
        final int iconResource = cursor.getInt(cursor.getColumnIndex(IndexColumns.ICON_RESOURCE));
        final String fragmentClassName = cursor.getString(
                cursor.getColumnIndex(IndexColumns.FRAGMENT));
        final String controllerClassName = cursor.getString(
                cursor.getColumnIndex(IndexColumns.CONTROLLER));

        return new SliceData.Builder()
                .setKey(key)
                .setTitle(title)
                .setSummary(summary)
                .setScreenTitle(screenTitle)
                .setIcon(iconResource)
                .setFragmentName(fragmentClassName)
                .setPreferenceControllerClassName(controllerClassName)
                .setUri(uri)
                .build();
    }

    private void verifyIndexing() {
        final long uidToken = Binder.clearCallingIdentity();
        try {
            FeatureFactory.getFactory(
                    mContext).getSlicesFeatureProvider().indexSliceData(mContext);
        } finally {
            Binder.restoreCallingIdentity(uidToken);
        }
    }
}