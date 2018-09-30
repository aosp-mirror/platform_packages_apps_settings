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

package com.android.settings.homepage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class CardContentLoaderTest {
    private static final String[] QUERY_PROJECTION = {
            CardDatabaseHelper.CardColumns.NAME,
            CardDatabaseHelper.CardColumns.TYPE,
            CardDatabaseHelper.CardColumns.SCORE,
            CardDatabaseHelper.CardColumns.SLICE_URI,
            CardDatabaseHelper.CardColumns.CATEGORY,
            CardDatabaseHelper.CardColumns.LOCALIZED_TO_LOCALE,
            CardDatabaseHelper.CardColumns.PACKAGE_NAME,
            CardDatabaseHelper.CardColumns.APP_VERSION,
            CardDatabaseHelper.CardColumns.TITLE_RES_NAME,
            CardDatabaseHelper.CardColumns.TITLE_TEXT,
            CardDatabaseHelper.CardColumns.SUMMARY_RES_NAME,
            CardDatabaseHelper.CardColumns.SUMMARY_TEXT,
            CardDatabaseHelper.CardColumns.ICON_RES_NAME,
            CardDatabaseHelper.CardColumns.ICON_RES_ID,
            CardDatabaseHelper.CardColumns.CARD_ACTION,
            CardDatabaseHelper.CardColumns.EXPIRE_TIME_MS,
            CardDatabaseHelper.CardColumns.SUPPORT_HALF_WIDTH
    };

    private Context mContext;
    private CardContentLoader mCardContentLoader;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mCardContentLoader = spy(new CardContentLoader(mContext));
    }

    @Test
    public void loadInBackground_hasDataInDb_shouldReturnData() {
        final Cursor cursor = generateTwoRowContextualCards();
        doReturn(cursor).when(mCardContentLoader).getContextualCardsFromProvider();

        final List<ContextualCard> contextualCards = mCardContentLoader.loadInBackground();

        assertThat(contextualCards.size()).isEqualTo(cursor.getCount());
    }

    @Test
    public void loadInBackground_hasNoData_shouldReturnThreeDefaultData() {
        final Cursor cursor = generateEmptyContextualCards();
        doReturn(cursor).when(mCardContentLoader).getContextualCardsFromProvider();

        final List<ContextualCard> contextualCards = mCardContentLoader.loadInBackground();

        assertThat(contextualCards.size()).isEqualTo(mCardContentLoader.createStaticCards().size());
    }

    private MatrixCursor generateEmptyContextualCards() {
        final MatrixCursor result = new MatrixCursor(QUERY_PROJECTION);
        return result;
    }

    private MatrixCursor generateTwoRowContextualCards() {
        final MatrixCursor result = generateEmptyContextualCards();
        result.addRow(generateFirstFakeData());
        result.addRow(generateSecondFakeData());
        return result;
    }

    private Object[] generateFirstFakeData() {
        final Object[] ref = new Object[]{
                "auto_rotate", /* NAME */
                ContextualCard.CardType.SLICE, /* TYPE */
                0.5, /* SCORE */
                "content://com.android.settings.slices/action/auto_rotate", /* SLICE_URI */
                2, /* CATEGORY */
                "", /* LOCALIZED_TO_LOCALE */
                "com.android.settings", /* PACKAGE_NAME */
                1l, /* APP_VERSION */
                "", /* TITLE_RES_NAME */
                "", /* TITLE_TEXT */
                "", /* SUMMARY_RES_NAME */
                "", /* SUMMARY_TEXT */
                "", /* ICON_RES_NAME */
                0, /* ICON_RES_ID */
                0, /* CARD_ACTION */
                -1, /* EXPIRE_TIME_MS */
                0 /* SUPPORT_HALF_WIDTH */
        };
        return ref;
    }

    private Object[] generateSecondFakeData() {
        final Object[] ref = new Object[]{
                "toggle_airplane", /* NAME */
                ContextualCard.CardType.SLICE, /* TYPE */
                0.5, /* SCORE */
                "content://com.android.settings.slices/action/toggle_airplane", /* SLICE_URI */
                2, /* CATEGORY */
                "", /* LOCALIZED_TO_LOCALE */
                "com.android.settings", /* PACKAGE_NAME */
                1l, /* APP_VERSION */
                "", /* TITLE_RES_NAME */
                "", /* TITLE_TEXT */
                "", /* SUMMARY_RES_NAME */
                "", /* SUMMARY_TEXT */
                "", /* ICON_RES_NAME */
                0, /* ICON_RES_ID */
                0, /* CARD_ACTION */
                -1, /* EXPIRE_TIME_MS */
                0 /* SUPPORT_HALF_WIDTH */
        };
        return ref;
    }
}
