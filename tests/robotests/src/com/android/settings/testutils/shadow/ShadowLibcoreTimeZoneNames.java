/*
 * Copyright (C) 2016 The Android Open Source Project
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

import libcore.icu.TimeZoneNames;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.util.ReflectionHelpers;

import java.util.Locale;
import java.util.TimeZone;

/**
 * System.logI used by ZoneStringsCache.create is a method new in API 24 and not available in
 * Robolectric's 6.0 jar. Create a shadow which removes that log call.
 */
@Implements(value = TimeZoneNames.class, isInAndroidSdk = false)
public class ShadowLibcoreTimeZoneNames {

    private static final String[] availableTimeZoneIds = TimeZone.getAvailableIDs();

    @Implements(value = TimeZoneNames.ZoneStringsCache.class, isInAndroidSdk = false)
    public static class ShadowZoneStringsCache {

        @RealObject
        private TimeZoneNames.ZoneStringsCache mRealObject;

        @Implementation
        public String[][] create(Locale locale) {
            // Set up the 2D array used to hold the names. The first column contains the Olson ids.
            String[][] result = new String[availableTimeZoneIds.length][5];
            for (int i = 0; i < availableTimeZoneIds.length; ++i) {
                result[i][0] = availableTimeZoneIds[i];
            }

            ReflectionHelpers.callInstanceMethod(TimeZoneNames.class,
                    mRealObject, "fillZoneStrings",
                    ReflectionHelpers.ClassParameter.from(String.class, locale.toLanguageTag()),
                    ReflectionHelpers.ClassParameter.from(String[][].class, result));

            return result;
        }
    }
}
