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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;

import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.utils.AsyncLoaderCompat;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * StorageAsyncLoader is a Loader which loads categorized app information and external stats for all
 * users
 */
public class StorageAsyncLoader
        extends AsyncLoaderCompat<SparseArray<StorageAsyncLoader.AppsStorageResult>> {
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
    public SparseArray<AppsStorageResult> loadInBackground() {
        return loadApps();
    }

    private SparseArray<AppsStorageResult> loadApps() {
        mSeenPackages = new ArraySet<>();
        SparseArray<AppsStorageResult> result = new SparseArray<>();
        List<UserInfo> infos = mUserManager.getUsers();
        // Sort the users by user id ascending.
        Collections.sort(
                infos,
                new Comparator<UserInfo>() {
                    @Override
                    public int compare(UserInfo userInfo, UserInfo otherUser) {
                        return Integer.compare(userInfo.id, otherUser.id);
                    }
                });
        for (int i = 0, userCount = infos.size(); i < userCount; i++) {
            UserInfo info = infos.get(i);
            result.put(info.id, getStorageResultForUser(info.id));
        }
        return result;
    }

    private AppsStorageResult getStorageResultForUser(int userId) {
        Log.d(TAG, "Loading apps");
        List<ApplicationInfo> applicationInfos =
                mPackageManager.getInstalledApplicationsAsUser(0, userId);
        AppsStorageResult result = new AppsStorageResult();
        UserHandle myUser = UserHandle.of(userId);
        for (int i = 0, size = applicationInfos.size(); i < size; i++) {
            ApplicationInfo app = applicationInfos.get(i);

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
            long blamedSize = dataSize;
            // Technically, we could overages as freeable on the storage settings screen.
            // If the app is using more cache than its quota, we would accidentally subtract the
            // overage from the system size (because it shows up as unused) during our attribution.
            // Thus, we cap the attribution at the quota size.
            if (cacheQuota < cacheBytes) {
                blamedSize = blamedSize - cacheBytes + cacheQuota;
            }

            // This isn't quite right because it slams the first user by user id with the whole code
            // size, but this ensures that we count all apps seen once.
            if (!mSeenPackages.contains(app.packageName)) {
                blamedSize += stats.getCodeBytes();
                mSeenPackages.add(app.packageName);
            }

            switch (app.category) {
                case CATEGORY_GAME:
                    result.gamesSize += blamedSize;
                    break;
                case CATEGORY_AUDIO:
                    result.musicAppsSize += blamedSize;
                    break;
                case CATEGORY_VIDEO:
                    result.videoAppsSize += blamedSize;
                    break;
                case CATEGORY_IMAGE:
                    result.photosAppsSize += blamedSize;
                    break;
                default:
                    // The deprecated game flag does not set the category.
                    if ((app.flags & ApplicationInfo.FLAG_IS_GAME) != 0) {
                        result.gamesSize += blamedSize;
                        break;
                    }
                    result.otherAppsSize += blamedSize;
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
    protected void onDiscardResult(SparseArray<AppsStorageResult> result) {
    }

    public static class AppsStorageResult {
        public long gamesSize;
        public long musicAppsSize;
        public long photosAppsSize;
        public long videoAppsSize;
        public long otherAppsSize;
        public long cacheSize;
        public StorageStatsSource.ExternalStorageStats externalStats;
    }

    /**
     * ResultHandler defines a destination of data which can handle a result from
     * {@link StorageAsyncLoader}.
     */
    public interface ResultHandler {
        void handleResult(SparseArray<AppsStorageResult> result);
    }
}
