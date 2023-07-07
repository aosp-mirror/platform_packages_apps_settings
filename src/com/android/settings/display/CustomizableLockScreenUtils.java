/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.display;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

/** Utilities for display settings related to customizable lock screen features. */
public final class CustomizableLockScreenUtils {

    private static final String TAG = "CustomizableLockScreenUtils";
    private static final Uri BASE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority("com.android.systemui.customization")
            .build();
    @VisibleForTesting
    static final Uri FLAGS_URI = BASE_URI.buildUpon()
            .path("flags")
            .build();
    @VisibleForTesting
    static final Uri SELECTIONS_URI = BASE_URI.buildUpon()
            .appendPath("lockscreen_quickaffordance")
            .appendPath("selections")
            .build();
    @VisibleForTesting
    static final String NAME = "name";
    @VisibleForTesting
    static final String VALUE = "value";
    @VisibleForTesting
    static final String ENABLED_FLAG =
            "is_custom_lock_screen_quick_affordances_feature_enabled";
    @VisibleForTesting
    static final String AFFORDANCE_NAME = "affordance_name";

    @VisibleForTesting
    static final String WALLPAPER_LAUNCH_SOURCE = "com.android.wallpaper.LAUNCH_SOURCE";
    @VisibleForTesting
    static final String LAUNCH_SOURCE_SETTINGS = "app_launched_settings";


    private CustomizableLockScreenUtils() {}

    /**
     * Queries and returns whether the customizable lock screen quick affordances feature is enabled
     * on the device.
     *
     * <p>This is a slow, blocking call that shouldn't be made on the main thread.
     */
    public static boolean isFeatureEnabled(Context context) {
        if (!isWallpaperPickerInstalled(context)) {
            return false;
        }

        try (Cursor cursor = context.getContentResolver().query(
                FLAGS_URI,
                null,
                null,
                null)) {
            if (cursor == null) {
                Log.w(TAG, "Cursor was null!");
                return false;
            }

            final int indexOfNameColumn = cursor.getColumnIndex(NAME);
            final int indexOfValueColumn = cursor.getColumnIndex(VALUE);
            if (indexOfNameColumn == -1 || indexOfValueColumn == -1) {
                Log.w(TAG, "Cursor doesn't contain " + NAME + " or " + VALUE + "!");
                return false;
            }

            while (cursor.moveToNext()) {
                final String name = cursor.getString(indexOfNameColumn);
                final int value = cursor.getInt(indexOfValueColumn);
                if (TextUtils.equals(ENABLED_FLAG, name)) {
                    Log.d(TAG, ENABLED_FLAG + "=" + value);
                    return value == 1;
                }
            }

            Log.w(TAG, "Flag with name \"" + ENABLED_FLAG + "\" not found!");
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Exception while querying quick affordance content provider", e);
            return false;
        }
    }

    /**
     * Queries and returns a summary text for the currently-selected lock screen quick affordances.
     *
     * <p>This is a slow, blocking call that shouldn't be made on the main thread.
     */
    @Nullable
    public static CharSequence getQuickAffordanceSummary(Context context) {
        try (Cursor cursor = context.getContentResolver().query(
                SELECTIONS_URI,
                null,
                null,
                null)) {
            if (cursor == null) {
                Log.w(TAG, "Cursor was null!");
                return null;
            }

            final int columnIndex = cursor.getColumnIndex(AFFORDANCE_NAME);
            if (columnIndex == -1) {
                Log.w(TAG, "Cursor doesn't contain \"" + AFFORDANCE_NAME + "\" column!");
                return null;
            }

            final List<String> affordanceNames = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                final String affordanceName = cursor.getString(columnIndex);
                if (!TextUtils.isEmpty(affordanceName)) {
                    affordanceNames.add(affordanceName);
                }
            }

            // We don't display more than the first two items.
            final int usableAffordanceNameCount = Math.min(2, affordanceNames.size());
            final List<String> arguments = new ArrayList<>(usableAffordanceNameCount);
            if (!affordanceNames.isEmpty()) {
                arguments.add(affordanceNames.get(0));
            }
            if (affordanceNames.size() > 1) {
                arguments.add(affordanceNames.get(1));
            }

            return context.getResources().getQuantityString(
                    R.plurals.lockscreen_quick_affordances_summary,
                    usableAffordanceNameCount,
                    arguments.toArray());
        } catch (Exception e) {
            Log.e(TAG, "Exception while querying quick affordance content provider", e);
            return null;
        }
    }

    /**
     * Returns a new {@link Intent} that can be used to start the wallpaper picker
     * activity.
     */
    public static Intent newIntent() {
        final Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
        // By adding the launch source here, we tell our destination (in this case, the wallpaper
        // picker app) that it's been launched from within settings. That way, if we are in a
        // multi-pane configuration (for example, for large screens), the wallpaper picker app can
        // safely skip redirecting to the multi-pane version of its activity, as it's already opened
        // within a multi-pane configuration context.
        intent.putExtra(WALLPAPER_LAUNCH_SOURCE, LAUNCH_SOURCE_SETTINGS);
        return intent;
    }

    private static boolean isWallpaperPickerInstalled(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        return newIntent().resolveActivity(packageManager) != null;
    }
}
