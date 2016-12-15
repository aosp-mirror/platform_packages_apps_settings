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

package com.android.settings.search2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.MatrixCursor;
import android.graphics.drawable.Drawable;

import android.util.ArrayMap;
import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.SubSettings;
import com.android.settings.TestConfig;
import com.android.settings.gestures.GestureSettings;
import com.android.settings.search2.ResultPayload.PayloadType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class CursorToSearchResultConverterTest {

    private CursorToSearchResultConverter mConverter;

    private static final String[] COLUMNS = new String[]{"rank", "title", "summary_on",
            "summary off", "entries", "keywords", "class name", "screen title", "icon",
            "intent action", "target package", "target class", "enabled", "key",
            "payload_type", "payload"};

    private static final String[] TITLES = new String[]{"title1", "title2", "title3"};
    private static final String SUMMARY = "SUMMARY";
    private static final int EXAMPLES = 3;
    private static final Intent mIntent = new Intent("com.android.settings");
    private static final int mIcon = R.drawable.ic_search_history;

    private Drawable mDrawable;

    @Before
    public void setUp() {
        Context context = Robolectric.buildActivity(Activity.class).get();
        mDrawable = context.getDrawable(mIcon);
        mConverter = new CursorToSearchResultConverter(context);
    }

    @Test
    public void testParseNullResults_ReturnsNull() {
        List<SearchResult> results = mConverter.convertCursor(null);
        assertThat(results).isNull();
    }

    @Test
    public void testParseCursor_NotNull() {
        List<SearchResult> results = mConverter.convertCursor(getDummyCursor());
        assertThat(results).isNotNull();
    }

    @Test
    public void testParseCursor_MatchesRank() {
        List<SearchResult> results = mConverter.convertCursor(getDummyCursor());
        for (int i = 0; i < EXAMPLES; i++) {
            assertThat(results.get(i).rank).isEqualTo(i);
        }
    }

    @Test
    public void testParseCursor_MatchesTitle() {
        List<SearchResult> results = mConverter.convertCursor(getDummyCursor());
        for (int i = 0; i < EXAMPLES; i++) {
            assertThat(results.get(i).title).isEqualTo(TITLES[i]);
        }
    }

    @Test
    public void testParseCursor_MatchesSummary() {
        List<SearchResult> results = mConverter.convertCursor(getDummyCursor());
        for (int i = 0; i < EXAMPLES; i++) {
            assertThat(results.get(i).summary).isEqualTo(SUMMARY);
        }
    }

    @Test
    public void testParseCursor_MatchesIcon() {
        List<SearchResult> results = mConverter.convertCursor(getDummyCursor());
        for (int i = 0; i < EXAMPLES; i++) {
            Drawable resultDrawable = results.get(i).icon;
            assertThat(resultDrawable.toString()).isEqualTo(mDrawable.toString());
        }
    }

    @Test
    public void testParseCursor_NoIcon() {
        List<SearchResult> results = mConverter.convertCursor(
                getDummyCursor(false /* hasIcon */));
        for (int i = 0; i < EXAMPLES; i++) {
            Drawable resultDrawable = results.get(i).icon;
            assertThat(resultDrawable).isNull();
        }
    }

    @Test
    public void testParseCursor_MatchesPayloadType() {
        List<SearchResult> results = mConverter.convertCursor(getDummyCursor());
        ResultPayload payload;
        for (int i = 0; i < EXAMPLES; i++) {
            payload = results.get(i).payload;
            assertThat(payload.getType()).isEqualTo(PayloadType.INTENT);
        }
    }

    @Test
    public void testParseCursor_MatchesIntentForSubSettings() {
        MatrixCursor cursor = new MatrixCursor(COLUMNS);
        final String BLANK = "";
        cursor.addRow(new Object[]{
                0,       // rank
                TITLES[0],
                SUMMARY,
                SUMMARY, // summary off
                BLANK,   // entries
                BLANK,   // Keywords
                GestureSettings.class.getName(),
                BLANK,   // screen title
                null,    // icon
                BLANK,   // action
                null,    // target package
                BLANK,   // target class
                BLANK,   // enabled
                BLANK,   // key
                0,       // Payload Type
                null     // Payload
        });
        List<SearchResult> results = mConverter.convertCursor(cursor);
        IntentPayload payload = (IntentPayload) results.get(0).payload;
        Intent intent = payload.intent;
        assertThat(intent.getComponent().getClassName()).isEqualTo(SubSettings.class.getName());
    }

    @Test
    public void testParseCursor_MatchesIntentPayload() {
        List<SearchResult> results = mConverter.convertCursor(getDummyCursor());
        IntentPayload payload;
        for (int i = 0; i < EXAMPLES; i++) {
            payload = (IntentPayload) results.get(i).payload;
            Intent intent = payload.intent;
            assertThat(intent.getAction()).isEqualTo(mIntent.getAction());
        }
    }

    @Test
    public void testParseCursor_MatchesInlineSwitchPayload() {
        MatrixCursor cursor = new MatrixCursor(COLUMNS);
        final String BLANK = "";
        final String uri = "test.com";
        final int type = ResultPayload.PayloadType.INLINE_SWITCH;
        final int source = ResultPayload.SettingsSource.SECURE;
        final ArrayMap<Integer, Boolean> map = new ArrayMap<>();
        map.put(1, true);
        map.put(0, false);
        final InlineSwitchPayload payload = new InlineSwitchPayload(uri, source, map);

        cursor.addRow(new Object[]{
                0,       // rank
                TITLES[0],
                SUMMARY,
                SUMMARY, // summary off
                BLANK,   // entries
                BLANK,   // Keywords
                GestureSettings.class.getName(),
                BLANK,   // screen title
                null,    // icon
                BLANK,   // action
                null,    // target package
                BLANK,   // target class
                BLANK,   // enabled
                BLANK,   // key
                type,    // Payload Type
                ResultPayloadUtils.marshall(payload) // Payload
        });
        List<SearchResult> results = mConverter.convertCursor(cursor);
        InlineSwitchPayload newPayload = (InlineSwitchPayload) results.get(0).payload;

        assertThat(newPayload.settingsUri).isEqualTo(uri);
        assertThat(newPayload.inlineType).isEqualTo(type);
        assertThat(newPayload.settingSource).isEqualTo(source);
        assertThat(newPayload.valueMap.get(1)).isTrue();
        assertThat(newPayload.valueMap.get(0)).isFalse();
    }

    private MatrixCursor getDummyCursor() {
        return getDummyCursor(true /* hasIcon */);
    }

    private MatrixCursor getDummyCursor(boolean hasIcon) {
        MatrixCursor cursor = new MatrixCursor(COLUMNS);
        final String BLANK = "";

        for (int i = 0; i < EXAMPLES; i++) {
            ArrayList<String> item = new ArrayList<>(COLUMNS.length);
            item.add(Integer.toString(i));
            item.add(TITLES[i]);
            item.add(SUMMARY);
            item.add(BLANK); // summary off
            item.add(BLANK); // entries
            item.add(BLANK); // keywords
            item.add(BLANK); // classname
            item.add(BLANK); // screen title
            item.add(hasIcon ? Integer.toString(mIcon) : null);
            item.add(mIntent.getAction());
            item.add(BLANK); // target package
            item.add(BLANK); // target class
            item.add(BLANK); // enabled
            item.add(BLANK); // key
                             // Note there is no user id. This is omitted because it is not being
                             // queried. Should the queries change, so should this method.
            item.add(Integer.toString(0));     // Payload Type
            item.add(null); // Payload

            cursor.addRow(item);
        }
        return cursor;
    }
}
