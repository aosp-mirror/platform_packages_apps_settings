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

import android.graphics.Paint;
import android.icu.text.Collator;
import android.icu.text.LocaleDisplayNames;
import android.icu.text.TimeZoneFormat;
import android.icu.text.TimeZoneNames;
import android.icu.text.TimeZoneNames.NameType;
import android.icu.util.Region;
import android.icu.util.Region.RegionType;
import android.icu.util.TimeZone;
import android.icu.util.TimeZone.SystemTimeZoneType;
import com.android.settingslib.datetime.ZoneGetter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides data for manual selection of time zones based associated to regions. This class makes no
 * attempt to avoid IO and processing intensive actions. This means it should not be called from the
 * UI thread.
 */
public class DataLoader {

    private static final int MIN_HOURS_OFFSET = -14;
    private static final int MAX_HOURS_OFFSET = +12;

    private final Locale mLocale;

    private final Collator mCollator;
    private final LocaleDisplayNames mLocaleDisplayNames;
    private final TimeZoneFormat mTimeZoneFormat;
    private final Paint mPaint;
    private final AtomicLong nextItemId = new AtomicLong(1);
    private final long mNow = System.currentTimeMillis();

    public DataLoader(Locale locale) {
        mLocale = locale;
        mCollator = Collator.getInstance(locale);
        mLocaleDisplayNames = LocaleDisplayNames.getInstance(locale);
        mTimeZoneFormat = TimeZoneFormat.getInstance(locale);
        mPaint = new Paint();
    }

    /**
     * Returns a {@link RegionInfo} object for each region that has selectable time zones. The
     * returned list will be sorted properly for display in the locale.
     */
    public List<RegionInfo> loadRegionInfos() {
        final Set<Region> regions = Region.getAvailable(RegionType.TERRITORY);
        final TreeSet<RegionInfo> regionInfos = new TreeSet<>(new RegionInfoComparator());
        for (final Region region : regions) {
            final String regionId = region.toString();
            final Set<String> timeZoneIds = getTimeZoneIds(regionId);
            if (timeZoneIds.isEmpty()) {
                continue;
            }

            final String name = mLocaleDisplayNames.regionDisplayName(regionId);
            final String regionalIndicator = createRegionalIndicator(regionId);

            regionInfos.add(new RegionInfo(regionId, name, regionalIndicator, timeZoneIds));
        }

        return Collections.unmodifiableList(new ArrayList<>(regionInfos));
    }

    /**
     * Returns a list of {@link TimeZoneInfo} objects. The returned list will be sorted properly for
     * display in the locale.It may be smaller than the input collection, if equivalent IDs are
     * passed in.
     *
     * @param timeZoneIds a list of Olson IDs.
     */
    public List<TimeZoneInfo> loadTimeZoneInfos(Collection<String> timeZoneIds) {
        final TreeSet<TimeZoneInfo> timeZoneInfos = new TreeSet<>(new TimeZoneInfoComparator());
        outer:
        for (final String timeZoneId : timeZoneIds) {
            final TimeZone timeZone = TimeZone.getFrozenTimeZone(timeZoneId);
            for (final TimeZoneInfo other : timeZoneInfos) {
                if (other.getTimeZone().hasSameRules(timeZone)) {
                    continue outer;
                }
            }
            timeZoneInfos.add(createTimeZoneInfo(timeZone));
        }
        return Collections.unmodifiableList(new ArrayList<>(timeZoneInfos));
    }

