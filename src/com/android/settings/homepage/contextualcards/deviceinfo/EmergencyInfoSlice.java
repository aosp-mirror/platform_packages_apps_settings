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
import android.content.Context;
import android.content.Intent;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.slices.CustomSliceRegistry;

// This is a slice helper class for EmergencyInfo
public class EmergencyInfoSlice {

    private static final String ACTION_EDIT_EMERGENCY_INFO = "android.settings.EDIT_EMERGENCY_INFO";

    public static Slice getSlice(Context context) {
        final ListBuilder listBuilder = new ListBuilder(context,
                CustomSliceRegistry.EMERGENCY_INFO_SLICE_URI,
                ListBuilder.INFINITY);
        listBuilder.addRow(
                new ListBuilder.RowBuilder()
                        .setTitle(context.getText(R.string.emergency_info_title))
                        .setSubtitle(
                                context.getText(R.string.emergency_info_contextual_card_summary))
                        .setPrimaryAction(createPrimaryAction(context)));
        return listBuilder.build();
    }

    private static SliceAction createPrimaryAction(Context context) {
        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context,
                        0 /* requestCode */,
                        new Intent(ACTION_EDIT_EMERGENCY_INFO),
                        PendingIntent.FLAG_UPDATE_CURRENT);

        return SliceAction.createDeeplink(
                pendingIntent,
                IconCompat.createWithResource(context, R.drawable.empty_icon),
                ListBuilder.ICON_IMAGE,
                context.getText(R.string.emergency_info_title));
    }
}
