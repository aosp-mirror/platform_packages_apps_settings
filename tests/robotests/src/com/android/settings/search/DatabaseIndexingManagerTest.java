/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.search2.DatabaseIndexingManager;
import com.android.settings.testutils.DatabaseTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DatabaseIndexingManagerTest {
    private final String localeStr = "en_US";

    private final int rank = 42;
    private final String title = "title\u2011title";
    private final String updatedTitle = "title-title";
    private final String normalizedTitle = "titletitle";
    private final String summaryOn = "summary\u2011on";
    private final String updatedSummaryOn = "summary-on";
    private final String normalizedSummaryOn = "summaryon";
    private final String summaryOff = "summary\u2011off";
    private final String updatedSummaryOff ="summary-off";
    private final String normalizedSummaryOff = "summaryoff";
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

    private Context mContext;
    private DatabaseIndexingManager mManager;
    private SQLiteDatabase mDb;

    @Before
    public void setUp() {
        mContext = ShadowApplication.getInstance().getApplicationContext();
        mManager = spy(new DatabaseIndexingManager(mContext, mContext.getPackageName()));
        mDb = IndexDatabaseHelper.getInstance(mContext).getWritableDatabase();
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb();
    }

    @Test
    public void testDatabaseSchema() {
        Cursor dbCursor = mDb.query("prefs_index", null, null, null, null, null, null);
        List<String> columnNames = new ArrayList<>(Arrays.asList(dbCursor.getColumnNames()));
        // Note that docid is not included.
        List<String> expColumnNames = new ArrayList<>(Arrays.asList(new String[ ]{
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
    public void testInsertRawColumn_RowInserted() {
        SearchIndexableRaw raw = getFakeRaw();
        mManager.indexOneSearchIndexableData(mDb, localeStr, raw, null /* Non-indexable keys */);
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        assertThat(cursor.getCount()).isEqualTo(1);
    }

    @Test
    public void testInsertRawColumn_RowMatches() {
        SearchIndexableRaw raw = getFakeRaw();
        mManager.indexOneSearchIndexableData(mDb, localeStr, raw, null /* Non-indexable keys */);
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
        assertThat(cursor.getBlob(20)).isNull();
    }

    @Test
    public void testInsertRawColumnMismatchedLocale_NoRowInserted() {
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
        SearchIndexableResource resource = getFakeResource(R.xml.gesture_settings);
        mManager.indexOneSearchIndexableData(mDb, localeStr, resource,
                new HashMap<>());
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        assertThat(cursor.getCount()).isEqualTo(6);
    }

    @Test
    public void testAddResourceHeader_RowsMatch() {
        SearchIndexableResource resource = getFakeResource(R.xml.application_settings);
        mManager.indexOneSearchIndexableData(mDb, localeStr, resource,
                new HashMap<>());

        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index ORDER BY data_title", null);
        cursor.moveToPosition(1);

        // Locale
        assertThat(cursor.getString(0)).isEqualTo(localeStr);
        // Data Rank
        assertThat(cursor.getInt(1)).isEqualTo(rank);
        // Data Title
        assertThat(cursor.getString(2)).isEqualTo("Apps");
        // Normalized Title
        assertThat(cursor.getString(3)).isEqualTo("apps");
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
        assertThat(cursor.getString(10)).isEqualTo("Apps");
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
        assertThat(cursor.getString(17)).isEqualTo("applications_settings");
        // User Id
        assertThat(cursor.getInt(18)).isEqualTo(userId);
        // Payload Type - default is 0
        assertThat(cursor.getInt(19)).isEqualTo(0);
        // Payload - should be updated to real payloads as controllers are added
        assertThat(cursor.getBlob(20)).isNull();
    }

    @Test
    public void testAddResourceCustomSetting_RowsMatch() {
        SearchIndexableResource resource = getFakeResource(R.xml.gesture_settings);
        mManager.indexOneSearchIndexableData(mDb, localeStr, resource,
                new HashMap<>());
        final String prefTitle =
                mContext.getString(R.string.fingerprint_swipe_for_notifications_title);
        final String prefSummary =
                mContext.getString(R.string.fingerprint_swipe_for_notifications_summary);
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
        assertThat(cursor.getString(9)).isEmpty();
        // Screen Title
        assertThat(cursor.getString(10)).isEqualTo(
                mContext.getString(R.string.gesture_preference_title));
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
        assertThat(cursor.getString(17)).isEqualTo("gesture_swipe_down_fingerprint");
        // User Id
        assertThat(cursor.getInt(18)).isEqualTo(userId);
        // Payload Type - default is 0
        assertThat(cursor.getInt(19)).isEqualTo(0);
        // Payload - should be updated to real payloads as controllers are added
        assertThat(cursor.getBlob(20)).isNull();
    }

    @Test
    public void testAddResourceCheckboxPreference_RowsMatch() {
        SearchIndexableResource resource = getFakeResource(R.xml.application_settings);
        mManager.indexOneSearchIndexableData(mDb, localeStr, resource,
                new HashMap<>());

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
        assertThat(cursor.getString(10)).isEqualTo("Apps");
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
        assertThat(cursor.getString(17)).isEqualTo("toggle_advanced_settings");
        // User Id
        assertThat(cursor.getInt(18)).isEqualTo(userId);
        // Payload Type - default is 0
        assertThat(cursor.getInt(19)).isEqualTo(0);
        // Payload - should be updated to real payloads as controllers are added
        assertThat(cursor.getBlob(20)).isNull();
    }

    @Test
    public void testAddResourceListPreference_RowsMatch() {
        SearchIndexableResource resource = getFakeResource(R.xml.application_settings);
        mManager.indexOneSearchIndexableData(mDb, localeStr, resource,
                new HashMap<>());

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
        assertThat(cursor.getString(4)).isEqualTo("Change the preferred installation location for new apps");
        // Summary On Normalized
        assertThat(cursor.getString(5)).isEqualTo("change the preferred installation location for new apps");
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
        assertThat(cursor.getString(10)).isEqualTo("Apps");
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
        assertThat(cursor.getString(17)).isEqualTo("app_install_location");
        // User Id
        assertThat(cursor.getInt(18)).isEqualTo(userId);
        // Payload Type - default is 0
        assertThat(cursor.getInt(19)).isEqualTo(0);
        // Payload - should be updated to real payloads as controllers are added
        assertThat(cursor.getBlob(20)).isNull();
    }

    // Tests for the flow: IndexOneResource -> IndexFromProvider -> IndexFromResource ->
    //                     UpdateOneRowWithFilteredData -> UpdateOneRow

    @Test
    public void testResourceProvider_RowInserted() {
        SearchIndexableResource resource = getFakeResource(R.xml.gesture_settings);
        resource.xmlResId = 0;
        resource.className = "com.android.settings.display.ScreenZoomSettings";

        mManager.indexOneSearchIndexableData(mDb, localeStr, resource,
                new HashMap<>());
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        assertThat(cursor.getCount()).isEqualTo(1);
    }

    @Test
    public void testResourceProvider_Matches() {
        SearchIndexableResource resource = getFakeResource(R.xml.gesture_settings);
        resource.xmlResId = 0;
        resource.className = "com.android.settings.display.ScreenZoomSettings";

        mManager.indexOneSearchIndexableData(mDb, localeStr, resource,
                new HashMap<>());
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        cursor.moveToPosition(0);

        // Locale
        assertThat(cursor.getString(0)).isEqualTo(localeStr);
        // Data Rank
        assertThat(cursor.getInt(1)).isEqualTo(rank);
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
        assertThat(cursor.getInt(12)).isEqualTo(iconResId);
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
        assertThat(cursor.getBlob(20)).isNull();
    }

    @Test
    public void testResourceProvider_ResourceRowInserted() {
        SearchIndexableResource resource = getFakeResource(R.xml.gesture_settings);
        resource.xmlResId = 0;
        resource.className = "com.android.settings.LegalSettings";

        mManager.indexOneSearchIndexableData(mDb, localeStr, resource,
                new HashMap<>());
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        assertThat(cursor.getCount()).isEqualTo(2);
    }

    @Test
    public void testResourceProvider_ResourceRowMatches() {
        SearchIndexableResource resource = getFakeResource(R.xml.gesture_settings);
        resource.xmlResId = 0;
        resource.className = "com.android.settings.LegalSettings";

        mManager.indexOneSearchIndexableData(mDb, localeStr, resource,
                new HashMap<>());
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index ORDER BY data_title", null);
        cursor.moveToPosition(0);

        // Locale
        assertThat(cursor.getString(0)).isEqualTo(localeStr);
        // Data Rank
        assertThat(cursor.getInt(1)).isEqualTo(rank);
        // Data Title
        assertThat(cursor.getString(2)).isEqualTo("Legal information");
        // Normalized Title
        assertThat(cursor.getString(3)).isEqualTo("legal information");
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
        assertThat(cursor.getString(9)).isEmpty();
        // Screen Title
        assertThat(cursor.getString(10)).isEqualTo("Legal information");
        // Class Name
        assertThat(cursor.getString(11))
                .isEqualTo("com.android.settings.LegalSettings");
        // Icon
        assertThat(cursor.getInt(12)).isEqualTo(iconResId);
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
        assertThat(cursor.getBlob(20)).isNull();
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
}
