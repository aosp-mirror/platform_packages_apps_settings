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
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
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

import java.util.ArrayList;
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

    private final String titleOne = "titleOne";
    private final String titleTwo = "titleTwo";
    private final String titleThree = "titleThree";
    private final String titleFour = "titleFour";
    private final String summaryOne = "summaryOne";
    private final String summaryTwo = "summaryTwo";
    private final String summaryThree = "summaryThree";

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
    public void testSpecialCaseWord_matchesNonPrefix() {
        insertSpecialCase("Data usage");
        loader = new DatabaseResultLoader(mContext, "usage", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseSpace_matches() {
        insertSpecialCase("space");
        loader = new DatabaseResultLoader(mContext, " space ", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseDash_matchesWordNoDash() {
        insertSpecialCase("wi-fi calling");
        loader = new DatabaseResultLoader(mContext, "wifi", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseDash_matchesWordWithDash() {
        insertSpecialCase("priorités seulment");
        loader = new DatabaseResultLoader(mContext, "priorités", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseDash_matchesWordWithoutDash() {
        insertSpecialCase("priorités seulment");
        loader = new DatabaseResultLoader(mContext, "priorites", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseDash_matchesEntireQueryWithoutDash() {
        insertSpecialCase("wi-fi calling");
        loader = new DatabaseResultLoader(mContext, "wifi calling", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCasePrefix_matchesPrefixOfEntry() {
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
    public void testSpecialCaseMultiWordPrefix_matchesPrefixOfEntry() {
        insertSpecialCase("Apps Notifications");
        loader = new DatabaseResultLoader(mContext, "Apps", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseMultiWordPrefix_matchesSecondWordPrefixOfEntry() {
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
    public void testSpecialCaseMultiWordPrefixWithSpecial_matchesPrefixOfEntry() {
        insertSpecialCase("Apps & Notifications");
        loader = new DatabaseResultLoader(mContext, "App", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseMultiWordPrefixWithSpecial_matchesPrefixOfSecondEntry() {
        insertSpecialCase("Apps & Notifications");
        loader = new DatabaseResultLoader(mContext, "No", mSiteMapManager);
        assertThat(loader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void testDeDupe_noDuplicates_originalListReturn() {
        // Three elements with unique titles and summaries
        List<SearchResult> results = new ArrayList();
        IntentPayload intentPayload = new IntentPayload(new Intent());

        SearchResult.Builder builder = new SearchResult.Builder();
        builder.addTitle(titleOne)
                .addSummary(summaryOne)
                .addPayload(intentPayload);
        SearchResult resultOne = builder.build();
        results.add(resultOne);

        builder.addTitle(titleTwo)
                .addSummary(summaryTwo);
        SearchResult resultTwo = builder.build();
        results.add(resultTwo);

        builder.addTitle(titleThree)
                .addSummary(summaryThree);
        SearchResult resultThree = builder.build();
        results.add(resultThree);

        loader = new DatabaseResultLoader(mContext, "", null);
        loader.removeDuplicates(results);
        assertThat(results.size()).isEqualTo(3);
        assertThat(results.get(0)).isEqualTo(resultOne);
        assertThat(results.get(1)).isEqualTo(resultTwo);
        assertThat(results.get(2)).isEqualTo(resultThree);
    }

    @Test
    public void testDeDupe_oneDuplicate_duplicateRemoved() {
        List<SearchResult> results = new ArrayList();
        IntentPayload intentPayload = new IntentPayload(new Intent());

        SearchResult.Builder builder = new SearchResult.Builder();
        builder.addTitle(titleOne)
                .addSummary(summaryOne)
                .addRank(0)
                .addPayload(intentPayload);
        SearchResult resultOne = builder.build();
        results.add(resultOne);

        // Duplicate of the first element
        builder.addTitle(titleOne)
                .addSummary(summaryOne)
                .addRank(1);
        SearchResult resultTwo = builder.build();
        results.add(resultTwo);

        // Unique
        builder.addTitle(titleThree)
                .addSummary(summaryThree);
        SearchResult resultThree = builder.build();
        results.add(resultThree);

        loader = new DatabaseResultLoader(mContext, "", null);
        loader.removeDuplicates(results);
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(0)).isEqualTo(resultOne);
        assertThat(results.get(1)).isEqualTo(resultThree);
    }

    @Test
    public void testDeDupe_firstDupeInline_secondDuplicateRemoved() {
        List<SearchResult> results = new ArrayList();
        InlineSwitchPayload inlinePayload = new InlineSwitchPayload("", 0,
                null);
        IntentPayload intentPayload = new IntentPayload(new Intent());

        SearchResult.Builder builder = new SearchResult.Builder();
        // Inline result
        builder.addTitle(titleOne)
                .addSummary(summaryOne)
                .addRank(0)
                .addPayload(inlinePayload);
        SearchResult resultOne = builder.build();
        results.add(resultOne);

        // Duplicate of first result, but Intent Result. Should be removed.
        builder.addTitle(titleOne)
                .addSummary(summaryOne)
                .addRank(1)
                .addPayload(intentPayload);
        SearchResult resultTwo = builder.build();
        results.add(resultTwo);

        // Unique
        builder.addTitle(titleThree)
                .addSummary(summaryThree);
        SearchResult resultThree = builder.build();
        results.add(resultThree);

        loader = new DatabaseResultLoader(mContext, "", null);
        loader.removeDuplicates(results);
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(0)).isEqualTo(resultOne);
        assertThat(results.get(1)).isEqualTo(resultThree);
    }

    @Test
    public void testDeDupe_secondDupeInline_firstDuplicateRemoved() {
        /*
         * Create a list as follows:
         * (5) Intent Four
         * (4) Inline Two
         * (3) Intent Three
         * (2) Intent Two
         * (1) Intent One
         *
         * After removing duplicates:
         * (4) Intent Four
         * (3) Inline Two
         * (2) Intent Three
         * (1) Intent One
         */
        List<SearchResult> results = new ArrayList();
        InlineSwitchPayload inlinePayload = new InlineSwitchPayload("", 0,
                null);
        IntentPayload intentPayload = new IntentPayload(new Intent());


        SearchResult.Builder builder = new SearchResult.Builder();
        // Intent One
        builder.addTitle(titleOne)
                .addSummary(summaryOne)
                .addPayload(intentPayload);
        SearchResult resultOne = builder.build();
        results.add(resultOne);

        // Intent Two
        builder.addTitle(titleTwo)
                .addSummary(summaryTwo)
                .addPayload(intentPayload);
        SearchResult resultTwo = builder.build();
        results.add(resultTwo);

        // Intent Three
        builder.addTitle(titleThree)
                .addSummary(summaryThree);
        SearchResult resultThree = builder.build();
        results.add(resultThree);

        // Inline Two
        builder.addTitle(titleTwo)
                .addSummary(summaryTwo)
                .addPayload(inlinePayload);
        SearchResult resultFour = builder.build();
        results.add(resultFour);

        // Intent Four
        builder.addTitle(titleFour)
                .addSummary(summaryOne)
                .addPayload(intentPayload);
        SearchResult resultFive = builder.build();
        results.add(resultFive);

        loader = new DatabaseResultLoader(mContext, "", null);
        loader.removeDuplicates(results);
        assertThat(results.size()).isEqualTo(4);
        assertThat(results.get(0)).isEqualTo(resultOne);
        assertThat(results.get(1)).isEqualTo(resultThree);
        assertThat(results.get(2)).isEqualTo(resultFour);
        assertThat(results.get(3)).isEqualTo(resultFive);
    }

    @Test
    public void testDeDupe_threeDuplicates_onlyOneStays() {
        /*
         * Create a list as follows:
         * (3) Intent One
         * (2) Intent One
         * (1) Intent One
         *
         * After removing duplicates:
         * (1) Intent One
         */
        List<SearchResult> results = new ArrayList();
        IntentPayload intentPayload = new IntentPayload(new Intent());

        SearchResult.Builder builder = new SearchResult.Builder();
        // Intent One
        builder.addTitle(titleOne)
                .addSummary(summaryOne)
                .addPayload(intentPayload);
        SearchResult resultOne = builder.build();
        results.add(resultOne);

        // Intent Two
        builder.addTitle(titleOne)
                .addSummary(summaryOne)
                .addPayload(intentPayload);
        SearchResult resultTwo = builder.build();
        results.add(resultTwo);

        // Intent Three
        builder.addTitle(titleOne)
                .addSummary(summaryOne)
                .addPayload(intentPayload);
        SearchResult resultThree = builder.build();
        results.add(resultThree);

        loader = new DatabaseResultLoader(mContext, "", null);
        loader.removeDuplicates(results);
        assertThat(results.size()).isEqualTo(1);
    }

    @Test
    public void testSpecialCaseTwoWords_firstWordMatches_ranksHigher() {
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