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

import static android.provider.SearchIndexablesContract.INDEXABLES_RAW_COLUMNS;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.SyncAdapterType;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.SearchIndexablesContract;
import android.text.TextUtils;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.HashMap;
import java.util.Map;

@Implements(ContentResolver.class)
public class ShadowContentResolver {

    private static SyncAdapterType[] sSyncAdapterTypes = new SyncAdapterType[0];
    private static Map<String, Integer> sSyncable = new HashMap<>();
    private static Map<String, Boolean> sSyncAutomatically = new HashMap<>();
    private static Map<Integer, Boolean> sMasterSyncAutomatically = new HashMap<>();

    @Implementation
    protected static SyncAdapterType[] getSyncAdapterTypesAsUser(int userId) {
        return sSyncAdapterTypes;
    }

    @Implementation
    protected final Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        MatrixCursor cursor = new MatrixCursor(INDEXABLES_RAW_COLUMNS);
        MatrixCursor.RowBuilder builder = cursor.newRow()
                .add(SearchIndexablesContract.NonIndexableKey.COLUMN_KEY_VALUE, "");
        return cursor;
    }

    @Implementation
    protected static int getIsSyncableAsUser(Account account, String authority, int userId) {
        return sSyncable.containsKey(authority) ? sSyncable.get(authority) : 1;
    }

    @Implementation
    protected static boolean getSyncAutomaticallyAsUser(Account account, String authority,
            int userId) {
        return sSyncAutomatically.containsKey(authority) ? sSyncAutomatically.get(authority) : true;
    }

    @Implementation
    protected static void setSyncAutomaticallyAsUser(Account account, String authority,
            boolean sync, int userId) {
        if (TextUtils.isEmpty(authority)) {
            throw new IllegalArgumentException("Authority must be non-empty");
        }
    }

    @Implementation
    protected static boolean getMasterSyncAutomaticallyAsUser(int userId) {
        return sMasterSyncAutomatically.containsKey(userId)
                ? sMasterSyncAutomatically.get(userId) : true;
    }

    public static void setSyncAdapterTypes(SyncAdapterType[] syncAdapterTypes) {
        sSyncAdapterTypes = syncAdapterTypes;
    }

    public static void setSyncable(String authority, int syncable) {
        sSyncable.put(authority, syncable);
    }

    public static void setSyncAutomatically(String authority, boolean syncAutomatically) {
        sSyncAutomatically.put(authority, syncAutomatically);
    }

    public static void setMasterSyncAutomatically(int userId, boolean syncAutomatically) {
        sMasterSyncAutomatically.put(userId, syncAutomatically);
    }

    public static void reset() {
        sSyncable.clear();
        sSyncAutomatically.clear();
        sMasterSyncAutomatically.clear();
        sSyncAdapterTypes = new SyncAdapterType[0];
    }
}
