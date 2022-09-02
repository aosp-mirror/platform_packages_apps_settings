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

package com.android.settings.accessibility;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.StringJoiner;

/** Provides utility methods to accessibility quick settings only. */
final class AccessibilityQuickSettingUtils {

    private static final String ACCESSIBILITY_PERF = "accessibility_prefs";
    private static final String KEY_TILE_SERVICE_SHOWN = "tile_service_shown";
    private static final char COMPONENT_NAME_SEPARATOR = ':';
    private static final TextUtils.SimpleStringSplitter sStringColonSplitter =
            new TextUtils.SimpleStringSplitter(COMPONENT_NAME_SEPARATOR);

    /**
     * Opts in component name into {@link AccessibilityQuickSettingUtils#KEY_TILE_SERVICE_SHOWN}
     * colon-separated string in {@link SharedPreferences}.
     *
     * @param context The current context.
     * @param componentName The component name that need to be opted in SharedPreferences.
     */
    public static void optInValueToSharedPreferences(Context context,
            @NonNull ComponentName componentName) {
        final String targetString = getFromSharedPreferences(context);
        if (hasValueInSharedPreferences(targetString, componentName)) {
            return;
        }

        final StringJoiner joiner = new StringJoiner(String.valueOf(COMPONENT_NAME_SEPARATOR));
        if (!TextUtils.isEmpty(targetString)) {
            joiner.add(targetString);
        }
        joiner.add(componentName.flattenToString());

        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(KEY_TILE_SERVICE_SHOWN, joiner.toString()).apply();
    }

    /**
     * Returns if component name existed in {@link
     * AccessibilityQuickSettingUtils#KEY_TILE_SERVICE_SHOWN} string in {@link SharedPreferences}.
     *
     * @param context The current context.
     * @param componentName The component name that need to be checked existed in SharedPreferences.
     * @return {@code true} if componentName existed in SharedPreferences.
     */
    public static boolean hasValueInSharedPreferences(Context context,
            @NonNull ComponentName componentName) {
        final String targetString = getFromSharedPreferences(context);
        return hasValueInSharedPreferences(targetString, componentName);
    }

    private static boolean hasValueInSharedPreferences(String targetString,
            @NonNull ComponentName componentName) {
        if (TextUtils.isEmpty(targetString)) {
            return false;
        }

        sStringColonSplitter.setString(targetString);

        while (sStringColonSplitter.hasNext()) {
            final String name = sStringColonSplitter.next();
            if (TextUtils.equals(componentName.flattenToString(), name)) {
                return true;
            }
        }
        return false;
    }

    private static String getFromSharedPreferences(Context context) {
        return getSharedPreferences(context).getString(KEY_TILE_SERVICE_SHOWN, "");
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(ACCESSIBILITY_PERF, Context.MODE_PRIVATE);
    }

    private AccessibilityQuickSettingUtils(){}
}
