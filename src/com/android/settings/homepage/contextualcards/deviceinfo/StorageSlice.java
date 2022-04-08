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

package com.android.settings.homepage.contextualcards.deviceinfo;

import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.text.format.Formatter;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.deviceinfo.StorageDashboardFragment;
import com.android.settings.deviceinfo.storage.StorageSummaryDonutPreferenceController;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;

public class StorageSlice implements CustomSliceable {
    private static final String TAG = "StorageSlice";

    private final Context mContext;

    public StorageSlice(Context context) {
        mContext = context;
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.STORAGE_SLICE_URI;
    }

    @Override
    public Slice getSlice() {
        final IconCompat icon = IconCompat.createWithResource(mContext,
                R.drawable.ic_homepage_storage);
        final String title = mContext.getString(R.string.storage_label);
        final SliceAction primaryAction = SliceAction.createDeeplink(getPrimaryAction(), icon,
                ListBuilder.ICON_IMAGE, title);
        final PrivateStorageInfo info = getPrivateStorageInfo();
        return new ListBuilder(mContext, CustomSliceRegistry.STORAGE_SLICE_URI,
                ListBuilder.INFINITY)
                .setAccentColor(Utils.getColorAccentDefaultColor(mContext))
                .setHeader(new ListBuilder.HeaderBuilder().setTitle(title))
                .addRow(new ListBuilder.RowBuilder()
                        .setTitle(getStorageUsedText(info))
                        .setSubtitle(getStorageSummaryText(info))
                        .setPrimaryAction(primaryAction))
                .build();
    }

    @Override
    public Intent getIntent() {
        final String screenTitle = mContext.getText(R.string.storage_label).toString();
        return SliceBuilderUtils.buildSearchResultPageIntent(mContext,
                StorageDashboardFragment.class.getName(), "" /* key */, screenTitle,
                SettingsEnums.SLICE)
                .setClassName(mContext.getPackageName(), SubSettings.class.getName())
                .setData(CustomSliceRegistry.STORAGE_SLICE_URI);
    }

    private PendingIntent getPrimaryAction() {
        final Intent intent = getIntent();
        return PendingIntent.getActivity(mContext, 0  /* requestCode */, intent, 0  /* flags */);
    }

    @VisibleForTesting
    PrivateStorageInfo getPrivateStorageInfo() {
        final StorageManager storageManager = mContext.getSystemService(StorageManager.class);
        final StorageManagerVolumeProvider smvp = new StorageManagerVolumeProvider(storageManager);
        return PrivateStorageInfo.getPrivateStorageInfo(smvp);
    }

    @VisibleForTesting
    CharSequence getStorageUsedText(PrivateStorageInfo info) {
        final long usedBytes = info.totalBytes - info.freeBytes;
        return StorageSummaryDonutPreferenceController.convertUsedBytesToFormattedText(mContext,
                usedBytes);
    }

    @VisibleForTesting
    CharSequence getStorageSummaryText(PrivateStorageInfo info) {
        return mContext.getString(R.string.storage_volume_total,
                Formatter.formatShortFileSize(mContext, info.totalBytes));
    }

    @Override
    public void onNotifyChange(Intent intent) {

    }
}
