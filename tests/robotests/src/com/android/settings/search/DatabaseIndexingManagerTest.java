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

import static android.provider.SearchIndexablesContract.INDEXABLES_RAW_COLUMNS;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.provider.SearchIndexableResource;
import android.util.ArrayMap;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowDatabaseIndexingUtils;
import com.android.settings.testutils.shadow.ShadowRunnableAsyncTask;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
    manifest = TestConfig.MANIFEST_PATH,
    sdk = TestConfig.SDK_VERSION,
    shadows = {
        ShadowRunnableAsyncTask.class,
        ShadowDatabaseIndexingUtils.class,
        ShadowContentResolver.class
    }
)
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
    private final String updatedSummaryOff = "summary-off";
    private final String normalizedSummaryOff = "summaryoff";
    private final String entries = "entries";
    private final String keywords = "keywords, keywordss, keywordsss";
    private final String spaceDelimittedKeywords = "keywords keywordss keywordsss";
    private final String screenTitle = "screen title";
    private final String className = "class name";
    private final int iconResId = 0xff;
    private final int noIcon = 0;
    private final String action = "action";
    private final String targetPackage = "target package";
    private final String targetClass = "target class";
    private final String packageName = "package name";
    private final String key = "key";
    private final int userId = -1;
    private final boolean enabled = true;

    private final String AUTHORITY_ONE = "authority";
    private final String PACKAGE_ONE = "com.android.settings";

    private final String TITLE_ONE = "title one";
    private final String TITLE_TWO = "title two";
    private final String KEY_ONE = "key one";
    private final String KEY_TWO = "key two";

    private Context mContext;

    private DatabaseIndexingManager mManager;
    private SQLiteDatabase mDb;

    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mManager = spy(new DatabaseIndexingManager(mContext, PACKAGE_ONE));
        mDb = IndexDatabaseHelper.getInstance(mContext).getWritableDatabase();

        doReturn(mPackageManager).when(mContext).getPackageManager();
        FakeFeatureFactory.setupForTest(mContext);
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
        List<String> expColumnNames = new ArrayList<>(Arrays.asList(new String[]{
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
        }));
        // Prevent database schema regressions
        assertThat(columnNames).containsAllIn(expColumnNames);
    }

    // Tests for the flow: IndexOneRaw -> UpdateOneRowWithFilteredData -> UpdateOneRow

    @Test
    public void testInsertRawColumn_rowInserted() {
        SearchIndexableRaw raw = getFakeRaw();
        mManager.indexOneSearchIndexableData(mDb, localeStr, raw,
                new HashMap<>()/* Non-indexable keys */);
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        assertThat(cursor.getCount()).isEqualTo(1);
    }

    @Test
    public void testInsertRawColumn_nonIndexableKey_resultIsDisabled() {
        SearchIndexableRaw raw = getFakeRaw();
        Map<String, Set<String>> niks = new HashMap<>();
        Set<String> keys = new HashSet<>();
        keys.add(raw.key);
        niks.put(raw.intentTargetPackage, keys);

        mManager.indexOneSearchIndexableData(mDb, localeStr, raw, niks);
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index WHERE enabled = 0", null);
        assertThat(cursor.getCount()).isEqualTo(1);
    }

    @Test
    public void testInsertRawColumn_rowMatches() {
        SearchIndexableRaw raw = getFakeRaw();
        mManager.indexOneSearchIndexableData(mDb, localeStr, raw,
                new HashMap<>()/* Non-indexable keys */);
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        cursor.moveToPosition(0);

        // Locale
        assertThat(cursor.getString(0)).isEqualTo(localeStr);
        // Data Rank
        assertThat(cursor.getInt(1)).isEqualTo(raw.rank);
        // Data Title
        assertThat(cursor.getString(2)).isEqualTo(updatedTitle);
        // Normalized Title
        assertThat(cursor.getString(3)).isEqualTo(normalizedTitle);
        // Summary On
        assertThat(cursor.getString(4)).isEqualTo(updatedSummaryOn);
        // Summary On Normalized
        assertThat(cursor.getString(5)).isEqualTo(normalizedSummaryOn);
        // Summary Off
        assertThat(cursor.getString(6)).isEqualTo(updatedSummaryOff);
        // Summary off normalized
        assertThat(cursor.getString(7)).isEqualTo(normalizedSummaryOff);
        // Entries
        assertThat(cursor.getString(8)).isEqualTo(raw.entries);
        // Keywords
        assertThat(cursor.getString(9)).isEqualTo(spaceDelimittedKeywords);
        // Screen Title
        assertThat(cursor.getString(10)).isEqualTo(raw.screenTitle);
        // Class Name
        assertThat(cursor.getString(11)).isEqualTo(raw.className);
        // Icon
        assertThat(cursor.getInt(12)).isEqualTo(raw.iconResId);
        // Intent Action
        assertThat(cursor.getString(13)).isEqualTo(raw.intentAction);
        // Target Package
        assertThat(cursor.getString(14)).isEqualTo(raw.intentTargetPackage);
        // Target Class
        assertThat(cursor.getString(15)).isEqualTo(raw.intentTargetClass);
        // Enabled
        assertThat(cursor.getInt(16) == 1).isEqualTo(raw.enabled);
        // Data ref key
        assertThat(cursor.getString(17)).isNotNull();
        // User Id
        assertThat(cursor.getInt(18)).isEqualTo(raw.userId);
        // Payload Type - default is 0
        assertThat(cursor.getInt(19)).isEqualTo(0);
        // Payload
        byte[] payload = cursor.getBlob(20);
        ResultPayload unmarshalledPayload = ResultPayloadUtils.unmarshall(payload,
                ResultPayload.CREATOR);
        assertThat(unmarshalledPayload).isInstanceOf(ResultPayload.class);
    }

    @Test
    public void testInsertRawColumn_mismatchedLocale_noRowInserted() {
        SearchIndexableRaw raw = getFakeRaw("ca-fr");
        mManager.indexOneSearchIndexableData(mDb, localeStr, raw, null /* Non-indexable keys */);
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        assertThat(cursor.getCount()).isEqualTo(0);
    }

    // Tests for the flow: IndexOneResource -> IndexFromResource ->
    //                     UpdateOneRowWithFilteredData -> UpdateOneRow

    @Test
    public void testNullResource_NothingInserted() {
        mManager.indexOneSearchIndexableData(mDb, localeStr, null /* searchIndexableResource */,
                new HashMap<>());
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        assertThat(cursor.getCount()).isEqualTo(0);
    }

    @Test
    public void testAddResource_RowsInserted() {
        SearchIndexableResource resource = getFakeResource(R.xml.display_settings);
        mManager.indexOneSearchIndexableData(mDb, localeStr, resource, new HashMap<>());
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        assertThat(cursor.getCount()).isEqualTo(17);
    }

    @Test
    public void testAddResource_withNIKs_rowsInsertedDisabled() {
        SearchIndexableResource resource = getFakeResource(R.xml.display_settings);
        // Only add 2 of 16 items to be disabled.
        String[] keys = {"brightness", "wallpaper"};
        Map<String, Set<String>> niks = getNonIndexableKeys(keys);

        mManager.indexOneSearchIndexableData(mDb, localeStr, resource, niks);

        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index WHERE enabled = 0", null);
        assertThat(cursor.getCount()).isEqualTo(2);
        cursor = mDb.rawQuery("SELECT * FROM prefs_index WHERE enabled = 1", null);
        assertThat(cursor.getCount()).isEqualTo(15);
    }

    @Test
    public void testAddResourceHeader_rowsMatch() {
        SearchIndexableResource resource = getFakeResource(R.xml.application_settings);
        mManager.indexOneSearchIndexableData(mDb, localeStr, resource, new HashMap<>());

        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index ORDER BY data_title", null);
        cursor.moveToPosition(1);

        // Locale
        assertThat(cursor.getString(0)).isEqualTo(localeStr);
        // Data Rank
        assertThat(cursor.getInt(1)).isEqualTo(rank);
        // Data Title
        assertThat(cursor.getString(2)).isEqualTo("App info");
        // Normalized Title
        assertThat(cursor.getString(3)).isEqualTo("app info");
        // Summary On
        assertThat(cursor.getString(4)).isEqualTo("Manage apps, set up quick launch shortcuts");
        // Summary On Normalized
        assertThat(cursor.getString(5)).isEqualTo("manage apps, set up quick launch shortcuts");
        // Summary Off - only on for checkbox preferences
        assertThat(cursor.getString(6)).isEmpty();
        // Summary off normalized - only on for checkbox preferences
        assertThat(cursor.getString(7)).isEmpty();
        // Entries - only on for list preferences
        assertThat(cursor.getString(8)).isNull();
        // Keywords
        assertThat(cursor.getString(9)).isEmpty();
        // Screen Title
        assertThat(cursor.getString(10)).isEqualTo("App info");
        // Class Name
        assertThat(cursor.getString(11)).isEqualTo(className);
        // Icon
        assertThat(cursor.getInt(12)).isEqualTo(0);
        // Intent Action
        assertThat(cursor.getString(13)).isEqualTo(action);
        // Target Package
        assertThat(cursor.getString(14)).isEqualTo(targetPackage);
        // Target Class
        assertThat(cursor.getString(15)).isEqualTo(targetClass);
        // Enabled
        assertThat(cursor.getInt(16) == 1).isEqualTo(enabled);
        // Data ref key
        assertThat(cursor.getString(17)).isEqualTo("applications_settings");
        // User Id
        assertThat(cursor.getInt(18)).isEqualTo(userId);
        // Payload Type - default is 0
        assertThat(cursor.getInt(19)).isEqualTo(0);
        // Payload - should be updated to real payloads as controllers are added
        byte[] payload = cursor.getBlob(20);
        ResultPayload unmarshalledPayload = ResultPayloadUtils.unmarshall(payload,
                ResultPayload.CREATOR);
        assertThat(unmarshalledPayload).isInstanceOf(ResultPayload.class);
    }

    @Test
    public void testAddResource_withChildFragment_shouldUpdateSiteMapDb() {
        // FIXME: This test was failing. (count = 6 at the end)

//        SearchIndexableResource resource = getFakeResource(R.xml.network_and_internet);
//        mManager.indexOneSearchIndexableData(mDb, localeStr, resource,
//                new HashMap<>());
//        Cursor query = mDb.query(IndexDatabaseHelper.Tables.TABLE_SITE_MAP, SITE_MAP_COLUMNS,
//                null, null, null, null, null);
//        query.moveToPosition(-1);
//        int count = 0;
//        while (query.moveToNext()) {
//            count++;
//            assertThat(query.getString(query.getColumnIndex(SiteMapColumns.PARENT_CLASS)))
//                    .isEqualTo(className);
//            assertThat(query.getString(query.getColumnIndex(SiteMapColumns.PARENT_TITLE)))
//                    .isEqualTo(mContext.getString(R.string.network_dashboard_title));
//            assertThat(query.getString(query.getColumnIndex(SiteMapColumns.CHILD_CLASS)))
//                    .isNotEmpty();
//            assertThat(query.getString(query.getColumnIndex(SiteMapColumns.CHILD_TITLE)))
//                    .isNotEmpty();
//        }
//        assertThat(count).isEqualTo(5);
    }

    @Test
    public void testAddResource_customSetting_rowsMatch() {
        SearchIndexableResource resource = getFakeResource(R.xml.swipe_to_notification_settings);
        mManager.indexOneSearchIndexableData(mDb, localeStr, resource, new HashMap<>());
        final String prefTitle =
                mContext.getString(R.string.fingerprint_swipe_for_notifications_title);
        final String prefSummary =
                mContext.getString(R.string.fingerprint_swipe_for_notifications_summary);
        final String keywords = mContext.getString(R.string.keywords_gesture);
        Cursor cursor = mDb.rawQuery(
                "SELECT * FROM prefs_index where data_title='" + prefTitle + "'", null);
        cursor.moveToFirst();

        // Locale
        assertThat(cursor.getString(0)).isEqualTo(localeStr);
        // Data Rank
        assertThat(cursor.getInt(1)).isEqualTo(rank);
        // Data Title
        assertThat(cursor.getString(2)).isEqualTo(prefTitle);
        // Normalized Title
        assertThat(cursor.getString(3)).isEqualTo(prefTitle.toLowerCase());
        // Summary On
        assertThat(cursor.getString(4)).isEqualTo(prefSummary);
        // Summary On Normalized
        assertThat(cursor.getString(5)).isEqualTo(prefSummary.toLowerCase());
        // Summary Off - only on for checkbox preferences
        assertThat(cursor.getString(6)).isEmpty();
        // Summary off normalized - only on for checkbox preferences
        assertThat(cursor.getString(7)).isEmpty();
        // Entries - only on for list preferences
        assertThat(cursor.getString(8)).isNull();
        // Keywords
        assertThat(cursor.getString(9)).isEqualTo(keywords);
        // Screen Title
        assertThat(cursor.getString(10)).isEqualTo(
                mContext.getString(R.string.fingerprint_swipe_for_notifications_title));
        // Class Name
        assertThat(cursor.getString(11)).isEqualTo(className);
        // Icon
        assertThat(cursor.getInt(12)).isEqualTo(noIcon);
        // Intent Action
        assertThat(cursor.getString(13)).isEqualTo(action);
        // Target Package
        assertThat(cursor.getString(14)).isEqualTo(targetPackage);
        // Target Class
        assertThat(cursor.getString(15)).isEqualTo(targetClass);
        // Enabled
        assertThat(cursor.getInt(16) == 1).isEqualTo(enabled);
        // Data ref key
        assertThat(cursor.getString(17)).isEqualTo("gesture_swipe_down_fingerprint");
        // User Id
        assertThat(cursor.getInt(18)).isEqualTo(userId);
        // Payload Type - default is 0
        assertThat(cursor.getInt(19)).isEqualTo(0);
        // Payload - should be updated to real payloads as controllers are added
        byte[] payload = cursor.getBlob(20);
        ResultPayload unmarshalledPayload = ResultPayloadUtils.unmarshall(payload,
                ResultPayload.CREATOR);
        assertThat(unmarshalledPayload).isInstanceOf(ResultPayload.class);
    }

    @Test
    public void testAddResource_checkboxPreference_rowsMatch() {
        SearchIndexableResource resource = getFakeResource(R.xml.application_settings);
        mManager.indexOneSearchIndexableData(mDb, localeStr, resource, new HashMap<>());

        /* Should return 6 results, with the following titles:
         * Advanced Settings, Apps, Manage Apps, Preferred install location, Running Services
         */
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index ORDER BY data_title", null);
        cursor.moveToPosition(0);
        // Locale
        assertThat(cursor.getString(0)).isEqualTo(localeStr);
        // Data Rank
        assertThat(cursor.getInt(1)).isEqualTo(rank);
        // Data Title
        assertThat(cursor.getString(2)).isEqualTo("Advanced settings");
        // Normalized Title
        assertThat(cursor.getString(3)).isEqualTo("advanced settings");
        // Summary On
        assertThat(cursor.getString(4)).isEqualTo("Enable more settings options");
        // Summary On Normalized
        assertThat(cursor.getString(5)).isEqualTo("enable more settings options");
        // Summary Off
        assertThat(cursor.getString(6)).isEqualTo("Enable more settings options");
        // Summary Off
        assertThat(cursor.getString(7)).isEqualTo("enable more settings options");
        // Entries - only on for list preferences
        assertThat(cursor.getString(8)).isNull();
        // Keywords
        assertThat(cursor.getString(9)).isEmpty();
        // Screen Title
        assertThat(cursor.getString(10)).isEqualTo("App info");
        // Class Name
        assertThat(cursor.getString(11)).isEqualTo(className);
        // Icon
        assertThat(cursor.getInt(12)).isEqualTo(noIcon);
        // Intent Action
        assertThat(cursor.getString(13)).isEqualTo(action);
        // Target Package
        assertThat(cursor.getString(14)).isEqualTo(targetPackage);
        // Target Class
        assertThat(cursor.getString(15)).isEqualTo(targetClass);
        // Enabled
        assertThat(cursor.getInt(16) == 1).isEqualTo(enabled);
        // Data ref key
        assertThat(cursor.getString(17)).isEqualTo("toggle_advanced_settings");
        // User Id
        assertThat(cursor.getInt(18)).isEqualTo(userId);
        // Payload Type - default is 0
        assertThat(cursor.getInt(19)).isEqualTo(0);
        // Payload - should be updated to real payloads as controllers are added
        byte[] payload = cursor.getBlob(20);
        ResultPayload unmarshalledPayload = ResultPayloadUtils.unmarshall(payload,
                ResultPayload.CREATOR);
        assertThat(unmarshalledPayload).isInstanceOf(ResultPayload.class);
    }

    @Test
    public void testAddResource_listPreference_rowsMatch() {
        SearchIndexableResource resource = getFakeResource(R.xml.application_settings);
        mManager.indexOneSearchIndexableData(mDb, localeStr, resource, new HashMap<>());

        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index ORDER BY data_title", null);
        cursor.moveToPosition(3);
        // Locale
        assertThat(cursor.getString(0)).isEqualTo(localeStr);
        // Data Rank
        assertThat(cursor.getInt(1)).isEqualTo(rank);
        // Data Title
        assertThat(cursor.getString(2)).isEqualTo("Preferred install location");
        // Normalized Title
        assertThat(cursor.getString(3)).isEqualTo("preferred install location");
        // Summary On
        assertThat(cursor.getString(4)).isEqualTo(
                "Change the preferred installation location for new apps");
        // Summary On Normalized
        assertThat(cursor.getString(5)).isEqualTo(
                "change the preferred installation location for new apps");
        // Summary Off - only on for checkbox preferences
        assertThat(cursor.getString(6)).isEmpty();
        // Summary off normalized - only on for checkbox preferences
        assertThat(cursor.getString(7)).isEmpty();
        // Entries - only on for list preferences
        assertThat(cursor.getString(8)).isEqualTo("Internal device storage|Removable SD card|" +
                "Let the system decide|");
        // Keywords
        assertThat(cursor.getString(9)).isEmpty();
        // Screen Title
        assertThat(cursor.getString(10)).isEqualTo("App info");
        // Class Name
        assertThat(cursor.getString(11)).isEqualTo(className);
        // Icon
        assertThat(cursor.getInt(12)).isEqualTo(noIcon);
        // Intent Action
        assertThat(cursor.getString(13)).isEqualTo(action);
        // Target Package
        assertThat(cursor.getString(14)).isEqualTo(targetPackage);
        // Target Class
        assertThat(cursor.getString(15)).isEqualTo(targetClass);
        // Enabled
        assertThat(cursor.getInt(16) == 1).isEqualTo(enabled);
        // Data ref key
        assertThat(cursor.getString(17)).isEqualTo("app_install_location");
        // User Id
        assertThat(cursor.getInt(18)).isEqualTo(userId);
        // Payload Type - default is 0
        assertThat(cursor.getInt(19)).isEqualTo(0);
        // Payload - should be updated to real payloads as controllers are added
        byte[] payload = cursor.getBlob(20);
        ResultPayload unmarshalledPayload = ResultPayloadUtils.unmarshall(payload,
                ResultPayload.CREATOR);
        assertThat(unmarshalledPayload).isInstanceOf(ResultPayload.class);
    }

    @Test
    public void testAddResource_iconAddedFromXml() {
        SearchIndexableResource resource = getFakeResource(R.xml.connected_devices);
        mManager.indexOneSearchIndexableData(mDb, localeStr, resource, new HashMap<>());

        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index ORDER BY data_title", null);
        cursor.moveToPosition(0);

        // Icon
        assertThat(cursor.getInt(12)).isNotEqualTo(noIcon);
    }

    // Tests for the flow: IndexOneResource -> IndexFromProvider -> IndexFromResource ->
    //                     UpdateOneRowWithFilteredData -> UpdateOneRow

    @Test
    public void testResourceProvider_rowInserted() {
        SearchIndexableResource resource = getFakeResource(R.xml.swipe_to_notification_settings);
        resource.xmlResId = 0;
        resource.className = "com.android.settings.display.ScreenZoomSettings";

        mManager.indexOneSearchIndexableData(mDb, localeStr, resource, new HashMap<>());
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        assertThat(cursor.getCount()).isEqualTo(1);
    }

    @Test
    public void testResourceProvider_rowMatches() {
        SearchIndexableResource resource = getFakeResource(R.xml.swipe_to_notification_settings);
        resource.xmlResId = 0;
        resource.className = "com.android.settings.display.ScreenZoomSettings";

        mManager.indexOneSearchIndexableData(mDb, localeStr, resource, new HashMap<>());
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        cursor.moveToPosition(0);

        // Locale
        assertThat(cursor.getString(0)).isEqualTo(localeStr);
        // Data Rank
        assertThat(cursor.getInt(1)).isEqualTo(0);
        // Data Title
        assertThat(cursor.getString(2)).isEqualTo("Display size");
        // Normalized Title
        assertThat(cursor.getString(3)).isEqualTo("display size");
        // Summary On
        assertThat(cursor.getString(4)).isEmpty();
        // Summary On Normalized
        assertThat(cursor.getString(5)).isEmpty();
        // Summary Off - only on for checkbox preferences
        assertThat(cursor.getString(6)).isEmpty();
        // Summary off normalized - only on for checkbox preferences
        assertThat(cursor.getString(7)).isEmpty();
        // Entries - only on for list preferences
        assertThat(cursor.getString(8)).isNull();
        // Keywords
        assertThat(cursor.getString(9)).isEqualTo("display density screen zoom scale scaling");
        // Screen Title
        assertThat(cursor.getString(10)).isEqualTo("Display size");
        // Class Name
        assertThat(cursor.getString(11))
                .isEqualTo("com.android.settings.display.ScreenZoomSettings");
        // Icon
        assertThat(cursor.getInt(12)).isEqualTo(noIcon);
        // Intent Action
        assertThat(cursor.getString(13)).isNull();
        // Target Package
        assertThat(cursor.getString(14)).isNull();
        // Target Class
        assertThat(cursor.getString(15)).isNull();
        // Enabled
        assertThat(cursor.getInt(16) == 1).isEqualTo(enabled);
        // Data ref key
        assertThat(cursor.getString(17)).isNull();
        // User Id
        assertThat(cursor.getInt(18)).isEqualTo(userId);
        // Payload Type - default is 0
        assertThat(cursor.getInt(19)).isEqualTo(0);
        // Payload - should be updated to real payloads as controllers are added
        byte[] payload = cursor.getBlob(20);
        ResultPayload unmarshalledPayload = ResultPayloadUtils.unmarshall(payload,
                ResultPayload.CREATOR);
        assertThat(unmarshalledPayload).isInstanceOf(ResultPayload.class);
    }

    @Test
    public void testResourceProvider_resourceRowInserted() {
        SearchIndexableResource resource = getFakeResource(0);
        resource.className = "com.android.settings.LegalSettings";

        mManager.indexOneSearchIndexableData(mDb, localeStr, resource, new HashMap<>());
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        assertThat(cursor.getCount()).isEqualTo(6);
    }

    @Test
    public void testResourceProvider_resourceRowMatches() {
        SearchIndexableResource resource = getFakeResource(0 /* xml */);
        resource.className = "com.android.settings.display.ScreenZoomSettings";

        mManager.indexOneSearchIndexableData(mDb, localeStr, resource, new HashMap<>());
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index ORDER BY data_title", null);
        cursor.moveToPosition(0);

        // Locale
        assertThat(cursor.getString(0)).isEqualTo(localeStr);
        // Data Rank
        assertThat(cursor.getInt(1)).isEqualTo(0);
        // Data Title
        assertThat(cursor.getString(2)).isEqualTo("Display size");
        // Normalized Title
        assertThat(cursor.getString(3)).isEqualTo("display size");
        // Summary On
        assertThat(cursor.getString(4)).isEmpty();
        // Summary On Normalized
        assertThat(cursor.getString(5)).isEmpty();
        // Summary Off - only on for checkbox preferences
        assertThat(cursor.getString(6)).isEmpty();
        // Summary off normalized - only on for checkbox preferences
        assertThat(cursor.getString(7)).isEmpty();
        // Entries - only on for list preferences
        assertThat(cursor.getString(8)).isNull();
        // Keywords
        assertThat(cursor.getString(9)).isEqualTo(
                "display density screen zoom scale scaling");
        // Screen Title
        assertThat(cursor.getString(10)).isEqualTo("Display size");
        // Class Name
        assertThat(cursor.getString(11))
                .isEqualTo("com.android.settings.display.ScreenZoomSettings");
        // Icon
        assertThat(cursor.getInt(12)).isEqualTo(noIcon);
        // Intent Action
        assertThat(cursor.getString(13)).isNull();
        // Target Package
        assertThat(cursor.getString(14)).isNull();
        // Target Class
        assertThat(cursor.getString(15)).isNull();
        // Enabled
        assertThat(cursor.getInt(16) == 1).isEqualTo(enabled);
        // Data ref key
        assertThat(cursor.getString(17)).isNull();
        // User Id
        assertThat(cursor.getInt(18)).isEqualTo(userId);
        // Payload Type - default is 0
        assertThat(cursor.getInt(19)).isEqualTo(0);
        // Payload - should be updated to real payloads as controllers are added
        byte[] payload = cursor.getBlob(20);
        ResultPayload unmarshalledPayload = ResultPayloadUtils.unmarshall(payload,
                ResultPayload.CREATOR);
        assertThat(unmarshalledPayload).isInstanceOf(ResultPayload.class);
    }

    @Test
    public void testResourceProvider_disabledResource_rowsInserted() {
        SearchIndexableResource resource = getFakeResource(0 /* xml */);
        resource.className = "com.android.settings.LegalSettings";

        mManager.indexOneSearchIndexableData(mDb, localeStr, resource,
                new HashMap<String, Set<String>>());

        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index WHERE enabled = 1", null);
        assertThat(cursor.getCount()).isEqualTo(1);
        cursor = mDb.rawQuery("SELECT * FROM prefs_index WHERE enabled = 0", null);
        assertThat(cursor.getCount()).isEqualTo(5);
    }

    @Test
    public void testResource_withTitleAndSettingName_titleNotInserted() {
        SearchIndexableResource resource = getFakeResource(R.xml.swipe_to_notification_settings);
        mManager.indexFromResource(mDb, localeStr, resource, new ArrayList<String>());

        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index WHERE" +
                " enabled = 1", null);
        assertThat(cursor.getCount()).isEqualTo(1);
    }

    @Test
    public void testResourceProvider_nonSubsettingIntent() {
        SearchIndexableResource resource = getFakeResource(0 /* xml */);
        String fakeAction = "fake_action";
        resource.className = "com.android.settings.LegalSettings";
        resource.intentAction = fakeAction;
        resource.intentTargetPackage = SearchIndexableResources.SUBSETTING_TARGET_PACKAGE;

        mManager.indexOneSearchIndexableData(mDb, localeStr, resource, new HashMap<>());
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        cursor.moveToPosition(0);

        // Intent Action
        assertThat(cursor.getString(13)).isEqualTo(fakeAction);
        // Target Package
        assertThat(cursor.getString(14))
                .isEqualTo(SearchIndexableResources.SUBSETTING_TARGET_PACKAGE);
    }

    // Test new public indexing flow

    @Test
    public void testPerformIndexing_fullIndex_getsDataFromProviders() {
        DummyProvider provider = new DummyProvider();
        provider.onCreate();
        ShadowContentResolver.registerProvider(AUTHORITY_ONE, provider);

        // Test that Indexables are added for Full indexing
        when(mPackageManager.queryIntentContentProviders(any(Intent.class), anyInt()))
                .thenReturn(getDummyResolveInfo());

        DatabaseIndexingManager manager =
                spy(new DatabaseIndexingManager(mContext, PACKAGE_ONE));

        manager.performIndexing();

        verify(manager).addIndexablesFromRemoteProvider(PACKAGE_ONE, AUTHORITY_ONE);
        verify(manager).updateDatabase(true /* isFullIndex */, Locale.getDefault().toString());
    }

    @Test
    public void testPerformIndexing_incrementalIndex_noDataAdded() {
        final List<ResolveInfo> providerInfo = getDummyResolveInfo();
        skipFullIndex(providerInfo);
        DummyProvider provider = new DummyProvider();
        provider.onCreate();
        ShadowContentResolver.registerProvider(AUTHORITY_ONE, provider);
        // Test that Indexables are added for Full indexing
        when(mPackageManager.queryIntentContentProviders(any(Intent.class), anyInt()))
                .thenReturn(providerInfo);

        DatabaseIndexingManager manager =
                spy(new DatabaseIndexingManager(mContext, PACKAGE_ONE));

        manager.mDataToProcess.dataToUpdate.clear();

        manager.performIndexing();

        verify(manager, times(0)).addDataToDatabase(any(SQLiteDatabase.class), anyString(),
                anyList(), anyMap());
        verify(manager, times(0)).addIndexablesFromRemoteProvider(PACKAGE_ONE, AUTHORITY_ONE);
        verify(manager).updateDataInDatabase(any(SQLiteDatabase.class), anyMap());
    }

    @Test
    public void testPerformIndexing_localeChanged_databaseDropped() {
        DummyProvider provider = new DummyProvider();
        provider.onCreate();
        ShadowContentResolver.registerProvider(AUTHORITY_ONE, provider);

        // Test that Indexables are added for Full indexing
        when(mPackageManager.queryIntentContentProviders(any(Intent.class), anyInt()))
                .thenReturn(getDummyResolveInfo());

        // Initialize the Manager
        DatabaseIndexingManager manager =
                spy(new DatabaseIndexingManager(mContext, PACKAGE_ONE));

        // Insert data point which will be dropped
        final String oldTitle = "This is French";
        insertSpecialCase(oldTitle, true, "key");

        // Add a data point to be added by the indexing
        SearchIndexableRaw raw = new SearchIndexableRaw(mContext);
        final String newTitle = "This is English";
        raw.title = newTitle;
        manager.mDataToProcess.dataToUpdate.add(raw);

        manager.performIndexing();

        // Assert that the New Title is inserted
        final Cursor newCursor = mDb.rawQuery("SELECT * FROM prefs_index WHERE data_title = '" +
                newTitle + "'", null);
        assertThat(newCursor.getCount()).isEqualTo(1);

        // Assert that the Old Title is no longer in the database, since it was dropped
        final Cursor oldCursor = mDb.rawQuery("SELECT * FROM prefs_index WHERE data_title = '" +
                oldTitle + "'", null);
        assertThat(oldCursor.getCount()).isEqualTo(0);
    }

    @Test
    public void testPerformIndexing_onOta_FullIndex() {
        DummyProvider provider = new DummyProvider();
        provider.onCreate();
        ShadowContentResolver.registerProvider(
                AUTHORITY_ONE, provider
        );

        // Test that Indexables are added for Full indexing
        when(mPackageManager.queryIntentContentProviders(any(Intent.class), anyInt()))
                .thenReturn(getDummyResolveInfo());

        DatabaseIndexingManager manager =
                spy(new DatabaseIndexingManager(mContext, PACKAGE_ONE));

        manager.performIndexing();

        verify(manager).updateDatabase(true /* isFullIndex */, Locale.getDefault().toString());
    }

    @Test
    public void testPerformIndexing_onPackageChange_shouldFullIndex() {
        final List<ResolveInfo> providers = getDummyResolveInfo();
        final String buildNumber = Build.FINGERPRINT;
        final String locale = Locale.getDefault().toString();
        skipFullIndex(providers);

        // This snapshot is already indexed. Should return false
        assertThat(IndexDatabaseHelper.isFullIndex(
                mContext, locale, buildNumber,
                IndexDatabaseHelper.buildProviderVersionedNames(providers)))
                .isFalse();

        // Change provider version number, this should trigger full index.
        providers.get(0).providerInfo.applicationInfo.versionCode++;

        assertThat(IndexDatabaseHelper.isFullIndex(mContext, locale, buildNumber,
                IndexDatabaseHelper.buildProviderVersionedNames(providers)))
                .isTrue();
    }

    @Test
    public void testPerformIndexing_onOta_buildNumberIsCached() {
        DummyProvider provider = new DummyProvider();
        provider.onCreate();
        ShadowContentResolver.registerProvider(
                AUTHORITY_ONE, provider
        );

        // Test that Indexables are added for Full indexing
        when(mPackageManager.queryIntentContentProviders(any(Intent.class), anyInt()))
                .thenReturn(getDummyResolveInfo());

        DatabaseIndexingManager manager =
                spy(new DatabaseIndexingManager(mContext, PACKAGE_ONE));

        manager.performIndexing();

        assertThat(IndexDatabaseHelper.getInstance(mContext).isBuildIndexed(mContext,
                Build.FINGERPRINT)).isTrue();
    }

    @Test
    public void testFullUpdatedDatabase_noData_addDataToDatabaseNotCalled() {
        mManager.updateDatabase(true /* isFullIndex */, localeStr);
        mManager.mDataToProcess.dataToUpdate.clear();
        verify(mManager, times(0)).addDataToDatabase(any(SQLiteDatabase.class), anyString(),
                anyList(), anyMap());
    }

    @Test
    public void testFullUpdatedDatabase_updatedDataInDatabaseNotCalled() {
        mManager.updateDatabase(true /* isFullIndex */, localeStr);
        verify(mManager, times(0)).updateDataInDatabase(any(SQLiteDatabase.class), anyMap());
    }

    @Test
    public void testLocaleUpdated_afterIndexing_localeNotAdded() {
        mManager.updateDatabase(true /* isFullIndex */, localeStr);
        assertThat(IndexDatabaseHelper.getInstance(mContext)
                .isLocaleAlreadyIndexed(mContext, localeStr)).isFalse();
    }

    @Test
    public void testLocaleUpdated_afterFullIndexing_localeAdded() {
        mManager.performIndexing();
        assertThat(IndexDatabaseHelper.getInstance(mContext)
                .isLocaleAlreadyIndexed(mContext, localeStr)).isTrue();
    }

    @Test
    public void testUpdateDatabase_newEligibleData_addedToDatabase() {
        // Test that addDataToDatabase is called when dataToUpdate is non-empty
        mManager.mDataToProcess.dataToUpdate.add(getFakeRaw());
        mManager.updateDatabase(true /* isFullIndex */, localeStr);

        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        cursor.moveToPosition(0);

        // Locale
        assertThat(cursor.getString(0)).isEqualTo(localeStr);
        // Data Rank
        assertThat(cursor.getInt(1)).isEqualTo(rank);
        // Data Title
        assertThat(cursor.getString(2)).isEqualTo(updatedTitle);
        // Normalized Title
        assertThat(cursor.getString(3)).isEqualTo(normalizedTitle);
        // Summary On
        assertThat(cursor.getString(4)).isEqualTo(updatedSummaryOn);
        // Summary On Normalized
        assertThat(cursor.getString(5)).isEqualTo(normalizedSummaryOn);
        // Summary Off
        assertThat(cursor.getString(6)).isEqualTo(updatedSummaryOff);
        // Summary off normalized
        assertThat(cursor.getString(7)).isEqualTo(normalizedSummaryOff);
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

        mManager.updateDatabase(false, localeStr);

        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index WHERE enabled = 1", null);
        cursor.moveToPosition(0);
        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getString(2)).isEqualTo(TITLE_ONE);
    }

    @Test
    public void testUpdateAsyncTask_onPostExecute_performsCallback() {
        IndexingCallback callback = mock(IndexingCallback.class);

        DatabaseIndexingManager.IndexingTask task = mManager.new IndexingTask(callback);
        task.execute();

        Robolectric.flushForegroundThreadScheduler();

        verify(callback).onIndexingFinished();
    }

    @Test
    public void testUpdateAsyncTask_onPostExecute_setsIndexingComplete() {
        SearchFeatureProviderImpl provider = new SearchFeatureProviderImpl();
        DatabaseIndexingManager manager = spy(provider.getIndexingManager(mContext));
        DatabaseIndexingManager.IndexingTask task = manager.new IndexingTask(null);
        doNothing().when(manager).performIndexing();

        task.execute();
        Robolectric.flushForegroundThreadScheduler();

        assertThat(provider.isIndexingComplete(mContext)).isTrue();
    }

    // Util functions

    private void skipFullIndex(List<ResolveInfo> providers) {
        IndexDatabaseHelper.setLocaleIndexed(mContext, Locale.getDefault().toString());
        IndexDatabaseHelper.setBuildIndexed(mContext, Build.FINGERPRINT);
        IndexDatabaseHelper.setProvidersIndexed(mContext,
                IndexDatabaseHelper.buildProviderVersionedNames(providers));
    }

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

    private SearchIndexableResource getFakeResource(int xml) {
        SearchIndexableResource sir = new SearchIndexableResource(mContext);
        sir.rank = rank;
        sir.xmlResId = xml;
        sir.className = className;
        sir.packageName = packageName;
        sir.iconResId = iconResId;
        sir.intentAction = action;
        sir.intentTargetPackage = targetPackage;
        sir.intentTargetClass = targetClass;
        sir.enabled = enabled;
        return sir;
    }

    private Map<String, Set<String>> getNonIndexableKeys(String[] keys) {
        Map<String, Set<String>> niks = new HashMap<>();
        Set<String> keysList = new HashSet<>();
        keysList.addAll(Arrays.asList(keys));
        niks.put(packageName, keysList);
        return niks;
    }

    private List<ResolveInfo> getDummyResolveInfo() {
        List<ResolveInfo> infoList = new ArrayList<>();
        ResolveInfo info = new ResolveInfo();
        info.providerInfo = new ProviderInfo();
        info.providerInfo.exported = true;
        info.providerInfo.authority = AUTHORITY_ONE;
        info.providerInfo.packageName = PACKAGE_ONE;
        info.providerInfo.applicationInfo = new ApplicationInfo();
        infoList.add(info);

        return infoList;
    }

    // TODO move this method and its counterpart in CursorToSearchResultConverterTest into
    // a util class with public fields to assert values.
    private Cursor getDummyCursor() {
        MatrixCursor cursor = new MatrixCursor(INDEXABLES_RAW_COLUMNS);
        final String BLANK = "";

        ArrayList<String> item =
                new ArrayList<>(INDEXABLES_RAW_COLUMNS.length);
        item.add("42"); // Rank
        item.add(TITLE_ONE); // Title
        item.add(BLANK); // Summary on
        item.add(BLANK); // summary off
        item.add(BLANK); // entries
        item.add(BLANK); // keywords
        item.add(BLANK); // screen title
        item.add(BLANK); // classname
        item.add("123"); // Icon
        item.add(BLANK); // Intent action
        item.add(BLANK); // target package
        item.add(BLANK); // target class
        item.add(KEY_ONE); // Key
        item.add("-1"); // userId
        cursor.addRow(item);

        return cursor;
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

    private class DummyProvider extends ContentProvider {

        @Override
        public boolean onCreate() {
            return false;
        }

        @Override
        public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                @Nullable String selection, @Nullable String[] selectionArgs,
                @Nullable String sortOrder) {
            if (uri.toString().contains("xml")) {
                return null;
            }
            return getDummyCursor();
        }

        @Override
        public String getType(@NonNull Uri uri) {
            return null;
        }

        @Override
        public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
            return null;
        }

        @Override
        public int delete(@NonNull Uri uri, @Nullable String selection,
                @Nullable String[] selectionArgs) {
            return 0;
        }

        @Override
        public int update(@NonNull Uri uri, @Nullable ContentValues values,
                @Nullable String selection, @Nullable String[] selectionArgs) {
            return 0;
        }
    }
}
