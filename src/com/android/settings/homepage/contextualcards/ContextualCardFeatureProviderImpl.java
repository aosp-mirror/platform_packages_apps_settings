/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.homepage.contextualcards;

import static com.android.settings.homepage.contextualcards.CardDatabaseHelper.CARD_TABLE;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settingslib.utils.ThreadUtils;

public class ContextualCardFeatureProviderImpl implements ContextualCardFeatureProvider {
    private static final String TAG = "ContextualCardFeatureProvider";

    private final Context mContext;

    public ContextualCardFeatureProviderImpl(Context context) {
        mContext = context;
    }

    @Override
    public Cursor getContextualCards() {
        final SQLiteDatabase db = CardDatabaseHelper.getInstance(mContext).getReadableDatabase();
        //TODO(b/149542061): Make the dismissal duration configurable.
        final long threshold = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS;
        final String selection = CardDatabaseHelper.CardColumns.DISMISSED_TIMESTAMP + " < ? OR "
                + CardDatabaseHelper.CardColumns.DISMISSED_TIMESTAMP + " IS NULL";
        final String[] selectionArgs = {String.valueOf(threshold)};
        final Cursor cursor = db.query(CARD_TABLE, null /* columns */, selection,
                selectionArgs /* selectionArgs */, null /* groupBy */, null /* having */,
                CardDatabaseHelper.CardColumns.SCORE + " DESC" /* orderBy */);
        ThreadUtils.postOnBackgroundThread(() -> resetDismissedTime(threshold));
        return cursor;
    }

    @Override
    public ContextualCard getDefaultContextualCard() {
        return null;
    }

    @Override
    public int markCardAsDismissed(Context context, String cardName) {
        final SQLiteDatabase db = CardDatabaseHelper.getInstance(mContext).getWritableDatabase();
        final ContentValues values = new ContentValues();
        values.put(CardDatabaseHelper.CardColumns.DISMISSED_TIMESTAMP, System.currentTimeMillis());
        final String selection = CardDatabaseHelper.CardColumns.NAME + "=?";
        final String[] selectionArgs = {cardName};
        final int rowsUpdated = db.update(CARD_TABLE, values, selection, selectionArgs);
        context.getContentResolver().notifyChange(CardContentProvider.DELETE_CARD_URI, null);
        return rowsUpdated;
    }

    @VisibleForTesting
    int resetDismissedTime(long threshold) {
        final SQLiteDatabase database =
                CardDatabaseHelper.getInstance(mContext).getWritableDatabase();
        final ContentValues values = new ContentValues();
        values.putNull(CardDatabaseHelper.CardColumns.DISMISSED_TIMESTAMP);
        final String selection = CardDatabaseHelper.CardColumns.DISMISSED_TIMESTAMP + " < ? AND "
                + CardDatabaseHelper.CardColumns.DISMISSED_TIMESTAMP + " IS NOT NULL";
        final String[] selectionArgs = {String.valueOf(threshold)};
        final int rowsUpdated = database.update(CARD_TABLE, values, selection, selectionArgs);
        if (Build.IS_DEBUGGABLE) {
            Log.d(TAG, "Reset " + rowsUpdated + " records of dismissed time.");
        }
        return rowsUpdated;
    }
}
