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

import android.content.Context;
import android.content.SharedPreferences;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/** Utility class for SharedPreferences. */
public final class SharedPreferenceUtils {

    private static final String ACCESSIBILITY_PERF = "accessibility_prefs";
    private static final String USER_SHORTCUT_TYPE = "user_shortcut_type";
    private SharedPreferenceUtils() { }

    private static SharedPreferences getSharedPreferences(Context context, String fileName) {
        return context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
    }

    /** Returns a set of user shortcuts list to determine user preferred service shortcut. */
    public static Set<String> getUserShortcutTypes(Context context) {
        return getSharedPreferences(context, ACCESSIBILITY_PERF)
                .getStringSet(USER_SHORTCUT_TYPE, ImmutableSet.of());
    }

    /** Sets a set of user shortcuts list to determine user preferred service shortcut. */
    public static void setUserShortcutType(Context context, Set<String> data) {
        SharedPreferences.Editor editor = getSharedPreferences(context, ACCESSIBILITY_PERF).edit();
        editor.remove(USER_SHORTCUT_TYPE).apply();
        editor.putStringSet(USER_SHORTCUT_TYPE, data).apply();
    }
}
