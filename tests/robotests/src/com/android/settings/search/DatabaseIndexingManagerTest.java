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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.provider.SearchIndexableData;
import android.util.ArrayMap;

import com.android.settings.search.indexing.PreIndexData;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowRunnableAsyncTask;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = ShadowRunnableAsyncTask.class)
public class DatabaseIndexingManagerTest {

    private final String localeStr = "en_US";

    private final int rank = 8;
    private final String title = "title\u2011title";
    private final String updatedTitle = "title-title";
    private final String normalizedTitle = "titletitle";
    private final String summaryOn = "summary\u2011on";
    private final String updatedSummaryOn = "summary-on";
    private final String normalizedSummaryOn = "summaryon";
    private final String summaryOff = "summary\u2011off";
    private final String entries = "entries";
    private final String keywords = "keywords, keywordss, keywordsss";
    private final String spaceDelimittedKeywords = "keywords keywordss keywordsss";
    private final String screenTitle = "screen title";
    private final String className = "class name";
    private final int iconResId = 0xff;
    private final String action = "action";
    private final String targetPackage = "target package";
    private final String targetClass = "target class";
    private final String packageName = "package name";
    private final String key = "key";
    private final int userId = -1;
    private final boolean enabled = true;

    private final String TITLE_ONE = "title one";
    private final String TITLE_TWO = "title two";
    private final String KEY_ONE = "key one";
    private final String KEY_TWO = "key two";

    private Context mContext;

    private DatabaseIndexingManager mManager;
    private SQLiteDatabase mDb;

    private final List<ResolveInfo> FAKE_PROVIDER_LIST = new ArrayList<>();

    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mManager = spy(new DatabaseIndexingManager(mContext));
        mDb = IndexDatabaseHelper.getInstance(mContext).getWritableDatabase();

        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(FAKE_PROVIDER_LIST).when(mPackageManager)
                .queryIntentContentProviders(any(Intent.class), anyInt());
        FakeFeatureFactory.setupForTest();
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void testDatabaseSchema() {
        Cursor dbCursor = mDb.query("prefs_index", null, null, null, null, null, null);
        List<String> columnNames = new ArrayList<>(Arrays.asList(dbCursor.getColumnNames()));
        // Note that docid is not included.
        List<String> expColumnNames = Arrays.asList(
                "locale",
                "data_rank",
                "data_title",
                "data_title_normalized",
                "data_summary_on",
                "data_summary_on_normalized",
                "data_summary_off",
                "data_summary_off_normalized",
                "data_entries",
                "data_keywords",
                "class_name",
                "screen_title",
                "intent_action",
                "intent_target_package",
                "intent_target_class",
                "icon",
                "enabled",
                "data_key_reference",
                "user_id",
                "payload_type",
                "payload"
        );
        // Prevent database schema regressions
        assertThat(columnNames).containsAllIn(expColumnNames);
    }

    // Test new public indexing flow

    @Test
    public void testPerformIndexing_fullIndex_getsDataFromProviders() {
        SearchIndexableRaw rawData = getFakeRaw();
        PreIndexData data = getPreIndexData(rawData);
        doReturn(data).when(mManager).getIndexDataFromProviders(anyList(), anyBoolean());
        doReturn(true).when(mManager)
            .isFullIndex(any(Context.class), anyString(), anyString(), anyString());

        mManager.performIndexing();

        verify(mManager).updateDatabase(data, true /* isFullIndex */);
    }

    @Test
    public void testPerformIndexing_fullIndex_databaseDropped() {
        // Initialize the Manager and force rebuild
        DatabaseIndexingManager manager =
                spy(new DatabaseIndexingManager(mContext));
        doReturn(false).when(mManager)
            .isFullIndex(any(Context.class), anyString(), anyString(), anyString());

        // Insert data point which will be dropped
        insertSpecialCase("Ceci n'est pas un pipe", true, "oui oui mon ami");

        manager.performIndexing();

        // Assert that the Old Title is no longer in the database, since it was dropped
        final Cursor oldCursor = mDb.rawQuery("SELECT * FROM prefs_index", null);

        assertThat(oldCursor.getCount()).isEqualTo(0);
    }

