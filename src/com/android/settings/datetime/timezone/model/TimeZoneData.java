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

import androidx.annotation.VisibleForTesting;
import androidx.collection.ArraySet;

import libcore.timezone.CountryTimeZones;
import libcore.timezone.CountryZonesFinder;
import libcore.timezone.TimeZoneFinder;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Wrapper of CountryZonesFinder to normalize the country code and only show the regions that are
 * has time zone shown in the time zone picker.
 * getInstance() reads the data from underlying file, and this means it should not be called
 * from the UI thread.
 */
public class TimeZoneData {

    private static WeakReference<TimeZoneData> sCache = null;

    private final CountryZonesFinder mCountryZonesFinder;
    private final Set<String> mRegionIds;

    public static synchronized TimeZoneData getInstance() {
        TimeZoneData data = sCache == null ? null : sCache.get();
        if (data != null) {
            return data;
        }
        data = new TimeZoneData(TimeZoneFinder.getInstance().getCountryZonesFinder());
        sCache = new WeakReference<>(data);
        return data;
    }

    @VisibleForTesting
    public TimeZoneData(CountryZonesFinder countryZonesFinder) {
        mCountryZonesFinder = countryZonesFinder;
        mRegionIds = getNormalizedRegionIds(mCountryZonesFinder.lookupAllCountryIsoCodes());
    }

    public Set<String> getRegionIds() {
        return mRegionIds;
    }

    public Set<String> lookupCountryCodesForZoneId(String tzId) {
        if (tzId == null) {
            return Collections.emptySet();
        }
        List<CountryTimeZones> countryTimeZones = mCountryZonesFinder
                .lookupCountryTimeZonesForZoneId(tzId);
        Set<String> regionIds = new ArraySet<>();
        for (CountryTimeZones countryTimeZone : countryTimeZones) {
            FilteredCountryTimeZones filteredZones = new FilteredCountryTimeZones(countryTimeZone);
            if (filteredZones.getTimeZoneIds().contains(tzId)) {
                regionIds.add(filteredZones.getRegionId());
            }
        }
        return regionIds;
    }

    public FilteredCountryTimeZones lookupCountryTimeZones(String regionId) {
        CountryTimeZones finder = regionId == null ? null
                : mCountryZonesFinder.lookupCountryTimeZones(regionId);
       return finder == null ? null : new FilteredCountryTimeZones(finder);
    }

    private static Set<String> getNormalizedRegionIds(List<String> regionIds) {
        final Set<String> result = new HashSet<>(regionIds.size());
        for (String regionId : regionIds) {
            result.add(normalizeRegionId(regionId));
        }
        return Collections.unmodifiableSet(result);
    }

    // Uppercase ASCII is normalized for the purpose of using ICU API
    public static String normalizeRegionId(String regionId) {
        return regionId == null ? null : regionId.toUpperCase(Locale.US);
    }
}
