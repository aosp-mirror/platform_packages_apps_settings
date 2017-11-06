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


import android.icu.util.TimeZone;

import com.android.settingslib.datetime.ZoneGetter;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Implements(ZoneGetter.ZoneGetterData.class)
public class ShadowZoneGetterData {
    private static final Map<String, List<String>> TIME_ZONE_LOOKUP = new HashMap<>();

    static {
        TIME_ZONE_LOOKUP.put("FR", Collections.singletonList(
                TimeZone.getTimeZone("Europe/Paris", TimeZone.TIMEZONE_JDK).getID()));
        TIME_ZONE_LOOKUP.put("ML", Collections.singletonList(
                TimeZone.getTimeZone("Europe/Amsterdam", TimeZone.TIMEZONE_JDK).getID()));
        TIME_ZONE_LOOKUP.put("US", Arrays.asList(
                TimeZone.getTimeZone("America/New_York", TimeZone.TIMEZONE_JDK).getID()));
        TIME_ZONE_LOOKUP.put("JP", Collections.singletonList(
                TimeZone.getTimeZone("Asia/Tokyo", TimeZone.TIMEZONE_JDK).getID()));
    }

    @Implementation
    public List<String> lookupTimeZoneIdsByCountry(String country) {
        return TIME_ZONE_LOOKUP.get(country);
    }
}
