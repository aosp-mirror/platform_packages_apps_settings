/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.applications;

import static android.os.storage.StorageVolume.ScopedAccessProviderContract.AUTHORITY;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS_COLUMNS;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS_COL_DIRECTORY;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS_COL_GRANTED;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS_COL_PACKAGE;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS_COL_VOLUME_UUID;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;

/**
 * Detailed settings for an app's directory access permissions (A.K.A Scoped Directory Access).
 */
// TODO(b/63720392): explain its layout
// TODO(b/63720392): add unit tests
public class DirectoryAccessDetails extends AppInfoWithHeader implements OnPreferenceChangeListener,
        OnPreferenceClickListener {
    private static final String MY_TAG = "DirectoryAccessDetails";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (true) {
            // TODO(b/63720392): temporarily hack so the screen doesn't crash..
            addPreferencesFromResource(R.xml.app_ops_permissions_details);
            // ... we need to dynamically create the preferences by calling the provider instead:
            try (Cursor cursor = getContentResolver().query(
                    new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                            .authority(AUTHORITY).appendPath(TABLE_PERMISSIONS).appendPath("*")
                            .build(),
                    TABLE_PERMISSIONS_COLUMNS, null, null)) {
                if (cursor == null) {
                    Log.w(TAG, "didn't get cursor");
                    return;
                }
                final int count = cursor.getCount();
                if (count == 0) {
                    Log.d(TAG, "no permissions");
                    return;
                }
                while (cursor.moveToNext()) {
                    final String pkg = cursor.getString(TABLE_PERMISSIONS_COL_PACKAGE);
                    final String uuid = cursor.getString(TABLE_PERMISSIONS_COL_VOLUME_UUID);
                    final String dir = cursor.getString(TABLE_PERMISSIONS_COL_DIRECTORY);
                    final boolean granted = cursor.getInt(TABLE_PERMISSIONS_COL_GRANTED) == 1;
                    Log.v(MY_TAG, "pkg:" + pkg + " uuid: " + uuid + " dir: " + dir + "> "
                            + granted);
                }
            }
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // TODO(b/63720392): implement or remove listener
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // TODO(b/63720392): implement or remove listener
        return false;
    }

    @Override
    protected boolean refreshUi() {
        // TODO(b/63720392): implement
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.APPLICATIONS_DIRECTORY_ACCESS_DETAIL;
    }
}
