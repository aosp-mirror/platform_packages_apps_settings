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

package com.android.settings.deviceinfo;

import android.content.Context;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.text.format.Formatter;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment.ProfileType;
import com.android.settings.deviceinfo.storage.StorageCacheHelper;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;
import com.android.settingslib.utils.ThreadUtils;

import java.text.NumberFormat;
import java.util.concurrent.Future;

public class TopLevelStoragePreferenceController extends BasePreferenceController {

    private final StorageManager mStorageManager;
    private final StorageManagerVolumeProvider mStorageManagerVolumeProvider;

    public TopLevelStoragePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mStorageManager = mContext.getSystemService(StorageManager.class);
        mStorageManagerVolumeProvider = new StorageManagerVolumeProvider(mStorageManager);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    protected void refreshSummary(Preference preference) {
        if (preference == null) {
            return;
        }

        refreshSummaryThread(preference);
    }

    @VisibleForTesting
    protected Future refreshSummaryThread(Preference preference) {
        int userId = Utils.getCurrentUserIdOfType(
                mContext.getSystemService(UserManager.class), ProfileType.PERSONAL);
        final StorageCacheHelper storageCacheHelper = new StorageCacheHelper(mContext, userId);
        long cachedUsedSize = storageCacheHelper.retrieveUsedSize();
        long cachedTotalSize = storageCacheHelper.retrieveCachedSize().totalSize;
        if (cachedUsedSize != 0 && cachedTotalSize != 0) {
            preference.setSummary(getSummary(cachedUsedSize, cachedTotalSize));
        }

        return ThreadUtils.postOnBackgroundThread(() -> {
            final PrivateStorageInfo info = PrivateStorageInfo.getPrivateStorageInfo(
                    getStorageManagerVolumeProvider());

            long usedBytes = info.totalBytes - info.freeBytes;
            storageCacheHelper.cacheUsedSize(usedBytes);
            ThreadUtils.postOnMainThread(() -> {
                preference.setSummary(
                        getSummary(usedBytes, info.totalBytes));
            });
        });
    }

    @VisibleForTesting
    protected StorageManagerVolumeProvider getStorageManagerVolumeProvider() {
        return mStorageManagerVolumeProvider;
    }

    private String getSummary(long usedBytes, long totalBytes) {
        NumberFormat percentageFormat = NumberFormat.getPercentInstance();

        return mContext.getString(R.string.storage_summary,
                totalBytes == 0L ? "0" : percentageFormat.format(((double) usedBytes) / totalBytes),
                Formatter.formatFileSize(mContext, totalBytes - usedBytes));
    }
}
