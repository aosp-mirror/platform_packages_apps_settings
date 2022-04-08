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
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.text.format.Formatter;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;
import com.android.settingslib.deviceinfo.StorageVolumeProvider;
import com.android.settingslib.utils.ThreadUtils;

import java.text.NumberFormat;

/**
 * SummaryPreferenceController updates the donut storage summary preference to have the
 * correct sizes showing.
 */
public class StorageSummaryDonutPreferenceController extends BasePreferenceController {
    private long mUsedBytes;
    private long mTotalBytes;
    private StorageSummaryDonutPreference mSummary;
    private final StorageManager mStorageManager;
    private final StorageManagerVolumeProvider mStorageManagerVolumeProvider;

    public StorageSummaryDonutPreferenceController(Context context, String key) {
        super(context, key);
        mStorageManager = mContext.getSystemService(StorageManager.class);
        mStorageManagerVolumeProvider = new StorageManagerVolumeProvider(mStorageManager);
    }

    /**
     * Converts a used storage amount to a formatted text.
     *
     * @param usedBytes used bytes of storage
     * @return a formatted text.
     */
    public static CharSequence convertUsedBytesToFormattedText(Context context, long usedBytes) {
        final Formatter.BytesResult result = Formatter.formatBytes(context.getResources(),
                usedBytes, 0);
        return TextUtils.expandTemplate(context.getText(R.string.storage_size_large_alternate),
                result.value, result.units);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mSummary = screen.findPreference(getPreferenceKey());
        mSummary.setEnabled(true);

        ThreadUtils.postOnBackgroundThread(() -> {
            final NumberFormat percentageFormat = NumberFormat.getPercentInstance();
            final PrivateStorageInfo info = PrivateStorageInfo.getPrivateStorageInfo(
                    mStorageManagerVolumeProvider);
            final double privateUsedBytes = info.totalBytes - info.freeBytes;
            mTotalBytes = info.totalBytes;
            mUsedBytes = info.totalBytes - info.freeBytes;

            ThreadUtils.postOnMainThread(() -> {
                updateState(mSummary);
            });
        });
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mSummary.setTitle(convertUsedBytesToFormattedText(mContext, mUsedBytes));
        mSummary.setSummary(mContext.getString(R.string.storage_volume_total,
                Formatter.formatShortFileSize(mContext, mTotalBytes)));
        mSummary.setPercent(mUsedBytes, mTotalBytes);
        mSummary.setEnabled(true);
    }

    /** Invalidates the data on the view and re-renders. */
    public void invalidateData() {
        if (mSummary != null) {
            updateState(mSummary);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    /**
     * Updates the state of the donut preference for the next update.
     *
     * @param used  Total number of used bytes on the summarized volume.
     * @param total Total number of bytes on the summarized volume.
     */
    public void updateBytes(long used, long total) {
        mUsedBytes = used;
        mTotalBytes = total;
        invalidateData();
    }

    /**
     * Updates the state of the donut preference for the next update using volume to summarize.
     *
     * @param volume VolumeInfo to use to populate the informayion.
     */
    public void updateSizes(StorageVolumeProvider svp, VolumeInfo volume) {
        final long sharedDataSize = volume.getPath().getTotalSpace();
        long totalSize = svp.getPrimaryStorageSize();

        if (totalSize <= 0) {
            totalSize = sharedDataSize;
        }

        final long usedBytes = totalSize - volume.getPath().getFreeSpace();
        updateBytes(usedBytes, totalSize);
    }
}
