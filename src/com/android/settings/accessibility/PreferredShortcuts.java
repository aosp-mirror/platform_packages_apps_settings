/*
 * Copyright (C) 2020 The Android Open Source Project
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

import com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;

import java.util.HashSet;
import java.util.Set;

/** Static utility methods relating to {@link PreferredShortcut} */
public final class PreferredShortcuts {

    private static final String ACCESSIBILITY_PERF = "accessibility_prefs";
    private static final String USER_SHORTCUT_TYPE = "user_shortcut_type";

    /**
     * Retrieves {@link UserShortcutType} for the given {@code componentName} from
     * SharedPreferences.
     *
     * @param context       {@link Context} to access the {@link SharedPreferences}
     * @param componentName Name of the service or activity, should be the format of {@link
     *                      ComponentName#flattenToString()}.
     * @param defaultType   See {@link UserShortcutType}
     * @return {@link UserShortcutType}
     */
    public static int retrieveUserShortcutType(Context context, String componentName,
            int defaultType) {
        if (componentName == null) {
            return defaultType;
        }

        // Create a mutable set to modify
        final Set<String> info = new HashSet<>(getFromSharedPreferences(context));
        info.removeIf(str -> !str.contains(componentName));

        if (info.isEmpty()) {
            return defaultType;
        }

        final String str = info.stream().findFirst().get();
        final PreferredShortcut shortcut = PreferredShortcut.fromString(str);
        return shortcut.getType();
    }

    /**
     * Saves a {@link PreferredShortcut} which containing {@link ComponentName#flattenToString()}
     * and {@link UserShortcutType} in SharedPreferences.
     *
     * @param context  {@link Context} to access the {@link SharedPreferences}
     * @param shortcut Contains {@link ComponentName#flattenToString()} and {@link UserShortcutType}
     */
    public static void saveUserShortcutType(Context context, PreferredShortcut shortcut) {
        final String componentName = shortcut.getComponentName();
        if (componentName == null) {
            return;
        }

        // Create a mutable set to modify
        final Set<String> info = new HashSet<>(getFromSharedPreferences(context));
        info.removeIf(str -> str.contains(componentName));
        info.add(shortcut.toString());
        saveToSharedPreferences(context, info);
    }

    /**
     * Returns a immutable set of {@link PreferredShortcut#toString()} list from
     * SharedPreferences.
     */
    private static Set<String> getFromSharedPreferences(Context context) {
        return getSharedPreferences(context).getStringSet(USER_SHORTCUT_TYPE, Set.of());
    }

    /** Sets a set of {@link PreferredShortcut#toString()} list into SharedPreferences. */
    private static void saveToSharedPreferences(Context context, Set<String> data) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putStringSet(USER_SHORTCUT_TYPE, data).apply();
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(ACCESSIBILITY_PERF, Context.MODE_PRIVATE);
    }

    private PreferredShortcuts() {}
}