    /**
     * Returns a {@link TimeZoneInfo} for each fixed offset time zone, such as UTC or GMT+4. The
     * returned list will be sorted in a reasonable way for display.
     */
    public List<TimeZoneInfo> loadFixedOffsets() {
        final List<TimeZoneInfo> timeZoneInfos = new ArrayList<>();
        timeZoneInfos.add(createTimeZoneInfo(TimeZone.getFrozenTimeZone("Etc/UTC")));
        for (int hoursOffset = MAX_HOURS_OFFSET; hoursOffset >= MIN_HOURS_OFFSET; --hoursOffset) {
            if (hoursOffset == 0) {
                // UTC is handled above, so don't add GMT +/-0 again.
                continue;
            }
            final String id = String.format("Etc/GMT%+d", hoursOffset);
            timeZoneInfos.add(createTimeZoneInfo(TimeZone.getFrozenTimeZone(id)));
        }
        return Collections.unmodifiableList(timeZoneInfos);
    }

    /**
     * Gets the set of ids for relevant TimeZones in the given region.
     */
    private Set<String> getTimeZoneIds(String regionId) {
        return TimeZone.getAvailableIDs(
            SystemTimeZoneType.CANONICAL_LOCATION, regionId, /* rawOffset */ null);
    }

    private TimeZoneInfo createTimeZoneInfo(TimeZone timeZone) {
        // Every timezone we handle must be an OlsonTimeZone.
        final String id = timeZone.getID();
        final TimeZoneNames timeZoneNames = mTimeZoneFormat.getTimeZoneNames();
        final java.util.TimeZone javaTimeZone = android.icu.impl.TimeZoneAdapter.wrap(timeZone);
        final CharSequence gmtOffset = ZoneGetter.getGmtOffsetText(mTimeZoneFormat, mLocale,
            javaTimeZone, new Date(mNow));
        return new TimeZoneInfo.Builder(timeZone)
                .setGenericName(timeZoneNames.getDisplayName(id, NameType.LONG_GENERIC, mNow))
                .setStandardName(timeZoneNames.getDisplayName(id, NameType.LONG_STANDARD, mNow))
                .setDaylightName(timeZoneNames.getDisplayName(id, NameType.LONG_DAYLIGHT, mNow))
                .setExemplarLocation(timeZoneNames.getExemplarLocationName(id))
                .setGmtOffset(gmtOffset)
                .setItemId(nextItemId.getAndIncrement())
                .build();
    }

    /**
     * Create a Unicode Region Indicator Symbol for a given region id (a.k.a flag emoji). If the
     * system can't render a flag for this region or the input is not a region id, this returns
     * {@code null}.
     *
     * @param id the two-character region id.
     * @return a String representing the flag of the region or {@code null}.
     */
    private String createRegionalIndicator(String id) {
        if (id.length() != 2) {
            return null;
        }
        final char c1 = id.charAt(0);
        final char c2 = id.charAt(1);
        if ('A' > c1 || c1 > 'Z' || 'A' > c2 || c2 > 'Z') {
            return null;
        }
        // Regional Indicator A is U+1F1E6 which is 0xD83C 0xDDE6 in UTF-16.
        final String regionalIndicator = new String(
            new char[]{0xd83c, (char) (0xdde6 - 'A' + c1), 0xd83c, (char) (0xdde6 - 'A' + c2)});
        if (!mPaint.hasGlyph(regionalIndicator)) {
            return null;
        }
        return regionalIndicator;
    }

    private class TimeZoneInfoComparator implements Comparator<TimeZoneInfo> {

        @Override
        public int compare(TimeZoneInfo tzi1, TimeZoneInfo tzi2) {
            int result =
                Integer
                    .compare(tzi1.getTimeZone().getRawOffset(), tzi2.getTimeZone().getRawOffset());
            if (result == 0) {
                result = mCollator.compare(tzi1.getExemplarLocation(), tzi2.getExemplarLocation());
            }
            if (result == 0 && tzi1.getGenericName() != null && tzi2.getGenericName() != null) {
                result = mCollator.compare(tzi1.getGenericName(), tzi2.getGenericName());
            }
            return result;
        }
    }

    private class RegionInfoComparator implements Comparator<RegionInfo> {

        @Override
        public int compare(RegionInfo r1, RegionInfo r2) {
            return mCollator.compare(r1.getName(), r2.getName());
        }
    }
}
