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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;

import com.android.settings.applications.PackageManagerWrapper;
import com.android.settings.applications.UserManagerWrapper;
import com.android.settings.utils.AsyncLoader;
import com.android.settingslib.applications.StorageStatsSource;

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
        ArraySet<Integer> seenUid = new ArraySet<>(); // some apps share a uid
        AppsStorageResult result = new AppsStorageResult();
        for (int i = 0, size = applicationInfos.size(); i < size; i++) {
            ApplicationInfo app = applicationInfos.get(i);
            if (seenUid.contains(app.uid)) {
                continue;
            }
            seenUid.add(app.uid);

            StorageStatsSource.AppStorageStats stats = mStatsManager.getStatsForUid(mUuid, app.uid);
            // Note: This omits cache intentionally -- we are not attributing it to the apps.
            long appSize = stats.getCodeBytes() + stats.getDataBytes();
            switch (app.category) {
                case CATEGORY_GAME:
                    result.gamesSize += appSize;
                    break;
                case CATEGORY_AUDIO:
                    result.musicAppsSize += appSize;
                    break;
                default:
                    result.otherAppsSize += appSize;
                    break;
            }
        }

        Log.d(TAG, "Loading external stats");
        result.externalStats = mStatsManager.getExternalStorageStats(mUuid, UserHandle.of(userId));
        Log.d(TAG, "Obtaining result completed");
        return result;
    }

    @Override
    protected void onDiscardResult(SparseArray<AppsStorageResult> result) {
    }

    public static class AppsStorageResult {
        public long gamesSize;
        public long musicAppsSize;
        public long otherAppsSize;
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
