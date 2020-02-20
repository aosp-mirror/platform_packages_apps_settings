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

import android.content.Context;
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
    private static final int DATABASE_VERSION = 7;

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
         * Package name for all card candidates.
         */
        String PACKAGE_NAME = "package_name";

        /**
         * Application version of the package.
         */
        String APP_VERSION = "app_version";

        /**
         * Timestamp of card being dismissed.
         */
        String DISMISSED_TIMESTAMP = "dismissed_timestamp";
    }

    private static final String CREATE_CARD_TABLE =
            "CREATE TABLE "
                    + CARD_TABLE
                    + "("
                    + CardColumns.NAME
                    + " TEXT NOT NULL PRIMARY KEY, "
                    + CardColumns.TYPE
                    + " INTEGER NOT NULL, "
                    + CardColumns.SCORE
                    + " DOUBLE NOT NULL, "
                    + CardColumns.SLICE_URI
                    + " TEXT, "
                    + CardColumns.CATEGORY
                    + " INTEGER DEFAULT 0, "
                    + CardColumns.PACKAGE_NAME
                    + " TEXT NOT NULL, "
                    + CardColumns.APP_VERSION
                    + " INTEGER NOT NULL, "
                    + CardColumns.DISMISSED_TIMESTAMP
                    + " INTEGER"
                    + ");";

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
}