    @Test
    public void testPerformIndexing_isfullIndex() {
        SearchIndexableRaw rawData = getFakeRaw();
        PreIndexData data = getPreIndexData(rawData);
        doReturn(data).when(mManager).getIndexDataFromProviders(anyList(), anyBoolean());
        doReturn(true).when(mManager)
            .isFullIndex(any(Context.class), anyString(), anyString(), anyString());

        mManager.performIndexing();

        verify(mManager).updateDatabase(data, true /* isFullIndex */);
    }

    @Test
    public void testPerformIndexing_onOta_buildNumberIsCached() {
        mManager.performIndexing();

        assertThat(IndexDatabaseHelper.isBuildIndexed(mContext, Build.FINGERPRINT)).isTrue();
    }

    @Test
    public void testLocaleUpdated_afterIndexing_localeNotAdded() {
        PreIndexData emptydata = new PreIndexData();
        mManager.updateDatabase(emptydata, true /* isFullIndex */);

        assertThat(IndexDatabaseHelper.isLocaleAlreadyIndexed(mContext, localeStr)).isFalse();
    }

    @Test
    public void testLocaleUpdated_afterFullIndexing_localeAdded() {
        mManager.performIndexing();

        assertThat(IndexDatabaseHelper.isLocaleAlreadyIndexed(mContext, localeStr)).isTrue();
    }

    @Test
    public void testUpdateDatabase_newEligibleData_addedToDatabase() {
        // Test that addDataToDatabase is called when dataToUpdate is non-empty
        PreIndexData indexData = new PreIndexData();
        indexData.dataToUpdate.add(getFakeRaw());
        mManager.updateDatabase(indexData, true /* isFullIndex */);

        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        cursor.moveToPosition(0);

        // Locale
        assertThat(cursor.getString(0)).isEqualTo(localeStr);
        // Data Title
        assertThat(cursor.getString(2)).isEqualTo(updatedTitle);
        // Normalized Title
        assertThat(cursor.getString(3)).isEqualTo(normalizedTitle);
        // Summary On
        assertThat(cursor.getString(4)).isEqualTo(updatedSummaryOn);
        // Summary On Normalized
        assertThat(cursor.getString(5)).isEqualTo(normalizedSummaryOn);
        // Entries
        assertThat(cursor.getString(8)).isEqualTo(entries);
        // Keywords
        assertThat(cursor.getString(9)).isEqualTo(spaceDelimittedKeywords);
        // Screen Title
        assertThat(cursor.getString(10)).isEqualTo(screenTitle);
        // Class Name
        assertThat(cursor.getString(11)).isEqualTo(className);
        // Icon
        assertThat(cursor.getInt(12)).isEqualTo(iconResId);
        // Intent Action
        assertThat(cursor.getString(13)).isEqualTo(action);
        // Target Package
        assertThat(cursor.getString(14)).isEqualTo(targetPackage);
        // Target Class
        assertThat(cursor.getString(15)).isEqualTo(targetClass);
        // Enabled
        assertThat(cursor.getInt(16) == 1).isEqualTo(enabled);
        // Data ref key
        assertThat(cursor.getString(17)).isNotNull();
        // User Id
        assertThat(cursor.getInt(18)).isEqualTo(userId);
        // Payload Type - default is 0
        assertThat(cursor.getInt(19)).isEqualTo(0);
        // Payload
        byte[] payload = cursor.getBlob(20);
        ResultPayload unmarshalledPayload = ResultPayloadUtils.unmarshall(payload,
                ResultPayload.CREATOR);
        assertThat(unmarshalledPayload).isInstanceOf(ResultPayload.class);
    }

    @Test
    public void testUpdateDataInDatabase_enabledResultsAreNonIndexable_becomeDisabled() {
        // Both results are enabled, and then TITLE_ONE gets disabled.
        final boolean enabled = true;
        insertSpecialCase(TITLE_ONE, enabled, KEY_ONE);
        insertSpecialCase(TITLE_TWO, enabled, KEY_TWO);
        Map<String, Set<String>> niks = new ArrayMap<>();
        Set<String> keys = new HashSet<>();
        keys.add(KEY_ONE);
        niks.put(targetPackage, keys);

        mManager.updateDataInDatabase(mDb, niks);

        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index WHERE enabled = 0", null);
        cursor.moveToPosition(0);

        assertThat(cursor.getString(2)).isEqualTo(TITLE_ONE);
    }

