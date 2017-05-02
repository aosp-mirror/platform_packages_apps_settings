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

package com.android.settings.testutils.shadow;

import android.content.ContentResolver;
import android.content.SyncAdapterType;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.SearchIndexablesContract;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import static android.provider.SearchIndexablesContract.INDEXABLES_RAW_COLUMNS;

@Implements(ContentResolver.class)
public class ShadowContentResolver {

    @Implementation
    public static SyncAdapterType[] getSyncAdapterTypesAsUser(int userId) {
        return new SyncAdapterType[0];
    }

    @Implementation
    public final Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        MatrixCursor cursor = new MatrixCursor(INDEXABLES_RAW_COLUMNS);
        MatrixCursor.RowBuilder builder = cursor.newRow()
                .add(SearchIndexablesContract.NonIndexableKey.COLUMN_KEY_VALUE, "");
        return cursor;
    }
}
