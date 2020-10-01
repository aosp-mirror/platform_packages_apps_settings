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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Binder;
import android.text.TextUtils;
import android.util.Pair;

import androidx.slice.Slice;

import com.android.settings.overlay.FeatureFactory;
import com.android.settings.slices.SlicesDatabaseHelper.IndexColumns;

import java.util.ArrayList;
import java.util.List;

/**
 * Class used to map a {@link Uri} from {@link SettingsSliceProvider} to a Slice.
 */
public class SlicesDatabaseAccessor {

    public static final String[] SELECT_COLUMNS_ALL = {
            IndexColumns.KEY,
            IndexColumns.TITLE,
            IndexColumns.SUMMARY,
            IndexColumns.SCREENTITLE,
            IndexColumns.KEYWORDS,
            IndexColumns.ICON_RESOURCE,
            IndexColumns.FRAGMENT,
            IndexColumns.CONTROLLER,
            IndexColumns.SLICE_TYPE,
            IndexColumns.UNAVAILABLE_SLICE_SUBTITLE,
    };

    private final Context mContext;
    private final SlicesDatabaseHelper mHelper;

    public SlicesDatabaseAccessor(Context context) {
        mContext = context;
        mHelper = SlicesDatabaseHelper.getInstance(mContext);
    }

    /**
     * Query the slices database and return a {@link SliceData} object corresponding to the row
     * matching the key provided by the {@param uri}. Additionally adds the {@param uri} to the
     * {@link SliceData} object so the {@link Slice} can bind to the {@link Uri}.
     * Used when building a {@link Slice}.
     */
    public SliceData getSliceDataFromUri(Uri uri) {
        Pair<Boolean, String> pathData = SliceBuilderUtils.getPathData(uri);
        if (pathData == null) {
            throw new IllegalStateException("Invalid Slices uri: " + uri);
        }
        try (Cursor cursor = getIndexedSliceData(pathData.second /* key */)) {
            return buildSliceData(cursor, uri, pathData.first /* isIntentOnly */);
        }
    }

    /**
     * Query the slices database and return a {@link SliceData} object corresponding to the row
     * matching the {@param key}.
     * Used when handling the action of the {@link Slice}.
     */
    public SliceData getSliceDataFromKey(String key) {
        try (Cursor cursor = getIndexedSliceData(key)) {
            return buildSliceData(cursor, null /* uri */, false /* isIntentOnly */);
        }
    }

    /**
     * @return a list of Slice {@link Uri}s based on their visibility {@param isPublicSlice } and
     * {@param authority}.
     */
    public List<Uri> getSliceUris(String authority, boolean isPublicSlice) {
        verifyIndexing();
        final List<Uri> uris = new ArrayList<>();
        final String whereClause = IndexColumns.PUBLIC_SLICE + (isPublicSlice ? "=1" : "=0");
        final SQLiteDatabase database = mHelper.getReadableDatabase();
        final String[] columns = new String[]{IndexColumns.SLICE_URI};
        try (Cursor resultCursor = database.query(TABLE_SLICES_INDEX, columns,
                whereClause /* where */, null /* selection */, null /* groupBy */,
                null /* having */, null /* orderBy */)) {
            if (!resultCursor.moveToFirst()) {
                return uris;
            }

            do {
                final Uri uri = Uri.parse(resultCursor.getString(0 /* SLICE_URI */));
                if (TextUtils.isEmpty(authority)
                        || TextUtils.equals(authority, uri.getAuthority())) {
                    uris.add(uri);
                }
            } while (resultCursor.moveToNext());
        }
        return uris;
    }

    private Cursor getIndexedSliceData(String path) {
        verifyIndexing();

        final String whereClause = buildKeyMatchWhereClause();
        final SQLiteDatabase database = mHelper.getReadableDatabase();
        final String[] selection = new String[]{path};
        final Cursor resultCursor = database.query(TABLE_SLICES_INDEX, SELECT_COLUMNS_ALL,
                whereClause, selection, null /* groupBy */, null /* having */, null /* orderBy */);

        int numResults = resultCursor.getCount();

        if (numResults == 0) {
            resultCursor.close();
            throw new IllegalStateException("Invalid Slices key from path: " + path);
        }

        if (numResults > 1) {
            resultCursor.close();
            throw new IllegalStateException(
                    "Should not match more than 1 slice with path: " + path);
        }

        resultCursor.moveToFirst();
        return resultCursor;
    }

    private String buildKeyMatchWhereClause() {
        return new StringBuilder(IndexColumns.KEY)
                .append(" = ?")
                .toString();
    }

    private static SliceData buildSliceData(Cursor cursor, Uri uri, boolean isIntentOnly) {
        final String key = cursor.getString(cursor.getColumnIndex(IndexColumns.KEY));
        final String title = cursor.getString(cursor.getColumnIndex(IndexColumns.TITLE));
        final String summary = cursor.getString(cursor.getColumnIndex(IndexColumns.SUMMARY));
        final String screenTitle = cursor.getString(
                cursor.getColumnIndex(IndexColumns.SCREENTITLE));
        final String keywords = cursor.getString(cursor.getColumnIndex(IndexColumns.KEYWORDS));
        final int iconResource = cursor.getInt(cursor.getColumnIndex(IndexColumns.ICON_RESOURCE));
        final String fragmentClassName = cursor.getString(
                cursor.getColumnIndex(IndexColumns.FRAGMENT));
        final String controllerClassName = cursor.getString(
                cursor.getColumnIndex(IndexColumns.CONTROLLER));
        int sliceType = cursor.getInt(
                cursor.getColumnIndex(IndexColumns.SLICE_TYPE));
        final String unavailableSliceSubtitle = cursor.getString(
                cursor.getColumnIndex(IndexColumns.UNAVAILABLE_SLICE_SUBTITLE));

        if (isIntentOnly) {
            sliceType = SliceData.SliceType.INTENT;
        }

        return new SliceData.Builder()
                .setKey(key)
                .setTitle(title)
                .setSummary(summary)
                .setScreenTitle(screenTitle)
                .setKeywords(keywords)
                .setIcon(iconResource)
                .setFragmentName(fragmentClassName)
                .setPreferenceControllerClassName(controllerClassName)
                .setUri(uri)
                .setSliceType(sliceType)
                .setUnavailableSliceSubtitle(unavailableSliceSubtitle)
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