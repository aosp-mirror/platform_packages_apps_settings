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

package com.android.settings.search;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.platform.test.annotations.Presubmit;
import android.provider.SearchIndexablesContract;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SettingsSearchIndexablesProviderTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @After
    public void cleanUp() {
        System.clearProperty(SettingsSearchIndexablesProvider.SYSPROP_CRASH_ON_ERROR);
    }

    @Test
    public void testSiteMapPairsFetched() {
        final Uri uri = Uri.parse("content://" + mContext.getPackageName() + "/" +
                SearchIndexablesContract.SITE_MAP_PAIRS_PATH);
        final Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);

        final int size = cursor.getCount();
        assertThat(size).isGreaterThan(0);
        while (cursor.moveToNext()) {
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow(
                    SearchIndexablesContract.SiteMapColumns.PARENT_CLASS)))
                    .isNotEmpty();
            assertThat(cursor.getString(cursor.getColumnIndexOrThrow(
                    SearchIndexablesContract.SiteMapColumns.CHILD_CLASS)))
                    .isNotEmpty();
        }
    }

    /**
     * All {@link Indexable.SearchIndexProvider} should collect a list of non-indexable keys
     * without crashing. This test enables crashing of individual providers in the indexing pipeline
     * and checks that there are no crashes.
     */
    @Test
    @Presubmit
    public void nonIndexableKeys_shouldNotCrash() {
        // Allow crashes in the indexing pipeline.
        System.setProperty(SettingsSearchIndexablesProvider.SYSPROP_CRASH_ON_ERROR,
                "enabled");

        final Uri uri = Uri.parse("content://" + mContext.getPackageName() + "/" +
                SearchIndexablesContract.NON_INDEXABLES_KEYS_PATH);
        mContext.getContentResolver().query(uri, null, null, null, null);
    }
}
