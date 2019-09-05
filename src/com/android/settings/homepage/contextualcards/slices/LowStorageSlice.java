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

package com.android.settings.homepage.contextualcards.slices;

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
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.deviceinfo.StorageSettings;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;

import java.text.NumberFormat;

public class LowStorageSlice implements CustomSliceable {

    /**
     * If used storage >= 85%, it would be low storage.
     */
    private static final double LOW_STORAGE_THRESHOLD = 0.85;

    private final Context mContext;

    public LowStorageSlice(Context context) {
        mContext = context;
    }

    @Override
    public Slice getSlice() {
        // Get used storage percentage from StorageManager.
        final PrivateStorageInfo info = PrivateStorageInfo.getPrivateStorageInfo(
                new StorageManagerVolumeProvider(mContext.getSystemService(StorageManager.class)));
        final double usedPercentage = (double) (info.totalBytes - info.freeBytes) / info.totalBytes;

        // Generate Low storage Slice.
        final String percentageString = NumberFormat.getPercentInstance().format(usedPercentage);
        final String freeSizeString = Formatter.formatFileSize(mContext, info.freeBytes);
        final ListBuilder listBuilder = new ListBuilder(mContext,
                CustomSliceRegistry.LOW_STORAGE_SLICE_URI, ListBuilder.INFINITY).setAccentColor(
                Utils.getColorAccentDefaultColor(mContext));
        final IconCompat icon = IconCompat.createWithResource(mContext, R.drawable.ic_storage);

        if (usedPercentage < LOW_STORAGE_THRESHOLD) {
            // For clients that ignore error checking, a generic storage slice will be given.
            final CharSequence titleStorage = mContext.getText(R.string.storage_settings);
            final String summaryStorage = mContext.getString(R.string.storage_summary,
                    percentageString, freeSizeString);

            return listBuilder
                    .addRow(buildRowBuilder(titleStorage, summaryStorage, icon))
                    .setIsError(true)
                    .build();
        }

        final CharSequence titleLowStorage = mContext.getText(R.string.storage_menu_free);
        final String summaryLowStorage = mContext.getString(R.string.low_storage_summary,
                percentageString, freeSizeString);

        return listBuilder
                .addRow(buildRowBuilder(titleLowStorage, summaryLowStorage, icon))
                .build();
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.LOW_STORAGE_SLICE_URI;
    }

    @Override
    public void onNotifyChange(Intent intent) {

    }

    @Override
    public Intent getIntent() {
        final String screenTitle = mContext.getText(R.string.storage_label)
                .toString();

        return SliceBuilderUtils.buildSearchResultPageIntent(mContext,
                StorageSettings.class.getName(), "" /* key */,
                screenTitle,
                SettingsEnums.SLICE)
                .setClassName(mContext.getPackageName(), SubSettings.class.getName())
                .setData(CustomSliceRegistry.LOW_STORAGE_SLICE_URI);
    }

    private RowBuilder buildRowBuilder(CharSequence title, String summary, IconCompat icon) {
        final SliceAction primarySliceAction = SliceAction.createDeeplink(
                PendingIntent.getActivity(mContext, 0, getIntent(), 0), icon,
                ListBuilder.ICON_IMAGE, title);

        return new RowBuilder()
                .setTitleItem(icon, ListBuilder.ICON_IMAGE)
                .setTitle(title)
                .setSubtitle(summary)
                .setPrimaryAction(primarySliceAction);
    }
}