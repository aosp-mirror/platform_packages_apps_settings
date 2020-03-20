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

import libcore.timezone.CountryTimeZones;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Wrap {@class CountryTimeZones} to filter time zone that are shown in the picker.
 */
public class FilteredCountryTimeZones {

    /**
     * The timestamp used to determine which time zones to show to users by using the notUsedAfter
     * metadata Android holds for each time zone.
     *
     * notUsedAfter exists because some time zones effectively "merge" with other time zones after
     * a given point in time (i.e. they have identical transitions, offsets, etc.). After that
     * point we only need to show one of the functionally identical ones.
     *
     * Rather than using System.currentTimeMillis(), UX folks asked for consistent behavior and so
     * a timestamp known to be in the recent past is used. This should be updated occasionally but
     * it doesn't have to be very often.
     */
    private static final long MIN_USE_DATE_OF_TIMEZONE = 1546300800000L; // 1/1/2019 00:00 UTC

    private final CountryTimeZones mCountryTimeZones;
    private final List<String> mTimeZoneIds;

    public FilteredCountryTimeZones(CountryTimeZones countryTimeZones) {
        mCountryTimeZones = countryTimeZones;
        List<String> timeZoneIds = countryTimeZones.getTimeZoneMappings().stream()
                .filter(timeZoneMapping ->
                        timeZoneMapping.isShownInPicker()
                                && (timeZoneMapping.getNotUsedAfter() == null
                                || timeZoneMapping.getNotUsedAfter() >= MIN_USE_DATE_OF_TIMEZONE))
                .map(timeZoneMapping -> timeZoneMapping.getTimeZoneId())
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
