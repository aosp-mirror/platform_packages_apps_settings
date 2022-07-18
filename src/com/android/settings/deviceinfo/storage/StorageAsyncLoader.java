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
 */

package com.android.settings.deviceinfo.storage;

import static android.content.pm.ApplicationInfo.CATEGORY_AUDIO;
import static android.content.pm.ApplicationInfo.CATEGORY_GAME;
import static android.content.pm.ApplicationInfo.CATEGORY_IMAGE;
import static android.content.pm.ApplicationInfo.CATEGORY_VIDEO;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Files.FileColumns;
import android.provider.MediaStore.MediaColumns;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;

import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.utils.AsyncLoaderCompat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * StorageAsyncLoader is a Loader which loads categorized app information and external stats for all
 * users
 */
public class StorageAsyncLoader
        extends AsyncLoaderCompat<SparseArray<StorageAsyncLoader.StorageResult>> {
    private UserManager mUserManager;
    private static final String TAG = "StorageAsyncLoader";

    private String mUuid;
    private StorageStatsSource mStatsManager;
    private PackageManager mPackageManager;
    private ArraySet<String> mSeenPackages;

    public StorageAsyncLoader(Context context, UserManager userManager,
            String uuid, StorageStatsSource source, PackageManager pm) {
        super(context);
        mUserManager = userManager;
        mUuid = uuid;
        mStatsManager = source;
        mPackageManager = pm;
    }

    @Override
    public SparseArray<StorageResult> loadInBackground() {
        return getStorageResultsForUsers();
    }

    private SparseArray<StorageResult> getStorageResultsForUsers() {
        mSeenPackages = new ArraySet<>();
        final SparseArray<StorageResult> results = new SparseArray<>();
        final List<UserInfo> infos = mUserManager.getUsers();

        // Sort the users by user id ascending.
        Collections.sort(infos,
                (userInfo, otherUser) -> Integer.compare(userInfo.id, otherUser.id));

        for (UserInfo info : infos) {
            final StorageResult result = getAppsAndGamesSize(info.id);
            final Bundle media = new Bundle();
            media.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, MediaColumns.VOLUME_NAME
                    + "= '" + MediaStore.VOLUME_EXTERNAL_PRIMARY + "'");
            result.imagesSize = getFilesSize(info.id, MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    media /* queryArgs */);
            result.videosSize = getFilesSize(info.id, MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    media /* queryArgs */);
            result.audioSize = getFilesSize(info.id, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    media /* queryArgs */);

            final Bundle documentsAndOtherQueryArgs = new Bundle();
            documentsAndOtherQueryArgs.putString(ContentResolver.QUERY_ARG_SQL_SELECTION,
                    FileColumns.MEDIA_TYPE + "!=" + FileColumns.MEDIA_TYPE_IMAGE
                    + " AND " + FileColumns.MEDIA_TYPE + "!=" + FileColumns.MEDIA_TYPE_VIDEO
                    + " AND " + FileColumns.MEDIA_TYPE + "!=" + FileColumns.MEDIA_TYPE_AUDIO
                    + " AND " + FileColumns.MIME_TYPE + " IS NOT NULL");
            result.documentsAndOtherSize = getFilesSize(info.id,
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    documentsAndOtherQueryArgs);

            final Bundle trashQueryArgs = new Bundle();
            trashQueryArgs.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY);
            result.trashSize = getFilesSize(info.id,
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    trashQueryArgs);

            results.put(info.id, result);
        }
        return results;
    }

    private long getFilesSize(int userId, Uri uri, Bundle queryArgs) {
        final Context perUserContext;
        try {
            perUserContext = getContext().createPackageContextAsUser(
                getContext().getApplicationContext().getPackageName(),
                0 /* flags= */,
                UserHandle.of(userId));
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Not able to get Context for user ID " + userId);
            return 0L;
        }

        try (Cursor cursor = perUserContext.getContentResolver().query(
                uri,
                new String[] {"sum(" + MediaColumns.SIZE + ")"},
                queryArgs,
                null /* cancellationSignal */)) {
            if (cursor == null) {
                return 0L;
            }
            return cursor.moveToFirst() ? cursor.getLong(0) : 0L;
        }
    }

    private StorageResult getAppsAndGamesSize(int userId) {
        Log.d(TAG, "Loading apps");
        final List<ApplicationInfo> applicationInfos =
                mPackageManager.getInstalledApplicationsAsUser(0, userId);
        final StorageResult result = new StorageResult();
        final UserHandle myUser = UserHandle.of(userId);
        for (int i = 0, size = applicationInfos.size(); i < size; i++) {
            final ApplicationInfo app = applicationInfos.get(i);

            StorageStatsSource.AppStorageStats stats;
            try {
                stats = mStatsManager.getStatsForPackage(mUuid, app.packageName, myUser);
            } catch (NameNotFoundException | IOException e) {
                // This may happen if the package was removed during our calculation.
                Log.w(TAG, "App unexpectedly not found", e);
                continue;
            }

            final long dataSize = stats.getDataBytes();
            final long cacheQuota = mStatsManager.getCacheQuotaBytes(mUuid, app.uid);
            final long cacheBytes = stats.getCacheBytes();
            long blamedSize = dataSize + stats.getCodeBytes();
            // Technically, we could overages as freeable on the storage settings screen.
            // If the app is using more cache than its quota, we would accidentally subtract the
            // overage from the system size (because it shows up as unused) during our attribution.
            // Thus, we cap the attribution at the quota size.
            if (cacheQuota < cacheBytes) {
                blamedSize = blamedSize - cacheBytes + cacheQuota;
            }

            // Code bytes may share between different profiles. To know all the duplicate code size
            // and we can get a reasonable system size in StorageItemPreferenceController.
            if (mSeenPackages.contains(app.packageName)) {
                result.duplicateCodeSize += stats.getCodeBytes();
            } else {
                mSeenPackages.add(app.packageName);
            }

            switch (app.category) {
                case CATEGORY_GAME:
                    result.gamesSize += blamedSize;
                    break;
                case CATEGORY_AUDIO:
                case CATEGORY_VIDEO:
                case CATEGORY_IMAGE:
                    result.allAppsExceptGamesSize += blamedSize;
                    break;
                default:
                    // The deprecated game flag does not set the category.
                    if ((app.flags & ApplicationInfo.FLAG_IS_GAME) != 0) {
                        result.gamesSize += blamedSize;
                        break;
                    }
                    result.allAppsExceptGamesSize += blamedSize;
                    break;
            }
        }

        Log.d(TAG, "Loading external stats");
        try {
            result.externalStats = mStatsManager.getExternalStorageStats(mUuid,
                    UserHandle.of(userId));
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        Log.d(TAG, "Obtaining result completed");
        return result;
    }

    @Override
    protected void onDiscardResult(SparseArray<StorageResult> result) {
    }

    /** Storage result for displaying file categories size in Storage Settings. */
    public static class StorageResult {
        // APP based sizes.
        public long gamesSize;
        public long allAppsExceptGamesSize;

        // File based sizes.
        public long audioSize;
        public long imagesSize;
        public long videosSize;
        public long documentsAndOtherSize;
        public long trashSize;

        public long cacheSize;
        public long duplicateCodeSize;
        public StorageStatsSource.ExternalStorageStats externalStats;
    }

    /**
     * ResultHandler defines a destination of data which can handle a result from
     * {@link StorageAsyncLoader}.
     */
    public interface ResultHandler {
        /** Overrides this method to get storage result once it's available. */
        void handleResult(SparseArray<StorageResult> result);
    }
}
