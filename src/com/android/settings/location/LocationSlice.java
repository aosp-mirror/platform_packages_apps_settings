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
 *
 */

package com.android.settings.location;

import static android.provider.SettingsSlicesContract.KEY_LOCATION;

import static androidx.slice.builders.ListBuilder.ICON_IMAGE;

import android.annotation.ColorInt;
import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBuilderUtils;

/**
 * Utility class to build an intent-based Location Slice.
 */
public class LocationSlice implements CustomSliceable {

    private final Context mContext;

    public LocationSlice(Context context) {
        mContext = context;
    }

    @Override
    public Slice getSlice() {
        final IconCompat icon = IconCompat.createWithResource(mContext,
                com.android.internal.R.drawable.ic_signal_location);
        final CharSequence title = mContext.getText(R.string.location_settings_title);
        @ColorInt final int color = Utils.getColorAccentDefaultColor(mContext);
        final PendingIntent primaryAction = getPrimaryAction();
        final SliceAction primarySliceAction = SliceAction.createDeeplink(primaryAction, icon,
                ListBuilder.ICON_IMAGE, title);

        return new ListBuilder(mContext, CustomSliceRegistry.LOCATION_SLICE_URI,
                ListBuilder.INFINITY)
                .setAccentColor(color)
                .addRow(new RowBuilder()
                        .setTitle(title)
                        .setTitleItem(icon, ICON_IMAGE)
                        .setPrimaryAction(primarySliceAction))
                .build();
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.LOCATION_SLICE_URI;
    }

    @Override
    public void onNotifyChange(Intent intent) {

    }

    @Override
    public Intent getIntent() {
        final String screenTitle = mContext.getText(R.string.location_settings_title).toString();
        final Uri contentUri = new Uri.Builder().appendPath(KEY_LOCATION).build();
        return SliceBuilderUtils.buildSearchResultPageIntent(mContext,
                LocationSettings.class.getName(), KEY_LOCATION, screenTitle,
                SettingsEnums.LOCATION)
                .setClassName(mContext.getPackageName(), SubSettings.class.getName())
                .setData(contentUri);
    }

    private PendingIntent getPrimaryAction() {
        final Intent intent = getIntent();
        return PendingIntent.getActivity(mContext, 0 /* requestCode */,
                intent, 0 /* flags */);
    }
}
