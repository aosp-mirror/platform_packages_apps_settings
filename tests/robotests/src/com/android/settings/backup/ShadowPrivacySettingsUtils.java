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
 * limitations under the License
 */

package com.android.settings.backup;

import android.content.Context;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@Implements(PrivacySettingsUtils.class)
public class ShadowPrivacySettingsUtils {
    private static boolean sIsAdminUser;
    private static boolean sIsInvisibleKey;

    @Resetter
    static void reset() {
        sIsAdminUser = true;
        sIsInvisibleKey = false;
    }

    @Implementation
    public static boolean isAdminUser(final Context context) {
        return sIsAdminUser;
    }

    @Implementation
    public static boolean isInvisibleKey(final Context context, final String key) {
        return sIsInvisibleKey;
    }

    public static void setIsAdminUser(boolean isAdminUser) {
        sIsAdminUser = isAdminUser;
    }

    public static void setIsInvisibleKey(boolean isInvisibleKey) {
        sIsInvisibleKey = isInvisibleKey;
    }
}
