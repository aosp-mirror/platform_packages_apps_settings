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
package com.android.settings.datetime.timezone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Data object describing a geographical region.
 *
 * Regions are roughly equivalent to countries, but not every region is a country (for example "U.S.
 * overseas territories" is treated as a country).
 */
public class RegionInfo {

    private final String mId;
    private final String mName;
    private final String mRegionalIndicator;
    private final Collection<String> mTimeZoneIds;

    public RegionInfo(String id, String name, String regionalIndicator,
        Collection<String> timeZoneIds) {
        mId = id;
        mName = name;
        mRegionalIndicator = regionalIndicator;
        mTimeZoneIds = Collections.unmodifiableList(new ArrayList<>(timeZoneIds));
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public Collection<String> getTimeZoneIds() {
        return mTimeZoneIds;
    }

    @Override
    public String toString() {
        return mRegionalIndicator != null ? mRegionalIndicator + " " + mName : mName;
    }
}
