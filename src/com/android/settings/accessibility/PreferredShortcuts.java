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
import android.os.UserHandle;
import android.util.ArrayMap;

import androidx.annotation.NonNull;

import com.android.internal.accessibility.common.ShortcutConstants;
import com.android.internal.accessibility.util.ShortcutUtils;
import com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Static utility methods relating to {@link PreferredShortcut} */
public final class PreferredShortcuts {

    private static final String ACCESSIBILITY_PERF = "accessibility_prefs";
    private static final String USER_SHORTCUT_TYPE = "user_shortcut_type";

    /**
     * Retrieves the user preferred shortcut types for the given {@code componentName} from
     * SharedPreferences.
     *
     * @param context       {@link Context} to access the {@link SharedPreferences}
     * @param componentName Name of the service or activity, should be the format of {@link
     *                      ComponentName#flattenToString()}.
     * @return {@link ShortcutConstants.UserShortcutType}
     */
    @ShortcutConstants.UserShortcutType
    public static int retrieveUserShortcutType(
            @NonNull Context context, @NonNull String componentName) {
        final int defaultTypes = getDefaultPreferredShortcutTypesForTarget(componentName);

        // Create a mutable set to modify
        final Set<String> info = new HashSet<>(getFromSharedPreferences(context));
        info.removeIf(str -> !str.contains(componentName));

        if (info.isEmpty()) {
            return defaultTypes;
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
     * Update the user preferred shortcut from Settings data
     *
     * @param context    {@link Context} to access the {@link SharedPreferences}
     * @param components contains a set of {@link ComponentName} the service or activity. The
     *                   string
     *                   representation of the ComponentName should be in the format of
     *                   {@link ComponentName#flattenToString()}.
     */
    public static void updatePreferredShortcutsFromSettings(
            @NonNull Context context, @NonNull Set<String> components) {
        final Map<Integer, Set<String>> shortcutTypeToTargets = new ArrayMap<>();
        for (int shortcutType : ShortcutConstants.USER_SHORTCUT_TYPES) {
            shortcutTypeToTargets.put(
                    shortcutType,
                    ShortcutUtils.getShortcutTargetsFromSettings(
                            context, shortcutType, UserHandle.myUserId()));
        }

        for (String target : components) {
            int shortcutTypes = ShortcutConstants.UserShortcutType.DEFAULT;
            for (Map.Entry<Integer, Set<String>> entry : shortcutTypeToTargets.entrySet()) {
                if (entry.getValue().contains(target)) {
                    shortcutTypes |= entry.getKey();
                }
            }

            if (shortcutTypes != ShortcutConstants.UserShortcutType.DEFAULT) {
                final PreferredShortcut shortcut = new PreferredShortcut(
                        target, shortcutTypes);
                PreferredShortcuts.saveUserShortcutType(context, shortcut);
            }
        }
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

    /**
     * Returns the default shortcut types for the given accessibility feature.
     */
    @ShortcutConstants.UserShortcutType
    private static int getDefaultPreferredShortcutTypesForTarget(@NonNull String componentName) {
        // TODO (b/322712028): return different default shortcut types for the given component
        return ShortcutConstants.UserShortcutType.SOFTWARE;
    }

    private PreferredShortcuts() {}
}
