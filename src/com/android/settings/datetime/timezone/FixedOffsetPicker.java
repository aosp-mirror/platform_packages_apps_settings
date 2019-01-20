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
import android.icu.util.TimeZone;

import com.android.settings.R;
import com.android.settings.datetime.timezone.model.TimeZoneData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Render a list of fixed offset time zone {@class TimeZoneInfo} into a list view.
 */
public class FixedOffsetPicker extends BaseTimeZoneInfoPicker {
    /**
     * Range of integer fixed UTC offsets shown in the pickers.
     */
    private static final int MIN_HOURS_OFFSET = -14;
    private static final int MAX_HOURS_OFFSET = +12;

    public FixedOffsetPicker() {
        super(R.string.date_time_select_fixed_offset_time_zones,
                R.string.search_settings, false, false);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_ZONE_PICKER_FIXED_OFFSET;
    }

    @Override
    public List<TimeZoneInfo> getAllTimeZoneInfos(TimeZoneData timeZoneData) {
        return loadFixedOffsets();
    }

    /**
     * Returns a {@link TimeZoneInfo} for each fixed offset time zone, such as UTC or GMT+4. The
     * returned list will be sorted in a reasonable way for display.
     */
    private List<TimeZoneInfo> loadFixedOffsets() {
        final TimeZoneInfo.Formatter formatter = new TimeZoneInfo.Formatter(getLocale(),
                new Date());
        final List<TimeZoneInfo> timeZoneInfos = new ArrayList<>();
        timeZoneInfos.add(formatter.format(TimeZone.getFrozenTimeZone("Etc/UTC")));
        for (int hoursOffset = MAX_HOURS_OFFSET; hoursOffset >= MIN_HOURS_OFFSET; --hoursOffset) {
            if (hoursOffset == 0) {
                // UTC is handled above, so don't add GMT +/-0 again.
                continue;
            }
            final String id = String.format(Locale.US, "Etc/GMT%+d", hoursOffset);
            timeZoneInfos.add(formatter.format(TimeZone.getFrozenTimeZone(id)));
        }
        return Collections.unmodifiableList(timeZoneInfos);
    }
}
