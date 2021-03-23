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

import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.os.storage.VolumeInfo;

import androidx.annotation.VisibleForTesting;

import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageVolumeProvider;
import com.android.settingslib.utils.AsyncLoaderCompat;

import java.io.IOException;

public class VolumeSizesLoader extends AsyncLoaderCompat<PrivateStorageInfo> {
    private StorageVolumeProvider mVolumeProvider;
    private StorageStatsManager mStats;
    private VolumeInfo mVolume;

    public VolumeSizesLoader(
            Context context,
            StorageVolumeProvider volumeProvider,
            StorageStatsManager stats,
            VolumeInfo volume) {
        super(context);
        mVolumeProvider = volumeProvider;
        mStats = stats;
        mVolume = volume;
    }

    @Override
    protected void onDiscardResult(PrivateStorageInfo result) {}

    @Override
    public PrivateStorageInfo loadInBackground() {
        PrivateStorageInfo volumeSizes;
        try {
            volumeSizes = getVolumeSize(mVolumeProvider, mStats, mVolume);
        } catch (IOException e) {
            return null;
        }
        return volumeSizes;
    }

    @VisibleForTesting
    static PrivateStorageInfo getVolumeSize(
            StorageVolumeProvider storageVolumeProvider, StorageStatsManager stats, VolumeInfo info)
            throws IOException {
        long privateTotalBytes = storageVolumeProvider.getTotalBytes(stats, info);
        long privateFreeBytes = storageVolumeProvider.getFreeBytes(stats, info);
        return new PrivateStorageInfo(privateFreeBytes, privateTotalBytes);
    }
}
