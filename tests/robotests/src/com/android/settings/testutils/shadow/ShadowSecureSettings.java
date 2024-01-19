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
 * limitations under the License.
 */

package com.android.settings.testutils.shadow;

import static android.provider.Settings.DEFAULT_OVERRIDEABLE_BY_RESTORE;

import android.content.ContentResolver;
import android.provider.Settings;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowSettings;

import java.util.Map;
import java.util.WeakHashMap;

@Implements(Settings.Secure.class)
public class ShadowSecureSettings extends ShadowSettings.ShadowSecure {

    private static final Map<ContentResolver, Table<Integer, String, Object>> sUserDataMap =
        new WeakHashMap<>();

    @Implementation
    public static boolean putStringForUser(ContentResolver resolver, String name, String value,
        String tag, boolean makeDefault, int userHandle, boolean overrideableByRestore) {
        final Table<Integer, String, Object> userTable = getUserTable(resolver);
        synchronized (userTable) {
            if (value != null) {
                userTable.put(userHandle, name, value);
            } else {
                userTable.remove(userHandle, name);
            }
            return true;
        }
    }

    /**
     * Same implementation as Settings.Secure because robolectric.ShadowSettings.ShadowSecure
     * overrides this API.
     */
    @Implementation
    public static boolean putString(ContentResolver resolver, String name, String value) {
        return putStringForUser(resolver, name, value, null, false,
                resolver.getUserId(), DEFAULT_OVERRIDEABLE_BY_RESTORE);
    }

    @Implementation
    public static String getStringForUser(ContentResolver resolver, String name, int userHandle) {
        final Table<Integer, String, Object> userTable = getUserTable(resolver);
        synchronized (userTable) {
            return (String) userTable.get(userHandle, name);
        }
    }

    /**
     * Same implementation as Settings.Secure because robolectric.ShadowSettings.ShadowSecure
     * overrides this API.
     */
    @Implementation
    public static boolean putInt(ContentResolver resolver, String name, int value) {
        return putIntForUser(resolver, name, value, resolver.getUserId());
    }

    @Implementation
    public static boolean putIntForUser(ContentResolver resolver, String name, int value,
        int userHandle) {
        final Table<Integer, String, Object> userTable = getUserTable(resolver);
        synchronized (userTable) {
            userTable.put(userHandle, name, value);
            return true;
        }
    }

    /**
     * Same implementation as Settings.Secure because robolectric.ShadowSettings.ShadowSecure
     * overrides this API.
     */
    @Implementation
    public static int getInt(ContentResolver resolver, String name, int def) {
        return getIntForUser(resolver, name, def, resolver.getUserId());
    }

    @Implementation
    public static int getIntForUser(ContentResolver resolver, String name, int def,
        int userHandle) {
        final Table<Integer, String, Object> userTable = getUserTable(resolver);
        synchronized (userTable) {
            final Object object = userTable.get(userHandle, name);
            return object instanceof Integer ? (Integer) object : def;
        }
    }

    @Implementation
    public static boolean putLongForUser(ContentResolver resolver, String name, long value,
        int userHandle) {
        final Table<Integer, String, Object> userTable = getUserTable(resolver);
        synchronized (userTable) {
            userTable.put(userHandle, name, value);
            return true;
        }
    }

    @Implementation
    public static long getLongForUser(ContentResolver resolver, String name, long def,
        int userHandle) {
        final Table<Integer, String, Object> userTable = getUserTable(resolver);
        synchronized (userTable) {
            final Object object = userTable.get(userHandle, name);
            return object instanceof Long ? (Long) object : def;
        }
    }

    @Implementation
    public static boolean putFloatForUser(
            ContentResolver resolver, String name, float value, int userHandle) {
        final Table<Integer, String, Object> userTable = getUserTable(resolver);
        synchronized (userTable) {
            userTable.put(userHandle, name, value);
            return true;
        }
    }

    @Implementation
    public static float getFloatForUser(
            ContentResolver resolver, String name, float def, int userHandle) {
        final Table<Integer, String, Object> userTable = getUserTable(resolver);
        synchronized (userTable) {
            final Object object = userTable.get(userHandle, name);
            return object instanceof Float ? (Float) object : def;
        }
    }

    public static void clear() {
        synchronized (sUserDataMap) {
            sUserDataMap.clear();
        }
    }

    private static Table<Integer, String, Object> getUserTable(ContentResolver contentResolver) {
        synchronized (sUserDataMap) {
            Table<Integer, String, Object> table = sUserDataMap.get(contentResolver);
            if (table == null) {
                table = HashBasedTable.create();
                sUserDataMap.put(contentResolver, table);
            }
            return table;
        }
    }
}
