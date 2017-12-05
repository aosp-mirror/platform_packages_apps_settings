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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

import com.android.settings.TestConfig;
import com.android.settings.dashboard.SiteMapManager;
import com.android.settings.search.DatabaseResultLoader.StaticSearchResultCallable;
import com.android.settings.search.indexing.IndexData;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION_O)
public class StaticSearchResultFutureTaskTest {

    @Mock
    private SiteMapManager mSiteMapManager;
    @Mock
    private ExecutorService mService;
    private Context mContext;

    SQLiteDatabase mDb;

    FakeFeatureFactory mFeatureFactory;

    private final String[] STATIC_TITLES = {"static one", "static two", "static three"};
    private final int[] STABLE_IDS =
            {"id_one".hashCode(), "id_two".hashCode(), "id_three".hashCode()};

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mFeatureFactory.searchFeatureProvider.getExecutorService()).thenReturn(mService);
        when(mFeatureFactory.searchFeatureProvider.getSiteMapManager())
                .thenReturn(mSiteMapManager);
        mDb = IndexDatabaseHelper.getInstance(mContext).getWritableDatabase();
        setUpDb();
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void testMatchTitle() throws Exception {
        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, "title",
                mSiteMapManager);

        assertThat(loader.call()).hasSize(2);
        verify(mSiteMapManager, times(2)).buildBreadCrumb(eq(mContext), anyString(), anyString());
    }

    @Test
    public void testMatchSummary() throws Exception {
        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, "summary",
                mSiteMapManager);

        assertThat(loader.call()).hasSize(2);
    }

    @Test
    public void testMatchKeywords() throws Exception {
        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, "keywords",
                mSiteMapManager);

        assertThat(loader.call()).hasSize(2);
    }

    @Test
    public void testMatchEntries() throws Exception {
        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, "entries",
                mSiteMapManager);

        assertThat(loader.call()).hasSize(2);
    }

    @Test
    public void testSpecialCaseWord_matchesNonPrefix() throws Exception {
        insertSpecialCase("Data usage");

        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, "usage",
                mSiteMapManager);

        assertThat(loader.call()).hasSize(1);
    }

    @Test
    public void testSpecialCaseDash_matchesWordNoDash() throws Exception {
        insertSpecialCase("wi-fi calling");

        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, "wifi",
                mSiteMapManager);

        assertThat(loader.call()).hasSize(1);
    }

    @Test
    public void testSpecialCaseDash_matchesWordWithDash() throws Exception {
        insertSpecialCase("priorités seulment");

        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, "priorités",
                mSiteMapManager);

        assertThat(loader.call()).hasSize(1);
    }

    @Test
    public void testSpecialCaseDash_matchesWordWithoutDash() throws Exception {
        insertSpecialCase("priorités seulment");

        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, "priorites",
                mSiteMapManager);

        assertThat(loader.call()).hasSize(1);
    }

    @Test
    public void testSpecialCaseDash_matchesEntireQueryWithoutDash() throws Exception {
        insertSpecialCase("wi-fi calling");

        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, "wifi calling",
                mSiteMapManager);

        assertThat(loader.call()).hasSize(1);
    }

    @Test
    public void testSpecialCasePrefix_matchesPrefixOfEntry() throws Exception {
        insertSpecialCase("Photos");

        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, "pho",
                mSiteMapManager);

        assertThat(loader.call()).hasSize(1);
    }

    @Test
    public void testSpecialCasePrefix_DoesNotMatchNonPrefixSubstring() throws Exception {
        insertSpecialCase("Photos");

        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, "hot",
                mSiteMapManager);

        assertThat(loader.call()).hasSize(0);
    }

    @Test
    public void testSpecialCaseMultiWordPrefix_matchesPrefixOfEntry() throws Exception {
        insertSpecialCase("Apps Notifications");

        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, "Apps",
                mSiteMapManager);

        assertThat(loader.call()).hasSize(1);
    }

    @Test
    public void testSpecialCaseMultiWordPrefix_matchesSecondWordPrefixOfEntry() throws Exception {
        insertSpecialCase("Apps Notifications");

        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, "Not",
                mSiteMapManager);

        assertThat(loader.call()).hasSize(1);
    }

    @Test
    public void testSpecialCaseMultiWordPrefix_DoesNotMatchMatchesPrefixOfFirstEntry()
            throws Exception {
        insertSpecialCase("Apps Notifications");

        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, "pp",
                mSiteMapManager);

        assertThat(loader.call()).hasSize(0);
    }

    @Test
    public void testSpecialCaseMultiWordPrefix_DoesNotMatchMatchesPrefixOfSecondEntry()
            throws Exception {
        insertSpecialCase("Apps Notifications");

        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, "tion",
                mSiteMapManager);

        assertThat(loader.call()).hasSize(0);
    }

    @Test
    public void testSpecialCaseMultiWordPrefixWithSpecial_matchesPrefixOfEntry() throws
            Exception {
        insertSpecialCase("Apps & Notifications");

        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, "App",
                mSiteMapManager);

        assertThat(loader.call()).hasSize(1);
    }

    @Test
    public void testSpecialCaseMultiWordPrefixWithSpecial_matchesPrefixOfSecondEntry()
            throws Exception {
        insertSpecialCase("Apps & Notifications");

        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, "No",
                mSiteMapManager);

        assertThat(loader.call()).hasSize(1);
    }

    @Test
    public void testResultMatchedByMultipleQueries_duplicatesRemoved() throws Exception {
        String key = "durr";
        insertSameValueAllFieldsCase(key);

        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, key, null);

        assertThat(loader.call()).hasSize(1);
    }

    @Test
    public void testSpecialCaseTwoWords_multipleResults() throws Exception {
        final String caseOne = "Apple pear";
        final String caseTwo = "Banana apple";
        insertSpecialCase(caseOne);
        insertSpecialCase(caseTwo);
        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, "App", null);

        List<? extends SearchResult> results = loader.call();

        Set<String> actualTitles = new HashSet<>();
        for (SearchResult result : results) {
            actualTitles.add(result.title.toString());
        }
        assertThat(actualTitles).containsAllOf(caseOne, caseTwo);
    }

    @Test
    public void testGetRankingScoreByStableId_sortedDynamically() throws Exception {
        FutureTask<List<Pair<String, Float>>> task = mock(FutureTask.class);
        when(task.get(anyLong(), any(TimeUnit.class))).thenReturn(getDummyRankingScores());
        when(mFeatureFactory.searchFeatureProvider.getRankerTask(any(Context.class),
                anyString())).thenReturn(task);
        when(mFeatureFactory.searchFeatureProvider.isSmartSearchRankingEnabled(any())).thenReturn(
                true);

        insertSpecialCase(STATIC_TITLES[0], STABLE_IDS[0]);
        insertSpecialCase(STATIC_TITLES[1], STABLE_IDS[1]);
        insertSpecialCase(STATIC_TITLES[2], STABLE_IDS[2]);

        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, "Static",
                null);

        List<? extends SearchResult> results = loader.call();

        assertThat(results.get(0).title).isEqualTo(STATIC_TITLES[2]);
        assertThat(results.get(1).title).isEqualTo(STATIC_TITLES[0]);
        assertThat(results.get(2).title).isEqualTo(STATIC_TITLES[1]);
    }

    @Test
    public void testGetRankingScoreByStableId_scoresTimeout_sortedStatically() throws Exception {
        Callable<List<Pair<String, Float>>> callable = mock(Callable.class);
        when(callable.call()).thenThrow(new TimeoutException());
        FutureTask<List<Pair<String, Float>>> task = new FutureTask<>(callable);
        when(mFeatureFactory.searchFeatureProvider.isSmartSearchRankingEnabled(any())).thenReturn(
                true);
        when(mFeatureFactory.searchFeatureProvider.getRankerTask(any(Context.class),
                anyString())).thenReturn(task);
        insertSpecialCase("title", STABLE_IDS[0]);

        StaticSearchResultCallable loader = new StaticSearchResultCallable(mContext, "title", null);

        List<? extends SearchResult> results = loader.call();
        assertThat(results.get(0).title).isEqualTo("title");
        assertThat(results.get(1).title).isEqualTo("alpha_title");
        assertThat(results.get(2).title).isEqualTo("bravo_title");
    }

    private void insertSpecialCase(String specialCase) {
        insertSpecialCase(specialCase, specialCase.hashCode());
    }

    private void insertSpecialCase(String specialCase, int docId) {
        String normalized = IndexData.normalizeHyphen(specialCase);
        normalized = IndexData.normalizeString(normalized);
        final ResultPayload payload = new ResultPayload(new Intent());

        ContentValues values = new ContentValues();
        values.put(IndexDatabaseHelper.IndexColumns.DOCID, docId);
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
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEY_REF, normalized.hashCode());
        values.put(IndexDatabaseHelper.IndexColumns.USER_ID, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD_TYPE, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD, ResultPayloadUtils.marshall(payload));

        mDb.replaceOrThrow(IndexDatabaseHelper.Tables.TABLE_PREFS_INDEX, null, values);
    }

    private void setUpDb() {
        final byte[] payload = ResultPayloadUtils.marshall(new ResultPayload(new Intent()));

        ContentValues values = new ContentValues();
        values.put(IndexDatabaseHelper.IndexColumns.DOCID, 1);
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
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEY_REF, "gesture_double_tap_power_0");
        values.put(IndexDatabaseHelper.IndexColumns.USER_ID, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD_TYPE, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD, payload);

        mDb.replaceOrThrow(IndexDatabaseHelper.Tables.TABLE_PREFS_INDEX, null, values);

        values = new ContentValues();
        values.put(IndexDatabaseHelper.IndexColumns.DOCID, 2);
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
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEY_REF, "gesture_double_tap_power_1");
        values.put(IndexDatabaseHelper.IndexColumns.USER_ID, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD_TYPE, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD, payload);
        mDb.replaceOrThrow(IndexDatabaseHelper.Tables.TABLE_PREFS_INDEX, null, values);

        values = new ContentValues();
        values.put(IndexDatabaseHelper.IndexColumns.DOCID, 3);
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
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEY_REF, "gesture_double_tap_power_2");
        values.put(IndexDatabaseHelper.IndexColumns.USER_ID, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD_TYPE, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD, payload);

        mDb.replaceOrThrow(IndexDatabaseHelper.Tables.TABLE_PREFS_INDEX, null, values);
    }

    private void insertSameValueAllFieldsCase(String key) {
        final ResultPayload payload = new ResultPayload(new Intent());

        ContentValues values = new ContentValues();
        values.put(IndexDatabaseHelper.IndexColumns.DOCID, key.hashCode());
        values.put(IndexDatabaseHelper.IndexColumns.LOCALE, "en-us");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_RANK, 1);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_TITLE, key);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_TITLE_NORMALIZED, key);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON, key);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON_NORMALIZED, key);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF, key);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF_NORMALIZED, key);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_ENTRIES, key);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEYWORDS, key);
        values.put(IndexDatabaseHelper.IndexColumns.CLASS_NAME, key);
        values.put(IndexDatabaseHelper.IndexColumns.SCREEN_TITLE, "Moves");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_ACTION, key);
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_PACKAGE, "");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_CLASS, key);
        values.put(IndexDatabaseHelper.IndexColumns.ICON, "");
        values.put(IndexDatabaseHelper.IndexColumns.ENABLED, true);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEY_REF, key.hashCode());
        values.put(IndexDatabaseHelper.IndexColumns.USER_ID, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD_TYPE, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD, ResultPayloadUtils.marshall(payload));

        mDb.replaceOrThrow(IndexDatabaseHelper.Tables.TABLE_PREFS_INDEX, null, values);
    }

    private List<? extends SearchResult> getDummyDbResults() {
        List<SearchResult> results = new ArrayList<>();
        ResultPayload payload = new ResultPayload(new Intent());
        SearchResult.Builder builder = new SearchResult.Builder();
        builder.setPayload(payload)
                .setTitle(STATIC_TITLES[0])
                .setStableId(STABLE_IDS[0]);
        results.add(builder.build());

        builder.setTitle(STATIC_TITLES[1])
                .setStableId(STABLE_IDS[1]);
        results.add(builder.build());

        builder.setTitle(STATIC_TITLES[2])
                .setStableId(STABLE_IDS[2]);
        results.add(builder.build());

        return results;
    }

    private List<Pair<String, Float>> getDummyRankingScores() {
        List<? extends SearchResult> results = getDummyDbResults();
        List<Pair<String, Float>> scores = new ArrayList<>();
        scores.add(new Pair<>(Long.toString(results.get(2).stableId), 0.9f)); // static_three
        scores.add(new Pair<>(Long.toString(results.get(0).stableId), 0.8f)); // static_one
        scores.add(new Pair<>(Long.toString(results.get(1).stableId), 0.2f)); // static_two
        return scores;
    }
}