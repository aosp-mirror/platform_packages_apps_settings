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

import com.android.settings.slices.SlicesDatabaseHelper.IndexColumns;
import com.android.settings.testutils.DatabaseTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
public class SlicesDatabaseHelperTest {

    private Context mContext;
    private SlicesDatabaseHelper mSlicesDatabaseHelper;
    private SQLiteDatabase mDatabase;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mSlicesDatabaseHelper = spy(SlicesDatabaseHelper.getInstance(mContext));
        mDatabase = mSlicesDatabaseHelper.getWritableDatabase();
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
        mDatabase.close();
    }

    @Test
    public void testDatabaseSchema() {
        Cursor cursor = mDatabase.rawQuery("SELECT * FROM slices_index", null);
        String[] columnNames = cursor.getColumnNames();

        String[] expectedNames = {
                IndexColumns.KEY,
                IndexColumns.SLICE_URI,
                IndexColumns.TITLE,
                IndexColumns.SUMMARY,
                IndexColumns.SCREENTITLE,
                IndexColumns.KEYWORDS,
                IndexColumns.ICON_RESOURCE,
                IndexColumns.FRAGMENT,
                IndexColumns.CONTROLLER,
                IndexColumns.SLICE_TYPE,
                IndexColumns.UNAVAILABLE_SLICE_SUBTITLE,
                IndexColumns.PUBLIC_SLICE
        };

        assertThat(columnNames).isEqualTo(expectedNames);
    }

    @Test
    public void testUpgrade_dropsOldData() {
        ContentValues dummyValues = getDummyRow();

        mDatabase.replaceOrThrow(SlicesDatabaseHelper.Tables.TABLE_SLICES_INDEX, null, dummyValues);
        Cursor baseline = mDatabase.rawQuery("SELECT * FROM slices_index", null);
        assertThat(baseline.getCount()).isEqualTo(1);

        mSlicesDatabaseHelper.onUpgrade(mDatabase, 0, 1);

        Cursor newCursor = mDatabase.rawQuery("SELECT * FROM slices_index", null);
        assertThat(newCursor.getCount()).isEqualTo(0);
    }

    @Test
    public void testIndexState_buildAndLocaleSet() {
        mSlicesDatabaseHelper.reconstruct(mDatabase);

        boolean baseState = mSlicesDatabaseHelper.isSliceDataIndexed();
        assertThat(baseState).isFalse();

        mSlicesDatabaseHelper.setIndexedState();
        boolean indexedState = mSlicesDatabaseHelper.isSliceDataIndexed();
        assertThat(indexedState).isTrue();
    }

    @Test
    public void testLocaleChanges_newIndexingState() {
        mSlicesDatabaseHelper.reconstruct(mDatabase);
        mSlicesDatabaseHelper.setIndexedState();

        Locale.setDefault(new Locale("ca"));

        assertThat(mSlicesDatabaseHelper.isSliceDataIndexed()).isFalse();
    }

    @Test
    public void testBuildFingerprintChanges_newIndexingState() {
        mSlicesDatabaseHelper.reconstruct(mDatabase);
        mSlicesDatabaseHelper.setIndexedState();
        doReturn("newBuild").when(mSlicesDatabaseHelper).getBuildTag();

        assertThat(mSlicesDatabaseHelper.isSliceDataIndexed()).isFalse();
    }

    private ContentValues getDummyRow() {
        final ContentValues values = new ContentValues();
        values.put(IndexColumns.KEY, "key");
        values.put(IndexColumns.TITLE, "title");
        values.put(IndexColumns.SUMMARY, "summary");
        values.put(IndexColumns.ICON_RESOURCE, 99);
        values.put(IndexColumns.FRAGMENT, "fragmentClassName");
        values.put(IndexColumns.CONTROLLER, "preferenceController");

        return values;
    }
}