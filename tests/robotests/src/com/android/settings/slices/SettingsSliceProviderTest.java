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
 *
 */

package com.android.settings.slices;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContentResolver;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.android.settings.TestConfig;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import androidx.app.slice.Slice;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SettingsSliceProviderTest {

    private final String fakeTitle = "title";
    private final String KEY = "key";

    private Context mContext;
    private SettingsSliceProvider mProvider;
    private SQLiteDatabase mDb;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mProvider = spy(new SettingsSliceProvider());
        mDb = SlicesDatabaseHelper.getInstance(mContext).getWritableDatabase();
        SlicesDatabaseHelper.getInstance(mContext).setIndexedState();
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void testInitialSliceReturned_emmptySlice() {
        Uri uri = SettingsSliceProvider.getUri(KEY);
        Slice slice = mProvider.onBindSlice(uri);

        assertThat(slice.getUri()).isEqualTo(uri);
        assertThat(slice.getItems()).isEmpty();
    }

    @Test
    public void testUriBuilder_returnsValidSliceUri() {
        Uri uri = SettingsSliceProvider.getUri(KEY);

        assertThat(uri.getScheme()).isEqualTo(ContentResolver.SCHEME_CONTENT);
        assertThat(uri.getAuthority()).isEqualTo(SettingsSliceProvider.SLICE_AUTHORITY);
        assertThat(uri.getLastPathSegment()).isEqualTo(KEY);
    }

    private void insertSpecialCase(String key) {
        ContentValues values = new ContentValues();
        values.put(SlicesDatabaseHelper.IndexColumns.KEY, key);
        values.put(SlicesDatabaseHelper.IndexColumns.TITLE, fakeTitle);
        values.put(SlicesDatabaseHelper.IndexColumns.SUMMARY, "s");
        values.put(SlicesDatabaseHelper.IndexColumns.SCREENTITLE, "s");
        values.put(SlicesDatabaseHelper.IndexColumns.ICON_RESOURCE, 1234);
        values.put(SlicesDatabaseHelper.IndexColumns.FRAGMENT, "test");
        values.put(SlicesDatabaseHelper.IndexColumns.CONTROLLER, "test");

        mDb.replaceOrThrow(SlicesDatabaseHelper.Tables.TABLE_SLICES_INDEX, null, values);
    }
}