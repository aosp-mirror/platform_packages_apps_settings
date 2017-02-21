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

package com.android.settings.search2;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.SiteMapManager;
import com.android.settings.search.IndexDatabaseHelper;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DatabaseResultLoaderTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mMockContext;
    @Mock
    private SiteMapManager mSiteMapManager;
    private Context mContext;
    private DatabaseResultLoader loader;

    SQLiteDatabase mDb;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        FakeFeatureFactory.setupForTest(mMockContext);
        FakeFeatureFactory factory =
                (FakeFeatureFactory) FakeFeatureFactory.getFactory(mMockContext);
        when(factory.searchFeatureProvider.getSiteMapManager())
                .thenReturn(mSiteMapManager);
        mDb = IndexDatabaseHelper.getInstance(mContext).getWritableDatabase();
        setUpDb();
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb();
    }

    @Test
    public void testMatchTitle() {
        loader = new DatabaseResultLoader(mContext, "title", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(2);
        verify(mSiteMapManager, times(2)).buildBreadCrumb(eq(mContext), anyString(), anyString());
    }

    @Test
    public void testMatchSummary() {
        loader = new DatabaseResultLoader(mContext, "summary", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(2);
    }

    @Test
    public void testMatchKeywords() {
        loader = new DatabaseResultLoader(mContext, "keywords", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(2);
    }

    @Test
    public void testMatchEntries() {
        loader = new DatabaseResultLoader(mContext, "entries", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(2);
    }

    @Test
    public void testSpecialCaseWord_MatchesNonPrefix() {
        insertSpecialCase("Data usage");
        loader = new DatabaseResultLoader(mContext, "usage", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseSpace_Matches() {
        insertSpecialCase("space");
        loader = new DatabaseResultLoader(mContext, " space ", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseDash_MatchesWordNoDash() {
        insertSpecialCase("wi-fi calling");
        loader = new DatabaseResultLoader(mContext, "wifi", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseDash_MatchesWordWithDash() {
        insertSpecialCase("priorités seulment");
        loader = new DatabaseResultLoader(mContext, "priorités", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseDash_MatchesWordWithoutDash() {
        insertSpecialCase("priorités seulment");
        loader = new DatabaseResultLoader(mContext, "priorites", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseDash_MatchesEntireQueryWithoutDash() {
        insertSpecialCase("wi-fi calling");
        loader = new DatabaseResultLoader(mContext, "wifi calling", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCasePrefix_MatchesPrefixOfEntry() {
        insertSpecialCase("Photos");
        loader = new DatabaseResultLoader(mContext, "pho", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCasePrefix_DoesNotMatchNonPrefixSubstring() {
        insertSpecialCase("Photos");
        loader = new DatabaseResultLoader(mContext, "hot", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(0);
    }

    @Test
    public void testSpecialCaseMultiWordPrefix_MatchesPrefixOfEntry() {
        insertSpecialCase("Apps Notifications");
        loader = new DatabaseResultLoader(mContext, "Apps", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseMultiWordPrefix_MatchesSecondWordPrefixOfEntry() {
        insertSpecialCase("Apps Notifications");
        loader = new DatabaseResultLoader(mContext, "Not", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseMultiWordPrefix_DoesNotMatchMatchesPrefixOfFirstEntry() {
        insertSpecialCase("Apps Notifications");
        loader = new DatabaseResultLoader(mContext, "pp", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(0);
    }

    @Test
    public void testSpecialCaseMultiWordPrefix_DoesNotMatchMatchesPrefixOfSecondEntry() {
        insertSpecialCase("Apps Notifications");
        loader = new DatabaseResultLoader(mContext, "tion", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(0);
    }

    @Test
    public void testSpecialCaseMultiWordPrefixWithSpecial_MatchesPrefixOfEntry() {
        insertSpecialCase("Apps & Notifications");
        loader = new DatabaseResultLoader(mContext, "App", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseMultiWordPrefixWithSpecial_MatchesPrefixOfSecondEntry() {
        insertSpecialCase("Apps & Notifications");
        loader = new DatabaseResultLoader(mContext, "No", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseTwoWords_FirstWordMatches_RanksHigher() {
        final String caseOne = "Apple pear";
        final String caseTwo = "Banana apple";
        insertSpecialCase(caseOne);
        insertSpecialCase(caseTwo);
        loader = new DatabaseResultLoader(mContext, "App", null);
        List<? extends SearchResult> results = loader.loadInBackground();

        assertThat(results.get(0).title).isEqualTo(caseOne);
        assertThat(results.get(1).title).isEqualTo(caseTwo);
        assertThat(results.get(0).rank).isLessThan(results.get(1).rank);
    }

    private void insertSpecialCase(String specialCase) {
        String normalized = DatabaseIndexingUtils.normalizeHyphen(specialCase);
        normalized = DatabaseIndexingUtils.normalizeString(normalized);

        ContentValues values = new ContentValues();
        values.put(IndexDatabaseHelper.IndexColumns.DOCID, normalized.hashCode());
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
        values.put(IndexDatabaseHelper.IndexColumns.ENABLED, true);
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
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON_NORMALIZED, "alpha summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF, "alpha_summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF_NORMALIZED, "alpha summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_ENTRIES, "alpha entries");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEYWORDS, "alpha keywords");
        values.put(IndexDatabaseHelper.IndexColumns.CLASS_NAME,
                "com.android.settings.gestures.GestureSettings");
        values.put(IndexDatabaseHelper.IndexColumns.SCREEN_TITLE, "Moves");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_ACTION, "");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_PACKAGE, "");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_CLASS, "");
        values.put(IndexDatabaseHelper.IndexColumns.ICON, "");
        values.put(IndexDatabaseHelper.IndexColumns.ENABLED, true);
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
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON_NORMALIZED, "bravo summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF, "bravo_summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF_NORMALIZED, "bravo summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_ENTRIES, "bravo entries");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEYWORDS, "bravo keywords");
        values.put(IndexDatabaseHelper.IndexColumns.CLASS_NAME,
                "com.android.settings.gestures.GestureSettings");
        values.put(IndexDatabaseHelper.IndexColumns.SCREEN_TITLE, "Moves");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_ACTION, "");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_PACKAGE, "");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_CLASS, "");
        values.put(IndexDatabaseHelper.IndexColumns.ICON, "");
        values.put(IndexDatabaseHelper.IndexColumns.ENABLED, true);
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
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON_NORMALIZED, "charlie summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF, "charlie_summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF_NORMALIZED, "charlie summary");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_ENTRIES, "charlie entries");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEYWORDS, "charlie keywords");
        values.put(IndexDatabaseHelper.IndexColumns.CLASS_NAME,
                "com.android.settings.gestures.GestureSettings");
        values.put(IndexDatabaseHelper.IndexColumns.SCREEN_TITLE, "Moves");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_ACTION, "");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_PACKAGE, "");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_CLASS, "");
        values.put(IndexDatabaseHelper.IndexColumns.ICON, "");
        values.put(IndexDatabaseHelper.IndexColumns.ENABLED, false);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEY_REF, "gesture_double_tap_power");
        values.put(IndexDatabaseHelper.IndexColumns.USER_ID, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD_TYPE, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD, (String) null);

        mDb.replaceOrThrow(IndexDatabaseHelper.Tables.TABLE_PREFS_INDEX, null, values);
    }
}