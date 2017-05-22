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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.drawable.Drawable;
import android.util.ArrayMap;

import com.android.settings.DisplaySettings;
import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.SiteMapManager;
import com.android.settings.gestures.SwipeToNotificationSettings;
import com.android.settings.search.CursorToSearchResultConverter;
import com.android.settings.search.DatabaseResultLoader;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;
import com.android.settings.search.ResultPayload.PayloadType;

import com.android.settings.search.ResultPayloadUtils;
import com.android.settings.search.SearchResult;
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
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class CursorToSearchResultConverterTest {

    private static final String ID = "id";
    private static final String[] TITLES = {"title1", "title2", "title3"};
    private static final String SUMMARY = "summary";
    private static final String TARGET_PACKAGE = "a.b.c";
    private static final String TARGET_CLASS = "a.b.c.class";
    private static final String QUERY = "query";
    private static final String KEY = "key";
    private static final Intent INTENT = new Intent("com.android.settings");
    private static final int ICON = R.drawable.ic_search_history;
    private static final int BASE_RANK = 1;
    private static final int EXAMPLES = 3;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SiteMapManager mSiteMapManager;
    private Drawable mDrawable;
    private CursorToSearchResultConverter mConverter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Context context = Robolectric.buildActivity(Activity.class).get();
        mDrawable = context.getDrawable(ICON);
        mConverter = new CursorToSearchResultConverter(context, QUERY);
    }

    @Test
    public void testParseNullResults_ReturnsNull() {
        final List<SearchResult> results = mConverter.convertCursor(mSiteMapManager, null, BASE_RANK);
        assertThat(results).isNull();
    }

    @Test
    public void testParseCursor_NotNull() {
        final List<SearchResult> results = mConverter.convertCursor(
                mSiteMapManager, getDummyCursor(), BASE_RANK);
        assertThat(results).isNotNull();
    }

    @Test
    public void testParseCursor_MatchesRank() {
        final List<SearchResult> results = mConverter.convertCursor(
                mSiteMapManager, getDummyCursor(), BASE_RANK);
        for (int i = 0; i < EXAMPLES; i++) {
            assertThat(results.get(i).rank).isEqualTo(BASE_RANK);
        }
    }

    @Test
    public void testParseCursor_MatchesTitle() {
        final List<SearchResult> results = mConverter.convertCursor(
                mSiteMapManager, getDummyCursor(), BASE_RANK);
        for (int i = 0; i < EXAMPLES; i++) {
            assertThat(results.get(i).title).isEqualTo(TITLES[i]);
        }
    }

    @Test
    public void testParseCursor_MatchesSummary() {
        final List<SearchResult> results = mConverter.convertCursor(
                mSiteMapManager, getDummyCursor(), BASE_RANK);
        for (int i = 0; i < EXAMPLES; i++) {
            assertThat(results.get(i).summary).isEqualTo(SUMMARY);
        }
    }

    @Test
    public void testParseCursor_MatchesIcon() {
        final MatrixCursor cursor = new MatrixCursor(DatabaseResultLoader.SELECT_COLUMNS);
        final byte[] payload = ResultPayloadUtils.marshall(new ResultPayload(INTENT));
        final String BLANK = "";
        cursor.addRow(new Object[]{
                ID,      // Doc ID
                "Longer than 20 characters", // Title
                SUMMARY, // Summary on
                SUMMARY, // summary off
                DisplaySettings.class.getName(),
                BLANK,   // screen title
                ICON,    // icon
                BLANK,   // action
                null,    // target package
                BLANK,   // target class
                BLANK,   // Key
                PayloadType.INTENT,       // Payload Type
                payload     // Payload
        });

        final List<SearchResult> results = mConverter.convertCursor(
                mSiteMapManager, cursor, BASE_RANK);

        Drawable resultDrawable = results.get(0).icon;
        assertThat(resultDrawable).isNotNull();
        assertThat(resultDrawable.toString()).isEqualTo(mDrawable.toString());
    }

    @Test
    public void testParseCursor_NoIcon() {
        final List<SearchResult> results = mConverter.convertCursor(
                mSiteMapManager, getDummyCursor(false /* hasIcon */, "" /* className */,
                        "" /* key */), BASE_RANK);
        for (int i = 0; i < EXAMPLES; i++) {
            Drawable resultDrawable = results.get(i).icon;
            assertThat(resultDrawable).isNull();
        }
    }

    @Test
    public void testParseCursor_MatchesPayloadType() {
        final List<SearchResult> results = mConverter.convertCursor(
                mSiteMapManager, getDummyCursor(), BASE_RANK);
        ResultPayload payload;
        for (int i = 0; i < EXAMPLES; i++) {
            payload = results.get(i).payload;
            assertThat(payload.getType()).isEqualTo(PayloadType.INTENT);
        }
    }

    @Test
    public void testLongTitle_PenalizedInRank() {
        final MatrixCursor cursor = new MatrixCursor(DatabaseResultLoader.SELECT_COLUMNS);
        final byte[] payload = ResultPayloadUtils.marshall(new ResultPayload(INTENT));
        final String BLANK = "";
        cursor.addRow(new Object[]{
                ID,      // Doc ID
                "Longer than 20 characters", // Title
                SUMMARY, // Summary on
                SUMMARY, // summary off
                DisplaySettings.class.getName(),
                BLANK,   // screen title
                null,    // icon
                BLANK,   // action
                null,    // target package
                BLANK,   // target class
                BLANK,   // Key
                PayloadType.INTENT,       // Payload Type
                payload     // Payload
        });
        final List<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor,
                BASE_RANK);
        assertThat(results.get(0).rank).isEqualTo(BASE_RANK + 2);
    }

    @Test
    public void testParseCursor_MatchesResultPayload() {
        final List<SearchResult> results = mConverter.convertCursor(
                mSiteMapManager, getDummyCursor(), BASE_RANK);
        ResultPayload payload;
        for (int i = 0; i < EXAMPLES; i++) {
            payload = results.get(i).payload;
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
        final ArrayMap<Integer, Boolean> map = new ArrayMap<>();
        map.put(1, true);
        map.put(0, false);
        final String intentKey = "key";
        final String intentVal = "value";
        final Intent intent = new Intent();
        intent.putExtra(intentKey, intentVal);
        final InlineSwitchPayload payload = new InlineSwitchPayload(uri, source, map, intent);

        cursor.addRow(new Object[]{
                ID,      // Doc ID
                TITLES[0], // Title
                SUMMARY, // Summary on
                SUMMARY, // summary off
                SwipeToNotificationSettings.class.getName(),
                BLANK,   // screen title
                null,    // icon
                BLANK,   // action
                null,    // target package
                BLANK,   // target class
                BLANK,   // Key
                type,    // Payload Type
                ResultPayloadUtils.marshall(payload) // Payload
        });
        final List<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor,
                BASE_RANK);
        final InlineSwitchPayload newPayload = (InlineSwitchPayload) results.get(0).payload;
        final Intent rebuiltIntent = newPayload.getIntent();
        assertThat(newPayload.settingsUri).isEqualTo(uri);
        assertThat(newPayload.inlineType).isEqualTo(type);
        assertThat(newPayload.settingSource).isEqualTo(source);
        assertThat(newPayload.valueMap.get(1)).isTrue();
        assertThat(newPayload.valueMap.get(0)).isFalse();
        assertThat(rebuiltIntent.getStringExtra(intentKey)).isEqualTo(intentVal);
    }

    // The following tests are temporary, and should be removed when we replace the Search
    // White-list solution for elevating ranking.

    @Test
    public void testWifiKey_PrioritizedResult() {
        final String key = "main_toggle_wifi";
        final Cursor cursor = getDummyCursor(key,  WifiSettings.class.getName());
        final List<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor,
                BASE_RANK);

        assertThat(results.get(0).rank).isEqualTo(SearchResult.TOP_RANK);
    }

    @Test
    public void testBluetoothKey_PrioritizedResult() {
        final String key = "main_toggle_bluetooth";
        final Cursor cursor = getDummyCursor(key,  WifiSettings.class.getName());
        final List<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor,
                BASE_RANK);

        assertThat(results.get(0).rank).isEqualTo(SearchResult.TOP_RANK);
    }

    @Test
    public void testAirplaneKey_PrioritizedResult() {
        final String key = "toggle_airplane";
        final Cursor cursor = getDummyCursor(key,  WifiSettings.class.getName());
        List<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor, BASE_RANK);

        assertThat(results.get(0).rank).isEqualTo(SearchResult.TOP_RANK);
    }

    @Test
    public void testHotspotKey_PrioritizedResult() {
        final String key = "tether_settings";
        final Cursor cursor = getDummyCursor(key,  WifiSettings.class.getName());
        final List<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor,
                BASE_RANK);

        assertThat(results.get(0).rank).isEqualTo(SearchResult.TOP_RANK);
    }

    @Test
    public void testBatterySaverKey_PrioritizedResult() {
        final String key = "battery_saver";
        final Cursor cursor = getDummyCursor(key,  WifiSettings.class.getName());
        final List<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor,
                BASE_RANK);

        assertThat(results.get(0).rank).isEqualTo(SearchResult.TOP_RANK);
    }

    @Test
    public void testNFCKey_PrioritizedResult() {
        final String key = "toggle_nfc";
        final Cursor cursor = getDummyCursor(key,  WifiSettings.class.getName());
        final List<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor,
                BASE_RANK);

        assertThat(results.get(0).rank).isEqualTo(SearchResult.TOP_RANK);
    }

    @Test
    public void testDataSaverKey_PrioritizedResult() {
        final String key = "restrict_background";
        final Cursor cursor = getDummyCursor(key,  WifiSettings.class.getName());
        final List<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor,
                BASE_RANK);

        assertThat(results.get(0).rank).isEqualTo(SearchResult.TOP_RANK);
    }

    @Test
    public void testDataUsageKey_PrioritizedResult() {
        final String key = "data_usage_enable";
        final Cursor cursor = getDummyCursor(key,  WifiSettings.class.getName());
        final List<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor,
                BASE_RANK);

        assertThat(results.get(0).rank).isEqualTo(SearchResult.TOP_RANK);
    }

    @Test
    public void testRoamingKey_PrioritizedResult() {
        final String key = "button_roaming_key";
        final Cursor cursor = getDummyCursor(key,  WifiSettings.class.getName());
        final List<SearchResult> results = mConverter.convertCursor(mSiteMapManager, cursor,
                BASE_RANK);

        assertThat(results.get(0).rank).isEqualTo(SearchResult.TOP_RANK);
    }

    // End of temporary tests

    private MatrixCursor getDummyCursor() {
        return getDummyCursor(true /* hasIcon */, KEY, "" /* className */);
    }

    private MatrixCursor getDummyCursor(String key, String className) {
        return getDummyCursor(false, key, className);
    }

    private MatrixCursor getDummyCursor(boolean hasIcon, String key, String className) {
        MatrixCursor cursor = new MatrixCursor(DatabaseResultLoader.SELECT_COLUMNS);
        final String BLANK = "";
        final byte[] payload = ResultPayloadUtils.marshall(new ResultPayload(INTENT));

        for (int i = 0; i < EXAMPLES; i++) {
            ArrayList<Object> item = new ArrayList<>(DatabaseResultLoader.SELECT_COLUMNS.length);
            item.add(ID + i); // Doc ID
            item.add(TITLES[i]); // Title
            item.add(SUMMARY); // Summary on
            item.add(BLANK); // summary off
            item.add(className); // classname
            item.add(BLANK); // screen title
            item.add(hasIcon ? Integer.toString(ICON) : null); // Icon
            item.add(INTENT.getAction()); // Intent action
            item.add(TARGET_PACKAGE); // target package
            item.add(TARGET_CLASS); // target class
            item.add(key); // Key
            item.add(Integer.toString(0));     // Payload Type
            item.add(payload); // Payload

            cursor.addRow(item);
        }
        return cursor;
    }
}
