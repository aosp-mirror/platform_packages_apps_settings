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
 * limitations under the License.
 *
 */

package com.android.settings.search;


import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.DatabaseTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SavedQueryLoaderTest {

    private Context mContext;
    private SQLiteDatabase mDb;
    private SavedQueryLoader mLoader;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mDb = IndexDatabaseHelper.getInstance(mContext).getWritableDatabase();
        mLoader = new SavedQueryLoader(mContext);
        setUpDb();
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void loadInBackground_shouldReturnSavedQueries() {
        final List<? extends SearchResult> results = mLoader.loadInBackground();
        assertThat(results.size()).isEqualTo(SavedQueryLoader.MAX_PROPOSED_SUGGESTIONS);
        for (SearchResult result : results) {
            assertThat(result.viewType).isEqualTo(ResultPayload.PayloadType.SAVED_QUERY);
        }
    }

    private void setUpDb() {
        final long now = System.currentTimeMillis();
        for (int i = 0; i < SavedQueryLoader.MAX_PROPOSED_SUGGESTIONS + 2; i++) {
            ContentValues values = new ContentValues();
            values.put(IndexDatabaseHelper.SavedQueriesColumns.QUERY, String.valueOf(i));
            values.put(IndexDatabaseHelper.SavedQueriesColumns.TIME_STAMP, now);
            mDb.replaceOrThrow(IndexDatabaseHelper.Tables.TABLE_SAVED_QUERIES, null, values);
        }
    }
}
