/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;

import android.content.ContentResolver;
import android.provider.Settings;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.shadows.ShadowSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

@Implements(value = Settings.System.class)
public class ShadowSystemSettings extends ShadowSettings.ShadowSystem {
    private static final Map<ContentResolver, Map<String, String>> sDataMap = new WeakHashMap<>();

    @Resetter
    public static void reset() {
        sDataMap.clear();
    }

    @Implementation(minSdk = JELLY_BEAN_MR1)
    protected static boolean putStringForUser(ContentResolver cr, String name, String value,
            int userHandle) {
        return putString(cr, name, value);
    }

    @Implementation(minSdk = JELLY_BEAN_MR1)
    protected static String getStringForUser(ContentResolver cr, String name, int userHandle) {
        return getString(cr, name);
    }

    @Implementation
    protected static boolean putString(ContentResolver cr, String name, String value) {
        get(cr).put(name, value);
        return true;
    }

    @Implementation
    protected static String getString(ContentResolver cr, String name) {
        return get(cr).get(name);
    }

    private static Map<String, String> get(ContentResolver cr) {
        Map<String, String> map = sDataMap.get(cr);
        if (map == null) {
            map = new HashMap<>();
            sDataMap.put(cr, map);
        }
        return map;
    }
}
