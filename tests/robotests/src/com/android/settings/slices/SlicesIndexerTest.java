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

import com.android.settings.TestConfig;
import com.android.settings.slices.SlicesDatabaseHelper.IndexColumns;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SlicesIndexerTest {

    private final String[] KEYS = new String[]{"key1", "key2", "key3"};
    private final String[] TITLES = new String[]{"title1", "title2", "title3"};
    private final String SUMMARY = "subtitle";
    private final String SCREEN_TITLE = "screen title";
    private final String FRAGMENT_NAME = "fragment name";
    private final int ICON = 1234; // I declare a thumb war
    private final Uri URI = Uri.parse("content://com.android.settings.slices/test");
    private final String PREF_CONTROLLER = "com.android.settings.slices.tester";

    private Context mContext;

    private SlicesIndexer mManager;

    private SQLiteDatabase mDb;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mManager = spy(new SlicesIndexer(mContext));
        mDb = SlicesDatabaseHelper.getInstance(mContext).getWritableDatabase();
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void testAlreadyIndexed_doesNotIndexAgain() {
        String newKey = "newKey";
        String newTitle = "newTitle";
        SlicesDatabaseHelper.getInstance(mContext).setIndexedState();
        Locale.setDefault(new Locale("ca"));
        insertSpecialCase(newKey, newTitle);

        // Attempt indexing - should not do anything.
        mManager.run();

        Cursor cursor = mDb.rawQuery("SELECT * FROM slices_index", null);
        cursor.moveToFirst();
        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getString(cursor.getColumnIndex(IndexColumns.KEY))).isEqualTo(newKey);
        assertThat(cursor.getString(cursor.getColumnIndex(IndexColumns.TITLE))).isEqualTo(newTitle);
    }

    @Test
    public void testInsertSliceData_indexedStateSet() {
        SlicesDatabaseHelper helper = SlicesDatabaseHelper.getInstance(mContext);
        helper.setIndexedState();
        doReturn(new ArrayList<SliceData>()).when(mManager).getSliceData();

        mManager.run();

        assertThat(helper.isSliceDataIndexed()).isTrue();
    }

    @Test
    public void testInsertSliceData_mockDataInserted() {
        List<SliceData> sliceData = getDummyIndexableData();
        doReturn(sliceData).when(mManager).getSliceData();

        mManager.run();

        Cursor cursor = mDb.rawQuery("SELECT * FROM slices_index", null);
        assertThat(cursor.getCount()).isEqualTo(sliceData.size());

        cursor.moveToFirst();
        for (int i = 0; i < sliceData.size(); i++) {
            assertThat(cursor.getString(cursor.getColumnIndex(IndexColumns.KEY))).isEqualTo(
                    KEYS[i]);
            assertThat(cursor.getString(cursor.getColumnIndex(IndexColumns.TITLE))).isEqualTo(
                    TITLES[i]);
            cursor.moveToNext();
        }
    }

    private void insertSpecialCase(String key, String title) {
        ContentValues values = new ContentValues();
        values.put(IndexColumns.KEY, key);
        values.put(IndexColumns.TITLE, title);

        mDb.replaceOrThrow(SlicesDatabaseHelper.Tables.TABLE_SLICES_INDEX, null, values);
    }

    private List<SliceData> getDummyIndexableData() {
        List<SliceData> sliceData = new ArrayList<>();
        SliceData.Builder builder = new SliceData.Builder();
        builder.setSummary(SUMMARY)
                .setScreenTitle(SCREEN_TITLE)
                .setFragmentName(FRAGMENT_NAME)
                .setIcon(ICON)
                .setUri(URI)
                .setPreferenceControllerClassName(PREF_CONTROLLER);

        for (int i = 0; i < KEYS.length; i++) {
            builder.setKey(KEYS[i])
                    .setTitle(TITLES[i]);
            sliceData.add(builder.build());
        }

        return sliceData;
    }
}