/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import android.annotation.Nullable;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.slice.SliceProvider;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.intelligence.ContextualCardProto;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ContextualCardFeatureProviderImplTest {

    private Context mContext;
    private ContextualCardFeatureProviderImpl mImpl;
    private CardDatabaseHelper mCardDatabaseHelper;
    private SQLiteDatabase mDatabase;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mImpl = new ContextualCardFeatureProviderImpl(mContext);
        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
        mCardDatabaseHelper = CardDatabaseHelper.getInstance(mContext);
        mDatabase = mCardDatabaseHelper.getWritableDatabase();
    }

    @After
    public void tearDown() {
        CardDatabaseHelper.getInstance(mContext).close();
        CardDatabaseHelper.sCardDatabaseHelper = null;
    }

    @Test
    public void getContextualCards_shouldSortByScore() {
        insertFakeCard(mDatabase, "card1", 1, "uri1", 1000L);
        insertFakeCard(mDatabase, "card2", 0, "uri2", 1000L);
        insertFakeCard(mDatabase, "card3", 10, "uri3", 1000L);
        // Should sort as 3,1,2
        try (Cursor cursor = mImpl.getContextualCards()) {
            assertThat(cursor.getCount()).isEqualTo(3);
            final List<ContextualCard> cards = new ArrayList<>();
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                cards.add(new ContextualCard(cursor));
            }
            assertThat(cards.get(0).getName()).isEqualTo("card3");
            assertThat(cards.get(1).getName()).isEqualTo("card1");
            assertThat(cards.get(2).getName()).isEqualTo("card2");
        }
    }

    @Test
    public void resetDismissedTime_durationExpired_shouldResetToNull() {
        insertFakeCard(mDatabase, "card1", 1, "uri1", 100L);
        final long threshold = 1000L;

        final int rowsUpdated = mImpl.resetDismissedTime(threshold);

        assertThat(rowsUpdated).isEqualTo(1);
    }

    @Test
    public void resetDismissedTime_durationNotExpired_shouldNotUpdate() {
        insertFakeCard(mDatabase, "card1", 1, "uri1", 1111L);
        final long threshold = 1000L;

        final int rowsUpdated = mImpl.resetDismissedTime(threshold);

        assertThat(rowsUpdated).isEqualTo(0);
    }

    private static void insertFakeCard(
            SQLiteDatabase db, String name, double score, String uri, @Nullable Long time) {
        final ContentValues value = new ContentValues();
        value.put(CardDatabaseHelper.CardColumns.NAME, name);
        value.put(CardDatabaseHelper.CardColumns.SCORE, score);
        value.put(CardDatabaseHelper.CardColumns.SLICE_URI, uri);
        value.put(CardDatabaseHelper.CardColumns.TYPE, ContextualCard.CardType.SLICE);
        value.put(CardDatabaseHelper.CardColumns.CATEGORY,
                ContextualCardProto.ContextualCard.Category.DEFAULT.getNumber());
        value.put(CardDatabaseHelper.CardColumns.PACKAGE_NAME,
                RuntimeEnvironment.application.getPackageName());
        value.put(CardDatabaseHelper.CardColumns.APP_VERSION, 1);
        if (time == null) {
            value.putNull(CardDatabaseHelper.CardColumns.DISMISSED_TIMESTAMP);
        } else {
            value.put(CardDatabaseHelper.CardColumns.DISMISSED_TIMESTAMP, time);
        }

        db.insert(CardDatabaseHelper.CARD_TABLE, null, value);
    }
}