    @Test
    public void testUpdateDataInDatabase_disabledResultsAreIndexable_becomeEnabled() {
        // Both results are initially disabled, and then TITLE_TWO gets enabled.
        final boolean enabled = false;
        insertSpecialCase(TITLE_ONE, enabled, KEY_ONE);
        insertSpecialCase(TITLE_TWO, enabled, KEY_TWO);
        Map<String, Set<String>> niks = new ArrayMap<>();
        Set<String> keys = new HashSet<>();
        keys.add(KEY_ONE);
        niks.put(targetPackage, keys);

        mManager.updateDataInDatabase(mDb, niks);

        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index WHERE enabled = 1", null);
        cursor.moveToPosition(0);

        assertThat(cursor.getString(2)).isEqualTo(TITLE_TWO);
    }

    @Test
    public void testEmptyNonIndexableKeys_emptyDataKeyResources_addedToDatabase() {
        insertSpecialCase(TITLE_ONE, true /* enabled */, null /* dataReferenceKey */);
        PreIndexData emptydata = new PreIndexData();
        mManager.updateDatabase(emptydata, false /* needsReindexing */);

        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index WHERE enabled = 1", null);
        cursor.moveToPosition(0);
        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getString(2)).isEqualTo(TITLE_ONE);
    }

    // Util functions

    private SearchIndexableRaw getFakeRaw() {
        return getFakeRaw(localeStr);
    }

    private SearchIndexableRaw getFakeRaw(String localeStr) {
        SearchIndexableRaw data = new SearchIndexableRaw(mContext);
        data.locale = new Locale(localeStr);
        data.rank = rank;
        data.title = title;
        data.summaryOn = summaryOn;
        data.summaryOff = summaryOff;
        data.entries = entries;
        data.keywords = keywords;
        data.screenTitle = screenTitle;
        data.className = className;
        data.packageName = packageName;
        data.iconResId = iconResId;
        data.intentAction = action;
        data.intentTargetPackage = targetPackage;
        data.intentTargetClass = targetClass;
        data.key = key;
        data.userId = userId;
        data.enabled = enabled;
        return data;
    }

    private void insertSpecialCase(String specialCase, boolean enabled, String key) {
        ContentValues values = new ContentValues();
        values.put(IndexDatabaseHelper.IndexColumns.DOCID, specialCase.hashCode());
        values.put(IndexDatabaseHelper.IndexColumns.LOCALE, localeStr);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_RANK, 1);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_TITLE, specialCase);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_TITLE_NORMALIZED, "");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON, "");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_ON_NORMALIZED, "");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF, "");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_SUMMARY_OFF_NORMALIZED, "");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_ENTRIES, "");
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEYWORDS, "");
        values.put(IndexDatabaseHelper.IndexColumns.CLASS_NAME, "");
        values.put(IndexDatabaseHelper.IndexColumns.SCREEN_TITLE, "Moves");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_ACTION, "");
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_PACKAGE, targetPackage);
        values.put(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_CLASS, "");
        values.put(IndexDatabaseHelper.IndexColumns.ICON, "");
        values.put(IndexDatabaseHelper.IndexColumns.ENABLED, enabled);
        values.put(IndexDatabaseHelper.IndexColumns.DATA_KEY_REF, key);
        values.put(IndexDatabaseHelper.IndexColumns.USER_ID, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD_TYPE, 0);
        values.put(IndexDatabaseHelper.IndexColumns.PAYLOAD, (String) null);

        mDb.replaceOrThrow(IndexDatabaseHelper.Tables.TABLE_PREFS_INDEX, null, values);
    }

    private PreIndexData getPreIndexData(SearchIndexableData fakeData) {
        PreIndexData data = new PreIndexData();
        data.dataToUpdate.add(fakeData);
        return data;
    }
}
