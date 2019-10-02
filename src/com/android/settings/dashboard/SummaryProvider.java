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
 * limitations under the License
 */

package com.android.settings.dashboard;

import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_SUMMARY;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.backup.BackupSettingsHelper;

/** Provide preference summary for injected items. */
public class SummaryProvider extends ContentProvider {
    private static final String BACKUP = "backup";
    private static final String USER = "user";

    @Override
    public Bundle call(String method, String uri, Bundle extras) {
        final Bundle bundle = new Bundle();
        switch (method) {
            case BACKUP:
                bundle.putString(META_DATA_PREFERENCE_SUMMARY,
                        new BackupSettingsHelper(getContext()).getSummary());
                break;
            case USER:
                final Context context = getContext();
                final UserInfo info = context.getSystemService(UserManager.class).getUserInfo(
                        UserHandle.myUserId());
                bundle.putString(META_DATA_PREFERENCE_SUMMARY,
                        context.getString(R.string.users_summary,
                                info.name));
                break;
            default:
                throw new IllegalArgumentException("Unknown Uri format: " + uri);
        }
        return bundle;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
