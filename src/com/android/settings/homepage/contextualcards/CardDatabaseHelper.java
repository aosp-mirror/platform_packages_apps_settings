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
 * limitations under the License.
 */

package com.android.settings.homepage.contextualcards;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

/**
 * Defines the schema for the Homepage Cards database.
 */
public class CardDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "CardDatabaseHelper";
    private static final String DATABASE_NAME = "homepage_cards.db";
    private static final int DATABASE_VERSION = 5;

    public static final String CARD_TABLE = "cards";

    public interface CardColumns {
        /**
         * Primary key. Name of the card.
         */
        String NAME = "name";

        /**
         * Type of the card.
         */
        String TYPE = "type";

        /**
         * Score of the card. Higher numbers have higher priorities.
         */
        String SCORE = "score";

        /**
         * URI of the slice card.
         */
        String SLICE_URI = "slice_uri";

        /**
         * Category of the card.
         */
        String CATEGORY = "category";

        /**
         * Keep the card last display's locale.
         */
        String LOCALIZED_TO_LOCALE = "localized_to_locale";

        /**
         * Package name for all card candidates.
         */
        String PACKAGE_NAME = "package_name";

        /**
         * Application version of the package.
         */
        String APP_VERSION = "app_version";

        /**
         * Title resource name of the package.
         */
        String TITLE_RES_NAME = "title_res_name";

        /**
         * Title of the package to be shown.
         */
        String TITLE_TEXT = "title_text";

        /**
         * Summary resource name of the package.
         */
        String SUMMARY_RES_NAME = "summary_res_name";

        /**
         * Summary of the package to be shown.
         */
        String SUMMARY_TEXT = "summary_text";

        /**
         * Icon resource name of the package.
         */
        String ICON_RES_NAME = "icon_res_name";

        /**
         * Icon resource id of the package.
         */
        String ICON_RES_ID = "icon_res_id";

        /**
         * Key value mapping to Intent in Settings. Do action when user presses card.
         */
        String CARD_ACTION = "card_action";

        /**
         * Expire time of the card. The unit of the value is mini-second.
         */
        String EXPIRE_TIME_MS = "expire_time_ms";

        /**
         * Decide the card display full-length width or half-width in screen.
         */
        String SUPPORT_HALF_WIDTH = "support_half_width";

        /**
         * Decide the card is dismissed or not.
         */
        String CARD_DISMISSED = "card_dismissed";
    }

    private static final String CREATE_CARD_TABLE =
            "CREATE TABLE " + CARD_TABLE +
                    "(" +
                    CardColumns.NAME +
                    " TEXT NOT NULL PRIMARY KEY, " +
                    CardColumns.TYPE +
                    " INTEGER NOT NULL, " +
                    CardColumns.SCORE +
                    " DOUBLE NOT NULL, " +
                    CardColumns.SLICE_URI +
                    " TEXT, " +
                    CardColumns.CATEGORY +
                    " INTEGER DEFAULT 0, " +
                    CardColumns.LOCALIZED_TO_LOCALE +
                    " TEXT, " +
                    CardColumns.PACKAGE_NAME +
                    " TEXT NOT NULL, " +
                    CardColumns.APP_VERSION +
                    " INTEGER NOT NULL, " +
                    CardColumns.TITLE_RES_NAME +
                    " TEXT, " +
                    CardColumns.TITLE_TEXT +
                    " TEXT, " +
                    CardColumns.SUMMARY_RES_NAME +
                    " TEXT, " +
                    CardColumns.SUMMARY_TEXT +
                    " TEXT, " +
                    CardColumns.ICON_RES_NAME +
                    " TEXT, " +
                    CardColumns.ICON_RES_ID +
                    " INTEGER DEFAULT 0, " +
                    CardColumns.CARD_ACTION +
                    " INTEGER, " +
                    CardColumns.EXPIRE_TIME_MS +
                    " INTEGER, " +
                    CardColumns.SUPPORT_HALF_WIDTH +
                    " INTEGER DEFAULT 0, " +
                    CardColumns.CARD_DISMISSED +
                    " INTEGER DEFAULT 0 " +
                    ");";

    public CardDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_CARD_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < newVersion) {
            Log.d(TAG, "Reconstructing DB from " + oldVersion + " to " + newVersion);
            db.execSQL("DROP TABLE IF EXISTS " + CARD_TABLE);
            onCreate(db);
        }
    }

    @VisibleForTesting
    static CardDatabaseHelper sCardDatabaseHelper;

    public static synchronized CardDatabaseHelper getInstance(Context context) {
        if (sCardDatabaseHelper == null) {
            sCardDatabaseHelper = new CardDatabaseHelper(context.getApplicationContext());
        }
        return sCardDatabaseHelper;
    }

    Cursor getContextualCards() {
        final SQLiteDatabase db = getReadableDatabase();
        final String selection = CardColumns.CARD_DISMISSED + "=0";
        return db.query(CARD_TABLE, null /* columns */, selection,
                null /* selectionArgs */, null /* groupBy */, null /* having */,
                CardColumns.SCORE + " DESC" /* orderBy */);
    }

    /**
     * Mark a specific ContextualCard with dismissal flag in the database to indicate that the
     * card has been dismissed.
     *
     * @param context  Context
     * @param cardName The card name of the ContextualCard which is dismissed by user.
     * @return The number of rows updated
     */
    public int markContextualCardAsDismissed(Context context, String cardName) {
        final SQLiteDatabase database = getWritableDatabase();
        final ContentValues values = new ContentValues();
        values.put(CardColumns.CARD_DISMISSED, 1);
        final String selection = CardColumns.NAME + "=?";
        final String[] selectionArgs = {cardName};
        final int rowsUpdated = database.update(CARD_TABLE, values, selection, selectionArgs);
        database.close();
        context.getContentResolver().notifyChange(CardContentProvider.DELETE_CARD_URI, null);
        return rowsUpdated;
    }
}
