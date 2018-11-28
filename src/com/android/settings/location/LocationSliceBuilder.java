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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.SliceBuilderUtils;

/**
 * Utility class to build an intent-based Location Slice.
 */
public class LocationSliceBuilder {

    private LocationSliceBuilder() {
    }

    /**
     * Return a Location Slice bound to {@link CustomSliceRegistry#LOCATION_SLICE_URI}.
     */
    public static Slice getSlice(Context context) {
        final IconCompat icon = IconCompat.createWithResource(context,
                R.drawable.ic_signal_location);
        final String title = context.getString(R.string.location_settings_title);
        @ColorInt final int color = Utils.getColorAccentDefaultColor(context);
        final PendingIntent primaryAction = getPrimaryAction(context);
        final SliceAction primarySliceAction = new SliceAction(primaryAction, icon, title);

        return new ListBuilder(context, CustomSliceRegistry.LOCATION_SLICE_URI,
                ListBuilder.INFINITY)
                .setAccentColor(color)
                .addRow(new RowBuilder()
                        .setTitle(title)
                        .setTitleItem(icon, ICON_IMAGE)
                        .setPrimaryAction(primarySliceAction))
                .build();
    }

    public static Intent getIntent(Context context) {
        final String screenTitle = context.getText(R.string.location_settings_title).toString();
        final Uri contentUri = new Uri.Builder().appendPath(KEY_LOCATION).build();
        return SliceBuilderUtils.buildSearchResultPageIntent(context,
                LocationSettings.class.getName(), KEY_LOCATION, screenTitle,
                MetricsEvent.LOCATION)
                .setClassName(context.getPackageName(), SubSettings.class.getName())
                .setData(contentUri);
    }

    private static PendingIntent getPrimaryAction(Context context) {
        final Intent intent = getIntent(context);
        return PendingIntent.getActivity(context, 0 /* requestCode */,
                intent, 0 /* flags */);
    }
}
