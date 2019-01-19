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

package com.android.settings.datetime.timezone;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.icu.text.Collator;
import android.icu.text.LocaleDisplayNames;
import android.icu.util.TimeZone;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.datetime.timezone.model.FilteredCountryTimeZones;
import com.android.settings.datetime.timezone.model.TimeZoneData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

/**
 * Given a region, render a list of time zone {@class TimeZoneInfo} into a list view.
 */
public class RegionZonePicker extends BaseTimeZoneInfoPicker {

    public static final String EXTRA_REGION_ID =
            "com.android.settings.datetime.timezone.region_id";

    private @Nullable String mRegionName;

    public RegionZonePicker() {
        super(R.string.date_time_set_timezone_title, R.string.search_settings, true, false);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_ZONE_PICKER_TIME_ZONE;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final LocaleDisplayNames localeDisplayNames = LocaleDisplayNames.getInstance(getLocale());
        final String regionId =
                getArguments() == null ? null : getArguments().getString(EXTRA_REGION_ID);
        mRegionName = regionId == null ? null : localeDisplayNames.regionDisplayName(regionId);
    }

    @Override
    protected @Nullable CharSequence getHeaderText() {
        return mRegionName;
    }

    /**
     * Add the extra region id into the result.
     */
    @Override
    protected Intent prepareResultData(TimeZoneInfo selectedTimeZoneInfo) {
        final Intent intent = super.prepareResultData(selectedTimeZoneInfo);
        intent.putExtra(EXTRA_RESULT_REGION_ID, getArguments().getString(EXTRA_REGION_ID));
        return intent;
    }

    @Override
    public List<TimeZoneInfo> getAllTimeZoneInfos(TimeZoneData timeZoneData) {
        if (getArguments() == null) {
            Log.e(TAG, "getArguments() == null");
            getActivity().finish();
            return Collections.emptyList();
        }
        String regionId = getArguments().getString(EXTRA_REGION_ID);

        FilteredCountryTimeZones filteredCountryTimeZones = timeZoneData.lookupCountryTimeZones(
                regionId);
        if (filteredCountryTimeZones == null) {
            Log.e(TAG, "region id is not valid: " + regionId);
            getActivity().finish();
            return Collections.emptyList();
        }

        // It could be a timely operations if there are many time zones. A region in time zone data
        // contains a maximum of 29 time zones currently. It may change in the future, but it's
        // unlikely to be changed drastically.
        return getRegionTimeZoneInfo(filteredCountryTimeZones.getTimeZoneIds());
    }

    /**
     * Returns a list of {@link TimeZoneInfo} objects. The returned list will be sorted properly for
     * display in the locale.It may be smaller than the input collection, if equivalent IDs are
     * passed in.
     *
     * @param timeZoneIds a list of Olson IDs.
     */
    public List<TimeZoneInfo> getRegionTimeZoneInfo(Collection<String> timeZoneIds) {
        final TimeZoneInfo.Formatter formatter = new TimeZoneInfo.Formatter(getLocale(),
                new Date());
        final TreeSet<TimeZoneInfo> timeZoneInfos =
                new TreeSet<>(new TimeZoneInfoComparator(Collator.getInstance(getLocale()),
                        new Date()));

        for (final String timeZoneId : timeZoneIds) {
            final TimeZone timeZone = TimeZone.getFrozenTimeZone(timeZoneId);
            // Skip time zone ICU isn't aware.
            if (timeZone.getID().equals(TimeZone.UNKNOWN_ZONE_ID)) {
                continue;
            }
            timeZoneInfos.add(formatter.format(timeZone));
        }
        return Collections.unmodifiableList(new ArrayList<>(timeZoneInfos));
    }

    @VisibleForTesting
    static class TimeZoneInfoComparator implements Comparator<TimeZoneInfo> {
        private Collator mCollator;
        private final Date mNow;

        @VisibleForTesting
        TimeZoneInfoComparator(Collator collator, Date now) {
            mCollator = collator;
            mNow = now;
        }

        @Override
        public int compare(TimeZoneInfo tzi1, TimeZoneInfo tzi2) {
            int result = Integer.compare(tzi1.getTimeZone().getOffset(mNow.getTime()),
                    tzi2.getTimeZone().getOffset(mNow.getTime()));
            if (result == 0) {
                result = Integer.compare(tzi1.getTimeZone().getRawOffset(),
                    tzi2.getTimeZone().getRawOffset());
            }
            if (result == 0) {
                result = mCollator.compare(tzi1.getExemplarLocation(), tzi2.getExemplarLocation());
            }
            if (result == 0 && tzi1.getGenericName() != null && tzi2.getGenericName() != null) {
                result = mCollator.compare(tzi1.getGenericName(), tzi2.getGenericName());
            }
            return result;
        }
    }
}
