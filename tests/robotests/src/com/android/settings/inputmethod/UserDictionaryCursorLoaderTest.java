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
 */

package com.android.settings.inputmethod;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.UserDictionary;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowContentResolver;

@RunWith(RobolectricTestRunner.class)
public class UserDictionaryCursorLoaderTest {

    private ContentProvider mContentProvider;
    private UserDictionaryCursorLoader mLoader;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContentProvider = new FakeProvider();
        mLoader = new UserDictionaryCursorLoader(RuntimeEnvironment.application, "" /* locale */);
        ShadowContentResolver.registerProviderInternal(UserDictionary.AUTHORITY, mContentProvider);
    }

    @Test
    public void testLoad_shouldRemoveDuplicate() {
        final Cursor cursor = mLoader.loadInBackground();

        assertThat(cursor.getCount()).isEqualTo(4);
    }

    public static class FakeProvider extends ContentProvider {

        @Override
        public boolean onCreate() {
            return false;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                String sortOrder) {
            final MatrixCursor cursor = new MatrixCursor(
                    UserDictionaryCursorLoader.QUERY_PROJECTION);
            cursor.addRow(new Object[]{1, "word1", "shortcut1"});
            cursor.addRow(new Object[]{2, "word2", "shortcut2"});
            cursor.addRow(new Object[]{3, "word3", "shortcut3"});
            cursor.addRow(new Object[]{4, "word3", "shortcut3"});   // dupe of 3
            cursor.addRow(new Object[]{5, "word5", null});          // no shortcut
            return cursor;
        }

        @Override
        public String getType(Uri uri) {
            return null;
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            return null;
        }

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            return 0;
        }

        @Override
        public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            return 0;
        }
    }
}
