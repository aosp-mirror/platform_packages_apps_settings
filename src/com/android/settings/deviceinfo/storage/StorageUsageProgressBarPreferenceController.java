/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.UsageProgressBarPreference;

import java.io.File;
import java.io.IOException;

/**
 * Shows storage summary and progress.
 */
public class StorageUsageProgressBarPreferenceController extends BasePreferenceController {

    private static final String TAG = "StorageProgressCtrl";

    private final StorageStatsManager mStorageStatsManager;
    @VisibleForTesting
    long mUsedBytes;
    @VisibleForTesting
    long mTotalBytes;
    private UsageProgressBarPreference mUsageProgressBarPreference;
    private StorageEntry mStorageEntry;
    boolean mIsUpdateStateFromSelectedStorageEntry;
    private StorageCacheHelper mStorageCacheHelper;

    public StorageUsageProgressBarPreferenceController(Context context, String key) {
        super(context, key);

        mStorageStatsManager = context.getSystemService(StorageStatsManager.class);
        mStorageCacheHelper = new StorageCacheHelper(context, UserHandle.myUserId());
    }

    /** Set StorageEntry to display. */
    public void setSelectedStorageEntry(StorageEntry storageEntry) {
        mStorageEntry = storageEntry;
        getStorageStatsAndUpdateUi();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mUsageProgressBarPreference = screen.findPreference(getPreferenceKey());
    }

    private void getStorageStatsAndUpdateUi() {
        // Use cached data for both total size and used size.
        if (mStorageEntry != null && mStorageEntry.isMounted() && mStorageEntry.isPrivate()) {
            StorageCacheHelper.StorageCache cachedData = mStorageCacheHelper.retrieveCachedSize();
            mTotalBytes = cachedData.totalSize;
            mUsedBytes = cachedData.totalUsedSize;
            mIsUpdateStateFromSelectedStorageEntry = true;
            updateState(mUsageProgressBarPreference);
        }
        // Get the latest data from StorageStatsManager.
        ThreadUtils.postOnBackgroundThread(() -> {
            try {
                if (mStorageEntry == null || !mStorageEntry.isMounted()) {
                    throw new IOException();
                }

                if (mStorageEntry.isPrivate()) {
                    // StorageStatsManager can only query private storages.
                    mTotalBytes = mStorageStatsManager.getTotalBytes(mStorageEntry.getFsUuid());
                    mUsedBytes = mTotalBytes
                            - mStorageStatsManager.getFreeBytes(mStorageEntry.getFsUuid());
                } else {
                    final File rootFile = mStorageEntry.getPath();
                    if (rootFile == null) {
                        Log.d(TAG, "Mounted public storage has null root path: " + mStorageEntry);
                        throw new IOException();
                    }
                    mTotalBytes = rootFile.getTotalSpace();
                    mUsedBytes = mTotalBytes - rootFile.getFreeSpace();
                }
            } catch (IOException e) {
                // The storage device isn't present.
                mTotalBytes = 0;
                mUsedBytes = 0;
            }

            if (mUsageProgressBarPreference == null) {
                return;
            }
            mIsUpdateStateFromSelectedStorageEntry = true;
            ThreadUtils.postOnMainThread(() -> updateState(mUsageProgressBarPreference));
        });
    }

    @Override
    public void updateState(Preference preference) {
        if (!mIsUpdateStateFromSelectedStorageEntry) {
            // Returns here to avoid jank by unnecessary UI update.
            return;
        }
        mIsUpdateStateFromSelectedStorageEntry = false;
        mUsageProgressBarPreference.setUsageSummary(StorageUtils.getStorageSummary(
                mContext, R.string.storage_usage_summary, mUsedBytes));
        mUsageProgressBarPreference.setTotalSummary(StorageUtils.getStorageSummary(
                mContext, R.string.storage_total_summary, mTotalBytes));
        mUsageProgressBarPreference.setPercent(mUsedBytes, mTotalBytes);
    }
}
