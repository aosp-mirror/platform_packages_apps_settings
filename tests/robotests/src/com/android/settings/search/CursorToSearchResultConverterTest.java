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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.drawable.Drawable;

import com.android.settings.DisplaySettings;
import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.SiteMapManager;
import com.android.settings.gestures.SwipeToNotificationSettings;
import com.android.settings.search.ResultPayload.Availability;
import com.android.settings.search.ResultPayload.PayloadType;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.wifi.WifiSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class CursorToSearchResultConverterTest {

    private static final List<String> TITLES = Arrays.asList("title1", "title2", "title3");
    private static final String SUMMARY = "summary";
    private static final String TARGET_PACKAGE = "a.b.c";
    private static final String TARGET_CLASS = "a.b.c.class";
    private static final String KEY = "key";
    private static final Intent INTENT = new Intent("com.android.settings");
    private static final int ICON = R.drawable.ic_search_24dp;
    private static final int BASE_RANK = 1;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SiteMapManager mSiteMapManager;
    private Drawable mDrawable;
    private CursorToSearchResultConverter mConverter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Context context = Robolectric.buildActivity(Activity.class).get();
        mDrawable = context.getDrawable(ICON);
        mConverter = new CursorToSearchResultConverter(context);
    }

    @Test
    public void testParseNullResults_ReturnsNull() {
        final Set<SearchResult> results = mConverter.convertCursor(
                mSiteMapManager, null, BASE_RANK);
        assertThat(results).isNull();
    }

    @Test
    public void testParseCursor_NotNull() {
        final Set<SearchResult> results = mConverter.convertCursor(
                mSiteMapManager, getDummyCursor(), BASE_RANK);
        assertThat(results).isNotNull();
    }

    @Test
    public void testParseCursor_MatchesRank() {
        final Set<SearchResult> results = mConverter.convertCursor(
                mSiteMapManager, getDummyCursor(), BASE_RANK);
        for (SearchResult result : results) {
            assertThat(result.rank).isEqualTo(BASE_RANK);
        }
    }

    @Test
    public void testParseCursor_MatchesTitle() {
        final Set<SearchResult> results = mConverter.convertCursor(
                mSiteMapManager, getDummyCursor(), BASE_RANK);
        for (SearchResult result : results) {
            assertThat(TITLES).contains(result.title);
        }
    }

    @Test
    public void testParseCursor_MatchesSummary() {
        final Set<SearchResult> results = mConverter.convertCursor(
                mSiteMapManager, getDummyCursor(), BASE_RANK);
        for (SearchResult result : results) {
            assertThat(result.summary).isEqualTo(SUMMARY);
        }
    }

    @Test
    public void testParseCursor_MatchesIcon() {
        final MatrixCursor cursor = new MatrixCursor(DatabaseResultLoader.SELECT_COLUMNS);
        final byte[] payload = ResultPayloadUtils.marshall(new ResultPayload(INTENT));
        final String BLANK = "";
        cursor.addRow(new Object[]{
                KEY.hashCode(),      // Doc ID
                "Longer than 20 characters", // Title
                SUMMARY, // Summary on
                SUMMARY, // summary off
                DisplaySettings.class.getName(),
                BLANK,   // screen title
                ICON,    // icon
                BLANK,   // action
                null,    // target package
                BLANK,   // target class
                KEY,   // Key
                PayloadType.INTENT,       // Payload Type
                payload     // Payload
        });

        final Set<SearchResult> results = mConverter.convertCursor(
                mSiteMapManager, cursor, BASE_RANK);

        for (SearchResult result : results) {
            Drawable resultDrawable = result.icon;
            assertThat(resultDrawable).isNotNull();
            assertThat(resultDrawable.toString()).isEqualTo(mDrawable.toString());
        }
    }

    @Test
    public void testParseCursor_NoIcon() {
        final Set<SearchResult> results = mConverter.convertCursor(
                mSiteMapManager, getDummyCursor("noIcon" /* key */, "" /* className */), BASE_RANK);
        for (SearchResult result : results) {
            assertThat(result.icon).isNull();
        }
    }

    @Test
    public void testParseCursor_MatchesPayloadType() {
        final Set<SearchResult> results = mConverter.convertCursor(
                mSiteMapManager, getDummyCursor(), BASE_RANK);
        ResultPayload payload;
        for (SearchResult result : results) {
            payload = result.payload;
            assertThat(payload.getType()).isEqualTo(PayloadType.INTENT);
        }
    }

    @Test
    public void testLongTitle_PenalizedInRank() {
        final MatrixCursor cursor = new MatrixCursor(DatabaseResultLoader.SELECT_COLUMNS);
        final byte[] payload = ResultPayloadUtils.marshall(new ResultPayload(INTENT));
        final String BLANK = "";
        cursor.addRow(new Object[]{
                KEY.hashCode(),      // Doc ID
                "Longer than 20 characters", // Title
                SUMMARY, // Summary on
                SUMMARY, // summary off
                DisplaySettings.class.getName(),
                BLANK,   // screen title
                null,    // icon
                BLANK,   // action
                null,    // target package
                BLANK,   // target class
                KEY,   // Key
                PayloadType.INTENT,       // Payload Type
                payload     // Payload
        });
        final Set<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor,
                BASE_RANK);
        for (SearchResult result : results) {
            assertThat(result.rank).isEqualTo(BASE_RANK + 1);
        }
    }

    @Test
    public void testParseCursor_MatchesResultPayload() {
        final Set<SearchResult> results = mConverter.convertCursor(
                mSiteMapManager, getDummyCursor(), BASE_RANK);
        ResultPayload payload;
        for (SearchResult result : results) {
            payload = result.payload;
            Intent intent = payload.getIntent();
            assertThat(intent.getAction()).isEqualTo(INTENT.getAction());
        }
    }


    @Test
    public void testParseCursor_MatchesInlineSwitchPayload() {
        MatrixCursor cursor = new MatrixCursor(DatabaseResultLoader.SELECT_COLUMNS);
        final String BLANK = "";
        final String uri = "test.com";
        final int type = ResultPayload.PayloadType.INLINE_SWITCH;
        final int source = ResultPayload.SettingsSource.SECURE;
        final String intentKey = "key";
        final String intentVal = "value";
        final Intent intent = new Intent();
        intent.putExtra(intentKey, intentVal);
        final InlineSwitchPayload payload = new InlineSwitchPayload(uri, source, 1 /* onValue */,
                intent, true /* isDeviceSupported */, 0 /* defautValue */);

        cursor.addRow(new Object[]{
                KEY.hashCode(),      // Doc ID
                TITLES.get(0), // Title
                SUMMARY, // Summary on
                SUMMARY, // summary off
                SwipeToNotificationSettings.class.getName(),
                BLANK,   // screen title
                null,    // icon
                BLANK,   // action
                null,    // target package
                BLANK,   // target class
                KEY,   // Key
                type,    // Payload Type
                ResultPayloadUtils.marshall(payload) // Payload
        });
        final Set<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor,
                BASE_RANK);

        for (SearchResult result : results) {
            final InlineSwitchPayload newPayload = (InlineSwitchPayload) result.payload;
            final Intent rebuiltIntent = newPayload.getIntent();
            assertThat(newPayload.mSettingKey).isEqualTo(uri);
            assertThat(newPayload.getType()).isEqualTo(type);
            assertThat(newPayload.mSettingSource).isEqualTo(source);
            assertThat(newPayload.isStandard()).isTrue();
            assertThat(newPayload.getAvailability()).isEqualTo(Availability.AVAILABLE);
            assertThat(rebuiltIntent.getStringExtra(intentKey)).isEqualTo(intentVal);
        }
    }

    // The following tests are temporary, and should be removed when we replace the Search
    // White-list solution for elevating ranking.

    @Test
    public void testWifiKey_PrioritizedResult() {
        final String key = "main_toggle_wifi";
        final Cursor cursor = getDummyCursor(key,  WifiSettings.class.getName());
        final Set<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor,
                BASE_RANK);

        for (SearchResult result : results) {
            assertThat(result.rank).isEqualTo(SearchResult.TOP_RANK);
        }
    }

    @Test
    public void testBluetoothKey_PrioritizedResult() {
        final String key = "main_toggle_bluetooth";
        final Cursor cursor = getDummyCursor(key,  WifiSettings.class.getName());
        final Set<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor,
                BASE_RANK);

        for (SearchResult result : results) {
            assertThat(result.rank).isEqualTo(SearchResult.TOP_RANK);
        }
    }

    @Test
    public void testAirplaneKey_PrioritizedResult() {
        final String key = "toggle_airplane";
        final Cursor cursor = getDummyCursor(key,  WifiSettings.class.getName());
        Set<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor, BASE_RANK);
        for (SearchResult result : results) {
            assertThat(result.rank).isEqualTo(SearchResult.TOP_RANK);
        }
    }

    @Test
    public void testHotspotKey_PrioritizedResult() {
        final String key = "tether_settings";
        final Cursor cursor = getDummyCursor(key,  WifiSettings.class.getName());
        final Set<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor,
                BASE_RANK);

        for (SearchResult result : results) {
            assertThat(result.rank).isEqualTo(SearchResult.TOP_RANK);
        }
    }

    @Test
    public void testBatterySaverKey_PrioritizedResult() {
        final String key = "battery_saver";
        final Cursor cursor = getDummyCursor(key,  WifiSettings.class.getName());
        final Set<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor,
                BASE_RANK);

        for (SearchResult result : results) {
            assertThat(result.rank).isEqualTo(SearchResult.TOP_RANK);
        }
    }

    @Test
    public void testNFCKey_PrioritizedResult() {
        final String key = "toggle_nfc";
        final Cursor cursor = getDummyCursor(key,  WifiSettings.class.getName());
        final Set<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor,
                BASE_RANK);

        for (SearchResult result : results) {
            assertThat(result.rank).isEqualTo(SearchResult.TOP_RANK);
        }
    }

    @Test
    public void testDataSaverKey_PrioritizedResult() {
        final String key = "restrict_background";
        final Cursor cursor = getDummyCursor(key,  WifiSettings.class.getName());
        final Set<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor,
                BASE_RANK);

        for (SearchResult result : results) {
            assertThat(result.rank).isEqualTo(SearchResult.TOP_RANK);
        }
    }

    @Test
    public void testDataUsageKey_PrioritizedResult() {
        final String key = "data_usage_enable";
        final Cursor cursor = getDummyCursor(key,  WifiSettings.class.getName());
        final Set<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor,
                BASE_RANK);
        for (SearchResult result : results) {
            assertThat(result.rank).isEqualTo(SearchResult.TOP_RANK);
        }
    }

    @Test
    public void testRoamingKey_PrioritizedResult() {
        final String key = "button_roaming_key";
        final Cursor cursor = getDummyCursor(key,  WifiSettings.class.getName());
        final Set<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor,
                BASE_RANK);

        for (SearchResult result : results) {
            assertThat(result.rank).isEqualTo(SearchResult.TOP_RANK);
        }
    }

    // End of temporary tests

    private MatrixCursor getDummyCursor() {
        String[] keys = new String[] {KEY + "1", KEY + "2", KEY + "3"};
        return getDummyCursor(keys, "" /* className */);
    }

    private MatrixCursor getDummyCursor(String key, String className) {
        String[] keys = new String[] {key};
        return getDummyCursor(keys, className);
    }

    private MatrixCursor getDummyCursor(String[] keys, String className) {
        MatrixCursor cursor = new MatrixCursor(DatabaseResultLoader.SELECT_COLUMNS);
        final String BLANK = "";
        final byte[] payload = ResultPayloadUtils.marshall(new ResultPayload(INTENT));

        for (int i = 0; i < keys.length; i++) {
            ArrayList<Object> item = new ArrayList<>(DatabaseResultLoader.SELECT_COLUMNS.length);
            item.add(keys[i].hashCode()); // Doc ID
            item.add(TITLES.get(i)); // Title
            item.add(SUMMARY); // Summary on
            item.add(BLANK); // summary off
            item.add(className); // classname
            item.add(BLANK); // screen title
            item.add(null); // Icon
            item.add(INTENT.getAction()); // Intent action
            item.add(TARGET_PACKAGE); // target package
            item.add(TARGET_CLASS); // target class
            item.add(keys[i]); // Key
            item.add(Integer.toString(0));     // Payload Type
            item.add(payload); // Payload

            cursor.addRow(item);
        }
        return cursor;
    }
}
