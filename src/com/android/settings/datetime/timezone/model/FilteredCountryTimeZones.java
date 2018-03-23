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
 * limitations under the License.
 */

package com.android.settings.datetime.timezone.model;

import libcore.util.CountryTimeZones;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Wrap {@class CountryTimeZones} to filter time zone that are shown in the picker.
 */
public class FilteredCountryTimeZones {

    // New timezone list and the meta data of time zone, notUsedAfter, is introduced in Android P
    // in 2018. Only show time zone used in or after 2018.
    private static final long MIN_USE_DATE_OF_TIMEZONE = 1514764800000L; // 1/1/2018 00:00 UTC

    private final CountryTimeZones mCountryTimeZones;
    private final List<String> mTimeZoneIds;

    public FilteredCountryTimeZones(CountryTimeZones countryTimeZones) {
        mCountryTimeZones = countryTimeZones;
        List<String> timeZoneIds = countryTimeZones.getTimeZoneMappings().stream()
                .filter(timeZoneMapping ->
                        timeZoneMapping.showInPicker && (timeZoneMapping.notUsedAfter == null
                                || timeZoneMapping.notUsedAfter >= MIN_USE_DATE_OF_TIMEZONE))
                .map(timeZoneMapping -> timeZoneMapping.timeZoneId)
                .collect(Collectors.toList());
        mTimeZoneIds = Collections.unmodifiableList(timeZoneIds);
    }

    public List<String> getTimeZoneIds() {
        return mTimeZoneIds;
    }

    public CountryTimeZones getCountryTimeZones() {
        return mCountryTimeZones;
    }

    public String getRegionId() {
        return TimeZoneData.normalizeRegionId(mCountryTimeZones.getCountryIso());
    }
}
