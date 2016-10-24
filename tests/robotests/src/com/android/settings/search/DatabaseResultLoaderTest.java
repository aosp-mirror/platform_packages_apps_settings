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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.MatrixCursor;
import android.graphics.drawable.Drawable;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.search2.DatabaseResultLoader;
import com.android.settings.search2.IntentPayload;
import com.android.settings.search2.ResultPayload;
import com.android.settings.search2.ResultPayload.PayloadType;
import com.android.settings.search2.SearchResult;
import com.android.settings.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.robolectric.annotation.Config;
import org.robolectric.Robolectric;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DatabaseResultLoaderTest {
    private DatabaseResultLoader mLoader;

    private static final String[] TITLES = new String[] {"title1", "title2", "title3"};
    private static final String SUMMARY = "SUMMARY";
    private static final int EXAMPLES = 3;
    private static final Intent mIntent = new Intent("com.android.settings");
    private static final int mIcon = R.drawable.ic_search_history;

    private Drawable mDrawable;

    @Before
    public void setUp() {
        Context context = Robolectric.buildActivity(Activity.class).get();
        mDrawable = context.getDrawable(mIcon);
        mLoader = new DatabaseResultLoader(context, "");
    }

    @Test
    public void testParseNullResults_ReturnsNull() {
        List<SearchResult> results = mLoader.parseCursorForSearch(null);
        assertThat(results).isNull();
    }

    @Test
    public void testParseCursor_NotNull() {
        List<SearchResult> results = mLoader.parseCursorForSearch(getDummyCursor());
        assertThat(results).isNotNull();
    }

    @Test
    public void testParseCursor_MatchesRank() {
        List<SearchResult> results = mLoader.parseCursorForSearch(getDummyCursor());
        for (int i = 0; i < EXAMPLES; i++) {
            assertThat(results.get(i).rank).isEqualTo(i);
        }
    }

    @Test
    public void testParseCursor_MatchesTitle() {
        List<SearchResult> results = mLoader.parseCursorForSearch(getDummyCursor());
        for (int i = 0; i < EXAMPLES; i++) {
            assertThat(results.get(i).title).isEqualTo(TITLES[i]);
        }
    }

    @Test
    public void testParseCursor_MatchesSummary() {
        List<SearchResult> results = mLoader.parseCursorForSearch(getDummyCursor());
        for (int i = 0; i < EXAMPLES; i++) {
            assertThat(results.get(i).summary).isEqualTo(SUMMARY);
        }
    }

    @Test
    public void testParseCursor_MatchesIcon() {
        List<SearchResult> results = mLoader.parseCursorForSearch(getDummyCursor());
        for (int i = 0; i < EXAMPLES; i++) {
            Drawable resultDrawable = results.get(i).icon;
            assertThat(resultDrawable.toString()).isEqualTo(mDrawable.toString());
        }
    }

    @Test
    public void testParseCursor_MatchesPayloadType() {
        List<SearchResult> results = mLoader.parseCursorForSearch(getDummyCursor());
        ResultPayload payload;
        for (int i = 0; i < EXAMPLES; i++) {
            payload = results.get(i).payload;
            assertThat(payload.getType()).isEqualTo(PayloadType.INTENT);
        }
    }

    @Test
    public void testParseCursor_MatchesIntentPayload() {
        List<SearchResult> results = mLoader.parseCursorForSearch(getDummyCursor());
        IntentPayload payload;
        for (int i = 0; i < EXAMPLES; i++) {
            payload = (IntentPayload) results.get(i).payload;
            Intent intent = payload.intent;
            assertThat(intent.getAction()).isEqualTo(mIntent.getAction());
        }
    }

    private MatrixCursor getDummyCursor() {
        String[] columns = new String[] {"rank", "title",  "summary_on", "summary off", "entries",
                "keywords", "class name", "screen title", "icon", "intent action",
                "target package", "target class", "enabled", "key", "user id"};
        MatrixCursor cursor = new MatrixCursor(columns);
        final String BLANK = "";

        for (int i = 0; i < EXAMPLES; i++) {
            ArrayList<String> item = new ArrayList<>(columns.length);
            item.add(Integer.toString(i));
            item.add(TITLES[i]);
            item.add(SUMMARY);
            item.add(BLANK); // summary off
            item.add(BLANK); // entries
            item.add(BLANK); // keywords
            item.add(BLANK); // classname
            item.add(BLANK); // screen title
            item.add(Integer.toString(mIcon));
            item.add(mIntent.getAction());
            item.add(BLANK); // target package
            item.add(BLANK); // target class
            item.add(BLANK); // enabled
            item.add(BLANK); // key
            item.add(BLANK); // user id

            cursor.addRow(item);
        }
        return cursor;
    }
}
