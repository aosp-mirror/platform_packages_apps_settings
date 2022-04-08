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
import android.os.storage.StorageManager;
import android.text.format.Formatter;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;
import com.android.settingslib.utils.ThreadUtils;

import java.text.NumberFormat;

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

        ThreadUtils.postOnBackgroundThread(() -> {
            final NumberFormat percentageFormat = NumberFormat.getPercentInstance();
            final PrivateStorageInfo info = PrivateStorageInfo.getPrivateStorageInfo(
                    mStorageManagerVolumeProvider);
            final double privateUsedBytes = info.totalBytes - info.freeBytes;

            ThreadUtils.postOnMainThread(() -> {
                preference.setSummary(mContext.getString(R.string.storage_summary,
                        percentageFormat.format(privateUsedBytes / info.totalBytes),
                        Formatter.formatFileSize(mContext, info.freeBytes)));
            });
        });
    }
}
