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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class CardDatabaseHelperTest {

    private Context mContext;
    private CardDatabaseHelper mCardDatabaseHelper;
    private SQLiteDatabase mDatabase;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mCardDatabaseHelper = CardDatabaseHelper.getInstance(mContext);
        mDatabase = mCardDatabaseHelper.getReadableDatabase();
    }

    @After
    public void cleanUp() {
        CardDatabaseHelper.getInstance(mContext).close();
        CardDatabaseHelper.sCardDatabaseHelper = null;
    }

    @Test
    public void testDatabaseSchema() {
        final Cursor cursor = mDatabase.rawQuery("SELECT * FROM " + CardDatabaseHelper.CARD_TABLE,
                null);
        final String[] columnNames = cursor.getColumnNames();

        final String[] expectedNames = {
                CardDatabaseHelper.CardColumns.NAME,
                CardDatabaseHelper.CardColumns.TYPE,
                CardDatabaseHelper.CardColumns.SCORE,
                CardDatabaseHelper.CardColumns.SLICE_URI,
                CardDatabaseHelper.CardColumns.CATEGORY,
                CardDatabaseHelper.CardColumns.PACKAGE_NAME,
                CardDatabaseHelper.CardColumns.APP_VERSION,
                CardDatabaseHelper.CardColumns.DISMISSED_TIMESTAMP,
        };

        assertThat(columnNames).isEqualTo(expectedNames);
        cursor.close();
    }
}
