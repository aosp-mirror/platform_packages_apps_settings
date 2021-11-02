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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.android.settings.slices.SlicesDatabaseHelper.IndexColumns;
import com.android.settings.testutils.DatabaseTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SlicesIndexerTest {

    private static final String[] KEYS = new String[]{"key1", "key2", "key3"};
    private static final String[] TITLES = new String[]{"title1", "title2", "title3"};
    private static final String SUMMARY = "subtitle";
    private static final String SCREEN_TITLE = "screen title";
    private static final String KEYWORDS = "a, b, c";
    private static final String FRAGMENT_NAME = "fragment name";
    private static final int ICON = 1234; // I declare a thumb war
    private static final Uri URI = Uri.parse("content://com.android.settings.slices/test");
    private static final String PREF_CONTROLLER = "com.android.settings.slices.tester";
    private static final int SLICE_TYPE = SliceData.SliceType.SLIDER;
    private static final String UNAVAILABLE_SLICE_SUBTITLE = "subtitleOfUnavailableSlice";
    private static final int HIGHLIGHT_MENU_KEY = 5678; // I declare a thumb war

    private Context mContext;

    private SlicesIndexer mManager;


    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mManager = spy(new SlicesIndexer(mContext));
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    @Ignore
    public void testAlreadyIndexed_doesNotIndexAgain() {
        String newKey = "newKey";
        String newTitle = "newTitle";
        SlicesDatabaseHelper.getInstance(mContext).setIndexedState();
        insertSpecialCase(newKey, newTitle);

        // Attempt indexing - should not do anything.
        mManager.run();

        final SQLiteDatabase db = SlicesDatabaseHelper.getInstance(mContext).getWritableDatabase();
        try (final Cursor cursor = db.rawQuery("SELECT * FROM slices_index", null)) {
            cursor.moveToFirst();
            assertThat(cursor.getCount()).isEqualTo(1);
            assertThat(cursor.getString(cursor.getColumnIndex(IndexColumns.KEY))).isEqualTo(newKey);
            assertThat(cursor.getString(cursor.getColumnIndex(IndexColumns.TITLE)))
                    .isEqualTo(newTitle);
        } finally {
            db.close();
        }
    }

    @Test
    public void testInsertSliceData_indexedStateSet() {
        final SlicesDatabaseHelper helper = SlicesDatabaseHelper.getInstance(mContext);
        helper.setIndexedState();
        doReturn(new ArrayList<SliceData>()).when(mManager).getSliceData();

        mManager.run();

        assertThat(helper.isSliceDataIndexed()).isTrue();
    }

    @Test
    @Ignore
    public void testInsertSliceData_nonPublicSlice_mockDataInserted() {
        final List<SliceData> sliceData = getMockIndexableData(false);
        doReturn(sliceData).when(mManager).getSliceData();

        mManager.run();

        final SQLiteDatabase db = SlicesDatabaseHelper.getInstance(mContext).getWritableDatabase();
        try (final Cursor cursor = db.rawQuery("SELECT * FROM slices_index", null)) {
            assertThat(cursor.getCount()).isEqualTo(sliceData.size());

            cursor.moveToFirst();
            for (int i = 0; i < sliceData.size(); i++) {
                assertThat(cursor.getString(cursor.getColumnIndex(IndexColumns.KEY)))
                        .isEqualTo(KEYS[i]);
                assertThat(cursor.getString(cursor.getColumnIndex(IndexColumns.TITLE)))
                        .isEqualTo(TITLES[i]);
                assertThat(
                        cursor.getString(cursor.getColumnIndex(IndexColumns.FRAGMENT)))
                        .isEqualTo(FRAGMENT_NAME);
                assertThat(cursor.getString(
                        cursor.getColumnIndex(IndexColumns.SCREENTITLE))).isEqualTo(SCREEN_TITLE);
                assertThat(
                        cursor.getString(cursor.getColumnIndex(IndexColumns.KEYWORDS)))
                        .isEqualTo(KEYWORDS);
                assertThat(
                        cursor.getInt(cursor.getColumnIndex(IndexColumns.ICON_RESOURCE)))
                        .isEqualTo(ICON);
                assertThat(
                        cursor.getString(cursor.getColumnIndex(IndexColumns.CONTROLLER)))
                        .isEqualTo(PREF_CONTROLLER);
                assertThat(cursor.getInt(cursor.getColumnIndex(IndexColumns.SLICE_TYPE)))
                        .isEqualTo(SLICE_TYPE);
                assertThat(cursor.getString(
                        cursor.getColumnIndex(IndexColumns.UNAVAILABLE_SLICE_SUBTITLE)))
                        .isEqualTo(UNAVAILABLE_SLICE_SUBTITLE);
                assertThat(cursor.getInt(
                        cursor.getColumnIndex(IndexColumns.PUBLIC_SLICE))).isEqualTo(0);
                assertThat(cursor.getInt(
                        cursor.getColumnIndex(IndexColumns.HIGHLIGHT_MENU_RESOURCE)))
                        .isEqualTo(HIGHLIGHT_MENU_KEY);
                cursor.moveToNext();
            }
        } finally {
            db.close();
        }
    }

    @Test
    @Ignore
    public void insertSliceData_publicSlice_mockDataInserted() {
        final List<SliceData> sliceData = getMockIndexableData(true);
        doReturn(sliceData).when(mManager).getSliceData();

        mManager.run();

        final SQLiteDatabase db = SlicesDatabaseHelper.getInstance(mContext).getWritableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT * FROM slices_index", null)) {
            assertThat(cursor.getCount()).isEqualTo(sliceData.size());

            cursor.moveToFirst();
            for (int i = 0; i < sliceData.size(); i++) {
                assertThat(cursor.getString(cursor.getColumnIndex(IndexColumns.KEY)))
                        .isEqualTo(KEYS[i]);
                assertThat(cursor.getString(cursor.getColumnIndex(IndexColumns.TITLE)))
                        .isEqualTo(TITLES[i]);
                assertThat(
                        cursor.getString(cursor.getColumnIndex(IndexColumns.FRAGMENT)))
                        .isEqualTo(FRAGMENT_NAME);
                assertThat(cursor.getString(
                        cursor.getColumnIndex(IndexColumns.SCREENTITLE))).isEqualTo(SCREEN_TITLE);
                assertThat(
                        cursor.getString(cursor.getColumnIndex(IndexColumns.KEYWORDS)))
                        .isEqualTo(KEYWORDS);
                assertThat(
                        cursor.getInt(cursor.getColumnIndex(IndexColumns.ICON_RESOURCE)))
                        .isEqualTo(ICON);
                assertThat(
                        cursor.getString(cursor.getColumnIndex(IndexColumns.CONTROLLER)))
                        .isEqualTo(PREF_CONTROLLER);
                assertThat(cursor.getInt(cursor.getColumnIndex(IndexColumns.SLICE_TYPE)))
                        .isEqualTo(SLICE_TYPE);
                assertThat(cursor.getString(
                        cursor.getColumnIndex(IndexColumns.UNAVAILABLE_SLICE_SUBTITLE)))
                        .isEqualTo(UNAVAILABLE_SLICE_SUBTITLE);
                assertThat(cursor.getInt(
                        cursor.getColumnIndex(IndexColumns.PUBLIC_SLICE))).isEqualTo(1);
                assertThat(cursor.getInt(
                        cursor.getColumnIndex(IndexColumns.HIGHLIGHT_MENU_RESOURCE)))
                        .isEqualTo(HIGHLIGHT_MENU_KEY);
                cursor.moveToNext();
            }
        } finally {
            db.close();
        }
    }

    private void insertSpecialCase(String key, String title) {
        final ContentValues values = new ContentValues();
        values.put(IndexColumns.KEY, key);
        values.put(IndexColumns.TITLE, title);
        final SQLiteDatabase db = SlicesDatabaseHelper.getInstance(mContext).getWritableDatabase();
        db.beginTransaction();
        try {
            db.replaceOrThrow(SlicesDatabaseHelper.Tables.TABLE_SLICES_INDEX, null, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        db.close();
    }

    private List<SliceData> getMockIndexableData(boolean isPublicSlice) {
        final List<SliceData> sliceData = new ArrayList<>();
        final SliceData.Builder builder = new SliceData.Builder()
                .setSummary(SUMMARY)
                .setScreenTitle(SCREEN_TITLE)
                .setKeywords(KEYWORDS)
                .setFragmentName(FRAGMENT_NAME)
                .setIcon(ICON)
                .setUri(URI)
                .setPreferenceControllerClassName(PREF_CONTROLLER)
                .setSliceType(SLICE_TYPE)
                .setUnavailableSliceSubtitle(UNAVAILABLE_SLICE_SUBTITLE)
                .setHighlightMenuRes(HIGHLIGHT_MENU_KEY);

        if (isPublicSlice) {
            builder.setIsPublicSlice(true);
        }

        for (int i = 0; i < KEYS.length; i++) {
            builder.setKey(KEYS[i]).setTitle(TITLES[i]);
            sliceData.add(builder.build());
        }

        return sliceData;
    }
}