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


import android.content.ContentResolver;
import android.provider.Settings;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.HashMap;
import java.util.Map;

@Implements(Settings.System.class)
public class ShadowSystemSettings {

    private static final Map<String, Object> sValueMap = new HashMap<>();

    @Implementation
    public static boolean putInt(ContentResolver resolver, String name, int value) {
        sValueMap.put(name, value);
        return true;
    }

    @Implementation
    public static boolean putString(ContentResolver resolver, String name, String value) {
        sValueMap.put(name, value);
        return true;
    }

    @Implementation
    public static String getString(ContentResolver resolver, String name) {
        return (String) sValueMap.get(name);
    }

    @Implementation
    public static String getStringForUser(ContentResolver resolver, String name, int userHandle) {
        return getString(resolver, name);
    }

    @Implementation
    public static boolean putIntForUser(ContentResolver cr, String name, int value,
            int userHandle) {
        return putInt(cr, name, value);
    }

    @Implementation
    public static int getIntForUser(ContentResolver cr, String name, int def, int userHandle) {
        return getInt(cr, name, def);
    }

    @Implementation
    public static int getInt(ContentResolver resolver, String name, int defaultValue) {
        Integer value = (Integer) sValueMap.get(name);
        return value == null ? defaultValue : value;
    }

    public static void reset() {
        sValueMap.clear();
    }
}
