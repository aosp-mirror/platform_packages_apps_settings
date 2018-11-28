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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.text.format.Formatter;
import android.util.Log;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;

import com.android.internal.logging.nano.MetricsProto;
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

    private static final String TAG = "LowStorageSlice";

    /**
     * If user used >= 85% storage.
     */
    private static final double LOW_STORAGE_THRESHOLD = 0.85;

    private final Context mContext;

    public LowStorageSlice(Context context) {
        mContext = context;
    }

    @Override
    public Slice getSlice() {
        // Get current storage percentage from StorageManager.
        final PrivateStorageInfo info = PrivateStorageInfo.getPrivateStorageInfo(
                new StorageManagerVolumeProvider(mContext.getSystemService(StorageManager.class)));
        final double currentStoragePercentage =
                (double) (info.totalBytes - info.freeBytes) / info.totalBytes;

        // Used storage < 85%. NOT show Low storage Slice.
        if (currentStoragePercentage < LOW_STORAGE_THRESHOLD) {
            /**
             * TODO(b/114808204): Contextual Home Page - "Low Storage"
             * The behavior is under decision making, will update new behavior or remove TODO later.
             */
            Log.i(TAG, "Not show low storage slice, not match condition.");
            return null;
        }

        // Show Low storage Slice.
        final IconCompat icon = IconCompat.createWithResource(mContext, R.drawable.ic_storage);
        final CharSequence title = mContext.getText(R.string.storage_menu_free);
        final SliceAction primarySliceAction = new SliceAction(
                PendingIntent.getActivity(mContext, 0, getIntent(), 0), icon, title);
        final String lowStorageSummary = mContext.getString(R.string.low_storage_summary,
                NumberFormat.getPercentInstance().format(currentStoragePercentage),
                Formatter.formatFileSize(mContext, info.freeBytes));

        /**
         * TODO(b/114808204): Contextual Home Page - "Low Storage"
         * Slices doesn't support "Icon on the left" in header. Now we intend to start with Icon
         * right aligned. Will update the icon to left until Slices support it.
         */
        return new ListBuilder(mContext, CustomSliceRegistry.LOW_STORAGE_SLICE_URI,
                ListBuilder.INFINITY)
                .setAccentColor(Utils.getColorAccentDefaultColor(mContext))
                .addRow(new RowBuilder()
                        .setTitle(title)
                        .setSubtitle(lowStorageSummary)
                        .addEndItem(icon, ListBuilder.ICON_IMAGE)
                        .setPrimaryAction(primarySliceAction))
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
                MetricsProto.MetricsEvent.SLICE)
                .setClassName(mContext.getPackageName(), SubSettings.class.getName());
    }
}