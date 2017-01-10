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
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.search2.DatabaseIndexingUtils;
import com.android.settings.search2.DatabaseResultLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DatabaseResultLoaderTest {
    private Context mContext;

    private DatabaseResultLoader loader;

    SQLiteDatabase mDb;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mDb = IndexDatabaseHelper.getInstance(mContext).getWritableDatabase();
        setUpDb();
    }

    @After
    public void cleanUp() {
        Field instance;
        Class clazz = IndexDatabaseHelper.class;
        try {
            instance = clazz.getDeclaredField("sSingleton");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    @Test
    public void testMatchTitle() {
        loader = new DatabaseResultLoader(mContext, "title");
        assertThat(loader.loadInBackground().size()).isEqualTo(3);
    }

    @Test
    public void testMatchSummary() {
        loader = new DatabaseResultLoader(mContext, "summary");
        assertThat(loader.loadInBackground().size()).isEqualTo(3);
    }

    @Test
    public void testMatchKeywords() {
        loader = new DatabaseResultLoader(mContext, "keywords");
        assertThat(loader.loadInBackground().size()).isEqualTo(3);
    }

    @Test
    public void testMatchEntries() {
        loader = new DatabaseResultLoader(mContext, "entries");
        assertThat(loader.loadInBackground().size()).isEqualTo(3);
    }

    @Test
    public void testSpecialCaseWord_MatchesNonPrefix() {
        insertSpecialCase("Data usage");
        loader = new DatabaseResultLoader(mContext, "usage");
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseSpace_Matches() {
        insertSpecialCase("space");
        loader = new DatabaseResultLoader(mContext, " space ");
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseDash_MatchesWordNoDash() {
        insertSpecialCase("wi-fi calling");
        loader = new DatabaseResultLoader(mContext, "wifi");
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseDash_MatchesWordWithDash() {
        insertSpecialCase("priorités seulment");
        loader = new DatabaseResultLoader(mContext, "priorités");
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseDash_MatchesWordWithoutDash() {
        insertSpecialCase("priorités seulment");
        loader = new DatabaseResultLoader(mContext, "priorites");
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseDash_MatchesEntireQueryWithoutDash() {
        insertSpecialCase("wi-fi calling");
        loader = new DatabaseResultLoader(mContext, "wifi calling");
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    private void insertSpecialCase(String specialCase) {
        String normalized = DatabaseIndexingUtils.normalizeHyphen(specialCase);
        normalized = DatabaseIndexingUtils.normalizeString(normalized);

        ContentValues values = new ContentValues();
        values.put(IndexDatabaseHelper.IndexColumns.DOCID, 0);
        values.put(IndexDatabaseHelper.IndexColumns.LOCALE, "en-us");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_RANK, 1);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_TITLE, specialCase);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_TITLE_NORMALIZED, normalized);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON, "");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON_NORMALIZED, "");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF, "");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF_NORMALIZED, "");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_ENTRIES, "");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEYWORDS, "");
        values.put(IndexDatabaseHelper.IndexColumns.CLASS_NAME,
                "com.android.settings.gestures.GestureSettings");
        values.put(IndexDatabaseHelper.IndexColumns.SCREEN_TITLE, "Moves");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_ACTION, "");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_PACKAGE, "");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_CLASS, "");
        values.put(IndexDatabaseHelper.IndexColumns.ICON, "");
        values.put(IndexDatabaseHelper.IndexColumns.ENABLED, "");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEY_REF, "gesture_double_tap_power");
        values.put(IndexDatabaseHelper.IndexColumns.USER_ID, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD_TYPE, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD, (String) null);

        mDb.replaceOrThrow(IndexDatabaseHelper.Tables.TABLE_PREFS_INDEX, null, values);
    }

    private void setUpDb() {
        ContentValues values = new ContentValues();
        values.put(IndexDatabaseHelper.IndexColumns.DOCID, 0);
        values.put(IndexDatabaseHelper.IndexColumns.LOCALE, "en-us");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_RANK, 1);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_TITLE, "alpha_title");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_TITLE_NORMALIZED, "alpha title");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON, "alpha_summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON_NORMALIZED, "alpha_summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF, "alpha_summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF_NORMALIZED, "alpha_summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_ENTRIES, "alpha_entries");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEYWORDS, "alpha_keywords");
        values.put(IndexDatabaseHelper.IndexColumns.CLASS_NAME,
                "com.android.settings.gestures.GestureSettings");
        values.put(IndexDatabaseHelper.IndexColumns.SCREEN_TITLE, "Moves");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_ACTION, "");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_PACKAGE, "");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_CLASS, "");
        values.put(IndexDatabaseHelper.IndexColumns.ICON, "");
        values.put(IndexDatabaseHelper.IndexColumns.ENABLED, "");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEY_REF, "gesture_double_tap_power");
        values.put(IndexDatabaseHelper.IndexColumns.USER_ID, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD_TYPE, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD, (String) null);

        mDb.replaceOrThrow(IndexDatabaseHelper.Tables.TABLE_PREFS_INDEX, null, values);

        values = new ContentValues();
        values.put(IndexDatabaseHelper.IndexColumns.DOCID, 1);
        values.put(IndexDatabaseHelper.IndexColumns.LOCALE, "en-us");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_RANK, 1);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_TITLE, "bravo_title");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_TITLE_NORMALIZED, "bravo title");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON, "bravo_summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON_NORMALIZED, "bravo_summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF, "bravo_summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF_NORMALIZED, "bravo_summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_ENTRIES, "bravo_entries");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEYWORDS, "bravo_keywords");
        values.put(IndexDatabaseHelper.IndexColumns.CLASS_NAME,
                "com.android.settings.gestures.GestureSettings");
        values.put(IndexDatabaseHelper.IndexColumns.SCREEN_TITLE, "Moves");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_ACTION, "");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_PACKAGE, "");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_CLASS, "");
        values.put(IndexDatabaseHelper.IndexColumns.ICON, "");
        values.put(IndexDatabaseHelper.IndexColumns.ENABLED, "");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEY_REF, "gesture_double_tap_power");
        values.put(IndexDatabaseHelper.IndexColumns.USER_ID, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD_TYPE, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD, (String) null);
        mDb.replaceOrThrow(IndexDatabaseHelper.Tables.TABLE_PREFS_INDEX, null, values);

        values = new ContentValues();
        values.put(IndexDatabaseHelper.IndexColumns.DOCID, 2);
        values.put(IndexDatabaseHelper.IndexColumns.LOCALE, "en-us");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_RANK, 1);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_TITLE, "charlie_title");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_TITLE_NORMALIZED, "charlie title");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON, "charlie_summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON_NORMALIZED, "charlie_summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF, "charlie_summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF_NORMALIZED, "charlie_summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_ENTRIES, "charlie_entries");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEYWORDS, "charlie_keywords");
        values.put(IndexDatabaseHelper.IndexColumns.CLASS_NAME,
                "com.android.settings.gestures.GestureSettings");
        values.put(IndexDatabaseHelper.IndexColumns.SCREEN_TITLE, "Moves");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_ACTION, "");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_PACKAGE, "");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_CLASS, "");
        values.put(IndexDatabaseHelper.IndexColumns.ICON, "");
        values.put(IndexDatabaseHelper.IndexColumns.ENABLED, "");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEY_REF, "gesture_double_tap_power");
        values.put(IndexDatabaseHelper.IndexColumns.USER_ID, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD_TYPE, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD, (String) null);

        mDb.replaceOrThrow(IndexDatabaseHelper.Tables.TABLE_PREFS_INDEX, null, values);
    }
}
