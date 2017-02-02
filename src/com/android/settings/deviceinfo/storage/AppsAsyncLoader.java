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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.ArraySet;

import com.android.settings.applications.PackageManagerWrapper;
import com.android.settings.utils.AsyncLoader;

import java.util.List;

/**
 * AppsAsyncLoader is a Loader which loads app storage information and categories it by the app's
 * specified categorization.
 */
public class AppsAsyncLoader extends AsyncLoader<AppsAsyncLoader.AppsStorageResult> {
    private int mUserId;
    private String mUuid;
    private StorageStatsSource mStatsManager;
    private PackageManagerWrapper mPackageManager;

    public AppsAsyncLoader(Context context, int userId, String uuid, StorageStatsSource source,
            PackageManagerWrapper pm) {
        super(context);
        mUserId = userId;
        mUuid = uuid;
        mStatsManager = source;
        mPackageManager = pm;
    }

    @Override
    public AppsStorageResult loadInBackground() {
        return loadApps();
    }

    private AppsStorageResult loadApps() {
        AppsStorageResult result = new AppsStorageResult();
        ArraySet<Integer> seenUid = new ArraySet<>(); // some apps share a uid

        List<ApplicationInfo> applicationInfos =
                mPackageManager.getInstalledApplicationsAsUser(0, mUserId);
        int size = applicationInfos.size();
        for (int i = 0; i < size; i++) {
            ApplicationInfo app = applicationInfos.get(i);
            if (seenUid.contains(app.uid)) {
                continue;
            }
            seenUid.add(app.uid);

            StorageStatsSource.AppStorageStats stats = mStatsManager.getStatsForUid(mUuid, app.uid);
            // Note: This omits cache intentionally -- we are not attributing it to the apps.
            long appSize = stats.getCodeBytes() + stats.getDataBytes();
            if (app.category == ApplicationInfo.CATEGORY_GAME) {
                result.gamesSize += appSize;
            } else {
                result.otherAppsSize += appSize;
            }
        }
        return result;
    }

    @Override
    protected void onDiscardResult(AppsStorageResult result) {
    }

    public static class AppsStorageResult {
        public long gamesSize;
        public long otherAppsSize;
    }
}
