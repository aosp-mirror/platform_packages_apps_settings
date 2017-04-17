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
import static android.content.pm.ApplicationInfo.CATEGORY_VIDEO;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.UserInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.UserHandle;
import android.util.Log;
import android.util.SparseArray;

import com.android.settings.applications.PackageManagerWrapper;
import com.android.settings.applications.UserManagerWrapper;
import com.android.settings.utils.AsyncLoader;
import com.android.settingslib.applications.StorageStatsSource;

import java.io.IOException;
import java.util.List;

/**
 * StorageAsyncLoader is a Loader which loads categorized app information and external stats for all
 * users
 */
public class StorageAsyncLoader
        extends AsyncLoader<SparseArray<StorageAsyncLoader.AppsStorageResult>> {
    private UserManagerWrapper mUserManager;
    private static final String TAG = "StorageAsyncLoader";

    private String mUuid;
    private StorageStatsSource mStatsManager;
    private PackageManagerWrapper mPackageManager;

    public StorageAsyncLoader(Context context, UserManagerWrapper userManager,
            String uuid, StorageStatsSource source, PackageManagerWrapper pm) {
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
        SparseArray<AppsStorageResult> result = new SparseArray<>();
        List<UserInfo> infos = mUserManager.getUsers();
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
                Log.w("App unexpectedly not found", e);
                continue;
            }

            long attributedAppSizeInBytes = stats.getDataBytes();
            // This matches how the package manager calculates sizes -- by zeroing out code sizes of
            // system apps which are not updated. My initial tests suggest that this results in the
            // original code size being counted for updated system apps when they shouldn't, but
            // I am not sure how to avoid this problem without specifically going in to find that
            // code size.
            if (!app.isSystemApp() || app.isUpdatedSystemApp()) {
                attributedAppSizeInBytes += stats.getCodeBytes();
            } else {
                result.systemSize += stats.getCodeBytes();
            }
            switch (app.category) {
                case CATEGORY_GAME:
                    result.gamesSize += attributedAppSizeInBytes;
                    break;
                case CATEGORY_AUDIO:
                    result.musicAppsSize += attributedAppSizeInBytes;
                    break;
                case CATEGORY_VIDEO:
                    result.videoAppsSize += attributedAppSizeInBytes;
                    break;
                default:
                    // The deprecated game flag does not set the category.
                    if ((app.flags & ApplicationInfo.FLAG_IS_GAME) != 0) {
                        result.gamesSize += attributedAppSizeInBytes;
                        break;
                    }
                    result.otherAppsSize += attributedAppSizeInBytes;
                    break;
            }
        }

        Log.d(TAG, "Loading external stats");
        try {
            result.externalStats = mStatsManager.getExternalStorageStats(mUuid, UserHandle.of(userId));
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
        public long videoAppsSize;
        public long otherAppsSize;
        public long systemSize;
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
