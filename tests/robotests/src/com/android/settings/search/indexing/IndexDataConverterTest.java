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

package com.android.settings.search.indexing;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.SearchIndexableResource;
import com.android.settings.TestConfig;
import com.android.settings.search.IndexDatabaseHelper;
import com.android.settings.search.ResultPayload;
import com.android.settings.search.ResultPayloadUtils;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.search.SearchIndexableResources;
import com.android.settings.testutils.DatabaseTestUtils;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.android.settings.R.*;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class IndexDataConverterTest {

    private final String localeStr = "en_US";

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

    private Context mContext;

    private IndexDataConverter mConverter;
    private SQLiteDatabase mDb;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mDb = IndexDatabaseHelper.getInstance(mContext).getWritableDatabase();
        mConverter = spy(new IndexDataConverter(mContext, mDb));
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void testInsertRawColumn_rowInserted() {
        SearchIndexableRaw raw = getFakeRaw();
        mConverter.indexOneSearchIndexableData(localeStr, raw,
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

        mConverter.indexOneSearchIndexableData(localeStr, raw, niks);
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index WHERE enabled = 0", null);
        assertThat(cursor.getCount()).isEqualTo(1);
    }

    @Test
    public void testInsertRawColumn_rowMatches() {
        SearchIndexableRaw raw = getFakeRaw();
        mConverter.indexOneSearchIndexableData(localeStr, raw,
                new HashMap<>()/* Non-indexable keys */);
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
        mConverter.indexOneSearchIndexableData(localeStr, raw, null /* Non-indexable keys */);
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        assertThat(cursor.getCount()).isEqualTo(0);
    }

    // Tests for the flow: IndexOneResource -> IndexFromResource ->
    //                     UpdateOneRowWithFilteredData -> UpdateOneRow

    @Test
    public void testNullResource_NothingInserted() {
        mConverter.indexOneSearchIndexableData(localeStr, null /* searchIndexableResource */,
                new HashMap<>());
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        assertThat(cursor.getCount()).isEqualTo(0);
    }

    @Test
    public void testAddResource_RowsInserted() {
        SearchIndexableResource resource = getFakeResource(xml.display_settings);
        mConverter.indexOneSearchIndexableData(localeStr, resource, new HashMap<>());
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        assertThat(cursor.getCount()).isEqualTo(17);
    }

    @Test
    public void testAddResource_withNIKs_rowsInsertedDisabled() {
        SearchIndexableResource resource = getFakeResource(xml.display_settings);
        // Only add 2 of 16 items to be disabled.
        String[] keys = {"brightness", "wallpaper"};
        Map<String, Set<String>> niks = getNonIndexableKeys(keys);

        mConverter.indexOneSearchIndexableData(localeStr, resource, niks);

        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index WHERE enabled = 0", null);
        assertThat(cursor.getCount()).isEqualTo(2);
        cursor = mDb.rawQuery("SELECT * FROM prefs_index WHERE enabled = 1", null);
        assertThat(cursor.getCount()).isEqualTo(15);
    }

    @Test
    public void testAddResourceHeader_rowsMatch() {
        SearchIndexableResource resource = getFakeResource(xml.application_settings);
        mConverter.indexOneSearchIndexableData(localeStr, resource, new HashMap<>());

        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index ORDER BY data_title", null);
        cursor.moveToPosition(1);

        // Locale
        assertThat(cursor.getString(0)).isEqualTo(localeStr);
        // Data Title
        assertThat(cursor.getString(2)).isEqualTo("App info");
        // Normalized Title
        assertThat(cursor.getString(3)).isEqualTo("app info");
        // Summary On
        assertThat(cursor.getString(4)).isEqualTo("Manage apps, set up quick launch shortcuts");
        // Summary On Normalized
        assertThat(cursor.getString(5)).isEqualTo("manage apps, set up quick launch shortcuts");
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
    public void testAddResource_customSetting_rowsMatch() {
        SearchIndexableResource resource = getFakeResource(xml.swipe_to_notification_settings);
        mConverter.indexOneSearchIndexableData(localeStr, resource, new HashMap<>());
        final String prefTitle =
                mContext.getString(string.fingerprint_swipe_for_notifications_title);
        final String prefSummary =
                mContext.getString(string.fingerprint_swipe_for_notifications_summary);
        final String keywords = mContext.getString(string.keywords_gesture);
        Cursor cursor = mDb.rawQuery(
                "SELECT * FROM prefs_index where data_title='" + prefTitle + "'", null);
        cursor.moveToFirst();

        // Locale
        assertThat(cursor.getString(0)).isEqualTo(localeStr);
        // Data Title
        assertThat(cursor.getString(2)).isEqualTo(prefTitle);
        // Normalized Title
        assertThat(cursor.getString(3)).isEqualTo(prefTitle.toLowerCase());
        // Summary On
        assertThat(cursor.getString(4)).isEqualTo(prefSummary);
        // Summary On Normalized
        assertThat(cursor.getString(5)).isEqualTo(prefSummary.toLowerCase());
        // Entries - only on for list preferences
        assertThat(cursor.getString(8)).isNull();
        // Keywords
        assertThat(cursor.getString(9)).isEqualTo(keywords);
        // Screen Title
        assertThat(cursor.getString(10)).isEqualTo(
                mContext.getString(string.fingerprint_swipe_for_notifications_title));
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
        SearchIndexableResource resource = getFakeResource(xml.application_settings);
        mConverter.indexOneSearchIndexableData(localeStr, resource, new HashMap<>());

        /* Should return 6 results, with the following titles:
         * Advanced Settings, Apps, Manage Apps, Preferred install location, Running Services
         */
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index ORDER BY data_title", null);
        cursor.moveToPosition(0);
        // Locale
        assertThat(cursor.getString(0)).isEqualTo(localeStr);
        // Data Title
        assertThat(cursor.getString(2)).isEqualTo("Advanced settings");
        // Normalized Title
        assertThat(cursor.getString(3)).isEqualTo("advanced settings");
        // Summary On
        assertThat(cursor.getString(4)).isEqualTo("Enable more settings options");
        // Summary On Normalized
        assertThat(cursor.getString(5)).isEqualTo("enable more settings options");
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
        SearchIndexableResource resource = getFakeResource(xml.application_settings);
        mConverter.indexOneSearchIndexableData(localeStr, resource, new HashMap<>());

        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index ORDER BY data_title", null);
        cursor.moveToPosition(3);
        // Locale
        assertThat(cursor.getString(0)).isEqualTo(localeStr);
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
        SearchIndexableResource resource = getFakeResource(xml.connected_devices);
        mConverter.indexOneSearchIndexableData(localeStr, resource, new HashMap<>());

        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index ORDER BY data_title", null);
        cursor.moveToPosition(0);

        // Icon
        assertThat(cursor.getInt(12)).isNotEqualTo(noIcon);
    }

    // Tests for the flow: IndexOneResource -> IndexFromProvider -> IndexFromResource ->
    //                     UpdateOneRowWithFilteredData -> UpdateOneRow

    @Test
    public void testResourceProvider_rowInserted() {
        SearchIndexableResource resource = getFakeResource(xml.swipe_to_notification_settings);
        resource.xmlResId = 0;
        resource.className = "com.android.settings.display.ScreenZoomSettings";

        mConverter.indexOneSearchIndexableData(localeStr, resource, new HashMap<>());
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        assertThat(cursor.getCount()).isEqualTo(1);
    }

    @Test
    public void testResourceProvider_rowMatches() {
        SearchIndexableResource resource = getFakeResource(xml.swipe_to_notification_settings);
        resource.xmlResId = 0;
        resource.className = "com.android.settings.display.ScreenZoomSettings";

        mConverter.indexOneSearchIndexableData(localeStr, resource, new HashMap<>());
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        cursor.moveToPosition(0);

        // Locale
        assertThat(cursor.getString(0)).isEqualTo(localeStr);
        // Data Title
        assertThat(cursor.getString(2)).isEqualTo("Display size");
        // Normalized Title
        assertThat(cursor.getString(3)).isEqualTo("display size");
        // Summary On
        assertThat(cursor.getString(4)).isEmpty();
        // Summary On Normalized
        assertThat(cursor.getString(5)).isEmpty();
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

        mConverter.indexOneSearchIndexableData(localeStr, resource, new HashMap<>());
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        assertThat(cursor.getCount()).isEqualTo(6);
    }

    @Test
    public void testResourceProvider_resourceRowMatches() {
        SearchIndexableResource resource = getFakeResource(0 /* xml */);
        resource.className = "com.android.settings.display.ScreenZoomSettings";

        mConverter.indexOneSearchIndexableData(localeStr, resource, new HashMap<>());
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index ORDER BY data_title", null);
        cursor.moveToPosition(0);

        // Locale
        assertThat(cursor.getString(0)).isEqualTo(localeStr);
        // Data Title
        assertThat(cursor.getString(2)).isEqualTo("Display size");
        // Normalized Title
        assertThat(cursor.getString(3)).isEqualTo("display size");
        // Summary On
        assertThat(cursor.getString(4)).isEmpty();
        // Summary On Normalized
        assertThat(cursor.getString(5)).isEmpty();
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

        mConverter.indexOneSearchIndexableData(localeStr, resource, new HashMap<>());

        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index WHERE enabled = 1", null);
        assertThat(cursor.getCount()).isEqualTo(1);
        cursor = mDb.rawQuery("SELECT * FROM prefs_index WHERE enabled = 0", null);
        assertThat(cursor.getCount()).isEqualTo(5);
    }

    @Test
    public void testResource_withTitleAndSettingName_titleNotInserted() {
        SearchIndexableResource resource = getFakeResource(xml.swipe_to_notification_settings);
        mConverter.indexFromResource(localeStr, resource, new ArrayList<>());

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

        mConverter.indexOneSearchIndexableData(localeStr, resource, new HashMap<>());
        Cursor cursor = mDb.rawQuery("SELECT * FROM prefs_index", null);
        cursor.moveToPosition(0);

        // Intent Action
        assertThat(cursor.getString(13)).isEqualTo(fakeAction);
        // Target Package
        assertThat(cursor.getString(14))
                .isEqualTo(SearchIndexableResources.SUBSETTING_TARGET_PACKAGE);
    }

    private SearchIndexableRaw getFakeRaw() {
        return getFakeRaw(localeStr);
    }

    private SearchIndexableRaw getFakeRaw(String localeStr) {
        SearchIndexableRaw data = new SearchIndexableRaw(mContext);
        data.locale = new Locale(localeStr);
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
}
