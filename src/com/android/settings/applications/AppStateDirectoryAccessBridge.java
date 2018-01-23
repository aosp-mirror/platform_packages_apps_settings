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
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PACKAGES;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PACKAGES_COLUMNS;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PACKAGES_COL_PACKAGE;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.ArraySet;
import android.util.Log;

import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;

import java.util.Set;

// TODO(b/72055774): add unit tests
public class AppStateDirectoryAccessBridge extends AppStateBaseBridge {

    private static final String TAG = "DirectoryAccessBridge";

    // TODO(b/72055774): set to false once feature is ready (or use Log.isLoggable)
    static final boolean DEBUG = true;
    static final boolean VERBOSE = true;

    public AppStateDirectoryAccessBridge(ApplicationsState appState, Callback callback) {
        super(appState, callback);
    }

    @Override
    protected void loadAllExtraInfo() { }

    @Override
    protected void updateExtraInfo(AppEntry app, String pkg, int uid) { }

    public static final AppFilter FILTER_APP_HAS_DIRECTORY_ACCESS = new AppFilter() {

        private Set<String> mPackages;

        @Override
        public void init() {
            throw new UnsupportedOperationException("Need to call constructor that takes context");
        }

        @Override
        public void init(Context context) {
            mPackages = null;
            final Uri providerUri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(AUTHORITY).appendPath(TABLE_PACKAGES).appendPath("*")
                    .build();
            try (Cursor cursor = context.getContentResolver().query(providerUri,
                    TABLE_PACKAGES_COLUMNS, null, null)) {
                if (cursor == null) {
                    Log.w(TAG, "Didn't get cursor for " + providerUri);
                    return;
                }
                final int count = cursor.getCount();
                if (count == 0) {
                    if (DEBUG) {
                        Log.d(TAG, "No packages anymore (was " + mPackages + ")");
                    }
                    return;
                }
                mPackages = new ArraySet<>(count);
                while (cursor.moveToNext()) {
                    mPackages.add(cursor.getString(TABLE_PACKAGES_COL_PACKAGE));
                }
                if (DEBUG) {
                    Log.d(TAG, "init(): " + mPackages);
                }
            }
        }


        @Override
        public boolean filterApp(AppEntry info) {
            return mPackages != null && mPackages.contains(info.info.packageName);
        }
    };
}
