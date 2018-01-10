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
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.android.settings.TestConfig;
import com.android.settings.search.FakeIndexProvider;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SlicesDatabaseAccessorTest {

    private final String fakeTitle = "title";
    private final String fakeSummary = "summary";
    private final String fakeScreenTitle = "screen_title";
    private final int fakeIcon = 1234;
    private final String fakeFragmentClassName = FakeIndexProvider.class.getName();
    private final String fakeControllerName = FakePreferenceController.class.getName();

    private Context mContext;
    private SQLiteDatabase mDb;
    private SlicesDatabaseAccessor mAccessor;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mAccessor = spy(new SlicesDatabaseAccessor(mContext));
        mDb = SlicesDatabaseHelper.getInstance(mContext).getWritableDatabase();
        SlicesDatabaseHelper.getInstance(mContext).setIndexedState();
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void testGetSliceDataFromKey_validKey_validSliceReturned() {
        String key = "key";
        insertSpecialCase(key);

        SliceData data = mAccessor.getSliceDataFromKey(key);

        assertThat(data.getKey()).isEqualTo(key);
        assertThat(data.getTitle()).isEqualTo(fakeTitle);
        assertThat(data.getSummary()).isEqualTo(fakeSummary);
        assertThat(data.getScreenTitle()).isEqualTo(fakeScreenTitle);
        assertThat(data.getIconResource()).isEqualTo(fakeIcon);
        assertThat(data.getFragmentClassName()).isEqualTo(fakeFragmentClassName);
        assertThat(data.getUri()).isNull();
        assertThat(data.getPreferenceController()).isEqualTo(fakeControllerName);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetSliceDataFromKey_invalidKey_errorThrown() {
        String key = "key";

        mAccessor.getSliceDataFromKey(key);
    }

    @Test
    public void testGetSliceFromUri_validUri_validSliceReturned() {
        String key = "key";
        insertSpecialCase(key);
        Uri uri = SettingsSliceProvider.getUri(key);

        SliceData data = mAccessor.getSliceDataFromUri(uri);

        assertThat(data.getKey()).isEqualTo(key);
        assertThat(data.getTitle()).isEqualTo(fakeTitle);
        assertThat(data.getSummary()).isEqualTo(fakeSummary);
        assertThat(data.getScreenTitle()).isEqualTo(fakeScreenTitle);
        assertThat(data.getIconResource()).isEqualTo(fakeIcon);
        assertThat(data.getFragmentClassName()).isEqualTo(fakeFragmentClassName);
        assertThat(data.getUri()).isEqualTo(uri);
        assertThat(data.getPreferenceController()).isEqualTo(fakeControllerName);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetSliceFromUri_invalidUri_errorThrown() {
        Uri uri = SettingsSliceProvider.getUri("durr");

        mAccessor.getSliceDataFromUri(uri);
    }

    private void insertSpecialCase(String key) {
        ContentValues values = new ContentValues();
        values.put(SlicesDatabaseHelper.IndexColumns.KEY, key);
        values.put(SlicesDatabaseHelper.IndexColumns.TITLE, fakeTitle);
        values.put(SlicesDatabaseHelper.IndexColumns.SUMMARY, fakeSummary);
        values.put(SlicesDatabaseHelper.IndexColumns.SCREENTITLE, fakeScreenTitle);
        values.put(SlicesDatabaseHelper.IndexColumns.ICON_RESOURCE, fakeIcon);
        values.put(SlicesDatabaseHelper.IndexColumns.FRAGMENT, fakeFragmentClassName);
        values.put(SlicesDatabaseHelper.IndexColumns.CONTROLLER, fakeControllerName);

        mDb.replaceOrThrow(SlicesDatabaseHelper.Tables.TABLE_SLICES_INDEX, null, values);
    }
}
