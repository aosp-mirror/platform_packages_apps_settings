/*
 * Copyright (C) 2019 The Android Open Source Project
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
 */

package com.android.settings.slices;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.SettingsSlicesContract;

import com.android.settings.testutils.FakeIndexProvider;
import com.android.settings.testutils.FakeToggleController;

class SliceTestUtils {

    public static final String FAKE_TITLE = "title";
    public static final String FAKE_SUMMARY = "summary";
    public static final String FAKE_SCREEN_TITLE = "screen_title";
    public static final String FAKE_KEYWORDS = "a, b, c";
    public static final int FAKE_ICON = 1234;
    public static final String FAKE_FRAGMENT_NAME = FakeIndexProvider.class.getName();
    public static final String FAKE_CONTROLLER_NAME = FakeToggleController.class.getName();


    public static void insertSliceToDb(Context context, String key) {
        insertSliceToDb(context, key, true /* isPlatformSlice */);
    }

    public static void insertSliceToDb(Context context, String key, boolean isPlatformSlice) {
        insertSliceToDb(context, key, isPlatformSlice, null /*customizedUnavailableSliceSubtitle*/);
    }

    public static void insertSliceToDb(Context context, String key, boolean isPlatformSlice,
            String customizedUnavailableSliceSubtitle) {
        insertSliceToDb(context, key, isPlatformSlice, customizedUnavailableSliceSubtitle, false);
    }

    public static void insertSliceToDb(Context context, String key, boolean isPlatformSlice,
            String customizedUnavailableSliceSubtitle, boolean isPublicSlice) {
        final SQLiteDatabase db = SlicesDatabaseHelper.getInstance(context).getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SlicesDatabaseHelper.IndexColumns.KEY, key);
        values.put(SlicesDatabaseHelper.IndexColumns.SLICE_URI,
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(isPlatformSlice
                                ? SettingsSlicesContract.AUTHORITY
                                : SettingsSliceProvider.SLICE_AUTHORITY)
                        .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                        .appendPath(key)
                        .build().toSafeString());
        values.put(SlicesDatabaseHelper.IndexColumns.TITLE, FAKE_TITLE);
        values.put(SlicesDatabaseHelper.IndexColumns.SUMMARY, FAKE_SUMMARY);
        values.put(SlicesDatabaseHelper.IndexColumns.SCREENTITLE, FAKE_SCREEN_TITLE);
        values.put(SlicesDatabaseHelper.IndexColumns.KEYWORDS, FAKE_KEYWORDS);
        values.put(SlicesDatabaseHelper.IndexColumns.ICON_RESOURCE, FAKE_ICON);
        values.put(SlicesDatabaseHelper.IndexColumns.FRAGMENT, FAKE_FRAGMENT_NAME);
        values.put(SlicesDatabaseHelper.IndexColumns.CONTROLLER, FAKE_CONTROLLER_NAME);
        values.put(SlicesDatabaseHelper.IndexColumns.SLICE_TYPE, SliceData.SliceType.INTENT);
        values.put(SlicesDatabaseHelper.IndexColumns.UNAVAILABLE_SLICE_SUBTITLE,
                customizedUnavailableSliceSubtitle);
        values.put(SlicesDatabaseHelper.IndexColumns.PUBLIC_SLICE, isPublicSlice);

        db.replaceOrThrow(SlicesDatabaseHelper.Tables.TABLE_SLICES_INDEX, null, values);
        db.close();
    }
}
