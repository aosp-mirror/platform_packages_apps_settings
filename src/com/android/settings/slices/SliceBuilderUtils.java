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
 * limitations under the License
 */

package com.android.settings.slices;

import static com.android.settings.slices.SettingsSliceProvider.EXTRA_SLICE_KEY;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.provider.SettingsSlicesContract;
import android.text.TextUtils;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settingslib.core.AbstractPreferenceController;

import androidx.app.slice.Slice;
import androidx.app.slice.builders.SliceAction;
import androidx.app.slice.builders.ListBuilder;
import androidx.app.slice.builders.ListBuilder.RowBuilder;

/**
 * Utility class to build Slices objects and Preference Controllers based on the Database managed
 * by {@link SlicesDatabaseHelper}
 */
public class SliceBuilderUtils {

    private static final String TAG = "SliceBuilder";

    /**
     * Build a Slice from {@link SliceData}.
     *
     * @return a {@link Slice} based on the data provided by {@param sliceData}.
     * Will build an {@link Intent} based Slice unless the Preference Controller name in
     * {@param sliceData} is an inline controller.
     */
    public static Slice buildSlice(Context context, SliceData sliceData) {
        final PendingIntent contentIntent = getContentIntent(context, sliceData);
        final Icon icon = Icon.createWithResource(context, sliceData.getIconResource());
        final BasePreferenceController controller = getPreferenceController(context, sliceData);

        final CharSequence subtitleText = getSubtitleText(context, controller, sliceData);

        final RowBuilder builder = new RowBuilder(context, sliceData.getUri())
                .setTitle(sliceData.getTitle())
                .setTitleItem(icon)
                .setSubtitle(subtitleText)
                .setPrimaryAction(new SliceAction(contentIntent, null, null));

        // TODO (b/71640747) Respect setting availability.

        if (sliceData.getSliceType() == SliceData.SliceType.SWITCH) {
            addToggleAction(context, builder, ((TogglePreferenceController) controller).isChecked(),
                    sliceData.getKey());
        }

        return new ListBuilder(context, sliceData.getUri())
                .addRow(builder)
                .build();
    }

    /**
     * @return the {@link SliceData.SliceType} for the {@param controllerClassName} and key.
     */
    @SliceData.SliceType
    public static int getSliceType(Context context, String controllerClassName,
            String controllerKey) {
        BasePreferenceController controller = getPreferenceController(context, controllerClassName,
                controllerKey);
        return controller.getSliceType();
    }

    /**
     * Looks at the {@link SliceData#preferenceController} from {@param sliceData} and attempts to
     * build an {@link AbstractPreferenceController}.
     */
    public static BasePreferenceController getPreferenceController(Context context,
            SliceData sliceData) {
        return getPreferenceController(context, sliceData.getPreferenceController(),
                sliceData.getKey());
    }

    public static Uri getUri(String path, boolean isPlatformSlice) {
        final String authority = isPlatformSlice
                ? SettingsSlicesContract.AUTHORITY
                : SettingsSliceProvider.SLICE_AUTHORITY;
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority)
                .appendPath(path)
                .build();
    }

    private static BasePreferenceController getPreferenceController(Context context,
            String controllerClassName, String controllerKey) {
        try {
            return BasePreferenceController.createInstance(context, controllerClassName);
        } catch (IllegalStateException e) {
            // Do nothing
        }

        return BasePreferenceController.createInstance(context, controllerClassName, controllerKey);
    }

    private static void addToggleAction(Context context, RowBuilder builder, boolean isChecked,
            String key) {
        PendingIntent actionIntent = getActionIntent(context,
                SettingsSliceProvider.ACTION_TOGGLE_CHANGED, key);
        builder.addEndItem(new SliceAction(actionIntent, null, isChecked));
    }

    private static PendingIntent getActionIntent(Context context, String action, String key) {
        Intent intent = new Intent(action);
        intent.setClass(context, SliceBroadcastReceiver.class);
        intent.putExtra(EXTRA_SLICE_KEY, key);
        return PendingIntent.getBroadcast(context, 0 /* requestCode */, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private static PendingIntent getContentIntent(Context context, SliceData sliceData) {
        Intent intent = DatabaseIndexingUtils.buildSearchResultPageIntent(context,
                sliceData.getFragmentClassName(), sliceData.getKey(), sliceData.getScreenTitle(),
                0 /* TODO */);
        intent.setClassName("com.android.settings", SubSettings.class.getName());
        return PendingIntent.getActivity(context, 0 /* requestCode */, intent, 0 /* flags */);
    }

    @VisibleForTesting
    static CharSequence getSubtitleText(Context context, AbstractPreferenceController controller,
            SliceData sliceData) {
        CharSequence summaryText = sliceData.getSummary();
        if (isValidSummary(context, summaryText)) {
            return summaryText;
        }

        if (controller != null) {
            summaryText = controller.getSummary();

            if (isValidSummary(context, summaryText)) {
                return summaryText;
            }
        }

        return sliceData.getScreenTitle();
    }

    private static boolean isValidSummary(Context context, CharSequence summary) {
        if (summary == null || TextUtils.isEmpty(summary.toString().trim())) {
            return false;
        }

        final CharSequence placeHolder = context.getText(R.string.summary_placeholder);
        final CharSequence doublePlaceHolder =
                context.getText(R.string.summary_two_lines_placeholder);

        return !(TextUtils.equals(summary, placeHolder)
                || TextUtils.equals(summary, doublePlaceHolder));
    }
}
