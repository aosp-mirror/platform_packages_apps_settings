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

import android.content.Context;

import com.android.settingslib.datetime.ZoneGetter;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Implements(ZoneGetter.class)
public class ShadowZoneGetter {

    @Implementation
    public static List<Map<String, Object>> getZonesList(Context context) {
        List<Map<String, Object>> zones = new ArrayList<>();
        zones.add(createDisplayEntry(TimeZone.getDefault(), "gmt-1:00", "FakePlace", 10000));
        return zones;
    }

    private static Map<String, Object> createDisplayEntry(
            TimeZone tz, CharSequence gmtOffsetText, CharSequence displayName, int offsetMillis) {
        Map<String, Object> map = new HashMap<>();
        map.put(ZoneGetter.KEY_ID, tz.getID());
        map.put(ZoneGetter.KEY_DISPLAYNAME, displayName.toString());
        map.put(ZoneGetter.KEY_DISPLAY_LABEL, displayName);
        map.put(ZoneGetter.KEY_GMT, gmtOffsetText.toString());
        map.put(ZoneGetter.KEY_OFFSET_LABEL, gmtOffsetText);
        map.put(ZoneGetter.KEY_OFFSET, offsetMillis);
        return map;
    }
}
